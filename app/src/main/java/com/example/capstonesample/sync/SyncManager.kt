package com.example.capstonesample.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncManager {

    private const val PERIODIC_SYNC_NAME =
        "sitepulse_periodic_sync"

    private const val IMMEDIATE_SYNC_NAME =
        "sitepulse_immediate_sync"


    // ============================================================
    // PERIODIC BACKGROUND SYNC
    // ============================================================

    fun startPeriodicSync(
        context: Context
    ) {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .setRequiresBatteryNotLow(
                    true
                )
                .build()


        val syncRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(
                    constraints
                )
                .build()


        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(

                PERIODIC_SYNC_NAME,

                ExistingPeriodicWorkPolicy.KEEP,

                syncRequest
            )
    }


    // ============================================================
    // SYNC AS SOON AS INTERNET IS AVAILABLE
    // ============================================================

    fun requestImmediateSync(
        context: Context
    ) {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()


        val request =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    constraints
                )
                .build()


        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(

                IMMEDIATE_SYNC_NAME,

                ExistingWorkPolicy.REPLACE,

                request
            )
    }
}