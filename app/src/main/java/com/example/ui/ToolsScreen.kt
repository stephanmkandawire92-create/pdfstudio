package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.DocumentEntity
import com.example.engine.PdfToolsEngine
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PdfRed
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class PdfTool(
    val name: String,
    val icon: ImageVector,
    val requiresMultiple: Boolean = false,
    val returnsFile: Boolean = true,
    val action: suspend (Context, List<Uri>, File) -> File? = { _, _, _ -> null }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: PdfAppViewModel,
    onBack: () -> Unit,
    onOpenDocument: (DocumentEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isProcessing by remember { mutableStateOf(false) }
    var processingToolName by remember { mutableStateOf("") }
    
    var activeTool by remember { mutableStateOf<PdfTool?>(null) }
    var showPageNumberDialog by remember { mutableStateOf(false) }
    var pendingUris by remember { mutableStateOf<List<Uri>?>(null) }
    var selectedPagePosition by remember { mutableStateOf(PdfToolsEngine.PageNumberPosition.FOOTER) }

    fun processTool(uris: List<Uri>, tool: PdfTool, extraAction: (suspend (Context, List<Uri>, File) -> File?)? = null) {
        isProcessing = true
        processingToolName = tool.name
        scope.launch {
            val outDir = File(context.filesDir, "user_pdfs")
            outDir.mkdirs()
            
            val actionToRun = extraAction ?: tool.action
            val resultFile = actionToRun(context, uris, outDir)
            if (tool.returnsFile) {
                if (resultFile != null && resultFile.exists()) {
                    val newDoc = DocumentEntity(
                        title = resultFile.nameWithoutExtension,
                        filePath = resultFile.absolutePath,
                        pageCount = 1, // Approximation
                        fileSize = resultFile.length(),
                        tags = "Tool Output"
                    )
                    viewModel.repository.updateDocument(newDoc)
                    Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show()
                    onOpenDocument(newDoc)
                } else {
                    Toast.makeText(context, "Operation failed or not supported.", Toast.LENGTH_SHORT).show()
                }
            }
            isProcessing = false
            activeTool = null
            pendingUris = null
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty() && activeTool != null) {
            if (activeTool!!.name == "Add Page #s") {
                pendingUris = uris
                showPageNumberDialog = true
            } else {
                processTool(uris, activeTool!!)
            }
        }
    }

    val tools = listOf(
        PdfTool("Merge PDFs", Icons.Default.MergeType, requiresMultiple = true) { ctx, uris, out -> PdfToolsEngine.mergePdfs(ctx, uris, out) },
        PdfTool("Split PDF", Icons.Default.CallSplit) { ctx, uris, out -> PdfToolsEngine.splitPdf(ctx, uris.first(), out).firstOrNull() },
        PdfTool("Extract Pages", Icons.Default.FilterNone) { ctx, uris, out -> PdfToolsEngine.extractPages(ctx, uris.first(), listOf(0), out) }, // Default to extract page 1
        PdfTool("Rotate PDF", Icons.Default.RotateRight) { ctx, uris, out -> PdfToolsEngine.rotatePdf(ctx, uris.first(), 90, out) },
        PdfTool("Compress PDF", Icons.Default.Compress, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Use Compress from Home Screen.", Toast.LENGTH_SHORT).show(); null },
        PdfTool("Protect PDF", Icons.Default.Lock, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Use Vault from Home Screen.", Toast.LENGTH_SHORT).show(); null },
        PdfTool("Unlock PDF", Icons.Default.LockOpen, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Use Vault from Home Screen.", Toast.LENGTH_SHORT).show(); null },
        PdfTool("Images to PDF", Icons.Default.Image, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Use Scanner from Home Screen.", Toast.LENGTH_SHORT).show(); null },
        PdfTool("PDF to Images", Icons.Default.AddAPhoto, returnsFile = false) { ctx, uris, out -> 
            val images = PdfToolsEngine.pdfToImages(ctx, uris.first(), out)
            if (images.isNotEmpty()) Toast.makeText(ctx, "Saved ${images.size} images.", Toast.LENGTH_SHORT).show()
            null
        },
        PdfTool("OCR Text", Icons.Default.DocumentScanner, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Use OCR from Home Screen.", Toast.LENGTH_SHORT).show(); null },
        PdfTool("PDF to Text", Icons.Default.TextFields, returnsFile = false) { ctx, uris, _ -> 
            val text = PdfToolsEngine.pdfToText(ctx, uris.first())
            if (!text.isNullOrBlank()) Toast.makeText(ctx, "Extracted ${text.length} chars. (Logging to console)", Toast.LENGTH_LONG).show()
            null
        },
        PdfTool("Fill Forms", Icons.Default.Edit, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Form Fill requires interactive UI (Pro Feature).", Toast.LENGTH_SHORT).show(); null },
        PdfTool("Sign PDF", Icons.Default.Draw, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Use Sign from Home Screen.", Toast.LENGTH_SHORT).show(); null },
        PdfTool("Resize Pages", Icons.Default.Transform, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Pro Feature", Toast.LENGTH_SHORT).show(); null },
        PdfTool("Remove Blank", Icons.Default.DeleteOutline) { ctx, uris, out -> PdfToolsEngine.removeBlankPages(ctx, uris.first(), out) },
        PdfTool("Add Page #s", Icons.Default.FormatListNumbered) { ctx, uris, out -> PdfToolsEngine.addPageNumbers(ctx, uris.first(), PdfToolsEngine.PageNumberPosition.FOOTER, out) },
        PdfTool("Watermark", Icons.Default.WaterDrop) { ctx, uris, out -> PdfToolsEngine.addWatermark(ctx, uris.first(), "CONFIDENTIAL", out) },
        PdfTool("Print PDF", Icons.Default.Print, returnsFile = false) { ctx, uris, _ -> PdfToolsEngine.printPdf(ctx, uris.first()); null },
        PdfTool("Word to PDF", Icons.Default.Description, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Requires Cloud Conversion", Toast.LENGTH_SHORT).show(); null },
        PdfTool("PDF to Word", Icons.Default.Description, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Requires Cloud Conversion", Toast.LENGTH_SHORT).show(); null },
        PdfTool("Excel to PDF", Icons.Default.TableChart, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Requires Cloud Conversion", Toast.LENGTH_SHORT).show(); null },
        PdfTool("PDF to Excel", Icons.Default.TableChart, returnsFile = false) { _, _, _ -> Toast.makeText(context, "Requires Cloud Conversion", Toast.LENGTH_SHORT).show(); null }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PDF Toolbox", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (showPageNumberDialog && pendingUris != null && activeTool != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { 
                    showPageNumberDialog = false
                    pendingUris = null
                    activeTool = null
                },
                title = { Text("Page Number Position") },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(
                                selected = selectedPagePosition == PdfToolsEngine.PageNumberPosition.HEADER,
                                onClick = { selectedPagePosition = PdfToolsEngine.PageNumberPosition.HEADER }
                            )
                            Text("Header (Top Center)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(
                                selected = selectedPagePosition == PdfToolsEngine.PageNumberPosition.FOOTER,
                                onClick = { selectedPagePosition = PdfToolsEngine.PageNumberPosition.FOOTER }
                            )
                            Text("Footer (Bottom Center)")
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showPageNumberDialog = false
                        val currentTool = activeTool!!
                        val currentUris = pendingUris!!
                        processTool(currentUris, currentTool) { ctx, uris, out ->
                            PdfToolsEngine.addPageNumbers(ctx, uris.first(), selectedPagePosition, out)
                        }
                    }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showPageNumberDialog = false
                        pendingUris = null
                        activeTool = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (isProcessing) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = PdfRed)
                Spacer(Modifier.height(16.dp))
                Text("Executing $processingToolName...", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tools) { tool ->
                    ToolItem(tool) {
                        activeTool = tool
                        filePicker.launch(arrayOf("application/pdf"))
                    }
                }
            }
        }
    }
}

@Composable
fun ToolItem(tool: PdfTool, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PdfRed),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = tool.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                color = Color.White
            )
        }
    }
}
