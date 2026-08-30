# PDFStudio

An advanced Android application for readers, built with Kotlin and Jetpack Compose. PDFStudio provides seamless PDF viewing, annotation, and management capabilities for a superior reading experience.

## Features

- 📄 **PDF Viewing** - Fast and responsive PDF rendering with smooth scrolling
- 🎨 **Modern UI** - Built with Jetpack Compose for a fluid, native Android experience
- 🔧 **Annotation Tools** - Mark up and annotate PDFs with ease
- 💾 **Local Storage** - Secure document management with Room database
- 🔍 **Search & Navigation** - Quickly find content within your documents
- 🌙 **Dark Mode Support** - Eye-friendly reading in any lighting condition
- 🔐 **Firebase Integration** - Optional cloud backup and authentication
- 📱 **Responsive Design** - Optimized for phones and tablets

## Tech Stack

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **PDF Library**: PDFBox Android
- **Authentication**: Firebase Auth + Google Sign-In
- **Cloud Services**: Firebase Firestore, Firebase AI
- **Image Loading**: Coil
- **Serialization**: Moshi

## Requirements

- Android SDK 24+ (minimum)
- Target SDK 36
- Java 11 compatible
- Android Studio Hedgehog or later (recommended)

## Getting Started

### Prerequisites

1. Clone the repository:
```bash
git clone https://github.com/stephanmkandawire92-create/pdfstudio.git
cd pdfstudio
```

2. Set up environment variables (optional for Firebase):
Create a `.env` file in the project root:
```
KEYSTORE_PATH=/path/to/your/keystore.jks
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
```

### Building

#### Debug Build
```bash
./gradlew assembleDebug
```

#### Release Build
Ensure you have a keystore configured, then:
```bash
./gradlew assembleRelease
```

### Running Tests

```bash
# Unit tests
./gradlew test

# UI tests
./gradlew connectedAndroidTest
```

## Project Structure

```
pdfstudio/
├── app/                          # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/             # Kotlin source code
│   │   │   └── res/              # Resources (layouts, strings, etc.)
│   │   ├── test/                 # Unit tests
│   │   └── androidTest/          # Instrumented tests
│   └── build.gradle.kts          # App-level build configuration
├── build.gradle.kts              # Project-level build configuration
├── settings.gradle.kts           # Gradle settings
└── gradle/                       # Gradle wrapper and plugins
```

## Development Guide

### Key Dependencies

- **androidx-compose-bom**: Compose UI framework
- **firebase-bom**: Firebase services
- **pdfbox-android**: PDF rendering and manipulation
- **room**: Local database persistence
- **retrofit**: REST API client

### Security

- Sensitive credentials are managed via `.env` files (not committed)
- Firebase App Check enabled for API protection
- Keystore secrets handled through environment variables

## Customization

### Branding

Update the following in `app/build.gradle.kts`:
- `applicationId` - Your app's unique package name
- `versionCode` / `versionName` - Version information
- `namespace` - Package namespace for resources

### Firebase Setup (Optional)

1. Create a Firebase project at [firebase.google.com](https://firebase.google.com)
2. Place `google-services.json` in the `app/` directory
3. Uncomment relevant Firebase dependencies in `app/build.gradle.kts`

## Performance Optimization

- PNG crunching disabled for faster builds (`isCrunchPngs = false`)
- Proguard enabled in release builds with custom rules
- Android resources excluded from APK
- Dependency analysis included for bundle optimization

## Testing

The project includes comprehensive testing support:
- **Unit Testing**: JUnit 4 + Robolectric
- **UI Testing**: Espresso + Compose test framework
- **Screenshot Testing**: Roborazzi for visual regression testing

## Contributing

We welcome contributions! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support & Contact

- **Issues**: [GitHub Issues](https://github.com/stephanmkandawire92-create/pdfstudio/issues)
- **Discussions**: [GitHub Discussions](https://github.com/stephanmkandawire92-create/pdfstudio/discussions)
- **Author**: [stephanmkandawire92-create](https://github.com/stephanmkandawire92-create)

## Roadmap

- [ ] Text selection and copy from PDFs
- [ ] Bookmarks and table of contents
- [ ] PDF encryption/decryption support
- [ ] Advanced search with regex
- [ ] Collaborative annotations
- [ ] Export to other formats
- [ ] Offline sync support

## Acknowledgments

- Built with [Android Studio Template](https://github.com/google-gemini/aistudio-repository-template)
- PDF processing powered by [PDFBox Android](https://pdfbox.apache.org/)
- UI framework: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Google AI integration via [Firebase Gemini API](https://firebase.google.com/docs/generate/overview)

---

**Made with ❤️ by stephanmkandawire92-create**
