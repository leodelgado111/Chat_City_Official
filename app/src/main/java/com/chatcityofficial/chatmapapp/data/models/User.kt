package com.chatcityofficial.chatmapapp.data.models

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val deviceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)