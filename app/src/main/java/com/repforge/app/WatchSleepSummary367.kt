package com.repforge.app

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * #367/9: the phone's authoritative "today" sleep total for the watch home snapshot.
 *
 * Same provider, same day row and the same [NativeDailyHealthDetails.sleepMinutes] as the phone
 * Today tile (VmHealthJournal), so an identical source/window shows an identical total on both
 * devices. Only the aggregated minutes travel (no records, no stages). Missing → no key, never 0.
 */
internal object WatchSleepSummary367 {
    /** Pure projection, unit tested. [day] is the already admitted (permission-projected) row. */
    fun payload(day: JSONObject?, provider: String, date: LocalDate, zone: ZoneId, now: Instant): JSONObject? {
        if (provider != HealthJournalStore.HC && provider != HealthJournalStore.SAMSUNG) return null
        val minutes = NativeDailyHealthDetails.sleepMinutes(day, date, zone, now) ?: return null
        val whole = minutes.toLong().takeIf { it in 1L..(24L * 60L) } ?: return null
        return JSONObject().put("date", date.toString()).put("minutes", whole)
            .put("provider", provider).put("computedAt", now.toEpochMilli())
    }

    /** Never throws: a failed read only omits the summary; the home snapshot is still published. */
    fun read(context: Context, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): JSONObject? = runCatching {
        val app = context.applicationContext
        val store = HealthJournalStore.get(app)
        if (store.pendingRestore().isNotEmpty()) return@runCatching null
        val provider = store.preferences().optString("provider", HealthJournalStore.HC)
        val feature = if (provider == HealthJournalStore.SAMSUNG) NativePrivacyControls.SAMSUNG_HEALTH
            else NativePrivacyControls.HEALTH_CONNECT
        if (!NativePrivacyControls.enabled(app, feature)) return@runCatching null
        val today = now.atZone(zone).toLocalDate()
        val raw = NativeHealthProviderData.day(store, provider, today)
        val day = if (provider == HealthJournalStore.HC) {
            val granted = HealthConnectRepository.READ_PERMS.filter { HealthConnectRepository.isAllowed(app, it) }.toSet()
            HealthConnectVisibleDay.project(raw, granted)
        } else raw
        payload(day, provider, today, zone, now)
    }.getOrNull()
}
