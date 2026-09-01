import re

with open("app/src/main/java/com/example/ui/BatchStudioScreen.kt", "r") as f:
    content = f.read()

launcher_code = """
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            scope.launch {
                var importedCount = 0
                for (u in uris) {
                    var displayName: String? = null
                    val cursor = context.contentResolver.query(u, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                displayName = it.getString(nameIndex)
                            }
                        }
                    }
                    val doc = viewModel.repository.importPdfFromUri(u, displayName)
                    if (doc != null) {
                        importedCount++
                        if (activeMode != BatchToolMode.MENU && uris.size == 1) {
                            selectedDocIds.clear()
                            selectedDocIds.add(doc.id)
                        } else if (activeMode == BatchToolMode.MERGE_PDFS) {
                            selectedDocIds.add(doc.id)
                        }
                    }
                }
                isProcessing = false
                Toast.makeText(context, "Imported $importedCount PDFs", Toast.LENGTH_SHORT).show()
            }
        }
    }
"""

content = content.replace("    // Image Picker for Batch Images to PDF", launcher_code + "\n    // Image Picker for Batch Images to PDF")

# Add the Import Card to MENU
import_card = """
                        item {
                            BatchToolCard(
                                title = "📥 Import PDFs from Device",
                                subtitle = "Browse your phone's storage to add external PDFs into the Studio.",
                                icon = Icons.Default.Add,
                                iconColor = androidx.compose.ui.graphics.Color(0xFF059669),
                                onClick = { pdfPickerLauncher.launch("application/pdf") }
                            )
                        }
"""
content = content.replace("                    LazyColumn(\n                        verticalArrangement = Arrangement.spacedBy(10.dp)\n                    ) {", "                    LazyColumn(\n                        verticalArrangement = Arrangement.spacedBy(10.dp)\n                    ) {\n" + import_card)

# Add Import button to MERGE_PDFS
merge_btn = """
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Browse Device for PDFs")
                        }"""
content = content.replace("                            label = { Text(\"Merged PDF Name\") },\n                            singleLine = true,\n                            modifier = Modifier.fillMaxWidth()\n                        )\n\n                        Spacer(Modifier.height(10.dp))", "                            label = { Text(\"Merged PDF Name\") },\n                            singleLine = true,\n                            modifier = Modifier.fillMaxWidth()\n                        )\n" + merge_btn + "\n                        Spacer(Modifier.height(10.dp))")

# Add Import button to COMPRESS_PDF
compress_btn = """
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Browse Device for PDFs")
                        }"""
content = content.replace("                        Text(\"Select a Document to Compress\", fontWeight = FontWeight.Bold, fontSize = 14.sp)\n                        Spacer(Modifier.height(8.dp))", "                        Text(\"Select a Document to Compress\", fontWeight = FontWeight.Bold, fontSize = 14.sp)\n" + compress_btn + "\n                        Spacer(Modifier.height(8.dp))")

# Add Import button to SPLIT_PDF
split_btn = """
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Browse Device for PDFs")
                        }"""
content = content.replace("                        Text(\"Select a Document to Extract Pages From\", fontWeight = FontWeight.Bold, fontSize = 14.sp)\n                        Spacer(Modifier.height(8.dp))", "                        Text(\"Select a Document to Extract Pages From\", fontWeight = FontWeight.Bold, fontSize = 14.sp)\n" + split_btn + "\n                        Spacer(Modifier.height(8.dp))")

# Add Import button to PDF_TO_IMAGES
images_btn = """
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Browse Device for PDFs")
                        }"""
content = content.replace("                        Text(\"Select Document to Export as Images\", fontWeight = FontWeight.Bold, fontSize = 14.sp)\n                        Spacer(Modifier.height(10.dp))", "                        Text(\"Select Document to Export as Images\", fontWeight = FontWeight.Bold, fontSize = 14.sp)\n" + images_btn + "\n                        Spacer(Modifier.height(10.dp))")

# Add Import button to WATERMARK_PDF
watermark_btn = """
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Browse Device for PDFs")
                        }"""
content = content.replace("                        Text(\"Select Document & Watermark Text\", fontWeight = FontWeight.Bold, fontSize = 14.sp)\n                        Spacer(Modifier.height(8.dp))", "                        Text(\"Select Document & Watermark Text\", fontWeight = FontWeight.Bold, fontSize = 14.sp)\n" + watermark_btn + "\n                        Spacer(Modifier.height(8.dp))")

with open("app/src/main/java/com/example/ui/BatchStudioScreen.kt", "w") as f:
    f.write(content)

