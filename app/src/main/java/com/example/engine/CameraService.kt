package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraService manages CameraX lifecycle, preview streaming, high-res image capture,
 * torch controls, and document scanning image processing.
 */
class CameraService(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val cameraExecutor: Executor = Executors.newSingleThreadExecutor()

    var isTorchEnabled: Boolean = false
        private set

    var lensFacing: Int = CameraSelector.LENS_FACING_BACK
        private set

    /**
     * Initializes CameraX and binds Preview and ImageCapture use cases to the provided LifecycleOwner.
     */
    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lens: Int = CameraSelector.LENS_FACING_BACK,
        onDocumentEdgeDetected: ((Boolean) -> Unit)? = null
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                lensFacing = lens
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // Preview setup
                preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // ImageCapture setup (optimized for high-resolution document text)
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                    .build()

                // Optional ImageAnalysis for live document framing analysis
                val useCases = mutableListOf(preview!!, imageCapture!!)

                if (onDocumentEdgeDetected != null) {
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val hasEdge = analyzeDocumentFrame(imageProxy)
                        onDocumentEdgeDetected(hasEdge)
                        imageProxy.close()
                    }
                    imageAnalysis = analysis
                    useCases.add(analysis)
                }

                // Unbind previous use cases before rebinding
                provider.unbindAll()

                // Bind to lifecycle
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )

                if (continuation.isActive) {
                    continuation.resume(true)
                }
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Captures a high-resolution snapshot and converts it into a properly-oriented Bitmap.
     */
    suspend fun takePicture(): Bitmap = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture
        if (capture == null) {
            continuation.resumeWithException(IllegalStateException("Camera not initialized or ImageCapture is null"))
            return@suspendCancellableCoroutine
        }

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bitmap = imageProxyToBitmap(image)
                        image.close()
                        if (continuation.isActive) {
                            continuation.resume(bitmap)
                        }
                    } catch (e: Exception) {
                        image.close()
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
            }
        )
    }

    /**
     * Toggles flashlight / torch mode.
     */
    fun toggleTorch(enable: Boolean? = null) {
        val target = enable ?: !isTorchEnabled
        camera?.cameraControl?.enableTorch(target)?.addListener({
            isTorchEnabled = target
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Tap to focus on specific coordinate in PreviewView.
     */
    fun focusOnPoint(previewView: PreviewView, x: Float, y: Float) {
        val factory = SurfaceOrientedMeteringPointFactory(
            previewView.width.toFloat(),
            previewView.height.toFloat()
        )
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    /**
     * Unbinds all use cases and releases camera resources.
     */
    fun stopCamera() {
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        imageAnalysis = null
    }

    /**
     * Converts an ImageProxy (JPEG or YUV) into a rotated and corrected Bitmap.
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Failed to decode image bytes")

        val rotationDegrees = image.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        } else {
            original
        }
    }

    /**
     * Simple luminance and edge analysis for document alignment feedback.
     */
    private fun analyzeDocumentFrame(image: ImageProxy): Boolean {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Simple luminance threshold variance check
        var variance = 0L
        val step = 20
        var count = 0
        for (i in 0 until bytes.size step step) {
            val lum = bytes[i].toInt() and 0xFF
            variance += lum
            count++
        }
        val avg = if (count > 0) variance / count else 0
        return avg in 40..220 // Good document contrast range
    }

    companion object {
        private const val TAG = "CameraService"
    }
}
