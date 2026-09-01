with open("app/build.gradle.kts", "r") as f:
    content = f.read()

if "com.google.mlkit:text-recognition" not in content:
    content = content.replace(
        "dependencies {",
        "dependencies {\n  implementation(\"com.google.mlkit:text-recognition:16.0.0\")"
    )

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
