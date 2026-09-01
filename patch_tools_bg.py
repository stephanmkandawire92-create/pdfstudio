import re

with open("app/src/main/java/com/example/ui/ToolsScreenV2.kt", "r") as f:
    content = f.read()

imports = """
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.theme.PdfRed
import com.example.ui.theme.PdfRedDark
import androidx.compose.foundation.layout.Box
"""
content = content.replace("import androidx.compose.foundation.layout.Column", "import androidx.compose.foundation.layout.Column" + imports)

# We use regex to replace the Scaffold to avoid exact string matching issues.
old_scaffold_regex = r"Scaffold\(topBar.*?\) \{ padding ->\n.*?if \(\!busy.*?\) \{.*?\} else \{.*?\}\n        \}"

new_scaffold = """Scaffold(topBar = {
        TopAppBar(
            title = { Text("PDF Toolbox", fontWeight = FontWeight.Bold) }, 
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PdfRed,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
    }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(PdfRed, PdfRedDark)))
                .padding(padding)
        ) {
            if (!busy.isNullOrBlank()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Color.White); Spacer(Modifier.height(16.dp)); Text("Processing $busy…", color = Color.White)
                }
            } else {
                LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        }"""

content = re.sub(old_scaffold_regex, new_scaffold, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/ToolsScreenV2.kt", "w") as f:
    f.write(content)
