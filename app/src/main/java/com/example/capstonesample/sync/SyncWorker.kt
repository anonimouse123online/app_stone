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
            // TODO:
            // SYNC PENDING TASKS
            // ================================================

            /*
            val pendingTasks =
                taskDao.getPendingSyncTasks()

            for (task in pendingTasks) {

                val response =
                    RetrofitClient.api.uploadTask(
                        ...
                    )

                if (response.isSuccessful) {

                    taskDao.markSynced(
                        task.id
                    )
                }
            }
            */


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