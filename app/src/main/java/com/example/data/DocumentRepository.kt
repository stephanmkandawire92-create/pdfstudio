package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.engine.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    val allDocuments: Flow<List<DocumentEntity>> = database.documentDao().getAllDocumentsFlow()
    val favoriteDocuments: Flow<List<DocumentEntity>> = database.documentDao().getFavoriteDocumentsFlow()
    val allSignatures: Flow<List<SignatureEntity>> = database.signatureDao().getAllSignaturesFlow()

    suspend fun getDocument(id: Long): DocumentEntity? = database.documentDao().getDocumentById(id)

    suspend fun updateLastOpened(id: Long) = database.documentDao().updateLastOpened(id)

    suspend fun toggleFavorite(id: Long, isFav: Boolean) = database.documentDao().setFavorite(id, isFav)

    suspend fun updateDocument(doc: DocumentEntity) = database.documentDao().updateDocument(doc)

    suspend fun deleteDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        database.annotationDao().deleteAllAnnotationsForDoc(doc.id)
        database.documentDao().deleteDocument(doc)
        val f = File(doc.filePath)
        if (f.exists() && !doc.isSample) {
            f.delete()
        }
    }

    suspend fun getAnnotationsForPage(docId: Long, pageIndex: Int): Flow<List<AnnotationEntity>> =
        database.annotationDao().getAnnotationsForPageFlow(docId, pageIndex)

    suspend fun getAllAnnotationsForDoc(docId: Long): List<AnnotationEntity> =
        database.annotationDao().getAllAnnotationsForDoc(docId)

    suspend fun saveAnnotation(ann: AnnotationEntity): Long =
        database.annotationDao().insertAnnotation(ann)

    suspend fun deleteAnnotation(id: Long) =
        database.annotationDao().deleteAnnotationById(id)

    suspend fun saveSignature(title: String, points: String, color: String = "#0F172A", strokeWidth: Float = 4f, isDefault: Boolean = false): Long {
        return database.signatureDao().insertSignature(
            SignatureEntity(
                title = title,
                pointsJson = points,
                strokeColorHex = color,
                strokeWidth = strokeWidth,
                isDefault = isDefault
            )
        )
    }

    suspend fun deleteSignature(sig: SignatureEntity) =
        database.signatureDao().deleteSignature(sig)

    suspend fun getFormFieldsForPage(docId: Long, pageIndex: Int): Flow<List<FormFieldEntity>> =
        database.formDao().getFormFieldsForPageFlow(docId, pageIndex)

    suspend fun saveFormField(field: FormFieldEntity): Long =
        database.formDao().insertOrUpdateField(field)

    suspend fun importPdfFromUri(uri: Uri, displayName: String? = null): DocumentEntity? = withContext(Dispatchers.IO) {
        try {
            val docsDir = File(context.filesDir, "user_pdfs")
            docsDir.mkdirs()
            val fileName = (displayName ?: "Imported_${System.currentTimeMillis()}").let {
                if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf"
            }
            val destFile = File(docsDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val pageCount = PdfEngine.getPageCount(destFile).coerceAtLeast(1)
            val doc = DocumentEntity(
                title = destFile.nameWithoutExtension,
                filePath = destFile.absolutePath,
                pageCount = pageCount,
                fileSize = destFile.length(),
                lastOpenedTimestamp = System.currentTimeMillis(),
                tags = "Imported"
            )
            val id = database.documentDao().insertDocument(doc)
            doc.copy(id = id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createPdfFromBitmaps(
        title: String,
        bitmaps: List<Bitmap>,
        tags: String = "Scanned"
    ): DocumentEntity? = withContext(Dispatchers.IO) {
        try {
            val docsDir = File(context.filesDir, "user_pdfs")
            docsDir.mkdirs()
            val safeTitle = title.replace("[^a-zA-Z0-9_ -]".toRegex(), "_")
            val destFile = File(docsDir, "$safeTitle.pdf")

            val success = PdfEngine.createPdfFromBitmaps(bitmaps, destFile)
            if (success) {
                val pageCount = PdfEngine.getPageCount(destFile).coerceAtLeast(1)
                val doc = DocumentEntity(
                    title = safeTitle,
                    filePath = destFile.absolutePath,
                    pageCount = pageCount,
                    fileSize = destFile.length(),
                    tags = tags
                )
                val id = database.documentDao().insertDocument(doc)
                doc.copy(id = id)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun seedSampleDocumentsIfEmpty() = withContext(Dispatchers.IO) {
        try {
            val samples = PdfEngine.generateSampleDocuments(context)
            for ((title, file) in samples) {
                val existing = database.documentDao().getDocumentByPath(file.absolutePath)
                if (existing == null) {
                    val count = PdfEngine.getPageCount(file)
                    val doc = DocumentEntity(
                        title = title,
                        filePath = file.absolutePath,
                        pageCount = count,
                        fileSize = file.length(),
                        isSample = true,
                        tags = if (title.contains("Guide")) "Guide, Showcase" else if (title.contains("NDA")) "Legal, Form" else "Finance, Template"
                    )
                    database.documentDao().insertDocument(doc)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
