package com.chatcityofficial.chatmapapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set the theme before setting content view
        setTheme(R.style.Theme_ChatCityOfficial_NoActionBar)
        setContentView(R.layout.activity_splash)

        // Hide system UI for immersive experience
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val logoImageView = findViewById<ImageView>(R.id.ivSplashImage)

        // Create fade-in and scale animation
        val animationSet = AnimationSet(true).apply {
            // Fade in animation
            val fadeIn = AlphaAnimation(0.3f, 1.0f).apply {
                duration = 800
                fillAfter = true
            }
            
            // Scale animation
            val scale = ScaleAnimation(
                0.8f, 1.0f,
                0.8f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 800
                fillAfter = true
            }
            
            addAnimation(fadeIn)
            addAnimation(scale)
        }

        // Start animation
        logoImageView.startAnimation(animationSet)

        // Navigate to MainActivity after a shorter delay
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            
            // Use a fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1200) // Reduced from default to 1.2 seconds
    }

    override fun onBackPressed() {
        // Prevent going back from splash screen
        // Do nothing
    }
}