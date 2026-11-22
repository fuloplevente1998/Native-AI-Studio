package com.repforge.app

import android.content.Context

/**
 * App-level opt-in is independent of Android/provider grants. System permission changes
 * must never re-enable a feature the user explicitly turned off. Existing installs retain
 * their pre-upgrade behavior until a choice is made. No health/workout records are erased.
 */
object NativePrivacyControls {
    const val GPS = "gps"
    const val NOTIFICATIONS = "notifications"
    const val HEALTH_CONNECT = "health_connect"
    const val SAMSUNG_HEALTH = "samsung_health"
    private const val PREFS = "trainpilot-privacy-controls-v1"
    private val keys = setOf(GPS, NOTIFICATIONS, HEALTH_CONNECT, SAMSUNG_HEALTH)

    @JvmStatic
    fun enabled(context: Context, feature: String): Boolean {
        require(feature in keys) { "Unknown privacy feature" }
        return context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(feature, true)
    }

    /** Synchronous durable commit: workers recheck this flag before publishing any records. */
    @JvmStatic
    fun set(context: Context, feature: String, enabled: Boolean) {
        require(feature in keys) { "Unknown privacy feature" }
        val app = context.applicationContext
        check(app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(feature, enabled).commit()) { "Privacy preference could not be saved" }
        when (feature) {
            GPS -> if (!enabled) DistanceTrackingService.stop(app)
            NOTIFICATIONS -> if (enabled) HealthBackgroundService.resume(app)
                else HealthBackgroundService.stop(app)
            HEALTH_CONNECT -> HealthBackgroundJob.schedule(app) // Cancels queued work when off.
        }
    }

    @JvmStatic
    fun requireEnabled(context: Context, feature: String) {
        check(enabled(context, feature)) { "Provider disabled by user" }
    }
}
