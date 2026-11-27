package com.example.capstonesample.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks")
    fun getTasks(): Flow<List<TaskEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(
        tasks: List<TaskEntity>
    )


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(
        task: TaskEntity
    )


    @Query(
        """
        SELECT *
        FROM tasks
        WHERE isSynced = 0
        """
    )
    suspend fun getUnsyncedTasks():
            List<TaskEntity>


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


    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}