package com.example.capstonesample.data.model

import com.google.gson.annotations.SerializedName


// ============================================================
// CONVERSATION LIST
// ============================================================

data class ConversationsResponse(
    val success: Boolean,
    val conversations: List<ConversationDto>
)

data class ConversationDto(

    @SerializedName("conversation_id")
    val conversationId: Int,

    @SerializedName("user_id")
    val userId: String?,

    @SerializedName("full_name")
    val fullName: String?,

    val email: String?,

    @SerializedName("last_message")
    val lastMessage: String?,

    @SerializedName("message_type")
    val messageType: String?,

    @SerializedName("last_message_time")
    val lastMessageTime: String?,

    @SerializedName("unread_count")
    val unreadCount: Int = 0
)


// ============================================================
// SEARCH USERS
// ============================================================

data class UserSearchResponse(
    val success: Boolean,
    val users: List<UserSearchDto>
)

data class UserSearchDto(

    val id: String,

    @SerializedName("full_name")
    val fullName: String?,

    val email: String?,

    val role: String? = null
)


// ============================================================
// CREATE / OPEN CONVERSATION
// ============================================================

data class CreateConversationRequest(

    val receiverId: String
)

data class CreateConversationResponse(

    val success: Boolean,

    val message: String? = null,

    val conversationId: Int
)


// ============================================================
// GET MESSAGES
// ============================================================

data class MessagesResponse(

    val success: Boolean,

    val messages: List<MessageDto>
)


// ============================================================
// MESSAGE
// ============================================================

data class MessageDto(

    val id: Int,

    @SerializedName("conversation_id")
    val conversationId: Int,

    @SerializedName("sender_id")
    val senderId: String,

    @SerializedName("sender_name")
    val senderName: String?,

    @SerializedName("message_text")
    val messageText: String?,

    @SerializedName("message_type")
    val messageType: String?,

    @SerializedName("is_read")
    val isRead: Boolean = false,

    @SerializedName("is_mine")
    val isMine: Boolean = false,

    @SerializedName("created_at")
    val createdAt: String?,

    val attachments: List<MessageAttachmentDto> = emptyList()
)


// ============================================================
// MESSAGE ATTACHMENT
// Used for images, PDF, Word files, etc.
// ============================================================

data class MessageAttachmentDto(

    val id: Int,

    val originalName: String?,

    val fileName: String?,

    val filePath: String?,

    val mimeType: String?,

    val fileSize: Long?
)


// ============================================================
// SEND TEXT MESSAGE
// ============================================================

data class SendMessageRequest(

    val conversationId: Int,

    val message: String
)


// ============================================================
// SEND MESSAGE RESPONSE
// ============================================================

data class SendMessageResponse(

    val success: Boolean,

    val message: MessageDto?
)