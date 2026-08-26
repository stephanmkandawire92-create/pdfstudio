package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min

/**
 * PDF Compression Utility.
 * Reduces document file size by optimizing embedded images, downsampling resolution,
 * re-encoding image streams with customizable compression levels, and stripping redundant
 * metadata, XMP packets, and unused dictionary bloat.
 */
object PdfCompressionEngine {

    private const val TAG = "PdfCompressor"

    enum class CompressionPreset(
        val title: String,
        val subtitle: String,
        val defaultQuality: Int,
        val scaleFactor: Float,
        val estimatedSavingsPercent: Int
    ) {
        EXTREME(
            title = "Extreme Compression",
            subtitle = "Lowest file size, downscaled resolution (~100 DPI)",
            defaultQuality = 35,
            scaleFactor = 0.65f,
            estimatedSavingsPercent = 65
        ),
        RECOMMENDED(
            title = "Recommended",
            subtitle = "Good quality, balanced compression (~150 DPI)",
            defaultQuality = 60,
            scaleFactor = 0.85f,
            estimatedSavingsPercent = 45
        ),
        HIGH_QUALITY(
            title = "High Quality",
            subtitle = "High clarity, minimal compression (~200 DPI)",
            defaultQuality = 80,
            scaleFactor = 1.0f,
            estimatedSavingsPercent = 25
        ),
        CUSTOM(
            title = "Custom",
            subtitle = "Fine-tune quality, DPI scale, grayscale, and metadata",
            defaultQuality = 60,
            scaleFactor = 0.85f,
            estimatedSavingsPercent = 40
        )
    }

    data class CompressionConfig(
        val preset: CompressionPreset = CompressionPreset.RECOMMENDED,
        val qualityPercent: Int = 60,
        val scaleFactor: Float = 0.85f,
        val convertToGrayscale: Boolean = false,
        val stripMetadata: Boolean = true,
        val removeAlphaChannel: Boolean = true
    )

    data class CompressionReport(
        val isSuccess: Boolean,
        val originalFile: File,
        val compressedFile: File?,
        val originalSizeBytes: Long,
        val compressedSizeBytes: Long,
        val savedBytes: Long,
        val reductionPercentage: Float,
        val pageCount: Int,
        val durationMs: Long,
        val errorMessage: String? = null
    )

    /**
     * Estimates the compressed file size and percentage reduction for UI preview.
     */
    fun estimateSavings(fileSize: Long, config: CompressionConfig): Pair<Long, Int> {
        val basePercent = when (config.preset) {
            CompressionPreset.EXTREME -> 65
            CompressionPreset.RECOMMENDED -> 45
            CompressionPreset.HIGH_QUALITY -> 25
            CompressionPreset.CUSTOM -> {
                val qRatio = (100 - config.qualityPercent).coerceIn(10, 85)
                val sRatio = ((1.0f - config.scaleFactor.coerceIn(0.4f, 1.2f)) * 40).toInt()
                val grayBonus = if (config.convertToGrayscale) 15 else 0
                val metaBonus = if (config.stripMetadata) 5 else 0
                (qRatio * 0.5f + sRatio + grayBonus + metaBonus).toInt().coerceIn(10, 85)
            }
        }
        val estimatedSize = max(1024L, (fileSize * (100 - basePercent) / 100))
        return Pair(estimatedSize, basePercent)
    }

    /**
     * Executes PDF compression by optimizing raster images, downscaling DPI,
     * re-encoding streams, and stripping redundant metadata.
     */
    suspend fun compressPdfDocument(
        context: Context,
        inputFile: File,
        outputFile: File,
        config: CompressionConfig,
        onProgress: ((currentPage: Int, totalPages: Int) -> Unit)? = null
    ): CompressionReport = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val originalSize = inputFile.length()

        if (!inputFile.exists() || originalSize == 0L) {
            return@withContext CompressionReport(
                isSuccess = false,
                originalFile = inputFile,
                compressedFile = null,
                originalSizeBytes = 0L,
                compressedSizeBytes = 0L,
                savedBytes = 0L,
                reductionPercentage = 0f,
                pageCount = 0,
                durationMs = 0L,
                errorMessage = "Source PDF file does not exist or is empty."
            )
        }

        try {
            outputFile.parentFile?.mkdirs()
            val pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            val pdfDoc = PdfDocument()

            val quality = config.qualityPercent.coerceIn(15, 95)
            val scale = config.scaleFactor.coerceIn(0.4f, 1.5f)

            // Paint for grayscale if requested
            val grayPaint = if (config.convertToGrayscale) {
                Paint().apply {
                    val matrix = ColorMatrix()
                    matrix.setSaturation(0f)
                    colorFilter = ColorMatrixColorFilter(matrix)
                    isFilterBitmap = true
                }
            } else {
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            }

            for (p in 0 until totalPages) {
                onProgress?.invoke(p + 1, totalPages)
                val page = renderer.openPage(p)

                val origW = page.width
                val origH = page.height
                val renderW = (origW * scale).toInt().coerceAtLeast(100)
                val renderH = (origH * scale).toInt().coerceAtLeast(100)

                // Render page to bitmap with white canvas
                val pageBitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                pageBitmap.eraseColor(Color.WHITE)
                page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Optional Grayscale / Color Optimization
                val processedBitmap = if (config.convertToGrayscale) {
                    val grayBmp = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                    val grayCanvas = Canvas(grayBmp)
                    grayCanvas.drawBitmap(pageBitmap, 0f, 0f, grayPaint)
                    pageBitmap.recycle()
                    grayBmp
                } else {
                    pageBitmap
                }

                // Compress image stream via JPEG re-encoding
                val stream = ByteArrayOutputStream()
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                val compressedBytes = stream.toByteArray()
                stream.close()

                // Decode compressed image back to draw into new PDF page
                val optBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                processedBitmap.recycle()

                // Write optimized page to PdfDocument
                val pageInfo = PdfDocument.PageInfo.Builder(origW, origH, p + 1).create()
                val newPage = pdfDoc.startPage(pageInfo)
                val canvas = newPage.canvas

                // Draw fitted
                val destRect = android.graphics.RectF(0f, 0f, origW.toFloat(), origH.toFloat())
                canvas.drawBitmap(optBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                pdfDoc.finishPage(newPage)
                optBitmap.recycle()
            }

            renderer.close()
            pfd.close()

            // Temporary intermediate file
            val tempRawOutput = File(context.cacheDir, "temp_raw_${System.currentTimeMillis()}.pdf")
            FileOutputStream(tempRawOutput).use { outStream ->
                pdfDoc.writeTo(outStream)
            }
            pdfDoc.close()

            // Strip redundant metadata and XMP packets if enabled
            if (config.stripMetadata) {
                stripRedundantMetadata(tempRawOutput, outputFile)
                tempRawOutput.delete()
            } else {
                if (outputFile.exists()) outputFile.delete()
                tempRawOutput.renameTo(outputFile)
            }

            val compressedSize = outputFile.length()
            val savedBytes = max(0L, originalSize - compressedSize)
            val reductionPct = if (originalSize > 0) {
                ((originalSize - compressedSize).toFloat() / originalSize * 100f).coerceAtLeast(0f)
            } else 0f
            val duration = System.currentTimeMillis() - startTime

            Log.i(TAG, "Compression completed: $originalSize -> $compressedSize bytes (-$reductionPct%) in ${duration}ms")

            CompressionReport(
                isSuccess = true,
                originalFile = inputFile,
                compressedFile = outputFile,
                originalSizeBytes = originalSize,
                compressedSizeBytes = compressedSize,
                savedBytes = savedBytes,
                reductionPercentage = reductionPct,
                pageCount = totalPages,
                durationMs = duration
            )
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed", e)
            CompressionReport(
                isSuccess = false,
                originalFile = inputFile,
                compressedFile = null,
                originalSizeBytes = originalSize,
                compressedSizeBytes = 0L,
                savedBytes = 0L,
                reductionPercentage = 0f,
                pageCount = 0,
                durationMs = System.currentTimeMillis() - startTime,
                errorMessage = e.localizedMessage ?: "Unknown compression error"
            )
        }
    }

    /**
     * Strips redundant XMP metadata packets, XML streams, and bloated /Producer /Creator tags
     * from raw PDF byte streams to minimize file overhead.
     */
    private fun stripRedundantMetadata(inputFile: File, outputFile: File) {
        try {
            val bytes = inputFile.readBytes()
            val contentStr = String(bytes, StandardCharsets.ISO_8859_1)

            // Strip XMP packet <?xpacket begin ... </xpacket>
            var sanitized = contentStr
            val xmpStart = sanitized.indexOf("<?xpacket begin")
            if (xmpStart != -1) {
                val xmpEnd = sanitized.indexOf("<?xpacket end", xmpStart)
                if (xmpEnd != -1) {
                    val fullEnd = sanitized.indexOf(">", xmpEnd)
                    if (fullEnd != -1) {
                        sanitized = sanitized.substring(0, xmpStart) + sanitized.substring(fullEnd + 1)
                    }
                }
            }

            // Clean redundant Producer / Creator / Metadata dict keys if present
            sanitized = sanitized.replace(Regex("/Producer\\s*\\([^)]*\\)"), "/Producer (PDF Studio)")
            sanitized = sanitized.replace(Regex("/Creator\\s*\\([^)]*\\)"), "")

            FileOutputStream(outputFile).use { fos ->
                fos.write(sanitized.toByteArray(StandardCharsets.ISO_8859_1))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Metadata stripping fallback to direct copy", e)
            inputFile.copyTo(outputFile, overwrite = true)
        }
    }
}
