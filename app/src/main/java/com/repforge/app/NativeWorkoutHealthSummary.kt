package com.repforge.app

import com.repforge.app.data.db.WorkoutHistoryEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

/** Read-only projection of saved evidence. Never split an aggregate or add competing observations. */
internal object NativeWorkoutHealthSummary {
    const val WEAR = "wear"
    data class Evidence(val provider: String, val data: JSONObject,
        val windows: List<NativeWorkoutActiveWindows.Window>, val legacyWindow: Boolean = false) {
        val milliseconds: Long get() = windows.sumOf { Duration.between(it.start, it.end).toMillis() }
    }
    data class Metric(val values: Map<String, Double>, val evidence: List<Evidence>,
        val incomplete: Boolean, val overlapExcluded: Boolean)
    data class Result(val heart: Metric, val energy: Metric, val rejectedEvidence: Boolean,
        val relationships: List<Relationship>)
    data class Relationship(val first: String, val second: String, val confirmedMirror: Boolean)

    fun read(workout: WorkoutHistoryEntity): Result {
        val saved = JSONObject().put("started", workout.started).put("finished", workout.finished)
        workout.activeIntervalsJson?.let { saved.put("activeIntervals", JSONArray(it)) }
        val raw = parse(workout.rawJson)
        saved.put("nativeHealthWindowStale", raw?.optBoolean("nativeHealthWindowStale") == true)
        saved.put("id", workout.id).put("syncId", workout.syncId).put("workoutKey", workout.workoutKey)
        return project(saved, NativeSavedWearHealth.read(workout), parse(workout.health240Json))
    }

    internal fun project(workout: JSONObject, wear: JSONObject?, document: JSONObject?): Result {
        val active = runCatching { NativeWorkoutActiveWindows.read(workout).filter { it.end.isAfter(it.start) } }.getOrDefault(emptyList())
        val evidence = mutableListOf<Evidence>()
        var rejected = false
        if (active.isNotEmpty() && wear != null) {
            val aliases = listOf("id", "syncId", "workoutKey").map { workout.optString(it) }.filter(String::isNotBlank)
            val id = wear.optString("workoutId")
            val explicitStart = wear.optString("windowStart", wear.optString("start"))
            val explicitEnd = wear.optString("windowEnd", wear.optString("end"))
            val windowMatches = if (explicitStart.isBlank() && explicitEnd.isBlank()) true else
                same(explicitStart, workout.optString("started")) && same(explicitEnd, workout.optString("finished"))
            if (!workout.optBoolean("nativeHealthWindowStale") && (id.isBlank() || id in aliases) && windowMatches)
                evidence.add(Evidence(WEAR, wear, active, explicitStart.isBlank()))
            else rejected = true
        }
        for (provider in listOf(HealthJournalStore.SAMSUNG, HealthJournalStore.HC)) {
            val snapshot = NativeWorkoutHealthSource.snapshot(document, provider) ?: continue
            val measured = JSONObject().put("started", snapshot.optString("windowStart", snapshot.optString("start")))
                .put("finished", snapshot.optString("windowEnd", snapshot.optString("end")))
            snapshot.optJSONArray("activeIntervals")?.let { measured.put("activeIntervals", it) }
            val valid = same(measured.optString("started"), workout.optString("started")) &&
                same(measured.optString("finished"), workout.optString("finished")) &&
                runCatching { NativeWorkoutActiveWindows.read(measured) == active }.getOrDefault(false)
            if (!valid || active.isEmpty()) { rejected = true; continue }
            val rows = snapshot.optJSONArray("measurementWindows")
            if (rows == null) evidence.add(Evidence(provider, snapshot, active, true))
            else for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i)
                val window = row?.let { runCatching {
                    NativeWorkoutActiveWindows.Window(Instant.parse(it.getString("start")), Instant.parse(it.getString("end")))
                }.getOrNull() }
                if (row != null && window != null && window.end.isAfter(window.start) &&
                    active.any { window.start >= it.start && window.end <= it.end }) {
                    // Carry the saved parent's warnings/origin into each actual read window.
                    val data = JSONObject(row.toString())
                    if (snapshot.optBoolean("partial")) data.put("partial", true)
                    evidence.add(Evidence(provider, data, listOf(window)))
                } else rejected = true
            }
        }
        fun number(data: JSONObject, key: String) = (data.opt(key) as? Number)?.toDouble()
            ?.takeIf { it.isFinite() && it >= 0 && (!key.contains("HeartRate") || it > 0) }
        fun select(candidates: List<Evidence>): Pair<List<Evidence>, Boolean> {
            val accepted = mutableListOf<Evidence>()
            var overlap = false
            // Input order is a stable, visible priority: saved Wear, direct Samsung, then HC.
            for (candidate in candidates) {
                if (accepted.any { overlaps(it, candidate) }) overlap = true else accepted.add(candidate)
            }
            return accepted to overlap
        }
        val expectedMs = active.sumOf { Duration.between(it.start, it.end).toMillis() }
        fun incomplete(rows: List<Evidence>) = rows.isEmpty() || rows.sumOf { it.milliseconds } < expectedMs || rows.any {
            it.data.optBoolean("partial") || (it.data.optJSONArray("warnings")?.length() ?: 0) > 0 ||
                (it.data.optJSONArray("failedFields")?.length() ?: 0) > 0 ||
                (it.data.optJSONObject("errors")?.length() ?: 0) > 0 ||
                (it.provider == WEAR && it.legacyWindow) ||
                it.data.optBoolean("workoutEnergyProrated")
        }
        val (heartRows, heartOverlap) = select(evidence.filter { number(it.data, "averageHeartRate") != null })
        val heartValues = linkedMapOf<String, Double>()
        val weighted = heartRows.all { (number(it.data, "heartRateSamples") ?: 0.0) > 0 }
        // One original aggregate is safe; multiple means require actual sample counts.
        if (heartRows.size == 1 || heartRows.isNotEmpty() && weighted) {
            val count = heartRows.sumOf { number(it.data, "heartRateSamples") ?: 0.0 }
            heartValues["averageHeartRate"] = if (heartRows.size == 1) number(heartRows.single().data, "averageHeartRate")!!
                else heartRows.sumOf { number(it.data, "averageHeartRate")!! * number(it.data, "heartRateSamples")!! } / count
            if (weighted) heartValues["heartRateSamples"] = count
            for (key in listOf("minHeartRate", "maxHeartRate")) {
                val values = heartRows.mapNotNull { number(it.data, key) }
                if (values.size == heartRows.size) heartValues[key] = if (key.startsWith("min")) values.min() else values.max()
            }
        }
        // The canonical session-energy matcher can report mixed active/total intervals.
        // Such an old combined value cannot be separated safely for this projection.
        fun energyNumber(data: JSONObject, key: String): Double? {
            if (key == "workoutCalories" && strings(data, "workoutEnergyTypes").size > 1) return null
            return number(data, key)
        }
        // Keep active, total and session energy separate; select one definition and label it.
        val energyKey = evidence.firstNotNullOfOrNull { row ->
            listOf("activeCalories", "totalCalories", "workoutCalories").firstOrNull { energyNumber(row.data, it) != null }
        }
        fun energyKind(row: Evidence): String = if (energyKey == "workoutCalories")
            strings(row.data, "workoutEnergyTypes").sorted().takeIf { it.isNotEmpty() }?.joinToString()
                ?: "unknown:${row.provider}:${row.windows}" else energyKey.orEmpty()
        val energyCandidates = evidence.filter { energyKey != null && energyNumber(it.data, energyKey) != null }
        val kind = energyCandidates.firstOrNull()?.let(::energyKind)
        val (energyRows, energyOverlap) = select(energyCandidates.filter { energyKind(it) == kind })
        val energyValues = if (energyRows.isEmpty() || energyKey == null) emptyMap() else
            mapOf(energyKey to energyRows.sumOf { number(it.data, energyKey)!! })
        val relationships = mutableListOf<Relationship>()
        for (i in evidence.indices) for (j in i + 1 until evidence.size) {
            val a = evidence[i]; val b = evidence[j]
            if (a.provider == b.provider || !overlaps(a, b)) continue
            val idsA = strings(a.data, "workoutEnergyRecordIds")
            val idsB = strings(b.data, "workoutEnergyRecordIds")
            relationships.add(Relationship(a.provider, b.provider, idsA.intersect(idsB).isNotEmpty()))
        }
        return Result(Metric(heartValues, heartRows, incomplete(heartRows) || heartValues.isEmpty(), heartOverlap),
            Metric(energyValues, energyRows, incomplete(energyRows), energyOverlap), rejected, relationships.distinct())
    }

    private fun overlaps(a: Evidence, b: Evidence) = a.windows.any { x -> b.windows.any { y -> x.start < y.end && y.start < x.end } }
    private fun same(a: String, b: String) = runCatching { Instant.parse(a) == Instant.parse(b) }.getOrDefault(false)
    private fun parse(raw: String?) = raw?.let { runCatching { JSONObject(it) }.getOrNull() }
    private fun strings(data: JSONObject, key: String): Set<String> = data.optJSONArray(key)?.let { array ->
        (0 until array.length()).map { array.optString(it) }.filter(String::isNotBlank).toSet()
    }.orEmpty()
    fun label(provider: String) = if (provider == WEAR) "TrainPilot WearOS" else NativeWorkoutHealthSource.label(provider)
}
