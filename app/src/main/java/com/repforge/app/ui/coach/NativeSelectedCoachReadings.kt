package com.repforge.app.ui.coach

import android.content.Context
import com.repforge.app.HealthJournalStore
import com.repforge.app.HealthConnectRepository
import com.repforge.app.HealthConnectVisibleDay
import com.repforge.app.NativeHealthProviderData
import com.repforge.app.NativePrivacyControls
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.healthreadings.HealthReadingsProjection
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Read admitted measurements for the shared Coach calculation from both health channels. */
internal object NativeSelectedCoachReadings {
    data class Snapshot(val provider: String, val readiness: NativeCoachReadiness.Result)

    suspend fun load(context: Context, db: TrainPilotDatabase, now: Instant, zone: ZoneId): Snapshot {
        val store = HealthJournalStore.get(context)
        val generation = store.generation()
        val provider = HealthJournalStore.HC
        val today = now.atZone(zone).toLocalDate()
        val hcEnabled = NativePrivacyControls.enabled(context, NativePrivacyControls.HEALTH_CONNECT)
        val samsungEnabled = NativePrivacyControls.enabled(context, NativePrivacyControls.SAMSUNG_HEALTH)
        val grants = if (hcEnabled) {
            try { HealthConnectRepository.currentlyGrantedReadKeys(context) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { emptySet() }
        } else emptySet()
        fun rows(channel: String): List<JSONObject> = when (channel) {
            HealthJournalStore.HC -> if (hcEnabled) {
                NativeHealthProviderData.recentRecoveryDays(store, channel, today)
                    .mapNotNull { HealthConnectVisibleDay.project(it, grants) }
            } else emptyList()
            else -> if (samsungEnabled) NativeHealthProviderData.recentRecoveryDays(store, channel, today)
                else emptyList()
        }
        val alternate = HealthJournalStore.SAMSUNG
        val history = db.workoutHistoryDao().getCoachRecentHistory(
            now.minus(Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli()).map { JSONObject(it) }
        val result = HealthReadingsProjection.coachReadiness(provider, rows(provider), rows(alternate), history, now, zone)
        check(store.generation() == generation &&
            hcEnabled == NativePrivacyControls.enabled(context, NativePrivacyControls.HEALTH_CONNECT) &&
            samsungEnabled == NativePrivacyControls.enabled(context, NativePrivacyControls.SAMSUNG_HEALTH)) {
            "Az egészségadat-forrás megváltozott. Frissítsd a Coach adatait."
        }
        return Snapshot(provider, result)
    }
}
