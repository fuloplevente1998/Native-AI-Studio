package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject

/**
 * Read-only UI projection of an HC journal day. A partial/revoked grant must NEVER
 * resurrect an older value from the journal as though it was the latest reading.
 * The saved raw record is kept for audit and a later re-grant.
 */
internal object HealthConnectVisibleDay {
    private val fieldsByPermission: Map<String, List<String>> = mapOf(
        "READ_STEPS" to listOf("steps", "stepsSource", "stepsOrigin", "stepsObservedAt",
            "stepsDeviceId", "availableStepOrigins"),
        "READ_ACTIVE_CALORIES_BURNED" to listOf("activeCalories"),
        "READ_TOTAL_CALORIES_BURNED" to listOf("totalCalories"),
        "READ_DISTANCE" to listOf("distanceMeters"),
        "READ_HEART_RATE" to listOf("averageHeartRate", "minHeartRate", "maxHeartRate",
            "heartRateSamples", "heartRateSeries"),
        "READ_RESTING_HEART_RATE" to listOf("restingHeartRate"),
        "READ_SLEEP" to listOf("sleepSessions", "sleepMinutes", "sleepScore"),
        "READ_HEART_RATE_VARIABILITY" to listOf("hrvRmssdMs", "hrvTime", "hrvSource", "hrvId"),
        "READ_EXERCISE" to listOf("exerciseSessions", "exerciseSessionCount", "exerciseMinutes"),
        "READ_WEIGHT" to listOf("weightKg", "weightTime", "weightSource", "weightId"),
        "READ_BODY_FAT" to listOf("bodyFatPercent", "bodyFatTime", "bodyFatSource", "bodyFatId"),
        "READ_OXYGEN_SATURATION" to listOf("oxygenSaturationPercent",
            "oxygenSaturationTime", "oxygenSaturationSource", "oxygenSaturationId"),
        "READ_VO2_MAX" to listOf("vo2Max", "vo2MaxTime", "vo2MaxSource",
            "vo2MaxId", "vo2MaxMeasurementMethod"),
        "READ_SPEED" to listOf("averageSpeedMps", "maxSpeedMps", "speedSamples"),
        "READ_BLOOD_PRESSURE" to listOf("bloodPressureSystolic", "bloodPressureDiastolic",
            "bloodPressureTime", "bloodPressureSource", "bloodPressureId"),
        "READ_BLOOD_GLUCOSE" to listOf("bloodGlucoseMmolL", "bloodGlucoseTime",
            "bloodGlucoseSource", "bloodGlucoseId"),
        "READ_RESPIRATORY_RATE" to listOf("respiratoryRate", "respiratoryRateTime",
            "respiratoryRateSource", "respiratoryRateId")
    )

    fun project(source: JSONObject?, granted: Set<String>): JSONObject? {
        if (source == null) return null
        val output = JSONObject(source.toString())
        val warnings = output.optJSONArray("warnings") ?: JSONArray()
        val failed = output.optJSONArray("failedFields") ?: JSONArray()
        val errors = output.optJSONObject("errors")
        val sources = output.optJSONObject("metricProviders")
        val permissions = JSONObject()
        for (permission in HealthConnectRepository.READ_PERMS) {
            val allowed = permission in granted
            permissions.put(permission, allowed)
            val fields = fieldsByPermission[permission].orEmpty()
            val warning = (0 until warnings.length()).any { warnings.optString(it).startsWith("$permission:") }
            val failedField = fields.any { field ->
                (0 until failed.length()).any { failed.optString(it) == field } ||
                    errors?.optString(field).orEmpty().isNotBlank()
            }
            if (!allowed || warning || failedField) {
                fields.forEach { field ->
                    output.remove(field)
                    sources?.remove(field)
                }
            }
        }
        output.put("permissions", permissions)
        return output
    }
}
