package com.chatcityofficial.chatmapapp.data

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImage: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false
)