import re

with open("app/src/main/java/com/example/ui/BatchStudioScreen.kt", "r") as f:
    content = f.read()

# Make sure we import the engines and core stuff
if "PdfToolsEngine" not in content:
    content = content.replace("import com.example.engine.PdfEngine", 
        "import com.example.engine.PdfEngine\nimport com.example.engine.PdfToolsEngine\nimport com.example.engine.PdfToolsAdvancedEngine\nimport androidx.core.net.toUri")

# 1. MERGE_PDFS
# Old: val files = selectedDocIds.mapNotNull { id -> allDocs.find { it.id == id }?.filePath?.let { File(it) } } ... val success = PdfEngine.mergePdfs(files, outFile)
old_merge = r"val files = selectedDocIds.*?val success = PdfEngine\.mergePdfs\(files, outFile\)\n\s*isProcessing = false\n\s*if \(success\) \{.*?onOpenDocument\(newDoc\)\n\s*\}"
new_merge = """val files = selectedDocIds.mapNotNull { id -> allDocs.find { it.id == id }?.filePath?.let { File(it) } }
                                val uris = files.map { it.toUri() }
                                val outDir = File(context.filesDir, "user_pdfs")
                                outDir.mkdirs()
                                val outFile = PdfToolsEngine.mergePdfs(context, uris, outDir)
                                isProcessing = false
                                if (outFile != null) {
                                    val newDoc = viewModel.repository.registerGeneratedPdf(outFile, "Merged")
                                    if (newDoc != null) {
                                        Toast.makeText(context, "Merged successfully!", Toast.LENGTH_SHORT).show()
                                        onOpenDocument(newDoc)
                                    }
                                }"""
content = re.sub(old_merge, new_merge, content, flags=re.DOTALL)

# 2. COMPRESS_PDF
# Keep the existing Compression tool since it works, BUT wait, let's verify if registerGeneratedPdf could be used instead of manual count.
# The user said "Batch Studio features are not doing the right job", so the primary issue is the other features that render out to bitmaps.

# 3. SPLIT_PDF
old_split = r"val origFile = File\(doc\.filePath\).*?val success = PdfEngine\.splitPdf\(origFile, pages, outFile\)\n\s*isProcessing = false\n\s*if \(success\) \{.*?onOpenDocument\(newDoc\)\n\s*\}"
new_split = """val origFile = File(doc.filePath)
                                    val outDir = File(context.filesDir, "user_pdfs")
                                    outDir.mkdirs()
                                    val outFile = PdfToolsEngine.extractPages(context, origFile.toUri(), pages, outDir)
                                    isProcessing = false
                                    if (outFile != null) {
                                        val newDoc = viewModel.repository.registerGeneratedPdf(outFile, "Extracted")
                                        if (newDoc != null) {
                                            Toast.makeText(context, "Extracted ${pages.size} pages!", Toast.LENGTH_SHORT).show()
                                            onOpenDocument(newDoc)
                                        }
                                    }"""
content = re.sub(old_split, new_split, content, flags=re.DOTALL)

# 4. PDF_TO_IMAGES
old_export = r"val origFile = File\(doc\.filePath\).*?val exported = PdfEngine\.exportPdfToImages\(origFile, outDir\)\n\s*isProcessing = false\n\s*Toast\.makeText\(context, \"Exported \$\{exported\.size\} PNG images to device!\", Toast\.LENGTH_SHORT\)\.show\(\)"
new_export = """val origFile = File(doc.filePath)
                                    val outDir = File(context.filesDir, "exported_images_${System.currentTimeMillis() % 10000}")
                                    outDir.mkdirs()
                                    val exported = PdfToolsAdvancedEngine.renderPdfToImages(context, origFile.toUri(), outDir)
                                    isProcessing = false
                                    Toast.makeText(context, "Exported ${exported.size} images to device!", Toast.LENGTH_SHORT).show()"""
content = re.sub(old_export, new_export, content, flags=re.DOTALL)

# 5. WATERMARK_PDF
old_watermark = r"val origFile = File\(doc\.filePath\).*?val success = PdfEngine\.exportAnnotatedPdf\(origFile, emptyList\(\), outFile, watermarkText\)\n\s*isProcessing = false\n\s*if \(success\) \{.*?onOpenDocument\(newDoc\)\n\s*\}"
new_watermark = """val origFile = File(doc.filePath)
                                    val outDir = File(context.filesDir, "user_pdfs")
                                    outDir.mkdirs()
                                    val outFile = PdfToolsEngine.addWatermark(context, origFile.toUri(), watermarkText, outDir)
                                    isProcessing = false
                                    if (outFile != null) {
                                        val newDoc = viewModel.repository.registerGeneratedPdf(outFile, "Watermarked")
                                        if (newDoc != null) {
                                            Toast.makeText(context, "Watermark stamped successfully!", Toast.LENGTH_SHORT).show()
                                            onOpenDocument(newDoc)
                                        }
                                    }"""
content = re.sub(old_watermark, new_watermark, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/BatchStudioScreen.kt", "w") as f:
    f.write(content)

