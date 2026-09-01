package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentEntity
import com.example.engine.PdfToolsAdvancedEngine
import com.example.engine.PdfToolsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class FunctionalPdfTool(val name: String, val multiSelect: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreenV2(viewModel: PdfAppViewModel, onBack: () -> Unit, onOpenDocument: (DocumentEntity) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }
    var selectedTool by remember { mutableStateOf<FunctionalPdfTool?>(null) }
    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pageDialog by remember { mutableStateOf(false) }
    var pageText by remember { mutableStateOf("1") }
    var passwordDialog by remember { mutableStateOf(false) }
    var passwordText by remember { mutableStateOf("") }
    var resizeDialog by remember { mutableStateOf(false) }
    var outputText by remember { mutableStateOf<String?>(null) }

    fun startTool(uris: List<Uri>) {
        val tool = selectedTool ?: return
        pendingUris = uris
        when (tool.name) {
            "Extract Pages" -> { pageText = "1"; pageDialog = true }
            "Protect PDF", "Unlock PDF" -> { passwordText = ""; passwordDialog = true }
            "Resize Pages" -> resizeDialog = true
            else -> runTool(context, viewModel, tool, uris, null, null, null, scope, { busy = it }, { busy = null; outputText = it }, onOpenDocument)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) startTool(uris)
    }

    val tools = listOf(
        FunctionalPdfTool("Merge PDFs", true), FunctionalPdfTool("Split PDF"), FunctionalPdfTool("Extract Pages"),
        FunctionalPdfTool("Rotate PDF"), FunctionalPdfTool("Protect PDF"), FunctionalPdfTool("Unlock PDF"),
        FunctionalPdfTool("Images to PDF", true), FunctionalPdfTool("PDF to Images"), FunctionalPdfTool("PDF to Text"),
        FunctionalPdfTool("Remove Blank"), FunctionalPdfTool("Watermark"), FunctionalPdfTool("Resize Pages"), FunctionalPdfTool("Print PDF")
    )

    Scaffold(topBar = {
        TopAppBar(title = { Text("PDF Toolbox", fontWeight = FontWeight.Bold) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        })
    }) { padding ->
        if (!busy.isNullOrBlank()) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text("Processing $busy…")
            }
        } else {
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tools) { tool ->
                    Card(onClick = {
                        selectedTool = tool
                        picker.launch(if (tool.name == "Images to PDF") arrayOf("image/*") else arrayOf("application/pdf"))
                    }, modifier = Modifier.height(112.dp)) {
                        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(iconFor(tool.name), null); Spacer(Modifier.height(8.dp)); Text(tool.name, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (pageDialog) AlertDialog(onDismissRequest = { pageDialog = false }, title = { Text("Extract pages") }, text = {
            OutlinedTextField(pageText, { pageText = it }, label = { Text("Pages: 1,3,5-7") })
        }, confirmButton = { Button(onClick = {
            pageDialog = false
            runTool(context, viewModel, selectedTool!!, pendingUris, pageText, null, null, scope, { busy = it }, { busy = null; outputText = it }, onOpenDocument)
        }) { Text("Extract") } }, dismissButton = { TextButton(onClick = { pageDialog = false }) { Text("Cancel") } })

        if (passwordDialog) AlertDialog(onDismissRequest = { passwordDialog = false }, title = { Text(if (selectedTool?.name == "Protect PDF") "Protect PDF" else "Unlock PDF") }, text = {
            OutlinedTextField(passwordText, { passwordText = it }, label = { Text("Password") })
        }, confirmButton = { Button(enabled = passwordText.isNotBlank(), onClick = {
            passwordDialog = false
            runTool(context, viewModel, selectedTool!!, pendingUris, null, passwordText, null, scope, { busy = it }, { busy = null; outputText = it }, onOpenDocument)
        }) { Text("Continue") } }, dismissButton = { TextButton(onClick = { passwordDialog = false }) { Text("Cancel") } })

        if (resizeDialog) AlertDialog(onDismissRequest = { resizeDialog = false }, title = { Text("Resize pages") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose output page size")
                listOf("A4", "A5", "LETTER", "LEGAL").forEach { size ->
                    OutlinedButton(onClick = {
                        resizeDialog = false
                        runTool(context, viewModel, selectedTool!!, pendingUris, null, null, size, scope, { busy = it }, { busy = null; outputText = it }, onOpenDocument)
                    }, modifier = Modifier.fillMaxWidth()) { Text(size) }
                }
            }
        }, confirmButton = {})

        outputText?.let { message ->
            AlertDialog(onDismissRequest = { outputText = null }, title = { Text("PDF Studio") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { outputText = null }) { Text("OK") } })
        }
    }
}

private fun iconFor(name: String) = when (name) {
    "Merge PDFs" -> Icons.AutoMirrored.Filled.MergeType; "Split PDF" -> Icons.AutoMirrored.Filled.CallSplit; "Extract Pages" -> Icons.Default.FilterNone
    "Rotate PDF" -> Icons.AutoMirrored.Filled.RotateRight; "Protect PDF" -> Icons.Default.Lock; "Unlock PDF" -> Icons.Default.LockOpen
    "Images to PDF", "PDF to Images" -> Icons.Default.Image; "PDF to Text" -> Icons.Default.TextFields; "Remove Blank" -> Icons.Default.DeleteOutline
    "Watermark" -> Icons.Default.WaterDrop; "Resize Pages" -> Icons.Default.Transform; else -> Icons.Default.Print
}

private fun parsePageSpec(spec: String): List<Int> {
    val result = mutableListOf<Int>()
    spec.split(',').map(String::trim).filter(String::isNotEmpty).forEach { token ->
        if ('-' in token) {
            val p = token.split('-', limit = 2); val a = p[0].toIntOrNull(); val b = p[1].toIntOrNull()
            if (a != null && b != null && a > 0 && b >= a) result += a..b
        } else token.toIntOrNull()?.takeIf { it > 0 }?.let(result::add)
    }
    return result.distinct()
}

private fun runTool(context: Context, viewModel: PdfAppViewModel, tool: FunctionalPdfTool, uris: List<Uri>, pages: String?, password: String?, resize: String?, scope: CoroutineScope, setBusy: (String) -> Unit, setResult: (String) -> Unit, onOpenDocument: (DocumentEntity) -> Unit) {
    setBusy(tool.name)
    scope.launch {
        val outDir = File(context.filesDir, "user_pdfs").apply { mkdirs() }
        try {
            when (tool.name) {
                "Merge PDFs" -> {
                    val file = PdfToolsEngine.mergePdfs(context, uris, outDir) ?: error("Merge failed")
                    registerAndOpen(viewModel, file, "Merged PDF", onOpenDocument); setResult("Merged ${uris.size} PDFs successfully.")
                }
                "Split PDF" -> {
                    val files = PdfToolsEngine.splitPdf(context, uris.first(), outDir); if (files.isEmpty()) error("Split failed")
                    files.forEach { viewModel.repository.registerGeneratedPdf(it, "Split") }; setResult("Split complete: ${files.size} PDF files created.")
                }
                "Extract Pages" -> {
                    val nums = parsePageSpec(pages ?: ""); if (nums.isEmpty()) error("Enter valid page numbers")
                    val file = PdfToolsAdvancedEngine.extractPages(context, uris.first(), nums, outDir) ?: error("No valid pages found")
                    registerAndOpen(viewModel, file, "Extracted", onOpenDocument); setResult("Extracted ${nums.size} page(s) successfully.")
                }
                "Rotate PDF" -> {
                    val file = PdfToolsEngine.rotatePdf(context, uris.first(), 90, outDir) ?: error("Rotation failed")
                    registerAndOpen(viewModel, file, "Rotated", onOpenDocument); setResult("Rotated all pages 90° clockwise.")
                }
                "Protect PDF" -> {
                    val file = PdfToolsAdvancedEngine.protectPdf(context, uris.first(), password!!, password, outDir) ?: error("Protection failed")
                    registerAndOpen(viewModel, file, "Protected, Encrypted", onOpenDocument); setResult("PDF encrypted successfully.")
                }
                "Unlock PDF" -> {
                    val file = PdfToolsAdvancedEngine.unlockPdf(context, uris.first(), password ?: "", outDir) ?: error("Wrong password or PDF is not encrypted")
                    registerAndOpen(viewModel, file, "Unlocked", onOpenDocument); setResult("PDF decrypted successfully.")
                }
                "Images to PDF" -> {
                    val file = PdfToolsAdvancedEngine.imagesToPdf(context, uris, outDir) ?: error("Could not create PDF")
                    registerAndOpen(viewModel, file, "Images to PDF", onOpenDocument); setResult("Created a PDF from ${uris.size} image(s).")
                }
                "PDF to Images" -> {
                    val files = PdfToolsAdvancedEngine.renderPdfToImages(context, uris.first(), outDir); if (files.isEmpty()) error("Could not render PDF")
                    setResult("Exported ${files.size} page images to PDF Studio storage.")
                }
                "PDF to Text" -> {
                    val text = PdfToolsEngine.pdfToText(context, uris.first()) ?: error("No extractable text found")
                    val file = File(outDir, "Text_${System.currentTimeMillis()}.txt")
                    FileOutputStream(file).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    val shareUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_STREAM, shareUri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share extracted text"))
                    setResult("Extracted ${text.length} characters and opened the share menu.")
                }
                "Remove Blank" -> {
                    val file = PdfToolsEngine.removeBlankPages(context, uris.first(), outDir) ?: error("No non-blank pages found")
                    registerAndOpen(viewModel, file, "No Blank Pages", onOpenDocument); setResult("Blank pages removed successfully.")
                }
                "Watermark" -> {
                    val file = PdfToolsEngine.addWatermark(context, uris.first(), "CONFIDENTIAL", outDir) ?: error("Watermark failed")
                    registerAndOpen(viewModel, file, "Watermarked", onOpenDocument); setResult("CONFIDENTIAL watermark added to every page.")
                }
                "Resize Pages" -> {
                    val file = PdfToolsAdvancedEngine.resizePages(context, uris.first(), resize ?: "A4", outDir) ?: error("Resize failed")
                    registerAndOpen(viewModel, file, "Resized", onOpenDocument); setResult("Pages resized to ${resize ?: "A4"}.")
                }
                "Print PDF" -> { PdfToolsEngine.printPdf(context, uris.first(), "PDF Studio Print"); setResult("Print dialog opened.") }
            }
        } catch (e: Exception) { setResult("${tool.name} failed: ${e.message ?: "Unknown error"}") }
        finally { setBusy("") }
    }
}

private suspend fun registerAndOpen(viewModel: PdfAppViewModel, file: File, tags: String, onOpenDocument: (DocumentEntity) -> Unit) {
    val doc = viewModel.repository.registerGeneratedPdf(file, tags) ?: error("Could not save output to library")
    onOpenDocument(doc)
}
