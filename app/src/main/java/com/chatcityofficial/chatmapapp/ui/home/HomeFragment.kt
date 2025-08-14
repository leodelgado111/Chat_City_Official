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
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import kotlinx.coroutines.*
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var locationText: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var logoImageView: ImageView
    private var currentLocation: Location? = null
    private var currentMapStyle: String = Style.MAPBOX_STREETS
    private var isDarkMode: Boolean = false
    
    // Single annotation manager and annotation for location pulse
    private var circleAnnotationManager: CircleAnnotationManager? = null
    private var pulseAnnotation: CircleAnnotation? = null
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
        
        // Initialize map
        initializeMap()
        
        return view
    }
    
    private fun initializeMap() {
        // Determine initial theme based on current time if location is available
        val initialStyle = if (currentLocation != null) {
            getMapStyleForCurrentTime(currentLocation!!)
        } else {
            Style.MAPBOX_STREETS
        }
        
        mapView.getMapboxMap().apply {
            loadStyleUri(initialStyle) { style ->
                // Map style loaded
                Log.d(TAG, "Map style loaded: $initialStyle")
                
                // Disable the default location component completely
                mapView.location.enabled = false
                
                // CRITICAL: Disable the scale bar (mile-radius bar) completely
                mapView.scalebar.enabled = false
                
                // Disable rotation gesture while keeping pinch zoom and pan
                mapView.gestures.rotateEnabled = false
                mapView.gestures.pinchToZoomEnabled = true
                mapView.gestures.scrollEnabled = true
                
                // Initialize single annotation manager
                initializeAnnotationManager()
                
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
                moveToLocation(it.latitude, it.longitude, 15.0)
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
            
            // Update map style
            mapView.getMapboxMap().loadStyleUri(newStyle) { style ->
                Log.d(TAG, "Theme switched to: ${if (isDarkMode) "DARK" else "LIGHT"}")
                currentMapStyle = newStyle
                
                // Re-initialize annotation manager after style change
                initializeAnnotationManager()
                
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
        // Clear existing animation and annotation
        clearPulseAnimation()
        
        // Create single pulse effect
        createPulseEffect(location)
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
    }
    
    private fun createPulseEffect(location: Location) {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        
        circleAnnotationManager?.let { manager ->
            // Create a single pulse ring with gradient-inspired color
            // Using a blend of the gradient colors with transparency
            // This approximates the conic gradient effect from your SVG
            val pulseColor = "#33FB86BB" // Pink with 20% opacity (main gradient color)
            
            // Create the pulse annotation
            pulseAnnotation = manager.create(
                CircleAnnotationOptions()
                    .withPoint(point)
                    .withCircleRadius(MIN_PULSE_RADIUS)
                    .withCircleColor(pulseColor)
                    .withCircleOpacity(0.5)
                    .withCircleStrokeWidth(0.0)
                    .withCircleBlur(0.3) // Add slight blur for softer edges
            )
            
            // Animate the single pulse ring
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
                            // Update the annotation
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
    
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }
    
    override fun onStop() {
        super.onStop()
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
        // Pause animation when fragment is not visible
        pulseAnimator?.pause()
        stopThemeUpdates()
    }
    
    override fun onResume() {
        super.onResume()
        // Resume animation when fragment becomes visible
        pulseAnimator?.resume()
        // Resume theme updates
        currentLocation?.let {
            updateMapThemeBasedOnTime(it)
            startThemeUpdates()
        }
    }
}
