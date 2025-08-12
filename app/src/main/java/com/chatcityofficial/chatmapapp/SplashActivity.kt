package com.chatcityofficial.chatmapapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {
    
    private lateinit var ivSplashImage: ImageView
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Make the status bar transparent
        makeStatusBarTransparent()
        
        // Initialize views
        initViews()
        
        // Start animations
        startAnimations()
        
        // Navigate to MainActivity after delay
        navigateToMain()
    }
    
    private fun makeStatusBarTransparent() {
        // Make status bar transparent
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        
        // Hide the status bar and navigation bar for immersive experience
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
    }
    
    private fun initViews() {
        ivSplashImage = findViewById(R.id.ivSplashImage)
        progressBar = findViewById(R.id.progressBar)
        
        // Image is already visible from system splash, no need to hide it
        // Only hide progress bar for animation
        progressBar.alpha = 0f
    }
    
    private fun startAnimations() {
        // No need to animate the splash image since it's already showing from system splash
        // This creates a seamless transition
        
        // Progress bar animation - fade in after delay
        progressBar.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    
    private fun navigateToMain() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            
            // Add a fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            
            finish()
        }, 3000) // 3 second delay
    }
}