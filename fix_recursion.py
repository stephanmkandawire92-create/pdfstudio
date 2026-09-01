with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

bad_block = """    val openDocumentWithAd = { doc: com.example.data.DocumentEntity ->
        val activity = com.example.util.findActivity(context)
        if (activity != null) {
            com.example.util.AdManager.showInterstitial(activity) { openDocumentWithAd(doc) }
        } else {
            openDocumentWithAd(doc)
        }
    }"""

good_block = """    val openDocumentWithAd = { doc: com.example.data.DocumentEntity ->
        val activity = com.example.util.findActivity(context)
        if (activity != null) {
            com.example.util.AdManager.showInterstitial(activity) { onOpenDocument(doc) }
        } else {
            onOpenDocument(doc)
        }
    }"""

content = content.replace(bad_block, good_block)

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)
