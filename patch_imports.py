with open("app/src/main/java/com/example/ui/PdfCompressionDialog.kt", "r") as f:
    content = f.read()

if "import com.example.util.AdManager" not in content:
    content = content.replace("package com.example.ui", "package com.example.ui\n\nimport com.example.util.AdManager\nimport com.example.util.findActivity")

with open("app/src/main/java/com/example/ui/PdfCompressionDialog.kt", "w") as f:
    f.write(content)
