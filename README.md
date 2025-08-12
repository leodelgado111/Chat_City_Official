# Chat City Official

An Android application featuring location-based chat functionality powered by Mapbox.

## 📱 Overview

Chat City Official is a mobile application that combines real-time messaging with location services, allowing users to engage in location-aware conversations and explore chat communities based on geographic proximity.

## 🚀 Features

- **Location-Based Chat**: Connect with users in your area
- **Mapbox Integration**: Interactive maps and location services
- **Real-Time Messaging**: Instant communication with other users
- **Android Native**: Built for Android devices using Kotlin

## 🛠️ Tech Stack

- **Platform**: Android
- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL)
- **Maps**: Mapbox SDK
- **IDE**: Android Studio / IntelliJ IDEA

## 📋 Prerequisites

- Android Studio (latest version recommended)
- JDK 11 or higher
- Android SDK
- Mapbox API key

## 🔧 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/leodelgado111/Chat_City_Official.git
   cd Chat_City_Official
   ```

2. Open the project in Android Studio

3. Add your Mapbox API key:
   - Create a `secrets.properties` file in the root directory (if not exists)
   - Add your Mapbox token:
     ```properties
     MAPBOX_ACCESS_TOKEN=your_mapbox_access_token_here
     ```

4. Sync the project with Gradle files

5. Build and run the application on an emulator or physical device

## 🏗️ Project Structure

```
Chat_City_Official/
├── app/                    # Main application module
├── gradle/                 # Gradle wrapper files
├── .gradle/               # Gradle build cache
├── .idea/                 # IntelliJ IDEA settings
├── .vscode/               # VS Code settings
├── build.gradle.kts       # Root build configuration
├── settings.gradle.kts    # Gradle settings
├── gradle.properties      # Gradle properties
├── local.properties       # Local configuration
└── mapbox_logs.txt       # Mapbox debug logs
```

## 🔑 Configuration

### Mapbox Setup

1. Sign up for a [Mapbox account](https://account.mapbox.com/auth/signup/)
2. Create an access token in your Mapbox account dashboard
3. Add the token to your project as described in the installation steps

### Local Properties

The `local.properties` file should contain:
- Android SDK location
- Other local environment-specific settings

## 🧪 Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## 📱 Minimum Requirements

- Android API Level: [Specify minimum SDK version]
- Target API Level: [Specify target SDK version]
- Device: GPS-enabled Android device recommended

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is currently not licensed. Please contact the repository owner for usage rights.

## 👤 Author

**leodelgado111**

- GitHub: [@leodelgado111](https://github.com/leodelgado111)

## 🐛 Bug Reports

If you discover any bugs, please create an issue [here](https://github.com/leodelgado111/Chat_City_Official/issues).

## 📧 Contact

For questions or support, please open an issue in the GitHub repository.

---

**Note**: This project is under active development. Features and documentation may change.
