package com.chatcityofficial.chatmapapp.ui.compose.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.chatcityofficial.chatmapapp.R
import com.chatcityofficial.chatmapapp.ui.compose.theme.ChatCityTheme

class ChatComposeActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_CHAT_NAME = "chat_name"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge and proper keyboard handling
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Make the design reach edge-to-edge with fully transparent navigation bar
        window.apply {
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
            
            // For API 29+ use setDecorFitsSystemWindows
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
                // Make navigation bar fully transparent with light navigation bar icons if needed
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    isNavigationBarContrastEnforced = false
                }
            } else {
                decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR  // Light nav bar icons for dark background
                )
            }
            
            // Add flags to draw behind system bars
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: ""
        val chatName = intent.getStringExtra(EXTRA_CHAT_NAME) ?: "Chat"
        
        setContent {
            ChatCityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0D0D)
                ) {
                    ChatComposeScreen(
                        chatId = chatId,
                        chatName = chatName,
                        onBackClick = {
                            setResult(RESULT_OK)
                            finish()
                            overridePendingTransition(
                                R.anim.fragment_slide_in_left,
                                R.anim.fragment_slide_out_right
                            )
                        }
                    )
                }
            }
        }
    }
    
    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
        overridePendingTransition(
            R.anim.fragment_slide_in_left,
            R.anim.fragment_slide_out_right
        )
    }
}