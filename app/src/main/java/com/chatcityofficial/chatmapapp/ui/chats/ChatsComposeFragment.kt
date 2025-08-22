package com.chatcityofficial.chatmapapp.ui.chats

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.chatcityofficial.chatmapapp.R
import com.chatcityofficial.chatmapapp.ui.compose.chat.ChatComposeActivity
import com.chatcityofficial.chatmapapp.ui.compose.chats.ChatsComposeScreen
import com.chatcityofficial.chatmapapp.ui.compose.theme.ChatCityTheme

class ChatsComposeFragment : Fragment() {
    
    companion object {
        private const val CHAT_ACTIVITY_REQUEST_CODE = 2001
        
        fun newInstance() = ChatsComposeFragment()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ChatCityTheme {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0D0D0D)),
                        color = Color(0xFF0D0D0D)
                    ) {
                        ChatsComposeScreen(
                            onChatClick = { chat ->
                                val intent = Intent(requireContext(), ChatComposeActivity::class.java).apply {
                                    putExtra(ChatComposeActivity.EXTRA_CHAT_ID, chat.id)
                                    putExtra(ChatComposeActivity.EXTRA_CHAT_NAME, chat.name)
                                }
                                startActivityForResult(intent, CHAT_ACTIVITY_REQUEST_CODE)
                                activity?.overridePendingTransition(
                                    R.anim.fragment_slide_in_right,
                                    R.anim.fragment_slide_out_left
                                )
                            },
                            onBackClick = {
                                // Navigate back to home
                                activity?.onBackPressed()
                            }
                        )
                    }
                }
            }
        }
    }
    
}