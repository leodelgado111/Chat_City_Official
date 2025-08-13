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
    private var currentOutlinePosition = 74f // Starting position for home icon
    
    // Absolute positions for centering outline on each icon
    // Icons in the gradient SVG are at: 41, 103, 165, 227, 289
    // Outline is 57dp wide, so we subtract 28.5 to center it
    // Adding 2dp for the gradient centering offset
    private val iconPositions = mapOf(
        R.id.navigation_saved to 14.5f,    // 41 - 28.5 + 2 = 14.5
        R.id.navigation_home to 76.5f,     // 103 - 28.5 + 2 = 76.5  
        R.id.navigation_create to 138.5f,  // 165 - 28.5 + 2 = 138.5
        R.id.navigation_chats to 200.5f,   // 227 - 28.5 + 2 = 200.5
        R.id.navigation_profile to 262.5f  // 289 - 28.5 + 2 = 262.5
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
        
        // Set initial position to home
        setOutlinePosition(iconPositions[R.id.navigation_home] ?: 76.5f)
        
        // Set up click listeners for navigation buttons
        setupNavigationButtons()
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
        
        // Animate the margin change instead of translation
        ValueAnimator.ofFloat(currentOutlinePosition, targetPosition).apply {
            duration = 100 // 100ms animation duration
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                setOutlinePosition(value)
            }
            start()
        }
        
        currentOutlinePosition = targetPosition
    }
    
    private fun setOutlinePosition(position: Float) {
        val layoutParams = navSelectionOutline.layoutParams as FrameLayout.LayoutParams
        layoutParams.marginStart = position.toInt()
        navSelectionOutline.layoutParams = layoutParams
    }
}