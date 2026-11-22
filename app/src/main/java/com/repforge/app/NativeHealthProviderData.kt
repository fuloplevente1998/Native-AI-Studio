package com.repforge.app

import org.json.JSONObject
import java.time.LocalDate

/** Exact provider/day selection; a missing Samsung day never falls back to HC. */
internal object NativeHealthProviderData {
    fun day(store: HealthJournalStore, provider: String, date: LocalDate): JSONObject? {
        NativeWorkoutHealthSource.requireProvider(provider)
        val days = store.page(date.toString(), date.toString(), provider, "", 0, 0, 1).optJSONArray("days")
        return (0 until (days?.length() ?: 0)).mapNotNull { days?.optJSONObject(it) }
            .firstOrNull { it.optString("day") == date.toString() && it.optString("channel") == provider }
    }

    fun recentRecoveryDays(store: HealthJournalStore, provider: String, today: LocalDate): List<JSONObject> {
        NativeWorkoutHealthSource.requireProvider(provider)
        val rows = store.page(today.minusDays(13).toString(), today.toString(), provider,
            "@aggregate", 0, 0, 1).optJSONArray("days") ?: return emptyList()
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.takeIf { row ->
            row.optString("channel") == provider
        } }
    }
}
