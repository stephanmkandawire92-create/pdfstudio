with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

good_block = """    val openDocumentWithAd = { doc: com.example.data.DocumentEntity ->
        val activity = com.example.util.findActivity(context)
        if (activity != null) {
            com.example.util.AdManager.showInterstitial(activity) { onOpenDocument(doc) }
        } else {
            onOpenDocument(doc)
        }
    }

    val navigateScannerWithAd = {
        val activity = com.example.util.findActivity(context)
        if (activity != null) {
            com.example.util.AdManager.showInterstitial(activity) { onNavigateScanner() }
        } else {
            onNavigateScanner()
        }
    }"""

content = content.replace("""    val openDocumentWithAd = { doc: com.example.data.DocumentEntity ->
        val activity = com.example.util.findActivity(context)
        if (activity != null) {
            com.example.util.AdManager.showInterstitial(activity) { onOpenDocument(doc) }
        } else {
            onOpenDocument(doc)
        }
    }""", good_block)

content = content.replace("onClick = onNavigateScanner", "onClick = navigateScannerWithAd")

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)
