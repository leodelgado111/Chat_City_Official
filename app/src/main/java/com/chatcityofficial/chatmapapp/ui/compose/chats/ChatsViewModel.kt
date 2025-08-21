package com.chatcityofficial.chatmapapp.ui.compose.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatcityofficial.chatmapapp.data.models.Chat
import com.chatcityofficial.chatmapapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatsViewModel : ViewModel() {
    private val repository = ChatRepository()
    
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        // Create sample chats on initialization
        viewModelScope.launch {
            try {
                repository.createSampleChats()
            } catch (e: Exception) {
                // Ignore errors for sample chat creation
            }
        }
    }
    
    fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllChats().collect { chatsList ->
                _chats.value = chatsList
                _isLoading.value = false
            }
        }
    }
}