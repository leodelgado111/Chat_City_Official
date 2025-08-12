package com.chatcityofficial.chatmapapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.chatcityofficial.chatmapapp.R
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.attribution.attribution

class HomeFragment : Fragment() {

    private var mapView: MapView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        
        mapView = root.findViewById(R.id.mapView)
        
        // Test with Mapbox's built-in light monochrome style first
        mapView?.getMapboxMap()?.loadStyleUri("mapbox://styles/mapbox/light-v11") { style ->
            if (style != null) {
                Log.d("HomeFragment", "✅ Built-in light monochrome loaded")
                Toast.makeText(context, "✅ Built-in light style loaded", Toast.LENGTH_SHORT).show()
                
                // Remove all UI elements
                mapView?.scalebar?.enabled = false
                mapView?.location?.enabled = false  
                mapView?.logo?.enabled = false
                mapView?.attribution?.enabled = false  // Remove the info/attribution button
                
            } else {
                Log.e("HomeFragment", "❌ Built-in style failed")
                Toast.makeText(context, "❌ Built-in style failed", Toast.LENGTH_SHORT).show()
            }
        }
        
        return root
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
        mapView?.onDestroy()
        mapView = null
    }
}