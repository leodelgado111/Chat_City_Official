package com.chatcityofficial.chatmapapp.ui.home

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
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
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatcityofficial.chatmapapp.BuildConfig
import com.chatcityofficial.chatmapapp.R
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.*
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var locationText: TextView
    private lateinit var locationContainer: LinearLayout
    private lateinit var chatCityLogo: ImageView
    private lateinit var searchContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var closeSearchButton: ImageButton
    private lateinit var searchResultsCard: CardView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var poweredByGoogle: TextView
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var searchAdapter: PlaceSearchAdapter
    private var sessionToken: AutocompleteSessionToken? = null
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    
    private var currentLocation: Location? = null
    private var currentLocationMarker: CircleAnnotation? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "HomeFragment"
        private const val SEARCH_DELAY_MS = 300L
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
        locationContainer = view.findViewById(R.id.locationContainer)
        chatCityLogo = view.findViewById(R.id.chatCityLogo)
        searchContainer = view.findViewById(R.id.searchContainer)
        searchEditText = view.findViewById(R.id.searchEditText)
        closeSearchButton = view.findViewById(R.id.closeSearchButton)
        searchResultsCard = view.findViewById(R.id.searchResultsCard)
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView)
        poweredByGoogle = view.findViewById(R.id.poweredByGoogle)
        
        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.GOOGLE_MAPS_API_KEY)
        }
        placesClient = Places.createClient(requireContext())
        sessionToken = AutocompleteSessionToken.newInstance()
        
        // Setup search adapter
        searchAdapter = PlaceSearchAdapter { prediction ->
            onPlaceSelected(prediction)
        }
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(context)
        searchResultsRecyclerView.adapter = searchAdapter
        
        // Setup click listeners
        locationContainer.setOnClickListener {
            showSearchView()
        }
        
        closeSearchButton.setOnClickListener {
            hideSearchView()
        }
        
        // Setup search text watcher
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                
                if (s.isNullOrEmpty()) {
                    searchResultsCard.visibility = View.GONE
                    poweredByGoogle.visibility = View.GONE
                    searchAdapter.updatePredictions(emptyList())
                } else {
                    searchRunnable = Runnable {
                        performSearch(s.toString())
                    }
                    searchHandler.postDelayed(searchRunnable!!, SEARCH_DELAY_MS)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Initialize map
        initializeMap()
        
        return view
    }
    
    private fun showSearchView() {
        // Animate hiding logo and location container
        val logoFadeOut = ObjectAnimator.ofFloat(chatCityLogo, "alpha", 1f, 0f)
        val locationFadeOut = ObjectAnimator.ofFloat(locationContainer, "alpha", 1f, 0f)
        
        val hideAnimatorSet = AnimatorSet()
        hideAnimatorSet.playTogether(logoFadeOut, locationFadeOut)
        hideAnimatorSet.duration = 200
        
        hideAnimatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                chatCityLogo.visibility = View.GONE
                locationContainer.visibility = View.GONE
                
                // Show search container
                searchContainer.visibility = View.VISIBLE
                searchContainer.alpha = 0f
                searchContainer.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
                
                // Focus on search field and show keyboard
                searchEditText.requestFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        })
        
        hideAnimatorSet.start()
    }
    
    private fun hideSearchView() {
        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        
        // Clear search
        searchEditText.text.clear()
        searchResultsCard.visibility = View.GONE
        poweredByGoogle.visibility = View.GONE
        
        // Animate hiding search container
        searchContainer.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                searchContainer.visibility = View.GONE
                
                // Show logo and location container
                chatCityLogo.visibility = View.VISIBLE
                locationContainer.visibility = View.VISIBLE
                chatCityLogo.alpha = 0f
                locationContainer.alpha = 0f
                
                val logoFadeIn = ObjectAnimator.ofFloat(chatCityLogo, "alpha", 0f, 1f)
                val locationFadeIn = ObjectAnimator.ofFloat(locationContainer, "alpha", 0f, 1f)
                
                val showAnimatorSet = AnimatorSet()
                showAnimatorSet.playTogether(logoFadeIn, locationFadeIn)
                showAnimatorSet.duration = 200
                showAnimatorSet.start()
            }
            .start()
    }
    
    private fun performSearch(query: String) {
        val bounds = currentLocation?.let {
            val southwest = LatLng(it.latitude - 0.5, it.longitude - 0.5)
            val northeast = LatLng(it.latitude + 0.5, it.longitude + 0.5)
            RectangularBounds.newInstance(southwest, northeast)
        }
        
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(query)
            .apply {
                bounds?.let { setLocationBias(it) }
            }
            .build()
        
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions
                if (predictions.isNotEmpty()) {
                    searchAdapter.updatePredictions(predictions)
                    searchResultsCard.visibility = View.VISIBLE
                    poweredByGoogle.visibility = View.VISIBLE
                } else {
                    searchResultsCard.visibility = View.GONE
                    poweredByGoogle.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                if (exception is ApiException) {
                    Log.e(TAG, "Place not found: ${exception.statusCode}")
                }
                searchResultsCard.visibility = View.GONE
                poweredByGoogle.visibility = View.GONE
            }
    }
    
    private fun onPlaceSelected(prediction: AutocompletePrediction) {
        val placeId = prediction.placeId
        
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.ADDRESS_COMPONENTS
        )
        
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
        
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val latLng = place.latLng
                
                if (latLng != null) {
                    // Move map to selected location
                    moveToLocation(latLng.latitude, latLng.longitude, 14.0)
                    
                    // Update location text with city/town name
                    updateLocationTextForPlace(place)
                    
                    // Hide search view
                    hideSearchView()
                    
                    // Reset session token for billing optimization
                    sessionToken = AutocompleteSessionToken.newInstance()
                }
            }
            .addOnFailureListener { exception ->
                if (exception is ApiException) {
                    Log.e(TAG, "Place not found: ${exception.statusCode}")
                    Toast.makeText(context, "Error loading place details", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun updateLocationTextForPlace(place: Place) {
        // Try to get city name from address components first
        var cityName: String? = null
        
        place.addressComponents?.asList()?.forEach { component ->
            val types = component.types
            if (types.contains("locality") || types.contains("administrative_area_level_3")) {
                cityName = component.name
                return@forEach
            }
        }
        
        // If no city found in components, try parsing the address
        if (cityName == null) {
            val address = place.address ?: place.name ?: "Unknown"
            val components = address.split(",")
            
            cityName = when {
                components.size >= 2 -> {
                    // Try to get the component that looks like a city (usually before state/country)
                    val potentialCity = components[components.size - 2].trim()
                    // Check if it's not a zip code or state abbreviation
                    if (!potentialCity.matches(Regex("\\d+")) && potentialCity.length > 2) {
                        potentialCity
                    } else if (components.size >= 3) {
                        components[components.size - 3].trim()
                    } else {
                        place.name
                    }
                }
                place.name != null -> place.name
                else -> "Unknown"
            }
        }
        
        locationText.text = cityName
    }
    
    private fun initializeMap() {
        mapView.getMapboxMap().apply {
            loadStyleUri(Style.MAPBOX_STREETS) { style ->
                // Map style loaded
                Log.d(TAG, "Map style loaded")
                
                // Enable location component but don't show the default puck
                mapView.location.enabled = false
                
                // Check permissions and get location
                checkLocationPermission()
            }
        }
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
                updateLocationMarker(it)
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
                    updateLocationMarker(location)
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
    
    private fun updateLocationMarker(location: Location) {
        mapView.getMapboxMap().getStyle { style ->
            val annotationApi = mapView.annotations
            val circleAnnotationManager = annotationApi.createCircleAnnotationManager()
            
            // Remove existing marker if any
            currentLocationMarker?.let {
                circleAnnotationManager.delete(it)
            }
            
            // Create new marker
            val circleAnnotationOptions = CircleAnnotationOptions()
                .withPoint(Point.fromLngLat(location.longitude, location.latitude))
                .withCircleRadius(8.0)
                .withCircleColor("#4285F4")
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#FFFFFF")
            
            currentLocationMarker = circleAnnotationManager.create(circleAnnotationOptions)
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
        mapView.onDestroy()
        scope.cancel()
        searchHandler.removeCallbacksAndMessages(null)
    }
}
