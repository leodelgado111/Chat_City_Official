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
    private var currentOutlinePosition = 72.5f // Starting position for home
    
    // Based on actual device measurements from logs
    private val iconPositions = mapOf(
        R.id.navigation_saved to 6.5f,     
        R.id.navigation_home to 72.5f,     
        R.id.navigation_create to 138.5f,  
        R.id.navigation_chats to 204.5f,   
        R.id.navigation_profile to 270.5f  
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
        currentOutlinePosition = iconPositions[R.id.navigation_home] ?: 72.5f
        
        // EXTENSIVE LOGGING
        navSelectionOutline.post {
            val container = findViewById<FrameLayout>(R.id.custom_nav_bar)
            val params = navSelectionOutline.layoutParams as FrameLayout.LayoutParams
            val density = resources.displayMetrics.density
            
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            android.util.Log.e("NAVBAR_DEBUG", "INITIALIZATION VALUES:")
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            android.util.Log.e("NAVBAR_DEBUG", "Container width: ${container.width}px (${container.width/density}dp)")
            android.util.Log.e("NAVBAR_DEBUG", "Outline width: ${navSelectionOutline.width}px (${navSelectionOutline.width/density}dp)")
            android.util.Log.e("NAVBAR_DEBUG", "Outline height: ${navSelectionOutline.height}px (${navSelectionOutline.height/density}dp)")
            android.util.Log.e("NAVBAR_DEBUG", "Screen density: $density")
            android.util.Log.e("NAVBAR_DEBUG", "Initial margin from layout: ${params.marginStart}px (${params.marginStart/density}dp)")
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            android.util.Log.e("NAVBAR_DEBUG", "CONFIGURED POSITIONS (dp):")
            iconPositions.forEach { (key, value) ->
                val name = when(key) {
                    R.id.navigation_saved -> "SAVED"
                    R.id.navigation_home -> "HOME"
                    R.id.navigation_create -> "CREATE"
                    R.id.navigation_chats -> "CHATS"
                    R.id.navigation_profile -> "PROFILE"
                    else -> "UNKNOWN"
                }
                android.util.Log.e("NAVBAR_DEBUG", "$name: ${value}dp = ${(value * density).toInt()}px")
            }
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            
            // Set initial position programmatically
            setOutlinePosition(currentOutlinePosition)
            android.util.Log.e("NAVBAR_DEBUG", "Set initial position to HOME: ${currentOutlinePosition}dp")
        }
        
        // Set up click listeners for navigation buttons
        setupNavigationButtons()
    }
    
    private fun setupNavigationButtons() {
        findViewById<View>(R.id.btn_saved).setOnClickListener {
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            android.util.Log.e("NAVBAR_DEBUG", "SAVED BUTTON CLICKED")
            android.util.Log.e("NAVBAR_DEBUG", "Current position: ${currentOutlinePosition}dp")
            android.util.Log.e("NAVBAR_DEBUG", "Target position: ${iconPositions[R.id.navigation_saved]}dp")
            navigateToDestination(R.id.navigation_saved)
            animateOutlineToPosition(R.id.navigation_saved)
        }
        
        findViewById<View>(R.id.btn_home).setOnClickListener {
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            android.util.Log.e("NAVBAR_DEBUG", "HOME BUTTON CLICKED")
            android.util.Log.e("NAVBAR_DEBUG", "Current position: ${currentOutlinePosition}dp")
            android.util.Log.e("NAVBAR_DEBUG", "Target position: ${iconPositions[R.id.navigation_home]}dp")
            navigateToDestination(R.id.navigation_home)
            animateOutlineToPosition(R.id.navigation_home)
        }
        
        findViewById<View>(R.id.btn_create).setOnClickListener {
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            android.util.Log.e("NAVBAR_DEBUG", "CREATE BUTTON CLICKED")
            android.util.Log.e("NAVBAR_DEBUG", "Current position: ${currentOutlinePosition}dp")
            android.util.Log.e("NAVBAR_DEBUG", "Target position: ${iconPositions[R.id.navigation_create]}dp")
            navigateToDestination(R.id.navigation_create)
            animateOutlineToPosition(R.id.navigation_create)
        }
        
        findViewById<View>(R.id.btn_chats).setOnClickListener {
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            android.util.Log.e("NAVBAR_DEBUG", "CHATS BUTTON CLICKED")
            android.util.Log.e("NAVBAR_DEBUG", "Current position: ${currentOutlinePosition}dp")
            android.util.Log.e("NAVBAR_DEBUG", "Target position: ${iconPositions[R.id.navigation_chats]}dp")
            navigateToDestination(R.id.navigation_chats)
            animateOutlineToPosition(R.id.navigation_chats)
        }
        
        findViewById<View>(R.id.btn_profile).setOnClickListener {
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            android.util.Log.e("NAVBAR_DEBUG", "PROFILE BUTTON CLICKED")
            android.util.Log.e("NAVBAR_DEBUG", "Current position: ${currentOutlinePosition}dp")
            android.util.Log.e("NAVBAR_DEBUG", "Target position: ${iconPositions[R.id.navigation_profile]}dp")
            navigateToDestination(R.id.navigation_profile)
            animateOutlineToPosition(R.id.navigation_profile)
        }
    }
    
    private fun navigateToDestination(destinationId: Int) {
        try {
            navController.navigate(destinationId)
        } catch (e: Exception) {
            android.util.Log.e("NAVBAR_DEBUG", "Navigation error: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun animateOutlineToPosition(destinationId: Int) {
        val targetPosition = iconPositions[destinationId] ?: return
        val density = resources.displayMetrics.density
        
        android.util.Log.e("NAVBAR_DEBUG", "Starting animation...")
        android.util.Log.e("NAVBAR_DEBUG", "From: ${currentOutlinePosition}dp (${(currentOutlinePosition * density).toInt()}px)")
        android.util.Log.e("NAVBAR_DEBUG", "To: ${targetPosition}dp (${(targetPosition * density).toInt()}px)")
        
        // Animate with proper dp to pixel conversion
        ValueAnimator.ofFloat(currentOutlinePosition, targetPosition).apply {
            duration = 100 // 100ms animation
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                setOutlinePosition(value)
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    val finalMargin = (navSelectionOutline.layoutParams as FrameLayout.LayoutParams).marginStart
                    android.util.Log.e("NAVBAR_DEBUG", "Animation complete!")
                    android.util.Log.e("NAVBAR_DEBUG", "Final margin: ${finalMargin}px (${finalMargin/density}dp)")
                    android.util.Log.e("NAVBAR_DEBUG", "Expected: ${(targetPosition * density).toInt()}px")
                    if (finalMargin != (targetPosition * density).toInt()) {
                        android.util.Log.e("NAVBAR_DEBUG", "WARNING: Position mismatch!")
                    }
                }
            })
            start()
        }
        
        currentOutlinePosition = targetPosition
    }
    
    private fun setOutlinePosition(positionDp: Float) {
        val density = resources.displayMetrics.density
        val positionPx = (positionDp * density).toInt()
        
        val layoutParams = navSelectionOutline.layoutParams as FrameLayout.LayoutParams
        layoutParams.marginStart = positionPx
        navSelectionOutline.layoutParams = layoutParams
    }
}