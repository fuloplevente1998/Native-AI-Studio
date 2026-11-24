package com.repforge.app

import android.content.Context

/**
 * User-chosen sync cadence (2805). Settings only: no workout, journal or health rows live here,
 * so a missing/corrupt value falls back to the documented default instead of resetting data.
 */
internal object NativeSyncSchedule {
    private const val PREFS = "trainpilot-sync-schedule"
    val FOREGROUND_MINUTES = listOf(2, 5, 15)
    val BACKGROUND_MINUTES = listOf(15, 30, 60)
    const val DEFAULT_FOREGROUND = 5
    const val DEFAULT_BACKGROUND = 15
    /** Returning to the app refreshes Health again only if the last automatic read is this old. */
    const val RESUME_MIN_GAP_MS = 2L * 60L * 1000L

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun foregroundMinutes(context: Context): Int =
        runCatching { prefs(context).getInt("healthForegroundMin", DEFAULT_FOREGROUND) }.getOrNull()
            ?.takeIf { it in FOREGROUND_MINUTES } ?: DEFAULT_FOREGROUND

    fun backgroundMinutes(context: Context): Int =
        runCatching { prefs(context).getInt("healthBackgroundMin", DEFAULT_BACKGROUND) }.getOrNull()
            ?.takeIf { it in BACKGROUND_MINUTES } ?: DEFAULT_BACKGROUND

    /** Bumped after a saved foreground change so the running refresh loop re-times at once. */
    val foregroundRevision = kotlinx.coroutines.flow.MutableStateFlow(0)

    fun setForegroundMinutes(context: Context, minutes: Int): Boolean =
        (minutes in FOREGROUND_MINUTES && prefs(context).edit().putInt("healthForegroundMin", minutes).commit())
            .also { if (it) foregroundRevision.value++ }

    fun setBackgroundMinutes(context: Context, minutes: Int): Boolean =
        minutes in BACKGROUND_MINUTES && prefs(context).edit().putInt("healthBackgroundMin", minutes).commit()

    data class GoogleAuto(val drive: Boolean = false, val calendar: Boolean = false, val wifiOnly: Boolean = true) {
        val any: Boolean get() = drive || calendar
    }

    fun googleAuto(context: Context): GoogleAuto = runCatching {
        val p = prefs(context)
        GoogleAuto(p.getBoolean("googleAutoDrive", false), p.getBoolean("googleAutoCalendar", false), p.getBoolean("googleAutoWifiOnly", true))
    }.getOrDefault(GoogleAuto())

    /** Durable write first; the caller reschedules only after this returned true. */
    fun setGoogleAuto(context: Context, value: GoogleAuto): Boolean = prefs(context).edit()
        .putBoolean("googleAutoDrive", value.drive).putBoolean("googleAutoCalendar", value.calendar)
        .putBoolean("googleAutoWifiOnly", value.wifiOnly).commit()

    /** Unattended failures are shown per service until the next success (manual or automatic). */
    fun autoFailure(context: Context, service: String): String =
        runCatching { prefs(context).getString("googleAutoFailed:$service", "").orEmpty() }.getOrDefault("")

    fun recordAutoResult(context: Context, service: String, failure: String?) {
        val editor = prefs(context).edit()
        if (failure.isNullOrBlank()) editor.remove("googleAutoFailed:$service") else editor.putString("googleAutoFailed:$service", failure.take(200))
        editor.commit()
    }
}
