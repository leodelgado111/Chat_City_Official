package com.chatcityofficial.chatmapapp.ui.chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chatcityofficial.chatmapapp.data.repository.ChatRepository
import com.chatcityofficial.chatmapapp.databinding.ActivityChatDetailBinding
import kotlinx.coroutines.CancellationException
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
            
            // Set dark status bar to match app theme
            window.statusBarColor = android.graphics.Color.parseColor("#0D0D0D")
            
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
                    
                    // Reinitialize adapter with correct userId for proper message alignment
                    messagesAdapter = MessagesAdapter(userId)
                    binding.messagesRecyclerView.adapter = messagesAdapter
                    
                    // Load messages
                    loadMessages()
                } catch (e: Exception) {
                    // Handle cancellation separately from real errors
                    if (e is CancellationException) {
                        Log.d("ChatDetailActivity", "User initialization cancelled")
                    } else {
                        Log.e("ChatDetailActivity", "Error initializing user", e)
                        Toast.makeText(
                            this@ChatDetailActivity,
                            "Error initializing chat: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    
                    // Fallback: use deviceId as userId and set up adapter
                    userId = deviceId
                    userName = "User_${deviceId.take(4)}"
                    messagesAdapter = MessagesAdapter(userId)
                    binding.messagesRecyclerView.adapter = messagesAdapter
                    loadMessages()
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                Log.d("ChatDetailActivity", "onCreate cancelled")
            } else {
                Log.e("ChatDetailActivity", "Error in onCreate", e)
                Toast.makeText(this, "Error opening chat: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
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
            // Initialize with deviceId for now, will update after user initialization
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
                    // Start the morphing animation
                    animateMessageSend(messageText) {
                        // After animation, actually send the message
                        sendMessage(messageText)
                        binding.messageInput.text?.clear()
                    }
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
                    // Create a new list instance to ensure DiffUtil detects changes
                    val messageList = messages.toList()
                    messagesAdapter.submitList(messageList) {
                        // Scroll to bottom after list is updated
                        if (messageList.isNotEmpty()) {
                            binding.messagesRecyclerView.post {
                                binding.messagesRecyclerView.smoothScrollToPosition(messageList.size - 1)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Don't show error for job cancellation (normal when exiting chat)
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("ChatDetailActivity", "Messages flow cancelled (normal when exiting chat)")
                } else {
                    Log.e("ChatDetailActivity", "Error loading messages", e)
                    Toast.makeText(
                        this@ChatDetailActivity,
                        "Error loading messages: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun animateMessageSend(messageText: String, onAnimationComplete: () -> Unit) {
        // Set up the morphing text view
        binding.morphingText.apply {
            text = messageText
            visibility = View.VISIBLE
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
        }
        
        // Get the input position
        val inputLocation = IntArray(2)
        binding.messageInput.getLocationOnScreen(inputLocation)
        
        // Position the morphing text at the input location initially
        binding.morphingText.x = inputLocation[0].toFloat()
        binding.morphingText.y = inputLocation[1].toFloat() - binding.root.top
        
        // Calculate target position (right side of the screen for sent messages)
        val screenWidth = binding.root.width
        val targetX = screenWidth - binding.morphingText.width - 32f // 32dp margin
        
        // Get the RecyclerView's bottom to position the message there
        val recyclerViewLocation = IntArray(2)
        binding.messagesRecyclerView.getLocationOnScreen(recyclerViewLocation)
        val targetY = recyclerViewLocation[1] + binding.messagesRecyclerView.height - binding.morphingText.height - 100f
        
        // Create animation set
        val animatorSet = AnimatorSet()
        
        // Create movement animations
        val moveX = ObjectAnimator.ofFloat(binding.morphingText, "x", binding.morphingText.x, targetX)
        val moveY = ObjectAnimator.ofFloat(binding.morphingText, "y", binding.morphingText.y, targetY)
        
        // Create scale animation (slight scale up then down for bouncy effect)
        val scaleUpX = ObjectAnimator.ofFloat(binding.morphingText, "scaleX", 1f, 1.1f)
        val scaleUpY = ObjectAnimator.ofFloat(binding.morphingText, "scaleY", 1f, 1.1f)
        val scaleDownX = ObjectAnimator.ofFloat(binding.morphingText, "scaleX", 1.1f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(binding.morphingText, "scaleY", 1.1f, 1f)
        
        // Create fade out animation
        val fadeOut = ObjectAnimator.ofFloat(binding.morphingText, "alpha", 1f, 0f)
        
        // Set up timing - faster for snappier feel
        val moveDuration = 300L
        val scaleDuration = 150L
        val fadeDuration = 100L
        
        // Configure animations
        moveX.duration = moveDuration
        moveY.duration = moveDuration
        scaleUpX.duration = scaleDuration
        scaleUpY.duration = scaleDuration
        scaleDownX.duration = scaleDuration
        scaleDownY.duration = scaleDuration
        fadeOut.duration = fadeDuration
        
        // Set interpolators for smooth motion
        moveX.interpolator = android.view.animation.DecelerateInterpolator()
        moveY.interpolator = android.view.animation.DecelerateInterpolator()
        
        // Create scale up sequence
        val scaleUp = AnimatorSet()
        scaleUp.playTogether(scaleUpX, scaleUpY)
        
        // Create scale down sequence
        val scaleDown = AnimatorSet()
        scaleDown.playTogether(scaleDownX, scaleDownY)
        
        // Create movement sequence
        val movement = AnimatorSet()
        movement.playTogether(moveX, moveY)
        
        // Play animations in sequence
        animatorSet.apply {
            play(movement).with(scaleUp)
            play(scaleDown).after(scaleUp)
            play(fadeOut).after(scaleDown)
            
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    // Disable input during animation
                    binding.messageInput.isEnabled = false
                    binding.sendButton.isEnabled = false
                }
                
                override fun onAnimationEnd(animation: Animator) {
                    // Hide the morphing text and re-enable input
                    binding.morphingText.visibility = View.GONE
                    binding.messageInput.isEnabled = true
                    binding.sendButton.isEnabled = true
                    
                    // Trigger the actual message send
                    onAnimationComplete()
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    binding.morphingText.visibility = View.GONE
                    binding.messageInput.isEnabled = true
                    binding.sendButton.isEnabled = true
                }
                
                override fun onAnimationRepeat(animation: Animator) {}
            })
            
            start()
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
        // Add slide back animation
        overridePendingTransition(
            com.chatcityofficial.chatmapapp.R.anim.slide_in_left,
            com.chatcityofficial.chatmapapp.R.anim.slide_out_right
        )
    }
    
    override fun finish() {
        super.finish()
        // Ensure animation plays when activity finishes
        overridePendingTransition(
            com.chatcityofficial.chatmapapp.R.anim.slide_in_left,
            com.chatcityofficial.chatmapapp.R.anim.slide_out_right
        )
    }
}