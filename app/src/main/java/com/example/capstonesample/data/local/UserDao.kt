package com.example.capstonesample.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(
        user: UserEntity
    ): Long


    @Query(
        "SELECT * FROM users WHERE email = :email LIMIT 1"
    )
    suspend fun getUserByEmail(
        email: String
    ): UserEntity?


    @Query(
        "SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)"
    )
    suspend fun userExists(
        email: String
    ): Boolean


    @Query(
        """
        UPDATE users
        SET isSynced = 1,
            serverUserId = :serverUserId
        WHERE email = :email
        """
    )
    suspend fun markUserSynced(
        email: String,

        // UUID from backend
        serverUserId: String?
    )


    @Query(
        "SELECT * FROM users WHERE isSynced = 0"
    )
    suspend fun getUnsyncedUsers(): List<UserEntity>
}