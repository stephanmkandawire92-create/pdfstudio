package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

enum class ScanFilter {
    ORIGINAL,
    MAGIC_COLOR,
    BW_DOCUMENT,
    GRAYSCALE,
    SHARPEN_CONTRAST
}

object ScannerEngine {

    /**
     * Applies document scanning filter to improve clarity, remove shadows, and enhance text.
     */
    suspend fun applyFilter(bitmap: Bitmap, filter: ScanFilter): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (filter) {
            ScanFilter.ORIGINAL -> {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            ScanFilter.MAGIC_COLOR -> {
                // Boost contrast & saturation while lifting whites to remove document shadows
                val cm = ColorMatrix().apply {
                    set(
                        floatArrayOf(
                            1.25f, 0f, 0f, 0f, 15f,
                            0f, 1.25f, 0f, 0f, 15f,
                            0f, 0f, 1.25f, 0f, 15f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            ScanFilter.GRAYSCALE -> {
                val cm = ColorMatrix().apply { setSaturation(0f) }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            ScanFilter.SHARPEN_CONTRAST -> {
                val cm = ColorMatrix().apply {
                    set(
                        floatArrayOf(
                            1.5f, 0f, 0f, 0f, -25f,
                            0f, 1.5f, 0f, 0f, -25f,
                            0f, 0f, 1.5f, 0f, -25f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
            ScanFilter.BW_DOCUMENT -> {
                // Clean thresholding for pure crisp black text on white paper
                val grayBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val grayCanvas = Canvas(grayBmp)
                val cm = ColorMatrix().apply { setSaturation(0f) }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                grayCanvas.drawBitmap(bitmap, 0f, 0f, paint)

                val pixels = IntArray(width * height)
                grayBmp.getPixels(pixels, 0, width, 0, 0, width, height)
                grayBmp.recycle()

                // Calculate adaptive threshold
                var totalLum = 0L
                for (i in 0 until (width * height) step 10) {
                    totalLum += Color.red(pixels[i])
                }
                val avgLum = (totalLum / ((width * height) / 10)).toInt()
                val threshold = (avgLum * 0.88f).toInt().coerceIn(80, 200)

                for (i in pixels.indices) {
                    val lum = Color.red(pixels[i])
                    pixels[i] = if (lum > threshold) Color.WHITE else Color.BLACK
                }
                output.setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }
        output
    }

    /**
     * Crops bitmap according to normalized rectangular coordinates (0f..1f).
     */
    suspend fun cropBitmap(
        bitmap: Bitmap,
        leftRatio: Float,
        topRatio: Float,
        rightRatio: Float,
        bottomRatio: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val l = (bitmap.width * leftRatio.coerceIn(0f, 0.9f)).toInt()
        val t = (bitmap.height * topRatio.coerceIn(0f, 0.9f)).toInt()
        val r = (bitmap.width * rightRatio.coerceIn(0.1f, 1.0f)).toInt()
        val b = (bitmap.height * bottomRatio.coerceIn(0.1f, 1.0f)).toInt()

        val cropW = max(50, r - l)
        val cropH = max(50, b - t)
        Bitmap.createBitmap(bitmap, l, t, min(cropW, bitmap.width - l), min(cropH, bitmap.height - t))
    }
}
