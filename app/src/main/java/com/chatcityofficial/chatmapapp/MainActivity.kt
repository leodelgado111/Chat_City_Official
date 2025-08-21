package com.chatcityofficial.chatmapapp

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
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
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import com.chatcityofficial.chatmapapp.ui.compose.chats.ChatsComposeActivity
import com.chatcityofficial.chatmapapp.ui.compose.navigation.ComposeBottomNavigationView
import com.chatcityofficial.chatmapapp.ui.compose.navigation.NavigationTab

class MainActivity : AppCompatActivity() {

    private lateinit var navController: androidx.navigation.NavController
    private lateinit var composeNavBar: ComposeBottomNavigationView
    
    // Track the current destination
    private var currentDestinationId = R.id.navigation_home
    private var previousDestinationId = R.id.navigation_home
    
    companion object {
        private const val CHATS_ACTIVITY_REQUEST_CODE = 1001
    }
    
    // Map between navigation IDs and NavigationTab enum
    private val navigationTabMap = mapOf(
        R.id.navigation_saved to NavigationTab.SAVED,
        R.id.navigation_home to NavigationTab.HOME,
        R.id.navigation_create to NavigationTab.CREATE,
        R.id.navigation_chats to NavigationTab.CHATS,
        R.id.navigation_profile to NavigationTab.PROFILE
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
        composeNavBar = findViewById(R.id.compose_nav_bar)
        
        // Apply window insets to position the nav bar correctly above the system navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(composeNavBar) { view, windowInsets ->
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
        
        // Set initial tab to home
        composeNavBar.setSelectedTab(NavigationTab.HOME)
        
        // Add navigation destination change listener to track current screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Update current destination when navigation changes
            when (destination.id) {
                R.id.navigation_saved,
                R.id.navigation_home,
                R.id.navigation_chats,
                R.id.navigation_profile -> {
                    currentDestinationId = destination.id
                    // Update selected tab when destination changes
                    navigationTabMap[destination.id]?.let { tab ->
                        composeNavBar.setSelectedTab(tab)
                    }
                }
            }
        }
        
        // Setup back button behavior
        setupBackButtonBehavior()
        
        // Set up click listeners for navigation buttons
        setupNavigationButtons()
    }
    
    private fun setupBackButtonBehavior() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // First check if the current fragment can handle back press
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
                val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
                
                // Check if it's HomeFragment and if search is visible
                if (currentFragment is com.chatcityofficial.chatmapapp.ui.home.HomeFragment) {
                    if (currentFragment.hideSearchViewIfVisible()) {
                        // Search was visible and is now hidden, don't do anything else
                        return
                    }
                }
                
                // Check if we can pop the back stack (for nested navigation)
                val popped = navController.popBackStack()
                
                if (!popped) {
                    // We're at a root destination (no back stack to pop)
                    when (currentDestinationId) {
                        R.id.navigation_home -> {
                            // We're on home screen, minimize the app
                            moveTaskToBack(true)
                        }
                        R.id.navigation_saved,
                        R.id.navigation_chats,
                        R.id.navigation_profile -> {
                            // Navigate back to home from other root destinations
                            navigateToHome()
                        }
                    }
                }
                // If popped is true, the navigation controller handled it and will update the destination
            }
        })
    }
    
    private fun navigateToHome() {
        // Navigate to home and clear any back stack using NavOptions
        val navOptions = NavOptions.Builder()
            .setPopUpTo(navController.graph.startDestinationId, false)
            .setLaunchSingleTop(true)
            .build()
            
        navController.navigate(R.id.navigation_home, null, navOptions)
        currentDestinationId = R.id.navigation_home
        composeNavBar.setSelectedTab(NavigationTab.HOME)
    }
    
    private fun setupNavigationButtons() {
        composeNavBar.setOnTabSelectedListener { tab ->
            when (tab) {
                NavigationTab.SAVED -> navigateToDestination(R.id.navigation_saved)
                NavigationTab.HOME -> navigateToDestination(R.id.navigation_home)
                NavigationTab.CREATE -> {
                    // Do nothing when create button is tapped (for now)
                    // No navigation, no action at all
                }
                NavigationTab.CHATS -> {
                    // Store the previous destination before updating
                    previousDestinationId = currentDestinationId
                    
                    // Launch ChatsComposeActivity instead of navigating to fragment
                    val intent = Intent(this, ChatsComposeActivity::class.java)
                    startActivityForResult(intent, CHATS_ACTIVITY_REQUEST_CODE)
                    overridePendingTransition(R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_left)
                    
                    // Update the selected tab to show chats is selected
                    currentDestinationId = R.id.navigation_chats
                }
                NavigationTab.PROFILE -> navigateToDestination(R.id.navigation_profile)
            }
        }
    }
    
    private fun navigateToDestination(destinationId: Int) {
        try {
            // Only navigate if we're not already at this destination
            if (currentDestinationId != destinationId) {
                // Determine animation direction based on current and target destinations
                val animations = getNavigationAnimations(currentDestinationId, destinationId)
                
                // Use enhanced NavOptions with Fragment Transition API optimizations
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(navController.graph.startDestinationId, false, true)
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setEnterAnim(animations[0])      // enter
                    .setExitAnim(animations[1])       // exit  
                    .setPopEnterAnim(animations[2])   // popEnter
                    .setPopExitAnim(animations[3])    // popExit
                    .build()
                
                // Navigate with optimized settings
                navController.navigate(destinationId, null, navOptions)
                
                previousDestinationId = currentDestinationId
                currentDestinationId = destinationId
                navigationTabMap[destinationId]?.let { tab ->
                    composeNavBar.setSelectedTab(tab)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getNavigationAnimations(currentId: Int, targetId: Int): List<Int> {
        // Define tab order for determining slide direction
        val tabOrder = listOf(
            R.id.navigation_saved,
            R.id.navigation_home,
            R.id.navigation_create,
            R.id.navigation_chats,
            R.id.navigation_profile
        )
        
        val currentIndex = tabOrder.indexOf(currentId)
        val targetIndex = tabOrder.indexOf(targetId)
        
        // Log for debugging
        android.util.Log.d("MainActivity", "Navigation from index $currentIndex to $targetIndex")
        
        return if (targetIndex > currentIndex) {
            // Moving right (to higher index) - new screen slides in from right
            android.util.Log.d("MainActivity", "Sliding right: fragment optimized animations")
            listOf(
                R.anim.fragment_slide_in_right,     // enter
                R.anim.fragment_slide_out_left,     // exit
                R.anim.fragment_slide_in_left,      // popEnter (back animation)
                R.anim.fragment_slide_out_right     // popExit (back animation)
            )
        } else {
            // Moving left (to lower index) - new screen slides in from left  
            android.util.Log.d("MainActivity", "Sliding left: fragment optimized animations")
            listOf(
                R.anim.fragment_slide_in_left,      // enter
                R.anim.fragment_slide_out_right,    // exit
                R.anim.fragment_slide_in_right,     // popEnter (back animation)
                R.anim.fragment_slide_out_left      // popExit (back animation)
            )
        }
    }
    
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == CHATS_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            // When returning from ChatsComposeActivity, restore the previous screen
            if (previousDestinationId != R.id.navigation_chats && previousDestinationId != 0) {
                // Restore the previous destination
                val destinationToRestore = previousDestinationId
                currentDestinationId = destinationToRestore
                navigationTabMap[destinationToRestore]?.let { tab ->
                    composeNavBar.setSelectedTab(tab)
                }
                
                // Navigate to the previous destination with a small delay to ensure smooth transition
                window.decorView.postDelayed({
                    when (destinationToRestore) {
                        R.id.navigation_saved, R.id.navigation_home, R.id.navigation_profile -> {
                            try {
                                navController.navigate(destinationToRestore)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Failed to navigate back from chats", e)
                            }
                        }
                    }
                }, 50)
            } else {
                // Default to home if no previous destination
                currentDestinationId = R.id.navigation_home
                composeNavBar.setSelectedTab(NavigationTab.HOME)
            }
        }
    }
}
