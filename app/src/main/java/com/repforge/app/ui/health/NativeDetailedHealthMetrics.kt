package com.repforge.app.ui.health

import com.repforge.app.HealthJournalStore
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Admission for the detail browser, independent of today's compact tiles and Coach. */
internal object NativeDetailedHealthMetrics {
    fun permission(key: String, samsung: Boolean): String = when (key) {
        "steps" -> "READ_STEPS"
        "activeCalories" -> "READ_ACTIVE_CALORIES_BURNED"
        "totalCalories" -> "READ_TOTAL_CALORIES_BURNED"
        "distanceMeters" -> "READ_DISTANCE"
        "averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples" -> "READ_HEART_RATE"
        "restingHeartRate" -> "READ_RESTING_HEART_RATE"
        "hrvRmssdMs" -> "READ_HEART_RATE_VARIABILITY"
        "weightKg" -> "READ_WEIGHT"
        "bodyFatPercent", "bodyFatMassKg", "skeletalMuscleMassKg", "fatFreeMassKg", "totalBodyWaterLiters", "bodyMassIndex" ->
            if (samsung) "READ_WEIGHT" else if (key == "bodyFatPercent") "READ_BODY_FAT" else "READ_BODY_WATER_MASS"
        "basalMetabolicRateKcal" -> if (samsung) "READ_WEIGHT" else "READ_BASAL_METABOLIC_RATE"
        "oxygenSaturationPercent" -> "READ_OXYGEN_SATURATION"
        "vo2Max" -> "READ_VO2_MAX"
        "energyScore" -> "READ_ENERGY_SCORE"
        "sleepScore" -> "READ_SLEEP"
        "bloodPressureSystolic", "bloodPressureDiastolic" -> "READ_BLOOD_PRESSURE"
        "bloodGlucoseMmolL" -> "READ_BLOOD_GLUCOSE"
        "respiratoryRate" -> "READ_RESPIRATORY_RATE"
        else -> ""
    }

    fun recordType(key: String): String = when (key) {
        "steps" -> "StepsRecord"
        "activeCalories" -> "ActiveCaloriesBurnedRecord"
        "totalCalories" -> "TotalCaloriesBurnedRecord"
        "distanceMeters" -> "DistanceRecord"
        "averageHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples" -> "HeartRateRecord"
        "restingHeartRate" -> "RestingHeartRateRecord"
        "hrvRmssdMs" -> "HeartRateVariabilityRmssdRecord"
        "weightKg" -> "WeightRecord"
        "bodyFatPercent" -> "BodyFatRecord"
        "bodyFatMassKg" -> "BodyFatMass"
        "skeletalMuscleMassKg" -> "SkeletalMuscleMass"
        "fatFreeMassKg" -> "FatFreeMass"
        "totalBodyWaterLiters" -> "TotalBodyWater"
        "oxygenSaturationPercent" -> "OxygenSaturationRecord"
        "vo2Max" -> "Vo2MaxRecord"
        "energyScore" -> "EnergyScoreRecord"
        "sleepScore" -> "SleepSessionRecord"
        "bloodPressureSystolic", "bloodPressureDiastolic" -> "BloodPressureRecord"
        "bloodGlucoseMmolL" -> "BloodGlucoseRecord"
        "respiratoryRate" -> "RespiratoryRateRecord"
        "basalMetabolicRateKcal" -> "BasalMetabolicRate"
        "bodyMassIndex" -> "BodyMassIndex"
        else -> ""
    }

    enum class Status { AVAILABLE, NO_DATA, DENIED, ERROR }
    fun status(raw: JSONObject?, key: String, provider: String, date: LocalDate,
        now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Status {
        if (raw == null || raw.optString("channel") != provider || raw.optString("day") != date.toString() ||
            date > now.atZone(zone).toLocalDate()) return Status.NO_DATA
        val originProvider = raw.optJSONObject("metricProviders")?.optString(key).orEmpty()
        if (originProvider.isNotBlank() && originProvider != provider) return Status.NO_DATA
        val grant = permission(key, provider == HealthJournalStore.SAMSUNG)
        val permissions = raw.optJSONObject("permissions")
        if (permissions?.has(grant) == true && !permissions.optBoolean(grant)) return Status.DENIED
        val errors = raw.optJSONObject("errors")
        if (errors?.optString(key) == "PERMISSION_DENIED") return Status.DENIED
        val failed = raw.optJSONArray("failedFields")
        val warnings = raw.optJSONArray("warnings")
        if ((0 until (failed?.length() ?: 0)).any { failed?.optString(it) == key } ||
            errors?.optString(key).orEmpty().isNotBlank() ||
            (0 until (warnings?.length() ?: 0)).any { warnings?.optString(it)?.startsWith("$grant:") == true }) return Status.ERROR
        // No HC substitutes for Samsung-only data, or unsupported Samsung physiological fields.
        if (provider == HealthJournalStore.HC && key in setOf("energyScore", "sleepScore") ||
            provider == HealthJournalStore.SAMSUNG && key in setOf("restingHeartRate", "hrvRmssdMs")) return Status.NO_DATA
        val value = when (val v = raw.opt(key)) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull(); else -> null }
            ?.takeIf { it.isFinite() && it >= 0 } ?: return Status.NO_DATA
        if (key in setOf("steps", "heartRateSamples") && value % 1.0 != 0.0) return Status.NO_DATA
        val zeroAllowed = key in setOf("steps", "activeCalories", "totalCalories", "distanceMeters", "heartRateSamples", "energyScore", "sleepScore")
        if (!zeroAllowed && value == 0.0 || key in setOf("energyScore", "sleepScore", "oxygenSaturationPercent", "bodyFatPercent") && value > 100)
            return Status.NO_DATA
        if (key == "energyScore") {
            val at = runCatching { Instant.parse(raw.optString("energyScoreTime")) }.getOrNull() ?: return Status.NO_DATA
            if (at.atZone(zone).toLocalDate() != date || at > now || raw.optString("energyScoreSource") !in
                setOf(HealthJournalStore.SAMSUNG, "com.sec.android.app.shealth")) return Status.NO_DATA
        }
        return Status.AVAILABLE
    }

    fun admitted(raw: JSONObject?, provider: String, date: LocalDate, now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault()): JSONObject = JSONObject().put("day", date.toString()).put("channel", provider).also { safe ->
        NativeHealthMetrics.definitions.forEach { d ->
            if (status(raw, d.key, provider, date, now, zone) == Status.AVAILABLE) safe.put(d.key, raw!!.get(d.key))
        }
        raw?.optJSONObject("metricProviders")?.let { safe.put("metricProviders", JSONObject(it.toString())) }
    }
}
