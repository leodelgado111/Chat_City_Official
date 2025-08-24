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
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.chatcityofficial.chatmapapp.ui.compose.navigation.ComposeBottomNavigationView
import com.chatcityofficial.chatmapapp.ui.compose.navigation.NavigationTab
import com.chatcityofficial.chatmapapp.ui.home.HomeFragment
import com.chatcityofficial.chatmapapp.ui.saved.SavedFragment
import com.chatcityofficial.chatmapapp.ui.profile.ProfileFragment
import com.chatcityofficial.chatmapapp.ui.chats.ChatsComposeFragment

class MainActivity : AppCompatActivity() {


    private lateinit var composeNavBar: ComposeBottomNavigationView
    
    // Track the current destination
    private var currentDestinationId = R.id.navigation_home
    private var previousDestinationId = R.id.navigation_home
    
    // Fragment instances to keep alive
    private var homeFragment: HomeFragment? = null
    private var savedFragment: SavedFragment? = null
    private var profileFragment: ProfileFragment? = null
    private var chatsFragment: ChatsComposeFragment? = null
    private var activeFragment: Fragment? = null
    
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
        composeNavBar = findViewById(R.id.compose_nav_bar)
        
        // Initialize fragments with manual management
        initializeFragments()
        
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
        
        // Note: We're no longer using NavController's destination listener 
        // since we're managing fragments manually
        
        // Setup back button behavior
        setupBackButtonBehavior()
        
        // Set up click listeners for navigation buttons
        setupNavigationButtons()
    }
    
    private fun setupBackButtonBehavior() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if the current active fragment can handle back press
                val currentFragment = activeFragment
                
                // Check if it's HomeFragment and if search is visible
                if (currentFragment is HomeFragment) {
                    if (currentFragment.hideSearchViewIfVisible()) {
                        // Search was visible and is now hidden, don't do anything else
                        return
                    }
                }
                
                // Handle navigation based on current destination
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
        })
    }
    
    private fun navigateToHome() {
        // Use fragment navigation for home
        navigateToFragment(R.id.navigation_home)
    }
    
    private fun initializeFragments() {
        // Create fragment instances if not already created
        if (homeFragment == null) {
            homeFragment = HomeFragment()
        }
        if (savedFragment == null) {
            savedFragment = SavedFragment()
        }
        if (profileFragment == null) {
            profileFragment = ProfileFragment()
        }
        if (chatsFragment == null) {
            chatsFragment = ChatsComposeFragment.newInstance()
        }
        
        // Add all fragments to the container but hide non-active ones
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        
        // Add fragments if not already added
        homeFragment?.let {
            if (!it.isAdded) {
                transaction.add(R.id.nav_host_fragment_activity_main, it, "home")
            }
        }
        savedFragment?.let {
            if (!it.isAdded) {
                transaction.add(R.id.nav_host_fragment_activity_main, it, "saved")
                transaction.hide(it)
            }
        }
        profileFragment?.let {
            if (!it.isAdded) {
                transaction.add(R.id.nav_host_fragment_activity_main, it, "profile")
                transaction.hide(it)
            }
        }
        chatsFragment?.let {
            if (!it.isAdded) {
                transaction.add(R.id.nav_host_fragment_activity_main, it, "chats")
                transaction.hide(it)
            }
        }
        
        transaction.commit()
        activeFragment = homeFragment
    }
    
    private fun setupNavigationButtons() {
        composeNavBar.setOnTabSelectedListener { tab ->
            when (tab) {
                NavigationTab.SAVED -> navigateToFragment(R.id.navigation_saved)
                NavigationTab.HOME -> navigateToFragment(R.id.navigation_home)
                NavigationTab.CREATE -> {
                    // Toggle post button visibility in HomeFragment
                    if (activeFragment is HomeFragment) {
                        (activeFragment as HomeFragment).togglePostButton()
                    }
                }
                NavigationTab.CHATS -> navigateToFragment(R.id.navigation_chats)
                NavigationTab.PROFILE -> navigateToFragment(R.id.navigation_profile)
            }
        }
    }
    
    private fun navigateToFragment(destinationId: Int) {
        try {
            // Only navigate if we're not already at this destination
            if (currentDestinationId != destinationId) {
                val targetFragment = when (destinationId) {
                    R.id.navigation_home -> homeFragment
                    R.id.navigation_saved -> savedFragment
                    R.id.navigation_chats -> chatsFragment
                    R.id.navigation_profile -> profileFragment
                    else -> null
                }
                
                targetFragment?.let { newFragment ->
                    val transaction = supportFragmentManager.beginTransaction()
                    
                    // Apply animations based on navigation direction
                    val animations = getNavigationAnimations(currentDestinationId, destinationId)
                    transaction.setCustomAnimations(
                        animations[0],  // enter
                        animations[1],  // exit
                        animations[2],  // popEnter
                        animations[3]   // popExit
                    )
                    
                    // Hide current fragment
                    activeFragment?.let { transaction.hide(it) }
                    
                    // If leaving home screen, hide the post buttons
                    if (currentDestinationId == R.id.navigation_home && destinationId != R.id.navigation_home) {
                        (homeFragment as? HomeFragment)?.hidePostButtons()
                    }
                    
                    // Show target fragment
                    transaction.show(newFragment)
                    transaction.commit()
                    
                    // Update tracking variables
                    activeFragment = newFragment
                    previousDestinationId = currentDestinationId
                    currentDestinationId = destinationId
                    navigationTabMap[destinationId]?.let { tab ->
                        composeNavBar.setSelectedTab(tab)
                    }
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
}
