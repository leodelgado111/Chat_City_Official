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
            Toast.makeText(context, "Error initializing chats", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        chatsAdapter = ChatsAdapter { chat ->
            try {
                // Open chat detail with null checks
                val intent = Intent(requireContext(), ChatDetailActivity::class.java).apply {
                    putExtra("CHAT_ID", chat.id ?: "")
                    putExtra("CHAT_NAME", chat.name ?: "Unknown")
                    putExtra("DEVICE_ID", deviceId)
                    // Add flags to prevent crashes
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error opening chat detail", e)
                Toast.makeText(context, "Error opening chat", Toast.LENGTH_SHORT).show()
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
                activity?.onBackPressed()
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error handling back press", e)
            }
        }
        
        binding.deleteButton.setOnClickListener {
            // Handle delete if needed
        }
    }

    private fun loadChats() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                chatRepository.getAllChats().collectLatest { chats ->
                    chatsAdapter.submitList(chats)
                    Log.d("ChatsFragment", "Loaded ${chats.size} chats")
                }
            } catch (e: Exception) {
                Log.e("ChatsFragment", "Error loading chats", e)
                Toast.makeText(context, "Error loading chats", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}