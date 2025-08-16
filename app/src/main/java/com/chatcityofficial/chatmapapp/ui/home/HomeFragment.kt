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
    private lateinit var locationText: TextView
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
    private lateinit var locationIcon: ImageView
    private lateinit var locationContainer: LinearLayout
    
    // Places API
    private lateinit var placesClient: PlacesClient
    private lateinit var searchAdapter: PlaceSearchAdapter
    private var sessionToken: AutocompleteSessionToken? = null
    private var isSearchVisible = false
    
    // Annotation managers
    private var circleAnnotationManager: CircleAnnotationManager? = null
    private var polygonAnnotationManager: PolygonAnnotationManager? = null
    private var pulseAnnotation: CircleAnnotation? = null
    private var centerDotAnnotation: CircleAnnotation? = null
    private var placeOutlineAnnotation: PolygonAnnotation? = null
    private var pulseAnimator: ValueAnimator? = null
    
    // Handler for periodic theme checks
    private val themeUpdateHandler = Handler(Looper.getMainLooper())
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
        
        // Load persisted camera state from SharedPreferences
        loadPersistedCameraState()
        
        // Initialize views
        mapView = view.findViewById(R.id.mapView)
        locationText = view.findViewById(R.id.locationText)
        logoImageView = view.findViewById(R.id.logoImageView)
        
        // Initialize search views
        defaultViewContainer = view.findViewById(R.id.defaultViewContainer)
        searchContainer = view.findViewById(R.id.searchContainer)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchBackButton = view.findViewById(R.id.searchBackButton)
        searchClearButton = view.findViewById(R.id.searchClearButton)
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView)
        locationIcon = view.findViewById(R.id.locationIcon)
        locationContainer = view.findViewById(R.id.locationContainer)
        
        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
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
        
        // Pre-determine theme before initializing map
        determinateInitialTheme()
        
        // Initialize map
        initializeMap()
        
        // Setup click listeners - moved after map initialization
        // Will be called after map is ready
        
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
            
            // Create a mock CameraState with the loaded values
            // We'll apply these values when the map is ready
            savedCameraState = null // Will be set properly when map loads
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
        // Make the entire location container clickable, not just the icon
        locationContainer.setOnClickListener {
            if (!isSearchVisible) {
                showSearchView()
            }
        }
        
        // Also keep the icon clickable for better touch target
        locationIcon.setOnClickListener {
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
        
        // Setup map click listener to close search
        setupMapClickListener()
    }
    
    private fun setupMapClickListener() {
        // Add click listener to map to close search when tapping outside
        // This is called after map is ready and will properly register the listener
        try {
            mapView.gestures.addOnMapClickListener { point ->
                Log.d(TAG, "Map clicked, isSearchVisible: $isSearchVisible")
                if (isSearchVisible) {
                    hideSearchView()
                    true // Consume the click event
                } else {
                    false // Let other click handlers process it
                }
            }
            Log.d(TAG, "Map click listener successfully registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up map click listener", e)
            // Try again after a short delay
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mapView.gestures.addOnMapClickListener { point ->
                        Log.d(TAG, "Map clicked (delayed), isSearchVisible: $isSearchVisible")
                        if (isSearchVisible) {
                            hideSearchView()
                            true
                        } else {
                            false
                        }
                    }
                    Log.d(TAG, "Map click listener successfully registered (delayed)")
                } catch (e2: Exception) {
                    Log.e(TAG, "Error setting up map click listener (delayed)", e2)
                }
            }, 500)
        }
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
        
        Log.d(TAG, "Showing search view")
        
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
        
        Log.d(TAG, "Hiding search view")
        
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
                    addPlaceOutline(latLng, place.viewport)
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
    
    private fun addPlaceOutline(center: LatLng, viewport: LatLngBounds?) {
        // Remove existing outline
        placeOutlineAnnotation?.let {
            polygonAnnotationManager?.delete(it)
            placeOutlineAnnotation = null
        }
        
        // Create polygon annotation manager if needed
        if (polygonAnnotationManager == null) {
            val annotationApi = mapView.annotations
            polygonAnnotationManager = annotationApi.createPolygonAnnotationManager()
        }
        
        // Create rectangle from viewport or default size
        val bounds = viewport ?: LatLngBounds.builder()
            .include(LatLng(center.latitude - 0.001, center.longitude - 0.001))
            .include(LatLng(center.latitude + 0.001, center.longitude + 0.001))
            .build()
        
        // Create polygon points for the outline
        val points = listOf(
            Point.fromLngLat(bounds.southwest.longitude, bounds.southwest.latitude),
            Point.fromLngLat(bounds.northeast.longitude, bounds.southwest.latitude),
            Point.fromLngLat(bounds.northeast.longitude, bounds.northeast.latitude),
            Point.fromLngLat(bounds.southwest.longitude, bounds.northeast.latitude),
            Point.fromLngLat(bounds.southwest.longitude, bounds.southwest.latitude)
        )
        
        // Create polygon with gradient-like outline
        polygonAnnotationManager?.let { manager ->
            placeOutlineAnnotation = manager.create(
                PolygonAnnotationOptions()
                    .withPoints(listOf(points))
                    .withFillOpacity(0.0) // No fill, only outline
                    .withFillOutlineColor("#4DFB86BB") // Pink with 30% opacity for outline
            )
        }
    }
    
    private fun determinateInitialTheme() {
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
                    
                    updateLogoColor(isDarkMode)
                }
            }
        }
    }
    
    private fun initializeMap() {
        mapView.getMapboxMap().apply {
            loadStyleUri(currentMapStyle) { style ->
                Log.d(TAG, "Map style loaded: $currentMapStyle")
                
                // Disable all Mapbox branding and UI elements
                mapView.location.enabled = false
                mapView.scalebar.enabled = false
                mapView.logo.enabled = false
                mapView.attribution.enabled = false
                
                // Disable rotation gesture while keeping pinch zoom and pan
                mapView.gestures.rotateEnabled = false
                mapView.gestures.pinchToZoomEnabled = true
                mapView.gestures.scrollEnabled = true
                
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
                
                // Setup search listeners AFTER map is ready
                setupSearchListeners()
                
                // Check permissions and get location
                checkLocationPermission()
            }
        }
    }
    
    private fun initializeAnnotationManager() {
        val annotationApi = mapView.annotations
        circleAnnotationManager = annotationApi.createCircleAnnotationManager()
        polygonAnnotationManager = annotationApi.createPolygonAnnotationManager()
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
            interval = 10000
            fastestInterval = 5000
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
        val sunLocation = com.luckycatlabs.sunrisesunset.dto.Location(location.latitude, location.longitude)
        val calculator = SunriseSunsetCalculator(sunLocation, TimeZone.getDefault())
        val now = Calendar.getInstance()
        
        val sunrise = calculator.getOfficialSunriseCalendarForDate(now)
        val sunset = calculator.getOfficialSunsetCalendarForDate(now)
        
        val shouldUseDarkTheme = now.after(sunset) || now.before(sunrise)
        
        if (shouldUseDarkTheme != isDarkMode) {
            isDarkMode = shouldUseDarkTheme
            val newStyle = if (isDarkMode) Style.DARK else Style.LIGHT
            
            saveCameraState()
            
            mapView.getMapboxMap().loadStyleUri(newStyle) { style ->
                Log.d(TAG, "Theme switched to: ${if (isDarkMode) "DARK" else "LIGHT"}")
                currentMapStyle = newStyle
                
                mapView.logo.enabled = false
                mapView.attribution.enabled = false
                mapView.scalebar.enabled = false
                
                initializeAnnotationManager()
                
                savedCameraState?.let { state ->
                    val cameraOptions = CameraOptions.Builder()
                        .center(state.center)
                        .zoom(state.zoom)
                        .bearing(state.bearing)
                        .pitch(state.pitch)
                        .build()
                    
                    mapView.getMapboxMap().setCamera(cameraOptions)
                }
                
                currentLocation?.let { updateLocationPulse(it) }
                
                // Re-setup map click listener after theme change
                setupMapClickListener()
            }
            
            updateLogoColor(isDarkMode)
        }
    }
    
    private fun updateLogoColor(useDarkMode: Boolean) {
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
        clearPulseAnimation()
        createLocationPuck(location)
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
        
        // Re-setup map click listener when resuming
        Handler(Looper.getMainLooper()).postDelayed({
            setupMapClickListener()
        }, 100)
        
        currentLocation?.let {
            updateMapThemeBasedOnTime(it)
            startThemeUpdates()
        }
    }
}
