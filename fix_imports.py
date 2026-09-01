with open("app/src/main/java/com/example/ui/BatchStudioScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
"""import com.example.engine.PdfEngine""",
"""import com.example.engine.PdfEngine
import com.example.engine.PdfToolsEngine
import com.example.engine.PdfToolsAdvancedEngine
import androidx.core.net.toUri"""
)

with open("app/src/main/java/com/example/ui/BatchStudioScreen.kt", "w") as f:
    f.write(content)
