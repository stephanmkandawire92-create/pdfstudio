package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.File
import java.io.FileOutputStream

/** Production-grade PDF operations that require user-selected parameters. */
object PdfToolsAdvancedEngine {
    private var initialized = false

    private fun init(context: Context) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context)
            initialized = true
        }
    }

    suspend fun extractPages(context: Context, uri: Uri, pageNumbers: List<Int>, outputDir: File): File? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            init(context)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    PDDocument.load(input).use { source ->
                        PDDocument().use { output ->
                            pageNumbers.distinct().sorted().forEach { oneBased ->
                                val index = oneBased - 1
                                if (index in 0 until source.numberOfPages) {
                                    output.importPage(source.getPage(index))
                                }
                            }
                            if (output.numberOfPages == 0) return@withContext null
                            val file = File(outputDir, "Extracted_${System.currentTimeMillis()}.pdf")
                            output.save(file)
                            file
                        }
                    }
                }
            } catch (_: Exception) { null }
        }

    suspend fun protectPdf(context: Context, uri: Uri, userPassword: String, ownerPassword: String, outputDir: File): File? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            init(context)
            if (userPassword.isBlank() || ownerPassword.isBlank()) return@withContext null
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    PDDocument.load(input).use { document ->
                        val permissions = AccessPermission()
                        permissions.setCanPrint(true)
                        permissions.setCanExtractContent(false)
                        permissions.setCanModify(false)
                        val policy = StandardProtectionPolicy(ownerPassword, userPassword, permissions).apply {
                            encryptionKeyLength = 256
                            this.permissions = permissions
                        }
                        document.protect(policy)
                        val file = File(outputDir, "Protected_${System.currentTimeMillis()}.pdf")
                        document.save(file)
                        file
                    }
                }
            } catch (_: Exception) { null }
        }

    suspend fun unlockPdf(context: Context, uri: Uri, password: String, outputDir: File): File? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            init(context)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    PDDocument.load(input, password).use { document ->
                        if (!document.isEncrypted) return@withContext null
                        document.setAllSecurityToBeRemoved(true)
                        val file = File(outputDir, "Unlocked_${System.currentTimeMillis()}.pdf")
                        document.save(file)
                        file
                    }
                }
            } catch (_: Exception) { null }
        }

    suspend fun resizePages(context: Context, uri: Uri, size: String, outputDir: File): File? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            init(context)
            val rectangle = when (size.uppercase()) {
                "A4" -> PDRectangle.A4
                "A5" -> PDRectangle.A5
                "LETTER" -> PDRectangle.LETTER
                "LEGAL" -> PDRectangle.LEGAL
                else -> return@withContext null
            }
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    PDDocument.load(input).use { document ->
                        for (page in document.pages) {
                            val landscape = page.mediaBox.width > page.mediaBox.height
                            page.mediaBox = if (landscape) PDRectangle(rectangle.height, rectangle.width) else rectangle
                            page.cropBox = page.mediaBox
                        }
                        val file = File(outputDir, "Resized_${size.uppercase()}_${System.currentTimeMillis()}.pdf")
                        document.save(file)
                        file
                    }
                }
            } catch (_: Exception) { null }
        }

    suspend fun imagesToPdf(context: Context, uris: List<Uri>, outputDir: File): File? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bitmaps = uris.mapNotNull { uri ->
                    context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
                }
                if (bitmaps.isEmpty()) return@withContext null
                val file = File(outputDir, "Images_${System.currentTimeMillis()}.pdf")
                val ok = PdfEngine.createPdfFromBitmaps(bitmaps, file)
                bitmaps.forEach { if (!it.isRecycled) it.recycle() }
                if (ok) file else null
            } catch (_: Exception) { null }
        }

    suspend fun renderPdfToImages(context: Context, uri: Uri, outputDir: File, quality: Int = 92): List<File> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val result = mutableListOf<File>()
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext emptyList()
                renderer = PdfRenderer(pfd)
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val file = File(outputDir, "PDF_Page_${i + 1}_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
                        bitmap.recycle()
                        result += file
                    }
                }
                result
            } catch (_: Exception) {
                result.forEach { it.delete() }
                emptyList()
            } finally {
                renderer?.close()
                pfd?.close()
            }
        }
}
