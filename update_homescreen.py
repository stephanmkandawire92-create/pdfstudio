import re

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

# Add the wrapper definition
wrapper_code = """
    var isSearchActive by remember { mutableStateOf(false) }

    val openDocumentWithAd = { doc: com.example.data.DocumentEntity ->
        val activity = com.example.util.findActivity(context)
        if (activity != null) {
            com.example.util.AdManager.showInterstitial(activity) { onOpenDocument(doc) }
        } else {
            onOpenDocument(doc)
        }
    }
"""
content = content.replace("    var isSearchActive by remember { mutableStateOf(false) }", wrapper_code)

# Replace the calls
content = re.sub(r'onOpenDocument\((doc|toOpen|compressedDoc)\)', r'openDocumentWithAd(\1)', content)

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)

