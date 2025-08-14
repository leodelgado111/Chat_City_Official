package com.chatcityofficial.chatmapapp.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chatcityofficial.chatmapapp.R
import com.google.android.gms.location.*
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location as SunLocation
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnRotateListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.attribution.attribution
import kotlinx.coroutines.*
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HomeFragment : Fragment() {

    private var mapView: MapView? = null
    private var locationText: TextView? = null
    private var chatCityLogo: ImageView? = null
    // REMOVED ALL BUTTON REFERENCES - NO ZOOM OR GPS CONTROLS
    
    private var isLocationPermissionGranted = false
    private var isMapReady = false
    private var hasCenteredOnLocation = false
    
    // Camera state preservation - STATIC to persist across fragment recreation
    companion object {
        private var savedCameraState: CameraState? = null
        private var hasEverCentered: Boolean = false
    }
    
    // Track if user has manually moved the map
    private var userHasMovedMap = false
    
    // Google Location Services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    
    // Geocoder for reverse geocoding
    private var geocoder: Geocoder? = null
    
    // Permission launcher
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    
    // Coroutine scope for delayed operations
    private val fragmentScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Job for debouncing location updates
    private var geocodingJob: Job? = null
    
    // Theme management
    private var isDarkTheme = false
    private var currentUserLocation: Location? = null
    private val themeHandler = Handler(Looper.getMainLooper())
    private lateinit var themeUpdateRunnable: Runnable
    
    // Store the last known user location
    private var lastKnownUserLocation: Point? = null
    
    // Location tracking listeners
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        // Store the last known user location
        lastKnownUserLocation = it
        
        // Only center on first location if we've never centered before and no saved state exists
        if (!hasEverCentered && savedCameraState == null && !userHasMovedMap) {
            // Only center automatically on first location update with zoom level 15
            mapView?.getMapboxMap()?.setCamera(
                CameraOptions.Builder()
                    .center(it)
                    .zoom(15.0)
                    .bearing(0.0) // Always keep north orientation
                    .pitch(0.0)   // No tilt
                    .build()
            )
            hasEverCentered = true
            hasCenteredOnLocation = true
            Log.d("HomeFragment", "📍 First time centering on location: ${it.latitude()}, ${it.longitude()}")
            
            // Update location text for initial position
            updateLocationText(it.latitude(), it.longitude())
        }
        
        // Update focal point for smooth gestures
        mapView?.gestures?.focalPoint = mapView?.getMapboxMap()?.pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            // User has started manually moving the map
            userHasMovedMap = true
            // Mark that we've interacted with the map
            if (!hasEverCentered) {
                hasEverCentered = true
            }
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            // Update location text as user moves the map
            val center = mapView?.getMapboxMap()?.cameraState?.center
            center?.let {
                updateLocationText(it.latitude(), it.longitude())
            }
            // Return false to allow the gesture to continue
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            // Final update when movement ends
            val center = mapView?.getMapboxMap()?.cameraState?.center
            center?.let {
                updateLocationText(it.latitude(), it.longitude())
                // Save camera state after user finishes moving
                saveCameraPosition()
            }
        }
    }
    
    // Scale listener for pinch zoom
    private val onScaleListener = object : OnScaleListener {
        override fun onScaleBegin(detector: StandardScaleGestureDetector) {
            // Don't block anything, just track the gesture
        }

        override fun onScale(detector: StandardScaleGestureDetector) {
            // Allow scale to continue
        }

        override fun onScaleEnd(detector: StandardScaleGestureDetector) {
            // Save camera state after zoom ends
            saveCameraPosition()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize geocoder
        context?.let {
            geocoder = Geocoder(it, Locale.getDefault())
        }
        
        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            
            if (fineLocationGranted || coarseLocationGranted) {
                Log.d("HomeFragment", "✅ Location permission granted via launcher")
                Toast.makeText(context, "Location permission granted", Toast.LENGTH_SHORT).show()
                isLocationPermissionGranted = true
                
                // Setup location after permission granted
                if (isMapReady) {
                    setupLocationComponent()
                    // Only request location if we don't have a saved camera state
                    if (savedCameraState == null) {
                        requestCurrentLocation()
                    }
                }
            } else {
                Log.d("HomeFragment", "❌ Location permission denied via launcher")
                Toast.makeText(
                    context,
                    "Location permission denied. Map will not center on your location.",
                    Toast.LENGTH_LONG
                ).show()
                isLocationPermissionGranted = false
            }
        }
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Create location request
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds
            fastestInterval = 2000 // 2 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        
        // Create location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d("HomeFragment", "📍 Location update from callback: ${location.latitude}, ${location.longitude}")
                    
                    // Store current location for theme calculations
                    currentUserLocation = location
                    
                    // Update theme based on location
                    updateThemeBasedOnSunriseSunset(location)
                    
                    // Only center if we've never centered before and no saved state
                    if (isMapReady && !hasEverCentered && savedCameraState == null && !userHasMovedMap) {
                        centerMapOnLocation(location)
                    }
                }
            }
        }
        
        // Setup theme update runnable
        themeUpdateRunnable = Runnable {
            currentUserLocation?.let { location ->
                updateThemeBasedOnSunriseSunset(location)
            }
            // Schedule next update in 1 minute
            themeHandler.postDelayed(themeUpdateRunnable, 60000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        
        mapView = root.findViewById(R.id.mapView)
        locationText = root.findViewById(R.id.locationText)
        chatCityLogo = root.findViewById(R.id.chatCityLogo)
        // NO BUTTON SETUP - ALL REMOVED
        
        // IMPORTANT: Immediately disable Mapbox UI elements before style loads
        // This prevents them from appearing during screen transitions
        mapView?.let { map ->
            // Disable all Mapbox UI elements immediately
            map.scalebar.enabled = false
            map.logo.enabled = false
            map.attribution.enabled = false
            
            // Set their visibility to GONE to ensure they never flash
            map.scalebar.updateSettings {
                enabled = false
            }
            map.logo.updateSettings {
                enabled = false
            }
            map.attribution.updateSettings {
                enabled = false
            }
        }
        
        // Determine initial theme based on time (before we have location)
        val initialTheme = determineInitialTheme()
        
        // Load map style based on initial theme
        val mapStyle = if (initialTheme) {
            "mapbox://styles/mapbox/dark-v11"
        } else {
            "mapbox://styles/mapbox/light-v11"
        }
        
        mapView?.getMapboxMap()?.loadStyleUri(mapStyle) { style ->
            if (style != null) {
                Log.d("HomeFragment", "✅ Map style loaded successfully")
                isMapReady = true
                
                // Update logo color based on initial theme
                updateLogoColor(initialTheme)
                
                // Double-check that UI elements are disabled after style loads
                // This is redundant but ensures they stay hidden
                mapView?.scalebar?.enabled = false
                mapView?.logo?.enabled = false
                mapView?.attribution?.enabled = false
                
                // CRITICAL FIX: Configure gestures for PROPER simultaneous pan and zoom
                mapView?.gestures?.apply {
                    // Disable rotation and pitch
                    rotateEnabled = false
                    pitchEnabled = false
                    
                    // Enable all pan and zoom gestures
                    scrollEnabled = true
                    pinchToZoomEnabled = true
                    doubleTapToZoomInEnabled = true
                    doubleTouchToZoomOutEnabled = true
                    quickZoomEnabled = true
                    
                    // IMPORTANT: These settings enable smooth simultaneous gestures
                    simultaneousRotateAndPinchToZoomEnabled = true  // Changed to true for simultaneous gestures
                    pinchToZoomDecelerationEnabled = true
                    scrollDecelerationEnabled = true
                    rotateDecelerationEnabled = false  // Keep rotation disabled
                    // Removed pitchDecelerationEnabled as it doesn't exist
                    
                    // Increase gesture thresholds for smoother interaction
                    increasePinchToZoomThresholdWhenRotating = false  // Don't increase threshold
                    increaseRotateThresholdWhenPinchingToZoom = false // Don't increase threshold
                    zoomAnimationAmount = 1.0f  // Full zoom animation
                    
                    // Add listeners
                    addOnMoveListener(onMoveListener)
                    addOnScaleListener(onScaleListener)
                    
                    // Still block rotation even though simultaneousRotateAndPinchToZoomEnabled is true
                    addOnRotateListener(object : OnRotateListener {
                        override fun onRotateBegin(detector: RotateGestureDetector) {
                            // Force bearing to 0
                            mapView?.getMapboxMap()?.setCamera(
                                CameraOptions.Builder()
                                    .bearing(0.0)
                                    .build()
                            )
                        }
                        
                        override fun onRotate(detector: RotateGestureDetector) {
                            // Block all rotation - immediately reset
                            mapView?.getMapboxMap()?.setCamera(
                                CameraOptions.Builder()
                                    .bearing(0.0)
                                    .build()
                            )
                        }
                        
                        override fun onRotateEnd(detector: RotateGestureDetector) {
                            // Ensure north orientation
                            mapView?.getMapboxMap()?.setCamera(
                                CameraOptions.Builder()
                                    .bearing(0.0)
                                    .build()
                            )
                        }
                    })
                }
                
                // Setup location component with custom styling
                setupLocationComponent()
                
                // ALWAYS restore camera position if we have saved state
                if (savedCameraState != null) {
                    Log.d("HomeFragment", "📸 Restoring saved camera position on view creation")
                    restoreCameraPosition()
                } else {
                    // Only check permissions if no saved state
                    checkAndRequestLocationPermissions()
                }
                
                // Add camera change listener
                mapView?.getMapboxMap()?.addOnCameraChangeListener {
                    val center = mapView?.getMapboxMap()?.cameraState?.center
                    center?.let {
                        updateLocationText(it.latitude(), it.longitude())
                    }
                }
                
            } else {
                Log.e("HomeFragment", "❌ Map style failed to load")
                Toast.makeText(context, "❌ Map style failed to load", Toast.LENGTH_SHORT).show()
            }
        }
        
        return root
    }
    
    private fun determineInitialTheme(): Boolean {
        // Use a simple time-based approach for initial theme
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Dark theme between 6 PM and 6 AM
        return hour >= 18 || hour < 6
    }
    
    private fun updateThemeBasedOnSunriseSunset(location: Location) {
        try {
            val sunLocation = SunLocation(location.latitude, location.longitude)
            val calculator = SunriseSunsetCalculator(sunLocation, TimeZone.getDefault())
            
            val calendar = Calendar.getInstance()
            val officialSunrise = calculator.getOfficialSunriseCalendarForDate(calendar)
            val officialSunset = calculator.getOfficialSunsetCalendarForDate(calendar)
            
            val currentTime = Calendar.getInstance()
            
            // Determine if it's currently dark (after sunset or before sunrise)
            val shouldUseDarkTheme = currentTime.after(officialSunset) || currentTime.before(officialSunrise)
            
            if (shouldUseDarkTheme != isDarkTheme) {
                isDarkTheme = shouldUseDarkTheme
                
                // Save camera state before theme change
                saveCameraPosition()
                
                // Switch map theme
                val newMapStyle = if (isDarkTheme) {
                    "mapbox://styles/mapbox/dark-v11"
                } else {
                    "mapbox://styles/mapbox/light-v11"
                }
                
                // IMPORTANT: Disable UI elements BEFORE loading new style
                mapView?.let { map ->
                    map.scalebar.enabled = false
                    map.logo.enabled = false
                    map.attribution.enabled = false
                }
                
                mapView?.getMapboxMap()?.loadStyleUri(newMapStyle) { style ->
                    Log.d("HomeFragment", "✅ Theme switched to ${if (isDarkTheme) "dark" else "light"}")
                    
                    // Ensure UI elements stay disabled after style change
                    mapView?.scalebar?.enabled = false
                    mapView?.logo?.enabled = false
                    mapView?.attribution?.enabled = false
                    
                    // Re-setup location component after style change with custom colors
                    setupLocationComponent()
                    
                    // Restore camera position after theme change
                    restoreCameraPosition()
                }
                
                // Update logo color
                updateLogoColor(isDarkTheme)
            }
            
            Log.d("HomeFragment", "🌅 Sunrise: ${officialSunrise.time}, 🌇 Sunset: ${officialSunset.time}")
            Log.d("HomeFragment", "🎨 Current theme: ${if (isDarkTheme) "dark" else "light"}")
            
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error calculating sunrise/sunset: ${e.message}")
            // Fall back to time-based theme
            isDarkTheme = determineInitialTheme()
            updateLogoColor(isDarkTheme)
        }
    }
    
    private fun updateLogoColor(isDark: Boolean) {
        chatCityLogo?.let { logo ->
            if (isDark) {
                // White logo for dark theme (sunset to sunrise)
                logo.colorFilter = PorterDuffColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.white),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                // Black logo for light theme (sunrise to sunset)
                logo.colorFilter = PorterDuffColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.black),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }
    }
    
    private fun updateLocationText(latitude: Double, longitude: Double) {
        // Cancel previous geocoding job if it exists
        geocodingJob?.cancel()
        
        // Debounce the geocoding request
        geocodingJob = fragmentScope.launch {
            delay(300) // Wait 300ms before executing
            
            withContext(Dispatchers.IO) {
                try {
                    geocoder?.let { geo ->
                        val addresses = geo.getFromLocation(latitude, longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            
                            // Get city name, or fallback to locality, sub-admin area, or admin area
                            val cityName = when {
                                !address.locality.isNullOrEmpty() -> address.locality
                                !address.subAdminArea.isNullOrEmpty() -> address.subAdminArea
                                !address.adminArea.isNullOrEmpty() -> address.adminArea
                                else -> "Unknown Location"
                            }
                            
                            withContext(Dispatchers.Main) {
                                locationText?.text = cityName
                                Log.d("HomeFragment", "📍 Location updated to: $cityName")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                locationText?.text = "Unknown Location"
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error getting location name: ${e.message}")
                    withContext(Dispatchers.Main) {
                        // Fallback to coordinates if geocoding fails
                        locationText?.text = "${latitude.format(2)}, ${longitude.format(2)}"
                    }
                }
            }
        }
    }
    
    private fun saveCameraPosition() {
        val currentState = mapView?.getMapboxMap()?.cameraState
        currentState?.let {
            savedCameraState = it
            Log.d("HomeFragment", "💾 Saved camera position - Center: ${it.center.latitude()}, ${it.center.longitude()}, Zoom: ${it.zoom}")
        }
    }
    
    private fun restoreCameraPosition() {
        savedCameraState?.let { state ->
            mapView?.getMapboxMap()?.setCamera(
                CameraOptions.Builder()
                    .center(state.center)
                    .zoom(state.zoom)
                    .bearing(0.0)  // Always restore with north orientation
                    .pitch(0.0)    // Always restore with no tilt
                    .build()
            )
            Log.d("HomeFragment", "✅ Restored camera position - Center: ${state.center.latitude()}, ${state.center.longitude()}, Zoom: ${state.zoom}")
            hasCenteredOnLocation = true // Prevent auto-centering after restore
            
            // Update location text for restored position
            updateLocationText(state.center.latitude(), state.center.longitude())
        }
    }
    
    private fun checkAndRequestLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                Log.d("HomeFragment", "✅ Location permission already granted")
                isLocationPermissionGranted = true
                // Only request location if no saved camera state
                if (savedCameraState == null) {
                    requestCurrentLocation()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Show explanation and request permission
                Log.d("HomeFragment", "ℹ️ Should show permission rationale")
                Toast.makeText(
                    context,
                    "Location permission is needed to show your position on the map",
                    Toast.LENGTH_LONG
                ).show()
                
                // Request permission after showing rationale
                fragmentScope.launch {
                    delay(2000) // Give user time to read the message
                    requestLocationPermissions()
                }
            }
            else -> {
                // Request permission directly
                Log.d("HomeFragment", "📍 Requesting location permission")
                requestLocationPermissions()
            }
        }
    }
    
    private fun requestLocationPermissions() {
        Log.d("HomeFragment", "🔔 Launching permission request dialog")
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun setupLocationComponent() {
        Log.d("HomeFragment", "⚙️ Setting up location component with custom colors")
        
        // Define custom colors based on the gradient
        // Using colors from the SVG gradient: pink-purple-blue theme
        val gradientPinkColor = Color.argb(51, 251, 134, 187)  // 20% opacity pink
        val gradientPurpleColor = Color.argb(51, 166, 170, 213)  // 20% opacity purple
        val gradientBlueColor = Color.argb(51, 151, 212, 240)  // 20% opacity light blue
        val mediumLightGray = Color.argb(128, 200, 200, 200)  // Medium-light gray for accuracy circle
        
        // Enable location component with custom styling
        mapView?.location?.apply {
            enabled = true
            pulsingEnabled = true
            
            // Custom location puck with gradient-inspired colors
            // Since we can't do a true gradient, we'll use the dominant purple-blue color
            locationPuck = LocationPuck2D(
                // Top icon (bearing image when device has bearing)
                topImage = null,  // Keep default or you can set a custom drawable
                // Shadow image
                shadowImage = null,  // Keep default shadow
                // Scale expression - keep default size
                scaleExpression = null
            )
            
            // Set pulsing color to match the gradient theme (using the purple-blue tone)
            pulsingColor = gradientPurpleColor
            
            // Set pulsing max radius (keep default or adjust as needed)
            pulsingMaxRadius = 20f  // Adjust this value to control pulse size
            
            // Accuracy ring color - set to medium-light gray
            accuracyRingColor = mediumLightGray
            accuracyRingBorderColor = mediumLightGray
            
            // Only add position listener
            addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            
            Log.d("HomeFragment", "✅ Location component enabled with custom gradient-inspired colors")
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {
        if (!isLocationPermissionGranted) {
            Log.e("HomeFragment", "❌ Cannot request location - permission not granted")
            return
        }
        
        Log.d("HomeFragment", "📍 Requesting current location from FusedLocationClient...")
        
        // Try to get last known location first
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("HomeFragment", "✅ Got last known location: ${location.latitude}, ${location.longitude}")
                    
                    // Store location and update theme
                    currentUserLocation = location
                    updateThemeBasedOnSunriseSunset(location)
                    
                    // Only center if we don't have saved camera state
                    if (isMapReady && savedCameraState == null && !hasEverCentered && !userHasMovedMap) {
                        centerMapOnLocation(location)
                    }
                } else {
                    Log.d("HomeFragment", "⚠️ Last location is null, requesting fresh location updates")
                    // Request location updates if last location is null
                    startLocationUpdates()
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "❌ Failed to get last location: ${e.message}")
                // Try requesting location updates as fallback
                startLocationUpdates()
            }
    }
    
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!isLocationPermissionGranted) {
            Log.e("HomeFragment", "❌ Cannot start location updates - permission not granted")
            return
        }
        
        Log.d("HomeFragment", "🔄 Starting continuous location updates...")
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error starting location updates: ${e.message}")
        }
    }
    
    private fun stopLocationUpdates() {
        Log.d("HomeFragment", "⏹️ Stopping location updates")
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error stopping location updates: ${e.message}")
        }
    }
    
    private fun centerMapOnLocation(location: Location) {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        
        mapView?.getMapboxMap()?.setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .bearing(0.0)  // Always north orientation
                .pitch(0.0)    // No tilt
                .build()
        )
        
        hasEverCentered = true
        hasCenteredOnLocation = true
        userHasMovedMap = false
        Log.d("HomeFragment", "✅ Map centered on location: ${location.latitude}, ${location.longitude}")
        
        // Update location text when centering
        updateLocationText(location.latitude, location.longitude)
        
        // Save this as the current camera position
        saveCameraPosition()
        
        // Stop location updates once we've centered the map
        stopLocationUpdates()
    }
    
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    
    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "📱 Fragment resumed")
        
        // IMPORTANT: Immediately hide Mapbox UI elements when resuming
        // This prevents them from flashing when returning to the screen
        mapView?.let { map ->
            map.scalebar.enabled = false
            map.logo.enabled = false
            map.attribution.enabled = false
        }
        
        // Start theme update handler
        themeHandler.post(themeUpdateRunnable)
        
        // ALWAYS restore camera if we have saved state
        if (savedCameraState != null && isMapReady) {
            Log.d("HomeFragment", "📸 Restoring camera on resume")
            restoreCameraPosition()
        } else if (isLocationPermissionGranted && !hasEverCentered && isMapReady) {
            // Only request location if we've never centered before
            Log.d("HomeFragment", "📍 Resuming - requesting location for first time")
            requestCurrentLocation()
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("HomeFragment", "⏸️ Fragment paused - saving camera position")
        
        // Stop theme update handler
        themeHandler.removeCallbacks(themeUpdateRunnable)
        
        // ALWAYS save the current camera position before pausing
        saveCameraPosition()
        
        // Stop location updates when fragment pauses
        stopLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        
        // IMPORTANT: Disable UI elements immediately when starting
        mapView?.let { map ->
            map.scalebar.enabled = false
            map.logo.enabled = false
            map.attribution.enabled = false
        }
        
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        Log.d("HomeFragment", "⏹️ Fragment stopped - saving camera position")
        
        // Save camera position when stopping
        saveCameraPosition()
        
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("HomeFragment", "🗑️ Fragment view destroyed - camera position preserved in companion object")
        
        // Save camera position before destroying view
        saveCameraPosition()
        
        // Stop theme update handler
        themeHandler.removeCallbacks(themeUpdateRunnable)
        
        // Cancel any pending geocoding jobs
        geocodingJob?.cancel()
        
        fragmentScope.cancel()
        stopLocationUpdates()
        mapView?.location?.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView?.gestures?.removeOnMoveListener(onMoveListener)
        mapView?.gestures?.removeOnScaleListener(onScaleListener)
        mapView?.onDestroy()
        mapView = null
        locationText = null
        chatCityLogo = null
        // NO BUTTON CLEANUP - ALL REMOVED
    }
}