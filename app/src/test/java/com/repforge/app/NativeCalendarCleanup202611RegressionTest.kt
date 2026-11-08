package com.repforge.app

import androidx.test.core.app.ApplicationProvider
import com.repforge.app.data.db.CalendarScheduleEntity
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeCalendarCleanup202611RegressionTest {
    private fun row(
        id: String,
        date: String,
        time: String = "10:00",
        status: String = "planned",
        programName: String = "Otthoni A/B – Alap",
        dayName: String = "Alap A",
        updatedAt: Long = 1L,
        cancelled: Boolean = false
    ) = CalendarScheduleEntity(
        id = id,
        googleEventId = "rf" + id.padEnd(64, 'a').take(64),
        date = date,
        startIso = "${date}T${time}:00Z",
        endIso = "${date}T${time.substringBefore(':').toInt().plus(1).toString().padStart(2, '0')}:${time.substringAfter(':')}:00Z",
        programId = if (programName.isBlank()) "" else "program",
        programName = programName,
        dayId = if (dayName.isBlank()) "" else "A",
        dayName = dayName,
        status = status,
        updatedAt = updatedAt,
        rawJson = JSONObject()
            .put("id", id)
            .put("date", date)
            .put("status", status)
            .put("cancelled", cancelled)
            .toString()
    )

    @Test fun historicalRowsAreDroppedButDifferentTimesOnSameDayRemain() {
        val cleaned = NativeCalendarCleanup202611.cleanRows(
            listOf(
                row("old", "2026-11-01"),
                row("nov2-planned", "2026-11-02", "10:00", status = "planned", updatedAt = 20),
                row("nov2-completed", "2026-11-02", "10:00", status = "completed", programName = "", dayName = "", updatedAt = 10),
                row("nov2-evening", "2026-11-02", "18:00", dayName = "Alap B"),
                row("nov4-legacy", "2026-11-04", "10:00", programName = "", dayName = "", updatedAt = 30),
                row("nov4-native", "2026-11-04", "10:00", programName = "Otthoni A/B – Alap", dayName = "Alap A", updatedAt = 10),
                row("dec1-a", "2026-12-01", "10:00"),
                row("dec1-b", "2026-12-01", "18:00", dayName = "Alap B")
            )
        )

        assertEquals(
            setOf("nov2-completed", "nov2-evening", "nov4-native", "dec1-a", "dec1-b"),
            cleaned.map { it.id }.toSet()
        )
        assertEquals(2, cleaned.count { it.date == "2026-11-02" })
        assertEquals(2, cleaned.count { it.date == "2026-12-01" })
        assertTrue(cleaned.none { it.date <= "2026-11-01" })
    }

    @Test fun driveMergeCannotResurrectHistoricalRowsOrSameSlotDuplicates() {
        fun scheduled(
            id: String,
            date: String,
            time: String = "10:00",
            status: String = "planned",
            rich: Boolean = true
        ) = JSONObject()
            .put("id", id)
            .put("scheduleId", id)
            .put("date", date)
            .put("start", "${date}T${time}:00Z")
            .put("end", "${date}T${time.substringBefore(':').toInt().plus(1).toString().padStart(2, '0')}:${time.substringAfter(':')}:00Z")
            .put("status", status)
            .put("updatedAt", if (rich) 100 else 200)
            .apply {
                if (rich) {
                    put("programId", "program")
                    put("programName", "Otthoni A/B – Alap")
                    put("dayId", "A")
                    put("dayName", "Alap A")
                }
            }

        val local = JSONObject().put("scheduled", JSONArray()
            .put(scheduled("local-old", "2026-10-30"))
            .put(scheduled("local-nov4", "2026-11-04", "10:00"))
            .put(scheduled("local-nov4-evening", "2026-11-04", "18:00")))
        val remote = JSONObject().put("scheduled", JSONArray()
            .put(scheduled("remote-old", "2026-10-30"))
            .put(scheduled("remote-nov4-legacy", "2026-11-04", "10:00", rich = false))
            .put(scheduled("remote-dec1-a", "2026-12-01", "10:00"))
            .put(scheduled("remote-dec1-dup", "2026-12-01", "10:00", rich = false))
            .put(scheduled("remote-dec1-evening", "2026-12-01", "18:00")))

        val merged = GoogleSyncRepository(ApplicationProvider.getApplicationContext()).mergeSync18(
            local = local,
            remotes = listOf(remote),
            base = null,
            archives = emptyList(),
            deletedHistoryIds = emptySet(),
            deletedHistoryKeys = emptySet(),
            preferLocalOnConflict = true
        )
        val scheduled = merged.getJSONArray("scheduled")
        val rows = (0 until scheduled.length()).map { scheduled.getJSONObject(it) }

        assertTrue(rows.none { it.getString("date") <= "2026-11-01" })
        assertEquals(2, rows.count { it.getString("date") == "2026-11-04" })
        assertEquals("local-nov4", rows.single {
            it.getString("date") == "2026-11-04" && it.getString("start").contains("T10:00")
        }.getString("id"))
        assertEquals(2, rows.count { it.getString("date") == "2026-12-01" })
    }

    @Test fun calendarSyncPlanAllowsSameDayDifferentTimesButEmitsOneWritePerStartSlot() {
        val entries = listOf(
            row("a", "2026-11-08", "10:00", updatedAt = 1),
            row("b", "2026-11-08", "10:00", updatedAt = 2, dayName = "Alap B"),
            row("c", "2026-11-08", "18:00", updatedAt = 3, dayName = "Alap A")
        )

        val operations = NativeCalendarSyncPlan.operations(entries).toList()

        assertEquals(2, operations.size)
        assertEquals(
            setOf("2026-11-08T10:00:00Z", "2026-11-08T18:00:00Z"),
            operations.mapNotNull { it.payload?.getJSONObject("start")?.getString("dateTime") }.toSet()
        )
    }
}
