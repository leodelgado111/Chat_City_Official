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
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chatcityofficial.chatmapapp.R
import com.google.android.gms.location.*
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
import kotlinx.coroutines.*
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var locationText: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    
    // Single annotation manager and annotations for location
    private var circleAnnotationManager: CircleAnnotationManager? = null
    private var pulseAnnotations = mutableListOf<CircleAnnotation>()
    private val pulseAnimators = mutableListOf<ValueAnimator>()
    private val pulseHandler = Handler(Looper.getMainLooper())
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "HomeFragment"
        private const val PULSE_DURATION = 3000L // 3 seconds per pulse
        private const val PULSE_COUNT = 3 // Number of pulse rings
        private const val PULSE_DELAY = 1000L // Delay between each pulse start
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
        
        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Initialize map
        initializeMap()
        
        return view
    }
    
    private fun initializeMap() {
        mapView.getMapboxMap().apply {
            loadStyleUri(Style.MAPBOX_STREETS) { style ->
                // Map style loaded
                Log.d(TAG, "Map style loaded")
                
                // Disable the default location component completely
                mapView.location.enabled = false
                
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
        // Clear all existing animations and annotations
        clearPulseAnimations()
        
        // Create pulse effect with gradient colors
        createPulseEffect(location)
    }
    
    private fun clearPulseAnimations() {
        // Cancel all animators
        pulseAnimators.forEach { it.cancel() }
        pulseAnimators.clear()
        
        // Delete all existing pulse annotations
        circleAnnotationManager?.let { manager ->
            pulseAnnotations.forEach { annotation ->
                try {
                    manager.delete(annotation)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting annotation", e)
                }
            }
        }
        pulseAnnotations.clear()
        
        // Clear any pending pulse creation
        pulseHandler.removeCallbacksAndMessages(null)
    }
    
    private fun createPulseEffect(location: Location) {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        
        // Create multiple pulse rings with staggered animations
        for (i in 0 until PULSE_COUNT) {
            pulseHandler.postDelayed({
                if (isAdded && circleAnnotationManager != null) {
                    createSinglePulse(point, i)
                }
            }, i * PULSE_DELAY)
        }
    }
    
    private fun createSinglePulse(point: Point, index: Int) {
        circleAnnotationManager?.let { manager ->
            // Create gradient colors based on the SVG
            // Using the three colors from the conic gradient with transparency
            val colors = listOf(
                "#33FB86BB", // Pink with 20% opacity
                "#33A6AAD5", // Purple with 20% opacity  
                "#3397D4F0"  // Blue with 20% opacity
            )
            
            // Create a pulse annotation
            val pulseAnnotation = manager.create(
                CircleAnnotationOptions()
                    .withPoint(point)
                    .withCircleRadius(0.0)
                    .withCircleColor(colors[index % colors.size])
                    .withCircleOpacity(0.0)
                    .withCircleStrokeWidth(0.0)
            )
            
            pulseAnnotations.add(pulseAnnotation)
            
            // Animate the pulse
            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = PULSE_DURATION
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    
                    // Expand radius from 5 to 50 meters
                    val radius = 5.0 + (45.0 * progress)
                    
                    // Fade out as it expands
                    val opacity = (1.0 - progress) * 0.3 // Max 30% opacity
                    
                    try {
                        if (isAdded && circleAnnotationManager != null) {
                            // Update the annotation
                            pulseAnnotation.circleRadius = radius
                            pulseAnnotation.circleOpacity = opacity
                            manager.update(pulseAnnotation)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating pulse animation", e)
                    }
                }
            }
            
            pulseAnimators.add(animator)
            animator.start()
        }
        
        // Add center dot (always visible)
        circleAnnotationManager?.let { manager ->
            val centerDot = manager.create(
                CircleAnnotationOptions()
                    .withPoint(point)
                    .withCircleRadius(4.0)
                    .withCircleColor("#FB86BB") // Solid pink color for center
                    .withCircleOpacity(1.0)
                    .withCircleStrokeWidth(2.0)
                    .withCircleStrokeColor("#FFFFFF")
            )
            pulseAnnotations.add(centerDot)
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
        clearPulseAnimations()
        mapView.onDestroy()
        scope.cancel()
    }
    
    override fun onPause() {
        super.onPause()
        // Pause animations when fragment is not visible
        pulseAnimators.forEach { it.pause() }
    }
    
    override fun onResume() {
        super.onResume()
        // Resume animations when fragment becomes visible
        pulseAnimators.forEach { it.resume() }
    }
}
