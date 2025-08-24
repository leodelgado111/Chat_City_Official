package com.chatcityofficial.chatmapapp.ui.home

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
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
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatcityofficial.chatmapapp.R
import com.chatcityofficial.chatmapapp.ui.compose.components.LocationButtonView
import com.chatcityofficial.chatmapapp.ui.compose.components.ThemedLocationContainerView
import com.chatcityofficial.chatmapapp.ui.profile.ProfileFragment
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
    
    // Cache for chat bubble bitmaps to avoid recreation
    private val chatBubbleBitmapCache = mutableMapOf<String, android.graphics.Bitmap>()
    
    // Track currently shown full bubble (for dismissing)
    private var currentlyShownFullBubbleIndex: Int? = null
    
    // Cluj-Napoca chat bubble locations with text
    data class ChatBubbleData(val location: Point, val text: String, val author: String, val timestamp: String, val category: String)
    
    private val clujChatBubbleData = listOf(
        ChatBubbleData(
            Point.fromLngLat(23.5897, 46.7712), 
            "Central Park is amazing in the morning! Perfect for jogging and morning walks.",
            "Alex M.",
            "8:30 AM",
            "Fitness"
        ),
        ChatBubbleData(
            Point.fromLngLat(23.5924, 46.7684), 
            "Best coffee shop near Union Square! Their espresso is absolutely incredible.",
            "Maria P.",
            "10:15 AM",
            "Food & Drink"
        ),
        ChatBubbleData(
            Point.fromLngLat(23.5862, 46.7731),
            "The Botanical Garden has beautiful roses this season. Worth visiting this weekend!",
            "Stefan R.",
            "Yesterday",
            "Nature"
        ),
        ChatBubbleData(
            Point.fromLngLat(23.5955, 46.7698),  // Near Cluj Arena area
            "Hey everyone! It's super sunny outside - make sure to wear sunscreen today.",
            "Community",
            "2 hours ago",
            "Weather"
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
        
        // Reset initialization flag so we center on user location when app opens
        hasEverInitialized = false
        
        // Load persisted camera state from SharedPreferences
        loadPersistedCameraState()
        
        // Initialize views
        mapView = view.findViewById(R.id.mapView)
        logoImageView = view.findViewById(R.id.logoImageView)
        
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
        
        // Setup arrow button click listeners
        setupArrowButtons(view)
        
        // Setup post button
        setupPostButton(view)
        
        return view
    }
    
    private fun loadPersistedCameraState() {
        // Don't load saved camera state anymore - always start at user's location
        // This function is kept for potential future use but does nothing now
        Log.d(TAG, "Skipping persisted camera state - will center on user location")
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
    
    private fun setupArrowButtons(view: View) {
        // Setup right arrow button click with animation
        view.findViewById<View>(R.id.floating_5whitebase)?.apply {
            setOnClickListener {
                Log.d(TAG, "Right arrow button clicked")
                // Apply white flash animation
                animateButtonTap(this)
                // Navigate to next chat bubble
                navigateToNextChatBubble()
            }
        }
        
        // Setup left arrow button click with animation
        view.findViewById<View>(R.id.floating_5whitebase_2)?.apply {
            setOnClickListener {
                Log.d(TAG, "Left arrow button clicked")
                // Apply white flash animation
                animateButtonTap(this)
                // Navigate to previous chat bubble
                navigateToPreviousChatBubble()
            }
        }
    }
    
    private fun animateButtonTap(button: View) {
        // Create a View with rounded corners using white color
        val overlay = View(requireContext()).apply {
            // Create a rounded drawable programmatically with white color
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(android.graphics.Color.WHITE)
                cornerRadius = 14f * resources.displayMetrics.density  // 14dp in pixels
            }
            background = drawable
            alpha = 0f
        }
        
        // Find the button's parent (ConstraintLayout)
        val parent = button.parent as? ViewGroup ?: return
        
        // Add the overlay to the parent
        parent.addView(overlay)
        
        // Set the overlay's layout params using ConstraintLayout params
        val params = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            button.width,
            button.height
        )
        overlay.layoutParams = params
        
        // Position the overlay exactly over the button
        overlay.x = button.x
        overlay.y = button.y
        
        // Make sure the overlay is on top
        overlay.elevation = button.elevation + 1f
        
        // Animate the overlay with good visibility
        overlay.animate()
            .alpha(0.6f)  // Good visibility for white
            .setDuration(100)
            .withEndAction {
                overlay.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        parent.removeView(overlay)
                    }
                    .start()
            }
            .start()
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
                
                // Check zoom level and update chat bubbles only when crossing threshold
                val newZoomLevel = cameraState.zoom
                val wasAboveThreshold = currentZoomLevel >= ZOOM_THRESHOLD
                val isAboveThreshold = newZoomLevel >= ZOOM_THRESHOLD
                
                // Don't hide chat bubbles when zooming - user requested they stay visible
                // Chat bubbles should only be closed by tapping them again
                
                if (wasAboveThreshold != isAboveThreshold) {
                    currentZoomLevel = newZoomLevel
                    updateChatBubblesForZoom()
                } else {
                    currentZoomLevel = newZoomLevel
                }
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
                
                // Don't add scroll listener - user wants chat bubbles to stay visible when scrolling
                // Chat bubbles should only be closed by tapping them again
                
                // CRITICAL FIX: Add map click listener using the gestures plugin
                mapView.gestures.addOnMapClickListener { point ->
                    if (isSearchVisible) {
                        Log.d(TAG, "Map clicked while search is visible - closing search")
                        hideSearchView()
                        true // Consume the click event
                    } else {
                        // Don't hide chat bubbles when clicking elsewhere - user wants them to stay visible
                        // Chat bubbles should only be closed by tapping them again
                        false // Let other click handlers process it
                    }
                }
                
                // Initialize annotation managers
                initializeAnnotationManager()
                
                // Check permissions and get location first - always center on user location when app opens
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
            if (location != null) {
                currentLocation = location
                updateLocationUI(location)
                
                // Always center on user's current location when app opens
                if (!hasEverInitialized) {
                    Log.d(TAG, "App opening - centering on current location")
                    moveToLocation(location.latitude, location.longitude, 15.0)
                    hasEverInitialized = true
                } else {
                    Log.d(TAG, "Location updated but not centering (already initialized)")
                }
                
                updateLocationPulse(location)
                updateMapThemeBasedOnTime(location)
                startThemeUpdates()
            } else {
                // If no last location, request a fresh location update
                Log.d(TAG, "No last location available, requesting fresh location")
                requestFreshLocation()
            }
        }
    }
    
    private fun requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        val locationRequest = LocationRequest.create().apply {
            numUpdates = 1
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    updateLocationUI(location)
                    
                    // Center on location if this is first initialization
                    if (!hasEverInitialized) {
                        Log.d(TAG, "App opening with fresh location - centering")
                        moveToLocation(location.latitude, location.longitude, 15.0)
                        hasEverInitialized = true
                    }
                    
                    updateLocationPulse(location)
                    updateMapThemeBasedOnTime(location)
                    startThemeUpdates()
                    
                    // Remove this callback after getting location
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
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
        // Clear the minimized bubble cache to ensure it uses the new touch size
        chatBubbleBitmapCache.remove("min")
        
        // Pre-cache all chat bubble bitmaps for instant display
        clujChatBubbleData.forEachIndexed { index, bubbleData ->
            chatBubbleBitmapCache.getOrPut("full_$index") {
                createGradientChatBubbleBitmap(bubbleData.text, bubbleData.author, bubbleData.timestamp, bubbleData.category)
            }
        }
        
        // Create point annotation manager if needed
        if (pointAnnotationManager == null) {
            val annotationApi = mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager().apply {
                // Add click listener for chat bubble taps
                addClickListener { annotation ->
                    handleChatBubbleClick(annotation)
                    true // Consume the click
                }
            }
        }
        
        pointAnnotationManager?.let { manager ->
            // Clear existing chat bubbles
            chatBubbles.forEach { manager.delete(it) }
            chatBubbles.clear()
            
            // Determine if we should show full or minimized bubbles
            val showFullBubbles = currentZoomLevel >= ZOOM_THRESHOLD
            
            // Add chat bubbles for Cluj-Napoca locations
            clujChatBubbleData.forEachIndexed { index, bubbleData ->
                try {
                    val cacheKey = if (showFullBubbles) {
                        "full_$index"
                    } else {
                        "min"  // All minimized bubbles use the same bitmap
                    }
                    
                    val bitmap = chatBubbleBitmapCache.getOrPut(cacheKey) {
                        if (showFullBubbles) {
                            // Create bitmap for chat bubble with gradient overlay
                            createGradientChatBubbleBitmap(bubbleData.text, bubbleData.author, bubbleData.timestamp, bubbleData.category)
                        } else {
                            // Create minimized bubble (just a dot)
                            createMinimizedBubbleBitmap()
                        }
                    }
                    
                    val annotation = manager.create(
                        PointAnnotationOptions()
                            .withPoint(bubbleData.location)
                            .withIconImage(bitmap)
                            .withIconSize(1.0)
                            .withIconOpacity(1.0)  // Start fully visible, animation will handle fade
                            .withIconOffset(if (showFullBubbles) listOf(0.0, -20.0) else listOf(0.0, 0.0))
                            .withData(com.google.gson.JsonObject().apply {
                                addProperty("text", bubbleData.text)
                                addProperty("author", bubbleData.author)
                                addProperty("index", index)
                                addProperty("isFullBubble", showFullBubbles)
                            })
                    )
                    chatBubbles.add(annotation)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating chat bubble", e)
                }
            }
            
            Log.d(TAG, "Added ${chatBubbles.size} chat bubbles (${if (showFullBubbles) "full" else "minimized"})")
        }
    }
    
    private fun handleChatBubbleClick(annotation: PointAnnotation) {
        // Get the data from the annotation
        val data = annotation.getData() ?: return
        if (!data.isJsonObject) return
        
        val jsonData = data.asJsonObject
        val isFullBubble = jsonData.get("isFullBubble")?.asBoolean ?: false
        val index = jsonData.get("index")?.asInt ?: 0
        
        if (currentZoomLevel >= ZOOM_THRESHOLD) {
            // Already showing all full bubbles, no individual tap handling
            return
        }
        
        // Post immediately to avoid any processing delay
        view?.post {
            if (isFullBubble) {
                // This is a full bubble, hide it and show the blue circle
                hideSingleChatBubble(index)
            } else {
                // This is a blue circle, show the full bubble immediately
                showSingleChatBubble(index)
            }
        }
    }
    
    private fun showSingleChatBubble(index: Int) {
        val pointAnnotationManager = pointAnnotationManager ?: return
        val bubbleData = clujChatBubbleData.getOrNull(index) ?: return
        
        // If there's already a bubble showing and it's different from the one being opened
        if (currentlyShownFullBubbleIndex != null && currentlyShownFullBubbleIndex != index) {
            // Close the existing bubble while opening the new one
            closeExistingBubbleAndOpenNew(currentlyShownFullBubbleIndex!!, index)
            return
        }
        
        // Find the existing minimized bubble for this index FIRST
        val existingBubble = chatBubbles.getOrNull(index) ?: return
        
        // Immediately start fading out the blue circle for instant visual feedback
        existingBubble.iconOpacity = 0.8
        pointAnnotationManager.update(existingBubble)
        
        // Track the currently shown bubble
        currentlyShownFullBubbleIndex = index
        
        // Get the cached bitmap first to avoid any delay
        val fullBubbleBitmap = chatBubbleBitmapCache.getOrPut("full_$index") {
            createGradientChatBubbleBitmap(bubbleData.text, bubbleData.author, bubbleData.timestamp, bubbleData.category)
        }
        
        // Create the full bubble immediately at the same location
        val newAnnotation = pointAnnotationManager.create(
            PointAnnotationOptions()
                .withPoint(bubbleData.location)
                .withIconImage(fullBubbleBitmap)
                .withIconSize(1.0)
                .withIconOpacity(0.0)  // Start invisible
                .withIconOffset(listOf(0.0, -45.0))  // Position so tail tip is at the top of the blue circle
                .withData(com.google.gson.JsonObject().apply {
                    addProperty("text", bubbleData.text)
                    addProperty("author", bubbleData.author)
                    addProperty("index", index)
                    addProperty("isFullBubble", true)
                })
        )
        
        // Store the new annotation ID in the existing bubble's data
        val existingData = existingBubble.getData()?.asJsonObject ?: com.google.gson.JsonObject()
        existingData.addProperty("tempFullBubble", newAnnotation.id)
        existingBubble.setData(existingData)
        
        // Make the new bubble slightly visible immediately for instant feedback
        newAnnotation.iconOpacity = 0.1
        pointAnnotationManager.update(newAnnotation)
        
        // Start animation immediately without delay
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 25  // Very fast animation
            startDelay = 0
            interpolator = LinearInterpolator()
            
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                
                // Fade out blue circle (0.8 -> 0)
                existingBubble.iconOpacity = (0.8 * (1f - progress)).toDouble()
                pointAnnotationManager.update(existingBubble)
                
                // Fade in chat bubble (0.1 -> 1)
                newAnnotation.iconOpacity = (0.1 + 0.9 * progress).toDouble()
                pointAnnotationManager.update(newAnnotation)
            }
            start()
        }
    }
    
    private fun closeExistingBubbleAndOpenNew(oldIndex: Int, newIndex: Int) {
        val pointAnnotationManager = pointAnnotationManager ?: return
        
        // Get the new bubble data FIRST for faster response
        val newBubbleData = clujChatBubbleData.getOrNull(newIndex) ?: return
        val newBlueBubble = chatBubbles.getOrNull(newIndex) ?: return
        
        // Immediately provide visual feedback on the new blue circle
        newBlueBubble.iconOpacity = 0.8
        pointAnnotationManager.update(newBlueBubble)
        
        // Get the old bubble data
        val oldBlueBubble = chatBubbles.getOrNull(oldIndex) ?: return
        val oldFullBubbleId = oldBlueBubble.getData()?.asJsonObject?.get("tempFullBubble")?.asLong
        val oldFullBubble = if (oldFullBubbleId != null) {
            pointAnnotationManager.annotations.find { it.id == oldFullBubbleId } as? PointAnnotation
        } else {
            null
        }
        
        // Create the new full bubble bitmap
        val newFullBubbleBitmap = chatBubbleBitmapCache.getOrPut("full_$newIndex") {
            createGradientChatBubbleBitmap(newBubbleData.text, newBubbleData.author, newBubbleData.timestamp, newBubbleData.category)
        }
        
        // Create the new full bubble annotation
        val newFullBubble = pointAnnotationManager.create(
            PointAnnotationOptions()
                .withPoint(newBubbleData.location)
                .withIconImage(newFullBubbleBitmap)
                .withIconSize(1.0)
                .withIconOpacity(0.0)  // Start invisible
                .withIconOffset(listOf(0.0, -45.0))
                .withData(com.google.gson.JsonObject().apply {
                    addProperty("text", newBubbleData.text)
                    addProperty("author", newBubbleData.author)
                    addProperty("index", newIndex)
                    addProperty("isFullBubble", true)
                })
        )
        
        // Store the new annotation ID in the new blue bubble's data
        val newBubbleData2 = newBlueBubble.getData()?.asJsonObject ?: com.google.gson.JsonObject()
        newBubbleData2.addProperty("tempFullBubble", newFullBubble.id)
        newBlueBubble.setData(newBubbleData2)
        
        // Update tracking
        currentlyShownFullBubbleIndex = newIndex
        
        // Set initial states with immediate visual feedback
        if (oldFullBubble != null) {
            oldFullBubble.iconOpacity = 0.9  // Start slightly faded
            pointAnnotationManager.update(oldFullBubble)
        }
        oldBlueBubble.iconOpacity = 0.1  // Slightly visible
        pointAnnotationManager.update(oldBlueBubble)
        
        newFullBubble.iconOpacity = 0.1  // Start slightly visible for instant feedback
        pointAnnotationManager.update(newFullBubble)
        
        // Animate with immediate start
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 25  // Very fast transition
            startDelay = 0
            interpolator = LinearInterpolator()
            
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                
                // Fade out old full bubble (if exists) from 0.9 to 0
                if (oldFullBubble != null) {
                    oldFullBubble.iconOpacity = (0.9 * (1f - progress)).toDouble()
                    pointAnnotationManager.update(oldFullBubble)
                }
                
                // Fade in old blue circle from 0.1 to 1
                oldBlueBubble.iconOpacity = (0.1 + 0.9 * progress).toDouble()
                pointAnnotationManager.update(oldBlueBubble)
                
                // Fade out new blue circle from 0.8 to 0
                newBlueBubble.iconOpacity = (0.8 * (1f - progress)).toDouble()
                pointAnnotationManager.update(newBlueBubble)
                
                // Fade in new full bubble from 0.1 to 1
                newFullBubble.iconOpacity = (0.1 + 0.9 * progress).toDouble()
                pointAnnotationManager.update(newFullBubble)
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Clean up: delete the old full bubble only after animation completes
                    if (oldFullBubble != null) {
                        oldFullBubble.iconOpacity = 0.0
                        pointAnnotationManager.update(oldFullBubble)
                        pointAnnotationManager.delete(oldFullBubble)
                    }
                    
                    // Clear the tempFullBubble reference from the old blue bubble
                    val oldData = oldBlueBubble.getData()?.asJsonObject
                    oldData?.remove("tempFullBubble")
                    oldBlueBubble.setData(oldData)
                    
                    // Ensure final states
                    oldBlueBubble.iconOpacity = 1.0
                    pointAnnotationManager.update(oldBlueBubble)
                    newBlueBubble.iconOpacity = 0.0
                    pointAnnotationManager.update(newBlueBubble)
                    newFullBubble.iconOpacity = 1.0
                    pointAnnotationManager.update(newFullBubble)
                }
            })
            
            start()
        }
    }
    
    private fun hideSingleChatBubble(index: Int) {
        val pointAnnotationManager = pointAnnotationManager ?: return
        val bubbleData = clujChatBubbleData.getOrNull(index) ?: return
        
        // Clear the tracking
        if (currentlyShownFullBubbleIndex == index) {
            currentlyShownFullBubbleIndex = null
        }
        
        // Find the blue circle annotation
        val blueBubble = chatBubbles.getOrNull(index) ?: return
        
        // Find the full bubble that's currently showing
        val fullBubbleId = blueBubble.getData()?.asJsonObject?.get("tempFullBubble")?.asLong
        val fullBubble = pointAnnotationManager.annotations.find { it.id == fullBubbleId } as? PointAnnotation
        
        if (fullBubble != null) {
            // Apply initial values immediately
            fullBubble.iconOpacity = 1.0
            pointAnnotationManager.update(fullBubble)
            blueBubble.iconOpacity = 0.0
            pointAnnotationManager.update(blueBubble)
            
            // Simultaneous cross-fade - faster response
            ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 25  // Very fast for instant response
                startDelay = 0
                interpolator = LinearInterpolator()
                
                addUpdateListener { animator ->
                    val opacity = animator.animatedValue as Float
                    // Fade out full bubble
                    fullBubble.iconOpacity = opacity.toDouble()
                    pointAnnotationManager.update(fullBubble)
                    
                    // Simultaneously fade in blue circle
                    blueBubble.iconOpacity = (1f - opacity).toDouble()
                    pointAnnotationManager.update(blueBubble)
                }
                doOnEnd {
                    // Delete the full bubble
                    pointAnnotationManager.delete(fullBubble)
                    // Ensure blue circle is fully visible
                    blueBubble.iconOpacity = 1.0
                    pointAnnotationManager.update(blueBubble)
                }
                // Start immediately
                start()
            }
        }
    }
    
    private fun updateChatBubblesForZoom() {
        // Animate transition between minimized and full bubbles
        val targetScale = if (currentZoomLevel >= ZOOM_THRESHOLD) 1f else 0f
        
        // If already at target, skip animation
        if (kotlin.math.abs(currentBubbleScale - targetScale) < 0.01f) {
            return
        }
        
        zoomTransitionAnimator?.cancel()
        
        val pointAnnotationManager = pointAnnotationManager ?: return
        
        // Store current bubbles for fade out
        val oldBubbles = chatBubbles.toList()
        
        // Clear current bubbles list (but keep them on map for animation)
        chatBubbles.clear()
        
        // Create new bubbles at target state (but invisible)
        currentBubbleScale = targetScale
        addChatBubbles()
        
        // Set new bubbles to transparent initially
        chatBubbles.forEach { annotation ->
            annotation.iconOpacity = 0.0
        }
        pointAnnotationManager.update(chatBubbles)
        
        // Cross-fade animation: old bubbles fade out, new bubbles fade in
        zoomTransitionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200  // 200ms cross-fade
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                
                // Fade out old bubbles
                oldBubbles.forEach { annotation ->
                    annotation.iconOpacity = (1.0 - progress).coerceIn(0.0, 1.0)
                }
                pointAnnotationManager.update(oldBubbles)
                
                // Fade in new bubbles
                chatBubbles.forEach { annotation ->
                    annotation.iconOpacity = progress.toDouble().coerceIn(0.0, 1.0)
                }
                pointAnnotationManager.update(chatBubbles)
            }
            doOnEnd {
                // Remove old bubbles after animation
                pointAnnotationManager.delete(oldBubbles)
            }
            start()
        }
    }
    
    private fun createMinimizedBubbleBitmap(): android.graphics.Bitmap {
        // Create a larger touch target (50% total increase) with the visual circle in the center
        val touchSize = 82  // 68 * 1.20 ≈ 82 pixels (50% larger than original 52)
        val visualSize = 40
        
        val bitmap = android.graphics.Bitmap.createBitmap(
            touchSize,
            touchSize,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        
        // Calculate offset to center the visual circle within the larger touch area
        val offset = (touchSize - visualSize) / 2f
        val centerX = touchSize / 2f
        val centerY = touchSize / 2f
        val radius = visualSize / 2f - 2f
        
        // Draw a small colored circle in the center
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FF9ED4F5")
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
            alpha = 200
        }
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Add white border
        paint.apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f
            alpha = 255
        }
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        return bitmap
    }
    
    private fun createGradientChatBubbleBitmap(text: String, author: String, timestamp: String = "", category: String = ""): android.graphics.Bitmap {
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
        
        val timestampPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#999999")
            textSize = 18f  // Smaller than author text
            isAntiAlias = true
        }
        
        val categoryPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#4A90E2")  // Blue color for category
            textSize = 20f  // Slightly larger than timestamp
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
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
        val metadataHeight = 28  // Height for the category/author/timestamp line
        val metadataOffset = 3  // 3dp offset as requested
        val bitmapHeight = padding * 2 + lines.size * lineHeight + metadataHeight + metadataOffset + 15
        val bitmapWidth = maxWidth
        
        // Create bitmap and canvas
        val bitmap = android.graphics.Bitmap.createBitmap(
            bitmapWidth,
            bitmapHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        
        // LAYER 1: Draw white base background with rounded corners
        val whiteBasePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
            alpha = 255
        }
        
        val rect = android.graphics.RectF(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat() - 10f)
        val cornerRadius = 40f  // Increased from 24f for more rounded corners
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, whiteBasePaint)
        
        // LAYER 2: Draw gradient overlay matching arrow buttons - sweep gradient
        val gradientPaint = android.graphics.Paint().apply {
            // Create sweep gradient matching ic_5strongergradient
            shader = android.graphics.SweepGradient(
                -10f,  // Center X - matching arrow button offset
                -10f,  // Center Y - matching arrow button offset
                intArrayOf(
                    android.graphics.Color.parseColor("#B3C4B3F7"),  // #B3C4B3F7 at 0.0
                    android.graphics.Color.parseColor("#CC9ED4F5"),  // #CC9ED4F5 at 0.35
                    android.graphics.Color.parseColor("#B3D4C2F0"),  // #B3D4C2F0 at 0.55
                    android.graphics.Color.parseColor("#33FFB3D9"),  // #33FFB3D9 at 0.75
                    android.graphics.Color.parseColor("#B3C4B3F7")   // Back to start at 1.0
                ),
                floatArrayOf(0f, 0.35f, 0.55f, 0.75f, 1f)
            )
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
            alpha = 230  // 90% opacity matching arrow button group alpha
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, gradientPaint)
        
        // Draw border
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f  // Reduced from 3f
            isAntiAlias = true
            alpha = 255
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        
        // Draw chat bubble tail/pointer
        val path = android.graphics.Path().apply {
            moveTo(bitmapWidth / 2f - 20f, bitmapHeight - 10f)
            lineTo(bitmapWidth / 2f, bitmapHeight.toFloat())
            lineTo(bitmapWidth / 2f + 20f, bitmapHeight - 10f)
            close()
        }
        canvas.drawPath(path, whiteBasePaint)
        
        // Draw gradient on tail too
        canvas.drawPath(path, gradientPaint)
        
        // LAYER 3: Draw text on top
        textPaint.alpha = 255
        authorPaint.alpha = 255
        
        // Draw text lines
        var y = padding.toFloat() + 28f
        for (line in lines) {
            canvas.drawText(line, padding.toFloat(), y, textPaint)
            y += lineHeight
        }
        
        // Move down by 3dp for metadata (author, timestamp)
        y += 3f
        
        // Draw author
        canvas.drawText(author, padding.toFloat(), y + 10f, authorPaint)
        
        // Draw timestamp if present (right-aligned)
        if (timestamp.isNotEmpty()) {
            val timestampWidth = timestampPaint.measureText(timestamp)
            canvas.drawText(timestamp, bitmapWidth - padding - timestampWidth, y + 10f, timestampPaint)
        }
        
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
        
        // Create bitmap and canvas
        val bitmap = android.graphics.Bitmap.createBitmap(
            bitmapWidth,
            bitmapHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw chat bubble background with more rounded corners
        val backgroundPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
            alpha = 255
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
            alpha = 255
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        
        // Draw tail (speech bubble pointer)
        if (true) {
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
            
            // Fly to the next chat bubble location
            val cameraOptions = CameraOptions.Builder()
                .center(nextBubble.point)
                .zoom(18.0)
                .build()
            mapView.getMapboxMap().flyTo(cameraOptions)
            
            // Add this chat bubble view to the profile activity feed
            val bubbleData = clujChatBubbleData[currentChatBubbleIndex]
            // Randomly assign a category for demonstration (in real app, this would be based on actual message type)
            val categories = listOf("Post", "Task", "Group", "Meet-Up")
            val category = categories[currentChatBubbleIndex % categories.size]
            ProfileFragment.addChatBubbleView(
                message = bubbleData.text,
                category = category,
                subcategory = bubbleData.author
            )
            
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
            
            // Fly to the previous chat bubble location
            val cameraOptions = CameraOptions.Builder()
                .center(prevBubble.point)
                .zoom(18.0)
                .build()
            mapView.getMapboxMap().flyTo(cameraOptions)
            
            // Add this chat bubble view to the profile activity feed
            val bubbleData = clujChatBubbleData[currentChatBubbleIndex]
            // Randomly assign a category for demonstration (in real app, this would be based on actual message type)
            val categories = listOf("Post", "Task", "Group", "Meet-Up")
            val category = categories[currentChatBubbleIndex % categories.size]
            ProfileFragment.addChatBubbleView(
                message = bubbleData.text,
                category = category,
                subcategory = bubbleData.author
            )
            
            Log.d(TAG, "Navigating to chat bubble $currentChatBubbleIndex")
        }
    }
    
    private var postButtonComposeView: ComposeView? = null
    private var postButtonComposeView2: ComposeView? = null
    private var isPostButtonVisible = false
    
    private fun setupPostButton(view: View) {
        postButtonComposeView = view.findViewById<ComposeView>(R.id.postButtonComposeView)
        postButtonComposeView?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PostButton(showIcon = true, iconType = "post")
            }
            // Initially hidden
            visibility = View.GONE
        }
        
        postButtonComposeView2 = view.findViewById<ComposeView>(R.id.postButtonComposeView2)
        postButtonComposeView2?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PostButton(showIcon = true, iconType = "group")
            }
            // Initially hidden
            visibility = View.GONE
        }
    }
    
    fun togglePostButton() {
        postButtonComposeView?.let { button ->
            isPostButtonVisible = !isPostButtonVisible
            if (isPostButtonVisible) {
                button.visibility = View.VISIBLE
                // Fade in animation (50% faster total - 112ms instead of 160ms)
                button.alpha = 0f
                button.animate()
                    .alpha(1f)
                    .setDuration(112)
                    .start()
                    
                // Also show the second button
                postButtonComposeView2?.let { button2 ->
                    button2.visibility = View.VISIBLE
                    button2.alpha = 0f
                    button2.animate()
                        .alpha(1f)
                        .setDuration(112)
                        .start()
                }
            } else {
                // Fade out animation (50% faster total - 112ms instead of 160ms)
                button.animate()
                    .alpha(0f)
                    .setDuration(112)
                    .withEndAction {
                        button.visibility = View.GONE
                    }
                    .start()
                    
                // Also hide the second button
                postButtonComposeView2?.let { button2 ->
                    button2.animate()
                        .alpha(0f)
                        .setDuration(112)
                        .withEndAction {
                            button2.visibility = View.GONE
                        }
                        .start()
                }
            }
        }
    }
    
    fun hidePostButtons() {
        if (isPostButtonVisible) {
            isPostButtonVisible = false
            postButtonComposeView?.visibility = View.GONE
            postButtonComposeView2?.visibility = View.GONE
        }
    }
    
    @Composable
    private fun PostButton(showIcon: Boolean = true, iconType: String = "post") {
        Box(
            modifier = Modifier
                .size(49.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // White base layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
            
            // Gradient overlay - same as left arrow button (ic_5strongergradient)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFC4B3F7).copy(alpha = 0.7f), // #B3C4B3F7 -> 70% of B3 (179/255)
                                Color(0xFF9ED4F5).copy(alpha = 0.8f), // #CC9ED4F5 -> 80% of CC (204/255)
                                Color(0xFFD4C2F0).copy(alpha = 0.7f), // #B3D4C2F0 -> 70% of B3
                                Color(0xFFFFB3D9).copy(alpha = 0.2f), // #33FFB3D9 -> 20% of 33 (51/255)
                                Color(0xFFC4B3F7).copy(alpha = 0.7f)  // Back to start
                            ),
                            center = Offset(-0.217f, -0.217f) // Offset to match centerX="-10" centerY="-10" on 46dp view
                        ),
                        alpha = 0.9f // Group alpha from the drawable
                    )
            )
            
            // White stroke (1dp border) - moved to be on top of gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
            )
            
            // Icon overlay - only show if showIcon is true
            if (showIcon) {
                val iconResource = if (iconType == "group") {
                    R.drawable.ic_group_icon_overlay
                } else {
                    R.drawable.ic_post_button_icon
                }
                Icon(
                    imageVector = ImageVector.vectorResource(id = iconResource),
                    contentDescription = if (iconType == "group") "Group" else "Post",
                    modifier = Modifier
                        .size(20.dp) // Slightly smaller than the original to fit well
                        .align(Alignment.Center), // Explicitly center the icon
                    tint = Color.Black
                )
            }
        }
    }
    
}
