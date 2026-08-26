package com.example.engine

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.data.AnnotationEntity
import com.example.data.AnnotationType
import com.example.data.AppDatabase
import com.example.data.DocumentEntity
import com.example.data.FormFieldEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Cloud model representation of a backed-up document.
 */
data class CloudDocument(
    val id: String = "",
    val title: String = "",
    val pageCount: Int = 1,
    val fileSize: Long = 0,
    val isFavorite: Boolean = false,
    val isEncrypted: Boolean = false,
    val tags: String = "General",
    val ocrExtractedText: String = "",
    val createdTimestamp: Long = 0,
    val lastOpenedTimestamp: Long = 0,
    val lastSyncedTimestamp: Long = System.currentTimeMillis(),
    val pdfBase64Data: String? = null, // Embedded Base64 for documents under 800KB
    val annotationsJson: List<Map<String, Any>> = emptyList(),
    val formFieldsJson: List<Map<String, Any>> = emptyList()
)

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String, val syncedCount: Int = 0) : SyncState()
    data class Error(val errorMessage: String) : SyncState()
}

/**
 * CloudStorageService handles cloud backup, multi-device document synchronization,
 * and Google Sign-In authentication via Jetpack CredentialManager with Firebase Firestore.
 */
class CloudStorageService(
    private val context: Context,
    private val database: AppDatabase
) {
    private val firebaseApp: FirebaseApp? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setProjectId("pdf-studio-cloud")
                    .setApiKey("AIzaSyFakeKeyForLocalFallbackSyncingMode00")
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.getInstance()
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp default initialization fallback", e)
            runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
        }
    }

    private val auth: FirebaseAuth? by lazy {
        try {
            firebaseApp?.let { FirebaseAuth.getInstance(it) } ?: runCatching { FirebaseAuth.getInstance() }.getOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth not available", e)
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            firebaseApp?.let { FirebaseFirestore.getInstance(it) } ?: runCatching { FirebaseFirestore.getInstance() }.getOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore not available", e)
            null
        }
    }

    private val credentialManager by lazy {
        try {
            CredentialManager.create(context)
        } catch (e: Exception) {
            Log.w(TAG, "CredentialManager not available", e)
            null
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    private var cloudSnapshotListener: ListenerRegistration? = null

    init {
        try {
            val currentAuth = auth
            if (currentAuth != null) {
                _currentUser.value = currentAuth.currentUser
                currentAuth.addAuthStateListener { firebaseAuth ->
                    _currentUser.value = firebaseAuth.currentUser
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing auth state listener", e)
        }
    }

    /**
     * Authenticate user with Google Sign-In using CredentialManager.
     */
    suspend fun signInWithGoogle(webClientId: String = ""): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val currentAuth = auth
        val credManager = credentialManager
        if (currentAuth == null || credManager == null) {
            return@withContext Result.failure(IllegalStateException("Firebase Auth / CredentialManager is not available on this device configuration."))
        }

        try {
            val serverClientId = if (webClientId.isNotBlank()) webClientId else getFallbackClientId()

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = currentAuth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase User is null after sign in")
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type: ${credential.javaClass.name}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out the current Firebase user.
     */
    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Sign out error", e)
        }
        _currentUser.value = null
        cloudSnapshotListener?.remove()
        cloudSnapshotListener = null
    }

    /**
     * Synchronizes and backs up a single document to Firebase Firestore under the authenticated user's account.
     */
    suspend fun backupDocument(document: DocumentEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth?.currentUser
        val currentFirestore = firestore
        if (user == null || currentFirestore == null) {
            val err = if (currentFirestore == null) "Cloud Firestore is not initialized." else "User must be signed in with Google to sync documents to the cloud."
            return@withContext Result.failure(IllegalStateException(err))
        }

        try {
            _syncState.value = SyncState.Syncing

            // Load annotations & form fields
            val annotations = database.annotationDao().getAllAnnotationsForDoc(document.id)
            val formFields = database.formDao().getAllFormFieldsForDoc(document.id)

            // Read PDF file bytes if within Firestore document payload threshold (under 800KB)
            val file = File(document.filePath)
            val base64Data = if (file.exists() && file.length() < 800 * 1024) {
                Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            } else {
                null
            }

            val cloudDocMap = hashMapOf<String, Any?>(
                "title" to document.title,
                "pageCount" to document.pageCount,
                "fileSize" to document.fileSize,
                "isFavorite" to document.isFavorite,
                "isEncrypted" to document.isEncrypted,
                "tags" to document.tags,
                "ocrExtractedText" to document.ocrExtractedText,
                "createdTimestamp" to document.createdTimestamp,
                "lastOpenedTimestamp" to document.lastOpenedTimestamp,
                "lastSyncedTimestamp" to System.currentTimeMillis(),
                "pdfBase64Data" to base64Data,
                "annotations" to annotations.map { ann ->
                    mapOf(
                        "pageIndex" to ann.pageIndex,
                        "type" to ann.type.name,
                        "colorHex" to ann.colorHex,
                        "strokeWidth" to ann.strokeWidth,
                        "opacity" to ann.opacity,
                        "pointsJson" to ann.pointsJson,
                        "textContent" to ann.textContent,
                        "rectBoundsJson" to ann.rectBoundsJson,
                        "extraData" to ann.extraData
                    )
                },
                "formFields" to formFields.map { ff ->
                    mapOf(
                        "pageIndex" to ff.pageIndex,
                        "fieldName" to ff.fieldName,
                        "fieldType" to ff.fieldType,
                        "fieldValue" to ff.fieldValue,
                        "rectBoundsJson" to ff.rectBoundsJson
                    )
                }
            )

            val docDocRef = currentFirestore.collection("users")
                .document(user.uid)
                .collection("documents")
                .document("doc_${document.id}_${document.title.hashCode()}")

            docDocRef.set(cloudDocMap, SetOptions.merge()).await()

            _lastBackupTime.value = System.currentTimeMillis()
            _syncState.value = SyncState.Success("Document '${document.title}' backed up to Cloud", 1)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup document", e)
            _syncState.value = SyncState.Error("Backup failed: ${e.localizedMessage ?: "Unknown error"}")
            Result.failure(e)
        }
    }

    /**
     * Backs up all local documents and annotations to Firebase Firestore.
     */
    suspend fun backupAllDocuments(): Result<Int> = withContext(Dispatchers.IO) {
        val user = auth?.currentUser
        if (user == null || firestore == null) {
            return@withContext Result.failure(IllegalStateException("User must be signed in with Google to sync documents to the cloud."))
        }

        try {
            _syncState.value = SyncState.Syncing
            val allDocs = database.documentDao().getAllDocumentsSync()
            var count = 0

            for (doc in allDocs) {
                val res = backupDocument(doc)
                if (res.isSuccess) {
                    count++
                }
            }

            _lastBackupTime.value = System.currentTimeMillis()
            _syncState.value = SyncState.Success("All $count document(s) synchronized to cloud", count)
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup all documents", e)
            _syncState.value = SyncState.Error("Backup failed: ${e.localizedMessage ?: "Unknown error"}")
            Result.failure(e)
        }
    }

    /**
     * Pulls and synchronizes documents and annotations from Firebase Firestore into the local Room database.
     */
    suspend fun restoreAndSyncFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        val user = auth?.currentUser
        val currentFirestore = firestore
        if (user == null || currentFirestore == null) {
            return@withContext Result.failure(IllegalStateException("User must be signed in to restore documents from cloud."))
        }

        try {
            _syncState.value = SyncState.Syncing

            val snapshot = currentFirestore.collection("users")
                .document(user.uid)
                .collection("documents")
                .get()
                .await()

            var restoredCount = 0
            val userPdfsDir = File(context.filesDir, "user_pdfs").apply { mkdirs() }

            for (docSnapshot in snapshot.documents) {
                val title = docSnapshot.getString("title") ?: "Cloud_Doc"
                val pageCount = docSnapshot.getLong("pageCount")?.toInt() ?: 1
                val fileSize = docSnapshot.getLong("fileSize") ?: 0L
                val isFav = docSnapshot.getBoolean("isFavorite") ?: false
                val isEnc = docSnapshot.getBoolean("isEncrypted") ?: false
                val tags = docSnapshot.getString("tags") ?: "CloudSync"
                val ocr = docSnapshot.getString("ocrExtractedText") ?: ""
                val created = docSnapshot.getLong("createdTimestamp") ?: System.currentTimeMillis()
                val lastOpened = docSnapshot.getLong("lastOpenedTimestamp") ?: System.currentTimeMillis()
                val base64Data = docSnapshot.getString("pdfBase64Data")

                // Check if file exists locally or reconstruct from base64
                val localFile = File(userPdfsDir, "${title.replace("[^a-zA-Z0-9_ -]".toRegex(), "_")}.pdf")
                if (!localFile.exists() && !base64Data.isNullOrBlank()) {
                    try {
                        val bytes = Base64.decode(base64Data, Base64.NO_WRAP)
                        FileOutputStream(localFile).use { fos ->
                            fos.write(bytes)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val existingDoc = database.documentDao().getDocumentByPath(localFile.absolutePath)
                val targetDocId: Long = if (existingDoc == null) {
                    val newDoc = DocumentEntity(
                        title = title,
                        filePath = localFile.absolutePath,
                        pageCount = pageCount,
                        fileSize = if (localFile.exists()) localFile.length() else fileSize,
                        createdTimestamp = created,
                        lastOpenedTimestamp = lastOpened,
                        isFavorite = isFav,
                        isEncrypted = isEnc,
                        tags = tags,
                        ocrExtractedText = ocr
                    )
                    database.documentDao().insertDocument(newDoc)
                } else {
                    existingDoc.id
                }

                // Restore annotations
                @Suppress("UNCHECKED_CAST")
                val annotationsList = docSnapshot.get("annotations") as? List<Map<String, Any>> ?: emptyList()
                for (annMap in annotationsList) {
                    try {
                        val pIndex = (annMap["pageIndex"] as? Number)?.toInt() ?: 0
                        val typeStr = annMap["type"] as? String ?: AnnotationType.HIGHLIGHT.name
                        val colorHex = annMap["colorHex"] as? String ?: "#FFFFEB3B"
                        val strokeW = (annMap["strokeWidth"] as? Number)?.toFloat() ?: 4f
                        val opac = (annMap["opacity"] as? Number)?.toFloat() ?: 0.5f
                        val pts = annMap["pointsJson"] as? String ?: ""
                        val text = annMap["textContent"] as? String ?: ""
                        val rect = annMap["rectBoundsJson"] as? String ?: ""
                        val extra = annMap["extraData"] as? String ?: ""

                        val ann = AnnotationEntity(
                            documentId = targetDocId,
                            pageIndex = pIndex,
                            type = runCatching { AnnotationType.valueOf(typeStr) }.getOrDefault(AnnotationType.HIGHLIGHT),
                            colorHex = colorHex,
                            strokeWidth = strokeW,
                            opacity = opac,
                            pointsJson = pts,
                            textContent = text,
                            rectBoundsJson = rect,
                            extraData = extra
                        )
                        database.annotationDao().insertAnnotation(ann)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                restoredCount++
            }

            _syncState.value = SyncState.Success("Successfully restored $restoredCount documents from cloud", restoredCount)
            Result.success(restoredCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore from cloud", e)
            _syncState.value = SyncState.Error("Restore failed: ${e.localizedMessage ?: "Unknown error"}")
            Result.failure(e)
        }
    }

    /**
     * Real-time flow of cloud-backed documents for the signed in user.
     */
    fun getCloudDocumentsFlow(): Flow<List<CloudDocument>> = callbackFlow {
        val user = auth?.currentUser
        val currentFirestore = firestore
        if (user == null || currentFirestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = currentFirestore.collection("users")
            .document(user.uid)
            .collection("documents")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    CloudDocument(
                        id = doc.id,
                        title = doc.getString("title") ?: "Cloud Doc",
                        pageCount = doc.getLong("pageCount")?.toInt() ?: 1,
                        fileSize = doc.getLong("fileSize") ?: 0L,
                        isFavorite = doc.getBoolean("isFavorite") ?: false,
                        isEncrypted = doc.getBoolean("isEncrypted") ?: false,
                        tags = doc.getString("tags") ?: "General",
                        ocrExtractedText = doc.getString("ocrExtractedText") ?: "",
                        createdTimestamp = doc.getLong("createdTimestamp") ?: 0L,
                        lastOpenedTimestamp = doc.getLong("lastOpenedTimestamp") ?: 0L,
                        lastSyncedTimestamp = doc.getLong("lastSyncedTimestamp") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()

                trySend(list)
            }

        awaitClose {
            registration.remove()
        }
    }

    private fun getFallbackClientId(): String {
        // Standard client ID placeholder or app package id representation
        return context.packageName
    }

    companion object {
        private const val TAG = "CloudStorageService"
    }
}
