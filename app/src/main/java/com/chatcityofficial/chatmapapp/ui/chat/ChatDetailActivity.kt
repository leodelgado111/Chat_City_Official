package com.chatcityofficial.chatmapapp.ui.chat

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
        
        try {
            binding = ActivityChatDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Get extras with validation
            chatId = intent.getStringExtra("CHAT_ID") ?: run {
                Log.e("ChatDetailActivity", "No CHAT_ID provided")
                Toast.makeText(this, "Error: No chat ID provided", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            chatName = intent.getStringExtra("CHAT_NAME") ?: "Chat"
            deviceId = intent.getStringExtra("DEVICE_ID") ?: Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"
            
            Log.d("ChatDetailActivity", "Opening chat: $chatId ($chatName) with device: $deviceId")
            
            // Initialize repository
            chatRepository = ChatRepository()
            
            // Setup UI
            setupToolbar()
            setupRecyclerView()
            setupMessageInput()
            
            // Get or create user
            lifecycleScope.launch {
                try {
                    val user = chatRepository.getOrCreateUser(
                        deviceId,
                        "User_${deviceId.take(4)}"
                    )
                    userId = user.id
                    userName = user.name
                    
                    Log.d("ChatDetailActivity", "User initialized: $userId ($userName)")
                    
                    // Load messages
                    loadMessages()
                } catch (e: Exception) {
                    Log.e("ChatDetailActivity", "Error initializing user", e)
                    Toast.makeText(
                        this@ChatDetailActivity,
                        "Error initializing chat: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error opening chat: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        try {
            binding.toolbar.title = chatName
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            
            binding.toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Error setting up toolbar", e)
        }
    }

    private fun setupRecyclerView() {
        try {
            messagesAdapter = MessagesAdapter(deviceId)
            
            binding.messagesRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@ChatDetailActivity).apply {
                    stackFromEnd = true
                }
                adapter = messagesAdapter
                // Add performance optimizations
                setHasFixedSize(false)
                itemAnimator = null // Disable animations for smoother scrolling
            }
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Error setting up RecyclerView", e)
            Toast.makeText(this, "Error setting up messages view", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMessageInput() {
        try {
            binding.sendButton.setOnClickListener {
                val messageText = binding.messageInput.text.toString().trim()
                if (messageText.isNotEmpty()) {
                    sendMessage(messageText)
                    binding.messageInput.text?.clear()
                }
            }
            
            // Enable send button only when there's text
            binding.messageInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    binding.sendButton.isEnabled = !s.isNullOrBlank()
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Error setting up message input", e)
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                chatRepository.getMessages(chatId).collectLatest { messages ->
                    Log.d("ChatDetailActivity", "Loaded ${messages.size} messages")
                    messagesAdapter.submitList(messages)
                    
                    // Scroll to bottom when new messages arrive
                    if (messages.isNotEmpty()) {
                        binding.messagesRecyclerView.post {
                            binding.messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatDetailActivity", "Error loading messages", e)
                Toast.makeText(
                    this@ChatDetailActivity,
                    "Error loading messages: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun sendMessage(text: String) {
        // Validate before sending
        if (userId.isEmpty()) {
            Log.e("ChatDetailActivity", "Cannot send message: User ID is empty")
            Toast.makeText(this, "Error: User not initialized", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (chatId.isEmpty()) {
            Log.e("ChatDetailActivity", "Cannot send message: Chat ID is empty")
            Toast.makeText(this, "Error: Chat not initialized", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                Log.d("ChatDetailActivity", "Sending message: $text")
                chatRepository.sendMessage(
                    chatId = chatId,
                    senderId = userId,
                    senderName = userName,
                    text = text
                )
            } catch (e: Exception) {
                Log.e("ChatDetailActivity", "Error sending message", e)
                Toast.makeText(
                    this@ChatDetailActivity,
                    "Error sending message: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}