package com.chatcityofficial.chatmapapp.data.repository

import android.util.Log
import com.chatcityofficial.chatmapapp.data.models.Chat
import com.chatcityofficial.chatmapapp.data.models.Message
import com.chatcityofficial.chatmapapp.data.models.User
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    companion object {
        private const val TAG = "ChatRepository"
    }
    
    private val firestore: FirebaseFirestore by lazy {
        try {
            // Ensure Firebase is initialized
            if (FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isEmpty()) {
                FirebaseApp.initializeApp(FirebaseApp.getInstance().applicationContext)
            }
            
            val db = FirebaseFirestore.getInstance()
            
            // Configure Firestore settings for better offline support
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            db.firestoreSettings = settings
            
            Log.d(TAG, "Firestore initialized successfully")
            db
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore", e)
            FirebaseFirestore.getInstance()
        }
    }
    
    private val chatsCollection = firestore.collection("chats")
    private val messagesCollection = firestore.collection("messages")
    private val usersCollection = firestore.collection("users")

    // Get or create user by device ID
    suspend fun getOrCreateUser(deviceId: String, userName: String): User {
        return try {
            // Check if user exists
            val existingUser = usersCollection
                .whereEqualTo("deviceId", deviceId)
                .get()
                .await()
                .documents
                .firstOrNull()

            if (existingUser != null) {
                existingUser.toObject(User::class.java) ?: createNewUser(deviceId, userName)
            } else {
                createNewUser(deviceId, userName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting/creating user: ${e.message}", e)
            // Return a local user object if Firebase fails
            User(
                id = deviceId,
                name = userName,
                deviceId = deviceId,
                email = "$deviceId@chatcity.local"
            )
        }
    }

    private suspend fun createNewUser(deviceId: String, userName: String): User {
        return try {
            val newUser = User(
                id = usersCollection.document().id,
                name = userName,
                deviceId = deviceId,
                email = "$deviceId@chatcity.local"
            )
            usersCollection.document(newUser.id).set(newUser).await()
            Log.d(TAG, "New user created: ${newUser.id}")
            newUser
        } catch (e: Exception) {
            Log.e(TAG, "Error creating new user: ${e.message}", e)
            // Return a local user object if creation fails
            User(
                id = deviceId,
                name = userName,
                deviceId = deviceId,
                email = "$deviceId@chatcity.local"
            )
        }
    }

    // Get test chat room (first chat)
    suspend fun getTestChatRoom(): Chat {
        val testChatId = "test_chat_room_001"
        
        return try {
            val existingChat = chatsCollection.document(testChatId).get().await()
            
            if (existingChat.exists()) {
                existingChat.toObject(Chat::class.java) ?: createTestChat(testChatId)
            } else {
                createTestChat(testChatId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting test chat room: ${e.message}", e)
            // Return a local chat object if Firebase fails
            Chat(
                id = testChatId,
                name = "Test Chat Room",
                lastMessage = "Welcome to the test chat!",
                isGroupChat = true,
                participants = listOf()
            )
        }
    }

    private suspend fun createTestChat(chatId: String): Chat {
        return try {
            val testChat = Chat(
                id = chatId,
                name = "Test Chat Room",
                lastMessage = "Welcome to the test chat!",
                lastMessageTime = System.currentTimeMillis(),
                isGroupChat = true,
                participants = listOf()
            )
            chatsCollection.document(chatId).set(testChat).await()
            Log.d(TAG, "Test chat created: $chatId")
            testChat
        } catch (e: Exception) {
            Log.e(TAG, "Error creating test chat: ${e.message}", e)
            // Return a local chat object if creation fails
            Chat(
                id = chatId,
                name = "Test Chat Room",
                lastMessage = "Welcome to the test chat!",
                isGroupChat = true,
                participants = listOf()
            )
        }
    }

    // Get all chats
    fun getAllChats(): Flow<List<Chat>> = callbackFlow {
        Log.d(TAG, "Starting to listen for chats...")
        
        val listener = try {
            // Try with ordering first
            chatsCollection
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to chats with ordering: ${error.message}", error)
                        
                        // If ordering fails (likely due to missing index), try without ordering
                        chatsCollection.addSnapshotListener { simpleSnapshot, simpleError ->
                            if (simpleError != null) {
                                Log.e(TAG, "Error listening to chats without ordering: ${simpleError.message}", simpleError)
                                // Send empty list instead of closing the flow
                                trySend(emptyList())
                                return@addSnapshotListener
                            }
                            
                            val chats = simpleSnapshot?.documents?.mapNotNull { doc ->
                                try {
                                    doc.toObject(Chat::class.java)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing chat document: ${e.message}", e)
                                    null
                                }
                            } ?: emptyList()
                            
                            Log.d(TAG, "Loaded ${chats.size} chats (without ordering)")
                            trySend(chats)
                        }
                        return@addSnapshotListener
                    }

                    val chats = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Chat::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing chat document: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    Log.d(TAG, "Loaded ${chats.size} chats")
                    trySend(chats)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up chat listener: ${e.message}", e)
            trySend(emptyList())
            null
        }

        awaitClose { 
            listener?.remove()
            Log.d(TAG, "Chat listener removed")
        }
    }

    // Get messages for a chat
    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        Log.d(TAG, "Starting to listen for messages in chat: $chatId")
        
        val listener = try {
            // Try with compound query first
            messagesCollection
                .whereEqualTo("chatId", chatId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to messages with ordering: ${error.message}", error)
                        
                        // If compound query fails, try simple query without ordering
                        messagesCollection
                            .whereEqualTo("chatId", chatId)
                            .addSnapshotListener { simpleSnapshot, simpleError ->
                                if (simpleError != null) {
                                    Log.e(TAG, "Error listening to messages without ordering: ${simpleError.message}", simpleError)
                                    // Send empty list instead of closing the flow
                                    trySend(emptyList())
                                    return@addSnapshotListener
                                }
                                
                                val messages = simpleSnapshot?.documents?.mapNotNull { doc ->
                                    try {
                                        doc.toObject(Message::class.java)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing message document: ${e.message}", e)
                                        null
                                    }
                                }?.sortedBy { it.timestamp } ?: emptyList()
                                
                                Log.d(TAG, "Loaded ${messages.size} messages for chat $chatId (manually sorted)")
                                trySend(messages)
                            }
                        return@addSnapshotListener
                    }

                    val messages = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Message::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing message document: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    Log.d(TAG, "Loaded ${messages.size} messages for chat $chatId")
                    trySend(messages)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up message listener: ${e.message}", e)
            trySend(emptyList())
            null
        }

        awaitClose { 
            listener?.remove()
            Log.d(TAG, "Message listener removed for chat $chatId")
        }
    }

    // Send a message
    suspend fun sendMessage(chatId: String, senderId: String, senderName: String, text: String) {
        try {
            val messageId = messagesCollection.document().id
            val message = Message(
                id = messageId,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            // Add message
            messagesCollection.document(messageId).set(message).await()
            Log.d(TAG, "Message sent successfully: $messageId")

            // Update chat's last message (with error handling)
            try {
                chatsCollection.document(chatId).update(
                    mapOf(
                        "lastMessage" to text,
                        "lastMessageTime" to message.timestamp
                    )
                ).await()
                Log.d(TAG, "Chat $chatId updated with last message")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating chat's last message: ${e.message}", e)
                // Message was sent, so don't throw - just log the error
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            throw e // Re-throw to handle in UI
        }
    }

    // Create sample chats for UI
    suspend fun createSampleChats() {
        val sampleChats = listOf(
            Chat(
                id = "test_chat_room_001",
                name = "Test Chat Room",
                lastMessage = "Welcome to the test chat!",
                lastMessageTime = System.currentTimeMillis(),
                isGroupChat = true
            ),
            Chat(
                id = "spartan_111",
                name = "Spartan 111",
                lastMessage = "I can't remember the last time I went out...",
                lastMessageTime = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000, // 5 days ago
                isGroupChat = false
            ),
            Chat(
                id = "spartan_112",
                name = "Spartan 112",
                lastMessage = "I think I'm camping this weekend...",
                lastMessageTime = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000, // 5 days ago
                isGroupChat = false
            )
        )

        sampleChats.forEach { chat ->
            try {
                // Check if chat already exists
                val existingChat = chatsCollection.document(chat.id).get().await()
                if (!existingChat.exists()) {
                    chatsCollection.document(chat.id).set(chat).await()
                    Log.d(TAG, "Sample chat created: ${chat.id}")
                } else {
                    Log.d(TAG, "Sample chat already exists: ${chat.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating sample chat ${chat.id}: ${e.message}", e)
                // Continue with other chats even if one fails
            }
        }
    }
}