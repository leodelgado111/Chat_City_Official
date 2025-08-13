package com.chatcityofficial.chatmapapp

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.findNavController

class MainActivity : AppCompatActivity() {

    private lateinit var navController: androidx.navigation.NavController
    private lateinit var navSelectionOutline: ImageView
    private var currentOutlinePosition = 73f // Starting position for home icon
    
    // CRITICAL ANALYSIS:
    // The container is 334dp wide, gradient layer is 330dp (centered with 2dp margins)
    // Icons in SVG are at x-coordinates: 41, 103, 165, 227, 289
    // BUT these are in a 330dp viewBox, not actual dp positions
    // 
    // The gradient is centered in the container: 2dp offset on each side
    // So actual icon positions in the container are: SVG_position + 2dp
    // Real positions: 43, 105, 167, 229, 291
    //
    // The outline is 57dp wide, needs 28.5dp offset to center
    // Final positions: icon_position - 28.5
    
    private val iconPositions = mapOf(
        R.id.navigation_saved to 14.5f,    // 43 - 28.5 = 14.5
        R.id.navigation_home to 76.5f,     // 105 - 28.5 = 76.5
        R.id.navigation_create to 138.5f,  // 167 - 28.5 = 138.5
        R.id.navigation_chats to 200.5f,   // 229 - 28.5 = 200.5
        R.id.navigation_profile to 262.5f  // 291 - 28.5 = 262.5
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
        
        // IMPORTANT: Set the initial position programmatically to ensure consistency
        // The layout XML has marginStart="73dp" but we need 76.5dp for proper centering
        currentOutlinePosition = iconPositions[R.id.navigation_home] ?: 76.5f
        navSelectionOutline.post {
            setOutlinePosition(currentOutlinePosition)
        }
        
        // Set up click listeners for navigation buttons
        setupNavigationButtons()
        
        // Debug: Log container and outline dimensions after layout
        navSelectionOutline.post {
            val container = findViewById<FrameLayout>(R.id.custom_nav_bar)
            android.util.Log.d("NavBar", "Container width: ${container.width}")
            android.util.Log.d("NavBar", "Outline width: ${navSelectionOutline.width}")
            android.util.Log.d("NavBar", "Initial outline marginStart: ${(navSelectionOutline.layoutParams as FrameLayout.LayoutParams).marginStart}")
            android.util.Log.d("NavBar", "Icon positions map: $iconPositions")
        }
    }
    
    private fun setupNavigationButtons() {
        // Saved button
        findViewById<View>(R.id.btn_saved).setOnClickListener {
            android.util.Log.d("NavBar", "=== SAVED CLICKED ===")
            android.util.Log.d("NavBar", "Current position: $currentOutlinePosition")
            android.util.Log.d("NavBar", "Target position: ${iconPositions[R.id.navigation_saved]}")
            navigateToDestination(R.id.navigation_saved)
            animateOutlineToPosition(R.id.navigation_saved)
        }
        
        // Home button
        findViewById<View>(R.id.btn_home).setOnClickListener {
            android.util.Log.d("NavBar", "=== HOME CLICKED ===")
            android.util.Log.d("NavBar", "Current position: $currentOutlinePosition")
            android.util.Log.d("NavBar", "Target position: ${iconPositions[R.id.navigation_home]}")
            navigateToDestination(R.id.navigation_home)
            animateOutlineToPosition(R.id.navigation_home)
        }
        
        // Create button
        findViewById<View>(R.id.btn_create).setOnClickListener {
            android.util.Log.d("NavBar", "=== CREATE CLICKED ===")
            android.util.Log.d("NavBar", "Current position: $currentOutlinePosition")
            android.util.Log.d("NavBar", "Target position: ${iconPositions[R.id.navigation_create]}")
            navigateToDestination(R.id.navigation_create)
            animateOutlineToPosition(R.id.navigation_create)
        }
        
        // Chats button
        findViewById<View>(R.id.btn_chats).setOnClickListener {
            android.util.Log.d("NavBar", "=== CHATS CLICKED ===")
            android.util.Log.d("NavBar", "Current position: $currentOutlinePosition")
            android.util.Log.d("NavBar", "Target position: ${iconPositions[R.id.navigation_chats]}")
            navigateToDestination(R.id.navigation_chats)
            animateOutlineToPosition(R.id.navigation_chats)
        }
        
        // Profile button
        findViewById<View>(R.id.btn_profile).setOnClickListener {
            android.util.Log.d("NavBar", "=== PROFILE CLICKED ===")
            android.util.Log.d("NavBar", "Current position: $currentOutlinePosition")
            android.util.Log.d("NavBar", "Target position: ${iconPositions[R.id.navigation_profile]}")
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
        
        android.util.Log.d("NavBar", "Starting animation from $currentOutlinePosition to $targetPosition")
        
        // Animate the margin change
        ValueAnimator.ofFloat(currentOutlinePosition, targetPosition).apply {
            duration = 100 // 100ms animation duration
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                setOutlinePosition(value)
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    android.util.Log.d("NavBar", "Animation completed. Final position: $targetPosition")
                    // Verify the final position
                    val finalMargin = (navSelectionOutline.layoutParams as FrameLayout.LayoutParams).marginStart
                    android.util.Log.d("NavBar", "Actual final marginStart: $finalMargin")
                }
            })
            start()
        }
        
        currentOutlinePosition = targetPosition
    }
    
    private fun setOutlinePosition(position: Float) {
        val layoutParams = navSelectionOutline.layoutParams as FrameLayout.LayoutParams
        val newMargin = position.toInt()
        layoutParams.marginStart = newMargin
        navSelectionOutline.layoutParams = layoutParams
        
        // Force layout update
        navSelectionOutline.requestLayout()
    }
}