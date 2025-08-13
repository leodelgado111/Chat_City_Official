package com.chatcityofficial.chatmapapp.ui.chat

import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chatcityofficial.chatmapapp.data.repository.ChatRepository
import com.chatcityofficial.chatmapapp.databinding.ActivityChatDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private lateinit var chatRepository: ChatRepository
    private lateinit var messagesAdapter: MessagesAdapter
    
    private var chatId: String = ""
    private var chatName: String = ""
    private var deviceId: String = ""
    private var userId: String = ""
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get extras
        chatId = intent.getStringExtra("CHAT_ID") ?: ""
        chatName = intent.getStringExtra("CHAT_NAME") ?: "Chat"
        deviceId = intent.getStringExtra("DEVICE_ID") ?: Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )
        
        // Initialize repository
        chatRepository = ChatRepository()
        
        // Setup UI
        setupToolbar()
        setupRecyclerView()
        setupMessageInput()
        
        // Get or create user
        lifecycleScope.launch {
            val user = chatRepository.getOrCreateUser(
                deviceId,
                "User_${deviceId.take(4)}"
            )
            userId = user.id
            userName = user.name
            
            // Load messages
            loadMessages()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = chatName
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(deviceId)
        
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatDetailActivity).apply {
                stackFromEnd = true
            }
            adapter = messagesAdapter
        }
    }

    private fun setupMessageInput() {
        binding.sendButton.setOnClickListener {
            val messageText = binding.messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.messageInput.text?.clear()
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            chatRepository.getMessages(chatId).collectLatest { messages ->
                messagesAdapter.submitList(messages)
                // Scroll to bottom when new messages arrive
                if (messages.isNotEmpty()) {
                    binding.messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        lifecycleScope.launch {
            chatRepository.sendMessage(
                chatId = chatId,
                senderId = userId,
                senderName = userName,
                text = text
            )
        }
    }
}