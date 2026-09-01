import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

content = re.sub(r'<uses-permission android:name="android\.permission\.READ_EXTERNAL_STORAGE".*?/>\n', '', content)
content = re.sub(r'<uses-permission android:name="android\.permission\.READ_MEDIA_IMAGES".*?/>\n', '', content)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
