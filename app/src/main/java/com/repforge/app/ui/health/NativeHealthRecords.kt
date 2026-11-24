package com.repforge.app.ui.health

import com.repforge.app.HealthJournalStore
import com.repforge.app.R
import org.json.JSONObject
import java.time.LocalDate
import java.time.Instant
import com.repforge.app.page
import com.repforge.app.pendingRestore

internal data class HealthRecordCursor(val id: Long, val time: Long) {
    init { require(id > 0 && time > 0) }
}
internal data class HealthRecordQuery(val day: LocalDate, val channel: String = "journal",
    val source: String = "", val type: String = "", val cursor: HealthRecordCursor? = null,
    val generation: Long? = null,
    // Appended to keep existing positional and named single-day callers source-compatible.
    val endDay: LocalDate = day) {
    val fromDay: LocalDate get() = day
    val toDay: LocalDate get() = endDay

    init {
        // Match HealthJournalStore.page's inclusive yyyy-MM-dd period contract.
        require(HealthJournalStore.validDay(fromDay.toString()) && HealthJournalStore.validDay(toDay.toString()) &&
            !toDay.isBefore(fromDay)) { "Invalid health period" }
        require(channel in setOf("journal", HealthJournalStore.HC, HealthJournalStore.SAMSUNG, HealthJournalStore.LEGACY, HealthJournalStore.BLE))
        require(source.length <= 255 && type.length <= 80)
        require(cursor == null || generation != null)
    }

    /** Filter changes always start a new 25-row cursor/generation scope. */
    fun withRange(from: LocalDate, to: LocalDate): HealthRecordQuery =
        copy(day = from, endDay = to, cursor = null, generation = null)

    fun withFilters(channel: String = this.channel, source: String = this.source,
        type: String = this.type): HealthRecordQuery =
        copy(channel = channel, source = source, type = type, cursor = null, generation = null)
}
internal data class HealthRecordRow(val id: String, val channel: String, val source: String,
    val type: String, val startMs: Long, val endMs: Long, val modifiedMs: Long, val data: String)
internal data class HealthRecordOrigin(val source: String, val name: String)
internal data class HealthRecordPage(val rows: List<HealthRecordRow>, val origins: List<HealthRecordOrigin>,
    val next: HealthRecordCursor?, val generation: Long)

/** Read-only adapter over the same health SQLite journal used by the real SDK providers. */
internal class NativeHealthRecords(private val store: HealthJournalStore) {
    class ReplacedJournal : IllegalStateException("The health journal changed; restart paging.")

    fun load(query: HealthRecordQuery): HealthRecordPage {
        val generation = store.generation()
        if (store.pendingRestore().isNotEmpty() || (query.generation != null && query.generation != generation))
            throw ReplacedJournal()
        // A small scan-ahead keeps each sleep page full even if old SDK-parent IDs
        // produced duplicate observations. Advance the raw cursor past the duplicates,
        // but never delete persisted source records or collapse different sleep stages.
        if (query.type == "SleepSessionRecord") {
            val seen = HashSet<String>()
            val distinct = mutableListOf<HealthRecordRow>()
            var cursor = query.cursor
            var origins = emptyList<HealthRecordOrigin>()
            var next: HealthRecordCursor? = null
            var hasAnother = false
            while (true) {
                val result = store.page(query.fromDay.toString(), query.toDay.toString(), query.channel,
                    query.source, cursor?.id ?: 0L, cursor?.time ?: 0L, 50, query.type, false)
                if (origins.isEmpty()) origins = originsFrom(result)
                val records = result.getJSONArray("records")
                for (index in 0 until records.length()) {
                    val json = records.getJSONObject(index)
                    val row = recordFrom(json)
                    val signature = NativeSleepRecordPresentation.duplicateKey(row)
                    if (signature != null && !seen.add(signature)) {
                        cursor = HealthRecordCursor(json.getLong("cursor"), json.getLong("cursorTime"))
                        continue
                    }
                    if (distinct.size == PAGE_SIZE) {
                        // Do not consume the 26th unique item: the next page starts here.
                        next = cursor
                        hasAnother = true
                        break
                    }
                    distinct.add(row)
                    cursor = HealthRecordCursor(json.getLong("cursor"), json.getLong("cursorTime"))
                }
                if (hasAnother) break
                if (result.getLong("nextCursor") <= 0L || records.length() == 0) break
            }
            if (generation != store.generation() || store.pendingRestore().isNotEmpty()) throw ReplacedJournal()
            return HealthRecordPage(distinct, origins, next, generation)
        }
        val result = store.page(query.fromDay.toString(), query.toDay.toString(), query.channel, query.source,
            query.cursor?.id ?: 0L, query.cursor?.time ?: 0L, PAGE_SIZE, query.type, false)
        if (generation != store.generation() || store.pendingRestore().isNotEmpty()) throw ReplacedJournal()
        val records = result.getJSONArray("records")
        val rows = (0 until records.length()).map { recordFrom(records.getJSONObject(it)) }
        val nextId = result.getLong("nextCursor")
        val next = if (nextId > 0) HealthRecordCursor(nextId, result.getLong("nextTime")) else null
        return HealthRecordPage(rows, originsFrom(result), next, generation)
    }

    private fun recordFrom(row: JSONObject): HealthRecordRow =
        HealthRecordRow(row.getString("recordId"), row.getString("channel"), row.getString("source"),
            row.getString("type"), row.getLong("startMs"), row.getLong("endMs"),
            row.getLong("lastModifiedMs"), row.getJSONObject("data").toString())

    private fun originsFrom(result: JSONObject): List<HealthRecordOrigin> {
        val sources = result.getJSONArray("sources")
        return (0 until sources.length()).map { index ->
            val source = sources.getJSONObject(index)
            HealthRecordOrigin(source.getString("source"), source.optString("name"))
        }.distinctBy { it.source }
    }

    companion object {
        const val PAGE_SIZE = 25
        fun title(type: String): Int = when (type) {
            "StepsRecord" -> R.string.health_metric_steps
            "ActiveCaloriesBurnedRecord" -> R.string.health_metric_activecalories
            "TotalCaloriesBurnedRecord" -> R.string.health_metric_totalcalories
            "DistanceRecord" -> R.string.health_metric_distancemeters
            "HeartRateRecord" -> R.string.health_record_heart_rate
            "RestingHeartRateRecord" -> R.string.health_metric_restingheartrate
            "HeartRateVariabilityRmssdRecord" -> R.string.health_metric_hrvrmssdms
            "WeightRecord" -> R.string.health_metric_weightkg
            "BodyFatRecord" -> R.string.health_metric_bodyfatpercent
            "OxygenSaturationRecord" -> R.string.health_metric_oxygensaturationpercent
            "Vo2MaxRecord" -> R.string.health_metric_vo2max
            "BloodPressureRecord" -> R.string.health_record_blood_pressure
            "BloodGlucoseRecord" -> R.string.health_metric_bloodglucosemmoll
            "RespiratoryRateRecord" -> R.string.health_metric_respiratoryrate
            "SleepSessionRecord" -> R.string.health_record_sleep
            "SpeedRecord" -> R.string.health_record_speed
            "ExerciseSessionRecord" -> R.string.health_record_exercise
            "EnergyScoreRecord" -> R.string.health_metric_energyscore
            "BodyFatMass" -> R.string.health_metric_bodyfatmasskg
            "SkeletalMuscleMass" -> R.string.health_metric_skeletalmusclemasskg
            "FatFreeMass" -> R.string.health_metric_fatfreemasskg
            "TotalBodyWater" -> R.string.health_metric_totalbodywaterliters
            "BasalMetabolicRate" -> R.string.health_metric_basalmetabolicratekcal
            "BodyMassIndex" -> R.string.health_metric_bodymassindex
            else -> R.string.health_record_measurement
        }
        fun number(data: JSONObject, key: String): Double? {
            val value = data.opt(key)
            val number = when (value) { is Number -> value.toDouble(); is String -> value.trim().toDoubleOrNull(); else -> null }
            return number?.takeIf { it.isFinite() }
        }
    }
}

internal data class HealthSeriesItem(val startMs: Long?, val endMs: Long?, val value: Double?, val stage: String?)
internal data class HealthSeriesPage(val items: List<HealthSeriesItem>, val total: Int)

/** Only the visible samples/stages are materialized; opening a large record does not create every UI row. */
internal object NativeHealthRecordSeries {
    const val PAGE_SIZE = 20
    fun page(data: JSONObject, offset: Int): HealthSeriesPage {
        require(offset >= 0)
        val stages = data.optJSONArray("stages")
        val array = stages ?: data.optJSONArray("samples")
        val count = array?.length() ?: 0
        val rows = (offset until minOf(offset.toLong() + PAGE_SIZE, count.toLong()).toInt()).map { index ->
            val item = array!!.optJSONObject(index) ?: JSONObject()
            fun time(key: String, isoKey: String): Long? =
                NativeHealthRecords.number(item, key)?.takeIf { it >= 0 && it < Long.MAX_VALUE.toDouble() }?.toLong()
                    ?: runCatching { Instant.parse(item.getString(isoKey)).toEpochMilli() }.getOrNull()
            HealthSeriesItem(time(if (stages == null) "timeMs" else "startMs", "start"),
                if (stages != null) time("endMs", "end") else null,
                if (stages == null) NativeHealthRecords.number(item, "value") else null,
                if (stages != null) (item.opt("stage") ?: item.opt("type"))?.takeIf { it != JSONObject.NULL }?.toString() else null)
        }
        return HealthSeriesPage(rows, count)
    }
    fun stageLabel(type: String?): Int = when (type) {
        "1", "7", "AWAKE", "AWAKE_IN_BED" -> R.string.health_sleep_awake
        "2", "SLEEPING" -> R.string.health_sleep_sleeping
        "3", "OUT_OF_BED" -> R.string.health_sleep_out_of_bed
        "4", "LIGHT" -> R.string.health_sleep_light
        "5", "DEEP" -> R.string.health_sleep_deep
        "6", "REM" -> R.string.health_sleep_rem
        else -> R.string.health_sleep_unknown
    }
}
