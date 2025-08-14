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
            
            // Get extras with better error handling
            chatId = intent.getStringExtra("CHAT_ID") ?: run {
                Log.e("ChatDetailActivity", "No CHAT_ID provided")
                Toast.makeText(this, "Error: No chat ID provided", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            chatName = intent.getStringExtra("CHAT_NAME") ?: "Chat"
            
            // Get device ID with fallback
            deviceId = intent.getStringExtra("DEVICE_ID") ?: try {
                Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ANDROID_ID
                ) ?: generateFallbackDeviceId()
            } catch (e: Exception) {
                Log.e("ChatDetailActivity", "Error getting device ID", e)
                generateFallbackDeviceId()
            }
            
            Log.d("ChatDetailActivity", "Chat ID: $chatId, Name: $chatName, Device ID: $deviceId")
            
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
                    
                    Log.d("ChatDetailActivity", "User ID: $userId, Name: $userName")
                    
                    // Load messages after user is ready
                    loadMessages()
                } catch (e: Exception) {
                    Log.e("ChatDetailActivity", "Error getting/creating user", e)
                    Toast.makeText(
                        this@ChatDetailActivity,
                        "Error initializing user: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Still try to load messages even if user creation fails
                    userId = deviceId
                    userName = "Anonymous"
                    loadMessages()
                }
            }
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Fatal error in onCreate", e)
            Toast.makeText(this, "Error opening chat: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun generateFallbackDeviceId(): String {
        // Generate a fallback device ID if we can't get the real one
        return "device_${System.currentTimeMillis()}"
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
            // If toolbar setup fails, still allow the activity to function
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
                setHasFixedSize(false) // Changed to false for dynamic content
            }
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Error setting up RecyclerView", e)
            Toast.makeText(this, "Error setting up messages view", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMessageInput() {
        try {
            binding.sendButton.setOnClickListener {
                val messageText = binding.messageInput.text?.toString()?.trim() ?: ""
                if (messageText.isNotEmpty()) {
                    sendMessage(messageText)
                    binding.messageInput.text?.clear()
                } else {
                    Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Enable/disable send button based on input
            binding.messageInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    binding.sendButton.isEnabled = !s.isNullOrBlank()
                }
            })
            
            // Initially disable send button
            binding.sendButton.isEnabled = false
            
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Error setting up message input", e)
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                chatRepository.getMessages(chatId).collectLatest { messages ->
                    if (!isFinishing && !isDestroyed) {
                        messagesAdapter.submitList(messages)
                        
                        // Scroll to bottom when new messages arrive
                        if (messages.isNotEmpty()) {
                            binding.messagesRecyclerView.post {
                                binding.messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                            }
                        }
                        
                        Log.d("ChatDetailActivity", "Loaded ${messages.size} messages")
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatDetailActivity", "Error loading messages", e)
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(
                        this@ChatDetailActivity,
                        "Error loading messages: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        // Validate before sending
        if (userId.isEmpty() || chatId.isEmpty()) {
            Log.e("ChatDetailActivity", "Cannot send message: userId=$userId, chatId=$chatId")
            Toast.makeText(this, "Error: Cannot send message at this time", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                chatRepository.sendMessage(
                    chatId = chatId,
                    senderId = userId,
                    senderName = userName,
                    text = text
                )
                Log.d("ChatDetailActivity", "Message sent successfully")
            } catch (e: Exception) {
                Log.e("ChatDetailActivity", "Error sending message", e)
                Toast.makeText(
                    this@ChatDetailActivity,
                    "Error sending message: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}