package com.chatcityofficial.chatmapapp.ui.chats

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        
        // Get device ID
        deviceId = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ANDROID_ID
        )
        
        // Initialize repository
        chatRepository = ChatRepository()
        
        // Setup UI
        setupRecyclerView()
        setupClickListeners()
        
        // Load chats
        loadChats()
        
        // Create sample chats on first run
        lifecycleScope.launch {
            chatRepository.createSampleChats()
        }
    }

    private fun setupRecyclerView() {
        chatsAdapter = ChatsAdapter { chat ->
            // Open chat detail
            val intent = Intent(requireContext(), ChatDetailActivity::class.java).apply {
                putExtra("CHAT_ID", chat.id)
                putExtra("CHAT_NAME", chat.name)
                putExtra("DEVICE_ID", deviceId)
            }
            startActivity(intent)
        }
        
        binding.chatsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            activity?.onBackPressed()
        }
        
        binding.deleteButton.setOnClickListener {
            // Handle delete if needed
        }
    }

    private fun loadChats() {
        viewLifecycleOwner.lifecycleScope.launch {
            chatRepository.getAllChats().collectLatest { chats ->
                chatsAdapter.submitList(chats)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}