package com.chatcityofficial.chatmapapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SplashActivity"
        private const val SPLASH_DELAY = 2000L // 2 seconds
        private const val FADE_DURATION = 500L // 0.5 seconds for fade animation
    }
    
    private lateinit var splashImage: ImageView
    private lateinit var handler: Handler
    private lateinit var navigateRunnable: Runnable
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Disable screen rotation - lock to portrait mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        Log.d(TAG, "onCreate: Starting splash screen")
        
        // Make fullscreen with transparent navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Make both status bar and navigation bar transparent
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        
        // Hide status bar during splash but keep navigation bar visible
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.let {
            it.hide(WindowInsetsCompat.Type.statusBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        setContentView(R.layout.activity_splash)
        
        // Get reference to the splash image
        splashImage = findViewById(R.id.ivSplashImage)
        
        // Initialize handler
        handler = Handler(Looper.getMainLooper())
        
        // Create the navigation runnable
        navigateRunnable = Runnable {
            navigateToMain()
        }
        
        // Start animations
        startLogoAnimation()
        
        // Schedule navigation to main activity
        handler.postDelayed(navigateRunnable, SPLASH_DELAY)
    }
    
    private fun startLogoAnimation() {
        // Start with image invisible
        splashImage.alpha = 0f
        splashImage.scaleX = 0.8f
        splashImage.scaleY = 0.8f
        
        // Fade in and scale up animation
        splashImage.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(FADE_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(null)
            .start()
        
        Log.d(TAG, "startLogoAnimation: Logo animation started")
    }
    
    private fun navigateToMain() {
        Log.d(TAG, "navigateToMain: Starting navigation to MainActivity")
        
        // Fade out animation before navigating
        splashImage.animate()
            .alpha(0f)
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(FADE_DURATION / 2)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    Log.d(TAG, "navigateToMain: Fade out complete, launching MainActivity")
                    
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    
                    // Use a smooth transition
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    
                    // Finish splash activity
                    finish()
                }
            })
            .start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up the handler callback to prevent memory leaks
        handler.removeCallbacks(navigateRunnable)
        Log.d(TAG, "onDestroy: Splash activity destroyed")
    }
    
    override fun onBackPressed() {
        // Disable back button during splash screen
        // Do nothing
    }
}