package com.chatcityofficial.chatmapapp

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.navigation.findNavController

class MainActivity : AppCompatActivity() {

    private lateinit var navController: androidx.navigation.NavController
    private lateinit var navSelectionOutline: ImageView
    private lateinit var customNavBar: FrameLayout
    private var currentOutlinePosition = 74.5f
    
    // FINAL POSITIONS - Based on actual SVG icon positions
    // Icons in SVG are at: 41, 103, 165, 227, 289
    // Container is 334dp, gradient is 330dp centered (2dp offset)
    // Outline is 57dp wide, needs 28.5dp offset to center
    // Final formula: (icon_position_in_svg * scale_factor) + container_offset - outline_centering
    // Scale factor = 1 (since we're using dp directly)
    private val iconPositions = mapOf(
        R.id.navigation_saved to 14.5f,    // 41 + 2 - 28.5 = 14.5
        R.id.navigation_home to 76.5f,     // 103 + 2 - 28.5 = 76.5
        R.id.navigation_create to 138.5f,  // 165 + 2 - 28.5 = 138.5
        R.id.navigation_chats to 200.5f,   // 227 + 2 - 28.5 = 200.5
        R.id.navigation_profile to 262.5f  // 289 + 2 - 28.5 = 262.5
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Disable screen rotation - lock to portrait mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        // Make both status bar and navigation bar transparent
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Set up transparent system bars
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        
        setContentView(R.layout.activity_main)
        
        // Get references
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        navSelectionOutline = findViewById(R.id.nav_selection_outline)
        customNavBar = findViewById(R.id.custom_nav_bar)
        
        // Apply window insets to position the nav bar correctly above the system navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(customNavBar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Set the bottom margin to be system navigation bar height + 12dp (22dp - 10dp additional adjustment)
            // Original was 22dp, subtracting 10dp makes it 12dp
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val additionalMargin = (12 * resources.displayMetrics.density).toInt()
                bottomMargin = insets.bottom + additionalMargin
            }
            
            WindowInsetsCompat.CONSUMED
        }
        
        // Request that insets be applied
        ViewCompat.requestApplyInsets(window.decorView)
        
        // Set initial position to home
        currentOutlinePosition = iconPositions[R.id.navigation_home] ?: 76.5f
        navSelectionOutline.post {
            setOutlinePosition(currentOutlinePosition)
        }
        
        // Set up click listeners for navigation buttons
        setupNavigationButtons()
    }
    
    private fun setupNavigationButtons() {
        findViewById<View>(R.id.btn_saved).setOnClickListener {
            navigateToDestination(R.id.navigation_saved)
            animateOutlineToPosition(R.id.navigation_saved)
        }
        
        findViewById<View>(R.id.btn_home).setOnClickListener {
            navigateToDestination(R.id.navigation_home)
            animateOutlineToPosition(R.id.navigation_home)
        }
        
        findViewById<View>(R.id.btn_create).setOnClickListener {
            // Do nothing when create button is tapped (for now)
            // No navigation, no outline movement, no action at all
        }
        
        findViewById<View>(R.id.btn_chats).setOnClickListener {
            navigateToDestination(R.id.navigation_chats)
            animateOutlineToPosition(R.id.navigation_chats)
        }
        
        findViewById<View>(R.id.btn_profile).setOnClickListener {
            navigateToDestination(R.id.navigation_profile)
            animateOutlineToPosition(R.id.navigation_profile)
        }
    }
    
    private fun navigateToDestination(destinationId: Int) {
        try {
            navController.navigate(destinationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun animateOutlineToPosition(destinationId: Int) {
        val targetPosition = iconPositions[destinationId] ?: return
        
        // Animate with proper dp to pixel conversion
        ValueAnimator.ofFloat(currentOutlinePosition, targetPosition).apply {
            duration = 100 // 100ms animation
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                setOutlinePosition(value)
            }
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
