package com.chatcityofficial.chatmapapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.chatcityofficial.chatmapapp.ui.compose.profile.ChatBubbleActivity
import com.chatcityofficial.chatmapapp.ui.compose.profile.ProfileComposeScreen

class ProfileFragment : Fragment() {
    
    companion object {
        // Shared list of activities that can be updated from HomeFragment
        private val chatBubbleActivities = mutableStateListOf<ChatBubbleActivity>()
        
        fun addChatBubbleView(message: String, category: String? = null, subcategory: String? = null) {
            // Add to beginning of list (most recent first)
            chatBubbleActivities.add(0, ChatBubbleActivity(
                message = message,
                category = category,
                subcategory = subcategory,
                timestamp = System.currentTimeMillis()
            ))
            
            // Keep only last 20 activities
            if (chatBubbleActivities.size > 20) {
                chatBubbleActivities.removeLast()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                // Add sample data for testing
                LaunchedEffect(Unit) {
                    if (chatBubbleActivities.isEmpty()) {
                        // Add sample activities for demonstration
                        chatBubbleActivities.add(ChatBubbleActivity(
                            message = "Homemade pizza with fresh mozzarella",
                            category = "Post",
                            subcategory = "Sarah K.",
                            timestamp = System.currentTimeMillis() - 300000 // 5 minutes ago
                        ))
                        chatBubbleActivities.add(ChatBubbleActivity(
                            message = "Best coffee shop in downtown",
                            category = "Task",
                            subcategory = "Mike D.",
                            timestamp = System.currentTimeMillis() - 3600000 // 1 hour ago
                        ))
                        chatBubbleActivities.add(ChatBubbleActivity(
                            message = "Yoga class at sunset beach",
                            category = "Meet-Up",
                            subcategory = "Emma L.",
                            timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
                        ))
                    }
                }
                
                ProfileComposeScreen(
                    onSettingsClick = {
                        // Handle settings click
                        // You can add navigation or dialog logic here
                    },
                    recentActivities = chatBubbleActivities
                )
            }
        }
    }
    
}