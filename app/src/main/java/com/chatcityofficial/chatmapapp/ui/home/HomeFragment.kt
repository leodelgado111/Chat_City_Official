package com.chatcityofficial.chatmapapp.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chatcityofficial.chatmapapp.R
import com.google.android.gms.location.*
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.attribution.attribution

class HomeFragment : Fragment() {

    private var mapView: MapView? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var isLocationPermissionGranted = false
    private var isMapReady = false
    private var hasCenteredOnLocation = false
    
    // Google Location Services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    
    // Location tracking listeners
    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView?.getMapboxMap()?.setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        if (!hasCenteredOnLocation) {
            // Only center automatically on first location update
            mapView?.getMapboxMap()?.setCamera(
                CameraOptions.Builder()
                    .center(it)
                    .zoom(14.0)
                    .build()
            )
            hasCenteredOnLocation = true
        }
        mapView?.gestures?.focalPoint = mapView?.getMapboxMap()?.pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                    Log.d("HomeFragment", "📍 Location update: ${location.latitude}, ${location.longitude}")
                    
                    // Center map on first location if not already centered
                    if (isMapReady && !hasCenteredOnLocation) {
                        centerMapOnLocation(location)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        
        mapView = root.findViewById(R.id.mapView)
        
        // Check for location permissions
        checkLocationPermissions()
        
        // Load map style
        mapView?.getMapboxMap()?.loadStyleUri("mapbox://styles/mapbox/light-v11") { style ->
            if (style != null) {
                Log.d("HomeFragment", "✅ Map style loaded")
                isMapReady = true
                
                // Remove unnecessary UI elements
                mapView?.scalebar?.enabled = false
                mapView?.logo?.enabled = false
                mapView?.attribution?.enabled = false
                
                // Setup location component
                setupLocationComponent()
                
                // Try to get current location if permission granted
                if (isLocationPermissionGranted) {
                    requestCurrentLocation()
                }
            } else {
                Log.e("HomeFragment", "❌ Map style failed to load")
                Toast.makeText(context, "❌ Map style failed to load", Toast.LENGTH_SHORT).show()
            }
        }
        
        return root
    }
    
    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            isLocationPermissionGranted = true
            Log.d("HomeFragment", "✅ Location permission already granted")
        } else {
            Log.d("HomeFragment", "❌ Requesting location permission")
            // Request permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun setupLocationComponent() {
        // Enable location component
        mapView?.location?.apply {
            enabled = true
            pulsingEnabled = true
            
            // Add listeners for location updates
            addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        }
        
        // Add gesture listener
        mapView?.gestures?.addOnMoveListener(onMoveListener)
        
        Log.d("HomeFragment", "✅ Location component setup complete")
    }
    
    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {
        if (!isLocationPermissionGranted) {
            Log.e("HomeFragment", "❌ Cannot request location - permission not granted")
            return
        }
        
        Log.d("HomeFragment", "📍 Requesting current location...")
        
        // Try to get last known location first
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Log.d("HomeFragment", "✅ Got last known location: ${location.latitude}, ${location.longitude}")
                if (isMapReady && !hasCenteredOnLocation) {
                    centerMapOnLocation(location)
                }
            } else {
                Log.d("HomeFragment", "⚠️ Last location is null, requesting location updates")
                // Request location updates if last location is null
                startLocationUpdates()
            }
        }.addOnFailureListener { e ->
            Log.e("HomeFragment", "❌ Failed to get location: ${e.message}")
            // Try requesting location updates as fallback
            startLocationUpdates()
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!isLocationPermissionGranted) {
            return
        }
        
        Log.d("HomeFragment", "📍 Starting location updates...")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
    
    private fun stopLocationUpdates() {
        Log.d("HomeFragment", "⏹️ Stopping location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    private fun centerMapOnLocation(location: Location) {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        
        mapView?.getMapboxMap()?.setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(14.0)
                .build()
        )
        
        hasCenteredOnLocation = true
        Log.d("HomeFragment", "✅ Map centered on location: ${location.latitude}, ${location.longitude}")
        
        // Stop location updates once we've centered the map
        stopLocationUpdates()
    }
    
    private fun onCameraTrackingDismissed() {
        mapView?.location?.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView?.location?.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView?.gestures?.removeOnMoveListener(onMoveListener)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isLocationPermissionGranted = true
                Toast.makeText(context, "Location permission granted", Toast.LENGTH_SHORT).show()
                Log.d("HomeFragment", "✅ Location permission granted by user")
                
                // Setup location component and request location now that we have permission
                if (isMapReady) {
                    setupLocationComponent()
                    requestCurrentLocation()
                }
            } else {
                Toast.makeText(
                    context,
                    "Location permission denied. Map will not center on your location.",
                    Toast.LENGTH_LONG
                ).show()
                Log.d("HomeFragment", "❌ Location permission denied by user")
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Request location updates when fragment resumes
        if (isLocationPermissionGranted && !hasCenteredOnLocation) {
            requestCurrentLocation()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop location updates when fragment pauses
        stopLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLocationUpdates()
        mapView?.location?.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView?.location?.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView?.gestures?.removeOnMoveListener(onMoveListener)
        mapView?.onDestroy()
        mapView = null
    }
}