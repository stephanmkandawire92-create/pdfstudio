with open("app/src/main/java/com/example/ui/CloudSyncDialog.kt", "r") as f:
    content = f.read()

content = content.replace("com.example.BuildConfig.GOOGLE_WEB_CLIENT_ID", "context.getString(com.example.R.string.default_web_client_id)")

with open("app/src/main/java/com/example/ui/CloudSyncDialog.kt", "w") as f:
    f.write(content)
