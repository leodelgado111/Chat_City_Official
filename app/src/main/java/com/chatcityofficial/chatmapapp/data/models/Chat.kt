package com.chatcityofficial.chatmapapp.data.models

data class Chat(
    val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val participants: List<String> = emptyList(),
    val avatarUrl: String? = null,
    val isGroupChat: Boolean = false
)