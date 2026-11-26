package com.repforge.app.ui.health

import com.repforge.app.HealthJournalStore
import com.repforge.app.healthreadings.HealthReadingsProjection
import com.repforge.app.healthreadings.PulseDayReading
import com.repforge.app.healthreadings.ReadingState
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A daily value comes from one provider. Missing readings can use the opted-in supplement. */
internal class HealthDaySources(
    private val day: LocalDate,
    primary: JSONObject?,
    supplement: JSONObject?,
    private val now: Instant,
    private val zone: ZoneId
) {
    data class Value(val number: Double, val provider: String)
    private val rows = listOf(
        HealthJournalStore.HC to primary,
        HealthJournalStore.SAMSUNG to supplement
    ).mapNotNull { (provider, raw) ->
        raw?.takeIf { it.optString("channel") == provider && it.optString("day") == day.toString() }
            ?.let { provider to it }
    }

    fun row(provider: String): JSONObject? = rows.firstOrNull { it.first == provider }?.second

    fun metric(key: String, eligible: (JSONObject, String) -> Boolean = { _, _ -> true }): Value? =
        rows.firstNotNullOfOrNull { (provider, raw) ->
            if (eligible(raw, provider) &&
                NativeDetailedHealthMetrics.status(raw, key, provider, day, now, zone) == NativeDetailedHealthMetrics.Status.AVAILABLE
            ) Value(raw.getDouble(key), provider) else null
        }

    fun sleep(): Value? = rows.firstNotNullOfOrNull { (provider, raw) ->
        NativeHealthJournal.sleepMinutes(raw, provider, day, now, zone)?.let { Value(it, provider) }
    }

    fun pulse(): PulseDayReading {
        val primary = HealthReadingsProjection.pulseDay(HealthJournalStore.HC, day, row(HealthJournalStore.HC), now, zone)
        if (primary.averageBpm != null || primary.lastBpm != null) return primary
        val supplement = HealthReadingsProjection.pulseDay(HealthJournalStore.SAMSUNG, day, row(HealthJournalStore.SAMSUNG), now, zone)
        return when {
            supplement.averageBpm != null || supplement.lastBpm != null -> supplement
            primary.state == ReadingState.AVAILABLE -> primary
            supplement.state == ReadingState.AVAILABLE -> supplement
            else -> primary
        }
    }
}
