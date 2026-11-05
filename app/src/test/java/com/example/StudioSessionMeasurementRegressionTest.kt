package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.SubScreen
import com.example.model.MainTab
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.workout.ActiveWorkoutViewModel
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class StudioSessionMeasurementRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)
    private val models = ViewModelStore()

    @Before fun prepare() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        native { db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises()) }
    }

    @After fun close() {
        models.clear(); parent.cancel(); shadowOf(Looper.getMainLooper()).idle(); db.close()
    }

    private fun model() = TrainPilotViewModel().also {
        it.bindNativeEnvironment(context, TrainPilotSettingsStore(context), db, scope)
        await { !it.uiState.value.savedRecordsLoading }
    }

    private fun nativeModel() = ActiveWorkoutViewModel(ApplicationProvider.getApplicationContext<Application>()).also {
        models.put("measurement", it)
        await { it.uiState.value.exercises.isNotEmpty() }
    }

    private fun timed(model: TrainPilotViewModel) = model.uiState.value.exercises.first { it.isBilateral }
    private fun strength(model: TrainPilotViewModel) = model.uiState.value.exercises.first { !it.isTimed && !it.isBilateral }
    private fun target(frame: JSONObject): NativeWorkoutTarget {
        val exercise = frame.getJSONArray("exercises").getJSONObject(frame.optInt("currentExercise"))
        return NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"),
            exercise.getString("id"), exercise.getJSONArray("sets").getJSONObject(0).getString("setId"))
    }

    private fun watchSelect(index: Int, sequence: Long = 1): JSONObject {
        val frame = ActiveWorkoutStore.current(context)!!
        val exercise = frame.getJSONArray("exercises").getJSONObject(index)
        return WearSyncRepository.commitWatchEditorMutation(context, frame, JSONObject()
            .put("action", "selectExercise").put("workoutId", frame.getString("workoutId"))
            .put("ownerEpoch193", frame.optLong("ownerEpoch193")).put("pauseRevision", frame.optLong("pauseRevision"))
            .put("sequence", sequence).put("exerciseIndex", index).put("exerciseId", exercise.getString("id")))
    }

    @Test fun watchSelectionUpdatesTheStudioTargetAndRepeatedStartKeepsOneNativeStopwatch() {
        val model = model()
        val timed = timed(model)
        model.startWorkoutWithExercises(listOf(strength(model), timed))
        awaitStarted(model)
        val before = ActiveWorkoutStore.current(context)!!
        val backStack = model.uiState.value.backStack
        val measurement = nativeModel()
        watchSelect(1)
        await { model.uiState.value.selectedExerciseId == timed.id }
        val state = model.uiState.value
        val durable = PhoneWorkoutExecution.store(context).snapshot()!!
        assertEquals(1, durable.getInt("currentExercise"))
        assertEquals(1, state.activeExerciseIndex)
        assertEquals(SubScreen.ACTIVE_BILATERAL, state.activeWorkoutSubScreen)
        assertEquals(SubScreen.ACTIVE_BILATERAL, state.currentSubScreen)
        assertEquals(backStack, state.backStack)
        assertEquals(before.getString("workoutId"), durable.getString("workoutId"))
        assertEquals(before.getLong("generation"), durable.getLong("generation"))
        val selectedSet = state.activeSets.first { !it.completed }
        assertEquals(selectedSet.id, target(durable).setId)
        val selectedTarget = NativeWorkoutTarget(state.activeWorkoutId, state.activeWorkoutGeneration,
            state.selectedExerciseId, selectedSet.id)
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get(), durable)
            measurement.startSetStopwatch(selectedTarget, "left")
            measurement.startSetStopwatch(selectedTarget, "left")
            await { measurement.uiState.value.stopwatchRunning }
            val firstClock = PhoneWorkoutExecution.store(context).snapshot()!!.getJSONObject(NativeSetStopwatch.FIELD).getString("id")
            measurement.startSetStopwatch(selectedTarget, "left")
            shadowOf(Looper.getMainLooper()).idle()
            val after = PhoneWorkoutExecution.store(context).snapshot()!!
            assertEquals(firstClock, after.getJSONObject(NativeSetStopwatch.FIELD).getString("id"))
            assertEquals(selectedSet.id, measurement.uiState.value.stopwatchSetId)
            assertFalse(selectedTarget.resolve(after).set.optBoolean("done"))
            assertEquals(backStack, model.uiState.value.backStack)
        } finally { service.destroy() }
    }

    @Test fun unchangedSelectionPreservesModeAndRemoteSelectionPreservesHomeAndExerciseBrowsing() {
        val model = model()
        val timed = timed(model)
        model.startWorkoutWithExercises(listOf(strength(model), timed))
        awaitStarted(model)
        model.navigateToSubScreen(SubScreen.ACTIVE_TIMED)
        assertTrue(model.pauseActiveWorkout(false))
        val modeStack = model.uiState.value.backStack
        watchSelect(0)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(SubScreen.ACTIVE_TIMED, model.uiState.value.currentSubScreen)
        assertEquals(SubScreen.ACTIVE_TIMED, model.uiState.value.activeWorkoutSubScreen)
        assertEquals(modeStack, model.uiState.value.backStack)
        model.selectTab(MainTab.HOME)
        assertTrue(model.pauseActiveWorkout(false))
        val homeStack = model.uiState.value.backStack
        watchSelect(1, 2)
        await { model.uiState.value.selectedExerciseId == timed.id }
        assertEquals(MainTab.HOME, model.uiState.value.currentTab)
        assertEquals(SubScreen.NONE, model.uiState.value.currentSubScreen)
        assertEquals(SubScreen.ACTIVE_BILATERAL, model.uiState.value.activeWorkoutSubScreen)
        assertEquals(homeStack, model.uiState.value.backStack)
        NativeWorkoutEditing.project(context)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(SubScreen.NONE, model.uiState.value.currentSubScreen)
        assertEquals(homeStack, model.uiState.value.backStack)
        val browsing = model.uiState.value.exercises.first { it.id != timed.id && it.id != strength(model).id }
        model.openExerciseDetail(browsing.id)
        val detailStack = model.uiState.value.backStack
        watchSelect(0, 3)
        await { model.uiState.value.activeExerciseIndex == 0 }
        assertEquals(browsing.id, model.uiState.value.selectedExerciseId)
        assertEquals(SubScreen.EXERCISE_DETAIL, model.uiState.value.currentSubScreen)
        assertEquals(detailStack, model.uiState.value.backStack)
        NativeWorkoutEditing.project(context)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(browsing.id, model.uiState.value.selectedExerciseId)
        model.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY)
        val libraryStack = model.uiState.value.backStack
        watchSelect(1, 4)
        await { model.uiState.value.activeExerciseIndex == 1 }
        assertEquals(browsing.id, model.uiState.value.selectedExerciseId)
        assertEquals(SubScreen.EXERCISE_LIBRARY, model.uiState.value.currentSubScreen)
        assertEquals(libraryStack, model.uiState.value.backStack)
        model.returnToActiveWorkout()
        assertEquals(timed.id, model.uiState.value.selectedExerciseId)
        assertEquals(SubScreen.ACTIVE_BILATERAL, model.uiState.value.currentSubScreen)
    }

    @Test fun reorderedSnapshotMapsSelectionByIdAndUnknownIdWarnsOnlyOnceWithoutGuessing() {
        val model = model()
        val timed = timed(model)
        model.startWorkoutWithExercises(listOf(strength(model), timed))
        awaitStarted(model)
        val frame = ActiveWorkoutStore.current(context)!!
        val rows = frame.getJSONArray("exercises")
        val reordered = JSONObject(frame.toString()).put("currentExercise", 0)
            .put("exercises", org.json.JSONArray().put(rows.getJSONObject(1)).put(rows.getJSONObject(0)))
        ActiveWorkoutStore.saveOwned(context, reordered)
        await { model.uiState.value.selectedExerciseId == timed.id }
        assertEquals(1, model.uiState.value.activeExerciseIndex)
        val before = model.uiState.value
        val unknown = JSONObject(reordered.toString()).apply {
            getJSONArray("exercises").getJSONObject(0).put("id", "unknown-watch-exercise")
        }
        ActiveWorkoutStore.saveOwned(context, unknown)
        await { model.uiState.value.statusBannerMessage?.contains("nem található") == true }
        assertEquals(before.activeExerciseIndex, model.uiState.value.activeExerciseIndex)
        assertEquals(before.selectedExerciseId, model.uiState.value.selectedExerciseId)
        assertEquals(before.activeSets, model.uiState.value.activeSets)
        assertEquals(before.currentSubScreen, model.uiState.value.currentSubScreen)
        assertEquals(before.backStack, model.uiState.value.backStack)
        model.clearBannerMessage()
        ActiveWorkoutStore.saveOwned(context, unknown)
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(model.uiState.value.statusBannerMessage)
    }

    @Test fun watchSetEditSurvivesTheNextCanonicalPhoneProjection() {
        val model = model()
        model.startQuickStrengthWithExercise(strength(model))
        await { model.uiState.value.isWorkoutSessionActive }
        val frame = ActiveWorkoutStore.current(context)!!
        val target = target(frame)
        val command = JSONObject().put("action", "updateSet").put("workoutId", target.workoutId)
            .put("ownerEpoch193", frame.optLong("ownerEpoch193")).put("pauseRevision", frame.optLong("pauseRevision"))
            .put("sequence", 1).put("exerciseIndex", 0).put("exerciseId", target.exerciseId)
            .put("setIndex", 0).put("setId", target.setId).put("field", "weight").put("value", "17.5")
        WearSyncRepository.commitWatchEditorMutation(context, frame, command)
        val durable = PhoneWorkoutExecution.store(context).snapshot()!!
        assertEquals(17.5, target.resolve(durable).set.getDouble("weight"), 0.0)
        val projected = NativeWorkoutEditing.project(context)
        assertEquals(17.5, target.resolve(projected).set.getDouble("weight"), 0.0)
        assertEquals(1L, projected.getLong("localSequence"))
        assertEquals(frame.getLong("generation"), projected.getLong("generation"))
    }

    @Test fun staleWatchTargetCannotEditAnotherCanonicalSet() {
        val model = model()
        model.startQuickStrengthWithExercise(strength(model))
        await { model.uiState.value.isWorkoutSessionActive }
        val frame = ActiveWorkoutStore.current(context)!!
        val target = target(frame)
        val before = target.resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set.toString()
        val command = JSONObject().put("action", "updateSet").put("workoutId", target.workoutId)
            .put("ownerEpoch193", frame.optLong("ownerEpoch193")).put("pauseRevision", frame.optLong("pauseRevision"))
            .put("exerciseIndex", 0).put("exerciseId", target.exerciseId).put("setIndex", 0)
            .put("setId", "old-set").put("field", "weight").put("value", "99")
        try {
            WearSyncRepository.commitWatchEditorMutation(context, frame, command)
            fail("A stale watch target was accepted")
        } catch (_: IllegalStateException) { }
        assertEquals(before, target.resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set.toString())
    }

    @Test fun manualStrengthRecordingDoesNotRequireAutomaticWorkoutPlanning() {
        val model = model()
        model.startQuickStrengthWithExercise(strength(model))
        await { model.uiState.value.isWorkoutSessionActive }
        val frame = PhoneWorkoutExecution.store(context).snapshot()!!
        val target = target(frame)
        frame.put("automaticEnabled", false)
        target.resolve(frame).set.put("reps", "12").put("weight", 10)
        PhoneWorkoutExecution.store(context).prepare(frame, frame.getLong("executionRevision"))
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get(), PhoneWorkoutExecution.store(context).snapshot()!!)
            NativeWorkoutEditing.completeStrengthSet(context, target.exerciseId, target.setId)
            val recorded = PhoneWorkoutExecution.store(context).snapshot()!!
            assertTrue(target.resolve(recorded).set.getBoolean("done"))
            assertFalse(recorded.getBoolean("automaticEnabled"))
            assertFalse(recorded.has("transition"))
        } finally { service.destroy() }
    }

    @Test fun screenOffPreservesTheArmedWorkoutAndExplicitPauseStillStopsIt() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model))
        awaitStarted(model)
        val frame = ActiveWorkoutStore.current(context)!!
        val target = target(frame)
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get(), frame)
            NativeWorkoutEditing.startStopwatch(context, target, "left")
            ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
            model.onAppBackground()
            val background = PhoneWorkoutExecution.store(context).snapshot()!!
            assertFalse(background.optBoolean("workoutPaused"))
            assertEquals(frame.optLong("pauseRevision"), background.optLong("pauseRevision"))
            assertTrue(NativeSetStopwatch.running(background.getJSONObject(NativeSetStopwatch.FIELD)))
            ShadowSystemClock.advanceBy(Duration.ofSeconds(3))
            assertTrue(NativeSetStopwatch.elapsed(background.getJSONObject(NativeSetStopwatch.FIELD)) >= 5_000)
            assertTrue(model.pauseActiveWorkout())
            val paused = PhoneWorkoutExecution.store(context).snapshot()!!
            assertTrue(paused.optBoolean("workoutPaused"))
            assertFalse(NativeSetStopwatch.running(paused.getJSONObject(NativeSetStopwatch.FIELD)))
            assertTrue(paused.optLong("pauseRevision") > background.optLong("pauseRevision"))
        } finally { service.destroy() }
    }

    @Test fun repeatedQuickTimedStartKeepsTheCanonicalWorkoutAndStableSetIds() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model))
        awaitStarted(model)
        val first = ActiveWorkoutStore.current(context)!!
        model.startQuickTimedWithExercise(timed(model))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(first.getString("workoutId"), model.uiState.value.activeWorkoutId)
        val current = ActiveWorkoutStore.current(context)!!
        assertEquals(first.getLong("generation"), current.getLong("generation"))
        assertEquals(target(first), target(current))
        assertEquals(current.getString("workoutId"), PhoneWorkoutExecution.store(context).snapshot()!!.getString("workoutId"))
    }

    @Test fun timedStartWhileStrengthIsActiveReopensThatSessionWithoutPhantomUi() {
        val model = model()
        model.startQuickStrengthWithExercise(strength(model))
        await { model.uiState.value.isWorkoutSessionActive }
        val first = ActiveWorkoutStore.current(context)!!
        model.startQuickTimedWithExercise(timed(model))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(first.getString("workoutId"), model.uiState.value.activeWorkoutId)
        assertEquals(SubScreen.ACTIVE_STRENGTH, model.uiState.value.currentSubScreen)
        assertEquals(target(first), target(ActiveWorkoutStore.current(context)!!))
    }

    @Test fun swappingBilateralExerciseKeepsTheNativeAndDisplayedMeasurementMetadataTogether() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model))
        awaitStarted(model)
        val before = ActiveWorkoutStore.current(context)!!
        val replacement = strength(model)
        model.switchCurrentActiveExercise(replacement)
        await { model.uiState.value.activeWorkoutExercises.getOrNull(model.uiState.value.activeExerciseIndex)?.id == replacement.id }
        val after = ActiveWorkoutStore.current(context)!!
        assertEquals(before.getString("workoutId"), after.getString("workoutId"))
        assertEquals(before.getLong("generation"), after.getLong("generation"))
        assertEquals(replacement.id, target(after).exerciseId)
        assertEquals(replacement.measurementType, after.getJSONArray("exercises").getJSONObject(0).getString("measurementType"))
        assertEquals(SubScreen.ACTIVE_STRENGTH, model.uiState.value.activeWorkoutSubScreen)
    }

    @Test fun repeatedNativeStartCannotImplicitlyStopOrRecordTheRunningSide() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model))
        awaitStarted(model)
        val frame = ActiveWorkoutStore.current(context)!!
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get(), frame)
            val target = target(frame)
            val started = NativeWorkoutEditing.startStopwatch(context, target, "left")
            val clockId = started.getJSONObject(NativeSetStopwatch.FIELD).getString("id")
            ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
            val repeated = NativeWorkoutEditing.startStopwatch(context, target, "left")
            assertEquals(clockId, repeated.getJSONObject(NativeSetStopwatch.FIELD).getString("id"))
            assertTrue(NativeSetStopwatch.running(repeated.getJSONObject(NativeSetStopwatch.FIELD)))
            assertTrue(NativeSetStopwatch.elapsed(repeated.getJSONObject(NativeSetStopwatch.FIELD)) >= 2_000)
            assertFalse(target.resolve(repeated).set.optBoolean("done"))
        } finally { service.destroy() }
    }

    @Test fun closingNativeWorkoutClearsBothStudioAndMeasurementProjections() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model))
        awaitStarted(model)
        val measurement = nativeModel()
        val before = ActiveWorkoutStore.current(context)!!
        native { TrainPilotWorkoutFinalization(context, db).discardWorkout(before) }
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(model.uiState.value.isWorkoutSessionActive)
        assertEquals("", model.uiState.value.activeWorkoutId)
        assertEquals("", measurement.uiState.value.workoutId)
        assertTrue(measurement.uiState.value.exercises.isEmpty())
        assertFalse(PhoneWorkoutExecution.store(context).snapshot()!!.optBoolean("active"))
    }

    @Test fun librarySwitchUsesCanonicalMetadataInBothDirectionsAndCanBeReopened() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model)); awaitStarted(model)
        val before = ActiveWorkoutStore.current(context)!!
        val weighted = strength(model)
        model.openActiveExerciseLibrary()
        assertEquals(SubScreen.EXERCISE_LIBRARY, model.uiState.value.currentSubScreen)
        model.selectLibraryExercise(weighted)
        await { model.uiState.value.currentSubScreen == SubScreen.ACTIVE_STRENGTH }
        assertEquals(weighted.id, target(ActiveWorkoutStore.current(context)!!).exerciseId)
        assertEquals(before.getString("workoutId"), model.uiState.value.activeWorkoutId)
        assertEquals(before.getLong("generation"), ActiveWorkoutStore.current(context)!!.getLong("generation"))
        model.openActiveExerciseLibrary(); model.selectLibraryExercise(timed(model))
        await { model.uiState.value.currentSubScreen == SubScreen.ACTIVE_BILATERAL }
        val reloaded = this.model()
        assertEquals(SubScreen.ACTIVE_BILATERAL, reloaded.uiState.value.activeWorkoutSubScreen)
        assertEquals(target(ActiveWorkoutStore.current(context)!!).exerciseId, reloaded.uiState.value.selectedExerciseId)
    }

    @Test fun partiallyRecordedSideSurvivesSwapAndItsLateCallbacksCannotTargetTheNewExercise() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model)); awaitStarted(model)
        val before = ActiveWorkoutStore.current(context)!!
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get(), before)
            val previous = target(before)
            NativeWorkoutEditing.startStopwatch(context, previous, "left")
            ShadowSystemClock.advanceBy(Duration.ofSeconds(3))
            val recorded = NativeWorkoutEditing.stopStopwatch(context, previous)
            val originalSet = previous.resolve(recorded).set.toString()
            model.openActiveExerciseLibrary(); model.selectLibraryExercise(strength(model))
            await { model.uiState.value.currentSubScreen == SubScreen.ACTIVE_STRENGTH }
            val changed = ActiveWorkoutStore.current(context)!!
            assertEquals(originalSet, previous.resolve(changed).set.toString())
            assertNotEquals(previous.setId, target(changed).setId)
            assertFalse(target(changed).resolve(changed).set.optBoolean("done"))
            assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.startStopwatch(context, previous, "right") }
            assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.stopStopwatch(context, previous) }
            assertEquals(originalSet, previous.resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set.toString())
        } finally { service.destroy() }
    }

    @Test fun staleLibrarySelectionAfterDiscardRestoreAndNewStartCannotEditTheNewSession() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model)); awaitStarted(model)
        val first = ActiveWorkoutStore.current(context)!!
        model.openActiveExerciseLibrary()
        native { TrainPilotWorkoutFinalization(context, db).discardWorkout(first) }
        native { NativeDataReplacement.exclusively(context) { } }
        model.startQuickStrengthWithExercise(strength(model)); awaitStarted(model)
        val second = ActiveWorkoutStore.current(context)!!
        model.selectLibraryExercise(timed(model))
        await { model.uiState.value.statusBannerMessage != null }
        assertEquals(second.getString("workoutId"), model.uiState.value.activeWorkoutId)
        assertEquals(second.getJSONArray("exercises").toString(), PhoneWorkoutExecution.store(context).snapshot()!!.getJSONArray("exercises").toString())
        assertNotEquals(first.getString("workoutId"), second.getString("workoutId"))
    }

    @Test fun singleStopwatchPauseReopenResumeAndRecordUseOnlyMeasuredTimeOnce() {
        val model = model()
        val single = model.uiState.value.exercises.first { it.measurementType == "time" && it.isTimed && !it.isBilateral }
        model.startQuickTimedWithExercise(single); awaitStarted(model)
        val frame = ActiveWorkoutStore.current(context)!!
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get(), frame)
            val measurement = nativeModel()
            val target = target(frame)
            measurement.startSetStopwatch(target, "single")
            await { measurement.uiState.value.stopwatchRunning }
            ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
            measurement.pauseSetStopwatch(target)
            assertFalse(target.resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set.optBoolean("done"))
            ShadowSystemClock.advanceBy(Duration.ofSeconds(30))
            val reopened = nativeModel()
            assertEquals(2L, reopened.uiState.value.stopwatchSeconds)
            reopened.startSetStopwatch(target, "single")
            await { reopened.uiState.value.stopwatchRunning }
            ShadowSystemClock.advanceBy(Duration.ofSeconds(3))
            reopened.pauseSetStopwatch(target)
            ShadowSystemClock.advanceBy(Duration.ofSeconds(10))
            reopened.recordSetStopwatch(target, "single")
            val recorded = PhoneWorkoutExecution.store(context).snapshot()!!
            assertEquals("5", target.resolve(recorded).set.getString("reps"))
            assertTrue(target.resolve(recorded).set.getBoolean("done"))
            reopened.recordSetStopwatch(target, "single")
            assertEquals(recorded.getLong("executionRevision"), PhoneWorkoutExecution.store(context).snapshot()!!.getLong("executionRevision"))
        } finally { service.destroy() }
    }

    @Test fun bilateralZeroStopAndFirstSideNeverCompleteAndDoubleRecordIsIdempotent() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model)); awaitStarted(model)
        val frame = ActiveWorkoutStore.current(context)!!
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get(), frame)
            val target = target(frame)
            NativeWorkoutEditing.startStopwatch(context, target, "left")
            val zero = NativeWorkoutEditing.stopStopwatch(context, target)
            assertFalse(target.resolve(zero).set.optBoolean("done"))
            assertEquals(0L, target.resolve(zero).set.optLong("leftSeconds"))
            NativeWorkoutEditing.startStopwatch(context, target, "left")
            ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
            val left = NativeWorkoutEditing.stopStopwatch(context, target)
            assertFalse(target.resolve(left).set.optBoolean("done"))
            assertEquals(2L, target.resolve(left).set.optLong("leftSeconds"))
            assertEquals(left.getLong("executionRevision"), NativeWorkoutEditing.stopStopwatch(context, target).getLong("executionRevision"))
            NativeWorkoutEditing.startStopwatch(context, target, "right")
            ShadowSystemClock.advanceBy(Duration.ofSeconds(4))
            val both = NativeWorkoutEditing.stopStopwatch(context, target)
            assertTrue(target.resolve(both).set.optBoolean("done"))
            assertEquals(4L, target.resolve(both).set.optLong("rightSeconds"))
            assertEquals(both.getLong("executionRevision"), NativeWorkoutEditing.stopStopwatch(context, target).getLong("executionRevision"))
        } finally { service.destroy() }
    }

    @Test fun explicitStartWaitsForForegroundArmingWithoutRequiringASecondTap() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model)); awaitStarted(model)
        val frame = ActiveWorkoutStore.current(context)!!
        val measurement = nativeModel()
        measurement.startSetStopwatch(target(frame), "left")
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(measurement.uiState.value.stopwatchRunning)
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get(), frame)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            assertTrue(measurement.uiState.value.stopwatchRunning)
            assertFalse(target(frame).resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set.optBoolean("done"))
        } finally { service.destroy() }
    }

    @Test fun unavailableForegroundArmingReturnsAVisibleReasonAndNeverCreatesMeasuredTime() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model)); awaitStarted(model)
        val frame = ActiveWorkoutStore.current(context)!!
        val measurement = nativeModel()
        measurement.startSetStopwatch(target(frame), "left")
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertFalse(measurement.uiState.value.stopwatchRunning)
        assertTrue(measurement.uiState.value.statusMessage.orEmpty().contains("háttéridőzítő"))
        assertFalse(PhoneWorkoutExecution.store(context).snapshot()!!.has(NativeSetStopwatch.FIELD))
        assertFalse(target(frame).resolve(PhoneWorkoutExecution.store(context).snapshot()!!).set.optBoolean("done"))
    }

    @Test fun duplicateAndLateDiscardCannotDeleteANewerSession() {
        val model = model()
        model.startQuickTimedWithExercise(timed(model)); awaitStarted(model)
        val first = ActiveWorkoutStore.current(context)!!
        val firstId = first.getString("workoutId")
        model.stopActiveWorkoutSession(firstId, first.getLong("generation"))
        model.stopActiveWorkoutSession(firstId, first.getLong("generation"))
        await { !model.uiState.value.isSavingWorkout && !model.uiState.value.isWorkoutSessionActive }
        val tombstone = native { db.workoutHistoryDao().getTombstones().single() }
        model.stopActiveWorkoutSession(firstId, first.getLong("generation"))
        await { !model.uiState.value.isSavingWorkout }
        assertEquals(tombstone, native { db.workoutHistoryDao().getTombstones().single() })
        model.startQuickStrengthWithExercise(strength(model)); awaitStarted(model)
        val second = ActiveWorkoutStore.current(context)!!
        model.stopActiveWorkoutSession(firstId, first.getLong("generation"))
        await { !model.uiState.value.isSavingWorkout }
        assertTrue(PhoneWorkoutExecution.store(context).snapshot()!!.getBoolean("active"))
        assertEquals(second.getString("workoutId"), ActiveWorkoutStore.current(context)!!.getString("workoutId"))
        assertEquals(tombstone, native { db.workoutHistoryDao().getTombstones().single() })
    }

    @Test fun explicitManualSecondsCanBeRecordedWithoutInventingMeasuredWork() {
        val model = model()
        val single = model.uiState.value.exercises.first { it.measurementType == "time" && it.isTimed && !it.isBilateral }
        model.startQuickTimedWithExercise(single); awaitStarted(model)
        val frame = ActiveWorkoutStore.current(context)!!
        // Explicit recording still requires the real editor's armed controller.
        // Manual seconds must not bypass this lifetime/ownership contract.
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get(), frame)
            val measurement = nativeModel()
            val target = target(frame)
            measurement.adjustSetStopwatch(target, "single", 15)
            measurement.adjustSetStopwatch(target, "single", -5)
            val adjusted = PhoneWorkoutExecution.store(context).snapshot()!!
            assertFalse(NativeSetStopwatch.running(adjusted.getJSONObject(NativeSetStopwatch.FIELD)))
            assertEquals(10_000L, NativeSetStopwatch.elapsed(adjusted.getJSONObject(NativeSetStopwatch.FIELD)))
            assertEquals(0L, NativeMeasuredTime.summary(adjusted).workMs)
            measurement.recordSetStopwatch(target, "single")
            val recorded = PhoneWorkoutExecution.store(context).snapshot()!!
            assertEquals("10", target.resolve(recorded).set.getString("reps"))
            assertTrue(target.resolve(recorded).set.getBoolean("done"))
            assertEquals(0L, NativeMeasuredTime.summary(recorded).workMs)
        } finally { service.destroy() }
    }

    private fun awaitStarted(model: TrainPilotViewModel) {
        await { ActiveWorkoutStore.current(context)?.let { it.optBoolean("active") &&
            model.uiState.value.activeWorkoutId == it.optString("workoutId") && model.uiState.value.isWorkoutSessionActive } == true }
    }

    private fun arm(service: IntervalTimerService, frame: JSONObject) {
        service.onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ARM
            putExtra("workoutId", frame.getString("workoutId")); putExtra("generation", frame.getLong("generation"))
        }, 0, 1)
        assertTrue(IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation")))
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            check(System.nanoTime() < deadline) { "Studio session timed out" }
            Thread.sleep(5)
        }
    }

    private fun <T> native(block: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) { "Native session operation timed out" } }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
}
