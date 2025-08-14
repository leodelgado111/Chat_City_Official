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

## 📝 Technical Implementation Details

### Navigation Bar Behavior
**File**: `MainActivity.kt`
- **Outline Movement**: The navigation bar has a selection outline that moves between icons
- **Create Button Special Behavior**: When the create button (center icon) is tapped, it does NOT move the outline and currently performs no action (placeholder for future functionality)
- **Active Navigation Items**: Only 4 icons move the outline: Saved, Home, Chats, Profile
- **Animation**: 100ms ValueAnimator for smooth outline transitions
- **Position Calculations**: Based on SVG icon positions with precise dp offsets

### Map Configuration
**File**: `HomeFragment.kt`

#### Location Display
- **Location Puck**: Custom pulse animation instead of default Mapbox location component
- **Single Annotation Manager**: Uses one `CircleAnnotationManager` instance to prevent duplicate markers
- **No Default Component**: `mapView.location.enabled = false` to disable Mapbox's built-in location indicator

#### Gesture Controls
- **Rotation Disabled**: `mapView.gestures.rotateEnabled = false` - Map stays north-oriented
- **Pinch Zoom Enabled**: `mapView.gestures.pinchToZoomEnabled = true` - Users can zoom with two fingers
- **Pan Enabled**: `mapView.gestures.scrollEnabled = true` - Users can drag to move around map

#### Custom Location Pulse Animation
- **Visual Design**: Single expanding ring (no center dot, no accuracy circle)
- **Color**: #FB86BB with 20% opacity (pink from gradient)
- **Size**: Expands from 3.5 to 35 meters radius (30% smaller than original 5-50m)
- **Duration**: 2-second animation cycle
- **Behavior**: Continuous loop with fade effect (50% to 0% opacity)
- **Blur**: 0.3 blur value for softer edges
- **Performance**: Proper lifecycle management (pause/resume with fragment lifecycle)

#### Location Updates
- **Update Interval**: 10 seconds
- **Fastest Interval**: 5 seconds
- **Priority**: `LocationRequest.PRIORITY_HIGH_ACCURACY`
- **Geocoding**: Converts coordinates to city names with fallback (locality > subAdminArea > adminArea)

### Google Places Search (Currently Disabled)
**Files preserved for future use**:
- `PlaceSearchAdapter.kt` - RecyclerView adapter for search results
- `fragment_home_with_search.xml` - Layout with search UI
- `item_place_search.xml` - Search result item layout
- `ic_close.xml` - Close button vector drawable

**Implementation Notes**:
- Uses Google Places SDK for autocomplete
- Session tokens for billing optimization
- Location biasing for relevant results
- Smooth animations between search/map views

### Build Configuration
**File**: `app/build.gradle.kts`
- **Mapbox SDK**: Version 10.16.1
- **Google Services**: Places API, Maps, Location Services
- **Firebase**: Firestore, Auth, Storage
- **Networking**: OkHttp, Retrofit, WebSocket support

### Important API Keys
⚠️ **Security Note**: API keys are currently hardcoded in `build.gradle.kts`. These should be moved to secure storage:
- Mapbox Access Token
- Google Maps API Key

### Animation Lifecycle
- **onPause()**: Animations pause when app goes to background
- **onResume()**: Animations resume when app returns
- **onDestroy()**: Proper cleanup of animators and coroutines

### Coroutines Usage
- **Scope**: `CoroutineScope(Dispatchers.Main + Job())`
- **Geocoding**: Runs on `Dispatchers.IO` for network calls
- **Cleanup**: `scope.cancel()` in onDestroy()

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
