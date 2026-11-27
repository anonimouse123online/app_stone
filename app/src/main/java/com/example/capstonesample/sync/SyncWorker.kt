package com.example.capstonesample.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.local.AppDatabase
import com.example.capstonesample.data.model.SignupRequest

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        val database =
            AppDatabase.getDatabase(
                applicationContext
            )

        val userDao =
            database.userDao()

        return try {

            val unsyncedUsers =
                userDao.getUnsyncedUsers()

            for (user in unsyncedUsers) {

                /*
                 * Signup requires the original password, which is not stored
                 * here. Account creation must sync during registration;
                 * other pending data can use this worker.
                 */
            }


<<<<<<< HEAD
            // TODO:
            /*
            val pendingTasks =
                taskDao.getPendingSyncTasks()
=======
            // ================================================
            // SYNC PENDING OFFLINE TASKS
            // ================================================

            val taskDao = database.taskDao()
            val unsyncedTasks = taskDao.getUnsyncedTasks()
>>>>>>> 832bdbbdbc8d87fa94e46afc2308761e8d217fec

            for (task in unsyncedTasks) {
                try {
                    val statusToSync = task.status
                    if (!statusToSync.isNullOrBlank()) {
                        val response = RetrofitClient.api.updateTaskWorkStatus(
                            taskId = task.id,
                            request = com.example.capstonesample.data.model.UpdateTaskWorkStatusRequest(
                                status = statusToSync
                            )
                        )

                        if (response.isSuccessful && response.body()?.success == true) {
                            taskDao.markTaskSynced(task.id)
                        } else if (response.code() == 409) {
                            // Conflict resolution: Server has newer status/state, accept server state
                            taskDao.markTaskSynced(task.id)
                        }
                    }
                } catch (taskErr: Exception) {
                    android.util.Log.e("SYNC_WORKER", "Failed to sync task ${task.id}", taskErr)
                }
            }


<<<<<<< HEAD
=======
            // ================================================
            // SYNC PENDING OFFLINE TIME LOGS & DIARY ENTRIES
            // ================================================
            try {
                OfflineSyncManager.syncPendingTimeLogs(applicationContext)
            } catch (timeLogErr: Exception) {
                android.util.Log.e("SYNC_WORKER", "Failed to sync offline time logs", timeLogErr)
            }

            // ================================================
            // EVERYTHING SUCCESSFUL
            // ================================================

>>>>>>> 832bdbbdbc8d87fa94e46afc2308761e8d217fec
            Result.success()

        } catch (e: Exception) {

            e.printStackTrace()

            Result.retry()
        }
    }
}