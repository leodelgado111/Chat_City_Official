package com.chatcityofficial.chatmapapp.ui.home

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chatcityofficial.chatmapapp.R
import com.google.android.gms.location.*
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import kotlinx.coroutines.*
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var locationText: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var logoImageView: ImageView
    private var currentLocation: Location? = null
    private var currentMapStyle: String = Style.DARK // Start with dark to prevent flashing
    private var isDarkMode: Boolean = true // Default to dark mode to prevent flashing
    
    // Single annotation manager and annotations for location pulse and center dot
    private var circleAnnotationManager: CircleAnnotationManager? = null
    private var pulseAnnotation: CircleAnnotation? = null
    private var centerDotAnnotation: CircleAnnotation? = null
    private var pulseAnimator: ValueAnimator? = null
    
    // Handler for periodic theme checks
    private val themeUpdateHandler = Handler(Looper.getMainLooper())
    private val themeUpdateRunnable = object : Runnable {
        override fun run() {
            currentLocation?.let { updateMapThemeBasedOnTime(it) }
            // Check every 5 minutes
            themeUpdateHandler.postDelayed(this, 5 * 60 * 1000)
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "HomeFragment"
        private const val PULSE_DURATION = 2000L // 2 seconds per pulse cycle
        private const val MAX_PULSE_RADIUS = 35.0 // Reduced by 30% from 50
        private const val MIN_PULSE_RADIUS = 3.5 // Reduced by 30% from 5
        private const val CENTER_DOT_RADIUS = 2.0 // Small center dot for the location puck
        
        // Camera state persistence
        private var savedCameraState: CameraState? = null
        private var hasInitializedCamera = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        // Initialize views
        mapView = view.findViewById(R.id.mapView)
        locationText = view.findViewById(R.id.locationText)
        logoImageView = view.findViewById(R.id.logoImageView)
        
        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Pre-determine theme before initializing map to prevent flashing
        determinateInitialTheme()
        
        // Initialize map
        initializeMap()
        
        return view
    }
    
    private fun determinateInitialTheme() {
        // Try to get last known location to determine theme immediately
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val sunLocation = com.luckycatlabs.sunrisesunset.dto.Location(it.latitude, it.longitude)
                    val calculator = SunriseSunsetCalculator(sunLocation, TimeZone.getDefault())
                    val now = Calendar.getInstance()
                    
                    val sunrise = calculator.getOfficialSunriseCalendarForDate(now)
                    val sunset = calculator.getOfficialSunsetCalendarForDate(now)
                    
                    isDarkMode = now.after(sunset) || now.before(sunrise)
                    currentMapStyle = if (isDarkMode) Style.DARK else Style.LIGHT
                    
                    // Update logo color immediately
                    updateLogoColor(isDarkMode)
                }
            }
        }
    }
    
    private fun initializeMap() {
        // Use the pre-determined style to prevent flashing
        mapView.getMapboxMap().apply {
            loadStyleUri(currentMapStyle) { style ->
                // Map style loaded
                Log.d(TAG, "Map style loaded: $currentMapStyle")
                
                // CRITICAL: Disable all Mapbox branding and UI elements
                mapView.location.enabled = false
                mapView.scalebar.enabled = false
                mapView.logo.enabled = false  // Disable Mapbox logo
                mapView.attribution.enabled = false  // Disable attribution icon
                
                // Disable rotation gesture while keeping pinch zoom and pan
                mapView.gestures.rotateEnabled = false
                mapView.gestures.pinchToZoomEnabled = true
                mapView.gestures.scrollEnabled = true
                
                // Initialize single annotation manager
                initializeAnnotationManager()
                
                // Restore saved camera position if available
                savedCameraState?.let { state ->
                    Log.d(TAG, "Restoring saved camera position")
                    val cameraOptions = CameraOptions.Builder()
                        .center(state.center)
                        .zoom(state.zoom)
                        .bearing(state.bearing)
                        .pitch(state.pitch)
                        .build()
                    
                    mapView.getMapboxMap().setCamera(cameraOptions)
                }
                
                // Check permissions and get location
                checkLocationPermission()
            }
        }
    }
    
    private fun initializeAnnotationManager() {
        val annotationApi = mapView.annotations
        circleAnnotationManager = annotationApi.createCircleAnnotationManager()
    }
    
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
            startLocationUpdates()
        }
    }
    
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = it
                updateLocationUI(it)
                
                // Only move to location on first initialization, not when returning to screen
                if (!hasInitializedCamera && savedCameraState == null) {
                    Log.d(TAG, "Initial camera setup - moving to current location")
                    moveToLocation(it.latitude, it.longitude, 15.0)
                    hasInitializedCamera = true
                } else {
                    Log.d(TAG, "Camera already initialized or saved state exists - not moving camera")
                }
                
                updateLocationPulse(it)
                updateMapThemeBasedOnTime(it)
                // Start periodic theme updates
                startThemeUpdates()
            }
        }
    }
    
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    updateLocationUI(location)
                    updateLocationPulse(location)
                    updateMapThemeBasedOnTime(location)
                    // Note: We do NOT move the camera on location updates
                }
            }
        }
        
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }
    
    private fun updateMapThemeBasedOnTime(location: Location) {
        // Create Location object for the calculator
        val sunLocation = com.luckycatlabs.sunrisesunset.dto.Location(location.latitude, location.longitude)
        val calculator = SunriseSunsetCalculator(sunLocation, TimeZone.getDefault())
        val now = Calendar.getInstance()
        
        val sunrise = calculator.getOfficialSunriseCalendarForDate(now)
        val sunset = calculator.getOfficialSunsetCalendarForDate(now)
        
        val shouldUseDarkTheme = now.after(sunset) || now.before(sunrise)
        
        if (shouldUseDarkTheme != isDarkMode) {
            isDarkMode = shouldUseDarkTheme
            val newStyle = if (isDarkMode) Style.DARK else Style.LIGHT
            
            // Save camera state before changing style
            saveCameraState()
            
            // Update map style
            mapView.getMapboxMap().loadStyleUri(newStyle) { style ->
                Log.d(TAG, "Theme switched to: ${if (isDarkMode) "DARK" else "LIGHT"}")
                currentMapStyle = newStyle
                
                // Re-disable all Mapbox branding after style change
                mapView.logo.enabled = false
                mapView.attribution.enabled = false
                mapView.scalebar.enabled = false
                
                // Re-initialize annotation manager after style change
                initializeAnnotationManager()
                
                // Restore camera position after style change
                savedCameraState?.let { state ->
                    val cameraOptions = CameraOptions.Builder()
                        .center(state.center)
                        .zoom(state.zoom)
                        .bearing(state.bearing)
                        .pitch(state.pitch)
                        .build()
                    
                    mapView.getMapboxMap().setCamera(cameraOptions)
                }
                
                // Recreate location pulse with new style
                currentLocation?.let { updateLocationPulse(it) }
            }
            
            // Update logo color
            updateLogoColor(isDarkMode)
        }
    }
    
    private fun getMapStyleForCurrentTime(location: Location): String {
        // Create Location object for the calculator
        val sunLocation = com.luckycatlabs.sunrisesunset.dto.Location(location.latitude, location.longitude)
        val calculator = SunriseSunsetCalculator(sunLocation, TimeZone.getDefault())
        val now = Calendar.getInstance()
        
        val sunrise = calculator.getOfficialSunriseCalendarForDate(now)
        val sunset = calculator.getOfficialSunsetCalendarForDate(now)
        
        isDarkMode = now.after(sunset) || now.before(sunrise)
        
        // Update logo color on initialization
        updateLogoColor(isDarkMode)
        
        return if (isDarkMode) Style.DARK else Style.LIGHT
    }
    
    private fun updateLogoColor(useDarkMode: Boolean) {
        // Update the Chat City logo tint
        // White for dark mode, black for light mode
        val colorFilter = if (useDarkMode) {
            PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        } else {
            PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
        }
        logoImageView.colorFilter = colorFilter
    }
    
    private fun startThemeUpdates() {
        themeUpdateHandler.removeCallbacks(themeUpdateRunnable)
        themeUpdateHandler.post(themeUpdateRunnable)
    }
    
    private fun stopThemeUpdates() {
        themeUpdateHandler.removeCallbacks(themeUpdateRunnable)
    }
    
    private fun updateLocationUI(location: Location) {
        scope.launch {
            val cityName = getCityName(location.latitude, location.longitude)
            locationText.text = cityName
        }
    }
    
    private suspend fun getCityName(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    // Priority: locality (city) > subAdminArea (county) > adminArea (state)
                    address.locality ?: address.subAdminArea ?: address.adminArea ?: "Unknown"
                } else {
                    "Unknown"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting city name", e)
                "Unknown"
            }
        }
    }
    
    private fun moveToLocation(latitude: Double, longitude: Double, zoom: Double) {
        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(longitude, latitude))
            .zoom(zoom)
            .build()
        
        mapView.getMapboxMap().flyTo(cameraOptions)
    }
    
    private fun updateLocationPulse(location: Location) {
        // Clear existing animation and annotations
        clearPulseAnimation()
        
        // Create center dot and pulse effect
        createLocationPuck(location)
    }
    
    private fun clearPulseAnimation() {
        // Cancel animator
        pulseAnimator?.cancel()
        pulseAnimator = null
        
        // Delete existing pulse annotation
        pulseAnnotation?.let { annotation ->
            circleAnnotationManager?.delete(annotation)
        }
        pulseAnnotation = null
        
        // Delete existing center dot annotation
        centerDotAnnotation?.let { annotation ->
            circleAnnotationManager?.delete(annotation)
        }
        centerDotAnnotation = null
    }
    
    private fun createLocationPuck(location: Location) {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        
        circleAnnotationManager?.let { manager ->
            // IMPORTANT: Create pulse FIRST so it's behind the center dot
            // Create the pulse ring animation (underneath)
            val pulseColor = "#33FB86BB" // Pink with 20% opacity for pulse
            
            pulseAnnotation = manager.create(
                CircleAnnotationOptions()
                    .withPoint(point)
                    .withCircleRadius(MIN_PULSE_RADIUS)
                    .withCircleColor(pulseColor)
                    .withCircleOpacity(0.5)
                    .withCircleStrokeWidth(0.0)
                    .withCircleBlur(0.3) // Add slight blur for softer edges
            )
            
            // Create center dot AFTER pulse so it appears on top
            val centerColor = "#FB86BB" // Pink color without transparency for center
            
            centerDotAnnotation = manager.create(
                CircleAnnotationOptions()
                    .withPoint(point)
                    .withCircleRadius(CENTER_DOT_RADIUS)
                    .withCircleColor(centerColor)
                    .withCircleOpacity(1.0) // Fully opaque center dot
                    .withCircleStrokeWidth(0.0)
            )
            
            // Animate only the pulse ring, not the center dot
            pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = PULSE_DURATION
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    
                    // Expand radius from MIN to MAX
                    val radius = MIN_PULSE_RADIUS + ((MAX_PULSE_RADIUS - MIN_PULSE_RADIUS) * progress)
                    
                    // Fade out as it expands (start at 50% opacity, fade to 0%)
                    val opacity = (1.0 - progress) * 0.5
                    
                    try {
                        if (isAdded && circleAnnotationManager != null && pulseAnnotation != null) {
                            // Update only the pulse annotation, not the center dot
                            pulseAnnotation?.circleRadius = radius
                            pulseAnnotation?.circleOpacity = opacity
                            pulseAnnotation?.let { manager.update(it) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating pulse animation", e)
                    }
                }
            }
            
            pulseAnimator?.start()
        }
    }
    
    private fun saveCameraState() {
        try {
            savedCameraState = mapView.getMapboxMap().cameraState
            Log.d(TAG, "Camera state saved: center=${savedCameraState?.center}, zoom=${savedCameraState?.zoom}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving camera state", e)
        }
    }
    
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }
    
    override fun onStop() {
        super.onStop()
        // Save camera state when leaving the fragment
        saveCameraState()
        mapView.onStop()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        clearPulseAnimation()
        stopThemeUpdates()
        mapView.onDestroy()
        scope.cancel()
    }
    
    override fun onPause() {
        super.onPause()
        // Save camera state when pausing
        saveCameraState()
        // Pause animation when fragment is not visible
        pulseAnimator?.pause()
        stopThemeUpdates()
    }
    
    override fun onResume() {
        super.onResume()
        // Resume animation when fragment becomes visible
        pulseAnimator?.resume()
        
        // Re-ensure Mapbox branding is hidden when resuming
        mapView.logo.enabled = false
        mapView.attribution.enabled = false
        mapView.scalebar.enabled = false
        
        // Resume theme updates
        currentLocation?.let {
            updateMapThemeBasedOnTime(it)
            startThemeUpdates()
        }
    }
}
