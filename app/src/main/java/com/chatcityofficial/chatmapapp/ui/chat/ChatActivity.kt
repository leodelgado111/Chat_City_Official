package com.chatcityofficial.chatmapapp.ui.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatcityofficial.chatmapapp.R
import com.chatcityofficial.chatmapapp.data.Message
import com.chatcityofficial.chatmapapp.data.MessageAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date
import java.util.UUID

class ChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var toolbar: Toolbar
    private lateinit var titleTextView: TextView
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val messages = mutableListOf<Message>()
    
    private var chatId: String = ""
    private var recipientId: String = ""
    private var recipientName: String = ""
    private var isDemo: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_modern)
        
        // Get data from intent
        recipientId = intent.getStringExtra("RECIPIENT_ID") ?: ""
        recipientName = intent.getStringExtra("RECIPIENT_NAME") ?: "User"
        isDemo = intent.getBooleanExtra("IS_DEMO", false)
        
        // Generate chat ID (combine user IDs in alphabetical order for consistency)
        val currentUserId = auth.currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"
        chatId = if (currentUserId < recipientId) {
            "${currentUserId}_${recipientId}"
        } else {
            "${recipientId}_${currentUserId}"
        }
        
        setupViews()
        setupRecyclerView()
        
        if (isDemo) {
            // For demo user, show initial message
            showDemoWelcomeMessage()
        } else {
            // For real users, setup Firebase listener
            setupMessageListener()
        }
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.rvChatMessages)
        messageInput = findViewById(R.id.etMessage)
        sendButton = findViewById(R.id.btnSend)
        titleTextView = findViewById(R.id.tvChatTitle)
        
        // Setup title
        titleTextView.text = recipientName
        
        // Setup back button
        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            finish()
        }
        
        // Setup send button
        sendButton.setOnClickListener {
            if (isDemo) {
                sendDemoMessage()
            } else {
                sendMessage()
            }
        }
    }
    
    private fun setupRecyclerView() {
        val currentUserId = auth.currentUser?.uid ?: "anonymous_user"
        messageAdapter = MessageAdapter(messages, currentUserId)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }
    
    private fun showDemoWelcomeMessage() {
        // Add initial welcome message from the assistant
        val welcomeMessage = Message(
            id = UUID.randomUUID().toString(),
            senderId = "demo_user_123",
            senderName = "Chat City Assistant",
            text = "What's on your mind? 🤔",
            timestamp = Date()
        )
        
        messages.add(welcomeMessage)
        messageAdapter.notifyDataSetChanged()
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    private fun sendDemoMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return
        
        val currentUserId = auth.currentUser?.uid ?: "anonymous_user"
        val currentUserName = auth.currentUser?.displayName ?: "You"
        
        // Add user's message
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            senderId = currentUserId,
            senderName = currentUserName,
            text = text,
            timestamp = Date()
        )
        
        messages.add(userMessage)
        messageAdapter.notifyDataSetChanged()
        recyclerView.scrollToPosition(messages.size - 1)
        messageInput.text.clear()
        
        // Simulate assistant typing and responding
        Handler(Looper.getMainLooper()).postDelayed({
            val responses = listOf(
                "That's interesting! Tell me more about it.",
                "I understand how you feel. What happened next?",
                "That sounds amazing! How did that make you feel?",
                "Wow, that's quite an experience! What did you learn from it?",
                "I'm here to listen. Please continue sharing your thoughts.",
                "That's a great perspective! Have you considered other viewpoints?",
                "Thanks for sharing that with me. How can I help you today?"
            )
            
            val randomResponse = responses.random()
            
            val assistantMessage = Message(
                id = UUID.randomUUID().toString(),
                senderId = "demo_user_123",
                senderName = "Chat City Assistant",
                text = randomResponse,
                timestamp = Date()
            )
            
            messages.add(assistantMessage)
            messageAdapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(messages.size - 1)
        }, 1500) // Delay to simulate typing
    }
    
    private fun setupMessageListener() {
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                
                snapshot?.let {
                    messages.clear()
                    for (doc in it.documents) {
                        val message = Message(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "Unknown",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getDate("timestamp") ?: Date()
                        )
                        messages.add(message)
                    }
                    messageAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }
    
    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return
        
        val currentUser = auth.currentUser
        val senderId = currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"
        val senderName = currentUser?.displayName ?: "Anonymous"
        
        val message = hashMapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to text,
            "timestamp" to Date()
        )
        
        // Save message to Firestore
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                messageInput.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        
        // Update chat metadata
        val chatMetadata = hashMapOf(
            "lastMessage" to text,
            "lastMessageTime" to Date(),
            "participants" to listOf(senderId, recipientId)
        )
        
        firestore.collection("chats")
            .document(chatId)
            .set(chatMetadata)
    }
}