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
    // Based on: icons are at positions 41, 103, 165, 227, 289 (center points)
    // Outline width is 57dp, so we need to offset by 28.5dp to center
    // Initial position in layout is 75dp (for home icon)
    private val iconPositions = mapOf(
        R.id.navigation_saved to -62.5f,   // 41 - 28.5 - 75 = -62.5 (move left from home position)
        R.id.navigation_home to 0f,        // 103 - 28.5 - 75 = 0 (home position, no translation)
        R.id.navigation_create to 61.5f,   // 165 - 28.5 - 75 = 61.5 (move right from home)
        R.id.navigation_chats to 123.5f,   // 227 - 28.5 - 75 = 123.5 (move right from home)
        R.id.navigation_profile to 185.5f  // 289 - 28.5 - 75 = 185.5 (move right from home)
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
        
        // Set initial outline position to home (no translation needed)
        navSelectionOutline.translationX = 0f
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