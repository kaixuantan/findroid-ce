package dev.jdtech.jellyfin.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object UserDataSync {
    const val UNIQUE_WORK_NAME = "syncUserData"

    fun shouldSyncOnTransition(
        previousEffectiveOffline: Boolean,
        newEffectiveOffline: Boolean,
    ): Boolean = previousEffectiveOffline && !newEffectiveOffline

    fun schedule(workManager: WorkManager) {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val syncWorkRequest =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName = UNIQUE_WORK_NAME,
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = syncWorkRequest,
        )
    }
}
