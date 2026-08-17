package com.example.capstonesample.data.repository

import com.example.capstonesample.data.local.UserDao
import com.example.capstonesample.data.local.UserEntity

class UserRepository(
    private val userDao: UserDao
) {

    // ============================================================
    // REGISTER / SAVE LOCAL USER
    // ============================================================

    suspend fun registerUser(
        user: UserEntity
    ): Long {

        return userDao.insertUser(user)
    }


    // ============================================================
    // FIND USER BY EMAIL
    // Used for offline login
    // ============================================================

    suspend fun getUserByEmail(
        email: String
    ): UserEntity? {

        return userDao.getUserByEmail(
            email.trim().lowercase()
        )
    }


    // ============================================================
    // CHECK IF EMAIL ALREADY EXISTS
    // Used during registration
    // ============================================================

    suspend fun userExists(
        email: String
    ): Boolean {

        return userDao.userExists(
            email.trim().lowercase()
        )
    }
}