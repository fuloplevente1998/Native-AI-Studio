package com.repforge.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppLanguage
import com.example.ui.components.HomePlanTiming
import com.example.ui.components.homePlannedDateLine
import com.repforge.app.data.db.*
import com.repforge.app.ui.coach.NativeCoachTargets
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** 2773: a plan for today stays the target all day (even after its time); the next one after midnight. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomePlannedTodayUntilMidnightRegressionTest {
    private val zone = ZoneId.of("Europe/Budapest")
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TrainPilotDatabase::class.java)
        .allowMainThreadQueries().build()

    @After fun close() = db.close()

    private fun program() = ProgramWithDays(
        ProgramEntity("p", "Otthoni A/B", "Home A/B", "Home A/B", "Home A/B", "home", "beginner", "", false, false,
            "custom", null, 1, "#7C3AED", 0, 1L, "{}"),
        listOf(ProgramDayEntity("p:a", "p", "a", "A nap", "Day A", "Tag A", "Ziua A", 0, "[]"),
            ProgramDayEntity("p:b", "p", "b", "B nap", "Day B", "Tag B", "Ziua B", 1, "[]")))

    private fun plan(id: String, local: ZonedDateTime, dayId: String) = CalendarScheduleEntity(
        id = id, googleEventId = "rf" + id.padEnd(64, 'a').take(64), date = local.toLocalDate().toString(),
        startIso = local.toInstant().toString(), endIso = local.plusHours(1).toInstant().toString(),
        programId = "p", programName = "Otthoni A/B", dayId = dayId, dayName = dayId, status = "planned",
        updatedAt = 1L, rawJson = JSONObject().put("id", id).put("cancelled", false).toString())

    @Test fun todaysPlanStaysAfterItsTimeUntilMidnightThenTheNextPlanIsSelected() = runBlocking {
        val today10 = ZonedDateTime.of(2026, 11, 19, 10, 0, 0, 0, zone)
        db.calendarScheduleDao().upsertEntries(listOf(plan("today", today10, "b"), plan("later", today10.plusDays(2), "a")))
        val targets = NativeCoachTargets(db)
        fun at(t: ZonedDateTime) = runBlocking { targets.select("p", "hu", t.toInstant(), zone, listOf(program())) }
        assertEquals("today", at(today10.withHour(9))?.schedule?.id)
        assertEquals("today", at(today10.withHour(10).withMinute(1))?.schedule?.id)
        assertEquals("today", at(today10.withHour(23).withMinute(59))?.schedule?.id)
        // After local midnight the missed plan is no longer "today": the next planned one wins.
        assertEquals("later", at(today10.plusDays(1).withHour(0).withMinute(1))?.schedule?.id)
    }

    @Test fun dateLineShowsTodayStillDoableNextAndCycle() {
        val now = ZonedDateTime.of(2026, 11, 19, 14, 30, 0, 0, zone)
        fun iso(t: ZonedDateTime) = t.toInstant().toString()
        assertEquals(HomePlanTiming.TODAY_UPCOMING, homePlannedDateLine(iso(now.withHour(18).withMinute(0)), now, AppLanguage.HU).timing)
        val passed = homePlannedDateLine(iso(now.withHour(10).withMinute(0)), now, AppLanguage.HU)
        assertEquals(HomePlanTiming.TODAY_PASSED, passed.timing)
        assertEquals("📅 Ma, 10:00 · még ma elvégezhető", passed.text)
        assertEquals("📅 Holnap, 10:00", homePlannedDateLine(iso(now.plusDays(1).withHour(10).withMinute(0)), now, AppLanguage.HU).text)
        assertTrue(homePlannedDateLine(iso(now.plusDays(2).withHour(10).withMinute(0)), now, AppLanguage.HU).text.startsWith("📅 Következő: "))
        assertEquals(HomePlanTiming.CYCLE, homePlannedDateLine(null, now, AppLanguage.HU).timing)
    }

    @Test fun coachCompactDateOnlyRemovesRedundantFuturePrefix() {
        val now = ZonedDateTime.of(2026, 11, 19, 14, 30, 0, 0, zone)
        val starts = listOf(null, now.minusHours(1).toInstant().toString(),
            now.plusHours(1).toInstant().toString(), now.plusDays(1).toInstant().toString())
        for (language in AppLanguage.entries) {
            for (start in starts) assertEquals(homePlannedDateLine(start, now, language),
                homePlannedDateLine(start, now, language, compact = true))
            val future = now.plusDays(2).toInstant().toString()
            val normal = homePlannedDateLine(future, now, language)
            val compact = homePlannedDateLine(future, now, language, compact = true)
            assertEquals(normal.timing, compact.timing)
            assertEquals("📅 " + normal.text.substringAfter(": "), compact.text)
        }
    }

    @Test fun dayChangesEmitsTheLocalDate() = runBlocking {
        val fixed = Instant.parse("2026-11-19T22:59:00Z") // 23:59 in Budapest
        val first = com.repforge.app.ui.home.NativeHomeRepository.dayChanges({ zone }, { fixed }).first()
        assertEquals(java.time.LocalDate.of(2026, 11, 19), first)
    }
}
