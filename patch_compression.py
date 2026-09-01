with open("app/src/main/java/com/example/ui/PdfCompressionDialog.kt", "r") as f:
    content = f.read()

# Add imports
if "import com.example.util.AdManager" not in content:
    content = content.replace("package com.example.ui", "package com.example.ui\n\nimport com.example.util.AdManager\nimport com.example.util.findActivity")

old_block = """                    onClick = {
                        isCompressing = true
                        scope.launch {"""

new_block = """                    onClick = {
                        val activity = context.findActivity()
                        if (activity != null) {
                            AdManager.showRewarded(
                                activity = activity,
                                onRewardEarned = {
                                    isCompressing = true
                                    scope.launch {
                                },
                                onAdDismissed = {
                                    // Ad dismissed
                                }
                            )
                        } else {
                            isCompressing = true
                            scope.launch {"""
# We can't just inject that easily because of brace matching. 
