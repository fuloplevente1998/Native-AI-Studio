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

    fun limitedMessage(language: String): String = when (language.lowercase(java.util.Locale.ROOT)) {
        "en" -> "Limited sync: daily data was read, but Health Connect change tracking is unavailable. Check for system and Health Connect updates; then sync again."
        "de" -> "Eingeschränkte Synchronisierung: Tagesdaten wurden gelesen, die Änderungsverfolgung von Health Connect ist nicht verfügbar. System- und Health-Connect-Updates prüfen und erneut synchronisieren."
        "ro" -> "Sincronizare limitată: datele zilnice au fost citite, dar urmărirea modificărilor Health Connect nu este disponibilă. Verifică actualizările de sistem și Health Connect, apoi sincronizează din nou."
        else -> "Korlátozott szinkron: a napi adatok olvasása megtörtént, de a Health Connect változáskövetése nem érhető el. Ellenőrizd a rendszer és a Health Connect frissítéseit, majd szinkronizálj újra."
    }

    fun failureMessage(error: Throwable, language: String): String = when (language.lowercase(java.util.Locale.ROOT)) {
        "en" -> "Sync failed. Check the provider, permissions and available system updates, then try again."
        "de" -> "Synchronisierung fehlgeschlagen. Anbieter, Berechtigungen und Systemupdates prüfen und erneut versuchen."
        "ro" -> "Sincronizarea a eșuat. Verifică sursa, permisiunile și actualizările de sistem, apoi încearcă din nou."
        else -> "A szinkron nem sikerült. Ellenőrizd az adatforrást, az engedélyeket és a rendszerfrissítéseket, majd próbáld újra."
    }
}
