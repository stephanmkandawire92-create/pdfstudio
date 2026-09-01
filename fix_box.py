with open("app/src/main/java/com/example/ui/ToolsScreenV2.kt", "r") as f:
    content = f.read()

old_tail = """            }
        }

        if (pageDialog) AlertDialog"""

new_tail = """            }
        }
        }

        if (pageDialog) AlertDialog"""

content = content.replace(old_tail, new_tail)

with open("app/src/main/java/com/example/ui/ToolsScreenV2.kt", "w") as f:
    f.write(content)
