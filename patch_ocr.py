import re

with open("app/src/main/java/com/example/engine/PdfEngine.kt", "r") as f:
    content = f.read()

# Make sure to import ML Kit stuff
if "com.google.mlkit.vision.text" not in content:
    content = content.replace(
        "import android.util.Log",
        "import android.util.Log\nimport com.google.mlkit.vision.common.InputImage\nimport com.google.mlkit.vision.text.TextRecognition\nimport com.google.mlkit.vision.text.latin.TextRecognizerOptions\nimport kotlinx.coroutines.tasks.await"
    )

# Replace performOcr
old_ocr = r"suspend fun performOcr.*?return sb\.toString\(\)\n    \}"
new_ocr = """suspend fun performOcr(bitmap: Bitmap): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = recognizer.process(image).await()
            if (result.text.isBlank()) {
                "No text could be found on this page."
            } else {
                result.text
            }
        } catch (e: Exception) {
            Log.e(TAG, "OCR Failed", e)
            "Error extracting text: ${e.message}"
        }
    }"""

content = re.sub(old_ocr, new_ocr, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/engine/PdfEngine.kt", "w") as f:
    f.write(content)
