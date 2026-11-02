package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject

/**
 * Diagnostic evidence from ONE Health Connect daily read, before journal preservation
 * of older fields. It never estimates measurements, retries through Samsung, or changes
 * the selected provider/origin. A successful SDK call with no measurements is not an error.
 */
internal object HealthConnectReadDiagnosis {
    const val DATA_PRESENT = "DATA_PRESENT"
    const val NO_DATA = "NO_DATA"
    const val NO_DATA_LIMITED_GRANTS = "NO_DATA_LIMITED_GRANTS"
    const val PARTIAL_GRANTS = "PARTIAL_GRANTS"
    const val READ_ERROR = "READ_ERROR"
    const val STEP_ORIGIN_NOT_OBSERVED = "STEP_ORIGIN_NOT_OBSERVED"

    fun inspect(day: JSONObject, preferredOrigin: String): JSONObject {
        val permissions = day.optJSONObject("permissions") ?: JSONObject()
        val granted = HealthConnectRepository.READ_PERMS.filter { permissions.optBoolean(it, false) }
        val missing = HealthConnectRepository.READ_PERMS.filterNot { permissions.optBoolean(it, false) }
        val warnings = day.optJSONArray("warnings") ?: JSONArray()
        val errors = (0 until warnings.length()).mapNotNull { index ->
            warnings.optString(index).takeIf { it.startsWith("READ_") && ": " in it }
        }
        val measuredFields = HealthProviderProjection.METRICS.filter { day.has(it) && !day.isNull(it) }
        val hasCollections = listOf("sleepSessions", "exerciseSessions", "heartRateSeries")
            .any { (day.optJSONArray(it)?.length() ?: 0) > 0 }
        val hasData = measuredFields.isNotEmpty() || hasCollections
        val origins = day.optJSONArray("availableStepOrigins") ?: JSONArray()
        val stepOrigins = (0 until origins.length()).mapNotNull { origins.optString(it).takeIf(String::isNotBlank) }
        val originSpecific = preferredOrigin.isNotEmpty() && preferredOrigin !in setOf("auto", "priority")
        val missingStepOrigin = originSpecific && stepOrigins.isNotEmpty() &&
            preferredOrigin !in stepOrigins && day.isNull("steps")

        val status = when {
            errors.isNotEmpty() -> READ_ERROR
            missingStepOrigin -> STEP_ORIGIN_NOT_OBSERVED
            !hasData && missing.isNotEmpty() -> NO_DATA_LIMITED_GRANTS
            !hasData -> NO_DATA
            missing.isNotEmpty() -> PARTIAL_GRANTS
            else -> DATA_PRESENT
        }
        return JSONObject()
            .put("status", status)
            .put("hasMeasurements", hasData)
            .put("grantedReadPermissions", JSONArray(granted))
            .put("missingReadPermissions", JSONArray(missing))
            .put("readErrorFields", JSONArray(errors))
            .put("observedMetricKeys", JSONArray(measuredFields))
            .put("selectedStepOrigin", day.optString("activityOrigin", ""))
            .put("preferredStepOrigin", preferredOrigin)
            .put("observedStepOrigins", JSONArray(stepOrigins))
    }

    fun describe(diagnosis: JSONObject): String = when (diagnosis.optString("status")) {
        NO_DATA -> "Health Connect: a kiválasztott napon nem érkezett mérés. Ellenőrizd, hogy van-e adat a Health Connectben, és megosztja-e azt a forrásalkalmazás."
        NO_DATA_LIMITED_GRANTS -> "Health Connect: nem érkezett mérés, és nem minden olvasási engedély adott. Ellenőrizd az engedélyeket és a forrásalkalmazás adatmegosztását."
        PARTIAL_GRANTS -> "Health Connect: csak részleges olvasási engedélyek állnak rendelkezésre; a hiányzó adatok nem tekinthetők nullának."
        READ_ERROR -> "Health Connect: legalább egy SDK-olvasás hibát jelzett; az adatok és a korábbi sikeres mérések részlegesek lehetnek."
        STEP_ORIGIN_NOT_OBSERVED -> "Health Connect: a kiválasztott lépésadat-forrás nem szerepelt a nap megfigyelt lépésforrásai között. Ellenőrizd a forrásszűrőt."
        else -> ""
    }
}
