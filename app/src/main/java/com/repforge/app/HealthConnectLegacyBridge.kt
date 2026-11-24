package com.repforge.app

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * The Android 9–13 Health Connect APK adapter. It publishes into the SAME HC journal
 * and daily schema as the platform implementation on Android 14+. No Samsung fallback,
 * synthetic metrics, mixing data sources, or separate user database.
 */
internal object HealthConnectLegacyBridge {
    @Volatile private var granted: Set<String> = emptySet()
    @Volatile private var hasFreshGrants = false

    fun available(context: Context): Boolean = Build.VERSION.SDK_INT in 28..33 &&
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun cachedAllowed(key: String): Boolean = hasFreshGrants && key in granted

    suspend fun refreshGrants(context: Context): Set<String> = withContext(Dispatchers.IO) {
        if (!available(context)) {
            granted = emptySet()
            hasFreshGrants = false
            return@withContext emptySet()
        }
        // A denial/revocation must be re-read from the APK, not inferred from Android runtime grants.
        val permissions = HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        val result = HealthConnectRepository.READ_PERMS.filter {
            "android.permission.health.$it" in permissions
        }.toSet()
        granted = result
        hasFreshGrants = true
        result
    }

    private fun types(): List<Pair<String, KClass<out Record>>> = listOf(
        "READ_STEPS" to StepsRecord::class,
        "READ_HEART_RATE" to HeartRateRecord::class,
        "READ_RESTING_HEART_RATE" to RestingHeartRateRecord::class,
        "READ_ACTIVE_CALORIES_BURNED" to ActiveCaloriesBurnedRecord::class,
        "READ_TOTAL_CALORIES_BURNED" to TotalCaloriesBurnedRecord::class,
        "READ_DISTANCE" to DistanceRecord::class,
        "READ_EXERCISE" to ExerciseSessionRecord::class,
        "READ_SLEEP" to SleepSessionRecord::class,
        "READ_HEART_RATE_VARIABILITY" to HeartRateVariabilityRmssdRecord::class,
        "READ_WEIGHT" to WeightRecord::class,
        "READ_BODY_FAT" to BodyFatRecord::class,
        "READ_OXYGEN_SATURATION" to OxygenSaturationRecord::class,
        "READ_VO2_MAX" to Vo2MaxRecord::class,
        "READ_SPEED" to SpeedRecord::class,
        "READ_BLOOD_PRESSURE" to BloodPressureRecord::class,
        "READ_BLOOD_GLUCOSE" to BloodGlucoseRecord::class,
        "READ_RESPIRATORY_RATE" to RespiratoryRateRecord::class
    )

    private suspend fun <T : Record> readAll(
        client: HealthConnectClient, type: KClass<T>, start: Instant, end: Instant,
        guard: () -> Unit
    ): List<T> {
        val result = ArrayList<T>()
        val seenTokens = HashSet<String>()
        var token: String? = null
        repeat(30) {
            guard()
            val response = client.readRecords(ReadRecordsRequest(
                recordType = type,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                pageSize = 1000,
                pageToken = token
            ))
            result.addAll(response.records)
            token = response.pageToken?.takeIf(String::isNotEmpty) ?: return result
            check(seenTokens.add(requireNotNull(token))) { "Repeated legacy HC page token" }
        }
        error("Legacy Health Connect record pagination exceeds safety limit")
    }

    private fun originalSource(record: Record) = record.metadata.dataOrigin.packageName

    private fun serialized(record: Record): JSONObject {
        val start: Instant
        val end: Instant
        val data = JSONObject()
        when (record) {
            is StepsRecord -> { start = record.startTime; end = record.endTime
                data.put("value", record.count).put("unit", "steps") }
            is ActiveCaloriesBurnedRecord -> { start = record.startTime; end = record.endTime
                data.put("value", record.energy.inKilocalories).put("unit", "kcal").put("energyType", "active") }
            is TotalCaloriesBurnedRecord -> { start = record.startTime; end = record.endTime
                data.put("value", record.energy.inKilocalories).put("unit", "kcal").put("energyType", "total") }
            is DistanceRecord -> { start = record.startTime; end = record.endTime
                data.put("value", record.distance.inMeters).put("unit", "m") }
            is HeartRateRecord -> { start = record.startTime; end = record.endTime
                val samples = JSONArray()
                record.samples.forEach { samples.put(JSONObject().put("timeMs", it.time.toEpochMilli())
                    .put("value", it.beatsPerMinute)) }
                data.put("samples", samples).put("unit", "bpm") }
            is RestingHeartRateRecord -> { start = record.time; end = start
                data.put("value", record.beatsPerMinute).put("unit", "bpm") }
            is HeartRateVariabilityRmssdRecord -> { start = record.time; end = start
                data.put("value", record.heartRateVariabilityMillis).put("unit", "ms") }
            is WeightRecord -> { start = record.time; end = start
                data.put("value", record.weight.inKilograms).put("unit", "kg") }
            is BodyFatRecord -> { start = record.time; end = start
                data.put("value", record.percentage.value).put("unit", "%") }
            is OxygenSaturationRecord -> { start = record.time; end = start
                data.put("value", record.percentage.value).put("unit", "%") }
            is Vo2MaxRecord -> { start = record.time; end = start
                data.put("value", record.vo2MillilitersPerMinuteKilogram).put("unit", "ml/kg/min")
                    .put("method", record.measurementMethod) }
            is BloodPressureRecord -> { start = record.time; end = start
                data.put("systolic", record.systolic.inMillimetersOfMercury)
                    .put("diastolic", record.diastolic.inMillimetersOfMercury).put("unit", "mmHg") }
            is BloodGlucoseRecord -> { start = record.time; end = start
                data.put("value", record.level.inMillimolesPerLiter).put("unit", "mmol/L") }
            is RespiratoryRateRecord -> { start = record.time; end = start
                data.put("value", record.rate).put("unit", "/min") }
            is SpeedRecord -> { start = record.startTime; end = record.endTime
                val samples = JSONArray()
                record.samples.forEach { samples.put(JSONObject().put("timeMs", it.time.toEpochMilli())
                    .put("value", it.speed.inMetersPerSecond)) }
                data.put("samples", samples).put("unit", "m/s") }
            is ExerciseSessionRecord -> { start = record.startTime; end = record.endTime
                data.put("value", Duration.between(start, end).toMillis() / 60_000.0)
                    .put("unit", "min").put("exerciseType", record.exerciseType)
                    .put("title", record.title ?: JSONObject.NULL)
                    .put("clientRecordId", record.metadata.clientRecordId ?: JSONObject.NULL)
                    .put("device", record.metadata.device?.let { device ->
                        JSONObject().put("manufacturer", device.manufacturer ?: JSONObject.NULL)
                            .put("model", device.model ?: JSONObject.NULL).put("type", device.type)
                    } ?: JSONObject.NULL)
                    .put("segments", JSONArray().apply {
                        for (segment in record.segments) put(externalWorkoutSegment(
                            segment.startTime, segment.endTime, segment.segmentType, segment.repetitions).apply {
                            if (segment.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE) put("pause", true)
                        })
                    })
                    .put("laps", JSONArray().apply {
                        for (lap in record.laps) put(externalWorkoutLap(
                            lap.startTime, lap.endTime, lap.length?.inMeters))
                    }) }
            is SleepSessionRecord -> { start = record.startTime; end = record.endTime
                val stages = JSONArray()
                val sleeps = mutableListOf<Pair<Long, Long>>()
                for (stage in record.stages) {
                    val a = max(start.toEpochMilli(), stage.startTime.toEpochMilli())
                    val b = min(end.toEpochMilli(), stage.endTime.toEpochMilli())
                    val asleep = stage.stage in listOf(
                        SleepSessionRecord.STAGE_TYPE_SLEEPING,
                        SleepSessionRecord.STAGE_TYPE_LIGHT,
                        SleepSessionRecord.STAGE_TYPE_DEEP,
                        SleepSessionRecord.STAGE_TYPE_REM
                    )
                    stages.put(JSONObject().put("startMs", a).put("endMs", b)
                        .put("type", stage.stage).put("isAsleep", asleep))
                    if (asleep && b > a) sleeps += a to b
                }
                val sorted = sleeps.sortedBy { it.first }
                var totalMs = 0L
                var a = -1L
                var b = -1L
                for ((x,y) in sorted) {
                    if (a < 0) { a = x; b = y }
                    else if (x <= b) b = max(b,y)
                    else { totalMs += b-a; a=x; b=y }
                }
                if (a >= 0) totalMs += b-a
                data.put("value", (if (record.stages.isEmpty()) Duration.between(start,end).toMillis()
                    else totalMs) / 60_000.0).put("unit", "min")
                    .put("durationKind", if (record.stages.isEmpty()) "session" else "asleep_stages")
                    .put("stages", stages)
            }
            else -> throw IllegalArgumentException("Unsupported legacy HC record")
        }
        return JSONObject()
            .put("recordId", record.metadata.id)
            .put("channel", HealthJournalStore.HC)
            .put("source", originalSource(record))
            .put("type", record.javaClass.simpleName)
            .put("startMs", start.toEpochMilli())
            .put("endMs", end.toEpochMilli())
            .put("lastModifiedMs", record.metadata.lastModifiedTime.toEpochMilli())
            .put("day", HealthJournalStore.day((if (record is SleepSessionRecord) end else start).toEpochMilli()))
            .put("data", data)
            .apply {
                if (record is ExerciseSessionRecord) {
                    record.startZoneOffset?.let { put("startZoneOffsetSeconds", it.totalSeconds) }
                    record.endZoneOffset?.let { put("endZoneOffsetSeconds", it.totalSeconds) }
                }
            }
    }

    private fun latest(rows: List<Record>, type: KClass<out Record>): Record? =
        rows.filter { type.isInstance(it) }.maxByOrNull { serialized(it).optLong("startMs") }

    private fun field(out: JSONObject, key: String, value: Any?) {
        out.put(key, value ?: JSONObject.NULL)
    }

    suspend fun readDay(
        context: Context, start: Instant, end: Instant, guard: () -> Unit,
        expectedGeneration: Long, commit: ((() -> Unit) -> Unit), stepsEnd: Instant = end
    ): JSONObject = withContext(Dispatchers.IO) {
        check(available(context)) { "Health Connect APK unavailable; install or update Health Connect" }
        check(end.isAfter(start) && Duration.between(start,end).toHours() <= 36)
        val keys = refreshGrants(context)
        check(keys.isNotEmpty()) { "Health Connect read permission required" }
        val client = HealthConnectClient.getOrCreate(context)
        val journal = HealthJournalStore.get(context)
        val warnings = JSONArray()
        if (keys.size < HealthConnectRepository.READ_PERMS.size)
            warnings.put("Partial Health Connect read permission")
        val all = mutableListOf<Record>()
        val byType = linkedMapOf<String, List<Record>>()
        for ((perm,type) in types()) {
            guard()
            if (perm !in keys) continue
            try {
                val readAt = System.currentTimeMillis()
                val rows = readAll(client, type, start, end, guard)
                    .filter { originalSource(it) != context.packageName }
                val encoded = JSONArray()
                rows.forEach { encoded.put(serialized(it)) }
                commit {
                    journal.reconcileRecords(encoded, type.simpleName!!, start.toEpochMilli(),
                        end.toEpochMilli(), readAt, expectedGeneration)
                }
                all.addAll(rows)
                byType[perm] = rows
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                warnings.put("$perm: " + (error.message ?: error.javaClass.simpleName))
            }
        }
        val origins = all.map(::originalSource).filter(String::isNotBlank).distinct().sorted()
        val requested = journal.preferences().optString("hcDailySource", "auto")
        val activityOrigin = HealthDailySource.choose("", requested, origins.toSet())
        val filterOrigins = if (activityOrigin.isBlank()) emptySet()
            else setOf(DataOrigin(activityOrigin))
        val range = TimeRangeFilter.between(start, end)
        val out = JSONObject()
            .put("provider", HealthJournalStore.HC)
            .put("channel", HealthJournalStore.HC)
            .put("start", start.toString()).put("end", end.toString())
            .put("source", "@aggregate").put("activityOrigin", activityOrigin)
            .put("aggregateOrigin", "health_connect_priority")
            .put("availableStepOrigins", JSONArray(byType["READ_STEPS"].orEmpty().map(::originalSource).distinct()))
            .put("sources", JSONArray(origins))
            .put("permissions", JSONObject().also { p ->
                HealthConnectRepository.READ_PERMS.forEach { p.put(it, it in keys) }
            })
            .put("warnings", warnings)
        suspend fun aggregate(metric: androidx.health.connect.client.aggregate.AggregateMetric<*>,
            timeRange: TimeRangeFilter = range): Any? {
            // Typed metrics are read via a single SDK aggregate response, never raw-row sums.
            val value = client.aggregate(AggregateRequest(
                metrics = setOf(metric), timeRangeFilter = timeRange,
                dataOriginFilter = filterOrigins
            ))
            return value[metric]
        }
        suspend fun measure(permission: String, key: String, get: suspend () -> Any?) {
            if (permission !in keys) { out.put(key, JSONObject.NULL); return }
            try { guard(); field(out,key,get()) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                out.put(key, JSONObject.NULL)
                warnings.put("$permission: " + (error.message ?: error.javaClass.simpleName))
            }
        }
        measure("READ_STEPS","steps") { aggregate(StepsRecord.COUNT_TOTAL, TimeRangeFilter.between(start, stepsEnd)) }
        measure("READ_ACTIVE_CALORIES_BURNED","activeCalories") {
            (aggregate(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL) as? androidx.health.connect.client.units.Energy)?.inKilocalories
        }
        measure("READ_TOTAL_CALORIES_BURNED","totalCalories") {
            (aggregate(TotalCaloriesBurnedRecord.ENERGY_TOTAL) as? androidx.health.connect.client.units.Energy)?.inKilocalories
        }
        measure("READ_DISTANCE","distanceMeters") {
            (aggregate(DistanceRecord.DISTANCE_TOTAL) as? androidx.health.connect.client.units.Length)?.inMeters
        }
        measure("READ_HEART_RATE","averageHeartRate") { aggregate(HeartRateRecord.BPM_AVG) }
        measure("READ_HEART_RATE","minHeartRate") { aggregate(HeartRateRecord.BPM_MIN) }
        measure("READ_HEART_RATE","maxHeartRate") { aggregate(HeartRateRecord.BPM_MAX) }
        measure("READ_HEART_RATE","heartRateSamples") { aggregate(HeartRateRecord.MEASUREMENTS_COUNT) }
        measure("READ_RESTING_HEART_RATE","restingHeartRate") { aggregate(RestingHeartRateRecord.BPM_AVG) }
        val pulseHistory = JSONArray()
        byType["READ_HEART_RATE"].orEmpty().filterIsInstance<HeartRateRecord>()
            .flatMap { record -> record.samples.map { sample ->
                Triple(sample.time, sample.beatsPerMinute, originalSource(record))
            } }.distinct().sortedBy { it.first }.takeLast(96).forEach { (at,bpm,src) ->
                pulseHistory.put(JSONObject().put("time", at.toString()).put("bpm",bpm).put("source",src))
            }
        out.put("heartRateSeries",pulseHistory)
        val sleeps = JSONArray()
        byType["READ_SLEEP"].orEmpty().filterIsInstance<SleepSessionRecord>().forEach { session ->
            val encoded = serialized(session)
            val phases = JSONArray()
            val raw = encoded.getJSONObject("data").getJSONArray("stages")
            for (i in 0 until raw.length()) {
                val s = raw.getJSONObject(i)
                phases.put(JSONObject().put("start", Instant.ofEpochMilli(s.getLong("startMs")).toString())
                    .put("end", Instant.ofEpochMilli(s.getLong("endMs")).toString())
                    .put("isAsleep", s.optBoolean("isAsleep"))
                    .put("stage", s.optInt("type")))
            }
            sleeps.put(JSONObject().put("start",session.startTime.toString())
                .put("end",session.endTime.toString()).put("source",originalSource(session))
                .put("stages",phases))
        }
        out.put("sleepSessions",sleeps)
        val sessions = JSONArray()
        byType["READ_EXERCISE"].orEmpty().filterIsInstance<ExerciseSessionRecord>().forEach {
            sessions.put(ExternalWorkout.fromRecord(serialized(it)).summary())
        }
        out.put("exerciseSessions",sessions)
        val single = listOf(
            Triple("READ_HEART_RATE_VARIABILITY","hrvRmssdMs","hrv"),
            Triple("READ_WEIGHT","weightKg","weight"),
            Triple("READ_BODY_FAT","bodyFatPercent","bodyFat"),
            Triple("READ_OXYGEN_SATURATION","oxygenSaturationPercent","oxygenSaturation"),
            Triple("READ_VO2_MAX","vo2Max","vo2Max"),
            Triple("READ_RESPIRATORY_RATE","respiratoryRate","respiratoryRate"),
            Triple("READ_BLOOD_GLUCOSE","bloodGlucoseMmolL","bloodGlucose")
        )
        for ((perm,key,prefix) in single) {
            val row = byType[perm].orEmpty().maxByOrNull { serialized(it).getLong("startMs") }
            val r = row?.let(::serialized)
            field(out,key,if (r==null) null else r.getJSONObject("data").opt("value"))
            if (r!=null) {
                out.put(prefix+"Time",Instant.ofEpochMilli(r.getLong("startMs")).toString())
                    .put(prefix+"Source",r.getString("source"))
                    .put(prefix+"Id",r.getString("recordId"))
            }
        }
        val pressure = byType["READ_BLOOD_PRESSURE"].orEmpty()
            .filterIsInstance<BloodPressureRecord>().maxByOrNull { it.time }
        field(out,"bloodPressureSystolic",pressure?.systolic?.inMillimetersOfMercury)
        field(out,"bloodPressureDiastolic",pressure?.diastolic?.inMillimetersOfMercury)
        if(pressure!=null) {
            out.put("bloodPressureTime",pressure.time.toString())
                .put("bloodPressureSource",originalSource(pressure))
                .put("bloodPressureId",pressure.metadata.id)
        }
        guard()
        out.put("readAt",Instant.now().toString())
            .put("hcReadDiagnosis",HealthConnectReadDiagnosis.inspect(out,requested))
        commit { journal.healthDay(HealthJournalStore.day(start.toEpochMilli()),out,expectedGeneration) }
        out
    }

    /**
     * Bounded workout-window read for pre-14 devices; it is NOT a daily sync, so it must
     * never overwrite the full-day journal. External session-specific calories remain
     * distinct from the aggregate daily energy and from TrainPilot's own exercise writes.
     */
    suspend fun readTrainingWindow(
        context: Context, start: Instant, end: Instant, guard: () -> Unit
    ): JSONObject = withContext(Dispatchers.IO) {
        check(available(context))
        check(end.isAfter(start) && Duration.between(start,end) <= Duration.ofHours(24))
        val keys = refreshGrants(context)
        check(keys.isNotEmpty()) { "Health Connect read permission required" }
        val client = HealthConnectClient.getOrCreate(context)
        val warnings = JSONArray()
        val out = JSONObject().put("provider",HealthJournalStore.HC)
            .put("source","health_connect").put("start",start.toString())
            .put("end",end.toString()).put("warnings",warnings)
        val timeRange = TimeRangeFilter.between(start,end)
        suspend fun <T : Any> value(metric: androidx.health.connect.client.aggregate.AggregateMetric<T>): T? {
            guard()
            return client.aggregate(AggregateRequest(setOf(metric),timeRange))[metric]
        }
        suspend fun measure(permission: String, key: String, block: suspend () -> Any?) {
            if (permission !in keys) { out.put(key,JSONObject.NULL); return }
            try { out.put(key, block() ?: JSONObject.NULL) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) { out.put(key,JSONObject.NULL)
                warnings.put("$permission: "+(e.message ?: e.javaClass.simpleName)) }
        }
        measure("READ_STEPS","steps") { value(StepsRecord.COUNT_TOTAL) }
        measure("READ_HEART_RATE","averageHeartRate") { value(HeartRateRecord.BPM_AVG) }
        measure("READ_HEART_RATE","minHeartRate") { value(HeartRateRecord.BPM_MIN) }
        measure("READ_HEART_RATE","maxHeartRate") { value(HeartRateRecord.BPM_MAX) }
        measure("READ_HEART_RATE","heartRateSamples") { value(HeartRateRecord.MEASUREMENTS_COUNT) }
        measure("READ_ACTIVE_CALORIES_BURNED","activeCalories") {
            value(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories
        }
        measure("READ_TOTAL_CALORIES_BURNED","totalCalories") {
            value(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories
        }
        measure("READ_DISTANCE","distanceMeters") { value(DistanceRecord.DISTANCE_TOTAL)?.inMeters }
        suspend fun <T : Record> safeRaw(permission: String, type: KClass<T>): List<T> {
            if (permission !in keys) return emptyList()
            return try { readAll(client,type,start,end,guard)
                .filter { originalSource(it) != context.packageName } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch(e: Exception) {
                warnings.put("$permission: "+(e.message ?: e.javaClass.simpleName))
                emptyList()
            }
        }
        val exercises = safeRaw("READ_EXERCISE",ExerciseSessionRecord::class)
        val active = safeRaw("READ_ACTIVE_CALORIES_BURNED",ActiveCaloriesBurnedRecord::class)
        val total = safeRaw("READ_TOTAL_CALORIES_BURNED",TotalCaloriesBurnedRecord::class)
        val speeds = safeRaw("READ_SPEED",SpeedRecord::class)
        val sessionsJson = JSONArray()
        val rawSessions = exercises.map { record ->
            sessionsJson.put(ExternalWorkout.fromRecord(serialized(record)).summary())
            HealthWorkoutEnergy.Session(originalSource(record),
                record.startTime.toEpochMilli(),record.endTime.toEpochMilli())
        }
        val rawEnergy = (active.map { row ->
            HealthWorkoutEnergy.EnergyRow(row.metadata.id,originalSource(row),
                row.startTime.toEpochMilli(),row.endTime.toEpochMilli(),
                row.energy.inKilocalories,false,row.metadata.lastModifiedTime.toEpochMilli())
        } + total.map { row ->
            HealthWorkoutEnergy.EnergyRow(row.metadata.id,originalSource(row),
                row.startTime.toEpochMilli(),row.endTime.toEpochMilli(),
                row.energy.inKilocalories,true,row.metadata.lastModifiedTime.toEpochMilli())
        })
        val energy = HealthWorkoutEnergy.match(rawSessions,rawEnergy,start.toEpochMilli(),
            end.toEpochMilli(),context.packageName)
        out.put("exerciseSessions",sessionsJson)
            .put("exerciseSessionCount",exercises.size)
            .put("exerciseMinutes",exercises.sumOf {
                maxOf(0L,Duration.between(maxOf(start,it.startTime),minOf(end,it.endTime)).toMinutes())
            })
            .put("workoutEnergyVersion",1)
            .put("workoutEnergyProvider",HealthJournalStore.HC)
            .put("workoutCalories",energy.calories ?: JSONObject.NULL)
            .put("workoutEnergyCoverageMs",energy.coverageMs)
            .put("workoutEnergyProrated",energy.prorated)
            .put("workoutEnergySources",JSONArray(energy.sources))
            .put("workoutEnergyTypes",JSONArray(energy.types))
            .put("workoutEnergyRecordIds",JSONArray(energy.recordIds))
        val speedReadings = speeds.flatMap { it.samples }.filter {
            !it.time.isBefore(start) && it.time.isBefore(end)
        }.map { it.speed.inMetersPerSecond }.filter { it.isFinite() && it >= 0 }
        out.put("averageSpeedMps",speedReadings.takeIf { it.isNotEmpty() }?.average() ?: JSONObject.NULL)
            .put("maxSpeedMps",speedReadings.maxOrNull() ?: JSONObject.NULL)
        guard()
        out
    }
}
