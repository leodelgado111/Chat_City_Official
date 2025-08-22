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
import com.chatcityofficial.chatmapapp.ui.compose.navigation.BottomNavigationBar
import com.chatcityofficial.chatmapapp.ui.compose.navigation.NavigationTab
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.dp

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
        
        // Ensure MainActivity arrow buttons are hidden when this activity is visible
        // This is a failsafe in case MainActivity didn't hide them properly
        hideMainActivityArrowButtons()
        
        setContent {
            ChatCityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0D0D)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Main content with padding for navigation bar
                        ChatsComposeScreen(
                            modifier = Modifier.padding(bottom = 68.dp), // Add bottom padding for nav bar
                            onChatClick = { chat ->
                                val intent = Intent(this@ChatsComposeActivity, ChatComposeActivity::class.java).apply {
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
                        
                        // Bottom Navigation Bar
                        Box(
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.BottomCenter)
                                .navigationBarsPadding() // Use system navigation bar padding
                                .padding(bottom = 12.dp) // Additional padding (same as MainActivity)
                        ) {
                            BottomNavigationBar(
                                selectedTab = NavigationTab.CHATS,
                                onTabSelected = { tab ->
                                    when (tab) {
                                        NavigationTab.SAVED, NavigationTab.HOME, NavigationTab.PROFILE -> {
                                            // Return to MainActivity with the selected tab
                                            setResult(RESULT_OK)
                                            finish()
                                            overridePendingTransition(
                                                R.anim.fragment_slide_in_left,
                                                R.anim.fragment_slide_out_right
                                            )
                                        }
                                        NavigationTab.CREATE -> {
                                            // Do nothing for CREATE
                                        }
                                        NavigationTab.CHATS -> {
                                            // Already on CHATS, do nothing
                                        }
                                    }
                                }
                            )
                        }
                    }
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
        
        // Re-hide arrow buttons in case MainActivity brought them back
        hideMainActivityArrowButtons()
    }
    
    private fun hideMainActivityArrowButtons() {
        try {
            // Try to find and hide MainActivity arrow buttons if they exist
            // These buttons are part of MainActivity's layout and might appear on top of this activity
            val context = this as? android.app.Activity
            context?.let {
                // Use reflection or direct access if possible
                // For now, this is a placeholder - the real solution is that MainActivity should handle this
                // when launching this activity
            }
        } catch (e: Exception) {
            // Silent failure - MainActivity should handle hiding buttons properly
        }
    }
}