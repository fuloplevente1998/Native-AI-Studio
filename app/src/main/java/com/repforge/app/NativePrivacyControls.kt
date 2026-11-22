package com.repforge.app

import android.content.Context
import android.app.job.JobScheduler

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
            GPS -> {
                if (!enabled) DistanceTrackingService.stop(app)
            }
            NOTIFICATIONS -> {
                if (enabled) HealthBackgroundService.resume(app)
                else HealthBackgroundService.stop(app)
            }
            HEALTH_CONNECT -> {
                // Do not synchronously open the health journal/database on the Compose UI thread.
                // Queued jobs are canceled immediately; an already executing worker checks
                // the same durable flag before each read and before publication.
                if (!enabled) (app.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler)
                    ?.cancel(HealthBackgroundJob.ID)
            }
            SAMSUNG_HEALTH -> Unit // Direct reads gate against the flag at admission + commit.
        }
    }

    @JvmStatic
    fun disabledMessage(language: String): String = when (language.lowercase(java.util.Locale.ROOT)) {
        "en" -> "This health source is switched off in privacy settings."
        "de" -> "Diese Gesundheitsquelle ist in den Datenschutzeinstellungen ausgeschaltet."
        "ro" -> "Această sursă de sănătate este dezactivată în setările de confidențialitate."
        else -> "Ez az egészségügyi adatforrás ki van kapcsolva az adatvédelmi beállításokban."
    }

    @JvmStatic
    fun backgroundPermissionMessage(language: String): String = when (language.lowercase(java.util.Locale.ROOT)) {
        "en" -> "Health Connect background access was not granted."
        "de" -> "Health Connect hat keine Hintergrundberechtigung erhalten."
        "ro" -> "Accesul în fundal Health Connect nu a fost acordat."
        else -> "A Health Connect nem kapott háttérengedélyt."
    }

    @JvmStatic
    fun writePermissionMessage(language: String): String = when (language.lowercase(java.util.Locale.ROOT)) {
        "en" -> "Health Connect needs workout-writing permission."
        "de" -> "Health Connect benötigt eine Berechtigung zum Schreiben von Trainings."
        "ro" -> "Health Connect necesită permisiune pentru scrierea antrenamentelor."
        else -> "A Health Connect edzésírásához külön engedély szükséges."
    }

    @JvmStatic
    fun requireEnabled(context: Context, feature: String) {
        check(enabled(context, feature)) { "Provider disabled by user" }
    }
}
