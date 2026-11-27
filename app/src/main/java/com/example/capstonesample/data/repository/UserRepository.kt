package com.example.capstonesample.data.repository

import com.example.capstonesample.data.local.UserDao
import com.example.capstonesample.data.local.UserEntity

class UserRepository(
    private val userDao: UserDao
) {

    suspend fun registerUser(
        user: UserEntity
    ): Long {

        return userDao.insertUser(user)
    }


    // Used for offline login
    suspend fun getUserByEmail(
        email: String
    ): UserEntity? {

        return userDao.getUserByEmail(
            email.trim().lowercase()
        )
    }


    // Used during registration
    suspend fun userExists(
        email: String
    ): Boolean {

        return userDao.userExists(
            email.trim().lowercase()
        )
    }
}