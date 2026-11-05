package com.repforge.app

import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/** Sequential provider reads, never provider selection or measurement aggregation. */
internal object NativeHealthProviderBatch {
    val providers = listOf(HealthJournalStore.SAMSUNG, HealthJournalStore.HC)
    enum class Status { SUCCESS, PARTIAL, UNAVAILABLE, FAILED }
    data class Outcome(val provider: String, val status: Status, val message: String = "", val retryable: Boolean = false)
    data class Report(val outcomes: List<Outcome>) {
        val successful get() = outcomes.all { it.status == Status.SUCCESS }
        val failed get() = outcomes.none { it.status == Status.SUCCESS || it.status == Status.PARTIAL }
        val partial get() = !successful && !failed
        val retryable get() = outcomes.any { it.retryable }
        fun json(language: String = "hu"): JSONObject {
            fun tr(hu: String, en: String, de: String, ro: String) = when (language) {
                "en" -> en; "de" -> de; "ro" -> ro; else -> hu
            }
            return JSONObject().put("partial", !successful)
            .put("failed", failed).put("message", outcomes.joinToString(" • ") { row ->
                NativeWorkoutHealthSource.label(row.provider) + ": " + when (row.status) {
                    Status.SUCCESS -> tr("sikeres szinkron", "sync completed", "Synchronisierung abgeschlossen", "sincronizare finalizată")
                    Status.PARTIAL -> tr("részleges szinkron; ellenőrizd az engedélyeket és a forrás hibáit", "partial sync; check permissions and source errors", "teilweise synchronisiert; Berechtigungen und Quellfehler prüfen", "sincronizare parțială; verifică permisiunile și erorile sursei")
                    Status.UNAVAILABLE -> tr("nem elérhető vagy hiányzó olvasási engedély", "unavailable or missing read permission", "nicht verfügbar oder Leseberechtigung fehlt", "indisponibil sau fără permisiune de citire")
                    Status.FAILED -> tr("sikertelen", "failed", "fehlgeschlagen", "eșuat") + " • ${row.message.take(180)}"
                }
            })
        }
    }

    suspend fun run(available: (String) -> Boolean, guard: () -> Unit,
        sync: suspend (String) -> Result<JSONObject>): Report {
        val results = mutableListOf<Outcome>()
        for (provider in providers) {
            guard()
            if (!available(provider)) {
                results.add(Outcome(provider, Status.UNAVAILABLE))
                continue
            }
            val result = try { sync(provider) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { Result.failure(failure) }
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            guard() // An obsolete screen/dataset is never permission to continue the next read.
            results.add(Outcome(provider, when {
                result.isFailure -> Status.FAILED
                result.getOrThrow().optBoolean("partial") -> Status.PARTIAL
                else -> Status.SUCCESS
            }, result.exceptionOrNull()?.message.orEmpty(),
                com.repforge.app.ui.health.NativeHealthAppRefreshPolicy.transient(result.exceptionOrNull()) ||
                    result.getOrNull()?.optBoolean("retryable") == true))
        }
        return Report(results)
    }
}
