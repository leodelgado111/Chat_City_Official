package com.chatcityofficial.chatmapapp.ui.compose.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatcityofficial.chatmapapp.data.models.Message
import com.chatcityofficial.chatmapapp.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""
    
    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getMessages(chatId).collect { messagesList ->
                _messages.value = messagesList
                _isLoading.value = false
            }
        }
    }
    
    fun sendMessage(chatId: String) {
        val text = _messageText.value.trim()
        if (text.isNotEmpty()) {
            viewModelScope.launch {
                val userId = currentUserId
                val userName = auth.currentUser?.displayName ?: "User"
                
                repository.sendMessage(chatId, userId, userName, text)
                _messageText.value = ""
            }
        }
    }
    
    fun updateMessageText(text: String) {
        _messageText.value = text
    }
}