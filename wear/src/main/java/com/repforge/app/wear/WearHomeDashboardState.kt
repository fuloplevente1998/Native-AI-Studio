package com.repforge.app.wear

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONObject

internal enum class HomeAction {
    START,
    RESUME,
    DAYS,
    RESULT,
    PHONE,
}

internal data class WearHomeDashboardState(
    val phase: String,
    val clock: String,
    val title: String,
    val subtitle: String,
    val detail: String,
    val action: HomeAction,
    val actionLabel: String,
    val measurement: String,
    val metrics: List<String>,
    val completed: Int,
    val total: Int,
    val source: String,
    val active: Boolean,
)

internal fun wearHomeDashboardState(
    home: WatchHomeSnapshot?,
    workout: WearWorkout?,
    closure: WearClosure?,
    health: JSONObject?,
    enabled: Boolean,
    now: Long,
): WearHomeDashboardState {
    val clock = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val homeClock = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    val source =
        if (home?.publishedAt?.let { it > 0L && it <= now + 5000 } == true)
            "Adat ${clock.format(Instant.ofEpochMilli(home.publishedAt))}"
        else "Helyi adat"
    val measurementId =
        workout?.workoutId ?: closure?.takeIf { it.status in listOf("pending", "error") }?.workoutId
    val currentHealth =
        health?.takeIf {
            measurementId != null &&
                measurementId.isNotBlank() &&
                it.optString("workoutId") == measurementId &&
                it.optString("source") == "wear_health_services"
        }
    val measurement =
        when (currentHealth?.optString("state")) {
            "active" ->
                if (now - currentHealth.optLong("updatedAt") > 20000L) "Legutóbbi mérés"
                else "Mérés folyamatban"
            "paused" -> "Mérés szünetel"
            "starting" -> "Mérés indul"
            "ending" -> "Mérés lezárása"
            "error" -> "Mérési hiba"
            "ended" -> "Mérés lezárva"
            else ->
                if (!enabled) "Mérés kikapcsolva"
                else if (workout != null) "Mérésre vár" else "Mérés bekapcsolva"
        }
    fun value(
        phase: String,
        title: String,
        subtitle: String,
        detail: String,
        action: HomeAction,
        label: String,
        total: Int = 0,
    ) =
        WearHomeDashboardState(
            phase,
            homeClock.format(Instant.ofEpochMilli(now)),
            title,
            subtitle,
            detail,
            action,
            label,
            measurement,
            emptyList(),
            0,
            total,
            source,
            false,
        )
    if (workout != null) {
        val rest = ((workout.restEndAt - now + 999L) / 1000L).coerceAtLeast(0L).toInt()
        val sets = workout.exercise?.sets.orEmpty()
        val completed = sets.count { it.done }
        val metrics =
            wearLiveWorkoutRows(currentHealth, workout.workoutId, now)
                .filter { it.contains("bpm") || it.contains("kcal") || it.startsWith("Edzés: ") }
                .map { it.removePrefix("Edzés: ") }
        return WearHomeDashboardState(
            if (rest > 0) "PIHENŐ" else "EDZÉS",
            formatSeconds(if (rest > 0) rest else workout.elapsedSeconds(now)).padStart(5, '0'),
            workout.exercise?.name ?: workout.programName,
            workout.dayName,
            if (completed == sets.size && sets.isNotEmpty()) "$completed/${sets.size} sorozat kész"
            else "Sorozat ${workout.currentSetIndex + 1}/${sets.size}",
            HomeAction.RESUME,
            "Folytatás",
            measurement,
            metrics,
            completed,
            sets.size,
            source,
            true,
        )
    }
    if (closure?.status in listOf("pending", "error"))
        return value(
            if (closure?.status == "error") "MENTÉSI HIBA" else "MENTÉS VÁR",
            if (closure?.action == "discardWorkout") "Elvetés visszaigazolása" else "Órán megőrizve",
            if (closure?.action == "discardWorkout") "A telefon válaszának ellenőrzése" else "Az edzés a telefonra vár",
            source,
            HomeAction.RESULT,
            "Állapot",
        )
    if (home == null)
        return value(
            "SZINKRONRA VÁR",
            "TrainPilot",
            "Nyisd meg a telefonon",
            "A programadat még nem érkezett meg",
            HomeAction.PHONE,
            "Telefon",
        )
    if (home.hasDraft)
        return value(
            "FÉLBEHAGYOTT",
            "Edzés a telefonon",
            "Nyisd meg a folytatáshoz",
            source,
            HomeAction.PHONE,
            "Telefon",
        )
    home.recommended
        ?.takeIf { it.day.exercises.isNotEmpty() }
        ?.let { rec ->
            val time =
                try {
                    if (rec.plannedStart.isBlank()) ""
                    else clock.format(Instant.parse(rec.plannedStart))
                } catch (_: Exception) {
                    ""
                }
            return value(
                "KÖVETKEZŐ EDZÉS",
                rec.day.name,
                rec.day.programName,
                "${rec.day.exercises.size} gyakorlat${if (time.isBlank()) "" else " · $time"}",
                HomeAction.START,
                "Indítás",
                rec.day.exercises.size,
            )
        }
    return if (home.days.any { it.exercises.isNotEmpty() })
        value(
            "EDZÉSRE KÉSZ",
            home.activeProgramName.ifBlank { "Programnapok" },
            "Válassz edzésnapot",
            source,
            HomeAction.DAYS,
            "Napok",
        )
    else
        value(
            "NINCS PROGRAM",
            "Válassz programot",
            "A telefonos alkalmazásban",
            source,
            HomeAction.PHONE,
            "Telefon",
        )
}

/** A home pulse is live only for the active, matching workout and a fresh available sample. */
internal fun wearHomeLivePulse(data: JSONObject?, workoutId: String?, enabled: Boolean, now: Long): Int? {
    if (!enabled || workoutId.isNullOrBlank() || data == null ||
        data.optString("workoutId") != workoutId || data.optString("source") != "wear_health_services" ||
        data.optString("state") != "active" || data.optString("heartRateAvailability") in listOf("acquiring", "unavailable")) return null
    val sample = data.optLong("lastSampleAt", data.optLong("updatedAt"))
    // 2808: the UI clock ticks once per second while samples arrive in between, so a sample can
    // be a few hundred ms "newer" than the tick. That is not stale; only a real skew is rejected.
    if (sample <= 0L || now - sample !in -WEAR_PULSE_SKEW_MS..15000L ||
        now - data.optLong("updatedAt") !in -WEAR_PULSE_SKEW_MS..20000L) return null
    val pulse = data.optDouble("heartRate")
    return pulse.takeIf { it.isFinite() && it in 20.0..250.0 }?.toInt()
}

internal const val WEAR_PULSE_SKEW_MS = 5000L
internal const val WEAR_PULSE_FRESH_MS = 120000L
internal const val WEAR_PULSE_KEEP_MS = 30L * 60L * 1000L

internal enum class WearPulseFreshness { LIVE, RECENT, PREVIOUS, NONE }

/** One stable heart-rate line: the number never alternates with a measurement status text. */
internal data class WearHomePulse(val bpm: Int?, val freshness: WearPulseFreshness, val at: Long?)

/**
 * 2808: a single presentation for the home heart-rate line. A live workout sample wins; else the
 * last passive sample is shown, fresh or marked as earlier with its time. Whether recording is on
 * is shown separately (heart colour), so it can never replace the value. Samples up to
 * [WEAR_PULSE_SKEW_MS] ahead of the once-per-second UI tick count as just measured.
 */
internal fun wearHomePulse(live: Int?, passive: WearDailyPulse?, now: Long): WearHomePulse {
    if (live != null) return WearHomePulse(live, WearPulseFreshness.LIVE, now)
    val sample = passive?.takeIf { it.bpm in 20..250 && it.at > 0L } ?: return WearHomePulse(null, WearPulseFreshness.NONE, null)
    val age = now - sample.at
    return when {
        age < -WEAR_PULSE_SKEW_MS -> WearHomePulse(null, WearPulseFreshness.NONE, null)
        age <= WEAR_PULSE_FRESH_MS -> WearHomePulse(sample.bpm, WearPulseFreshness.RECENT, sample.at)
        age <= WEAR_PULSE_KEEP_MS -> WearHomePulse(sample.bpm, WearPulseFreshness.PREVIOUS, sample.at)
        else -> WearHomePulse(null, WearPulseFreshness.NONE, null)
    }
}

internal fun wearHomePulseText(pulse: WearHomePulse, zone: ZoneId = ZoneId.systemDefault(), language: String = java.util.Locale.getDefault().language): String {
    val bpm = pulse.bpm ?: return "–"
    if (pulse.freshness != WearPulseFreshness.PREVIOUS || pulse.at == null) return "$bpm bpm"
    val time = DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(Instant.ofEpochMilli(pulse.at))
    return "$bpm bpm · ${wearText("korábbi", language)} $time"
}

/**
 * 2808: bezel arcs of the home page, in drawArc degrees (0° = 3 o'clock, clockwise). Both arcs
 * are concentric with the round screen a few dp from its edge; each % label sits on the same
 * ring just past the lower arc end, so it never overlaps an arc, the clock or the buttons.
 */
internal data class WearHomeArcLayout(
    val inset: Float,
    val stroke: Float,
    val sweep: Float,
    val batteryStart: Float,
    val stepsStart: Float,
    val batteryLabel: Float,
    val stepsLabel: Float,
    val labelSweep: Float,
)

internal fun wearHomeArcLayout(diameter: Float): WearHomeArcLayout {
    val sweep = 103f
    val batteryStart = 147f // lower-left end, filling clockwise up to 250°
    return WearHomeArcLayout(
        inset = 2.5f,
        stroke = if (diameter < 180f) 3.5f else 4f,
        sweep = sweep,
        batteryStart = batteryStart,
        stepsStart = 180f - batteryStart - sweep, // mirrored: -70° … 33°
        batteryLabel = 131f,
        stepsLabel = 49f,
        labelSweep = 28f,
    )
}
