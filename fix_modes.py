import re

with open("app/src/main/java/com/example/ui/BatchStudioScreen.kt", "r") as f:
    content = f.read()

# Replace everything from BatchToolMode.COMPRESS_PDF -> { to the end of the when block
old_pattern = r"BatchToolMode\.COMPRESS_PDF -> \{.*?Stamp Watermark onto PDF\"\)\n\s*\}\n\s*\}\n\s*\}"
new_pattern = """BatchToolMode.COMPRESS_PDF -> {
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
                        androidx.compose.material3.Slider(
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
                                    val config = com.example.engine.PdfCompressionEngine.CompressionConfig(
                                        qualityPercent = compressionQuality.toInt()
                                    )
                                    val report = com.example.engine.PdfCompressionEngine.compressPdfDocument(
                                        context = context,
                                        inputFile = origFile,
                                        outputFile = outFile,
                                        config = config
                                    )
                                    isProcessing = false
                                    if (report.isSuccess && report.compressedFile != null) {
                                        val newDoc = viewModel.repository.registerGeneratedPdf(report.compressedFile, "Compressed")
                                        if (newDoc != null) {
                                            Toast.makeText(context, "Compressed successfully!", Toast.LENGTH_SHORT).show()
                                            onOpenDocument(newDoc)
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessing && selectedDocIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_compress_button")
                        ) {
                            Text("Compress PDF")
                        }
                    }
                }

                BatchToolMode.SPLIT_PDF -> {
                    Column {
                        Text("Select a Document to Extract Pages From", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = splitPageRange,
                            onValueChange = { splitPageRange = it },
                            label = { Text("Pages to Extract (e.g. 1, 3, 5)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            selectedDocIds.clear()
                                            selectedDocIds.add(doc.id)
                                        }
                                        .border(1.dp, if (isSelected) PdfRed else Color.Transparent, RoundedCornerShape(10.dp))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CallSplit, contentDescription = null, tint = PdfRed)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${doc.pageCount} Pages • ${(doc.fileSize / 1024)} KB", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = PdfRed)
                                    }
                                }
                            }
                        }
                        
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
                                    val outFile = PdfToolsEngine.extractPages(context, androidx.core.net.toUri(origFile), pages, outDir)
                                    isProcessing = false
                                    if (outFile != null) {
                                        val newDoc = viewModel.repository.registerGeneratedPdf(outFile, "Extracted")
                                        if (newDoc != null) {
                                            Toast.makeText(context, "Extracted ${pages.size} pages!", Toast.LENGTH_SHORT).show()
                                            onOpenDocument(newDoc)
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessing && selectedDocIds.isNotEmpty() && splitPageRange.isNotBlank(),
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
                        Spacer(Modifier.height(10.dp))

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allDocs) { doc ->
                                val isSelected = selectedDocIds.contains(doc.id)
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) androidx.compose.ui.graphics.Color(0xFF7C3AED).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDocIds.clear()
                                            selectedDocIds.add(doc.id)
                                        }
                                        .border(1.dp, if (isSelected) androidx.compose.ui.graphics.Color(0xFF7C3AED) else Color.Transparent, RoundedCornerShape(10.dp))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF7C3AED))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${doc.pageCount} Pages • ${(doc.fileSize / 1024)} KB", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF7C3AED))
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
                                    outDir.mkdirs()
                                    val exported = PdfToolsAdvancedEngine.renderPdfToImages(context, androidx.core.net.toUri(origFile), outDir)
                                    isProcessing = false
                                    Toast.makeText(context, "Exported ${exported.size} PNG images to device!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isProcessing && selectedDocIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF7C3AED)),
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
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) androidx.compose.ui.graphics.Color(0xFF0891B2).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDocIds.clear()
                                            selectedDocIds.add(doc.id)
                                        }
                                        .border(1.dp, if (isSelected) androidx.compose.ui.graphics.Color(0xFF0891B2) else Color.Transparent, RoundedCornerShape(10.dp))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF0891B2))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${doc.pageCount} Pages • ${(doc.fileSize / 1024)} KB", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF0891B2))
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
                                    val outFile = PdfToolsEngine.addWatermark(context, androidx.core.net.toUri(origFile), watermarkText, outDir)
                                    isProcessing = false
                                    if (outFile != null) {
                                        val newDoc = viewModel.repository.registerGeneratedPdf(outFile, "Watermarked")
                                        if (newDoc != null) {
                                            Toast.makeText(context, "Watermark stamped successfully!", Toast.LENGTH_SHORT).show()
                                            onOpenDocument(newDoc)
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessing && selectedDocIds.isNotEmpty() && watermarkText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0891B2)),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_watermark_button")
                        ) {
                            Text("Stamp Watermark onto PDF")
                        }
                    }
                }"""

content = re.sub(old_pattern, new_pattern, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/BatchStudioScreen.kt", "w") as f:
    f.write(content)

