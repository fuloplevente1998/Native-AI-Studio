package com.repforge.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * 2805: opt-in daily Google Drive backup and Calendar refresh. Android decides the exact time
 * inside the window; the job keeps the user's Wi-Fi-only choice and skips a low battery.
 */
class GoogleAutoSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val UNIQUE = "trainpilot-google-auto-sync"
        const val PERIOD_HOURS = 24L
        private const val MAX_RETRIES = 3

        internal fun constraints(auto: NativeSyncSchedule.GoogleAuto): Constraints = Constraints.Builder()
            .setRequiredNetworkType(if (auto.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        /** Apply the saved choice: enqueue/update when any service is on, cancel otherwise. */
        fun schedule(context: Context): Boolean = runCatching {
            val auto = NativeSyncSchedule.googleAuto(context)
            val work = WorkManager.getInstance(context.applicationContext)
            if (!auto.any) work.cancelUniqueWork(UNIQUE)
            else work.enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<GoogleAutoSyncWorker>(PERIOD_HOURS, TimeUnit.HOURS)
                    .setConstraints(constraints(auto)).build())
            true
        }.getOrDefault(false)
    }

    override suspend fun doWork(): Result {
        val auto = NativeSyncSchedule.googleAuto(applicationContext)
        if (!auto.any) return Result.success()
        // An active workout owns the dataset; try again later rather than reporting an error.
        if (ActiveWorkoutStore.current(applicationContext)?.optBoolean("active", false) == true)
            return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
        val repo = GoogleSyncRepository(applicationContext)
        var retry = false
        val services = buildList {
            if (auto.drive) add(GoogleSyncAction.DRIVE)
            if (auto.calendar) add(GoogleSyncAction.CALENDAR)
        }
        for (action in services) {
            if (!repo.unattendedAllowed(action)) continue
            val result = try { repo.runUnattended(action) } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { kotlin.Result.failure(error) }
            val failure = result.exceptionOrNull()
            if (failure is NativeGoogleOperationGate.Busy) { retry = true; continue }
            NativeSyncSchedule.recordAutoResult(applicationContext, action.key, failure?.message ?: failure?.let { "error" })
            if (failure != null && failure.cause is java.io.IOException) retry = true
        }
        return if (retry && runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
    }
}
