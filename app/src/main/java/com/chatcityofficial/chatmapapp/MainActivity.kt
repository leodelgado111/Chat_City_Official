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
    
    // DIAGNOSTIC: Let's use simple math based on what we know works
    // Home icon works perfectly at 73dp (from your screenshots)
    // Let's assume icons are evenly spaced in the 330dp container
    // 330dp / 5 icons = 66dp per icon
    // Icon centers should be at: 33, 99, 165, 231, 297 (in 330dp space)
    // But wait - the container is 334dp and gradient is centered
    // So we need to add 2dp offset: 35, 101, 167, 233, 299
    // Outline is 57dp wide, so subtract 28.5dp to center
    // That gives us: 6.5, 72.5, 138.5, 204.5, 270.5
    // But home at 73 works perfectly, not 72.5, so let's add 0.5 to all
    
    private val iconPositions = mapOf(
        R.id.navigation_saved to 7f,       // First icon
        R.id.navigation_home to 73f,       // This works perfectly from your test
        R.id.navigation_create to 139f,    // Center position
        R.id.navigation_chats to 205f,     // Fourth position
        R.id.navigation_profile to 271f    // Last position
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
        
        // DIAGNOSTIC: Let's see what the actual dimensions are
        navSelectionOutline.post {
            val container = findViewById<FrameLayout>(R.id.custom_nav_bar)
            val params = navSelectionOutline.layoutParams as FrameLayout.LayoutParams
            
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            android.util.Log.e("NAVBAR_DEBUG", "CONTAINER WIDTH: ${container.width}px")
            android.util.Log.e("NAVBAR_DEBUG", "OUTLINE WIDTH: ${navSelectionOutline.width}px")
            android.util.Log.e("NAVBAR_DEBUG", "OUTLINE HEIGHT: ${navSelectionOutline.height}px")
            android.util.Log.e("NAVBAR_DEBUG", "INITIAL MARGIN START: ${params.marginStart}px")
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
            
            // Get the actual pixel density
            val density = resources.displayMetrics.density
            android.util.Log.e("NAVBAR_DEBUG", "SCREEN DENSITY: $density")
            android.util.Log.e("NAVBAR_DEBUG", "73dp in pixels: ${(73 * density).toInt()}px")
            android.util.Log.e("NAVBAR_DEBUG", "=================================")
        }
        
        // Set initial position
        currentOutlinePosition = iconPositions[R.id.navigation_home] ?: 73f
        
        // Set up click listeners for navigation buttons
        setupNavigationButtons()
    }
    
    private fun setupNavigationButtons() {
        // Let's try a different approach - directly set positions without animation first
        findViewById<View>(R.id.btn_saved).setOnClickListener {
            android.util.Log.e("NAVBAR_DEBUG", "SAVED CLICKED - Moving to: ${iconPositions[R.id.navigation_saved]}dp")
            navigateToDestination(R.id.navigation_saved)
            // TEST: Set position directly without animation
            setOutlinePositionDirectly(iconPositions[R.id.navigation_saved] ?: 7f)
        }
        
        findViewById<View>(R.id.btn_home).setOnClickListener {
            android.util.Log.e("NAVBAR_DEBUG", "HOME CLICKED - Moving to: ${iconPositions[R.id.navigation_home]}dp")
            navigateToDestination(R.id.navigation_home)
            setOutlinePositionDirectly(iconPositions[R.id.navigation_home] ?: 73f)
        }
        
        findViewById<View>(R.id.btn_create).setOnClickListener {
            android.util.Log.e("NAVBAR_DEBUG", "CREATE CLICKED - Moving to: ${iconPositions[R.id.navigation_create]}dp")
            navigateToDestination(R.id.navigation_create)
            setOutlinePositionDirectly(iconPositions[R.id.navigation_create] ?: 139f)
        }
        
        findViewById<View>(R.id.btn_chats).setOnClickListener {
            android.util.Log.e("NAVBAR_DEBUG", "CHATS CLICKED - Moving to: ${iconPositions[R.id.navigation_chats]}dp")
            navigateToDestination(R.id.navigation_chats)
            setOutlinePositionDirectly(iconPositions[R.id.navigation_chats] ?: 205f)
        }
        
        findViewById<View>(R.id.btn_profile).setOnClickListener {
            android.util.Log.e("NAVBAR_DEBUG", "PROFILE CLICKED - Moving to: ${iconPositions[R.id.navigation_profile]}dp")
            navigateToDestination(R.id.navigation_profile)
            setOutlinePositionDirectly(iconPositions[R.id.navigation_profile] ?: 271f)
        }
    }
    
    private fun navigateToDestination(destinationId: Int) {
        try {
            navController.navigate(destinationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // TEST FUNCTION: Set position directly without animation
    private fun setOutlinePositionDirectly(positionDp: Float) {
        val density = resources.displayMetrics.density
        val positionPx = (positionDp * density).toInt()
        
        val layoutParams = navSelectionOutline.layoutParams as FrameLayout.LayoutParams
        layoutParams.marginStart = positionPx
        navSelectionOutline.layoutParams = layoutParams
        
        android.util.Log.e("NAVBAR_DEBUG", "Set margin to: ${positionPx}px (${positionDp}dp)")
        
        // Verify it actually moved
        navSelectionOutline.post {
            val actualMargin = (navSelectionOutline.layoutParams as FrameLayout.LayoutParams).marginStart
            android.util.Log.e("NAVBAR_DEBUG", "VERIFIED: Actual margin is now: ${actualMargin}px")
        }
    }
}