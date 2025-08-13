package com.chatcityofficial.chatmapapp.data.repository

import com.chatcityofficial.chatmapapp.data.models.Chat
import com.chatcityofficial.chatmapapp.data.models.Message
import com.chatcityofficial.chatmapapp.data.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val chatsCollection = firestore.collection("chats")
    private val messagesCollection = firestore.collection("messages")
    private val usersCollection = firestore.collection("users")

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

    // Get all chats
    fun getAllChats(): Flow<List<Chat>> = callbackFlow {
        val listener = chatsCollection
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)
                } ?: emptyList()

                trySend(chats)
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
                    close(error)
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

        // Update chat's last message
        chatsCollection.document(chatId).update(
            mapOf(
                "lastMessage" to text,
                "lastMessageTime" to message.timestamp
            )
        ).await()
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
                chatsCollection.document(chat.id).set(chat).await()
            } catch (e: Exception) {
                // Ignore if already exists
            }
        }
    }
}