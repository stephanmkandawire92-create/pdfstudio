with open("app/src/main/java/com/example/ui/ToolsScreenV2.kt", "r") as f:
    content = f.read()

content = content.replace(
"""        outputText?.let { message ->
            AlertDialog(onDismissRequest = { outputText = null }, title = { Text("PDF Studio") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { outputText = null }) { Text("OK") } })
        }
    }
}

private fun iconFor""",
"""        outputText?.let { message ->
            AlertDialog(onDismissRequest = { outputText = null }, title = { Text("PDF Studio") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { outputText = null }) { Text("OK") } })
        }
    }

private fun iconFor"""
)

with open("app/src/main/java/com/example/ui/ToolsScreenV2.kt", "w") as f:
    f.write(content)
