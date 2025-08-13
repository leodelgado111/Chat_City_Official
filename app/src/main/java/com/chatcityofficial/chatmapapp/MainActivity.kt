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
    
    // Based on your screenshots, the home icon outline is perfectly centered at 73dp
    // The icons appear to be spaced 62dp apart from each other
    // So we calculate positions relative to the home position
    private val iconPositions = mapOf(
        R.id.navigation_saved to 11f,      // 73 - 62 = 11
        R.id.navigation_home to 73f,       // Perfect position from screenshot
        R.id.navigation_create to 135f,    // 73 + 62 = 135
        R.id.navigation_chats to 197f,     // 73 + 62*2 = 197
        R.id.navigation_profile to 259f    // 73 + 62*3 = 259
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
        currentOutlinePosition = iconPositions[R.id.navigation_home] ?: 73f
        setOutlinePosition(currentOutlinePosition)
        
        // Set up click listeners for navigation buttons
        setupNavigationButtons()
        
        // Debug: Log the actual positions being used
        android.util.Log.d("NavBar", "Icon positions: $iconPositions")
    }
    
    private fun setupNavigationButtons() {
        // Saved button
        findViewById<View>(R.id.btn_saved).setOnClickListener {
            android.util.Log.d("NavBar", "Saved clicked - moving to position ${iconPositions[R.id.navigation_saved]}")
            navigateToDestination(R.id.navigation_saved)
            animateOutlineToPosition(R.id.navigation_saved)
        }
        
        // Home button
        findViewById<View>(R.id.btn_home).setOnClickListener {
            android.util.Log.d("NavBar", "Home clicked - moving to position ${iconPositions[R.id.navigation_home]}")
            navigateToDestination(R.id.navigation_home)
            animateOutlineToPosition(R.id.navigation_home)
        }
        
        // Create button
        findViewById<View>(R.id.btn_create).setOnClickListener {
            android.util.Log.d("NavBar", "Create clicked - moving to position ${iconPositions[R.id.navigation_create]}")
            navigateToDestination(R.id.navigation_create)
            animateOutlineToPosition(R.id.navigation_create)
        }
        
        // Chats button
        findViewById<View>(R.id.btn_chats).setOnClickListener {
            android.util.Log.d("NavBar", "Chats clicked - moving to position ${iconPositions[R.id.navigation_chats]}")
            navigateToDestination(R.id.navigation_chats)
            animateOutlineToPosition(R.id.navigation_chats)
        }
        
        // Profile button
        findViewById<View>(R.id.btn_profile).setOnClickListener {
            android.util.Log.d("NavBar", "Profile clicked - moving to position ${iconPositions[R.id.navigation_profile]}")
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
                android.util.Log.d("NavBar", "Animating outline to position: $value")
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