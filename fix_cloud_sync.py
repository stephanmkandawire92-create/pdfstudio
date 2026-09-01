with open("app/src/main/java/com/example/ui/CloudSyncDialog.kt", "r") as f:
    content = f.read()

content = content.replace("viewModel.signInWithGoogle { success, error ->", "viewModel.signInWithGoogle(com.example.BuildConfig.GOOGLE_WEB_CLIENT_ID) { success, error ->")

with open("app/src/main/java/com/example/ui/CloudSyncDialog.kt", "w") as f:
    f.write(content)
