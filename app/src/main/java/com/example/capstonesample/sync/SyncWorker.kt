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


            // TODO:
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


            Result.success()

        } catch (e: Exception) {

            e.printStackTrace()

            Result.retry()
        }
    }
}