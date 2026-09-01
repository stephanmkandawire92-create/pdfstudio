import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace(
    "com.example.util.AdManager.loadInterstitial(this)",
    "com.example.util.AdManager.loadInterstitial(this)\n        com.example.util.AdManager.loadRewarded(this)"
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
