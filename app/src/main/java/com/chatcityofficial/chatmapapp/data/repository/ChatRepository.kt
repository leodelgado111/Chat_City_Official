package com.chatcityofficial.chatmapapp.data.repository

import com.chatcityofficial.chatmapapp.data.models.Chat
import com.chatcityofficial.chatmapapp.data.models.Message
import com.chatcityofficial.chatmapapp.data.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val chatsCollection = firestore.collection("chats")
    private val messagesCollection = firestore.collection("messages")
    private val usersCollection = firestore.collection("users")

    companion object {
        private const val TAG = "ChatRepository"
    }

    // Get or create user by device ID
    suspend fun getOrCreateUser(deviceId: String, userName: String): User {
        try {
            // Check if user exists
            val existingUser = usersCollection
                .whereEqualTo("deviceId", deviceId)
                .get()
                .await()
                .documents
                .firstOrNull()

            return if (existingUser != null) {
                existingUser.toObject(User::class.java) ?: createNewUser(deviceId, userName)
            } else {
                createNewUser(deviceId, userName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting/creating user", e)
            return createNewUser(deviceId, userName)
        }
    }

    private suspend fun createNewUser(deviceId: String, userName: String): User {
        val newUser = User(
            id = usersCollection.document().id,
            name = userName,
            deviceId = deviceId,
            email = "$deviceId@chatcity.local"
        )
        usersCollection.document(newUser.id).set(newUser).await()
        return newUser
    }

    // Get test chat room (first chat)
    suspend fun getTestChatRoom(): Chat {
        val testChatId = "test_chat_room_001"
        
        try {
            val existingChat = chatsCollection.document(testChatId).get().await()
            
            if (existingChat.exists()) {
                return existingChat.toObject(Chat::class.java) ?: createTestChat(testChatId)
            } else {
                return createTestChat(testChatId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting test chat room", e)
            return createTestChat(testChatId)
        }
    }

    private suspend fun createTestChat(chatId: String): Chat {
        val testChat = Chat(
            id = chatId,
            name = "Test Chat Room",
            lastMessage = "Welcome to the test chat!",
            isGroupChat = true,
            participants = listOf()
        )
        chatsCollection.document(chatId).set(testChat).await()
        return testChat
    }

    // Get all chats - ONLY return Test Chat Room
    fun getAllChats(): Flow<List<Chat>> = callbackFlow {
        // First, clean up any Spartan chats if they exist
        try {
            // Delete Spartan 111
            chatsCollection.document("spartan_111").delete()
            // Delete Spartan 112
            chatsCollection.document("spartan_112").delete()
            Log.d(TAG, "Cleaned up Spartan chat threads")
        } catch (e: Exception) {
            Log.d(TAG, "No Spartan threads to clean up or error cleaning: ${e.message}")
        }

        val listener = chatsCollection
            .whereEqualTo("id", "test_chat_room_001") // Only get Test Chat Room
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting chats", error)
                    // Return only Test Chat Room as fallback
                    val testChat = Chat(
                        id = "test_chat_room_001",
                        name = "Test Chat Room",
                        lastMessage = "Welcome to the test chat!",
                        lastMessageTime = System.currentTimeMillis(),
                        isGroupChat = true
                    )
                    trySend(listOf(testChat))
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)
                } ?: emptyList()

                // If no chats found, create and return Test Chat Room
                if (chats.isEmpty()) {
                    val testChat = Chat(
                        id = "test_chat_room_001",
                        name = "Test Chat Room",
                        lastMessage = "Welcome to the test chat!",
                        lastMessageTime = System.currentTimeMillis(),
                        isGroupChat = true
                    )
                    trySend(listOf(testChat))
                } else {
                    trySend(chats)
                }
            }

        awaitClose { listener.remove() }
    }

    // Get messages for a chat
    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = messagesCollection
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Check if the error is FAILED_PRECONDITION (missing index)
                    if (error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                        Log.e(TAG, "Missing index for messages query. Creating fallback query.", error)
                        // Try a simpler query without ordering
                        messagesCollection
                            .whereEqualTo("chatId", chatId)
                            .get()
                            .addOnSuccessListener { documents ->
                                val messages = documents.mapNotNull { doc ->
                                    doc.toObject(Message::class.java)
                                }.sortedBy { it.timestamp }
                                trySend(messages)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Fallback messages query failed", e)
                                trySend(emptyList())
                            }
                    } else {
                        Log.e(TAG, "Error getting messages", error)
                        close(error)
                    }
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
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

            // Update chat's last message
            chatsCollection.document(chatId).update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageTime" to message.timestamp
                )
            ).await()
            Log.d(TAG, "Chat updated with last message")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            throw e
        }
    }

    // Create sample chats for UI - ONLY Test Chat Room now
    suspend fun createSampleChats() {
        // First, try to delete any existing Spartan chats
        try {
            chatsCollection.document("spartan_111").delete().await()
            chatsCollection.document("spartan_112").delete().await()
            Log.d(TAG, "Removed Spartan chat threads")
        } catch (e: Exception) {
            Log.d(TAG, "Could not remove Spartan threads: ${e.message}")
        }

        // Only create Test Chat Room
        val testChat = Chat(
            id = "test_chat_room_001",
            name = "Test Chat Room",
            lastMessage = "Welcome to the test chat!",
            lastMessageTime = System.currentTimeMillis(),
            isGroupChat = true
        )

        try {
            chatsCollection.document(testChat.id).set(testChat).await()
            Log.d(TAG, "Sample chat created: ${testChat.name}")
        } catch (e: Exception) {
            // Ignore if already exists
            Log.d(TAG, "Sample chat already exists or error: ${testChat.name}", e)
        }
    }
}