# Technical Changes Log - Chat City Official

## Purpose
This file serves as a permanent record of all technical changes made to the Chat City Official project. 
**IMPORTANT**: This is an append-only log. Never delete or modify existing entries. Only add new entries at the bottom.
**CRITICAL**: Every time changes are made, update this log with those changes.

---

## Log Format
Each entry should follow this format:
```
### [DATE - YYYY-MM-DD HH:MM] - [DEVELOPER/CONTRIBUTOR]
**Category**: [Feature/Bug Fix/Refactor/Configuration/Documentation/Security/Performance/UI/UX]
**Files Modified**: [List of files]
**Description**: [Detailed description of changes]
**Technical Details**: [Implementation specifics]
**Breaking Changes**: [Yes/No - If yes, describe]
**Testing Notes**: [How to test the changes]
**Related Issues/PRs**: [GitHub issue or PR numbers if applicable]
---
```

---

## Change History

### 2025-01-23 - Initial Log Creation
**Category**: Documentation
**Files Modified**: TECHNICAL_CHANGES_LOG.md (created)
**Description**: Created technical changes log file to track all project modifications
**Technical Details**: 
- Established append-only log format
- Created template for consistent logging
- Set up categories for different types of changes
**Breaking Changes**: No
**Testing Notes**: N/A - Documentation only
**Related Issues/PRs**: N/A
---

### 2025-01-23 - Project State Documentation
**Category**: Documentation
**Files Modified**: TECHNICAL_CHANGES_LOG.md
**Description**: Documenting current project state as baseline for future changes
**Technical Details**: 
Current Implementation Status:
- **Navigation System**: Custom navigation bar with animated outline (100ms ValueAnimator)
  - 4 active navigation items: Saved, Home, Chats, Profile
  - Create button (center) is placeholder - no action implemented
  - Outline movement based on SVG icon positions with precise dp offsets
  
- **Map Configuration** (HomeFragment.kt):
  - Mapbox SDK 10.16.1 integration
  - Custom location pulse animation (pink #FB86BB, 20% opacity)
  - Animation: 3.5 to 35 meters radius, 2-second cycle
  - Gestures: Pan and pinch-to-zoom enabled, rotation disabled
  - Location updates: 10-second intervals with HIGH_ACCURACY priority
  - Geocoding: Fallback chain (locality > subAdminArea > adminArea)
  
- **Build Configuration**:
  - Kotlin with Gradle Kotlin DSL
  - Firebase integration (Firestore, Auth, Storage)
  - Google Services (Places API, Maps, Location)
  - Networking: OkHttp, Retrofit, WebSocket support
  
- **Known Issues**:
  - API keys hardcoded in build.gradle.kts (security concern)
  - Google Places Search implementation disabled but code preserved
  - Create button in navigation has no functionality
  
- **Animation Lifecycle**:
  - Proper pause/resume with fragment lifecycle
  - Coroutines with Main dispatcher + Job
  - Cleanup in onDestroy()

**Breaking Changes**: No
**Testing Notes**: Review current implementation against this baseline
**Related Issues/PRs**: N/A
---

### 2025-01-23 - Log Policy Establishment
**Category**: Documentation
**Files Modified**: TECHNICAL_CHANGES_LOG.md
**Description**: Established logging policies and procedures
**Technical Details**: 
Policies established:
1. **Append-Only Rule**: No deletion or modification of existing entries
2. **Mandatory Fields**: All template fields must be completed
3. **Change Categories**: Feature, Bug Fix, Refactor, Configuration, Documentation, Security, Performance, UI, UX
4. **Version Tracking**: Major changes should note version numbers if applicable
5. **Rollback Documentation**: If a change is reverted, add a new entry explaining the rollback
6. **Code Snippets**: Include relevant code snippets for complex changes
7. **Migration Steps**: Document any required migration steps for breaking changes

**Breaking Changes**: No
**Testing Notes**: N/A
**Related Issues/PRs**: N/A
---

### 2025-01-23 - Added Critical Reminder to Log
**Category**: Documentation
**Files Modified**: TECHNICAL_CHANGES_LOG.md
**Description**: Added critical reminder at the beginning of the log to ensure all changes are documented
**Technical Details**: 
- Added "CRITICAL: Every time changes are made, update this log with those changes." to the Purpose section
- This reinforces the importance of maintaining the log with every code change
- Helps prevent undocumented changes that could lead to issues later
**Breaking Changes**: No
**Testing Notes**: N/A - Documentation only
**Related Issues/PRs**: N/A
---

### 2025-01-23 21:35 - Claude/Assistant
**Category**: UI
**Files Modified**: app/src/main/java/com/chatcityofficial/chatmapapp/ui/home/HomeFragment.kt
**Description**: Disabled Mapbox scale bar (mile-radius indicator) to provide cleaner map interface
**Technical Details**: 
- Added import for `com.mapbox.maps.plugin.scalebar.scalebar`
- Added `mapView.scalebar.enabled = false` in the `initializeMap()` function
- This is set right after disabling the location component and before gesture configuration
- The scale bar (showing distance/radius measurements like "200 ft" or "1 mi") will no longer appear on the map
- Code snippet:
```kotlin
// CRITICAL: Disable the scale bar (mile-radius bar) completely
mapView.scalebar.enabled = false
```
**Breaking Changes**: No
**Testing Notes**: 
1. Launch the app and navigate to the Home screen with the map
2. Verify that no scale bar appears in any corner of the map
3. Test at different zoom levels - the scale bar should never appear
4. Pinch to zoom in/out and pan around - scale bar should remain hidden
**Related Issues/PRs**: N/A
---

### 2025-01-23 21:54 - Claude/Assistant
**Category**: Bug Fix
**Files Modified**: app/src/main/res/values/themes.xml
**Description**: Fixed duplicate theme definition build error
**Technical Details**: 
- Removed duplicate `Theme.ChatCityOfficial` definition from themes.xml
- The theme was defined in both styles.xml and themes.xml causing a resource merger conflict
- Kept the more complete definition in styles.xml which includes all related styles
- themes.xml now only contains the `Theme.SplashScreen` style
- Build error was: "Duplicate resources: [style/Theme.ChatCityOfficial]"
**Breaking Changes**: No
**Testing Notes**: 
1. Run `./gradlew clean` to clear build cache
2. Run `./gradlew assembleDebug` - build should complete successfully
3. Verify app launches normally with correct theming
**Related Issues/PRs**: N/A
---

### 2025-01-23 23:04 - Claude/Assistant
**Category**: Feature
**Files Modified**: 
- app/src/main/java/com/chatcityofficial/chatmapapp/ui/home/HomeFragment.kt
- app/src/main/res/layout/fragment_home.xml
- app/src/main/java/com/chatcityofficial/chatmapapp/SplashActivity.kt
**Description**: Implemented sunrise/sunset-based theme switching and edge-to-edge splash screen
**Technical Details**: 
**Change 2 - Day/Night Theme Switching:**
- Added SunriseSunsetCalculator integration to determine sunrise/sunset times based on location
- Map automatically switches between Style.DARK (after sunset) and Style.LIGHT (after sunrise)
- Logo color changes: white in dark mode, black in light mode
- Theme checks run every 5 minutes and on location updates
- Added proper lifecycle management for theme update handler
- ColorFilter applied to logo ImageView based on current theme
- Code snippet:
```kotlin
val shouldUseDarkTheme = now.after(sunset) || now.before(sunrise)
val newStyle = if (isDarkMode) Style.DARK else Style.LIGHT
```

**Change 3 - Edge-to-Edge Splash Screen:**
- Implemented WindowCompat.setDecorFitsSystemWindows(window, false) for true edge-to-edge
- Added support for display cutouts (notches) with LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
- Made status bar and navigation bar fully transparent
- Added proper API level checks for compatibility (API 30+ and older versions)
- WindowInsetsControllerCompat for modern immersive mode implementation
- Re-applies edge-to-edge on window focus changes to maintain immersive experience

**Breaking Changes**: No
**Testing Notes**: 
1. **Theme Switching**: 
   - Test app during day and night times
   - Verify map switches to dark theme after sunset
   - Verify logo changes to white in dark mode
   - Check theme updates when moving to different time zones
2. **Splash Screen**:
   - Test on devices with notches/cutouts
   - Verify gradient extends to all screen edges
   - Test on different Android versions (API 24+)
   - Ensure no black bars at top/bottom of screen
**Related Issues/PRs**: N/A
---

### 2025-08-14 23:35 - Claude/Assistant
**Category**: Bug Fix / UI
**Files Modified**: app/src/main/java/com/chatcityofficial/chatmapapp/ui/home/HomeFragment.kt
**Description**: Fixed location puck centering, theme flashing, and removed Mapbox branding
**Technical Details**: 
**Fix 1 - Location Puck Centering:**
- Added a separate center dot annotation that stays at the exact location coordinates
- Created `centerDotAnnotation` with 2.0 meter radius, fully opaque pink (#FB86BB)
- Pulse animation now only affects the expanding ring, not the center dot
- Both annotations share the same Point.fromLngLat(location.longitude, location.latitude)
- Modified `createLocationPuck()` function to create both annotations
- Code snippet:
```kotlin
// Create center dot that stays fixed
centerDotAnnotation = manager.create(
    CircleAnnotationOptions()
        .withPoint(point)
        .withCircleRadius(CENTER_DOT_RADIUS)
        .withCircleColor(centerColor)
        .withCircleOpacity(1.0) // Fully opaque
)
```

**Fix 2 - Theme Flashing Prevention:**
- Changed default map style from Style.MAPBOX_STREETS to Style.DARK
- Changed default isDarkMode from false to true
- Added `determinateInitialTheme()` function to pre-calculate theme before map loads
- This prevents the white/light theme from briefly appearing on load
- Theme is determined using last known location if available

**Fix 3 - Removed Mapbox Branding:**
- Added imports for logo and attribution plugins
- Disabled Mapbox logo: `mapView.logo.enabled = false`
- Disabled attribution icon: `mapView.attribution.enabled = false`
- Re-applies these settings in onResume() to ensure they stay hidden
- Re-applies after theme changes in updateMapThemeBasedOnTime()
- Code snippet:
```kotlin
// CRITICAL: Disable all Mapbox branding and UI elements
mapView.logo.enabled = false
mapView.attribution.enabled = false
mapView.scalebar.enabled = false
```

**Breaking Changes**: No
**Testing Notes**: 
1. **Location Puck Centering**:
   - Launch app and verify center dot appears at exact user location
   - Watch pulse animation and confirm center dot stays fixed while ring expands
   - Move to different location and verify both elements update correctly
2. **Theme Flashing**:
   - Force close and restart app multiple times
   - Verify no white/light flash appears before dark theme loads
   - Switch between fragments and verify no theme flashing
3. **Mapbox Branding**:
   - Check all corners of map - no Mapbox logo should appear
   - Verify no "i" information/attribution icon
   - Switch between fragments and confirm branding stays hidden
   - Change themes (day/night) and verify branding remains hidden
**Related Issues/PRs**: N/A
---

### 2025-08-14 19:47 - Claude/Assistant
**Category**: UX
**Files Modified**: app/src/main/java/com/chatcityofficial/chatmapapp/ui/home/HomeFragment.kt
**Description**: Implemented map camera position persistence when switching between screens
**Technical Details**: 
**Implementation Changes:**
- Added `savedCameraState` as a companion object variable to persist camera position across fragment lifecycle
- Added `hasInitializedCamera` flag to track if the initial camera setup has been completed
- Created `saveCameraState()` function to capture current map camera position, zoom, bearing, and pitch
- Camera state is saved in `onStop()` and `onPause()` lifecycle methods
- Camera state is restored in `initializeMap()` when the fragment is recreated
- Modified `getCurrentLocation()` to only move camera to user location on first initialization
- Removed automatic camera panning from location updates in `startLocationUpdates()`
- Camera state is also saved before theme changes to maintain position during style reloads

**Key Code Changes:**
```kotlin
// Save camera state
private fun saveCameraState() {
    savedCameraState = mapView.getMapboxMap().cameraState
}

// Restore camera state
savedCameraState?.let { state ->
    val cameraOptions = CameraOptions.Builder()
        .center(state.center)
        .zoom(state.zoom)
        .bearing(state.bearing)
        .pitch(state.pitch)
        .build()
    mapView.getMapboxMap().setCamera(cameraOptions)
}

// Only move to location on first load
if (!hasInitializedCamera && savedCameraState == null) {
    moveToLocation(it.latitude, it.longitude, 15.0)
    hasInitializedCamera = true
}
```

**Behavior Changes:**
- Map now remembers the last viewed position when switching between tabs
- Map only centers on user location on the very first load
- User can manually pan/zoom the map and the position will be maintained
- Location puck continues to update with current position without moving the camera
- Camera position persists through theme changes (day/night transitions)

**Breaking Changes**: No
**Testing Notes**: 
1. **Position Persistence**:
   - Launch app and wait for map to center on current location
   - Pan and zoom to a different area of the map
   - Switch to another tab (Saved, Chats, Profile)
   - Return to Home tab - map should show the last viewed area, not current location
2. **First Load Behavior**:
   - Force close and restart app
   - Verify map centers on current location on first load
   - Pan to different area and switch tabs
   - Return to Home - should maintain panned position
3. **Location Updates**:
   - Pan map away from current location
   - Wait for location updates (10 seconds)
   - Verify location puck updates position but camera doesn't move
4. **Theme Changes**:
   - Pan to specific area during day/night transition time
   - Wait for theme to change (or manually trigger by changing device time)
   - Verify camera position is maintained after theme switch
**Related Issues/PRs**: N/A
---

### 2025-08-14 20:04 - Claude/Assistant
**Category**: UI/UX
**Files Modified**: 
- app/src/main/java/com/chatcityofficial/chatmapapp/ui/home/HomeFragment.kt
- app/src/main/res/layout/fragment_home.xml
- app/src/main/java/com/chatcityofficial/chatmapapp/ui/chats/ChatsFragment.kt
**Description**: Fixed location puck layering, aligned Chat City logo with navigation bar, and removed error messages from Chats screen
**Technical Details**: 
**Fix 1 - Location Puck Layering:**
- Changed the order of annotation creation in `createLocationPuck()` function
- Pulse animation is now created FIRST, then center dot is created AFTER
- This ensures the center dot appears on top of the pulse animation
- Annotations are rendered in the order they are created (first = bottom, last = top)
- Code snippet:
```kotlin
// IMPORTANT: Create pulse FIRST so it's behind the center dot
pulseAnnotation = manager.create(
    CircleAnnotationOptions()
        .withPoint(point)
        .withCircleRadius(MIN_PULSE_RADIUS)
        // ... pulse settings
)

// Create center dot AFTER pulse so it appears on top
centerDotAnnotation = manager.create(
    CircleAnnotationOptions()
        .withPoint(point)
        .withCircleRadius(CENTER_DOT_RADIUS)
        // ... center dot settings
)
```

**Fix 2 - Chat City Logo Alignment:**
- Changed logo left margin from 28dp to 16dp in fragment_home.xml
- This aligns the leftmost edge of the "C" in Chat City with the navigation bar edge
- Navigation bar icons have 28dp margin, but the logo needs 16dp due to its internal spacing
- Ensures visual consistency between top logo and bottom navigation

**Fix 3 - Removed Error Toast Messages:**
- Commented out all Toast.makeText() calls in ChatsFragment.kt
- Errors are still logged with Log.e() for debugging purposes
- Prevents user-facing error messages from appearing when navigating to Chats
- Affected areas:
  - onViewCreated error handling
  - Chat item click validation
  - Activity launch failures
  - Delete button placeholder message
  - Chat loading errors
- Silent failures provide cleaner UX while maintaining debug capability

**Breaking Changes**: No
**Testing Notes**: 
1. **Location Puck Layering**:
   - Launch app and observe the location indicator
   - Verify the pink center dot is clearly visible on top of the expanding pulse
   - The pulse should expand behind the center dot, not cover it
2. **Logo Alignment**:
   - Check that the leftmost curve of the "C" in Chat City aligns with the left edge of navigation icons
   - Compare visual alignment between logo and saved/home/chats/profile icons
3. **Chats Error Handling**:
   - Navigate to Chats tab
   - No error toasts should appear even if there are issues
   - Try clicking on chat items - failures should be silent
   - Check logcat for error messages (they should still be logged)
**Related Issues/PRs**: N/A
---

### 2025-08-15 01:35 - Claude/Assistant
**Category**: Feature
**Files Modified**: 
- app/src/main/java/com/chatcityofficial/chatmapapp/ui/home/HomeFragment.kt
- app/src/main/res/layout/fragment_home.xml
- app/src/main/java/com/chatcityofficial/chatmapapp/ui/home/PlaceSearchAdapter.kt (created)
- app/src/main/res/layout/item_place_search.xml (created)
- app/src/main/res/drawable/ic_search_bar.xml (created)
- app/src/main/res/drawable/ic_close.xml (created)
- app/src/main/res/drawable/ic_arrow_back.xml (created)
- app/src/main/res/values/strings.xml
**Description**: Implemented Google Places search functionality with animated UI transitions
**Technical Details**: 
**Core Implementation:**
- Integrated Google Places API for location search with autocomplete
- Added session tokens for optimized billing with Places API
- Location-biased search results prioritizing places near current location
- Support for searching: addresses, zip codes, cities, towns, parks, bodies of water, businesses

**UI Components:**
- Custom search bar that fades in when location icon is tapped
- RecyclerView with PlaceSearchAdapter for displaying autocomplete results
- Material Design CardView for search results with location icons
- "Powered by Google" attribution text above search bar

**Animations:**
- Initial implementation with 200ms fade in/out animations
- Smooth transitions between default view and search view
- Keyboard automatically shows when entering search mode

**Place Selection:**
- When place selected, camera flies to location with 17.0 zoom level
- Attempts to outline selected place with pink (#4DFB86BB) 30% opacity border
- Uses place viewport for determining outline bounds
- Polygon annotation for place outlining (no fill, only stroke)

**Search Adapter:**
- ListAdapter with DiffUtil for efficient updates
- Displays primary text (place name) and secondary text (address)
- Click handler triggers place selection and map navigation

**API Key Configuration:**
- Added Google Maps API key to strings.xml: AIzaSyDG9OSQU6lrj6Ecz2KwPPCvjV9MAGx5Wgc

**Code Snippets:**
```kotlin
// Initialize Places API
if (!Places.isInitialized()) {
    Places.initialize(requireContext(), getString(R.string.google_maps_api_key))
}
placesClient = Places.createClient(requireContext())

// Perform place search
val request = FindAutocompletePredictionsRequest.builder()
    .setQuery(query)
    .setSessionToken(sessionToken)
    .setLocationBias(/* current location bounds */)
    .build()
```

**Breaking Changes**: No
**Testing Notes**: 
1. Tap location icon to open search
2. Type any location (minimum 3 characters)
3. Select from autocomplete results
4. Verify map flies to selected location
5. Check that place is outlined with pink border
**Related Issues/PRs**: N/A
---

### 2025-08-15 01:50 - Claude/Assistant
**Category**: UX
**Files Modified**: 
- app/src/main/java/com/chatcityofficial/chatmapapp/ui/home/HomeFragment.kt
- app/src/main/res/layout/fragment_home.xml
**Description**: Improved search functionality UX with better tap detection, faster animations, and map tap to close
**Technical Details**: 
**Improved Tap Detection:**
- Made entire location container clickable with `android:clickable="true"` and `android:focusable="true"`
- Added `android:foreground="?attr/selectableItemBackground"` for touch feedback
- Both container and icon are clickable for redundancy
- Added `isSearchVisible` flag to prevent double triggers and track state

**UI Layout Changes:**
- Moved "Powered by Google" text ABOVE search bar (was below)
- Positioned at right with proper margins
- Search container marginTop adjusted to 70dp for better spacing

**Map Interaction:**
- Added `mapView.getMapboxMap().addOnMapClickListener` to close search on map tap
- When search is visible and user taps map, search closes automatically
- Logo and location icons fade back in when search closes

**Animation Improvements:**
- Reduced animation duration from 200ms to 100ms (ANIMATION_DURATION constant)
- All fade in/out animations now use 100ms for snappier response
- Better perceived performance with faster transitions

**Search Flow Enhancement:**
- Search automatically hides after place selection
- Clear search results when hiding search view
- Proper keyboard management (hide on selection, show on open)
- Session token regenerated after each place selection

**Code Snippets:**
```kotlin
// Map click to close search
mapView.getMapboxMap().addOnMapClickListener { point ->
    if (isSearchVisible) {
        hideSearchView()
        true // Consume the click event
    } else {
        false // Let other handlers process it
    }
}

// Animation duration constant
companion object {
    private const val ANIMATION_DURATION = 100L // Changed from 200ms
}
```

**Place Outline Refinement:**
- Ensured outline uses 30% opacity (#4DFB86BB)
- No fill, only outline as specified
- Outline removed when search closes or new search begins

**Breaking Changes**: No
**Testing Notes**: 
1. **Tap Detection**: Tap anywhere on location container (not just icon)
2. **Animation Speed**: Verify 100ms transitions feel responsive
3. **Map Tap**: Open search, then tap on map - should close search
4. **Search Flow**: Select a place - verify search closes and map moves
5. **Powered by Google**: Check text appears above search bar
**Related Issues/PRs**: N/A
---

### 2025-08-15 02:19 - Claude/Assistant
**Category**: Bug Fix
**Files Modified**: app/src/main/java/com/chatcityofficial/chatmapapp/ui/home/HomeFragment.kt
**Description**: Fixed Mapbox SDK v10 compilation error by using gestures plugin for map click listener
**Technical Details**: 
- Replaced deprecated `mapView.getMapboxMap().addOnMapClickListener` with `mapView.gestures.addOnMapClickListener`
- In Mapbox SDK v10, map click listeners are accessed through the gestures plugin
- Added import for `com.mapbox.maps.plugin.gestures.addOnMapClickListener`
- The gestures plugin method has the same signature and behavior as the deprecated method
- Code snippet:
```kotlin
// Use the gestures plugin's addOnMapClickListener
mapView.gestures.addOnMapClickListener { point ->
    if (isSearchVisible) {
        hideSearchView()
        true // Consume the click event
    } else {
        false // Let other click handlers process it
    }
}
```
- This fixes the compilation error: "Unresolved reference: addOnMapClickListener"
**Breaking Changes**: No
**Testing Notes**: 
1. Run `./gradlew clean` to clear build cache
2. Run `./gradlew assembleDebug` - build should complete successfully
3. Test map tap functionality:
   - Open search by tapping location icon
   - Tap anywhere on the map
   - Verify search closes properly
4. Ensure no other click handlers are affected
**Related Issues/PRs**: N/A
---

### 2025-08-15 14:06 - Claude/Assistant
**Category**: UX/Feature
**Files Modified**: 
- app/src/main/java/com/chatcityofficial/chatmapapp/MainActivity.kt
- app/src/main/java/com/chatcityofficial/chatmapapp/ui/home/HomeFragment.kt
**Description**: Implemented improved back button behavior and fixed initial app launch to center on user location
**Technical Details**: 
**Change 1 - Back Button Behavior (MainActivity.kt):**
- Added `OnBackPressedCallback` to handle system back button presses
- Added `currentDestinationId` tracking to know which screen is active
- When on home screen: back button minimizes app using `moveTaskToBack(true)`
- When on other screens: back button navigates to home and moves outline
- Import added: `androidx.activity.OnBackPressedCallback`
- Code snippet:
```kotlin
onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
        if (currentDestinationId == R.id.navigation_home) {
            moveTaskToBack(true) // Minimize app
        } else {
            navigateToDestination(R.id.navigation_home)
            animateOutlineToPosition(R.id.navigation_home)
        }
    }
})
```

**Change 2 - Search Bar Already Working:**
- Confirmed existing implementation: tapping anywhere on map closes search bar when open
- This was already properly implemented with `mapView.gestures.addOnMapClickListener`
- No changes needed - working as requested

**Change 3 - Initial Location Centering (HomeFragment.kt):**
- Reset `hasInitializedCamera` flag to false in `onCreateView()` for fresh fragment creation
- This ensures app always centers on user location when launched
- Modified camera initialization logic to prioritize initial centering over saved state
- Only skip centering if both: hasInitializedCamera is true AND savedCameraState exists
- Added fallback: if initialized but no saved state, still center on location
- Added public methods for potential search handling: `isSearchViewVisible()` and `hideSearchViewIfVisible()`
- Code changes:
```kotlin
// In onCreateView
hasInitializedCamera = false // Reset for fresh launch

// In getCurrentLocation
if (!hasInitializedCamera) {
    moveToLocation(it.latitude, it.longitude, 15.0)
    hasInitializedCamera = true
} else if (savedCameraState != null) {
    // Use saved state
} else {
    // Fallback to centering
    moveToLocation(it.latitude, it.longitude, 15.0)
}
```

**Breaking Changes**: No
**Testing Notes**: 
1. **Back Button Behavior**:
   - From Home screen: Press back button - app should minimize (go to background)
   - From Saved/Chats/Profile: Press back - should return to Home with outline animation
   - Verify outline moves correctly when navigating via back button
2. **Search Bar Map Tap**:
   - Open search by tapping location icon
   - Tap anywhere on the map
   - Verify search closes and default UI returns
3. **Initial Location Centering**:
   - Force close app completely
   - Launch app fresh
   - Verify map centers on current user location
   - Switch tabs and return - position should persist
   - Force close and relaunch - should center on location again
**Related Issues/PRs**: N/A
---

## Notes Section

### Important Reminders
- Always add new entries at the bottom
- Never delete or modify existing entries
- If you need to correct an entry, add a new entry with the correction
- Include enough detail that someone could understand the change without looking at the code
- For security-related changes, be careful not to expose sensitive information

### Categories Guide
- **Feature**: New functionality added
- **Bug Fix**: Correction of defects
- **Refactor**: Code improvements without changing functionality
- **Configuration**: Build, dependency, or environment changes
- **Documentation**: README, comments, or documentation updates
- **Security**: Security improvements or vulnerability fixes
- **Performance**: Optimization and performance improvements
- **UI**: User interface changes
- **UX**: User experience improvements

### Future Considerations
- Consider adding automated tooling to enforce append-only policy
- May want to add tags or labels for easier searching
- Consider splitting into yearly files if log becomes too large
- Add integration with CI/CD to auto-generate entries for certain changes

---

END OF LOG - ADD NEW ENTRIES BELOW THIS LINE
---
