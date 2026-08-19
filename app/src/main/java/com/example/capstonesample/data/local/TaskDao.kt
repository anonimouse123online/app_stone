package com.example.capstonesample.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ============================================================
    // GET ALL LOCAL TASKS
    // ============================================================

    @Query("SELECT * FROM tasks")
    fun getTasks(): Flow<List<TaskEntity>>


    // ============================================================
    // INSERT MANY
    // ============================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(
        tasks: List<TaskEntity>
    )


    // ============================================================
    // INSERT ONE
    // ============================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(
        task: TaskEntity
    )


    // ============================================================
    // GET UNSYNCED TASKS
    // ============================================================

    @Query(
        """
        SELECT *
        FROM tasks
        WHERE isSynced = 0
        """
    )
    suspend fun getUnsyncedTasks():
            List<TaskEntity>


    // ============================================================
    // MARK SYNCED
    // ============================================================

    @Query(
        """
        UPDATE tasks
        SET isSynced = 1
        WHERE id = :taskId
        """
    )
    suspend fun markTaskSynced(
        taskId: String
    )


    // ============================================================
    // MARK UNSYNCED
    // ============================================================

    @Query(
        """
        UPDATE tasks
        SET isSynced = 0
        WHERE id = :taskId
        """
    )
    suspend fun markTaskUnsynced(
        taskId: String
    )


    // ============================================================
    // DELETE ALL
    // ============================================================

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}