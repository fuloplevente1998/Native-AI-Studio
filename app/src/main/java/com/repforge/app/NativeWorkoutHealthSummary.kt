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
        val incomplete: Boolean, val overlapExcluded: Boolean) {
        val hasUnverifiedProviderCoverage: Boolean get() = evidence.any { it.provider != WEAR }
    }
    data class Result(val heart: Metric, val energy: Metric, val rejectedEvidence: Boolean,
        val relationships: List<Relationship>, val activeEnergy: Metric, val totalEnergy: Metric)
    data class Relationship(val first: String, val second: String, val confirmedMirror: Boolean)

    fun read(workout: WorkoutHistoryEntity): Result =
        project(savedFrame(workout), NativeSavedWearHealth.read(workout), parse(workout.health240Json))

    /** Provider-specific daily workout row and the combined detail share the same validated windows. */
    fun readProvider(workout: WorkoutHistoryEntity, provider: String): Result {
        val snapshot = NativeWorkoutHealthSource.snapshot(parse(workout.health240Json), provider)
        val selected = snapshot?.let { JSONObject().put("providers",JSONObject().put(provider,it)) }
        return project(savedFrame(workout),null,selected)
    }

    private fun savedFrame(workout: WorkoutHistoryEntity): JSONObject {
        val saved = JSONObject().put("started", workout.started).put("finished", workout.finished)
        workout.activeIntervalsJson?.let { saved.put("activeIntervals", JSONArray(it)) }
        val raw = parse(workout.rawJson)
        saved.put("nativeHealthWindowStale", raw?.optBoolean("nativeHealthWindowStale") == true)
        saved.put("id", workout.id).put("syncId", workout.syncId).put("workoutKey", workout.workoutKey)
        return saved
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
            val aliases = listOf("id", "syncId", "workoutKey", "workoutId").map { workout.optString(it) }.filter(String::isNotBlank)
            val snapshotId = snapshot.optString("workoutId")
            val valid = snapshot.optString("provider", provider) == provider &&
                (snapshotId.isBlank() || snapshotId in aliases) &&
                same(measured.optString("started"), workout.optString("started")) &&
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
        evidence.sortBy { if (it.provider == HealthJournalStore.HC) 0 else 1 }
        fun number(data: JSONObject, key: String) = (data.opt(key) as? Number)?.toDouble()
            ?.takeIf { it.isFinite() && it >= 0 && (!key.contains("HeartRate") || it > 0) }
        fun select(candidates: List<Evidence>, rank: (Evidence) -> Int = { 0 },
            separateHeartOrigins: Boolean = false): Pair<List<Evidence>, Boolean> {
            val primary = candidates.filter { it.provider == HealthJournalStore.HC }.ifEmpty { candidates }
            val ranked = primary.sortedByDescending(rank)
            val provider = ranked.firstOrNull()?.provider ?: return emptyList<Evidence>() to false
            val source = ranked.filter { it.provider == provider }
            val origin = if (separateHeartOrigins && provider == HealthJournalStore.HC)
                NativeWorkoutHeartRate.preferredOrigin(source.map { it.data })
            else null
            val selected = if (origin == null) source else source.filter { it.data.optString("heartRateOrigin") == origin }
            val accepted = mutableListOf<Evidence>()
            var overlap = false
            for (candidate in selected) {
                if (accepted.any { overlaps(it, candidate) }) overlap = true else accepted.add(candidate)
            }
            overlap = overlap || candidates.any { row ->
                row !in selected && accepted.any { overlaps(it, row) }
            }
            return accepted to overlap
        }
        val expectedMs = active.sumOf { Duration.between(it.start, it.end).toMillis() }
        fun energyRank(row: Evidence, key: String): Int =
            if (row.provider == WEAR && (row.data.optBoolean("partial") ||
                WearWorkoutEnergyEvidence.incompleteDuration(row.data,expectedMs))) 0
            else WorkoutEnergyCoverage.rank(row.data,key)
        fun incomplete(rows: List<Evidence>) = rows.isEmpty() || rows.sumOf { it.milliseconds } < expectedMs || rows.any {
            it.data.optBoolean("partial") || (it.data.optJSONArray("warnings")?.length() ?: 0) > 0 ||
                (it.data.optJSONArray("failedFields")?.length() ?: 0) > 0 ||
                (it.data.optJSONObject("errors")?.length() ?: 0) > 0 ||
                (it.provider == WEAR && (it.legacyWindow || WearWorkoutEnergyEvidence.incompleteDuration(it.data,expectedMs))) ||
                it.data.optBoolean("workoutEnergyProrated")
        }
        val (heartRows, heartOverlap) = select(evidence.filter { number(it.data, "averageHeartRate") != null },
            separateHeartOrigins = true)
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
        val (energyRows, energyOverlap) = select(energyCandidates.filter { energyKind(it) == kind },
            rank = { if (energyKey in setOf("activeCalories", "totalCalories"))
                energyRank(it, requireNotNull(energyKey)) else 0 })
        val energyValues = if (energyRows.isEmpty() || energyKey == null) emptyMap() else
            mapOf(energyKey to energyRows.sumOf { number(it.data, energyKey)!! })
        // Each energy definition independently uses the primary provider or one fallback.
        fun separateEnergy(key: String): Metric {
            val candidates = evidence.filter { number(it.data, key) != null }
            val (rows, overlap) = select(candidates, rank = { energyRank(it, key) })
            val values = if (rows.isEmpty()) emptyMap() else mapOf(key to rows.sumOf { number(it.data, key)!! })
            return Metric(values, rows, incomplete(rows) || rows.any {
                WorkoutEnergyCoverage.status(it.data,key) == WorkoutEnergyCoverage.Status.PARTIAL_RAW
            }, overlap)
        }
        val relationships = mutableListOf<Relationship>()
        for (i in evidence.indices) for (j in i + 1 until evidence.size) {
            val a = evidence[i]; val b = evidence[j]
            if (a.provider == b.provider || !overlaps(a, b)) continue
            val idsA = strings(a.data, "workoutEnergyRecordIds")
            val idsB = strings(b.data, "workoutEnergyRecordIds")
            relationships.add(Relationship(a.provider, b.provider, idsA.intersect(idsB).isNotEmpty()))
        }
        return Result(Metric(heartValues, heartRows, incomplete(heartRows) || heartValues.isEmpty(), heartOverlap),
            Metric(energyValues, energyRows, incomplete(energyRows) || energyRows.any {
                energyKey != null && WorkoutEnergyCoverage.status(it.data,energyKey) == WorkoutEnergyCoverage.Status.PARTIAL_RAW
            }, energyOverlap), rejected, relationships.distinct(),
            separateEnergy("activeCalories"), separateEnergy("totalCalories"))
    }

    private fun overlaps(a: Evidence, b: Evidence) = a.windows.any { x -> b.windows.any { y -> x.start < y.end && y.start < x.end } }
    private fun same(a: String, b: String) = runCatching { Instant.parse(a) == Instant.parse(b) }.getOrDefault(false)
    private fun parse(raw: String?) = raw?.let { runCatching { JSONObject(it) }.getOrNull() }
    private fun strings(data: JSONObject, key: String): Set<String> = data.optJSONArray(key)?.let { array ->
        (0 until array.length()).map { array.optString(it) }.filter(String::isNotBlank).toSet()
    }.orEmpty()
    fun label(provider: String) = if (provider == WEAR) "TrainPilot WearOS" else NativeWorkoutHealthSource.label(provider)
}
