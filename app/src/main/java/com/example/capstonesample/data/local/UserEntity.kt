package com.example.capstonesample.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(

    // Local Room ID
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val fullName: String,

    val email: String,

    val passwordHash: String,

    val role: String = "Engineer",

    val phone: String? = null,

    val isSynced: Boolean = false,

    // Backend PostgreSQL UUID
    val serverUserId: String? = null
)