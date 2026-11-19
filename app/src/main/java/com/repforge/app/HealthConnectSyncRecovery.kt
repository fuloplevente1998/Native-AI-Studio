package com.repforge.app

import kotlinx.coroutines.CancellationException
import java.util.Collections
import java.util.IdentityHashMap

/** Only the observed provider schema failure permits bypassing change-token APIs. */
internal object HealthConnectSyncRecovery {
    fun schemaFailure(error: Throwable): Boolean {
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var cause: Throwable? = error
        var matched = false
        while (cause != null && seen.add(cause)) {
            if (cause is CancellationException || cause is SecurityException) return false
            val message = cause.message.orEmpty().lowercase(java.util.Locale.ROOT)
            matched = matched || ("change_log_request_table" in message &&
                "no column named row_offset" in message)
            cause = cause.cause
        }
        return matched
    }

    /**
     * 2774: the phone's Health Connect change log is broken (system schema bug), so the app reads
     * the days directly. That read is a successful sync; this is an informational note, not an error.
     */
    fun limitedMessage(language: String): String = when (language.lowercase(java.util.Locale.ROOT)) {
        "en" -> "Synced by reading days directly. Health Connect change tracking does not work on this phone; updating Health Connect in the Play Store may fix it."
        "de" -> "Durch direktes Lesen der Tage synchronisiert. Die Änderungsverfolgung von Health Connect funktioniert auf diesem Telefon nicht; ein Health-Connect-Update im Play Store kann helfen."
        "ro" -> "Sincronizat prin citirea directă a zilelor. Urmărirea modificărilor Health Connect nu funcționează pe acest telefon; actualizarea Health Connect din Play Store poate ajuta."
        else -> "Szinkron kész a napok közvetlen olvasásával. A Health Connect változáskövetése ezen a telefonon nem működik; a Health Connect frissítése a Play Áruházban segíthet."
    }

    fun failureMessage(error: Throwable, language: String): String = when (language.lowercase(java.util.Locale.ROOT)) {
        "en" -> "Sync failed. Check the provider, permissions and available system updates, then try again."
        "de" -> "Synchronisierung fehlgeschlagen. Anbieter, Berechtigungen und Systemupdates prüfen und erneut versuchen."
        "ro" -> "Sincronizarea a eșuat. Verifică sursa, permisiunile și actualizările de sistem, apoi încearcă din nou."
        else -> "A szinkron nem sikerült. Ellenőrizd az adatforrást, az engedélyeket és a rendszerfrissítéseket, majd próbáld újra."
    }
}
