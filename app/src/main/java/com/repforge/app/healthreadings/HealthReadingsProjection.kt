package com.repforge.app.healthreadings

import com.repforge.app.HealthJournalStore
import com.repforge.app.ui.coach.NativeCoachReadiness
import com.repforge.app.ui.coach.NativeCoachSignals
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** View-only projections. Inputs must be daily journal records of the explicitly chosen provider. */
enum class ReadingState { AVAILABLE, NOT_SYNCED, NO_DATA, PERMISSION_DENIED, READ_ERROR, WRONG_DAY_OR_PROVIDER }

data class HeartSample(val at: Instant, val bpm: Int, val origin: String)

data class PulseDayReading(
    val day: LocalDate,
    val provider: String,
    val state: ReadingState,
    val averageBpm: Double? = null,
    val averageDescriptionHu: String? = null,
    val lastBpm: Int? = null,
    val lastAt: Instant? = null,
    val restingBpm: Double? = null,
    val observedMinBpm: Double? = null,
    val observedMaxBpm: Double? = null,
    val rangeDescriptionHu: String? = null,
    val totalSamples: Long? = null,
    val displayedSamples: List<HeartSample> = emptyList(),
    val explanationHu: String
) {
    // A value from a bounded series is NOT a whole-day average or a live pulse.
    val mainLabelHu: String get() = when {
        averageBpm != null -> "${averageBpm.roundToInt()} bpm · ${averageDescriptionHu.orEmpty()}"
        lastBpm != null -> "$lastBpm bpm · Utolsó időbélyeges mérés"
        restingBpm != null -> "${restingBpm.roundToInt()} bpm · Nyugalmi pulzus"
        else -> "—"
    }
}

enum class RecoverySource { SAMSUNG_ENERGY, TRAINPILOT_ESTIMATE, NONE }

data class RecoveryDayReading(
    val day: LocalDate,
    val provider: String,
    val state: ReadingState,
    val source: RecoverySource,
    val score: Double?,
    val coachScore: Double?,
    val samsungEnergy: Double?,
    val samsungAt: String?,
    val trainPilotEstimate: Int?,
    val explanationHu: String
) {
    val displayScoreHu: String get() = score?.let { "${it.roundToInt()}/100" } ?: "—"
}

/**
 * No persistence, no cross-provider fallback and no invented HR or readiness values.
 * The caller provides only rows from the currently selected HealthJournalStore provider.
 */
object HealthReadingsProjection {
    private val pulseKeys = setOf("averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples", "heartRateSeries")
    private val heartKeys = pulseKeys + "restingHeartRate"

    fun pulseDay(
        provider: String,
        day: LocalDate,
        raw: JSONObject?,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault()
    ): PulseDayReading {
        requireProvider(provider)
        fun empty(state: ReadingState, note: String) =
            PulseDayReading(day, provider, state, explanationHu = note)
        if (raw == null) return empty(ReadingState.NOT_SYNCED, "Nincs eltárolt napi olvasás ennél az adatforrásnál.")
        if (!matches(raw, provider, day))
            return empty(ReadingState.WRONG_DAY_OR_PROVIDER, "Az adat másik naphoz vagy adatforráshoz tartozik; nem jeleníthető meg.")
        if (day.atStartOfDay(zone).toInstant().isAfter(now))
            return empty(ReadingState.NOT_SYNCED, "Jövőbeli napra nem jeleníthető meg pulzusmérés.")

        val permissions = raw.optJSONObject("permissions")
        val pulseDenied = permissions?.has("READ_HEART_RATE") == true && !permissions.optBoolean("READ_HEART_RATE")
        val restingDenied = provider == HealthJournalStore.SAMSUNG ||
            permissions?.has("READ_RESTING_HEART_RATE") == true && !permissions.optBoolean("READ_RESTING_HEART_RATE")
        val failed = errorFields(raw)
        val pulseError = failed.any { it in pulseKeys } && !pulseDenied
        val restingError = "restingHeartRate" in failed && !restingDenied
        // Persisted cached values must not be reused as newly granted measurements on a failed read.
        val usePulse = !pulseDenied && !pulseError
        val useResting = !restingDenied && !restingError

        val start = day.atStartOfDay(zone).toInstant()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        val sampleArray = raw.optJSONArray("heartRateSeries")
        val unique = linkedMapOf<Pair<Instant, String>, HeartSample>()
        if (usePulse && sampleArray != null) for (i in 0 until sampleArray.length()) {
            val row = sampleArray.optJSONObject(i) ?: continue
            val at = instant(row.optString("time")) ?: continue
            val bpm = number(row, "bpm")?.takeIf { it in 20.0..250.0 }?.roundToInt() ?: continue
            if (at.isBefore(start) || !at.isBefore(end) || at.isAfter(now)) continue
            val origin = row.optString("source").takeIf { it != "null" }.orEmpty()
            unique[at to origin] = HeartSample(at, bpm, origin)
        }
        val samples = unique.values.sortedWith(compareBy<HeartSample> { it.at }.thenBy { it.origin }).takeLast(96)
        val mean = if (usePulse) number(raw, "averageHeartRate")?.takeIf { it in 20.0..250.0 } else null
        val resting = if (useResting) number(raw, "restingHeartRate")?.takeIf { it in 20.0..250.0 } else null
        val min = if (usePulse) number(raw, "minHeartRate")?.takeIf { it in 20.0..250.0 }
            ?: samples.minOfOrNull { it.bpm.toDouble() } else null
        val max = if (usePulse) number(raw, "maxHeartRate")?.takeIf { it in 20.0..250.0 }
            ?: samples.maxOfOrNull { it.bpm.toDouble() } else null
        val count = if (usePulse) number(raw, "heartRateSamples")?.takeIf { it >= 0 && it <= Long.MAX_VALUE && it % 1.0 == 0.0 }?.toLong() else null
        val last = samples.lastOrNull()
        val hasReading = mean != null || resting != null || min != null || max != null || last != null
        val state = when {
            hasReading -> ReadingState.AVAILABLE
            pulseError || restingError -> ReadingState.READ_ERROR
            pulseDenied && restingDenied -> ReadingState.PERMISSION_DENIED
            else -> ReadingState.NO_DATA
        }
        val note = when (state) {
            ReadingState.AVAILABLE -> when {
                pulseError || restingError -> "Részleges mérés: egyes pulzusmezők olvasása hibás."
                pulseDenied || restingDenied -> "Részleges engedély: nem minden pulzusadat olvasható."
                else -> "A kiválasztott forrás eltárolt mérései."
            }
            ReadingState.READ_ERROR -> "A pulzusadat olvasása SDK-hibát jelzett; nem engedélyhibaként kezeljük."
            ReadingState.PERMISSION_DENIED -> "A pulzusolvasási engedély nincs megadva."
            else -> if (pulseDenied || restingDenied) "Nincs mért pulzusadat; egyes engedélyek hiányoznak."
                    else "Nincs eltárolt pulzusmérés erre a napra; ez önmagában nem bizonyít engedélyhibát."
        }
        return PulseDayReading(day, provider, state, mean,
            if (mean == null) null else if (provider == HealthJournalStore.SAMSUNG) "lekért minták átlaga" else "HC napi aggregált átlag",
            last?.bpm, last?.at, resting, min, max,
            if (min != null || max != null) {
                if (number(raw, "minHeartRate") != null || number(raw, "maxHeartRate") != null)
                    "Adatforrás által megfigyelt tartomány" else "Megjelenített minták tartománya"
            } else null,
            count, samples, note)
    }

    /** Seven distinct local dates, including explicit empty rows; never substitute another day. */
    fun pulseWeek(
        provider: String,
        firstDay: LocalDate,
        rows: List<JSONObject>,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
        dayCount: Int = 7
    ): List<PulseDayReading> {
        require(dayCount in 1..31)
        requireProvider(provider)
        val byDay = rows.mapNotNull { row ->
            val date = runCatching { LocalDate.parse(row.optString("day")) }.getOrNull()
            if (date != null && matches(row, provider, date)) date to row else null
        }.toMap()
        return (0 until dayCount).map { offset ->
            val day = firstDay.plusDays(offset.toLong())
            pulseDay(provider, day, byDay[day], now, zone)
        }
    }

    /**
     * Use the accepted canonical Coach formula, not a new percentage estimate.
     * Samsung Energy and TrainPilot's training/sleep/HRV estimate remain visibly distinct.
     */
    fun recoveryToday(
        provider: String,
        days: List<JSONObject>,
        workouts: List<JSONObject>,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault()
    ): RecoveryDayReading {
        requireProvider(provider)
        val today = now.atZone(zone).toLocalDate()
        val ownDays = days.filter { row ->
            val date = runCatching { LocalDate.parse(row.optString("day")) }.getOrNull()
            date != null && !date.isAfter(today) && matches(row, provider, date)
        }.map { original ->
            JSONObject(original.toString()).also { safe ->
                // Even an accidentally copied Samsung value must not enter an HC calculation.
                if (provider != HealthJournalStore.SAMSUNG ||
                    metricState(safe, "energyScore", "READ_ENERGY_SCORE", true) in
                    setOf(ReadingState.PERMISSION_DENIED, ReadingState.READ_ERROR)) {
                    safe.remove("energyScore")
                    safe.remove("energyScoreTime")
                    safe.remove("energyScoreSource")
                }
                val grants = safe.optJSONObject("permissions")
                val failures = errorFields(safe)
                if (grants?.has("READ_SLEEP") == true && !grants.optBoolean("READ_SLEEP") ||
                    "sleepMinutes" in failures) {
                    for (key in listOf("sleepSessions", "sleepMinutes", "sleepStart", "sleepEnd", "sleepSource"))
                        safe.remove(key)
                }
                if (grants?.has("READ_HEART_RATE_VARIABILITY") == true &&
                    !grants.optBoolean("READ_HEART_RATE_VARIABILITY") || "hrvRmssdMs" in failures) {
                    for (key in listOf("hrvRmssdMs", "hrvTime", "hrvSource", "hrvBaselineMs"))
                        safe.remove(key)
                }
            }
        }
        val current = ownDays.firstOrNull { it.optString("day") == today.toString() }
            ?: return RecoveryDayReading(today, provider, ReadingState.NOT_SYNCED, RecoverySource.NONE,
                null, null, null, null, null, "Nincs kiválasztott forrásból származó mai napi adat.")
        val status = recoveryStatus(current, provider)
        val signals = NativeCoachSignals.fromDays(ownDays, today, zone)
        // Previous-day measurements can build an HRV baseline, but cannot fill today's missing recovery.
        val todayRecovery = signals.recovery.takeIf { it.optString("day") == today.toString() } ?: JSONObject()
        val calculation = NativeCoachReadiness.calculate(todayRecovery, signals.daily, workouts, now, zone)
        // The accepted Home presentation already validates genuine Samsung Energy 0 through the
        // canonical validator using a temporary positive probe. Preserve that zero/null distinction.
        val verifiedZeroAt = if (provider == HealthJournalStore.SAMSUNG &&
            number(current, "energyScore") == 0.0) {
            val probe = JSONObject(current.toString()).put("energyScore", 1.0)
            NativeCoachReadiness.calculate(todayRecovery, signals.daily, workouts, now, zone,
                externalEnergy = probe, externalEnergyAvailable = true)
                .takeIf { it.samsungScore == 1.0 }?.samsungTime
        } else null
        val source = when {
            (calculation.samsungScore != null || verifiedZeroAt != null) && provider == HealthJournalStore.SAMSUNG -> RecoverySource.SAMSUNG_ENERGY
            calculation.trainpilotScore != null && calculation.trainpilotObservations > 0 -> RecoverySource.TRAINPILOT_ESTIMATE
            else -> RecoverySource.NONE
        }
        val selectedScore = when (source) {
            RecoverySource.SAMSUNG_ENERGY -> calculation.samsungScore ?: 0.0
            RecoverySource.TRAINPILOT_ESTIMATE -> calculation.trainpilotScore?.toDouble()
            RecoverySource.NONE -> null
        }
        val finalState = when {
            selectedScore != null -> ReadingState.AVAILABLE
            status == ReadingState.PERMISSION_DENIED || status == ReadingState.READ_ERROR -> status
            current == null -> ReadingState.NOT_SYNCED
            else -> ReadingState.NO_DATA
        }
        val explanation = when (source) {
            RecoverySource.SAMSUNG_ENERGY -> "Samsung Health Energy Score, mai időbélyeges mérés; nem a TrainPilot becslése."
            RecoverySource.TRAINPILOT_ESTIMATE -> if (calculation.hasRecoverySignal)
                "TrainPilot-becslés valós alvás/HRV és naplózott terhelés alapján; nem Samsung Energy Score."
                else "TrainPilot terhelésalapú becslés; nincs elegendő friss alvás/HRV mérés a regeneráció megítéléséhez."
            RecoverySource.NONE -> when (finalState) {
                ReadingState.READ_ERROR -> "A regenerációhoz szükséges SDK-olvasás hibát jelzett."
                ReadingState.PERMISSION_DENIED -> "A Samsung Energy Score olvasási engedélye hiányzik."
                ReadingState.NOT_SYNCED -> "Nincs kiválasztott forrásból származó mai napi adat."
                else -> "Nincs ellenőrizhető friss regenerációs jel; pontszám nem becsülhető."
            }
        }
        return RecoveryDayReading(today, provider, finalState, source, selectedScore,
            calculation.score, calculation.samsungScore ?: if (verifiedZeroAt != null) 0.0 else null,
            calculation.samsungTime ?: verifiedZeroAt, calculation.trainpilotScore, explanation)
    }

    private fun recoveryStatus(raw: JSONObject, provider: String): ReadingState {
        if (provider == HealthJournalStore.SAMSUNG)
            return metricState(raw, "energyScore", "READ_ENERGY_SCORE", true)
        val permissions = raw.optJSONObject("permissions")
        val sleepDenied = permissions?.has("READ_SLEEP") == true && !permissions.optBoolean("READ_SLEEP")
        val hrvDenied = permissions?.has("READ_HEART_RATE_VARIABILITY") == true &&
            !permissions.optBoolean("READ_HEART_RATE_VARIABILITY")
        if (sleepDenied && hrvDenied) return ReadingState.PERMISSION_DENIED
        if (errorFields(raw).any { it == "sleepMinutes" || it == "hrvRmssdMs" }) return ReadingState.READ_ERROR
        return ReadingState.NO_DATA
    }

    private fun metricState(raw: JSONObject, metric: String, grant: String, checkGrant: Boolean): ReadingState {
        val permission = raw.optJSONObject("permissions")
        if (checkGrant && permission?.has(grant) == true && !permission.optBoolean(grant)) return ReadingState.PERMISSION_DENIED
        if (metric in errorFields(raw)) return ReadingState.READ_ERROR
        return ReadingState.NO_DATA
    }

    private fun errorFields(raw: JSONObject): Set<String> {
        val errors = raw.optJSONObject("errors")
        val failed = raw.optJSONArray("failedFields")
        return buildSet {
            if (errors != null) {
                val names = errors.keys()
                while (names.hasNext()) {
                    val key = names.next()
                    if (errors.optString(key).isNotBlank() && errors.optString(key) != "PERMISSION_DENIED") add(key)
                }
            }
            if (failed != null) for (i in 0 until failed.length()) {
                val key = failed.optString(i)
                if (errors?.optString(key) != "PERMISSION_DENIED" && key.isNotBlank()) add(key)
            }
            // Health Connect uses warnings (and hcReadDiagnosis.readErrorFields), not Samsung's errors.
            val diagnostic = raw.optJSONObject("hcReadDiagnosis")?.optJSONArray("readErrorFields")
            val warnings = raw.optJSONArray("warnings")
            for (list in listOfNotNull(diagnostic, warnings)) for (i in 0 until list.length()) {
                val message = list.optString(i)
                when {
                    message.startsWith("READ_HEART_RATE:") -> addAll(pulseKeys)
                    message.startsWith("READ_RESTING_HEART_RATE:") -> add("restingHeartRate")
                    message.startsWith("READ_SLEEP:") -> add("sleepMinutes")
                    message.startsWith("READ_HEART_RATE_VARIABILITY:") -> add("hrvRmssdMs")
                    message.startsWith("READ_ENERGY_SCORE:") -> add("energyScore")
                }
            }
        }
    }

    private fun number(json: JSONObject, name: String): Double? {
        if (!json.has(name) || json.isNull(name)) return null
        val v = when (val raw = json.opt(name)) {
            is Number -> raw.toDouble()
            is String -> raw.trim().toDoubleOrNull()
            else -> null
        }
        return v?.takeIf { it.isFinite() }
    }

    private fun instant(raw: String): Instant? = runCatching { Instant.parse(raw) }.getOrNull()

    private fun matches(raw: JSONObject, provider: String, day: LocalDate): Boolean =
        raw.optString("day") == day.toString() &&
            raw.optString("provider").let { it.isBlank() || it == provider } &&
            raw.optString("channel").let { it.isBlank() || it == provider }

    private fun requireProvider(provider: String) {
        require(provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG) {
            "Only explicit Health Connect or Samsung Health daily providers are supported"
        }
    }
}
