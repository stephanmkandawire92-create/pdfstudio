package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.DocumentEntity
import com.example.engine.CameraService
import com.example.engine.ScanFilter
import com.example.engine.ScannerEngine
import com.example.ui.theme.PdfRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: PdfAppViewModel,
    onBack: () -> Unit,
    onPdfCreated: (DocumentEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scannedBitmaps by viewModel.scannedBitmaps.collectAsState()
    var selectedIndex by remember { mutableIntStateOf(0) }
    var currentFilter by remember { mutableStateOf(ScanFilter.MAGIC_COLOR) }
    var documentTitle by remember { mutableStateOf("Scan_${System.currentTimeMillis() % 100000}") }
    var isProcessing by remember { mutableStateOf(false) }
    var isLiveCameraActive by remember { mutableStateOf(false) }

    // Camera Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isLiveCameraActive = true
        } else {
            Toast.makeText(context, "Camera permission required for document scanning", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Multi-Image Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                for (u in uris) {
                    try {
                        context.contentResolver.openInputStream(u)?.use { input ->
                            val bmp = BitmapFactory.decodeStream(input)
                            if (bmp != null) {
                                viewModel.addScannedBitmap(bmp)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    if (isLiveCameraActive) {
        CameraXLiveScanner(
            onImageCaptured = { bmp ->
                viewModel.addScannedBitmap(bmp)
                selectedIndex = scannedBitmaps.size
            },
            pageCount = scannedBitmaps.size,
            onClose = { isLiveCameraActive = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Document Scanner", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${scannedBitmaps.size} Page(s) captured",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            isLiveCameraActive = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Take Photo", tint = PdfRed)
                    }
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Import Gallery", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (scannedBitmaps.isEmpty()) {
                // Empty state to prompt camera / gallery capture
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(PdfRed.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = PdfRed,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Capture or Import Documents",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Use CameraX scanner to auto-frame and capture contracts, receipts, notes, or books, or pick images from gallery.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        isLiveCameraActive = true
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                                modifier = Modifier.testTag("start_camera_scan_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Camera Scan")
                            }

                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("start_gallery_scan_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Import Gallery")
                            }
                        }
                    }
                }
            } else {
                // Active Scan Viewer
                val activeBmp = scannedBitmaps.getOrNull(selectedIndex) ?: scannedBitmaps.first()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = activeBmp.asImageBitmap(),
                        contentDescription = "Active Scan Page",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Page Badge overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Page ${selectedIndex + 1} of ${scannedBitmaps.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Delete Page
                    IconButton(
                        onClick = {
                            viewModel.removeScannedBitmap(selectedIndex)
                            selectedIndex = 0.coerceAtLeast(selectedIndex - 1)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete page", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Enhancement Filter Chips
                Text(
                    text = "Document Enhancement Filters",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf(
                        ScanFilter.MAGIC_COLOR to "Magic Color",
                        ScanFilter.BW_DOCUMENT to "B&W Clean",
                        ScanFilter.GRAYSCALE to "Grayscale",
                        ScanFilter.SHARPEN_CONTRAST to "Sharpen",
                        ScanFilter.ORIGINAL to "Original"
                    )
                    for ((f, label) in filters) {
                        FilterChip(
                            selected = currentFilter == f,
                            onClick = {
                                currentFilter = f
                                scope.launch {
                                    val filtered = ScannerEngine.applyFilter(activeBmp, f)
                                    val list = scannedBitmaps.toMutableList()
                                    if (selectedIndex in list.indices) {
                                        list[selectedIndex] = filtered
                                        viewModel.scannedBitmaps.value = list
                                    }
                                }
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PdfRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Multi-page thumbnail filmstrip
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(scannedBitmaps) { idx, bmp ->
                        Box(
                            modifier = Modifier
                                .size(54.dp, 68.dp)
                                .border(
                                    width = if (selectedIndex == idx) 2.5.dp else 1.dp,
                                    color = if (selectedIndex == idx) PdfRed else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIndex = idx }
                                .padding(2.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Thumb $idx",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Add more pages button
                    item {
                        IconButton(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    isLiveCameraActive = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .size(54.dp, 68.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Add page", tint = PdfRed)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Title Input & Save PDF Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = documentTitle,
                        onValueChange = { documentTitle = it },
                        label = { Text("PDF Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("scanned_pdf_title_input")
                    )

                    Button(
                        onClick = {
                            isProcessing = true
                            viewModel.saveScannedPdf(documentTitle) { newDoc ->
                                isProcessing = false
                                if (newDoc != null) {
                                    Toast.makeText(context, "PDF saved successfully!", Toast.LENGTH_SHORT).show()
                                    onPdfCreated(newDoc)
                                }
                            }
                        },
                        enabled = !isProcessing && scannedBitmaps.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                        modifier = Modifier.height(56.dp).testTag("save_scanned_pdf_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save PDF")
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen CameraX Live Viewfinder for document scanning with edge detection guides,
 * torch toggling, camera flip, and batch rapid-fire capture.
 */
@Composable
fun CameraXLiveScanner(
    onImageCaptured: (Bitmap) -> Unit,
    pageCount: Int,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraService = remember { CameraService(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var hasDocumentEdge by remember { mutableStateOf(true) }
    var currentLens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraService.stopCamera()
        }
    }

    LaunchedEffect(previewView, currentLens) {
        val view = previewView ?: return@LaunchedEffect
        cameraService.startCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = view,
            lens = currentLens,
            onDocumentEdgeDetected = { edge ->
                hasDocumentEdge = edge
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Live Preview View
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also {
                    previewView = it
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Document framing viewfinder overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 4.dp.toPx()
            val paddingH = size.width * 0.08f
            val paddingV = size.height * 0.18f
            val rectW = size.width - (paddingH * 2)
            val rectH = size.height - (paddingV * 2)
            val cornerLen = 32.dp.toPx()

            val frameColor = if (hasDocumentEdge) Color(0xFF4CAF50) else Color(0xAAFFFFFF)

            // Draw 4 corner guides
            // Top-Left
            drawLine(frameColor, Offset(paddingH, paddingV), Offset(paddingH + cornerLen, paddingV), strokeW)
            drawLine(frameColor, Offset(paddingH, paddingV), Offset(paddingH, paddingV + cornerLen), strokeW)

            // Top-Right
            drawLine(frameColor, Offset(paddingH + rectW, paddingV), Offset(paddingH + rectW - cornerLen, paddingV), strokeW)
            drawLine(frameColor, Offset(paddingH + rectW, paddingV), Offset(paddingH + rectW, paddingV + cornerLen), strokeW)

            // Bottom-Left
            drawLine(frameColor, Offset(paddingH, paddingV + rectH), Offset(paddingH + cornerLen, paddingV + rectH), strokeW)
            drawLine(frameColor, Offset(paddingH, paddingV + rectH), Offset(paddingH, paddingV + rectH - cornerLen), strokeW)

            // Bottom-Right
            drawLine(frameColor, Offset(paddingH + rectW, paddingV + rectH), Offset(paddingH + rectW - cornerLen, paddingV + rectH), strokeW)
            drawLine(frameColor, Offset(paddingH + rectW, paddingV + rectH), Offset(paddingH + rectW, paddingV + rectH - cornerLen), strokeW)

            // Subtle bounding border
            drawRoundRect(
                color = frameColor.copy(alpha = 0.35f),
                topLeft = Offset(paddingH, paddingV),
                size = Size(rectW, rectH),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Top Controls: Close, Torch, Flip Camera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
            }

            // Framing Guide Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = if (hasDocumentEdge) "Document in Frame" else "Align document inside box",
                    color = if (hasDocumentEdge) Color(0xFF81C784) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Torch toggle
                IconButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        cameraService.toggleTorch(isTorchOn)
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = if (isTorchOn) Color(0xFFFFD54F) else Color.White
                    )
                }

                // Camera Lens Flip
                IconButton(
                    onClick = {
                        currentLens = if (currentLens == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip Camera", tint = Color.White)
                }
            }
        }

        // Bottom Capture Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(bottom = 36.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$pageCount page(s) in current scan batch",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Empty spacer for balance
                Spacer(Modifier.size(56.dp))

                // Main Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable(enabled = !isCapturing) {
                            isCapturing = true
                            scope.launch {
                                try {
                                    val bitmap = cameraService.takePicture()
                                    onImageCaptured(bitmap)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Capture failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isCapturing = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(color = PdfRed, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(PdfRed, CircleShape)
                        )
                    }
                }

                // Done / Finish button when at least 1 page captured
                if (pageCount > 0) {
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = CircleShape,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(Modifier.size(56.dp))
                }
            }
        }
    }
}
