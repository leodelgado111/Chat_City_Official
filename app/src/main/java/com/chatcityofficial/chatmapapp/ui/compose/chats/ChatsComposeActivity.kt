package com.chatcityofficial.chatmapapp.ui.compose.chats

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.chatcityofficial.chatmapapp.R
import com.chatcityofficial.chatmapapp.ui.compose.chat.ChatComposeActivity
import com.chatcityofficial.chatmapapp.ui.compose.theme.ChatCityTheme

class ChatsComposeActivity : ComponentActivity() {
    
    companion object {
        private const val CHAT_ACTIVITY_REQUEST_CODE = 2001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
        
        setContent {
            ChatCityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0D0D)
                ) {
                    ChatsComposeScreen(
                        onChatClick = { chat ->
                            val intent = Intent(this, ChatComposeActivity::class.java).apply {
                                putExtra(ChatComposeActivity.EXTRA_CHAT_ID, chat.id)
                                putExtra(ChatComposeActivity.EXTRA_CHAT_NAME, chat.name)
                            }
                            startActivityForResult(intent, CHAT_ACTIVITY_REQUEST_CODE)
                            overridePendingTransition(
                                R.anim.fragment_slide_in_right,
                                R.anim.fragment_slide_out_left
                            )
                        },
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
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == CHAT_ACTIVITY_REQUEST_CODE) {
            // When returning from ChatComposeActivity, just ensure we'll pass the result back
            // The back button press will trigger setResult(RESULT_OK) and finish
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure proper visibility when returning from ChatComposeActivity
        window.decorView.requestLayout()
    }
}