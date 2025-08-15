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
import com.chatcityofficial.chatmapapp.databinding.FragmentChatsBinding
import com.chatcityofficial.chatmapapp.ui.chat.ChatDetailActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatRepository: ChatRepository
    private lateinit var chatsAdapter: ChatsAdapter
    private lateinit var deviceId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
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
                val intent = Intent(ctx, ChatDetailActivity::class.java).apply {
                    putExtra("CHAT_ID", chat.id)
                    putExtra("CHAT_NAME", chat.name ?: "Unknown Chat")
                    putExtra("DEVICE_ID", deviceId)
                }
                
                // Try to start activity with error handling
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("ChatsFragment", "Failed to start ChatDetailActivity", e)
                    // Removed Toast error message - silent failure
                }
                
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error opening chat detail", e)
                // Removed Toast error message - silent failure
            }
        }
        
        binding.chatsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatsAdapter
            // Add item decoration for better UI
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            try {
                // Use requireActivity() instead of activity?. for safer navigation
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error handling back press", e)
                // Try alternative navigation
                try {
                    requireActivity().finish()
                } catch (ex: Exception) {
                    Log.e("ChatsFragment", "Failed to finish activity", ex)
                }
            }
        }
        
        binding.deleteButton.setOnClickListener {
            // Handle delete if needed - removed Toast message for coming soon
            Log.d("ChatsFragment", "Delete functionality not yet implemented")
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
