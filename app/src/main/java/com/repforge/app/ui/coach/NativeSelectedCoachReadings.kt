package com.repforge.app.ui.coach

import android.content.Context
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeHealthProviderData
import com.repforge.app.NativePrivacyControls
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.healthreadings.HealthReadingsProjection
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Selected recovery inputs plus independently verified Samsung Energy from the direct channel. */
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
        val samsungEnergy = if (provider != HealthJournalStore.SAMSUNG &&
            NativePrivacyControls.enabled(context, NativePrivacyControls.SAMSUNG_HEALTH)) {
            HealthReadingsProjection.verifiedSamsungDay(
                NativeHealthProviderData.day(store, HealthJournalStore.SAMSUNG, today), now, zone)
        } else null
        val result = NativeCoachReadiness.calculate(
            recovery, signals.daily, if (hasToday || samsungEnergy != null) history else emptyList(), now, zone,
            externalEnergy = samsungEnergy, externalEnergyAvailable = samsungEnergy != null
        )
        check(store.generation() == generation && store.preferences().optString("provider", HealthJournalStore.HC) == provider) {
            "Az egészségadat-forrás megváltozott. Frissítsd a Coach adatait."
        }
        return Snapshot(provider, result)
    }
}
