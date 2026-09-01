import re

with open("app/src/main/java/com/example/ui/BatchStudioScreen.kt", "r") as f:
    content = f.read()

# Fix duplicate imports
content = re.sub(r"import com.example.engine.PdfToolsEngine\n", "", content)
content = re.sub(r"import com.example.engine.PdfToolsAdvancedEngine\n", "", content)
content = re.sub(r"import androidx.core.net.toUri\n", "", content)

# Add them back exactly once
content = content.replace("import com.example.engine.PdfEngine", 
        "import com.example.engine.PdfEngine\nimport com.example.engine.PdfToolsEngine\nimport com.example.engine.PdfToolsAdvancedEngine")

# Fix `toUri()` to `Uri.fromFile()`
content = content.replace("androidx.core.net.toUri(origFile)", "Uri.fromFile(origFile)")
content = content.replace("it.toUri()", "Uri.fromFile(it)")

with open("app/src/main/java/com/example/ui/BatchStudioScreen.kt", "w") as f:
    f.write(content)
