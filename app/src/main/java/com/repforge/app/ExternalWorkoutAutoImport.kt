package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.health.NativeHealthForegroundAdmission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Device-local, explicit opt-in. No SDK reads or scheduling of its own. */
internal object ExternalWorkoutAutoImport {
    private const val PREFS = "trainpilot-external-import-359"
    private const val KEY = "auto"
    private const val REV = "revision"
    private const val LIMIT = 512
    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY, false)

    @Synchronized fun setEnabled(context: Context, enable: Boolean): Boolean {
        val p = prefs(context)
        if (p.getBoolean(KEY, false) == enable) return true
        return p.edit().putBoolean(KEY, enable).putLong(REV, p.getLong(REV, 0) + 1).commit()
    }

    @Synchronized internal fun consent(context: Context): Long? =
        prefs(context).let { if (it.getBoolean(KEY, false)) it.getLong(REV, 0) else null }

    fun admit(context: Context, provider: String, today: LocalDate, zone: ZoneId): Flight? =
        consent(context)?.let { Flight(it, provider, today, zone) }

    data class Observed(val identity: ExternalWorkoutIdentity, val modified: Long, val start: Long, val end: Long)
    data class Report(val saved: Int = 0, val overlap: Int = 0, val skipped: Int = 0,
                      val failed: Boolean = false, val limited: Boolean = false) {
        fun json() = JSONObject().put("saved", saved).put("overlap", overlap).put("skipped", skipped)
            .put("failed", failed).put("limited", limited)
    }

    internal class Flight(private val revision: Long, private val provider: String,
                          private val today: LocalDate, private val zone: ZoneId) {
        private val observed = linkedMapOf<ExternalWorkoutIdentity, Observed>()
        private var limited = false
        internal fun candidates(): List<Observed> = observed.values.toList()

        /** Unrelated missing permissions do not prevent a successfully read exercise from being considered.
         * Exercise read failures never turn old cached rows into new auto-import candidates.
         */
        fun observe(day: LocalDate, result: JSONObject) {
            if (day.isBefore(today.minusDays(29)) || day.isAfter(today)) return
            val warnings = result.optJSONArray("warnings")
            if (warnings != null && (0 until warnings.length()).any {
                    warnings.optString(it).startsWith("READ_EXERCISE:")
                }) return
            val failed = result.optJSONArray("failedFields")
            if (failed != null && (0 until failed.length()).any { failed.optString(it) == "exerciseSessions" }) return
            val rows = result.optJSONArray("exerciseSessions") ?: return
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val source = row.optString("source")
                val id = row.optString("id")
                if (source.isBlank() || id.isBlank() || !row.has("lastModifiedMs") || row.isNull("lastModifiedMs")) continue
                val start = runCatching { Instant.parse(row.optString("start")).toEpochMilli() }.getOrNull() ?: continue
                val end = runCatching { Instant.parse(row.optString("end")).toEpochMilli() }.getOrNull() ?: continue
                if (end <= start || start < today.minusDays(29).atStartOfDay(zone).toInstant().toEpochMilli()) continue
                val identity = ExternalWorkoutIdentity(provider, source, id)
                val modified = row.optLong("lastModifiedMs", -1)
                if (modified < 0) continue
                val prior = observed[identity]
                if (prior != null && prior.modified > modified) continue
                if (prior == null && observed.size >= LIMIT) { limited = true; continue }
                observed[identity] = Observed(identity, modified, start, end)
            }
        }

        suspend fun publish(context: Context, generation: Long, syncGuard: () -> Unit): Report =
            withContext(Dispatchers.IO) {
                val app = context.applicationContext
                val coroutine = currentCoroutineContext()
                val ticket = NativeDataReplacement.continuationForCurrentData(app)
                val journal = HealthJournalStore.get(app)
                var saved = 0; var overlap = 0; var skipped = 0
                val guard = {
                    coroutine.ensureActive()
                    syncGuard()
                    ticket.requireCurrent(app)
                    check(consent(app) == revision) { "Automatic import consent changed" }
                    check(journal.generation() == generation && journal.pendingRestore().isEmpty())
                    check(NativeHealthBackupGate.available() && NativeHealthForegroundAdmission.safeOffMain(app))
                }
                try {
                    if (consent(app) != revision || observed.isEmpty()) return@withContext Report(limited = limited)
                    val db = TrainPilotDatabase.getInstance(app)
                    for (batch in candidates().chunked(16)) {
                        guard()
                        NativeHealthForegroundAdmission.requireMainAdmission(app)
                        if (!ExternalWorkoutImport.hasReadGrant(app, provider, guard))
                            return@withContext Report(saved, overlap, skipped, failed = true, limited = limited)
                        for (seen in batch) {
                            guard()
                            // No provider IPC under the write lease. Verify the exact observed version.
                            val outcome = NativeDataReplacement.mutate(app) {
                                guard()
                                val row = journal.externalWorkoutRecord(seen.identity, generation)
                                    ?: return@mutate ExternalWorkoutImport.Outcome.UNAVAILABLE
                                if (row.modifiedMs != seen.modified || row.startMs != seen.start || row.endMs != seen.end)
                                    return@mutate ExternalWorkoutImport.Outcome.CHANGED
                                ExternalWorkoutImport.persist(app, db, row) {
                                    guard()
                                    val current = journal.externalWorkoutRecord(seen.identity, generation)
                                    check(current != null && current.summary().toString() == row.summary().toString())
                                }
                            }
                            when (outcome) {
                                ExternalWorkoutImport.Outcome.IMPORTED -> saved++
                                ExternalWorkoutImport.Outcome.REVIEW_OVERLAP -> overlap++
                                else -> skipped++
                            }
                        }
                    }
                    Report(saved, overlap, skipped, limited = limited)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { Report(saved, overlap, skipped, failed = true, limited = limited) }
                finally { if (saved > 0) ExternalWorkoutImport.notifyImported() }
            }
    }
}
