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
}
