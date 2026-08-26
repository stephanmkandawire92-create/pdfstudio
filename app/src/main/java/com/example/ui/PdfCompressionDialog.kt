package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.DocumentEntity
import com.example.engine.PdfCompressionEngine
import com.example.engine.PdfCompressionEngine.CompressionConfig
import com.example.engine.PdfCompressionEngine.CompressionPreset
import com.example.engine.PdfCompressionEngine.CompressionReport
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PdfRed
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun PdfCompressionDialog(
    document: DocumentEntity,
    viewModel: PdfAppViewModel,
    onDismiss: () -> Unit,
    onOpenDocument: (DocumentEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPreset by remember { mutableStateOf(CompressionPreset.RECOMMENDED) }
    var customQuality by remember { mutableFloatStateOf(60f) }
    var customScale by remember { mutableFloatStateOf(0.85f) }
    var stripMetadata by remember { mutableStateOf(true) }
    var convertToGrayscale by remember { mutableStateOf(false) }

    var isCompressing by remember { mutableStateOf(false) }
    var progressCurrentPage by remember { mutableIntStateOf(0) }
    var progressTotalPages by remember { mutableIntStateOf(document.pageCount) }
    var compressionReport by remember { mutableStateOf<CompressionReport?>(null) }
    var createdDocEntity by remember { mutableStateOf<DocumentEntity?>(null) }

    val currentConfig = remember(selectedPreset, customQuality, customScale, stripMetadata, convertToGrayscale) {
        when (selectedPreset) {
            CompressionPreset.EXTREME -> CompressionConfig(
                preset = CompressionPreset.EXTREME,
                qualityPercent = CompressionPreset.EXTREME.defaultQuality,
                scaleFactor = CompressionPreset.EXTREME.scaleFactor,
                convertToGrayscale = convertToGrayscale,
                stripMetadata = stripMetadata
            )
            CompressionPreset.RECOMMENDED -> CompressionConfig(
                preset = CompressionPreset.RECOMMENDED,
                qualityPercent = CompressionPreset.RECOMMENDED.defaultQuality,
                scaleFactor = CompressionPreset.RECOMMENDED.scaleFactor,
                convertToGrayscale = convertToGrayscale,
                stripMetadata = stripMetadata
            )
            CompressionPreset.HIGH_QUALITY -> CompressionConfig(
                preset = CompressionPreset.HIGH_QUALITY,
                qualityPercent = CompressionPreset.HIGH_QUALITY.defaultQuality,
                scaleFactor = CompressionPreset.HIGH_QUALITY.scaleFactor,
                convertToGrayscale = convertToGrayscale,
                stripMetadata = stripMetadata
            )
            CompressionPreset.CUSTOM -> CompressionConfig(
                preset = CompressionPreset.CUSTOM,
                qualityPercent = customQuality.toInt(),
                scaleFactor = customScale,
                convertToGrayscale = convertToGrayscale,
                stripMetadata = stripMetadata
            )
        }
    }

    val (estimatedSize, estimatedSavingsPct) = remember(document.fileSize, currentConfig) {
        PdfCompressionEngine.estimateSavings(document.fileSize, currentConfig)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isCompressing) onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AccentEmerald.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Compress,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text("PDF Compression Utility", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Image optimization & metadata cleaner", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                if (compressionReport == null) {
                    // Document Summary Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PdfRed, modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(document.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${document.pageCount} Pages · ${formatFileSize(document.fileSize)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Preset Selection Label
                    Text("Select Compression Level", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))

                    // Presets List
                    CompressionPresetItem(
                        title = "🚀 Extreme Compression",
                        subtitle = "Smallest file size (~100 DPI images)",
                        badge = "-65% Size",
                        isSelected = selectedPreset == CompressionPreset.EXTREME,
                        onClick = { selectedPreset = CompressionPreset.EXTREME }
                    )
                    Spacer(Modifier.height(6.dp))
                    CompressionPresetItem(
                        title = "⭐ Recommended",
                        subtitle = "Balanced quality & size (~150 DPI)",
                        badge = "-45% Size",
                        isSelected = selectedPreset == CompressionPreset.RECOMMENDED,
                        onClick = { selectedPreset = CompressionPreset.RECOMMENDED }
                    )
                    Spacer(Modifier.height(6.dp))
                    CompressionPresetItem(
                        title = "💎 High Quality",
                        subtitle = "Max visual fidelity (~200 DPI)",
                        badge = "-25% Size",
                        isSelected = selectedPreset == CompressionPreset.HIGH_QUALITY,
                        onClick = { selectedPreset = CompressionPreset.HIGH_QUALITY }
                    )
                    Spacer(Modifier.height(6.dp))
                    CompressionPresetItem(
                        title = "⚙️ Custom Settings",
                        subtitle = "Manual quality, scale & color control",
                        badge = "Custom",
                        isSelected = selectedPreset == CompressionPreset.CUSTOM,
                        onClick = { selectedPreset = CompressionPreset.CUSTOM }
                    )

                    // Custom controls
                    AnimatedVisibility(visible = selectedPreset == CompressionPreset.CUSTOM) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text("Image Quality: ${customQuality.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Slider(
                                value = customQuality,
                                onValueChange = { customQuality = it },
                                valueRange = 15f..95f,
                                modifier = Modifier.height(32.dp)
                            )

                            Spacer(Modifier.height(6.dp))
                            Text("Resolution Scale: ${(customScale * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Slider(
                                value = customScale,
                                onValueChange = { customScale = it },
                                valueRange = 0.4f..1.2f,
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Advanced Optimization Toggles
                    Text("Optimization Features", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Strip Redundant Metadata", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Removes XML XMP packets, scan bloat & unused tags", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = stripMetadata,
                            onCheckedChange = { stripMetadata = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentEmerald, checkedTrackColor = AccentEmerald.copy(alpha = 0.5f))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Convert to Grayscale", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Removes color chrominance for up to 3x smaller files", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = convertToGrayscale,
                            onCheckedChange = { convertToGrayscale = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentEmerald, checkedTrackColor = AccentEmerald.copy(alpha = 0.5f))
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Estimated Reduction Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentEmerald.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    "Estimated Output: ~${formatFileSize(estimatedSize)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentEmerald
                                )
                                Text(
                                    "Expected reduction of ~$estimatedSavingsPct% in total file size",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (isCompressing) {
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { if (progressTotalPages > 0) progressCurrentPage.toFloat() / progressTotalPages else 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = AccentEmerald
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Compressing page $progressCurrentPage of $progressTotalPages...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // COMPRESSION REPORT COMPLETED
                    val report = compressionReport!!
                    if (report.isSuccess) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(AccentEmerald.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(34.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Compression Successful!", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AccentEmerald)
                            Text(
                                "Saved ${formatFileSize(report.savedBytes)} (${String.format(Locale.getDefault(), "%.1f", report.reductionPercentage)}% reduction)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(16.dp))

                            // Comparison Card
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Original Size:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatFileSize(report.originalSizeBytes), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Compressed Size:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatFileSize(report.compressedSizeBytes), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentEmerald)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Pages:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${report.pageCount} pgs", fontSize = 12.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Processing Time:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${report.durationMs} ms", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Error View
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Compression Failed", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Text(report.errorMessage ?: "Unknown error occurred.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (compressionReport == null) {
                Button(
                    onClick = {
                        isCompressing = true
                        scope.launch {
                            val origFile = File(document.filePath)
                            val outDir = File(context.filesDir, "user_pdfs")
                            outDir.mkdirs()
                            val cleanTitle = "${document.title.removeSuffix(".pdf")}_compressed_${System.currentTimeMillis() % 10000}"
                            val outFile = File(outDir, "$cleanTitle.pdf")

                            val report = PdfCompressionEngine.compressPdfDocument(
                                context = context,
                                inputFile = origFile,
                                outputFile = outFile,
                                config = currentConfig,
                                onProgress = { cur, tot ->
                                    progressCurrentPage = cur
                                    progressTotalPages = tot
                                }
                            )

                            isCompressing = false
                            compressionReport = report

                            if (report.isSuccess && report.compressedFile != null) {
                                val newDoc = DocumentEntity(
                                    title = report.compressedFile.nameWithoutExtension,
                                    filePath = report.compressedFile.absolutePath,
                                    pageCount = report.pageCount,
                                    fileSize = report.compressedSizeBytes,
                                    tags = "Compressed"
                                )
                                createdDocEntity = newDoc
                                viewModel.repository.updateDocument(newDoc)
                                Toast.makeText(context, "Saved compressed PDF (${formatFileSize(report.compressedSizeBytes)})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isCompressing,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    modifier = Modifier.testTag("start_compression_dialog_button")
                ) {
                    if (isCompressing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Optimizing...")
                    } else {
                        Icon(Icons.Default.Compress, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compress PDF")
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val report = compressionReport
                    if (report?.isSuccess == true && createdDocEntity != null) {
                        OutlinedButton(
                            onClick = {
                                val file = File(createdDocEntity!!.filePath)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Compressed PDF"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                createdDocEntity?.let { onOpenDocument(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open PDF", fontSize = 12.sp)
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (compressionReport == null && !isCompressing) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun CompressionPresetItem(
    title: String,
    subtitle: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AccentEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) AccentEmerald else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) AccentEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ) {
                Text(
                    text = badge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}
