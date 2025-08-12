package com.chatcityofficial.chatmapapp.data

import java.util.Date

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Date = Date()
)