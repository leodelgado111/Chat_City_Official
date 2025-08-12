package com.chatcityofficial.chatmapapp.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chatcityofficial.chatmapapp.R
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
    
    // Location tracking listeners
    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView?.getMapboxMap()?.setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView?.getMapboxMap()?.setCamera(CameraOptions.Builder().center(it).build())
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
                
                // Remove unnecessary UI elements
                mapView?.scalebar?.enabled = false
                mapView?.logo?.enabled = false
                mapView?.attribution?.enabled = false
                
                // Setup location component if permission granted
                if (isLocationPermissionGranted) {
                    setupLocationComponent()
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
        } else {
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
        
        // Get current location and center map
        mapView?.location?.locationProvider?.lastLocation?.let { location ->
            val point = Point.fromLngLat(location.longitude, location.latitude)
            mapView?.getMapboxMap()?.setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(14.0)
                    .build()
            )
            Log.d("HomeFragment", "📍 Centered on user location: ${location.latitude}, ${location.longitude}")
        }
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
                
                // Setup location component now that we have permission
                setupLocationComponent()
            } else {
                Toast.makeText(
                    context,
                    "Location permission denied. Map will not center on your location.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
        mapView?.location?.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView?.location?.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView?.gestures?.removeOnMoveListener(onMoveListener)
        mapView?.onDestroy()
        mapView = null
    }
}