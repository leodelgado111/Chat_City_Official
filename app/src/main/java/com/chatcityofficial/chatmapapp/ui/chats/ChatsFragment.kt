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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // IMPORTANT: Explicitly disable options menu to prevent three dots from appearing
        setHasOptionsMenu(false)
    }

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
            // Get device ID with better null safety
            deviceId = try {
                Settings.Secure.getString(
                    requireContext().contentResolver,
                    Settings.Secure.ANDROID_ID
                ) ?: generateFallbackDeviceId()
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error getting device ID", e)
                generateFallbackDeviceId()
            }
            
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
            Toast.makeText(context, "Error initializing chats", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateFallbackDeviceId(): String {
        // Generate a fallback device ID if we can't get the real one
        return "device_${System.currentTimeMillis()}"
    }

    private fun setupRecyclerView() {
        chatsAdapter = ChatsAdapter { chat ->
            try {
                // Validate chat data before opening detail
                if (chat.id.isNullOrEmpty()) {
                    Log.e("ChatsFragment", "Chat ID is null or empty")
                    Toast.makeText(context, "Invalid chat data", Toast.LENGTH_SHORT).show()
                    return@ChatsAdapter
                }
                
                // Check if activity context is available
                val activityContext = activity
                if (activityContext == null) {
                    Log.e("ChatsFragment", "Activity context is null")
                    Toast.makeText(context, "Cannot open chat at this time", Toast.LENGTH_SHORT).show()
                    return@ChatsAdapter
                }
                
                // Create intent with better error handling
                val intent = Intent(activityContext, ChatDetailActivity::class.java).apply {
                    putExtra("CHAT_ID", chat.id)
                    putExtra("CHAT_NAME", chat.name ?: "Unknown Chat")
                    putExtra("DEVICE_ID", deviceId)
                    // Remove FLAG_ACTIVITY_NEW_TASK as it can cause issues with navigation
                    // addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                // Start activity with error handling
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("ChatsFragment", "Error starting ChatDetailActivity", e)
                    Toast.makeText(context, "Error opening chat: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error opening chat detail", e)
                Toast.makeText(context, "Error opening chat: ${e.message}", Toast.LENGTH_SHORT).show()
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
                // Use more modern navigation approach
                activity?.onBackPressedDispatcher?.onBackPressed()
                    ?: activity?.onBackPressed()
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error handling back press", e)
                // Try to finish the activity as a fallback
                try {
                    activity?.finish()
                } catch (ex: Exception) {
                    Log.e("ChatsFragment", "Error finishing activity", ex)
                }
            }
        }
        
        binding.deleteButton.setOnClickListener {
            // Handle delete if needed
            // For now, we'll just show a toast
            Toast.makeText(context, "Delete functionality not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadChats() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                chatRepository.getAllChats().collectLatest { chats ->
                    if (isAdded && _binding != null) {
                        chatsAdapter.submitList(chats)
                        Log.d("ChatsFragment", "Loaded ${chats.size} chats")
                        
                        // Show empty state if no chats
                        if (chats.isEmpty()) {
                            // You might want to show an empty state view here
                            Log.d("ChatsFragment", "No chats available")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error loading chats", e)
                if (isAdded) {
                    Toast.makeText(context, "Error loading chats: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up binding reference to avoid memory leaks
        _binding = null
    }
}