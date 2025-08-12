package com.chatcityofficial.chatmapapp.ui.chat

import android.os.Bundle
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        // Get data from intent
        recipientId = intent.getStringExtra("RECIPIENT_ID") ?: ""
        recipientName = intent.getStringExtra("RECIPIENT_NAME") ?: "User"
        
        // Generate chat ID (combine user IDs in alphabetical order for consistency)
        val currentUserId = auth.currentUser?.uid ?: "anonymous_${UUID.randomUUID()}"
        chatId = if (currentUserId < recipientId) {
            "${currentUserId}_${recipientId}"
        } else {
            "${recipientId}_${currentUserId}"
        }
        
        setupViews()
        setupRecyclerView()
        setupMessageListener()
    }
    
    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        titleTextView = findViewById(R.id.toolbar_title)
        recyclerView = findViewById(R.id.messages_recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send_button)
        
        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        titleTextView.text = recipientName
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Setup send button
        sendButton.setOnClickListener {
            sendMessage()
        }
    }
    
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages, auth.currentUser?.uid ?: "")
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
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