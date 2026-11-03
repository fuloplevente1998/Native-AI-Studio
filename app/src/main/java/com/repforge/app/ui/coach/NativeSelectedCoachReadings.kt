package com.repforge.app.ui.coach

import android.content.Context
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeHealthProviderData
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.healthreadings.HealthReadingsProjection
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** One selected-provider admission path; never read the merged journal channel for Coach. */
internal object NativeSelectedCoachReadings {
    data class Snapshot(val provider: String, val readiness: NativeCoachReadiness.Result)

    suspend fun load(context: Context, db: TrainPilotDatabase, now: Instant, zone: ZoneId): Snapshot {
        val store = HealthJournalStore.get(context)
        val generation = store.generation()
        val provider = store.preferences().optString("provider", HealthJournalStore.HC)
        require(provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG)
        val today = now.atZone(zone).toLocalDate()
        val rows = (13 downTo 0).mapNotNull { offset ->
            val day = today.minusDays(offset.toLong())
            NativeHealthProviderData.day(store, provider, day)?.let { raw ->
                JSONObject(raw.toString()).put("day", day.toString())
            }
        }
        val own = HealthReadingsProjection.validatedRecoveryDays(provider, rows, now, zone)
        val history = db.workoutHistoryDao().getCoachRecentHistory(
            now.minus(Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli()).map { JSONObject(it) }
        val signals = NativeCoachSignals.fromDays(own, today, zone)
        val hasToday = own.any { it.optString("day") == today.toString() }
        val recovery = signals.recovery.takeIf { it.optString("day") == today.toString() } ?: JSONObject()
        val result = if (hasToday) NativeCoachReadiness.calculate(recovery, signals.daily, history, now, zone)
            else NativeCoachReadiness.calculate(JSONObject(), JSONObject(), emptyList(), now, zone)
        check(store.generation() == generation && store.preferences().optString("provider", HealthJournalStore.HC) == provider) {
            "Az egészségadat-forrás megváltozott. Frissítsd a Coach adatait."
        }
        return Snapshot(provider, result)
    }
}
