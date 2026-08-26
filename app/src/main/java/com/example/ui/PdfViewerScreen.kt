package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.AnnotationEntity
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PdfRed
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    viewModel: PdfAppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.viewerState.collectAsState()
    val signatures by viewModel.signatures.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf(ActiveEditorTool.NONE) }
    var selectedColor by remember { mutableStateOf(Color(0xFFDC2626)) }
    var selectedStrokeWidth by remember { mutableFloatStateOf(4f) }
    var selectedStampText by remember { mutableStateOf("APPROVED") }
    var signatureToPlace by remember { mutableStateOf<String?>(null) }

    // Dialogs state
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showPageManagerDialog by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showCompressionDialog by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }

    // Transform / Zoom
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.8f, 4f)
        offset += offsetChange
    }

    val doc = state.document ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = {
                                searchInput = it
                            },
                            placeholder = { Text("Search text in PDF...", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pdf_search_input"),
                            trailingIcon = {
                                IconButton(onClick = { isSearchActive = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    } else {
                        Column {
                            Text(
                                text = doc.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Page ${state.currentPageIndex + 1} of ${state.totalPages} • ${if (isEditMode) "✏️ Annotation Mode" else "📖 Reading Mode"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Search in Doc
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }

                    // Mode Toggle (Read vs Edit)
                    IconButton(
                        onClick = {
                            isEditMode = !isEditMode
                            if (!isEditMode) activeTool = ActiveEditorTool.NONE
                        },
                        modifier = Modifier
                            .background(
                                if (isEditMode) PdfRed.copy(alpha = 0.15f) else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (isEditMode) Icons.Default.Edit else Icons.Default.MenuBook,
                            contentDescription = "Toggle Edit Mode",
                            tint = if (isEditMode) PdfRed else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(onClick = { showMenuDropdown = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🔢 Organize Pages") },
                                onClick = {
                                    showMenuDropdown = false
                                    showPageManagerDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.ViewModule, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("🔎 OCR Text Recognition") },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.runOcrOnCurrentPage()
                                    showOcrDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("✍️ Signature Studio") },
                                onClick = {
                                    showMenuDropdown = false
                                    showSignatureDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.isNightFilter) "☀️ Disable Night Mode" else "🌙 Enable Night Mode") },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.toggleNightFilter()
                                },
                                leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("🗜️ Compress & Optimize PDF") },
                                onClick = {
                                    showMenuDropdown = false
                                    showCompressionDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Compress, contentDescription = null, tint = AccentEmerald) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (doc.isEncrypted) "🔐 Vault Security" else "🔒 Lock & Encrypt PDF") },
                                onClick = {
                                    showMenuDropdown = false
                                    showSecurityDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("📤 Share / Export Annotated PDF") },
                                onClick = {
                                    showMenuDropdown = false
                                    viewModel.exportAnnotatedPdf { exportedFile ->
                                        if (exportedFile != null) {
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportedFile)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (isEditMode) {
                // Editing & Annotation Toolbar
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Tool Selector Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                EditToolButton(
                                    icon = Icons.Default.Highlight,
                                    label = "Highlight",
                                    isSelected = activeTool == ActiveEditorTool.HIGHLIGHT,
                                    onClick = {
                                        activeTool = ActiveEditorTool.HIGHLIGHT
                                        selectedColor = Color(0xFFFFEB3B)
                                    }
                                )
                            }
                            item {
                                EditToolButton(
                                    icon = Icons.Default.FormatUnderlined,
                                    label = "Underline",
                                    isSelected = activeTool == ActiveEditorTool.UNDERLINE,
                                    onClick = {
                                        activeTool = ActiveEditorTool.UNDERLINE
                                        selectedColor = Color(0xFFDC2626)
                                        selectedStrokeWidth = 3f
                                    }
                                )
                            }
                            item {
                                EditToolButton(
                                    icon = Icons.Default.FormatStrikethrough,
                                    label = "Strike",
                                    isSelected = activeTool == ActiveEditorTool.STRIKETHROUGH,
                                    onClick = {
                                        activeTool = ActiveEditorTool.STRIKETHROUGH
                                        selectedColor = Color(0xFFDC2626)
                                        selectedStrokeWidth = 3f
                                    }
                                )
                            }
                            item {
                                EditToolButton(
                                    icon = Icons.Default.Brush,
                                    label = "Pen",
                                    isSelected = activeTool == ActiveEditorTool.PENCIL,
                                    onClick = {
                                        activeTool = ActiveEditorTool.PENCIL
                                        selectedColor = Color(0xFF0F172A)
                                        selectedStrokeWidth = 4f
                                    }
                                )
                            }
                            item {
                                EditToolButton(
                                    icon = Icons.Default.TextFields,
                                    label = "Text",
                                    isSelected = activeTool == ActiveEditorTool.FREE_TEXT,
                                    onClick = {
                                        activeTool = ActiveEditorTool.FREE_TEXT
                                        selectedColor = Color(0xFF0F172A)
                                    }
                                )
                            }
                            item {
                                EditToolButton(
                                    icon = Icons.Default.Note,
                                    label = "Note",
                                    isSelected = activeTool == ActiveEditorTool.TEXT_NOTE,
                                    onClick = {
                                        activeTool = ActiveEditorTool.TEXT_NOTE
                                    }
                                )
                            }
                            item {
                                EditToolButton(
                                    icon = Icons.Default.Draw,
                                    label = "Signature",
                                    isSelected = activeTool == ActiveEditorTool.SIGNATURE,
                                    onClick = {
                                        if (signatures.isNotEmpty()) {
                                            signatureToPlace = signatures.first().pointsJson
                                            activeTool = ActiveEditorTool.SIGNATURE
                                            Toast.makeText(context, "Tap anywhere on page to place signature", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showSignatureDialog = true
                                        }
                                    }
                                )
                            }
                            item {
                                EditToolButton(
                                    icon = Icons.Default.ColorLens,
                                    label = "Stamp",
                                    isSelected = activeTool == ActiveEditorTool.STAMP,
                                    onClick = {
                                        activeTool = ActiveEditorTool.STAMP
                                    }
                                )
                            }
                            item {
                                EditToolButton(
                                    icon = Icons.Default.RadioButtonUnchecked,
                                    label = "Circle",
                                    isSelected = activeTool == ActiveEditorTool.SHAPE_CIRCLE,
                                    onClick = {
                                        activeTool = ActiveEditorTool.SHAPE_CIRCLE
                                        selectedColor = Color(0xFFDC2626)
                                        selectedStrokeWidth = 3f
                                    }
                                )
                            }
                            item {
                                EditToolButton(
                                    icon = Icons.Default.LinearScale,
                                    label = "Arrow",
                                    isSelected = activeTool == ActiveEditorTool.SHAPE_ARROW,
                                    onClick = {
                                        activeTool = ActiveEditorTool.SHAPE_ARROW
                                        selectedColor = Color(0xFFDC2626)
                                        selectedStrokeWidth = 3f
                                    }
                                )
                            }
                            item {
                                EditToolButton(
                                    icon = Icons.Default.Delete,
                                    label = "Clear",
                                    isSelected = false,
                                    onClick = { viewModel.clearAnnotationsOnCurrentPage() }
                                )
                            }
                        }

                        // Sub-options bar (Color Palette & Stamp selector)
                        if (activeTool != ActiveEditorTool.NONE) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (activeTool == ActiveEditorTool.STAMP) {
                                    val stamps = listOf("APPROVED", "REJECTED", "CONFIDENTIAL", "DRAFT", "FINAL", "PAID")
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(stamps) { stamp ->
                                            FilterChip(
                                                selected = selectedStampText == stamp,
                                                onClick = { selectedStampText = stamp },
                                                label = { Text(stamp, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                            )
                                        }
                                    }
                                } else {
                                    // Color chips
                                    val palette = listOf(
                                        Color(0xFFDC2626), // Red
                                        Color(0xFFFFEB3B), // Yellow
                                        Color(0xFF10B981), // Emerald
                                        Color(0xFF2563EB), // Blue
                                        Color(0xFF0F172A), // Black
                                        Color(0xFF9333EA)  // Purple
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (c in palette) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(c, CircleShape)
                                                    .border(
                                                        width = if (selectedColor == c) 2.5.dp else 0.5.dp,
                                                        color = if (selectedColor == c) Color.White else Color.Gray,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { selectedColor = c }
                                            )
                                        }
                                    }

                                    // Stroke width indicator
                                    Text(
                                        text = "${selectedStrokeWidth.toInt()}pt",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Reading Mode Bottom Bar (Page Navigation & Ribbon)
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.goToPrevPage() },
                            enabled = state.currentPageIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Page")
                        }

                        // Page Jumper Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        ) {
                            Slider(
                                value = state.currentPageIndex.toFloat(),
                                onValueChange = { viewModel.jumpToPage(it.toInt()) },
                                valueRange = 0f..(state.totalPages - 1).coerceAtLeast(0).toFloat(),
                                steps = (state.totalPages - 2).coerceAtLeast(0),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${state.currentPageIndex + 1} / ${state.totalPages}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.goToNextPage() },
                            enabled = state.currentPageIndex < state.totalPages - 1
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Page")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (state.isNightFilter) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            val bmp = state.currentBitmap

            if (state.isLoadingPage) {
                CircularProgressIndicator(color = PdfRed)
            } else if (bmp != null && !bmp.isRecycled) {
                // Main PDF Page Viewer Box with transform gestures and annotation canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(
                            state = transformState,
                            enabled = !isEditMode || activeTool == ActiveEditorTool.NONE
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box {
                            // Rendered PDF Page Bitmap
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF Page ${state.currentPageIndex + 1}",
                                colorFilter = if (state.isNightFilter) {
                                    val cm = ColorMatrix(
                                        floatArrayOf(
                                            -1f, 0f, 0f, 0f, 255f,
                                            0f, -1f, 0f, 0f, 255f,
                                            0f, 0f, -1f, 0f, 255f,
                                            0f, 0f, 0f, 1f, 0f
                                        )
                                    )
                                    ColorFilter.colorMatrix(cm)
                                } else null,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Interactive Editor Overlay Canvas
                            PdfEditorCanvas(
                                modifier = Modifier.matchParentSize(),
                                activeTool = activeTool,
                                selectedColor = selectedColor,
                                selectedStrokeWidth = selectedStrokeWidth,
                                annotations = state.annotations,
                                onAddAnnotation = { ann ->
                                    viewModel.addAnnotation(
                                        ann.copy(
                                            documentId = doc.id,
                                            pageIndex = state.currentPageIndex
                                        )
                                    )
                                },
                                onDeleteAnnotation = { id -> viewModel.deleteAnnotation(id) },
                                selectedStampText = selectedStampText,
                                signaturePointsToPlace = signatureToPlace
                            )
                        }
                    }
                }
            } else {
                Text("Error rendering page", color = Color.Gray)
            }
        }
    }

    // Dialogs
    if (showSignatureDialog) {
        SignatureStudioDialog(
            savedSignatures = signatures,
            onDismiss = { showSignatureDialog = false },
            onSaveSignature = { title, points, col, width ->
                viewModel.saveSignature(title, points, col, width)
            },
            onDeleteSignature = { sig -> viewModel.deleteSignature(sig) },
            onSelectSignatureToPlace = { pts ->
                signatureToPlace = pts
                activeTool = ActiveEditorTool.SIGNATURE
                Toast.makeText(context, "Tap on page to place signature", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPageManagerDialog) {
        PageManagerDialog(
            document = doc,
            onDismiss = { showPageManagerDialog = false },
            onApplyChanges = { specs ->
                viewModel.modifyPages(specs) { success ->
                    showPageManagerDialog = false
                    if (success) {
                        Toast.makeText(context, "Pages reorganized successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showOcrDialog) {
        OcrResultDialog(
            extractedText = state.ocrText,
            isLoading = state.isOcrLoading,
            onDismiss = { showOcrDialog = false }
        )
    }

    if (showSecurityDialog) {
        SecurityLockDialog(
            document = doc,
            onDismiss = { showSecurityDialog = false },
            onLockWithPassword = { pass ->
                viewModel.lockDocumentWithPassword(doc, pass)
            },
            onUnlockDocument = {
                viewModel.unlockDocument(doc)
            }
        )
    }

    if (showCompressionDialog) {
        PdfCompressionDialog(
            document = doc,
            viewModel = viewModel,
            onDismiss = { showCompressionDialog = false },
            onOpenDocument = { compressedDoc ->
                showCompressionDialog = false
                viewModel.openDocument(compressedDoc)
            }
        )
    }
}

@Composable
fun EditToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) PdfRed else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PdfRed else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
