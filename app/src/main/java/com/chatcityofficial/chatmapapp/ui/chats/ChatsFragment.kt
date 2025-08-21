package com.chatcityofficial.chatmapapp.ui.chats

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chatcityofficial.chatmapapp.data.repository.ChatRepository
import com.chatcityofficial.chatmapapp.databinding.FragmentChatsModernBinding
import com.chatcityofficial.chatmapapp.ui.compose.chat.ChatComposeActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsModernBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatRepository: ChatRepository
    private lateinit var chatsAdapter: ChatsAdapter
    private lateinit var deviceId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsModernBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            // Get device ID
            deviceId = Settings.Secure.getString(
                requireContext().contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"
            
            Log.d("ChatsFragment", "Device ID: $deviceId")
            
            // Initialize repository
            chatRepository = ChatRepository()
            
            // Setup UI
            setupRecyclerView()
            setupClickListeners()
            
            // Load chats
            loadChats()
            
            // Create sample chats on first run
            lifecycleScope.launch {
                try {
                    chatRepository.createSampleChats()
                } catch (e: Exception) {
                    Log.e("ChatsFragment", "Error creating sample chats", e)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatsFragment", "Error in onViewCreated", e)
            // Removed Toast error message - silent failure
        }
    }

    private fun setupRecyclerView() {
        chatsAdapter = ChatsAdapter { chat ->
            try {
                // Validate chat data before opening
                if (chat.id.isNullOrEmpty()) {
                    Log.e("ChatsFragment", "Chat ID is null or empty")
                    // Removed Toast error message - silent failure
                    return@ChatsAdapter
                }
                
                // Check if activity context is available
                val ctx = context ?: run {
                    Log.e("ChatsFragment", "Context is null")
                    return@ChatsAdapter
                }
                
                // Create intent with proper validation
                val intent = Intent(ctx, ChatComposeActivity::class.java).apply {
                    putExtra(ChatComposeActivity.EXTRA_CHAT_ID, chat.id)
                    putExtra(ChatComposeActivity.EXTRA_CHAT_NAME, chat.name ?: "Unknown Chat")
                }
                
                // Try to start activity with error handling and animations
                try {
                    startActivity(intent)
                    // Add slide transition animation
                    requireActivity().overridePendingTransition(
                        com.chatcityofficial.chatmapapp.R.anim.slide_in_right,
                        com.chatcityofficial.chatmapapp.R.anim.slide_out_left
                    )
                } catch (e: Exception) {
                    Log.e("ChatsFragment", "Failed to start ChatComposeActivity", e)
                    // Removed Toast error message - silent failure
                }
                
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error opening chat detail", e)
                // Removed Toast error message - silent failure
            }
        }
        
        binding.rvChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatsAdapter
            // Add item decoration for better UI
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.archiveButton.setOnClickListener {
            // Handle archive functionality - placeholder for now
            Log.d("ChatsFragment", "Archive functionality not yet implemented")
        }
    }

    private fun loadChats() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                chatRepository.getAllChats().collectLatest { chats ->
                    Log.d("ChatsFragment", "Loaded ${chats.size} chats")
                    
                    // Check if chats list is empty
                    if (chats.isEmpty()) {
                        Log.d("ChatsFragment", "No chats available, creating sample chats")
                        // You could show an empty state here
                    }
                    
                    chatsAdapter.submitList(chats)
                }
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error loading chats", e)
                // Removed Toast error message - silent failure
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
