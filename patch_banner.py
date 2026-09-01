import re

with open("app/src/main/java/com/example/ui/AdMobBanner.kt", "r") as f:
    content = f.read()

content = content.replace('adUnitId = "ca-app-pub-3940256099942544/6300978111"', 'adUnitId = com.example.util.AdConfig.bannerAdUnitId')

with open("app/src/main/java/com/example/ui/AdMobBanner.kt", "w") as f:
    f.write(content)
