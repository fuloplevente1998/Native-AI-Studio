package com.example

import com.example.model.StudioProfileCompletion
import com.repforge.app.NativeWorkoutActiveWindows
import com.repforge.app.StudioWorkoutBridge
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudioNativeRegressionTest {
    private fun ms(time: String) = Instant.parse("2026-11-01T${time}:00Z").toEpochMilli()
    private fun started() = JSONObject().put("started", "2026-11-01T10:00:00Z")
        .put("workoutId", "original-session").put("workoutPaused", false)
        .put("activeIntervals", JSONArray().put(JSONObject().put("start", "2026-11-01T10:00:00Z").put("end", JSONObject.NULL)))

    @Test fun longPauseAndDuplicateCommandsPreserveExactly45ActiveMinutes() {
        val paused = StudioWorkoutBridge.transition(started(), true, ms("10:25"))
        val duplicate = StudioWorkoutBridge.transition(paused, true, ms("12:00"))
        assertEquals(paused.toString(), duplicate.toString())
        val resumed = StudioWorkoutBridge.transition(duplicate, false, ms("16:00"))
        val duplicateResume = StudioWorkoutBridge.transition(resumed, false, ms("16:01"))
        assertEquals(2, duplicateResume.getJSONArray("activeIntervals").length())
        val finished = StudioWorkoutBridge.transition(duplicateResume, true, ms("16:20"))
            .put("finished", "2026-11-01T16:20:00Z")
        assertEquals("original-session", finished.getString("workoutId"))
        assertEquals(45 * 60L, NativeWorkoutActiveWindows.seconds(JSONObject(finished.toString())))
    }

    @Test fun overlappingIntervalsAreClippedAndCountedOnlyOnce() {
        val workout = started().put("finished", "2026-11-01T10:25:00Z")
        workout.getJSONArray("activeIntervals").put(JSONObject().put("start", "2026-11-01T10:10:00Z").put("end", "2026-11-01T11:00:00Z"))
        assertEquals(25 * 60L, NativeWorkoutActiveWindows.seconds(workout))
    }

    @Test fun healthReaderNeverReceivesPauseAndWeightsPulseBySampleCount() = runBlocking {
        val workout = StudioWorkoutBridge.transition(StudioWorkoutBridge.transition(started(), true, ms("10:25")), false, ms("16:00"))
            .put("finished", "2026-11-01T16:20:00Z")
        val queried = mutableListOf<NativeWorkoutActiveWindows.Window>()
        val health = NativeWorkoutActiveWindows.health(workout) { window ->
            queried.add(window)
            JSONObject().put("start", window.start.toString()).put("end", window.end.toString())
                .put("averageHeartRate", if (queried.size == 1) 100 else 160)
                .put("heartRateSamples", if (queried.size == 1) 2 else 1).put("activeCalories", 50)
        }
        assertEquals(2, queried.size)
        assertEquals(Instant.ofEpochMilli(ms("10:25")), queried[0].end)
        assertEquals(Instant.ofEpochMilli(ms("16:00")), queried[1].start)
        assertEquals(120.0, health.getDouble("averageHeartRate"), .001)
        assertEquals(100.0, health.getDouble("activeCalories"), .001)
    }

    @Test fun unknownHealthDoesNotBecomeZeroOrEstimatedPulse() = runBlocking {
        val workout = started().put("finished", "2026-11-01T10:25:00Z")
        val health = NativeWorkoutActiveWindows.health(workout) { w -> JSONObject().put("start", w.start.toString()).put("end", w.end.toString()) }
        assertFalse(health.has("averageHeartRate"))
        assertFalse(health.has("activeCalories"))
    }

    @Test fun profileCompletionSurvivesSerializationWithoutInferringUiDefaults() {
        assertFalse(StudioProfileCompletion.complete(JSONObject()))
        val profile = JSONObject().put("height", 179).put("weight", 65).put("goal", "Erő")
            .put("experience", "Kezdő").put("location", "Otthon").put("gear", JSONArray().put("Pad"))
            .put("daysPerWeek", 3).put("minutes", 45)
        assertTrue(StudioProfileCompletion.complete(JSONObject(profile.toString())))
        profile.remove("goal")
        assertFalse(StudioProfileCompletion.complete(profile))
    }
    @Test fun stopwatchLabelsKeepMinutesAndHoursWithoutWrappingTokens() {
        val cases = mapOf(0 to "00:00", 59 to "00:59", 60 to "01:00", 600 to "10:00", 3600 to "01:00:00")
        cases.forEach { (seconds, label) -> assertEquals(label, com.example.ui.screens.studioTimerLabel(seconds)) }
    }

    @Test fun calendarFullDateAndRealDurationCrossDstAndYearBoundary() {
        fun event(date: String, time: String, minutes: Int) = com.example.model.CalendarDayEvent(
            id = "own-plan", dayOfMonth = java.time.LocalDate.parse(date).dayOfMonth,
            timeLabel = time, titleHu = "Terv", exerciseCount = 2, durationMinutes = minutes,
            status = com.example.model.CalendarEventStatus.PLANNED, dateIso = date)
        val zone = java.time.ZoneId.of("Europe/Budapest")
        val (start, end) = com.example.model.StudioCalendarDates.window(event("2026-03-29", "01:30", 60), zone)
        assertEquals(3600L, java.time.Duration.between(start, end).seconds)
        assertEquals(3, end.atZone(zone).hour)
        val (_, yearEnd) = com.example.model.StudioCalendarDates.window(event("2026-12-31", "23:30", 90), zone)
        assertEquals("2027-01-01", yearEnd.atZone(zone).toLocalDate().toString())
    }

}
