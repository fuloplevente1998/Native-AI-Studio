package com.repforge.app.ui.health

/** Foreground retries are bounded and never substitute for grants or safe session admission. */
internal object NativeHealthAppRefreshPolicy {
    const val FOREGROUND_INTERVAL_MS = 5L * 60L * 1000L

    fun retryDelay(completedRetries: Int, transientFailure: Boolean): Long? =
        if (!transientFailure) null else when (completedRetries) {
            0 -> 30_000L
            1 -> 120_000L
            else -> null
        }

    fun transient(failure: Throwable?): Boolean {
        var cause = failure
        repeat(8) {
            if (cause is java.io.IOException || cause is android.os.DeadObjectException) return true
            if (cause is SecurityException || cause is kotlinx.coroutines.CancellationException) return false
            cause = cause?.cause ?: return false
        }
        return false
    }
}
