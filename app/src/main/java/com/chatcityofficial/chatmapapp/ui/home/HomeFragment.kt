package com.chatcityofficial.chatmapapp.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import com.luckycatlabs.sunrisesunset.dto.SunLocation
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnRotateListener
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
    private var isLocationPermissionGranted = false
    private var isMapReady = false
    private var hasCenteredOnLocation = false
    
    // Camera state preservation
    private var savedCameraState: CameraState? = null
    private var shouldRestoreCamera = false
    
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
    
    // Location tracking listeners
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        if (!hasCenteredOnLocation && !shouldRestoreCamera) {
            // Only center automatically on first location update with zoom level 15
            mapView?.getMapboxMap()?.setCamera(
                CameraOptions.Builder()
                    .center(it)
                    .zoom(15.0)
                    .bearing(0.0) // Always keep north orientation
                    .pitch(0.0)   // No tilt
                    .build()
            )
            hasCenteredOnLocation = true
            Log.d("HomeFragment", "📍 Centered on location from indicator: ${it.latitude()}, ${it.longitude()}")
            
            // Update location text for initial position
            updateLocationText(it.latitude(), it.longitude())
        }
        mapView?.gestures?.focalPoint = mapView?.getMapboxMap()?.pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            // Allow move to begin
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            // Update location text as user moves the map
            val center = mapView?.getMapboxMap()?.cameraState?.center
            center?.let {
                updateLocationText(it.latitude(), it.longitude())
            }
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            // Final update when movement ends
            val center = mapView?.getMapboxMap()?.cameraState?.center
            center?.let {
                updateLocationText(it.latitude(), it.longitude())
            }
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
                    if (!shouldRestoreCamera) {
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
                    
                    // Center map on first location if not already centered and not restoring
                    if (isMapReady && !hasCenteredOnLocation && !shouldRestoreCamera) {
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
                
                // Remove UI elements
                mapView?.scalebar?.enabled = false
                mapView?.logo?.enabled = false
                mapView?.attribution?.enabled = false
                
                // Aggressively disable all rotation
                mapView?.gestures?.apply {
                    rotateEnabled = false
                    simultaneousRotateAndPinchToZoomEnabled = false
                    pitchEnabled = false
                    
                    // Allow scrolling and zooming
                    scrollEnabled = true
                    doubleTapToZoomInEnabled = true
                    doubleTouchToZoomOutEnabled = true
                    quickZoomEnabled = true
                    pinchToZoomEnabled = true
                }
                
                // Override gesture detectors to block rotation
                mapView?.gestures?.addOnRotateListener(object : OnRotateListener {
                    override fun onRotateBegin(detector: RotateGestureDetector) {
                        // Block rotation start - immediately reset
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
                        // Reset to north if needed
                        mapView?.getMapboxMap()?.setCamera(
                            CameraOptions.Builder()
                                .bearing(0.0)
                                .build()
                        )
                    }
                })
                
                // Use a timer to continuously enforce no rotation
                fragmentScope.launch {
                    while (isActive) {
                        delay(100) // Check every 100ms
                        
                        // Force bearing to 0 if it changed
                        val bearing = mapView?.getMapboxMap()?.cameraState?.bearing ?: 0.0
                        if (bearing != 0.0) {
                            mapView?.getMapboxMap()?.setCamera(
                                CameraOptions.Builder()
                                    .bearing(0.0)
                                    .build()
                            )
                        }
                        
                        // Re-disable rotation gestures
                        mapView?.gestures?.rotateEnabled = false
                    }
                }
                
                // Setup location component
                setupLocationComponent()
                
                // Check if we should restore camera position
                if (savedCameraState != null && shouldRestoreCamera) {
                    Log.d("HomeFragment", "📸 Restoring saved camera position")
                    restoreCameraPosition()
                } else {
                    // Check and request permissions after map is ready
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
                
                // Switch map theme
                val newMapStyle = if (isDarkTheme) {
                    "mapbox://styles/mapbox/dark-v11"
                } else {
                    "mapbox://styles/mapbox/light-v11"
                }
                
                mapView?.getMapboxMap()?.loadStyleUri(newMapStyle) { style ->
                    Log.d("HomeFragment", "✅ Theme switched to ${if (isDarkTheme) "dark" else "light"}")
                    
                    // Re-setup location component after style change
                    setupLocationComponent()
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
        savedCameraState = mapView?.getMapboxMap()?.cameraState
        savedCameraState?.let {
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
            Log.d("HomeFragment", "✅ Restored camera position")
            hasCenteredOnLocation = true // Prevent auto-centering after restore
            
            // Update location text for restored position
            updateLocationText(state.center.latitude(), state.center.longitude())
        }
        shouldRestoreCamera = false
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
                if (!shouldRestoreCamera) {
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
        Log.d("HomeFragment", "⚙️ Setting up location component")
        
        // Enable location component
        mapView?.location?.apply {
            enabled = true
            pulsingEnabled = true
            
            // Only add position listener
            addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            
            Log.d("HomeFragment", "✅ Location component enabled with pulsing")
        }
        
        // Add move listener
        mapView?.gestures?.addOnMoveListener(onMoveListener)
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
                    
                    if (isMapReady && !hasCenteredOnLocation && !shouldRestoreCamera) {
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
        
        hasCenteredOnLocation = true
        Log.d("HomeFragment", "✅ Map centered on location: ${location.latitude}, ${location.longitude}")
        
        // Update location text when centering
        updateLocationText(location.latitude, location.longitude)
        
        // Stop location updates once we've centered the map
        stopLocationUpdates()
    }
    
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    
    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "📱 Fragment resumed")
        
        // Start theme update handler
        themeHandler.post(themeUpdateRunnable)
        
        // If we have a saved camera state and the map is ready, restore it
        if (savedCameraState != null && isMapReady) {
            shouldRestoreCamera = true
            restoreCameraPosition()
        } else if (isLocationPermissionGranted && !hasCenteredOnLocation && isMapReady) {
            // Otherwise, request location if we haven't centered yet
            Log.d("HomeFragment", "📍 Resuming - requesting location")
            requestCurrentLocation()
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("HomeFragment", "⏸️ Fragment paused")
        
        // Stop theme update handler
        themeHandler.removeCallbacks(themeUpdateRunnable)
        
        // Save the current camera position before pausing
        saveCameraPosition()
        shouldRestoreCamera = true
        
        // Stop location updates when fragment pauses
        stopLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        
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
        Log.d("HomeFragment", "🗑️ Fragment view destroyed")
        
        // Save camera position before destroying view
        saveCameraPosition()
        shouldRestoreCamera = true
        
        // Stop theme update handler
        themeHandler.removeCallbacks(themeUpdateRunnable)
        
        // Cancel any pending geocoding jobs
        geocodingJob?.cancel()
        
        fragmentScope.cancel()
        stopLocationUpdates()
        mapView?.location?.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView?.gestures?.removeOnMoveListener(onMoveListener)
        mapView?.onDestroy()
        mapView = null
        locationText = null
        chatCityLogo = null
    }
}