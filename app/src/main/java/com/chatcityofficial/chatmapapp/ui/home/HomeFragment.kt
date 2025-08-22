package com.chatcityofficial.chatmapapp.ui.home

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatcityofficial.chatmapapp.R
import com.chatcityofficial.chatmapapp.ui.compose.components.LocationButtonView
import com.chatcityofficial.chatmapapp.ui.compose.components.ThemedLocationContainerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import kotlinx.coroutines.*
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var logoImageView: ImageView
    private lateinit var chatBubbleOverlay: ImageView
    private var currentLocation: Location? = null
    private var currentMapStyle: String = Style.DARK
    private var isDarkMode: Boolean = true
    
    // Search UI elements
    private lateinit var defaultViewContainer: LinearLayout
    private lateinit var searchContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var searchBackButton: ImageView
    private lateinit var searchClearButton: ImageView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var locationContainer: ThemedLocationContainerView
    
    // Root view for touch interception
    private lateinit var rootView: View
    
    // Places API
    private lateinit var placesClient: PlacesClient
    private lateinit var searchAdapter: PlaceSearchAdapter
    private var sessionToken: AutocompleteSessionToken? = null
    private var isSearchVisible = false
    
    // Annotation managers
    private var circleAnnotationManager: CircleAnnotationManager? = null
    private var polygonAnnotationManager: PolygonAnnotationManager? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var pulseAnnotation: CircleAnnotation? = null
    private var centerDotAnnotation: CircleAnnotation? = null
    private var placeMarkerAnnotation: PointAnnotation? = null
    
    // Chat bubbles
    private val chatBubbles = mutableListOf<PointAnnotation>()
    private var currentChatBubbleIndex = 0
    private var currentZoomLevel = 10.0
    private val ZOOM_THRESHOLD = 17.0  // Zoom level below which bubbles are minimized
    private var zoomTransitionAnimator: ValueAnimator? = null
    private var currentBubbleScale = 0f
    
    // Cluj-Napoca chat bubble locations with text
    data class ChatBubbleData(val location: Point, val text: String, val author: String)
    
    private val clujChatBubbleData = listOf(
        ChatBubbleData(
            Point.fromLngLat(23.5897, 46.7712), 
            "Central Park is amazing in the morning! Perfect for jogging 🏃‍♂️",
            "Alex M."
        ),
        ChatBubbleData(
            Point.fromLngLat(23.5924, 46.7684), 
            "Best coffee shop near Union Square! Try the espresso ☕",
            "Maria P."
        ),
        ChatBubbleData(
            Point.fromLngLat(23.5862, 46.7731),
            "The Botanical Garden has beautiful roses this season 🌹",
            "Stefan R."
        ),
        ChatBubbleData(
            Point.fromLngLat(23.5955, 46.7698),  // Near Cluj Arena area
            "Hey everyone! It's super sunny outside - make sure to wear sunscreen today.",
            "Community"
        )
    )
    private var pulseAnimator: ValueAnimator? = null
    
    // Handler for periodic theme checks
    private val themeUpdateHandler = Handler(Looper.getMainLooper())
    private var cameraUpdateJob: Job? = null
    private val themeUpdateRunnable = object : Runnable {
        override fun run() {
            currentLocation?.let { updateMapThemeBasedOnTime(it) }
            themeUpdateHandler.postDelayed(this, 5 * 60 * 1000)
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "HomeFragment"
        private const val PULSE_DURATION = 2000L
        private const val MAX_PULSE_RADIUS = 35.0
        private const val MIN_PULSE_RADIUS = 3.5
        private const val CENTER_DOT_RADIUS = 2.0
        private const val ANIMATION_DURATION = 100L
        
        // Camera state persistence - PERSISTENT across app lifecycle
        private var savedCameraState: CameraState? = null
        private var hasEverInitialized = false
        private const val PREFS_NAME = "ChatCityPrefs"
        private const val KEY_CAMERA_LAT = "camera_lat"
        private const val KEY_CAMERA_LNG = "camera_lng"
        private const val KEY_CAMERA_ZOOM = "camera_zoom"
        private const val KEY_CAMERA_BEARING = "camera_bearing"
        private const val KEY_CAMERA_PITCH = "camera_pitch"
        private const val KEY_HAS_SAVED_CAMERA = "has_saved_camera"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        rootView = view
        
        // Load persisted camera state from SharedPreferences
        loadPersistedCameraState()
        
        // Initialize views
        mapView = view.findViewById(R.id.mapView)
        logoImageView = view.findViewById(R.id.logoImageView)
        chatBubbleOverlay = view.findViewById(R.id.chatBubbleOverlay)
        
        // Set initial logo color based on current theme (defaults to dark)
        Log.d(TAG, "Setting initial logo color, isDarkMode: $isDarkMode")
        updateLogoColor(isDarkMode)
        
        // Initialize search views
        defaultViewContainer = view.findViewById(R.id.defaultViewContainer)
        searchContainer = view.findViewById(R.id.searchContainer)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchBackButton = view.findViewById(R.id.searchBackButton)
        searchClearButton = view.findViewById(R.id.searchClearButton)
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView)
        locationContainer = view.findViewById(R.id.locationContainer)
        
        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Determine initial theme after location client is initialized
        determinateInitialTheme()
        
        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_api_key))
        }
        placesClient = Places.createClient(requireContext())
        
        // Setup search adapter
        searchAdapter = PlaceSearchAdapter { prediction ->
            onPlaceSelected(prediction)
        }
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchResultsRecyclerView.adapter = searchAdapter
        
        // Initialize map (theme already determined above)
        initializeMap()
        
        // Setup click listeners
        setupSearchListeners()
        
        return view
    }
    
    private fun loadPersistedCameraState() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasSavedCamera = prefs.getBoolean(KEY_HAS_SAVED_CAMERA, false)
        
        if (hasSavedCamera) {
            val lat = prefs.getFloat(KEY_CAMERA_LAT, 0f).toDouble()
            val lng = prefs.getFloat(KEY_CAMERA_LNG, 0f).toDouble()
            val zoom = prefs.getFloat(KEY_CAMERA_ZOOM, 15f).toDouble()
            val bearing = prefs.getFloat(KEY_CAMERA_BEARING, 0f).toDouble()
            val pitch = prefs.getFloat(KEY_CAMERA_PITCH, 0f).toDouble()
            
            savedCameraState = null
            hasEverInitialized = true
            
            Log.d(TAG, "Loaded persisted camera state: lat=$lat, lng=$lng, zoom=$zoom")
        }
    }
    
    private fun persistCameraState() {
        try {
            val currentState = mapView.getMapboxMap().cameraState
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            currentState.center?.let { center ->
                prefs.edit().apply {
                    putFloat(KEY_CAMERA_LAT, center.latitude().toFloat())
                    putFloat(KEY_CAMERA_LNG, center.longitude().toFloat())
                    putFloat(KEY_CAMERA_ZOOM, currentState.zoom.toFloat())
                    putFloat(KEY_CAMERA_BEARING, currentState.bearing.toFloat())
                    putFloat(KEY_CAMERA_PITCH, currentState.pitch.toFloat())
                    putBoolean(KEY_HAS_SAVED_CAMERA, true)
                    apply()
                }
                
                Log.d(TAG, "Persisted camera state: lat=${center.latitude()}, lng=${center.longitude()}, zoom=${currentState.zoom}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting camera state", e)
        }
    }
    
    private fun setupSearchListeners() {
        // Make the entire location container clickable
        locationContainer.setOnLocationClickListener {
            if (!isSearchVisible) {
                showSearchView()
            }
        }
        
        
        // Back button - hide search
        searchBackButton.setOnClickListener {
            hideSearchView()
        }
        
        // Clear button
        searchClearButton.setOnClickListener {
            searchEditText.text.clear()
            searchClearButton.visibility = View.GONE
            searchAdapter.submitList(emptyList())
        }
        
        // Search text changes
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                searchClearButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                
                if (query.length > 2) {
                    performPlaceSearch(query)
                } else {
                    searchAdapter.submitList(emptyList())
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    // Public method to check if search is visible (for back button handling)
    fun isSearchViewVisible(): Boolean = isSearchVisible
    
    // Public method to hide search (for back button handling)
    fun hideSearchViewIfVisible(): Boolean {
        return if (isSearchVisible) {
            hideSearchView()
            true
        } else {
            false
        }
    }
    
    private fun showSearchView() {
        if (isSearchVisible) return
        isSearchVisible = true
        
        Log.d(TAG, "SEARCH VIEW OPENING - Map click listener will be active")
        
        // Create new session token for billing
        sessionToken = AutocompleteSessionToken.newInstance()
        
        // Fade out default views
        defaultViewContainer.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    defaultViewContainer.visibility = View.GONE
                    
                    // Fade in search views
                    searchContainer.visibility = View.VISIBLE
                    searchResultsRecyclerView.visibility = View.VISIBLE
                    
                    searchContainer.animate()
                        .alpha(1f)
                        .setDuration(ANIMATION_DURATION)
                        .setListener(null)
                    
                    searchResultsRecyclerView.animate()
                        .alpha(1f)
                        .setDuration(ANIMATION_DURATION)
                        .setListener(null)
                    
                    // Show keyboard
                    searchEditText.requestFocus()
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
                }
            })
    }
    
    private fun hideSearchView() {
        if (!isSearchVisible) return
        isSearchVisible = false
        
        Log.d(TAG, "SEARCH VIEW CLOSING")
        
        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        
        // Clear search
        searchEditText.text.clear()
        searchAdapter.submitList(emptyList())
        
        // Fade out search views
        searchContainer.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    searchContainer.visibility = View.GONE
                    searchResultsRecyclerView.visibility = View.GONE
                    
                    // Fade in default views
                    defaultViewContainer.visibility = View.VISIBLE
                    defaultViewContainer.animate()
                        .alpha(1f)
                        .setDuration(ANIMATION_DURATION)
                        .setListener(null)
                }
            })
        
        searchResultsRecyclerView.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .setListener(null)
    }
    
    private fun performPlaceSearch(query: String) {
        // Create autocomplete request
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(sessionToken)
            .setLocationBias(
                currentLocation?.let {
                    RectangularBounds.newInstance(
                        LatLng(it.latitude - 0.1, it.longitude - 0.1),
                        LatLng(it.latitude + 0.1, it.longitude + 0.1)
                    )
                }
            )
            .build()
        
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                searchAdapter.submitList(response.autocompletePredictions)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Place search failed", exception)
            }
    }
    
    private fun onPlaceSelected(prediction: AutocompletePrediction) {
        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        
        // Update search text with selected place
        searchEditText.setText(prediction.getPrimaryText(null).toString())
        
        // Fetch place details
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.LAT_LNG,
            Place.Field.VIEWPORT
        )
        
        val fetchPlaceRequest = FetchPlaceRequest.builder(prediction.placeId, placeFields)
            .setSessionToken(sessionToken)
            .build()
        
        placesClient.fetchPlace(fetchPlaceRequest)
            .addOnSuccessListener { response ->
                val place = response.place
                place.latLng?.let { latLng ->
                    // Move camera to place
                    moveToLocation(latLng.latitude, latLng.longitude, 17.0)
                    
                    // Add outline around place
                    addPlaceMarker(latLng, place.viewport)
                }
                
                // Hide search view after selection
                hideSearchView()
                
                // New session token for next search
                sessionToken = AutocompleteSessionToken.newInstance()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Place fetch failed", exception)
                // Still hide search view even if fetch fails
                hideSearchView()
            }
    }
    
    private fun addPlaceMarker(center: LatLng, viewport: LatLngBounds?) {
        // Remove existing marker
        placeMarkerAnnotation?.let {
            pointAnnotationManager?.delete(it)
            placeMarkerAnnotation = null
        }
        
        // Create point annotation manager if needed
        if (pointAnnotationManager == null) {
            val annotationApi = mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager()
        }
        
        // Create the marker at the center of the place
        pointAnnotationManager?.let { manager ->
            try {
                // Create custom bitmap from vector drawable
                val drawable = androidx.core.content.ContextCompat.getDrawable(
                    requireContext(), 
                    R.drawable.ic_map_marker_layered
                )
                
                if (drawable != null) {
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    
                    // Create the marker annotation
                    placeMarkerAnnotation = manager.create(
                        PointAnnotationOptions()
                            .withPoint(Point.fromLngLat(center.longitude, center.latitude))
                            .withIconImage(bitmap)
                            .withIconSize(1.2) // Slightly larger for visibility
                    )
                    
                    Log.d(TAG, "Place marker created at ${center.latitude}, ${center.longitude}")
                } else {
                    Log.e(TAG, "Failed to load marker drawable")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating place marker", e)
            }
        }
    }
    
    private fun determinateInitialTheme() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    determineThemeFromLocation(location)
                } else {
                    Log.w(TAG, "Location is null, trying to get current location")
                    // If no location, try to get current location
                    getCurrentLocation()
                    // For now, determine theme based on current time for a default location (Cluj-Napoca)
                    val defaultLocation = Location("default").apply {
                        latitude = 46.7712  // Cluj-Napoca
                        longitude = 23.6236
                    }
                    determineThemeFromLocation(defaultLocation)
                }
            }
        } else {
            Log.w(TAG, "No location permission, using default dark theme")
            updateLogoColor(isDarkMode)
        }
    }
    
    private fun determineThemeFromLocation(location: Location) {
        try {
            val sunLocation = com.luckycatlabs.sunrisesunset.dto.Location(location.latitude, location.longitude)
            val calculator = SunriseSunsetCalculator(sunLocation, TimeZone.getDefault())
            val now = Calendar.getInstance()
            
            val sunrise = calculator.getOfficialSunriseCalendarForDate(now)
            val sunset = calculator.getOfficialSunsetCalendarForDate(now)
            
            val wasInDarkMode = isDarkMode
            
            // Check if current time is between sunset and sunrise (next day)
            val currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val sunriseInMinutes = sunrise.get(Calendar.HOUR_OF_DAY) * 60 + sunrise.get(Calendar.MINUTE)
            val sunsetInMinutes = sunset.get(Calendar.HOUR_OF_DAY) * 60 + sunset.get(Calendar.MINUTE)
            
            // Dark mode should be active from sunset to sunrise
            isDarkMode = currentTimeInMinutes >= sunsetInMinutes || currentTimeInMinutes < sunriseInMinutes
            currentMapStyle = if (isDarkMode) Style.DARK else Style.LIGHT
            
            Log.d(TAG, "Location: lat=${location.latitude}, lng=${location.longitude}")
            Log.d(TAG, "Theme determined - Dark mode: $isDarkMode (was: $wasInDarkMode)")
            Log.d(TAG, "Current time: ${now.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", now.get(Calendar.MINUTE))} ($currentTimeInMinutes min)")
            Log.d(TAG, "Sunrise: ${sunrise.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", sunrise.get(Calendar.MINUTE))} ($sunriseInMinutes min)")
            Log.d(TAG, "Sunset: ${sunset.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", sunset.get(Calendar.MINUTE))} ($sunsetInMinutes min)")
            
            // Always update logo color
            view?.post {
                updateLogoColor(isDarkMode)
            }
            
            // Update map style if it's different
            if (wasInDarkMode != isDarkMode) {
                mapView.getMapboxMap().loadStyleUri(currentMapStyle) { style ->
                    Log.d(TAG, "Map style changed to: $currentMapStyle")
                    view?.post {
                        updateLogoColor(isDarkMode)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error determining theme from location", e)
            // Default to dark mode on error
            isDarkMode = true
            currentMapStyle = Style.DARK
            view?.post {
                updateLogoColor(isDarkMode)
            }
        }
    }
    
    private fun initializeMap() {
        mapView.getMapboxMap().apply {
            // Add camera change listener to update location text and handle zoom
            addOnCameraChangeListener {
                val center = cameraState.center
                center?.let {
                    updateLocationUIFromCamera(it.latitude(), it.longitude())
                }
                
                // Check zoom level and update chat bubbles
                val newZoomLevel = cameraState.zoom
                if ((currentZoomLevel >= ZOOM_THRESHOLD && newZoomLevel < ZOOM_THRESHOLD) ||
                    (currentZoomLevel < ZOOM_THRESHOLD && newZoomLevel >= ZOOM_THRESHOLD)) {
                    currentZoomLevel = newZoomLevel
                    updateChatBubblesForZoom()
                }
                currentZoomLevel = newZoomLevel
            }
            
            loadStyleUri(currentMapStyle) { style ->
                Log.d(TAG, "Map style loaded: $currentMapStyle")
                
                // Force update logo color based on current theme
                view?.post {
                    updateLogoColor(isDarkMode)
                }
                
                // Disable all Mapbox branding and UI elements
                mapView.location.enabled = false
                mapView.scalebar.enabled = false
                mapView.logo.enabled = false
                mapView.attribution.enabled = false
                
                // Disable rotation gesture while keeping pinch zoom and pan
                mapView.gestures.rotateEnabled = false
                mapView.gestures.pinchToZoomEnabled = true
                mapView.gestures.scrollEnabled = true
                
                // CRITICAL FIX: Add map click listener using the gestures plugin
                mapView.gestures.addOnMapClickListener { point ->
                    if (isSearchVisible) {
                        Log.d(TAG, "Map clicked while search is visible - closing search")
                        hideSearchView()
                        true // Consume the click event
                    } else {
                        false // Let other click handlers process it
                    }
                }
                
                // Initialize annotation managers
                initializeAnnotationManager()
                
                // Load saved camera position from SharedPreferences if available
                val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val hasSavedCamera = prefs.getBoolean(KEY_HAS_SAVED_CAMERA, false)
                
                if (hasSavedCamera) {
                    val lat = prefs.getFloat(KEY_CAMERA_LAT, 0f).toDouble()
                    val lng = prefs.getFloat(KEY_CAMERA_LNG, 0f).toDouble()
                    val zoom = prefs.getFloat(KEY_CAMERA_ZOOM, 15f).toDouble()
                    val bearing = prefs.getFloat(KEY_CAMERA_BEARING, 0f).toDouble()
                    val pitch = prefs.getFloat(KEY_CAMERA_PITCH, 0f).toDouble()
                    
                    Log.d(TAG, "Restoring persisted camera position")
                    val cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(lng, lat))
                        .zoom(zoom)
                        .bearing(bearing)
                        .pitch(pitch)
                        .build()
                    
                    mapView.getMapboxMap().setCamera(cameraOptions)
                    hasEverInitialized = true
                } else if (savedCameraState != null) {
                    // Fallback to in-memory saved state
                    Log.d(TAG, "Restoring in-memory camera position")
                    savedCameraState?.let { state ->
                        val cameraOptions = CameraOptions.Builder()
                            .center(state.center)
                            .zoom(state.zoom)
                            .bearing(state.bearing)
                            .pitch(state.pitch)
                            .build()
                        
                        mapView.getMapboxMap().setCamera(cameraOptions)
                    }
                }
                
                // Check permissions and get location
                checkLocationPermission()
            }
        }
    }
    
    private fun initializeAnnotationManager() {
        val annotationApi = mapView.annotations
        circleAnnotationManager = annotationApi.createCircleAnnotationManager()
        polygonAnnotationManager = annotationApi.createPolygonAnnotationManager()
        
        // Add chat bubbles in Cluj-Napoca
        addChatBubbles()
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
                
                // Only center on location if this is the very first time ever
                // Check SharedPreferences to see if we have any saved camera state
                val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val hasSavedCamera = prefs.getBoolean(KEY_HAS_SAVED_CAMERA, false)
                
                if (!hasSavedCamera && !hasEverInitialized) {
                    Log.d(TAG, "First time ever - centering on current location")
                    moveToLocation(it.latitude, it.longitude, 15.0)
                    hasEverInitialized = true
                } else {
                    Log.d(TAG, "Using saved camera position - not centering on location")
                }
                
                updateLocationPulse(it)
                updateMapThemeBasedOnTime(it)
                startThemeUpdates()
            }
        }
    }
    
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000  // Update every 5 seconds instead of 10
            fastestInterval = 2000  // Allow updates as fast as every 2 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 5f  // Update only if moved at least 5 meters
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
        determineThemeFromLocation(location)
    }
    
    private fun updateLogoColor(useDarkMode: Boolean) {
        // Switch between white and black logo drawables
        val logoResource = if (useDarkMode) {
            R.drawable.chat_city_logo_pure_white  // Use pure white logo for dark mode
        } else {
            R.drawable.chat_city_logo_with_shadow
        }
        logoImageView.setImageResource(logoResource)
        logoImageView.invalidate() // Force redraw
        logoImageView.requestLayout() // Force layout update
        Log.d(TAG, "Logo switched to: ${if (useDarkMode) "PURE WHITE" else "BLACK"} version, resource ID: $logoResource")
        
        // Log the actual drawable being used
        val drawable = logoImageView.drawable
        Log.d(TAG, "Logo drawable: $drawable")
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
            locationContainer.setLocationText(cityName)
        }
    }
    
    private fun updateLocationUIFromCamera(latitude: Double, longitude: Double) {
        // Cancel previous job to debounce rapid camera changes
        cameraUpdateJob?.cancel()
        cameraUpdateJob = scope.launch {
            delay(500) // Wait 500ms to avoid too many geocoding requests
            val cityName = getCityName(latitude, longitude)
            locationContainer.setLocationText(cityName)
        }
    }
    
    private suspend fun getCityName(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
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
    
    private fun animateToCurrentLocation() {
        currentLocation?.let { location ->
            val cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .zoom(16.0)
                .build()
            
            mapView.getMapboxMap().flyTo(cameraOptions)
        } ?: run {
            // If no current location, try to get it
            getCurrentLocation()
        }
    }
    
    private fun updateLocationPulse(location: Location) {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        
        // If annotations exist, just update their position
        if (centerDotAnnotation != null && pulseAnnotation != null && circleAnnotationManager != null) {
            centerDotAnnotation?.point = point
            pulseAnnotation?.point = point
            
            circleAnnotationManager?.let { manager ->
                centerDotAnnotation?.let { manager.update(it) }
                pulseAnnotation?.let { manager.update(it) }
            }
        } else {
            // Only create new annotations if they don't exist
            clearPulseAnimation()
            createLocationPuck(location)
        }
    }
    
    private fun clearPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        
        pulseAnnotation?.let { annotation ->
            circleAnnotationManager?.delete(annotation)
        }
        pulseAnnotation = null
        
        centerDotAnnotation?.let { annotation ->
            circleAnnotationManager?.delete(annotation)
        }
        centerDotAnnotation = null
    }
    
    private fun createLocationPuck(location: Location) {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        
        circleAnnotationManager?.let { manager ->
            // Create pulse FIRST so it's behind the center dot
            val pulseColor = "#33FB86BB"
            
            pulseAnnotation = manager.create(
                CircleAnnotationOptions()
                    .withPoint(point)
                    .withCircleRadius(MIN_PULSE_RADIUS)
                    .withCircleColor(pulseColor)
                    .withCircleOpacity(0.5)
                    .withCircleStrokeWidth(0.0)
                    .withCircleBlur(0.3)
            )
            
            // Create center dot AFTER pulse so it appears on top
            val centerColor = "#FB86BB"
            
            centerDotAnnotation = manager.create(
                CircleAnnotationOptions()
                    .withPoint(point)
                    .withCircleRadius(CENTER_DOT_RADIUS)
                    .withCircleColor(centerColor)
                    .withCircleOpacity(1.0)
                    .withCircleStrokeWidth(0.0)
            )
            
            // Animate only the pulse ring
            pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = PULSE_DURATION
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    
                    val radius = MIN_PULSE_RADIUS + ((MAX_PULSE_RADIUS - MIN_PULSE_RADIUS) * progress)
                    val opacity = (1.0 - progress) * 0.5
                    
                    try {
                        if (isAdded && circleAnnotationManager != null && pulseAnnotation != null) {
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
            persistCameraState() // Also persist to SharedPreferences
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
        saveCameraState()
        pulseAnimator?.pause()
        stopThemeUpdates()
    }
    
    override fun onResume() {
        super.onResume()
        pulseAnimator?.resume()
        
        mapView.logo.enabled = false
        mapView.attribution.enabled = false
        mapView.scalebar.enabled = false
        
        currentLocation?.let {
            updateMapThemeBasedOnTime(it)
            startThemeUpdates()
        }
    }
    
    private fun addChatBubbles() {
        // Create point annotation manager if needed
        if (pointAnnotationManager == null) {
            val annotationApi = mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager()
        }
        
        pointAnnotationManager?.let { manager ->
            // Clear existing chat bubbles
            chatBubbles.forEach { manager.delete(it) }
            chatBubbles.clear()
            
            // Determine if we should show full or minimized bubbles
            val showFullBubbles = currentZoomLevel >= ZOOM_THRESHOLD
            
            // Add chat bubbles for Cluj-Napoca locations
            clujChatBubbleData.forEach { bubbleData ->
                try {
                    val bitmap = if (showFullBubbles) {
                        // Create bitmap for chat bubble with text
                        createChatBubbleBitmap(bubbleData.text, bubbleData.author)
                    } else {
                        // Create minimized bubble (just a dot)
                        createMinimizedBubbleBitmap()
                    }
                    
                    val annotation = manager.create(
                        PointAnnotationOptions()
                            .withPoint(bubbleData.location)
                            .withIconImage(bitmap)
                            .withIconSize(1.0)
                            .withIconOffset(if (showFullBubbles) listOf(0.0, -20.0) else listOf(0.0, 0.0))
                    )
                    chatBubbles.add(annotation)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating chat bubble", e)
                }
            }
            
            Log.d(TAG, "Added ${chatBubbles.size} chat bubbles (${if (showFullBubbles) "full" else "minimized"})")
        }
    }
    
    private fun updateChatBubblesForZoom() {
        // Animate transition between minimized and full bubbles
        val targetScale = if (currentZoomLevel >= ZOOM_THRESHOLD) 1f else 0f
        
        zoomTransitionAnimator?.cancel()
        zoomTransitionAnimator = ValueAnimator.ofFloat(currentBubbleScale, targetScale).apply {
            duration = 300  // 300ms smooth transition
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animator ->
                currentBubbleScale = animator.animatedValue as Float
                // Re-create chat bubbles with new scale
                addChatBubbles()
            }
            start()
        }
    }
    
    private fun createMinimizedBubbleBitmap(): android.graphics.Bitmap {
        // Create a small dot for minimized view that scales based on transition
        val baseSize = 30
        val expandedSize = 60
        val size = (baseSize + (expandedSize - baseSize) * currentBubbleScale).toInt()
        
        val bitmap = android.graphics.Bitmap.createBitmap(
            size.coerceAtLeast(10),
            size.coerceAtLeast(10),
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw a small colored circle that fades as it expands
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FF9ED4F5")
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
            alpha = (255 * (1f - currentBubbleScale * 0.7f)).toInt().coerceIn(0, 255)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)
        
        // Add white border that also fades
        paint.apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f
            alpha = (255 * (1f - currentBubbleScale * 0.7f)).toInt().coerceIn(0, 255)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)
        
        return bitmap
    }
    
    private fun createChatBubbleBitmap(text: String, author: String): android.graphics.Bitmap {
        // Create a text paint - smaller size
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 28f  // Reduced from 36f
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        val authorPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#666666")
            textSize = 22f  // Reduced from 28f
            isAntiAlias = true
        }
        
        // Measure text - smaller max width
        val maxWidth = 300  // Reduced from 400
        val padding = 24  // Reduced from 32
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = textPaint.measureText(testLine)
            if (textWidth > maxWidth - padding * 2) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    lines.add(word)
                }
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        // Calculate bitmap size - smaller dimensions
        val lineHeight = 35  // Reduced from 45
        val authorHeight = 28  // Reduced from 35
        val bitmapHeight = padding * 2 + lines.size * lineHeight + authorHeight + 15
        val bitmapWidth = maxWidth
        
        // Apply scale for transition animation
        val scaledWidth = (bitmapWidth * (0.3f + currentBubbleScale * 0.7f)).toInt()
        val scaledHeight = (bitmapHeight * (0.3f + currentBubbleScale * 0.7f)).toInt()
        
        // Create bitmap and canvas
        val bitmap = android.graphics.Bitmap.createBitmap(
            scaledWidth.coerceAtLeast(30),
            scaledHeight.coerceAtLeast(30),
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        
        // Scale canvas for smooth transition
        val scale = 0.3f + currentBubbleScale * 0.7f
        canvas.scale(scale, scale)
        
        // Draw chat bubble background with more rounded corners
        val backgroundPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
            alpha = (255 * currentBubbleScale).toInt().coerceIn(0, 255)
        }
        
        val rect = android.graphics.RectF(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat() - 10f)
        val cornerRadius = 40f  // Increased from 24f for more rounded corners
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)
        
        // Draw border
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f  // Reduced from 3f
            isAntiAlias = true
            alpha = (255 * currentBubbleScale).toInt().coerceIn(0, 255)
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        
        // Draw tail (speech bubble pointer) - only if mostly visible
        if (currentBubbleScale > 0.5f) {
            val tailPath = android.graphics.Path().apply {
                moveTo(bitmapWidth / 2f - 10f, bitmapHeight.toFloat() - 10f)
                lineTo(bitmapWidth / 2f, bitmapHeight.toFloat())
                lineTo(bitmapWidth / 2f + 10f, bitmapHeight.toFloat() - 10f)
                close()
            }
            canvas.drawPath(tailPath, backgroundPaint)
        }
        
        // Draw text - only if scale is significant
        if (currentBubbleScale > 0.3f) {
            textPaint.alpha = (255 * currentBubbleScale).toInt().coerceIn(0, 255)
            authorPaint.alpha = (255 * currentBubbleScale).toInt().coerceIn(0, 255)
            
            var y = padding.toFloat() + 28f
            for (line in lines) {
                canvas.drawText(line, padding.toFloat(), y, textPaint)
                y += lineHeight
            }
            
            // Draw author
            canvas.drawText("- $author", padding.toFloat(), y + 8f, authorPaint)
        }
        
        return bitmap
    }
    
    fun navigateToNextChatBubble() {
        if (chatBubbles.isEmpty()) {
            addChatBubbles()
        }
        
        if (chatBubbles.isNotEmpty()) {
            currentChatBubbleIndex = (currentChatBubbleIndex + 1) % chatBubbles.size
            val nextBubble = chatBubbles[currentChatBubbleIndex]
            
            // Hide any overlay since we're using actual PNG annotations on the map
            hideChatBubbleOverlay()
            
            // Fly to the next chat bubble location
            val cameraOptions = CameraOptions.Builder()
                .center(nextBubble.point)
                .zoom(18.0)
                .build()
            mapView.getMapboxMap().flyTo(cameraOptions)
            
            Log.d(TAG, "Navigating to chat bubble $currentChatBubbleIndex")
        }
    }
    
    fun navigateToPreviousChatBubble() {
        if (chatBubbles.isEmpty()) {
            addChatBubbles()
        }
        
        if (chatBubbles.isNotEmpty()) {
            currentChatBubbleIndex = if (currentChatBubbleIndex > 0) {
                currentChatBubbleIndex - 1
            } else {
                chatBubbles.size - 1
            }
            val prevBubble = chatBubbles[currentChatBubbleIndex]
            
            // Hide any overlay since we're using actual PNG annotations on the map
            hideChatBubbleOverlay()
            
            // Fly to the previous chat bubble location
            val cameraOptions = CameraOptions.Builder()
                .center(prevBubble.point)
                .zoom(18.0)
                .build()
            mapView.getMapboxMap().flyTo(cameraOptions)
            
            Log.d(TAG, "Navigating to chat bubble $currentChatBubbleIndex")
        }
    }
    
    private fun showChatBubbleOverlay() {
        chatBubbleOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .withStartAction {
                chatBubbleOverlay.visibility = View.VISIBLE
            }
            .start()
    }
    
    private fun hideChatBubbleOverlay() {
        chatBubbleOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                chatBubbleOverlay.visibility = View.GONE
            }
            .start()
    }
}
