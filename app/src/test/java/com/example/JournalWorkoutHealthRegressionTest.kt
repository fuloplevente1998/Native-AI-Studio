package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
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
import com.example.ui.viewmodel.navigateBack
import com.example.ui.viewmodel.navigateToSubScreen
import com.example.ui.viewmodel.openLogWorkoutDetail
import com.example.ui.viewmodel.selectTab

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class JournalWorkoutHealthRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private val models = androidx.lifecycle.ViewModelStore()
    private val start = "2026-11-01T10:00:00Z"
    private val end = "2026-11-01T16:20:00Z"
    private fun intervals() = JSONArray("""[
        {"start":"2026-11-01T10:00:00Z","end":"2026-11-01T10:25:00Z"},
        {"start":"2026-11-01T16:00:00Z","end":"2026-11-01T16:20:00Z"}]
    """)

    @Before fun open() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        val roomField = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        (roomField.get(null) as? TrainPilotDatabase)?.close()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        roomField.set(null, db)
    }
    @After fun close() {
        models.clear()
        db.close()
        TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }
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

    @Test fun manualAttachmentKeepsStoredMeasurementsWhenTheProviderReturnsLess() {
        val native = NativeWorkoutHealth(context, db)
        val workout = seed().workout
        val initial = awaitNative { native.prepare(workout) }
        assertTrue(awaitNative { native.attach(initial, response()) })
        val saved = awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(workout.id)!!.workout }
        val refresh = awaitNative { native.prepare(saved) }
        val empty = runBlocking {
            NativeWorkoutActiveWindows.health(JSONObject().put("started", start).put("finished", end)
                .put("activeIntervals", intervals())) { window ->
                JSONObject().put("start", window.start.toString()).put("end", window.end.toString())
            }
        }
        assertFalse(awaitNative { native.attach(refresh, empty) })
        val pulseOnly = response().apply {
            remove("activeCalories")
            val windows = getJSONArray("measurementWindows")
            for (index in 0 until windows.length()) windows.getJSONObject(index).remove("activeCalories")
        }
        assertFalse(awaitNative { native.attach(refresh, pulseOnly) })
        val unchanged = awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(workout.id)!!.workout }
        assertEquals(saved.health240Json, unchanged.health240Json)
    }

    @Test fun lateHeartRateSavesToRoomWithoutReplacingPreviouslySavedEnergy() {
        val native = NativeWorkoutHealth(context, db)
        val workout = seed().workout
        val window = JSONObject().put("started", start).put("finished", end).put("activeIntervals", intervals())
        val energy = runBlocking {
            NativeWorkoutActiveWindows.health(window) {
                JSONObject().put("start", it.start.toString()).put("end", it.end.toString())
                    .put("activeCalories", 60).put("totalCalories", 85)
            }
        }
        val initial = awaitNative { native.prepare(workout) }
        assertTrue(awaitNative { native.attach(initial, energy) })
        val saved = awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(workout.id)!!.workout }
        val refresh = awaitNative { native.prepare(saved) }
        val pulse = runBlocking {
            NativeWorkoutActiveWindows.health(window) {
                JSONObject().put("start", it.start.toString()).put("end", it.end.toString())
                    .put("averageHeartRate", 111).put("heartRateSamples", 2).put("heartRateOrigin", "sample.provider")
            }
        }
        assertTrue(awaitNative { native.attach(refresh, pulse) })
        val updated = awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(workout.id)!!.workout }
        val result = NativeWorkoutHealthSummary.read(updated)
        assertEquals(mapOf("activeCalories" to 120.0), result.activeEnergy.values)
        assertEquals(mapOf("totalCalories" to 170.0), result.totalEnergy.values)
        assertEquals(mapOf("averageHeartRate" to 111.0, "heartRateSamples" to 4.0), result.heart.values)
        assertEquals(setOf(HealthJournalStore.HC), result.heart.evidence.map { it.provider }.toSet())
    }

    @Test fun dailyJournalWorkoutEnergyUsesOnlySavedProviderAndExactActiveWindow() {
        val snapshot = response().put("windowStart", start).put("windowEnd", end)
        val row = seed(health = snapshot).workout.copy(originDevice = "phone")
        val measured = com.repforge.app.ui.health.workoutDayEnergy(row, HealthJournalStore.HC)!!
        assertEquals(100.0, measured.active!!, 0.001)
        assertNull(measured.total)
        assertEquals(java.time.Instant.parse(start), measured.start)
        assertEquals(java.time.Instant.parse(end), measured.end)
        assertNull(com.repforge.app.ui.health.workoutDayEnergy(row, HealthJournalStore.SAMSUNG))
        assertNull(com.repforge.app.ui.health.workoutDayEnergy(row.copy(activeIntervalsJson = null), HealthJournalStore.HC))
        assertNull(com.repforge.app.ui.health.workoutDayEnergy(row.copy(originDevice = "external"), HealthJournalStore.HC))
    }

    @Test fun dailyHealthProviderProjectionIsIndependentOfWearOnlyJournal() {
        val saved = seed(health = response().put("windowStart", start).put("windowEnd", end))
        val row = saved.workout.copy(originDevice = "phone")
        val before = row.health240Json
        val wear = NativeWorkoutHealthSummary.readJournal(row)
        assertEquals(200.0, wear.workoutTotal.values.getValue("totalCalories"), 0.001)
        assertTrue(wear.activeEnergy.values.isEmpty())
        val hc = com.repforge.app.ui.health.workoutDayEnergy(row, HealthJournalStore.HC)!!
        assertEquals(100.0, hc.active!!, 0.001)
        assertNull(hc.total)
        assertTrue(hc.activeEvidence.evidence.all { it.provider == HealthJournalStore.HC })
        assertNull(com.repforge.app.ui.health.workoutDayEnergy(row, HealthJournalStore.SAMSUNG))
        assertEquals(before, row.health240Json)
    }

    @Test fun dailyJournalKeepsPartialProviderEnergyWhileWorkoutSourceIsWearOnly() {
        val data = response().put("windowStart",start).put("windowEnd",end)
        data.getJSONArray("measurementWindows").getJSONObject(1).remove("activeCalories")
        data.remove("activeCalories")
        val saved = seed(id = "partial", health = data)
        val row = saved.workout.copy(originDevice = "phone")
        val detail = NativeWorkoutHealthSummary.read(row)
        val daily = com.repforge.app.ui.health.workoutDayEnergy(row,HealthJournalStore.HC)!!
        assertEquals(50.0,daily.active!!,0.001)
        assertEquals(detail.activeEnergy.values.getValue("activeCalories"),daily.active!!,0.001)
        assertTrue(daily.partial)
        assertTrue(detail.activeEnergy.incomplete)
        assertNull(daily.total)
        compose.setContent {
            TrainPilotTheme {
                JournalHealthPanel(saved, nativeResources(context, "hu"), "hu", false, null, true) { }
            }
        }
        compose.onNodeWithTag("journal_wear_source").assertTextEquals("TrainPilot WearOS")
        compose.onNodeWithTag("journal_provider_health_connect").assertDoesNotExist()
        compose.onNodeWithTag("journal_hc_activeCalories_value").assertDoesNotExist()
        assertEquals(saved.workout.health240Json, awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("partial")!!.workout.health240Json })
    }

    @Test fun savedProviderEvidenceRemainsIntactButOnlyWearSourceIsShown() {
        val before = seed(health = response().put("windowStart", start).put("windowEnd", end))
        val native = NativeWorkoutHealth(context, db)
        val request = awaitNative { native.prepare(before.workout) }
        awaitNative { native.attach(request, response().put("averageHeartRate", 155), HealthJournalStore.SAMSUNG) }
        val saved = awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("fresh")!! }
        val health = JSONObject(saved.workout.health240Json!!)
        assertEquals(120.0, NativeWorkoutHealthSource.snapshot(health, HealthJournalStore.HC)!!.getDouble("averageHeartRate"), .01)
        assertEquals(155.0, NativeWorkoutHealthSource.snapshot(health, HealthJournalStore.SAMSUNG)!!.getDouble("averageHeartRate"), .01)
        assertEquals(before.workout.wearMetricsJson, saved.workout.wearMetricsJson)
        val reads = mutableListOf<String>()
        compose.setContent { TrainPilotTheme {
            JournalHealthPanel(saved, nativeResources(context, "hu"), "hu", false, null, true,
                selectProvider = { reads.add(it) }, samsungEnabled = true) { reads.add("refresh") }
        } }
        compose.onNodeWithTag("journal_wear_source").assertTextEquals("TrainPilot WearOS")
        compose.onNodeWithTag("journal_provider_health_connect").assertDoesNotExist()
        compose.onNodeWithTag("journal_provider_samsung_health").assertDoesNotExist()
        compose.onNodeWithTag("journal_hc_averageHeartRate_value").assertDoesNotExist()
        compose.onNodeWithTag("journal_samsung_averageHeartRate_value").assertDoesNotExist()
        compose.runOnIdle { assertTrue(reads.isEmpty()) }
        assertEquals(saved.workout.health240Json, awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("fresh")!!.workout.health240Json })
    }

    @Test fun missingWearShowsTipEvenWithHealthConnectAndLaterIsPerWorkout() {
        val saved = seed(health = response().put("windowStart", start).put("windowEnd", end))
        val row = saved.workout.copy(wearMetricsJson = null, rawJson = JSONObject(saved.workout.rawJson).apply {
            remove("wear"); remove("healthSummary"); remove("healthWear129")
        }.toString())
        var selected by androidx.compose.runtime.mutableStateOf(saved.copy(workout = row))
        compose.setContent { TrainPilotTheme {
            com.repforge.app.ui.journal.JournalWorkoutHealthSummary(selected, nativeResources(context, "hu"), "hu")
        } }
        compose.onNodeWithTag("journal_wear_measurement_missing").assertTextEquals("Nincs óramérés ehhez az edzéshez.")
        compose.onNodeWithTag("journal_summary_heart_averageHeartRate_value").assertDoesNotExist()
        compose.onNodeWithTag("journal_summary_energy_activeCalories_value").assertDoesNotExist()
        compose.onNodeWithTag("journal_wear_install").assertHasClickAction()
        compose.onNodeWithTag("journal_wear_tip_later").performClick()
        compose.onNodeWithTag("journal_wear_install_tip").assertDoesNotExist()
        compose.onNodeWithTag("journal_wear_measurement_missing").assertExists()
        compose.runOnIdle { selected = saved.copy(workout = row.copy(id = "another-workout")) }
        compose.onNodeWithTag("journal_wear_install_tip").assertExists()
        compose.runOnIdle { selected = saved.copy(workout = row) }
        compose.onNodeWithTag("journal_wear_install_tip").assertDoesNotExist()
        compose.runOnIdle { selected = saved }
        compose.onNodeWithTag("journal_summary_energy_totalCalories_value").assertTextEquals("200 kcal")
        compose.onNodeWithTag("journal_wear_install_tip").assertDoesNotExist()
        val imported = saved.copy(workout = row.copy(id = "import", originDevice = "external",
            rawJson = JSONObject(row.rawJson).put("externalWorkout359", JSONObject()
                .put("provider", HealthJournalStore.HC).put("source", "com.sec.android.app.shealth")
                .put("start", start).put("end", end).put("metrics", JSONObject().put("values",
                    JSONObject().put("maxHeartRate", 161).put("totalCalories", 312)))).toString()))
        compose.runOnIdle { selected = imported }
        compose.onNodeWithTag("journal_external_metrics").assertExists()
        compose.onNodeWithTag("journal_external_maxHeartRate_value").assertTextEquals("161 bpm")
        compose.onNodeWithTag("journal_external_totalCalories_value").assertTextEquals("312 kcal")
        compose.onNodeWithTag("journal_wear_install_tip").assertDoesNotExist()
        assertEquals("market://details?id=com.TrainPilot.com", com.repforge.app.ui.journal.wearInstallIntent().data.toString())
        assertEquals("https://play.google.com/store/apps/details?id=com.TrainPilot.com",
            com.repforge.app.ui.journal.wearInstallIntent(web = true).data.toString())
        assertEquals(saved.workout.health240Json, awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("fresh")!!.workout.health240Json })
    }

    @Test fun workoutStatisticsShowOnlyWearTotalAndNeverSubstituteActiveEnergy() {
        val saved = seed()
        val full = JSONObject().put("averageHeartRate", 128).put("maxHeartRate", 161)
            .put("activeCalories", 47.8).put("totalCalories", 312)
        var selected by androidx.compose.runtime.mutableStateOf(saved.copy(workout =
            saved.workout.copy(wearMetricsJson = full.toString())))
        compose.setContent { TrainPilotTheme {
            com.repforge.app.ui.journal.JournalWorkoutHealthSummary(selected, nativeResources(context, "hu"), "hu")
        } }
        compose.onNodeWithTag("journal_summary_heart_averageHeartRate_value").assertTextEquals("128 bpm")
        compose.onNodeWithTag("journal_summary_heart_maxHeartRate_value").assertTextEquals("161 bpm")
        compose.onNodeWithTag("journal_summary_energy_totalCalories_value").assertTextEquals("312 kcal")
        compose.onNodeWithTag("journal_summary_energy_activeCalories_value").assertDoesNotExist()
        val activeOnly = JSONObject().put("activeCalories", 47.8)
        compose.runOnIdle { selected = saved.copy(workout = saved.workout.copy(wearMetricsJson = activeOnly.toString())) }
        compose.onNodeWithTag("journal_summary_total_missing").assertExists()
        compose.onNodeWithTag("journal_summary_energy_totalCalories_value").assertDoesNotExist()
        compose.onNodeWithTag("journal_summary_energy_activeCalories_value").assertDoesNotExist()
        compose.onNodeWithTag("journal_wear_install_tip").assertDoesNotExist()
        val projection = NativeWorkoutHealthSummary.readJournal(saved.workout.copy(wearMetricsJson = full.toString()))
        assertEquals(47.8, projection.activeEnergy.values.getValue("activeCalories"), 0.001)
        assertEquals(312.0, projection.workoutTotal.values.getValue("totalCalories"), 0.001)
    }

    @Test fun importedSourceKeepsSamsungViaHealthConnectAndOwnMetrics() {
        val saved = seed()
        val imported = saved.copy(workout = saved.workout.copy(originDevice = "external",
            rawJson = JSONObject(saved.workout.rawJson).put("externalWorkout359", JSONObject()
                .put("provider", HealthJournalStore.HC).put("source", "com.sec.android.app.shealth")
                .put("start", start).put("end", end).put("metrics", JSONObject().put("values",
                    JSONObject().put("totalCalories", 312)))).toString()))
        compose.setContent { TrainPilotTheme {
            JournalHealthPanel(imported, nativeResources(context, "hu"), "hu", false, null, true) { }
        } }
        compose.onNodeWithTag("journal_external_source_label").assertTextEquals("Samsung Health – Health Connecten át")
        compose.onNodeWithTag("journal_external_totalCalories_value").assertTextEquals("312 kcal")
        compose.onNodeWithTag("journal_wear_source").assertDoesNotExist()
    }

    @Test fun sourceCardRestorationKeepsWearOnly() {
        val detail = seed(health = response().put("windowStart", start).put("windowEnd", end))
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TrainPilotTheme {
            JournalHealthPanel(detail, nativeResources(context, "hu"), "hu", false, null, true) { }
        } }
        compose.onNodeWithTag("journal_wear_source").assertTextEquals("TrainPilot WearOS")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("journal_wear_source").assertTextEquals("TrainPilot WearOS")
        compose.onNodeWithTag("journal_provider_health_connect").assertDoesNotExist()
        compose.onNodeWithTag("journal_provider_samsung_health").assertDoesNotExist()
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

    @Test fun studioJournalShowsActualWearOnlyInWorkoutDetails() {
        val data = response().put("windowStart", start).put("windowEnd", end).put("syncedAt", end)
        val detail = seed(health = data)
        val entry = LoggedWorkoutEntry("fresh", "2026.11.01", "Edzés", 45, "45:00", "—", 1, 150, "Futás", isOutdoorRun = true)
        val model = com.repforge.app.ui.journal.JournalViewModel(context as android.app.Application)
        models.put("journal", model)
        var canonicalSources = false
        var tab by androidx.compose.runtime.mutableStateOf(1)
        compose.setContent {
            TrainPilotTheme {
                val health = com.example.ui.components.studioWorkoutHealthContent("fresh", "hu", {},
                    { id, wear, hc -> canonicalSources = id == "fresh" && wear && !hc }, model)
                LogWorkoutDetailScreen(TrainPilotUiState(loggedWorkouts = listOf(entry), selectedLogWorkoutId = entry.id,
                    logDetailTabIndex = tab), TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, { tab = it }, {}, {}, {}, {}, {},
                    workoutHealth = health)
            }
        }
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            model.uiState.value.detail != null && canonicalSources }
        assertFalse(model.uiState.value.healthRefreshing)
        assertNull(model.uiState.value.healthError)
        val list = compose.onNodeWithTag("log_workout_detail_screen")
        val header = compose.onNodeWithTag("journal_detail_header").getUnclippedBoundsInRoot()
        assertTrue("Compact header must not consume a large empty top section", header.bottom - header.top <= 160.dp)
        assertTrue("No duplicate top spacer", header.top - list.getUnclippedBoundsInRoot().top <= 10.dp)
        compose.onNodeWithTag("journal_detail_location").assertTextEquals("Fejlődés · Napló")
        compose.onNodeWithText("Edzés részletei").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("journal_summary_heart_averageHeartRate_value"))
        compose.onNodeWithTag("journal_summary_heart_averageHeartRate_value").assertTextEquals("130 bpm")
        list.performScrollToNode(hasTestTag("journal_summary_energy_totalCalories_value"))
        compose.onNodeWithTag("journal_summary_energy_totalCalories_value").assertTextEquals("200 kcal")
        compose.onNodeWithTag("journal_summary_energy_activeCalories_value").assertDoesNotExist()
        compose.onNodeWithTag("journal_health_source_group").assertDoesNotExist()
        compose.onNodeWithTag("log_detail_open_watch_sync").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("log_detail_tab_2"))
        compose.onNodeWithTag("log_detail_tab_2").performClick()
        compose.onNodeWithTag("journal_workout_health_summary").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("journal_wear_source"))
        compose.onNodeWithTag("journal_wear_source").assertTextEquals("TrainPilot WearOS")
        compose.onNodeWithTag("journal_provider_health_connect").assertDoesNotExist()
        compose.onNodeWithTag("journal_provider_samsung_health").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("log_detail_tab_1"))
        compose.onNodeWithTag("log_detail_tab_1").performClick()
        list.performScrollToNode(hasTestTag("journal_summary_energy_totalCalories_value"))
        compose.onNodeWithTag("journal_summary_energy_totalCalories_value").assertTextEquals("200 kcal")
        compose.onNodeWithTag("journal_health_source_group").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("log_detail_tab_2"))
        compose.onNodeWithTag("log_detail_tab_2").performClick()
        list.performScrollToNode(hasTestTag("journal_wear_source"))
        compose.onNodeWithTag("journal_wear_source").assertTextEquals("TrainPilot WearOS")
        assertFalse(model.uiState.value.healthRefreshing)
        assertNull(model.uiState.value.healthError)
    }
    @Test fun emptyCachedProviderIsNotReadOnOpenAndNeverAdvertised() {
        // The foreground-only refresh contract needs an explicitly resumed host and
        // denied grants; neither state is an implicit Robolectric fixture guarantee.
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        shadowOf(context as android.app.Application).denyPermissions(*listOf(
            "READ_STEPS", "READ_HEART_RATE", "READ_ACTIVE_CALORIES_BURNED", "READ_TOTAL_CALORIES_BURNED",
            "READ_EXERCISE", "READ_DISTANCE", "READ_SPEED"
        ).map { "android.permission.health.$it" }.toTypedArray())
        assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, compose.activity.lifecycle.currentState)
        val empty = JSONObject().put("windowStart", start).put("windowEnd", end).put("activeIntervals", intervals())
        val detail = seed(health = empty)
        assertTrue(NativeWorkoutHealth.matchesWindow(empty, detail.workout))
        val model = com.repforge.app.ui.journal.JournalViewModel(context as android.app.Application)
        models.put("empty-journal", model)
        var healthAvailable = true
        compose.setContent {
            TrainPilotTheme {
                val content = com.example.ui.components.studioWorkoutHealthContent("fresh", "hu", {},
                    { _, _, hc -> healthAvailable = hc }, model)
                content()
            }
        }
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            model.uiState.value.detail != null && !healthAvailable
        }
        assertNull(model.uiState.value.healthError)
        assertFalse(model.uiState.value.healthRefreshing)
        assertFalse(healthAvailable)
        compose.onNodeWithTag("journal_hc_averageHeartRate_value").assertDoesNotExist()
        compose.onNodeWithTag("journal_hc_activeCalories_value").assertDoesNotExist()
        assertEquals(detail.workout.health240Json,
            awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("fresh")!!.workout.health240Json })
    }

    @Test fun journalDetailsAndRouteKeepJournalSelectedAndBackRestoresTheOrigin() {
        val model = com.example.ui.viewmodel.TrainPilotViewModel()
        models.put("studio-navigation", model)
        model.openLogWorkoutDetail("fresh")
        assertEquals(com.example.model.MainTab.LOG, model.uiState.value.currentTab)
        compose.setContent {
            val state by model.uiState.collectAsState()
            TrainPilotTheme {
                com.example.ui.components.TrainPilotGlobalTopHeaderAndNav(
                    currentTab = state.currentTab, currentSubScreen = state.currentSubScreen,
                    currentLanguage = state.language, selectedPaletteId = state.selectedPaletteId,
                    strings = TrainPilotStrings.forLanguage(state.language), onSelectTab = model::selectTab,
                    onNavigateToSubScreen = { _, _ -> }, onSelectLanguage = {}, onOpenPalettePicker = {}, onOpenSettings = {})
            }
        }
        compose.onNodeWithTag("top_bar_journal_button").assertIsSelected()
        compose.runOnIdle { model.navigateToSubScreen(com.example.model.SubScreen.LOG_ROUTE_MAP) }
        compose.onNodeWithTag("top_bar_journal_button").assertIsSelected()
        compose.runOnIdle { model.navigateBack() }
        compose.onNodeWithTag("top_bar_journal_button").assertIsSelected()
        compose.runOnIdle { model.navigateBack() }
        compose.onNodeWithTag("top_nav_home").assertIsSelected()
    }

    @Test fun studioDeleteRequiresConfirmationAndReallyDeletesTheSavedWorkout() {
        val detail = seed()
        val model = com.repforge.app.ui.journal.JournalViewModel(context as android.app.Application)
        models.put("delete", model)
        var deleted = ""
        compose.setContent { TrainPilotTheme {
            com.example.ui.components.StudioLoggedWorkoutDeleteActions(detail.workout.id, { deleted = it }, model)
        } }
        compose.runOnIdle { model.openDetails(detail.workout.id) }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); model.uiState.value.detail != null }
        compose.onNodeWithTag("journal_delete_workout").performClick()
        compose.onNodeWithTag("journal_cancel_delete").performClick()
        assertNotNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(detail.workout.id) })
        compose.onNodeWithTag("journal_delete_workout").performClick()
        compose.onNodeWithTag("journal_confirm_delete").performClick()
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); deleted == detail.workout.id }
        assertNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(detail.workout.id) })
        assertNotNull(awaitNative { db.workoutHistoryDao().getHistoryTombstone(detail.workout.id) })
    }

    @Test fun changingTheSelectedWorkoutCancelsTheOldDeleteConfirmation() {
        val first = seed("delete-first")
        val second = seed("delete-second")
        val model = com.repforge.app.ui.journal.JournalViewModel(context as android.app.Application)
        models.put("delete-change", model)
        var selected by androidx.compose.runtime.mutableStateOf(first.workout.id)
        compose.setContent { TrainPilotTheme {
            com.example.ui.components.StudioLoggedWorkoutDeleteActions(selected, {}, model)
        } }
        compose.runOnIdle { model.openDetails(first.workout.id) }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); model.uiState.value.detail != null }
        compose.onNodeWithTag("journal_delete_workout").performClick()
        compose.runOnIdle { selected = second.workout.id; model.openDetails(second.workout.id) }
        compose.onNodeWithTag("journal_confirm_delete").assertDoesNotExist()
        assertNotNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(first.workout.id) })
        assertNotNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(second.workout.id) })
    }

    @Test fun narrowJournalCalendarHeaderFitsHintAndBothActionsWithoutTallEmptySpace() {
        var language by androidx.compose.runtime.mutableStateOf(AppLanguage.HU)
        compose.setContent { TrainPilotTheme {
            androidx.compose.foundation.layout.Box(Modifier.width(320.dp)) {
                com.example.ui.screens.LogTabScreen(TrainPilotUiState(language = language),
                    TrainPilotStrings.forLanguage(language), {}, {}, {}, {}, {})
            }
        } }
        compose.onNodeWithTag("log_month_picker_button").performClick()
        for (lang in AppLanguage.entries) {
            compose.runOnIdle { language = lang }
            val header = compose.onNodeWithTag("log_interval_calendar_header").fetchSemanticsNode().boundsInRoot
            assertTrue("Calendar header too tall: $lang / $header", header.height <= 160 * compose.activity.resources.displayMetrics.density)
            listOf("log_jump_today_button", "log_clear_filter_button").forEach { tag ->
                val action = compose.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertTrue(action.left >= header.left && action.right <= header.right)
            }
        }
        compose.onNodeWithTag("log_jump_today_button").performClick()
        compose.onNodeWithTag("log_clear_filter_button").performClick()
        compose.onNodeWithText("Selectează două zile pentru a vedea intervalul.").assertExists()
    }

    @Test fun listDeletionConfirmsTheClickedRowWithoutOpeningTheDetailPage() {
        val first = seed("list-delete-first")
        val second = seed("list-delete-second")
        var rows by androidx.compose.runtime.mutableStateOf(listOf(first, second).map { detail ->
            LoggedWorkoutEntry(detail.workout.id, "2026.11.01", "Edzés", 45, "45:00", "—", 1, 150, "Futás",
                savedStartIso = detail.workout.started)
        })
        var opened = 0
        var deleted = ""
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.LogTabScreen(TrainPilotUiState(loggedWorkouts = rows),
                TrainPilotStrings.forLanguage(AppLanguage.HU), {}, { opened++ }, {}, {}, {},
                onDeletedWorkout = { id -> deleted = id; rows = rows.filterNot { it.id == id } })
        } }
        compose.onNodeWithTag("journal_list_delete_${first.workout.id}").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithTag("journal_cancel_delete").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, opened)
        compose.onNodeWithTag("journal_cancel_delete").performClick()
        assertNotNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(first.workout.id) })
        compose.onNodeWithTag("journal_list_delete_${first.workout.id}").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithTag("journal_confirm_delete").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("journal_confirm_delete").performClick()
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); deleted == first.workout.id }
        assertNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(first.workout.id) })
        assertNotNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(second.workout.id) })
        compose.onNodeWithTag("log_card_${first.workout.id}").assertDoesNotExist()
        compose.onNodeWithTag("log_card_${second.workout.id}").assertExists()
        assertEquals(0, opened)
    }

    @Test fun listDeletionShowsAnErrorIfTheRecordHasAlreadyDisappeared() {
        val entry = LoggedWorkoutEntry("missing-row", "2026.11.01", "Edzés", 20, "20:00", "—", 1, 100, "", savedStartIso = start)
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.LogTabScreen(TrainPilotUiState(loggedWorkouts = listOf(entry)),
                TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("journal_list_delete_missing-row").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithTag("journal_list_delete_error").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("journal_confirm_delete").assertDoesNotExist()
        assertTrue(awaitNative { db.workoutHistoryDao().getTombstones().isEmpty() })
    }

}
