package com.example.capstonesample

import androidx.compose.ui.graphics.Color

data class ChatUser(

    val conversationId: Int,

    val userId: String,

    val name: String,

    val lastMessage: String,

    val time: String,

    val unreadCount: Int = 0,

    val avatarColor: Color =
        Color(0xFFDDEBFF),

    val avatarTextColor: Color =
        Color(0xFF2962CC)
)