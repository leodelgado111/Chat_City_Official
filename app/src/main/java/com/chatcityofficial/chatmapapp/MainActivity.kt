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

class MainActivity : AppCompatActivity() {

    private lateinit var navController: androidx.navigation.NavController
    private lateinit var navSelectionOutline: ImageView
    
    // X positions for centering outline on each icon
    // The gradient layer is 330dp wide, centered in 334dp container (2dp margin each side)
    // Icons in the gradient are at: 41, 103, 165, 227, 289 (from the SVG)
    // Outline is 57dp wide, needs to be centered on each icon
    // Initial layout position is 74dp (marginStart)
    
    private val iconPositions = mapOf(
        R.id.navigation_saved to -61f,     // (41 + 2 - 28.5) - 74 = -59.5 ≈ -61
        R.id.navigation_home to 0.5f,      // (103 + 2 - 28.5) - 74 = 2.5 ≈ 0.5
        R.id.navigation_create to 63.5f,   // (165 + 2 - 28.5) - 74 = 64.5 ≈ 63.5
        R.id.navigation_chats to 125.5f,   // (227 + 2 - 28.5) - 74 = 126.5 ≈ 125.5
        R.id.navigation_profile to 187.5f  // (289 + 2 - 28.5) - 74 = 188.5 ≈ 187.5
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
        
        // Get references
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        navSelectionOutline = findViewById(R.id.nav_selection_outline)
        
        // Set up click listeners for navigation buttons
        setupNavigationButtons()
        
        // Set initial outline position to home
        navSelectionOutline.translationX = 0.5f
    }
    
    private fun setupNavigationButtons() {
        // Saved button
        findViewById<View>(R.id.btn_saved).setOnClickListener {
            navigateToDestination(R.id.navigation_saved)
            animateOutlineToPosition(R.id.navigation_saved)
        }
        
        // Home button
        findViewById<View>(R.id.btn_home).setOnClickListener {
            navigateToDestination(R.id.navigation_home)
            animateOutlineToPosition(R.id.navigation_home)
        }
        
        // Create button
        findViewById<View>(R.id.btn_create).setOnClickListener {
            navigateToDestination(R.id.navigation_create)
            animateOutlineToPosition(R.id.navigation_create)
        }
        
        // Chats button
        findViewById<View>(R.id.btn_chats).setOnClickListener {
            navigateToDestination(R.id.navigation_chats)
            animateOutlineToPosition(R.id.navigation_chats)
        }
        
        // Profile button
        findViewById<View>(R.id.btn_profile).setOnClickListener {
            navigateToDestination(R.id.navigation_profile)
            animateOutlineToPosition(R.id.navigation_profile)
        }
    }
    
    private fun navigateToDestination(destinationId: Int) {
        try {
            navController.navigate(destinationId)
        } catch (e: Exception) {
            // Handle navigation error if destination doesn't exist
            e.printStackTrace()
        }
    }
    
    private fun animateOutlineToPosition(destinationId: Int) {
        val targetPosition = iconPositions[destinationId] ?: return
        
        // Create smooth sliding animation with 100ms duration
        ObjectAnimator.ofFloat(navSelectionOutline, "translationX", targetPosition).apply {
            duration = 100 // 100ms animation duration
            start()
        }
    }
}