package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AnnotationEntity
import com.example.data.AnnotationType
import com.example.data.AppDatabase
import com.example.data.DocumentEntity
import com.example.data.DocumentRepository
import com.example.data.FormFieldEntity
import com.example.data.SignatureEntity
import com.example.engine.CloudDocument
import com.example.engine.CloudStorageService
import com.example.engine.PdfCompressionEngine
import com.example.engine.PdfCompressionEngine.CompressionConfig
import com.example.engine.PdfCompressionEngine.CompressionReport
import com.example.engine.PdfEngine
import com.example.engine.ScanFilter
import com.example.engine.ScannerEngine
import com.example.engine.SyncState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class HomeTab {
    ALL, RECENTS, STARRED, SCANS, VAULT
}

enum class ViewerMode {
    READING, ANNOTATING, FORM_FILLING, PAGE_MANAGEMENT
}

data class ActivePdfState(
    val document: DocumentEntity? = null,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 1,
    val currentBitmap: Bitmap? = null,
    val isLoadingPage: Boolean = false,
    val annotations: List<AnnotationEntity> = emptyList(),
    val formFields: List<FormFieldEntity> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Int> = emptyList(),
    val currentSearchIndex: Int = 0,
    val ocrText: String = "",
    val isOcrLoading: Boolean = false,
    val isNightFilter: Boolean = false
)

class PdfAppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "pdf_studio.db"
    ).build()

    val repository = DocumentRepository(db, application)
    val cloudStorageService = CloudStorageService(application, db)

    // Cloud Authentication & Sync State
    val currentUser: StateFlow<FirebaseUser?> = cloudStorageService.currentUser
    val syncState: StateFlow<SyncState> = cloudStorageService.syncState
    val lastBackupTime: StateFlow<Long?> = cloudStorageService.lastBackupTime
    val cloudDocuments: StateFlow<List<CloudDocument>> = cloudStorageService.getCloudDocumentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    val selectedTab = MutableStateFlow(HomeTab.ALL)
    val searchQuery = MutableStateFlow("")
    val isGridView = MutableStateFlow(false)
    val isAppDarkMode = MutableStateFlow(false)

    private val prefs = application.getSharedPreferences("pdf_studio_prefs", android.content.Context.MODE_PRIVATE)
    val isAutoBackupEnabled = MutableStateFlow(prefs.getBoolean("auto_backup_enabled", false))

    fun toggleAutoBackup() {
        val newValue = !isAutoBackupEnabled.value
        isAutoBackupEnabled.value = newValue

        if (newValue && currentUser.value != null) {
            backupAllDocumentsToCloud()
        }
        prefs.edit().putBoolean("auto_backup_enabled", newValue).apply()
    }

    val externalUriToOpen = MutableStateFlow<Uri?>(null)

    fun setExternalUri(uri: Uri?) {
        externalUriToOpen.value = uri
    }

    // Active document viewer state
    private val _viewerState = MutableStateFlow(ActivePdfState())
    val viewerState: StateFlow<ActivePdfState> = _viewerState.asStateFlow()

    val currentViewerMode = MutableStateFlow(ViewerMode.READING)

    // Scanner multi-page buffer
    val scannedBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val currentScanFilter = MutableStateFlow(ScanFilter.MAGIC_COLOR)

    // Signatures
    val signatures: StateFlow<List<SignatureEntity>> = repository.allSignatures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered documents
    val documents: StateFlow<List<DocumentEntity>> = combine(
        repository.allDocuments,
        selectedTab,
        searchQuery
    ) { allDocs, tab, query ->
        var list = when (tab) {
            HomeTab.ALL -> allDocs
            HomeTab.RECENTS -> allDocs.sortedByDescending { it.lastOpenedTimestamp }
            HomeTab.STARRED -> allDocs.filter { it.isFavorite }
            HomeTab.SCANS -> allDocs.filter { it.tags.contains("Scan", ignoreCase = true) }
            HomeTab.VAULT -> allDocs.filter { it.isEncrypted }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true) ||
                it.ocrExtractedText.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedSampleDocumentsIfEmpty()
        }

        viewModelScope.launch {
            repository.allDocuments.collectLatest { 
                kotlinx.coroutines.delay(3000)
                if (isAutoBackupEnabled.value && currentUser.value != null) {
                    backupAllDocumentsToCloud()
                }
            }
        }
    }

    fun setTab(tab: HomeTab) {
        selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleGridView() {
        isGridView.value = !isGridView.value
    }

    fun toggleDarkMode() {
        isAppDarkMode.value = !isAppDarkMode.value
    }

    fun toggleFavorite(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(doc.id, !doc.isFavorite)
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            Toast.makeText(getApplication(), "Document deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun renameDocument(doc: DocumentEntity, newTitle: String) {
        viewModelScope.launch {
            repository.updateDocument(doc.copy(title = newTitle))
            Toast.makeText(getApplication(), "Renamed to $newTitle", Toast.LENGTH_SHORT).show()
        }
    }

    // Open Document
    fun openDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.updateLastOpened(doc.id)
            val file = File(doc.filePath)
            val count = PdfEngine.getPageCount(file).coerceAtLeast(1)
            _viewerState.value = ActivePdfState(
                document = doc,
                currentPageIndex = 0,
                totalPages = count
            )
            loadPage(doc, 0)
            loadAnnotations(doc.id, 0)
        }
    }

    fun closeDocument() {
        _viewerState.value.currentBitmap?.recycle()
        _viewerState.value = ActivePdfState()
    }

    fun loadPage(doc: DocumentEntity, pageIndex: Int) {
        viewModelScope.launch {
            _viewerState.value = _viewerState.value.copy(isLoadingPage = true, currentPageIndex = pageIndex)
            val file = File(doc.filePath)
            val bmp = PdfEngine.renderPage(file, pageIndex, renderScale = 2.0f)
            _viewerState.value = _viewerState.value.copy(
                currentPageIndex = pageIndex,
                currentBitmap = bmp,
                isLoadingPage = false
            )
            loadAnnotations(doc.id, pageIndex)
        }
    }

    fun goToNextPage() {
        val st = _viewerState.value
        val doc = st.document ?: return
        if (st.currentPageIndex < st.totalPages - 1) {
            loadPage(doc, st.currentPageIndex + 1)
        }
    }

    fun goToPrevPage() {
        val st = _viewerState.value
        val doc = st.document ?: return
        if (st.currentPageIndex > 0) {
            loadPage(doc, st.currentPageIndex - 1)
        }
    }

    fun jumpToPage(pageIndex: Int) {
        val st = _viewerState.value
        val doc = st.document ?: return
        val clamped = pageIndex.coerceIn(0, (st.totalPages - 1).coerceAtLeast(0))
        loadPage(doc, clamped)
    }

    fun toggleNightFilter() {
        _viewerState.value = _viewerState.value.copy(isNightFilter = !_viewerState.value.isNightFilter)
    }

    // Annotations
    private fun loadAnnotations(docId: Long, pageIndex: Int) {
        viewModelScope.launch {
            repository.getAnnotationsForPage(docId, pageIndex).collect { list ->
                _viewerState.value = _viewerState.value.copy(annotations = list)
            }
        }
    }

    fun addAnnotation(ann: AnnotationEntity) {
        viewModelScope.launch {
            repository.saveAnnotation(ann)
        }
    }

    fun deleteAnnotation(annId: Long) {
        viewModelScope.launch {
            repository.deleteAnnotation(annId)
        }
    }

    fun clearAnnotationsOnCurrentPage() {
        viewModelScope.launch {
            val doc = _viewerState.value.document ?: return@launch
            val list = _viewerState.value.annotations
            for (a in list) {
                repository.deleteAnnotation(a.id)
            }
            Toast.makeText(getApplication(), "Page annotations cleared", Toast.LENGTH_SHORT).show()
        }
    }

    // OCR on current page
    fun runOcrOnCurrentPage() {
        val bmp = _viewerState.value.currentBitmap ?: return
        viewModelScope.launch {
            _viewerState.value = _viewerState.value.copy(isOcrLoading = true)
            val extracted = PdfEngine.performOcr(bmp)
            _viewerState.value = _viewerState.value.copy(ocrText = extracted, isOcrLoading = false)
        }
    }

    // Signatures
    fun saveSignature(title: String, points: String, color: String = "#0F172A", width: Float = 4f) {
        viewModelScope.launch {
            repository.saveSignature(title, points, color, width)
            Toast.makeText(getApplication(), "Signature saved to vault", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteSignature(sig: SignatureEntity) {
        viewModelScope.launch {
            repository.deleteSignature(sig)
        }
    }

    // Document Security
    fun lockDocumentWithPassword(doc: DocumentEntity, pass: String) {
        viewModelScope.launch {
            val hash = PdfEngine.hashPassword(pass)
            repository.updateDocument(doc.copy(isEncrypted = true, passwordHash = hash))
            Toast.makeText(getApplication(), "Document locked with password protection", Toast.LENGTH_SHORT).show()
        }
    }

    fun unlockDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.updateDocument(doc.copy(isEncrypted = false, passwordHash = null))
            Toast.makeText(getApplication(), "Document unlocked", Toast.LENGTH_SHORT).show()
        }
    }

    fun verifyPassword(doc: DocumentEntity, pass: String): Boolean {
        val hash = PdfEngine.hashPassword(pass)
        return hash == doc.passwordHash
    }

    // Import from URI
    fun importPdf(uri: Uri, displayName: String?, onComplete: ((DocumentEntity?) -> Unit)? = null) {
        viewModelScope.launch {
            val doc = repository.importPdfFromUri(uri, displayName)
            if (doc != null) {
                Toast.makeText(getApplication(), "Imported: ${doc.title}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "Failed to import PDF", Toast.LENGTH_SHORT).show()
            }
            onComplete?.invoke(doc)
        }
    }

    // Export Flattened PDF with annotations
    fun exportAnnotatedPdf(watermarkText: String? = null, onResult: (File?) -> Unit) {
        val doc = _viewerState.value.document ?: return
        viewModelScope.launch {
            val allAnns = repository.getAllAnnotationsForDoc(doc.id)
            val exportsDir = File(getApplication<Application>().filesDir, "exports")
            exportsDir.mkdirs()
            val outFile = File(exportsDir, "${doc.title}_annotated_${System.currentTimeMillis()}.pdf")
            val success = PdfEngine.exportAnnotatedPdf(File(doc.filePath), allAnns, outFile, watermarkText)
            if (success) {
                // Also add to documents repository
                val count = PdfEngine.getPageCount(outFile)
                val newDoc = DocumentEntity(
                    title = outFile.nameWithoutExtension,
                    filePath = outFile.absolutePath,
                    pageCount = count,
                    fileSize = outFile.length(),
                    tags = "Annotated, Export"
                )
                db.documentDao().insertDocument(newDoc)
                onResult(outFile)
            } else {
                onResult(null)
            }
        }
    }

    // Page Management operations
    fun modifyPages(pageSpecs: List<Pair<Int, Int>>, onComplete: (Boolean) -> Unit) {
        val doc = _viewerState.value.document ?: return
        viewModelScope.launch {
            val origFile = File(doc.filePath)
            val newFile = File(origFile.parentFile, "${doc.title}_modified_${System.currentTimeMillis()}.pdf")
            val success = PdfEngine.modifyPages(origFile, pageSpecs, newFile)
            if (success) {
                val newCount = PdfEngine.getPageCount(newFile)
                val updated = doc.copy(filePath = newFile.absolutePath, pageCount = newCount, fileSize = newFile.length())
                repository.updateDocument(updated)
                _viewerState.value = _viewerState.value.copy(document = updated, totalPages = newCount)
                loadPage(updated, 0)
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    // Scanner actions
    fun addScannedBitmap(bmp: Bitmap) {
        viewModelScope.launch {
            val filtered = ScannerEngine.applyFilter(bmp, currentScanFilter.value)
            scannedBitmaps.value = scannedBitmaps.value + filtered
        }
    }

    fun removeScannedBitmap(index: Int) {
        if (index in scannedBitmaps.value.indices) {
            val list = scannedBitmaps.value.toMutableList()
            list.removeAt(index)
            scannedBitmaps.value = list
        }
    }

    fun clearScannedBitmaps() {
        scannedBitmaps.value = emptyList()
    }

    fun saveScannedPdf(title: String, onComplete: (DocumentEntity?) -> Unit) {
        viewModelScope.launch {
            val bmps = scannedBitmaps.value
            if (bmps.isEmpty()) {
                onComplete(null)
                return@launch
            }
            val doc = repository.createPdfFromBitmaps(title, bmps, "Scanned")
            clearScannedBitmaps()
            onComplete(doc)
        }
    }

    // Cloud Synchronization & Backup Actions
    fun signInWithGoogle(webClientId: String = "", onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = cloudStorageService.signInWithGoogle(webClientId)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    fun signOutFromCloud() {
        cloudStorageService.signOut()
    }

    fun backupSingleDocumentToCloud(doc: DocumentEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = cloudStorageService.backupDocument(doc)
            onComplete?.invoke(res.isSuccess)
        }
    }

    fun backupAllDocumentsToCloud(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val res = cloudStorageService.backupAllDocuments()
            onComplete?.invoke(res.getOrDefault(0))
        }
    }

    fun restoreAndSyncFromCloud(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val res = cloudStorageService.restoreAndSyncFromCloud()
            onComplete?.invoke(res.getOrDefault(0))
        }
    }

    // PDF Compression Utilities
    fun compressDocument(
        doc: DocumentEntity,
        config: CompressionConfig,
        onProgress: ((Int, Int) -> Unit)? = null,
        onComplete: (CompressionReport, DocumentEntity?) -> Unit
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val origFile = File(doc.filePath)
            val outDir = File(app.filesDir, "user_pdfs")
            outDir.mkdirs()
            val cleanTitle = "${doc.title.removeSuffix(".pdf")}_compressed_${System.currentTimeMillis() % 10000}"
            val outFile = File(outDir, "$cleanTitle.pdf")

            val report = PdfCompressionEngine.compressPdfDocument(
                context = app,
                inputFile = origFile,
                outputFile = outFile,
                config = config,
                onProgress = onProgress
            )

            var createdEntity: DocumentEntity? = null
            if (report.isSuccess && report.compressedFile != null) {
                createdEntity = DocumentEntity(
                    title = report.compressedFile.nameWithoutExtension,
                    filePath = report.compressedFile.absolutePath,
                    pageCount = report.pageCount,
                    fileSize = report.compressedSizeBytes,
                    tags = "Compressed"
                )
                repository.updateDocument(createdEntity)
            }

            onComplete(report, createdEntity)
        }
    }
}
