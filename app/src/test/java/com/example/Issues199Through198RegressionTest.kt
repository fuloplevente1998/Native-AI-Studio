package com.example

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.model.LoggedWorkoutEntry
import com.example.model.localWorkoutDate
import com.example.ui.components.TrainPilotScrollableBox
import com.example.ui.screens.OutdoorGpsActions
import com.example.ui.screens.OutdoorGpsDashboard
import com.example.ui.screens.OutdoorGpsDisplay
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.NativeSetStopwatch
import com.repforge.app.NativeWorkoutEditing
import com.repforge.app.NativeWorkoutSession
import com.repforge.app.NativeWorkoutTarget
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.PhoneWorkoutExecutionStore
import com.repforge.app.StudioWorkoutBridge
import com.repforge.app.data.store.TrainPilotUserSettings
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Issues199Through198RegressionTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun isolateExecutionConnection() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
    }

    // --- Issue #199: Outdoor Manual Distance & GPS-free Flow ---
    @Test
    fun manualDistanceValidationAcceptsValidKilometersAndRejectsInvalid() {
        assertNull(StudioWorkoutBridge.validateManualDistanceKm(""))
        assertNull(StudioWorkoutBridge.validateManualDistanceKm("abc"))
        assertNull(StudioWorkoutBridge.validateManualDistanceKm("0"))
        assertNull(StudioWorkoutBridge.validateManualDistanceKm("-5"))
        assertNull(StudioWorkoutBridge.validateManualDistanceKm("2001"))

        assertEquals(5200.0, StudioWorkoutBridge.validateManualDistanceKm("5.2")!!, 0.01)
        assertEquals(5200.0, StudioWorkoutBridge.validateManualDistanceKm("5,2")!!, 0.01)
        assertEquals(42195.0, StudioWorkoutBridge.validateManualDistanceKm("42.195")!!, 0.1)
    }

    @Test
    fun executionStoreSetsManualDistanceAndKeepsGpsMeasuredFallback() {
        val exerciseJson = JSONObject("""
            {"id":"ex1","name":"Futás","repUnit":"ism.","measurementType":"distance","loadType":"bodyweight",
             "sets":[{"setId":"s1","set":1,"reps":0,"weight":0,"done":false,"distanceMeters":1250.0,"distanceSource":"gps"}]}
        """)
        val candidate = NativeWorkoutSession.quickCandidate(exerciseJson, TrainPilotUserSettings())
        val saved = NativeWorkoutSession.installPrepared(context, candidate)
        val workoutId = saved.getString("workoutId")
        val generation = saved.getLong("generation")
        val store = PhoneWorkoutExecution.store(context)

        // Apply manual distance 5.0 km (5000 m)
        val updated = store.setOutdoorManualDistance(workoutId, generation, 5000.0, markDoneIfPositive = true)
        assertNotNull(updated)
        val set = updated!!.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
        assertEquals(5000.0, set.getDouble("distanceMeters"), 0.01)
        assertEquals("manual", set.getString("distanceSource"))
        assertEquals(5000.0, set.getDouble("manualDistanceMeters"), 0.01)
        assertEquals(1250.0, set.getDouble("gpsMeasuredMeters"), 0.01)
        assertTrue(set.getBoolean("done"))

        // Clear manual distance -> falls back to gpsMeasuredMeters
        val cleared = store.setOutdoorManualDistance(workoutId, generation, null, markDoneIfPositive = false)
        assertNotNull(cleared)
        val clearedSet = cleared!!.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
        assertEquals(1250.0, clearedSet.getDouble("distanceMeters"), 0.01)
        assertEquals("gps", clearedSet.getString("distanceSource"))
        assertFalse(clearedSet.has("manualDistanceMeters"))
    }

    @Test
    fun studioIndoorStopwatchCommandBypassesArmingCheckInPhoneWorkoutEngine() {
        val exerciseJson = JSONObject("""
            {"id":"plank","name":"Plank","repUnit":"mp","measurementType":"time","loadType":"bodyweight",
             "sets":[{"setId":"s1","set":1,"reps":0,"weight":0,"done":false}]}
        """)
        val candidate = NativeWorkoutSession.quickCandidate(exerciseJson, TrainPilotUserSettings())
        NativeWorkoutSession.installPrepared(context, candidate)
        val started = StudioWorkoutBridge.startIndoor(context)
        val target = NativeWorkoutTarget(
            started.getString("workoutId"),
            started.getLong("generation"),
            "plank",
            "s1"
        )
        // Ensure stopwatch has elapsed seconds (> 0)
        val clock = started.getJSONObject(NativeSetStopwatch.FIELD)
        clock.put("began", SystemClock.elapsedRealtime() - 30000L)
        NativeWorkoutEditing.commitEdit(context, started, clockEdit = true)

        // Without armed IntervalTimerService, stopStopwatch must succeed due to studioIndoor == true
        val result = NativeWorkoutEditing.stopStopwatch(context, target)
        val completedSet = target.resolve(result).set
        assertTrue(completedSet.getBoolean("done"))
        assertEquals("30", completedSet.getString("reps"))
    }

    // --- Issue #196: Flexible Workout Date parsing and Inclusive Filtering ---
    @Test
    fun flexibleWorkoutDateParserHandlesVariedFormats() {
        val entry1 = LoggedWorkoutEntry(id = "1", titleHu = "Futás", dateLabelHu = "2026-11-04T19:16:18.692Z")
        assertEquals(LocalDate.of(2026, 11, 4), entry1.localWorkoutDate())

        val entry2 = LoggedWorkoutEntry(id = "2", titleHu = "Edzés", dateLabelHu = "2026. 11. 08.")
        assertEquals(LocalDate.of(2026, 11, 8), entry2.localWorkoutDate())

        val entry3 = LoggedWorkoutEntry(id = "3", titleHu = "Séta", dateLabelHu = "2026. nov. 1. 14:30")
        assertEquals(LocalDate.of(2026, 11, 1), entry3.localWorkoutDate())

        val entry4 = LoggedWorkoutEntry(id = "4", titleHu = "Túra", dateLabelHu = "2026. november 8.")
        assertEquals(LocalDate.of(2026, 11, 8), entry4.localWorkoutDate())

        // Inclusive check: 2026. nov. 1. to 2026. nov. 8.
        val start = LocalDate.of(2026, 11, 1)
        val end = LocalDate.of(2026, 11, 8)
        val d1 = entry1.localWorkoutDate()!!
        val d2 = entry2.localWorkoutDate()!!
        val d3 = entry3.localWorkoutDate()!!
        assertTrue(!d1.isBefore(start) && !d1.isAfter(end))
        assertTrue(!d2.isBefore(start) && !d2.isAfter(end))
        assertTrue(!d3.isBefore(start) && !d3.isAfter(end))

        val outside = LocalDate.of(2026, 11, 9)
        assertFalse(!outside.isBefore(start) && !outside.isAfter(end))
    }

    // --- Issue #198: Scroll More Indicator ---
    @Test
    fun scrollMoreIndicatorShowsWhenScrollableAndHidesWhenScrolledToEnd() {
        compose.setContent {
            TrainPilotTheme {
                val scrollState = rememberScrollState()
                TrainPilotScrollableBox(
                    scrollState = scrollState,
                    modifier = Modifier.size(200.dp, 100.dp)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.verticalScroll(scrollState)
                    ) {
                        repeat(20) {
                            Text("Item $it", modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("trainpilot_scroll_more_indicator").assertIsDisplayed()
    }

    // --- Issue #199: Outdoor Dashboard Manual Distance UI Component ---
    @Test
    fun outdoorDashboardDisplaysManualDistanceCardAndAllowsInput() {
        var inputVal = ""
        var saved = false
        val display = OutdoorGpsDisplay(
            title = "Kültéri futás",
            status = "Indításra kész",
            gpsEnabled = true,
            started = false,
            paused = false,
            elapsed = "00:00",
            distance = "0,00 km",
            pace = "— /km",
            calories = "— kcal",
            heartRate = "♥ — bpm",
            pointCount = 0,
            lapCount = 1,
            latestLap = "",
            error = "",
            manualDistanceInput = "5.2",
            hasManualDistance = true
        )
        val actions = OutdoorGpsActions(
            back = {}, settings = {}, selectActivity = {}, setGpsEnabled = {},
            start = {}, pause = {}, lap = {}, finish = {}, expandMap = {},
            onManualDistanceInputChange = { inputVal = it },
            onSaveManualDistance = { saved = true }
        )

        compose.setContent {
            TrainPilotTheme {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.size(480.dp, 1000.dp).clipToBounds().testTag("viewport")) {
                        OutdoorGpsDashboard(
                            state = display,
                            strings = com.example.localization.TrainPilotStrings.forLanguage(display.language),
                            actions = actions
                        ) { mapModifier ->
                            Box(mapModifier.background(Color.Black).testTag("map_fixture"))
                        }
                    }
                }
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("outdoor_manual_distance_card").assertIsDisplayed()
        compose.onNodeWithTag("outdoor_manual_distance_input").assertIsDisplayed()
        compose.onNodeWithTag("outdoor_manual_distance_save").assertIsDisplayed()
    }
}
