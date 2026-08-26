package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentEntity
import com.example.engine.PdfCompressionEngine
import com.example.engine.PdfEngine
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PdfRed
import kotlinx.coroutines.launch
import java.io.File

enum class BatchToolMode {
    MENU,
    IMAGES_TO_PDF,
    MERGE_PDFS,
    COMPRESS_PDF,
    SPLIT_PDF,
    PDF_TO_IMAGES,
    WATERMARK_PDF
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchStudioScreen(
    viewModel: PdfAppViewModel,
    onBack: () -> Unit,
    onOpenDocument: (DocumentEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allDocs by viewModel.documents.collectAsState()
    var activeMode by remember { mutableStateOf(BatchToolMode.MENU) }

    // State for operations
    val selectedDocIds = remember { mutableStateListOf<Long>() }
    var operationTitle by remember { mutableStateOf("") }
    var compressionQuality by remember { mutableFloatStateOf(60f) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var splitPageRange by remember { mutableStateOf("1") }
    var isProcessing by remember { mutableStateOf(false) }
    var processSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Image Picker for Batch Images to PDF
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            scope.launch {
                val bitmaps = mutableListOf<Bitmap>()
                for (u in uris) {
                    try {
                        context.contentResolver.openInputStream(u)?.use { input ->
                            val bmp = BitmapFactory.decodeStream(input)
                            if (bmp != null) bitmaps.add(bmp)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (bitmaps.isNotEmpty()) {
                    val title = if (operationTitle.isNotBlank()) operationTitle else "Images_Album_${System.currentTimeMillis() % 10000}"
                    val doc = viewModel.repository.createPdfFromBitmaps(title, bitmaps, "Batch, Images")
                    isProcessing = false
                    if (doc != null) {
                        Toast.makeText(context, "Batch PDF created with ${bitmaps.size} pages!", Toast.LENGTH_SHORT).show()
                        onOpenDocument(doc)
                    }
                } else {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (activeMode) {
                            BatchToolMode.MENU -> "Batch Processing Studio"
                            BatchToolMode.IMAGES_TO_PDF -> "Batch Images → PDF"
                            BatchToolMode.MERGE_PDFS -> "Merge Multiple PDFs"
                            BatchToolMode.COMPRESS_PDF -> "Compress PDF"
                            BatchToolMode.SPLIT_PDF -> "Split PDF / Extract Pages"
                            BatchToolMode.PDF_TO_IMAGES -> "Export PDF → Images"
                            BatchToolMode.WATERMARK_PDF -> "Batch Watermark PDF"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeMode != BatchToolMode.MENU) {
                            activeMode = BatchToolMode.MENU
                            selectedDocIds.clear()
                            processSuccessMsg = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp)
        ) {
            when (activeMode) {
                BatchToolMode.MENU -> {
                    // Hub Menu of Advanced PDF Tools
                    Text(
                        text = "Advanced Document Processing Suite",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(14.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            BatchToolCard(
                                title = "📄 Batch Images → PDF",
                                subtitle = "Select multiple photos/PNGs and convert into a clean organized PDF album.",
                                icon = Icons.Default.Image,
                                iconColor = Color(0xFF2563EB),
                                onClick = {
                                    activeMode = BatchToolMode.IMAGES_TO_PDF
                                    operationTitle = "Photo_Album_${System.currentTimeMillis() % 10000}"
                                }
                            )
                        }

                        item {
                            BatchToolCard(
                                title = "🔗 Merge Multiple PDFs",
                                subtitle = "Combine 2 or more PDF documents into a single consolidated file.",
                                icon = Icons.Default.MergeType,
                                iconColor = PdfRed,
                                onClick = {
                                    activeMode = BatchToolMode.MERGE_PDFS
                                    operationTitle = "Merged_Document_${System.currentTimeMillis() % 10000}"
                                }
                            )
                        }

                        item {
                            BatchToolCard(
                                title = "🗜️ Compress PDF",
                                subtitle = "Reduce document file size with selectable DPI downsampling.",
                                icon = Icons.Default.Compress,
                                iconColor = AccentEmerald,
                                onClick = { activeMode = BatchToolMode.COMPRESS_PDF }
                            )
                        }

                        item {
                            BatchToolCard(
                                title = "✂️ Split PDF / Extract Pages",
                                subtitle = "Extract specific pages or separate sections into a new PDF.",
                                icon = Icons.Default.CallSplit,
                                iconColor = Color(0xFFD97706),
                                onClick = { activeMode = BatchToolMode.SPLIT_PDF }
                            )
                        }

                        item {
                            BatchToolCard(
                                title = "📑 Export PDF → High-Res Images",
                                subtitle = "Render and save all pages of a PDF as separate PNG/JPG image files.",
                                icon = Icons.Default.Image,
                                iconColor = Color(0xFF7C3AED),
                                onClick = { activeMode = BatchToolMode.PDF_TO_IMAGES }
                            )
                        }

                        item {
                            BatchToolCard(
                                title = "💧 Watermark & Stamp PDF",
                                subtitle = "Apply diagonal 'CONFIDENTIAL' or custom stamps across all pages.",
                                icon = Icons.Default.WaterDrop,
                                iconColor = Color(0xFF0891B2),
                                onClick = { activeMode = BatchToolMode.WATERMARK_PDF }
                            )
                        }
                    }
                }

                BatchToolMode.IMAGES_TO_PDF -> {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = "1. Enter PDF Document Title",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = operationTitle,
                            onValueChange = { operationTitle = it },
                            label = { Text("Output PDF Title") },
                            modifier = Modifier.fillMaxWidth().testTag("batch_images_pdf_title")
                        )

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "2. Select Images from Device",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("select_images_button")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Pick Images & Convert to PDF")
                            }
                        }
                    }
                }

                BatchToolMode.MERGE_PDFS -> {
                    Column {
                        Text(
                            text = "Select PDFs to Merge (${selectedDocIds.size} selected)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = operationTitle,
                            onValueChange = { operationTitle = it },
                            label = { Text("Merged PDF Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allDocs) { doc ->
                                val isSelected = selectedDocIds.contains(doc.id)
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) PdfRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedDocIds.remove(doc.id) else selectedDocIds.add(doc.id)
                                        }
                                        .border(
                                            1.dp,
                                            if (isSelected) PdfRed else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.MergeType,
                                            contentDescription = null,
                                            tint = if (isSelected) PdfRed else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${doc.pageCount} Pages • ${(doc.fileSize / 1024)} KB", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = PdfRed)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                scope.launch {
                                    val files = selectedDocIds.mapNotNull { id ->
                                        allDocs.find { it.id == id }?.let { File(it.filePath) }
                                    }
                                    val outDir = File(context.filesDir, "user_pdfs")
                                    outDir.mkdirs()
                                    val safeTitle = operationTitle.ifBlank { "Merged_${System.currentTimeMillis() % 10000}" }
                                    val outFile = File(outDir, "$safeTitle.pdf")
                                    val success = PdfEngine.mergePdfs(files, outFile)
                                    isProcessing = false
                                    if (success) {
                                        val count = PdfEngine.getPageCount(outFile)
                                        val newDoc = DocumentEntity(
                                            title = safeTitle,
                                            filePath = outFile.absolutePath,
                                            pageCount = count,
                                            fileSize = outFile.length(),
                                            tags = "Merged"
                                        )
                                        viewModel.repository.updateDocument(newDoc)
                                        Toast.makeText(context, "Merged into $safeTitle.pdf!", Toast.LENGTH_SHORT).show()
                                        onOpenDocument(newDoc)
                                    }
                                }
                            },
                            enabled = !isProcessing && selectedDocIds.size >= 2,
                            colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_merge_button")
                        ) {
                            Text("Merge ${selectedDocIds.size} PDFs")
                        }
                    }
                }

                BatchToolMode.COMPRESS_PDF -> {
                    Column {
                        Text("Select a Document to Compress", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allDocs) { doc ->
                                val isSelected = selectedDocIds.contains(doc.id)
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) AccentEmerald.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDocIds.clear()
                                            selectedDocIds.add(doc.id)
                                        }
                                        .border(1.dp, if (isSelected) AccentEmerald else Color.Transparent, RoundedCornerShape(10.dp))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Compress, contentDescription = null, tint = AccentEmerald)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${doc.pageCount} Pages • ${(doc.fileSize / 1024)} KB", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = AccentEmerald)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text("Compression Level: ${compressionQuality.toInt()}% Quality", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = compressionQuality,
                            onValueChange = { compressionQuality = it },
                            valueRange = 25f..90f
                        )

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val doc = allDocs.find { it.id == selectedDocIds.firstOrNull() } ?: return@Button
                                isProcessing = true
                                scope.launch {
                                    val origFile = File(doc.filePath)
                                    val outDir = File(context.filesDir, "user_pdfs")
                                    outDir.mkdirs()
                                    val outFile = File(outDir, "${doc.title}_compressed_${System.currentTimeMillis() % 10000}.pdf")
                                    
                                    val config = PdfCompressionEngine.CompressionConfig(
                                        preset = PdfCompressionEngine.CompressionPreset.CUSTOM,
                                        qualityPercent = compressionQuality.toInt(),
                                        scaleFactor = (compressionQuality / 100f).coerceIn(0.6f, 1.0f),
                                        stripMetadata = true
                                    )
                                    
                                    val report = PdfCompressionEngine.compressPdfDocument(
                                        context = context,
                                        inputFile = origFile,
                                        outputFile = outFile,
                                        config = config
                                    )
                                    
                                    isProcessing = false
                                    if (report.isSuccess && report.compressedFile != null) {
                                        val newDoc = DocumentEntity(
                                            title = report.compressedFile.nameWithoutExtension,
                                            filePath = report.compressedFile.absolutePath,
                                            pageCount = report.pageCount,
                                            fileSize = report.compressedSizeBytes,
                                            tags = "Compressed"
                                        )
                                        viewModel.repository.updateDocument(newDoc)
                                        Toast.makeText(
                                            context,
                                            "Compressed: ${(report.compressedSizeBytes / 1024)} KB (-${report.reductionPercentage.toInt()}%)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onOpenDocument(newDoc)
                                    } else {
                                        Toast.makeText(context, "Compression failed: ${report.errorMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isProcessing && selectedDocIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_compress_button")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Compressing...")
                            } else {
                                Text("Start Compression & Optimize")
                            }
                        }
                    }
                }

                BatchToolMode.SPLIT_PDF -> {
                    Column {
                        Text("Select Document & Pages to Extract", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allDocs) { doc ->
                                val isSelected = selectedDocIds.contains(doc.id)
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) PdfRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedDocIds.clear()
                                        selectedDocIds.add(doc.id)
                                    }.border(1.dp, if (isSelected) PdfRed else Color.Transparent, RoundedCornerShape(10.dp))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CallSplit, contentDescription = null, tint = PdfRed)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${doc.pageCount} Total Pages", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = PdfRed)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = splitPageRange,
                            onValueChange = { splitPageRange = it },
                            label = { Text("Page Numbers (e.g. 1, 2, 3)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val doc = allDocs.find { it.id == selectedDocIds.firstOrNull() } ?: return@Button
                                val pages = splitPageRange.split(",").mapNotNull { it.trim().toIntOrNull()?.minus(1) }
                                if (pages.isEmpty()) return@Button

                                isProcessing = true
                                scope.launch {
                                    val origFile = File(doc.filePath)
                                    val outDir = File(context.filesDir, "user_pdfs")
                                    outDir.mkdirs()
                                    val outFile = File(outDir, "${doc.title}_split_${System.currentTimeMillis() % 10000}.pdf")
                                    val success = PdfEngine.splitPdf(origFile, pages, outFile)
                                    isProcessing = false
                                    if (success) {
                                        val count = PdfEngine.getPageCount(outFile)
                                        val newDoc = DocumentEntity(
                                            title = outFile.nameWithoutExtension,
                                            filePath = outFile.absolutePath,
                                            pageCount = count,
                                            fileSize = outFile.length(),
                                            tags = "Extracted"
                                        )
                                        viewModel.repository.updateDocument(newDoc)
                                        Toast.makeText(context, "Extracted ${pages.size} pages!", Toast.LENGTH_SHORT).show()
                                        onOpenDocument(newDoc)
                                    }
                                }
                            },
                            enabled = !isProcessing && selectedDocIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_split_button")
                        ) {
                            Text("Extract Selected Pages")
                        }
                    }
                }

                BatchToolMode.PDF_TO_IMAGES -> {
                    Column {
                        Text("Select Document to Export as Images", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allDocs) { doc ->
                                val isSelected = selectedDocIds.contains(doc.id)
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF7C3AED).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedDocIds.clear()
                                        selectedDocIds.add(doc.id)
                                    }.border(1.dp, if (isSelected) Color(0xFF7C3AED) else Color.Transparent, RoundedCornerShape(10.dp))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF7C3AED))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${doc.pageCount} Pages to Export", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF7C3AED))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val doc = allDocs.find { it.id == selectedDocIds.firstOrNull() } ?: return@Button
                                isProcessing = true
                                scope.launch {
                                    val origFile = File(doc.filePath)
                                    val outDir = File(context.filesDir, "exported_images_${System.currentTimeMillis() % 10000}")
                                    val exported = PdfEngine.exportPdfToImages(origFile, outDir)
                                    isProcessing = false
                                    Toast.makeText(context, "Exported ${exported.size} PNG images to device!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isProcessing && selectedDocIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_export_images_button")
                        ) {
                            Text("Export All Pages as Images")
                        }
                    }
                }

                BatchToolMode.WATERMARK_PDF -> {
                    Column {
                        Text("Select Document & Watermark Text", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("Watermark Text (e.g. CONFIDENTIAL)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allDocs) { doc ->
                                val isSelected = selectedDocIds.contains(doc.id)
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF0891B2).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedDocIds.clear()
                                        selectedDocIds.add(doc.id)
                                    }.border(1.dp, if (isSelected) Color(0xFF0891B2) else Color.Transparent, RoundedCornerShape(10.dp))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF0891B2))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${doc.pageCount} Pages", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0891B2))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val doc = allDocs.find { it.id == selectedDocIds.firstOrNull() } ?: return@Button
                                isProcessing = true
                                scope.launch {
                                    val origFile = File(doc.filePath)
                                    val outDir = File(context.filesDir, "user_pdfs")
                                    outDir.mkdirs()
                                    val outFile = File(outDir, "${doc.title}_watermarked_${System.currentTimeMillis() % 10000}.pdf")
                                    val success = PdfEngine.exportAnnotatedPdf(origFile, emptyList(), outFile, watermarkText)
                                    isProcessing = false
                                    if (success) {
                                        val count = PdfEngine.getPageCount(outFile)
                                        val newDoc = DocumentEntity(
                                            title = outFile.nameWithoutExtension,
                                            filePath = outFile.absolutePath,
                                            pageCount = count,
                                            fileSize = outFile.length(),
                                            tags = "Watermarked"
                                        )
                                        viewModel.repository.updateDocument(newDoc)
                                        Toast.makeText(context, "Watermark stamped successfully!", Toast.LENGTH_SHORT).show()
                                        onOpenDocument(newDoc)
                                    }
                                }
                            },
                            enabled = !isProcessing && selectedDocIds.isNotEmpty() && watermarkText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2)),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_watermark_button")
                        ) {
                            Text("Stamp Watermark onto PDF")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatchToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
        }
    }
}
