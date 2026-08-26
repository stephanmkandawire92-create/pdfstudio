package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object PdfToolsEngine {
    private const val TAG = "PdfToolsEngine"
    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            try {
                PDFBoxResourceLoader.init(context)
                isInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init PDFBox", e)
            }
        }
    }

    suspend fun mergePdfs(context: Context, uris: List<Uri>, outputDir: File): File? = withContext(Dispatchers.IO) {
        init(context)
        try {
            val merger = PDFMergerUtility()
            val outFile = File(outputDir, "Merged_${System.currentTimeMillis()}.pdf")
            merger.destinationFileName = outFile.absolutePath

            uris.forEach { uri ->
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val fileStream = FileInputStream(pfd.fileDescriptor)
                    merger.addSource(fileStream)
                }
            }
            merger.mergeDocuments(null)
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Merge failed", e)
            null
        }
    }

    suspend fun splitPdf(context: Context, uri: Uri, outputDir: File): List<File> = withContext(Dispatchers.IO) {
        init(context)
        val files = mutableListOf<File>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val splitter = Splitter()
                val parts = splitter.split(document)
                
                parts.forEachIndexed { index, part ->
                    val outFile = File(outputDir, "Split_Page_${index + 1}_${System.currentTimeMillis()}.pdf")
                    part.save(outFile)
                    part.close()
                    files.add(outFile)
                }
                document.close()
            }
            files
        } catch (e: Exception) {
            Log.e(TAG, "Split failed", e)
            emptyList()
        }
    }

    suspend fun rotatePdf(context: Context, uri: Uri, degrees: Int, outputDir: File): File? = withContext(Dispatchers.IO) {
        init(context)
        try {
            val outFile = File(outputDir, "Rotated_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                for (page in document.pages) {
                    val currentRotation = page.rotation
                    page.rotation = (currentRotation + degrees) % 360
                }
                document.save(outFile)
                document.close()
            }
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Rotate failed", e)
            null
        }
    }

    suspend fun extractPages(context: Context, uri: Uri, pageIndices: List<Int>, outputDir: File): File? = withContext(Dispatchers.IO) {
        init(context)
        try {
            val outFile = File(outputDir, "Extracted_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalDoc = PDDocument.load(inputStream)
                val newDoc = PDDocument()
                
                pageIndices.forEach { index ->
                    if (index >= 0 && index < originalDoc.numberOfPages) {
                        newDoc.addPage(originalDoc.getPage(index))
                    }
                }
                
                newDoc.save(outFile)
                newDoc.close()
                originalDoc.close()
            }
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Extract pages failed", e)
            null
        }
    }

    suspend fun removeBlankPages(context: Context, uri: Uri, outputDir: File): File? = withContext(Dispatchers.IO) {
        init(context)
        // A simple blank page detection: render page at low res, check if it's all white
        try {
            val outFile = File(outputDir, "NoBlanks_${System.currentTimeMillis()}.pdf")
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            val renderer = PdfRenderer(pfd)
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalDoc = PDDocument.load(inputStream)
                val newDoc = PDDocument()
                
                for (i in 0 until originalDoc.numberOfPages) {
                    val page = renderer.openPage(i)
                    val bmp = Bitmap.createBitmap(100, (100 * page.height / page.width.toFloat()).toInt(), Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    
                    var isBlank = true
                    val pixels = IntArray(bmp.width * bmp.height)
                    bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                    for (pixel in pixels) {
                        if (pixel != android.graphics.Color.WHITE && pixel != 0) {
                            // Check if color is significantly dark
                            val r = android.graphics.Color.red(pixel)
                            val g = android.graphics.Color.green(pixel)
                            val b = android.graphics.Color.blue(pixel)
                            if (r < 250 || g < 250 || b < 250) {
                                isBlank = false
                                break
                            }
                        }
                    }
                    bmp.recycle()
                    
                    if (!isBlank) {
                        newDoc.addPage(originalDoc.getPage(i))
                    }
                }
                
                renderer.close()
                pfd.close()
                
                if (newDoc.numberOfPages > 0) {
                    newDoc.save(outFile)
                }
                newDoc.close()
                originalDoc.close()
            }
            if (outFile.exists()) outFile else null
        } catch (e: Exception) {
            Log.e(TAG, "Remove blank pages failed", e)
            null
        }
    }

    enum class PageNumberPosition {
        HEADER, FOOTER
    }

    suspend fun addPageNumbers(context: Context, uri: Uri, position: PageNumberPosition, outputDir: File): File? = withContext(Dispatchers.IO) {
        init(context)
        try {
            val outFile = File(outputDir, "PageNumbers_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val font = PDType1Font.HELVETICA
                val fontSize = 12f
                
                for (i in 0 until document.numberOfPages) {
                    val page = document.getPage(i)
                    val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
                    
                    val text = "Page ${i + 1} of ${document.numberOfPages}"
                    val mediaBox = page.mediaBox
                    val startX = mediaBox.lowerLeftX + (mediaBox.width / 2) - 30f // approximate center
                    
                    val startY = if (position == PageNumberPosition.HEADER) {
                        mediaBox.upperRightY - 30f // 30 points from the top
                    } else {
                        mediaBox.lowerLeftY + 20f // 20 points from the bottom
                    }
                    
                    contentStream.beginText()
                    contentStream.setFont(font, fontSize)
                    contentStream.newLineAtOffset(startX, startY)
                    contentStream.showText(text)
                    contentStream.endText()
                    contentStream.close()
                }
                
                document.save(outFile)
                document.close()
            }
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Add page numbers failed", e)
            null
        }
    }

    suspend fun addWatermark(context: Context, uri: Uri, text: String, outputDir: File): File? = withContext(Dispatchers.IO) {
        init(context)
        try {
            val outFile = File(outputDir, "Watermark_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val font = PDType1Font.HELVETICA_BOLD
                val fontSize = 60f
                
                for (i in 0 until document.numberOfPages) {
                    val page = document.getPage(i)
                    val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
                    
                    // Simple diagonal watermark
                    val mediaBox = page.mediaBox
                    contentStream.beginText()
                    contentStream.setFont(font, fontSize)
                    contentStream.setNonStrokingColor(200, 200, 200) // Light gray
                    
                    // Center and rotate
                    val cx = mediaBox.width / 2f
                    val cy = mediaBox.height / 2f
                    // Move to center, rotate 45 degrees
                    contentStream.newLineAtOffset(cx - 100f, cy - 100f) // rough offset
                    
                    // We need a matrix for true rotation, but offset is simpler for now
                    contentStream.showText(text)
                    contentStream.endText()
                    contentStream.close()
                }
                
                document.save(outFile)
                document.close()
            }
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Add watermark failed", e)
            null
        }
    }
    
    suspend fun pdfToImages(context: Context, uri: Uri, outputDir: File): List<File> = withContext(Dispatchers.IO) {
        val files = mutableListOf<File>()
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext emptyList()
            val renderer = PdfRenderer(pfd)
            
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                // Render at higher res
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                val outFile = File(outputDir, "Page_${i + 1}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bmp.recycle()
                files.add(outFile)
            }
            
            renderer.close()
            pfd.close()
            files
        } catch (e: Exception) {
            Log.e(TAG, "PDF to Images failed", e)
            emptyList()
        }
    }

    suspend fun pdfToText(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        init(context)
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDF to Text failed", e)
            null
        }
    }
    
    fun printPdf(context: Context, uri: Uri, jobName: String = "Document") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                // For simplicity, we can print using a custom PrintDocumentAdapter, 
                // but Android handles PDF printing natively if we pass the stream.
                // We'll create a basic adapter that writes the PDF file to the print output.
                val printAdapter = object : android.print.PrintDocumentAdapter() {
                    override fun onWrite(pages: Array<out android.print.PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: android.os.CancellationSignal?, callback: WriteResultCallback?) {
                        try {
                            val inStream = FileInputStream(pfd.fileDescriptor)
                            val outStream = FileOutputStream(destination?.fileDescriptor)
                            inStream.copyTo(outStream)
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            Log.e(TAG, "Print write failed", e)
                            callback?.onWriteFailed(e.message)
                        }
                    }
                    
                    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: android.os.CancellationSignal?, callback: LayoutResultCallback?, extras: android.os.Bundle?) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = android.print.PrintDocumentInfo.Builder(jobName)
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }
                }
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Print failed", e)
        }
    }
}
