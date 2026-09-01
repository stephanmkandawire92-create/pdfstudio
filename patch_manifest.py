with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

content = content.replace(
    'android:name="com.google.android.gms.ads.APPLICATION_ID"\n            android:value="ca-app-pub-3940256099942544~3347511713"',
    'android:name="com.google.android.gms.ads.APPLICATION_ID"\n            android:value="ca-app-pub-4641751826914800~5300658596"'
)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
