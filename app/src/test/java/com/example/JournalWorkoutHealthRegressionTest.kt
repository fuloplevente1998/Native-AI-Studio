package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.LoggedWorkoutEntry
import com.example.ui.screens.LogWorkoutDetailScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.journal.JournalHealthPanel
import com.repforge.app.ui.text.nativeResources
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class JournalWorkoutHealthRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private val start = "2026-11-01T10:00:00Z"
    private val end = "2026-11-01T16:20:00Z"
    private fun intervals() = JSONArray("""[
        {"start":"2026-11-01T10:00:00Z","end":"2026-11-01T10:25:00Z"},
        {"start":"2026-11-01T16:00:00Z","end":"2026-11-01T16:20:00Z"}]
    """)

    @Before fun open() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun close() { db.close() }
    private fun <T> awaitNative(operation: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { operation() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) { "Health operation timed out" } }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
    private fun seed(id: String = "fresh", health: JSONObject? = null): WorkoutHistoryWithDetails = awaitNative {
        val raw = JSONObject().put("id", id).put("key", id).put("started", start).put("finished", end)
            .put("active", false).put("activeIntervals", intervals()).put("exercises", JSONArray())
            .put("programName", "Edzés").put("wear", JSONObject().put("averageHeartRate", 130).put("totalCalories", 200))
        if (health != null) raw.put("health240", health)
        LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(raw)
        db.workoutHistoryDao().getWorkoutWithDetailsById(id)!!
    }
    private fun response() = runBlocking {
        val workout = JSONObject().put("started", start).put("finished", end).put("activeIntervals", intervals())
        val queried = mutableListOf<NativeWorkoutActiveWindows.Window>()
        NativeWorkoutActiveWindows.health(workout) { window ->
            queried.add(window)
            JSONObject().put("start", window.start.toString()).put("end", window.end.toString())
                .put("averageHeartRate", if (queried.size == 1) 100 else 160)
                .put("heartRateSamples", if (queried.size == 1) 2 else 1).put("activeCalories", 50)
                .put("averageSpeedMps", if (queried.size == 1) 2 else 4).put("speedSamples", if (queried.size == 1) 3 else 1)
                .put("workoutEnergyProrated", true).put("sources", JSONArray().put("com.example.provider"))
                .put("sourceLabels", JSONObject().put("com.example.provider", "Measured provider"))
        }.also { assertEquals(2, queried.size); assertEquals("2026-11-01T10:25:00Z", queried[0].end.toString()) }
    }

    @Test fun freshWorkoutHealthAttachesToRoomExcludesLongPauseAndKeepsWearSeparate() {
        val before = seed()
        assertEquals(45 * 60, before.workout.durationSeconds)
        val native = NativeWorkoutHealth(context, db)
        val request = awaitNative { native.prepare(before.workout) }
        awaitNative { native.attach(request, response()) }
        val saved = awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("fresh")!! }
        val data = JSONObject(saved.workout.health240Json!!)
        assertTrue(NativeWorkoutHealth.matchesWindow(data, saved.workout))
        assertEquals(120.0, data.getDouble("averageHeartRate"), .001)
        assertEquals(100.0, data.getDouble("activeCalories"), .001)
        assertEquals(2.5, data.getDouble("averageSpeedMps"), .001)
        assertTrue(data.getBoolean("workoutEnergyProrated"))
        assertEquals("Measured provider", data.getJSONObject("sourceLabels").getString("com.example.provider"))
        assertEquals(before.workout.wearMetricsJson, saved.workout.wearMetricsJson)
        assertEquals(before.workout.activeIntervalsJson, saved.workout.activeIntervalsJson)
        val restored = seed("restored", data)
        assertTrue(NativeWorkoutHealth.matchesWindow(JSONObject(restored.workout.health240Json!!), restored.workout))
        assertEquals(data.toString(), JSONObject(restored.workout.health240Json!!).toString())
    }

    @Test fun sameOuterDatesDoNotValidateHealthThatIncludesThePause() {
        val row = seed().workout
        val fullWindow = JSONObject().put("windowStart", start).put("windowEnd", end).put("averageHeartRate", 150)
        assertFalse(NativeWorkoutHealth.matchesWindow(fullWindow, row))
        fullWindow.put("activeIntervals", intervals())
        assertTrue(NativeWorkoutHealth.matchesWindow(fullWindow, row))
        fullWindow.getJSONArray("activeIntervals").getJSONObject(1).put("start", "2026-11-01T15:00:00Z")
        assertFalse(NativeWorkoutHealth.matchesWindow(fullWindow, row))
        assertTrue(NativeWorkoutHealth.matchesWindow(JSONObject().put("windowStart", start).put("windowEnd", end),
            row.copy(activeIntervalsJson = null)))
    }

    @Test fun mismatchedActiveWindowOrDeletedWorkoutCannotReceiveLateHealth() {
        val native = NativeWorkoutHealth(context, db)
        val row = seed().workout
        val request = awaitNative { native.prepare(row) }
        val invalid = response().apply { remove("activeIntervals") }
        try { awaitNative { native.attach(request, invalid) }; fail("Pause-inclusive data accepted") }
        catch (error: java.util.concurrent.ExecutionException) { assertTrue(error.cause is IllegalStateException) }
        assertNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(row.id)!!.workout.health240Json })
        awaitNative { db.workoutHistoryDao().deleteAllWorkouts() }
        try { awaitNative { native.attach(request, response()) }; fail("Deleted workout resurrected") }
        catch (error: java.util.concurrent.ExecutionException) { assertTrue(error.cause is IllegalStateException) }
        assertNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(row.id) })
    }

    @Test fun studioJournalShowsActualSavedHealthAndWearValuesInWorkoutDetails() {
        val data = response().put("windowStart", start).put("windowEnd", end).put("syncedAt", end)
        val detail = seed(health = data)
        val entry = LoggedWorkoutEntry("fresh", "2026.11.01", "Edzés", 45, "45:00", "—", 1, 150, "Futás", isOutdoorRun = true)
        val text = nativeResources(context, "hu")
        compose.setContent {
            TrainPilotTheme {
                LogWorkoutDetailScreen(TrainPilotUiState(loggedWorkouts = listOf(entry), selectedLogWorkoutId = entry.id,
                    logDetailTabIndex = 1), TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, {}, {}, {}, {}, {}, {},
                    workoutHealth = { JournalHealthPanel(detail, text, "hu", false, null, true) {} })
            }
        }
        val list = compose.onNodeWithTag("log_workout_detail_screen")
        list.performScrollToNode(hasTestTag("journal_hc_averageHeartRate_value"))
        compose.onNodeWithTag("journal_hc_averageHeartRate_value").assertTextEquals("120 bpm")
        list.performScrollToNode(hasTestTag("journal_hc_activeCalories_value"))
        compose.onNodeWithTag("journal_hc_activeCalories_value").assertTextEquals("100 kcal")
        list.performScrollToNode(hasTestTag("journal_wear_totalCalories_value"))
        compose.onNodeWithTag("journal_wear_totalCalories_value").assertTextEquals("200 kcal")
    }
}
