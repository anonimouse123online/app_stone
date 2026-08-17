package com.example.capstonesample.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ============================================================
    // GET ALL TASKS
    // Used by UI even when offline
    // ============================================================

    @Query("SELECT * FROM tasks")
    fun getTasks(): Flow<List<TaskEntity>>


    // ============================================================
    // INSERT / UPDATE TASKS
    // ============================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(
        tasks: List<TaskEntity>
    )


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(
        task: TaskEntity
    )


    // ============================================================
    // GET UNSYNCED TASKS
    // SyncWorker uses this
    // ============================================================

    @Query(
        """
        SELECT * FROM tasks
        WHERE isSynced = 0
        """
    )
    suspend fun getUnsyncedTasks(): List<TaskEntity>


    // ============================================================
    // MARK TASK AS SYNCED
    // Called after backend successfully receives the task
    // ============================================================

    @Query(
        """
        UPDATE tasks
        SET isSynced = 1
        WHERE id = :taskId
        """
    )
    suspend fun markTaskSynced(
        taskId: Int
    )


    // ============================================================
    // MARK TASK AS UNSYNCED
    // Use when engineer modifies task locally
    // ============================================================

    @Query(
        """
        UPDATE tasks
        SET isSynced = 0
        WHERE id = :taskId
        """
    )
    suspend fun markTaskUnsynced(
        taskId: Int
    )


    // ============================================================
    // DELETE
    // ============================================================

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}