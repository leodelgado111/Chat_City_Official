package com.chatcityofficial.chatmapapp

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navSelectionOutline: ImageView
    private lateinit var navView: BottomNavigationView
    
    // X positions for each navigation item (adjust these based on your icon positions)
    private val iconPositions = mapOf(
        R.id.navigation_saved to 14f,    // Saved icon position
        R.id.navigation_home to 76f,     // Home icon position  
        R.id.navigation_create to 137f,  // Create icon position
        R.id.navigation_chats to 198f,   // Chats icon position
        R.id.navigation_profile to 259f  // Profile icon position
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make status bar transparent
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        // Hide system navigation bar
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.let {
            it.hide(WindowInsetsCompat.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        setContentView(R.layout.activity_main)

        navView = findViewById(R.id.nav_view)
        navSelectionOutline = findViewById(R.id.nav_selection_outline)
        
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        
        // Remove all default animations by disabling icon tint changes
        navView.itemIconTintList = null
        
        // Set up navigation with controller
        navView.setupWithNavController(navController)
        
        // Handle navigation item selection with sliding animation
        navView.setOnItemSelectedListener { item ->
            // Animate the outline to the selected position
            animateOutlineToPosition(item.itemId)
            
            // Navigate to the selected destination
            navController.navigate(item.itemId)
            true
        }
        
        // Set initial position to Home
        navSelectionOutline.translationX = iconPositions[R.id.navigation_home] ?: 76f
    }
    
    private fun animateOutlineToPosition(itemId: Int) {
        val targetPosition = iconPositions[itemId] ?: return
        
        // Create smooth sliding animation
        ObjectAnimator.ofFloat(navSelectionOutline, "translationX", targetPosition).apply {
            duration = 300 // Animation duration in milliseconds
            start()
        }
    }
}