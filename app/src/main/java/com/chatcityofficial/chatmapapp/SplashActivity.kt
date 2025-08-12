package com.chatcityofficial.chatmapapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class SplashActivity : AppCompatActivity() {
    
    private lateinit var ivSplashImage: ImageView
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // CRITICAL: Remove the translucent flag once activity starts
        // This makes the activity opaque after the system splash is skipped
        window.setWindowIsTranslucent(false)
        
        // Make it truly fullscreen - edge to edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_splash)
        
        // Set the background color to match the gradient start
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#FF9AC8"))
        
        // Hide system bars
        hideSystemUI()
        
        // Initialize views
        initViews()
        
        // Start animations immediately for faster display
        startAnimations()
        
        // Navigate to MainActivity after delay
        navigateToMain()
    }
    
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
        
        // Make status bar and navigation bar transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Add flags to go full screen
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }
    
    private fun initViews() {
        ivSplashImage = findViewById(R.id.ivSplashImage)
        progressBar = findViewById(R.id.progressBar)
        
        // Start with image visible immediately (no fade)
        ivSplashImage.alpha = 1f
        progressBar.alpha = 0f
    }
    
    private fun startAnimations() {
        // No animation for the image - it's already visible
        // This makes the splash appear instantly
        
        // Progress bar animation - fade in after delay
        progressBar.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(500)
            .start()
    }
    
    private fun navigateToMain() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            
            // No transition animation for instant switch
            overridePendingTransition(0, 0)
            
            finish()
        }, 2500) // Reduced to 2.5 seconds for faster experience
    }
    
    // Extension function to set window translucent
    private fun WindowManager.setWindowIsTranslucent(translucent: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val method = Window::class.java.getDeclaredMethod(
                "setTranslucent",
                Boolean::class.javaPrimitiveType
            )
            method.isAccessible = true
            method.invoke(this, translucent)
        }
    }
}