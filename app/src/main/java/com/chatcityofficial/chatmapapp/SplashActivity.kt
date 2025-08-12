package com.chatcityofficial.chatmapapp

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {
    
    private lateinit var ivLogo: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var tvTagline: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvVersion: TextView
    
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
        ivLogo = findViewById(R.id.ivLogo)
        tvAppName = findViewById(R.id.tvAppName)
        tvTagline = findViewById(R.id.tvTagline)
        progressBar = findViewById(R.id.progressBar)
        tvVersion = findViewById(R.id.tvVersion)
        
        // Initially hide elements for animation
        ivLogo.alpha = 0f
        tvAppName.alpha = 0f
        tvTagline.alpha = 0f
        tvVersion.alpha = 0f
        progressBar.alpha = 0f
        
        ivLogo.scaleX = 0.3f
        ivLogo.scaleY = 0.3f
    }
    
    private fun startAnimations() {
        // Logo animation - scale and fade in
        val logoScaleX = ObjectAnimator.ofFloat(ivLogo, View.SCALE_X, 0.3f, 1f)
        val logoScaleY = ObjectAnimator.ofFloat(ivLogo, View.SCALE_Y, 0.3f, 1f)
        val logoAlpha = ObjectAnimator.ofFloat(ivLogo, View.ALPHA, 0f, 1f)
        
        logoScaleX.interpolator = OvershootInterpolator()
        logoScaleY.interpolator = OvershootInterpolator()
        logoAlpha.interpolator = AccelerateDecelerateInterpolator()
        
        val logoAnimatorSet = AnimatorSet()
        logoAnimatorSet.playTogether(logoScaleX, logoScaleY, logoAlpha)
        logoAnimatorSet.duration = 1000
        logoAnimatorSet.start()
        
        // App name animation - fade in with delay
        tvAppName.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        
        // Tagline animation - fade in with delay
        tvTagline.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        
        // Progress bar animation - fade in
        progressBar.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        
        // Version text animation - fade in
        tvVersion.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(1200)
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
        }, 3000) // 3 second delay to show all animations
    }
}