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

            // ================================================
            // GET LOCAL USERS THAT HAVE NOT BEEN SYNCED
            // ================================================

            val unsyncedUsers =
                userDao.getUnsyncedUsers()

            for (user in unsyncedUsers) {

                /*
                 * We deliberately cannot sync the user here
                 * if we only stored passwordHash.
                 *
                 * The backend /auth/signup requires the
                 * original password.
                 *
                 * So account creation should ideally sync
                 * while registration has the password.
                 *
                 * Other objects such as tasks, reports,
                 * issues and project updates are much
                 * better candidates for this worker.
                 */
            }


            // ================================================
            // SYNC PENDING OFFLINE TASKS
            // ================================================

            val taskDao = database.taskDao()
            val unsyncedTasks = taskDao.getUnsyncedTasks()

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

            Result.success()

        } catch (e: Exception) {

            e.printStackTrace()

            // WorkManager tries again later
            Result.retry()
        }
    }
}