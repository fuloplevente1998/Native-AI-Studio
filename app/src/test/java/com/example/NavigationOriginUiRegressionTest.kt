package com.example

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.LoggedWorkoutEntry
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.components.TrainPilotRouteContent
import com.example.ui.screens.LogWorkoutDetailScreen
import com.example.ui.screens.ProgressTabScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.ui.theme.TrainPilotButton
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate
import kotlin.math.roundToInt
import com.example.ui.viewmodel.activateProgram
import com.example.ui.viewmodel.addEditorDay
import com.example.ui.viewmodel.addPlannedWorkoutOnDay
import com.example.ui.viewmodel.applyCalendarReplan
import com.example.ui.viewmodel.cancelCalendarClear
import com.example.ui.viewmodel.clearCalendarPlans
import com.example.ui.viewmodel.copyEditorProgram
import com.example.ui.viewmodel.copyProgram
import com.example.ui.viewmodel.createNewCustomProgram
import com.example.ui.viewmodel.deleteCalendarEvent
import com.example.ui.viewmodel.deleteEditorDay
import com.example.ui.viewmodel.deleteEditorProgram
import com.example.ui.viewmodel.deleteProgram
import com.example.ui.viewmodel.modifyCalendarEvent
import com.example.ui.viewmodel.moveCalendarEvent
import com.example.ui.viewmodel.moveEditorExercise
import com.example.ui.viewmodel.moveProgramOrder
import com.example.ui.viewmodel.navigateBack
import com.example.ui.viewmodel.navigateToSubScreen
import com.example.ui.viewmodel.openCalendarEventWorkoutDetail
import com.example.ui.viewmodel.openExerciseDetail
import com.example.ui.viewmodel.openLogWorkoutDetail
import com.example.ui.viewmodel.openProgramEditor
import com.example.ui.viewmodel.planMonthlyWorkouts
import com.example.ui.viewmodel.removeEditorExercise
import com.example.ui.viewmodel.renameEditorProgram
import com.example.ui.viewmodel.renameProgram
import com.example.ui.viewmodel.requestCalendarClear
import com.example.ui.viewmodel.saveEditorProgram
import com.example.ui.viewmodel.selectCalendarDay
import com.example.ui.viewmodel.selectCalendarMonth
import com.example.ui.viewmodel.selectEditingDayIndex
import com.example.ui.viewmodel.selectExpandedStatsCard
import com.example.ui.viewmodel.selectLogDetailTab
import com.example.ui.viewmodel.selectProgressExercise
import com.example.ui.viewmodel.selectProgressMainSection
import com.example.ui.viewmodel.selectProgressRange
import com.example.ui.viewmodel.selectStatsSubTab
import com.example.ui.viewmodel.selectTab
import com.example.ui.viewmodel.selectVolumeBar
import com.example.ui.viewmodel.setProgramsTabCustomOnly
import com.example.ui.viewmodel.toggleCardioFilterStepsMode
import com.example.ui.viewmodel.toggleExpandedEditorExercise
import com.example.ui.viewmodel.toggleInlineProgramEditor
import com.example.ui.viewmodel.toggleReplanModal
import com.example.ui.viewmodel.undoCalendarAction
import com.example.ui.viewmodel.updatePlanningParameters
import com.example.ui.viewmodel.updateProgramExercisePrescription

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class NavigationOriginUiRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun realProgressWorkoutRowAndDetailCloseOrSystemBackRestoreSectionAndScroll() {
        val model = TrainPilotViewModel()
        val workout = LoggedWorkoutEntry(id = "return-workout", titleHu = "Visszatérési próba",
            dateLabelHu = "${LocalDate.now()} 12:00", savedStartIso = "${LocalDate.now()}T12:00:00Z",
            durationMinutes = 40, totalVolumeKg = 1200)
        val field = TrainPilotViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(model) as MutableStateFlow<TrainPilotUiState>
        flow.value = model.uiState.value.copy(currentTab = MainTab.PROGRESS, progressMainSection = 0,
            expandedStatsCardIndex = 2, loggedWorkouts = listOf(workout))
        compose.setContent { TrainPilotTheme {
            val state by model.uiState.collectAsState()
            val strings = TrainPilotStrings.forLanguage(state.language)
            BackHandler(state.currentSubScreen != SubScreen.NONE) { model.navigateBack() }
            Box(Modifier.size(400.dp, 480.dp)) { TrainPilotRouteContent(state) {
                if (state.currentSubScreen == SubScreen.LOG_WORKOUT_DETAIL) {
                    LogWorkoutDetailScreen(state, strings, onBack = { model.navigateBack() }, onOpenSettings = {},
                        onSelectDetailTab = model::selectLogDetailTab, onToggleExpandedExercise = {},
                        onOpenPhotos = {}, onOpenRouteMap = {}, onOpenWatchSync = {}, onOpenHealthTab = {})
                } else {
                    ProgressTabScreen(state, strings, onSelectMainSection = model::selectProgressMainSection,
                        onSelectExercise = model::selectProgressExercise, onSelectRange = model::selectProgressRange,
                        onSelectVolumeBar = model::selectVolumeBar, onSelectExpandedStatsCard = model::selectExpandedStatsCard,
                        onSelectStatsSubTab = model::selectStatsSubTab, onToggleCardioFilterStepsMode = model::toggleCardioFilterStepsMode,
                        onOpenWorkoutDetail = model::openLogWorkoutDetail, onOpenSettings = {}, onOpenPalettePicker = {})
                }
            } }
        } }
        for (systemBack in listOf(false, true)) {
            // The row can already fit on a compact layout: deliberately establish a
            // nonzero origin first rather than assuming a particular card height.
            compose.onNodeWithTag("progress_overview_list").performTouchInput { swipeUp() }
            val row = compose.onNodeWithTag("progress_workout_${workout.id}").performScrollTo()
            val scroll = compose.onNodeWithTag("progress_overview_list")
                .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            assertTrue("The origin must be deliberately scrolled", scroll > 0f)
            if (!systemBack) {
                val bounds = row.fetchSemanticsNode().boundsInRoot
                fun pixels() = compose.runOnIdle {
                    // 2771: the workout rows live in the stats details bottom sheet (its own window).
                    val host = (row.fetchSemanticsNode().root as androidx.compose.ui.platform.ViewRootForTest).view
                    val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
                    host.draw(Canvas(bitmap))
                    bitmap.asImageBitmap().toPixelMap()
                }
                val before = pixels()
                row.performTouchInput { down(center) }
                // Scrollable clickables defer their press indication to distinguish a drag.
                compose.runOnIdle {
                    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                        .idleFor(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                }
                compose.mainClock.advanceTimeBy(200)
                val held = pixels()
                val y = (bounds.top + 4).roundToInt()
                for (x in listOf((bounds.left + 12).roundToInt(), (bounds.right - 12).roundToInt()))
                    assertNotEquals("Whole workout row must react, including its ends", before[x, y].toArgb(), held[x, y].toArgb())
                row.performTouchInput { cancel() }
                compose.runOnIdle { assertEquals(SubScreen.NONE, model.uiState.value.currentSubScreen) }
            }
            row.performClick()
            compose.onNodeWithTag("log_workout_detail_screen").assertExists()
            if (systemBack) compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            else compose.onNodeWithTag("subscreen_back_button").performScrollTo().performClick()
            compose.onNodeWithTag("progress_overview_list").assertExists()
            val returned = compose.onNodeWithTag("progress_overview_list")
                .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            assertTrue("The restored view must remain scrolled", returned > 0f)
            assertEquals("Return to the same scrolled origin", scroll, returned, 0.000001f)
            compose.runOnIdle {
                assertEquals(0, model.uiState.value.progressMainSection)
                assertEquals(2, model.uiState.value.expandedStatsCardIndex)
            }
        }
    }

    @Test fun aPoppedEditorDoesNotResurrectItsCancelledLocalDraft() {
        val model = TrainPilotViewModel()
        model.navigateToSubScreen(SubScreen.SETTINGS_MAIN)
        compose.setContent { TrainPilotTheme {
            val state by model.uiState.collectAsState()
            TrainPilotRouteContent(state) {
                if (state.currentSubScreen == SubScreen.BACKUP_RESTORE) {
                    var draft by rememberSaveable { mutableIntStateOf(0) }
                    TrainPilotButton(onClick = { draft++ }, modifier = Modifier.testTag("local_draft")) { Text("$draft") }
                } else Text("Settings")
            }
        } }
        compose.runOnIdle { model.navigateToSubScreen(SubScreen.BACKUP_RESTORE) }
        compose.onNodeWithTag("local_draft").performClick().assertTextEquals("1")
        compose.runOnIdle { model.navigateBack() }
        compose.onNodeWithText("Settings").assertExists()
        compose.runOnIdle { model.navigateToSubScreen(SubScreen.BACKUP_RESTORE) }
        compose.onNodeWithTag("local_draft").assertTextEquals("0")
    }

    @Test fun programsLongPressAndMoreButtonOpenActionSheetAndEditReturnsDirectlyWithTabAndScroll() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = com.repforge.app.data.store.TrainPilotSettingsStore(context)
        val dbField = com.repforge.app.data.db.TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        (dbField.get(null) as? com.repforge.app.data.db.TrainPilotDatabase)?.close()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.repforge.app.data.db.TrainPilotDatabase::class.java)
            .allowMainThreadQueries().build()
        dbField.set(null, db)
        val job = kotlinx.coroutines.SupervisorJob()
        val scope = kotlinx.coroutines.CoroutineScope(job + kotlinx.coroutines.Dispatchers.Main.immediate)
        try {
            val model = TrainPilotViewModel()
            model.bindNativeEnvironment(context, store, db, scope)
            compose.waitUntil(20_000) {
                androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                !model.uiState.value.savedRecordsLoading && model.uiState.value.programs.isNotEmpty()
            }
            // Create two custom programs so Custom tab has multiple items
            model.createNewCustomProgram("Alpha Egyéni", "Teszt 1")
            compose.waitUntil(10_000) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                model.uiState.value.programs.any { it.nameHu == "Alpha Egyéni" }
            }
            model.navigateBack()
            model.createNewCustomProgram("Beta Egyéni", "Teszt 2")
            compose.waitUntil(10_000) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                model.uiState.value.programs.any { it.nameHu == "Beta Egyéni" }
            }
            model.navigateBack()
            model.selectTab(MainTab.PROGRAMS)
            model.setProgramsTabCustomOnly(true)

            val alpha = model.uiState.value.programs.first { it.nameHu == "Alpha Egyéni" }
            val beta = model.uiState.value.programs.first { it.nameHu == "Beta Egyéni" }

            compose.setContent {
                TrainPilotTheme {
                    val state by model.uiState.collectAsState()
                    BackHandler(state.currentSubScreen != SubScreen.NONE) { model.navigateBack() }
                    Box(Modifier.size(400.dp, 640.dp)) {
                        TrainPilotRouteContent(state) {
                            if (state.currentSubScreen == SubScreen.PROGRAM_EDITOR) {
                                com.example.ui.screens.ProgramEditorScreen(
                                    state = state,
                                    onBack = { model.navigateBack() },
                                    onOpenSettings = {},
                                    onSelectDayIndex = model::selectEditingDayIndex,
                                    onToggleExpandExercise = model::toggleExpandedEditorExercise,
                                    onSaveExercisePrescription = model::updateProgramExercisePrescription,
                                    onOpenExerciseDetail = model::openExerciseDetail,
                                    onOpenExerciseLibrary = {},
                                    onMoveExercise = model::moveEditorExercise,
                                    onCopyProgram = model::copyEditorProgram,
                                    onDeleteProgram = model::deleteEditorProgram,
                                    onAddDay = model::addEditorDay,
                                    onDeleteDay = model::deleteEditorDay,
                                    onRemoveExercise = model::removeEditorExercise,
                                    onRenameProgram = model::renameEditorProgram,
                                    onSaveProgram = model::saveEditorProgram
                                )
                            } else {
                                com.example.ui.screens.ProgramsListScreen(
                                    state = state,
                                    onBack = { model.navigateBack() },
                                    onOpenSettings = {},
                                    onToggleCustomTab = model::setProgramsTabCustomOnly,
                                    onActivateProgram = model::activateProgram,
                                    onOpenProgramEditor = { id -> model.openProgramEditor(id, 0) },
                                    onCopyProgram = model::copyProgram,
                                    onDeleteProgram = model::deleteProgram,
                                    onOpenExerciseLibrary = {},
                                    onOpenPersonalPlanner = {},
                                    onMoveProgramOrder = model::moveProgramOrder,
                                    onCreateCustomProgram = model::createNewCustomProgram,
                                    onToggleInlineEditor = { id, host -> model.toggleInlineProgramEditor(id, 0, host) },
                                    onSaveInlineDrafts = model::saveEditorProgram,
                                    onRenameProgram = model::renameProgram,
                                    inlineEditor = { program, pending ->
                                        com.example.ui.screens.ProgramEditorContent(
                                            state = state,
                                            program = program,
                                            pendingPrescriptions = pending,
                                            showNameField = false,
                                            onSelectDayIndex = model::selectEditingDayIndex,
                                            onToggleExpandExercise = model::toggleExpandedEditorExercise,
                                            onSaveExercisePrescription = model::updateProgramExercisePrescription,
                                            onOpenExerciseDetail = model::openExerciseDetail,
                                            onOpenExerciseLibrary = {},
                                            onMoveExercise = model::moveEditorExercise,
                                            onCopyProgram = model::copyEditorProgram,
                                            onDeleteProgram = model::deleteEditorProgram,
                                            onAddDay = model::addEditorDay,
                                            onDeleteDay = model::deleteEditorDay,
                                            onRemoveExercise = model::removeEditorExercise,
                                            onRenameProgram = model::renameEditorProgram,
                                            onSaveProgram = model::saveEditorProgram
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 1. Long-press on card opens Program Action Sheet
            compose.onNodeWithTag("program_card_${alpha.id}").performScrollTo()
                .performTouchInput { longClick() }
            compose.onNodeWithTag("program_action_sheet").assertIsDisplayed()
            compose.onNodeWithTag("program_action_sheet_title").assertTextEquals("Alpha Egyéni")

            // Closing via X dismisses sheet and stays on Custom tab
            compose.onNodeWithTag("program_action_sheet_close").performClick()
            compose.onNodeWithTag("program_action_sheet").assertDoesNotExist()
            assertTrue(model.uiState.value.programsTabCustomOnly)

            // 2. Dedicated 48dp more button has TalkBack contentDescription and opens same sheet
            val moreBtn = compose.onNodeWithTag("program_more_button_${alpha.id}").performScrollTo()
            val cd = moreBtn.fetchSemanticsNode().config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
            assertTrue("More button must expose program-identifying TalkBack description",
                cd.any { it.contains("Alpha Egyéni") })
            moreBtn.performClick()
            compose.onNodeWithTag("program_action_sheet").assertIsDisplayed()

            // 2773: the ⋮ sheet no longer repeats Activate / Edit (both stay on the card itself).
            compose.onNodeWithTag("program_action_activate").assertDoesNotExist()
            compose.onNodeWithTag("program_action_edit").assertDoesNotExist()
            compose.onNodeWithTag("program_action_rename").assertExists()
            compose.onNodeWithTag("program_action_sheet_close").performClick()
            compose.onNodeWithTag("program_action_sheet").assertDoesNotExist()
            // 3. Tapping the card header expands the editor inside this card: no separate page, tab and list stay.
            compose.onNodeWithTag("program_header_${alpha.id}").performScrollTo().performClick()
            assertEquals(SubScreen.NONE, model.uiState.value.currentSubScreen)
            assertEquals(MainTab.PROGRAMS, model.uiState.value.currentTab)
            assertEquals(alpha.id, model.uiState.value.editingProgramId)
            assertEquals(alpha.id, model.uiState.value.inlineEditorProgramId)
            assertTrue("Custom tab selection must be preserved", model.uiState.value.programsTabCustomOnly)
            compose.onNodeWithTag("program_inline_editor_${alpha.id}", useUnmergedTree = true).assertExists()

            // Only one card is open: tapping another card's header moves the editor there.
            compose.onNodeWithTag("program_header_${beta.id}").performScrollTo().performClick()
            assertEquals(beta.id, model.uiState.value.inlineEditorProgramId)
            assertEquals(beta.id, model.uiState.value.editingProgramId)
            compose.onNodeWithTag("program_inline_editor_${alpha.id}", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithTag("program_inline_editor_${beta.id}", useUnmergedTree = true).assertExists()
            // Tapping the open card again collapses it; nothing else changes.
            compose.onNodeWithTag("program_header_${beta.id}").performScrollTo().performClick()
            assertEquals(null, model.uiState.value.inlineEditorProgramId)
            assertEquals(SubScreen.NONE, model.uiState.value.currentSubScreen)
            assertTrue(model.uiState.value.programs.any { it.id == alpha.id } && model.uiState.value.programs.any { it.id == beta.id })
            compose.onNodeWithTag("program_action_sheet").assertDoesNotExist()

            // 4. Delete confirmation identifies program by name; cancel keeps program; missing-record guard protects next item
            compose.onNodeWithTag("program_more_button_${alpha.id}").performScrollTo().performClick()
            compose.onNodeWithTag("program_action_delete").performClick()
            compose.onNodeWithTag("program_delete_confirm_dialog").assertIsDisplayed()
            compose.onNodeWithTag("program_delete_confirm_title").assertTextContains("Alpha Egyéni", substring = true)
            compose.onNodeWithTag("program_delete_cancel_button").performClick()
            compose.onNodeWithTag("program_delete_confirm_dialog").assertDoesNotExist()
            assertTrue(model.uiState.value.programs.any { it.id == alpha.id })

            // Simulate concurrent disappearance of alpha in Room before delete confirm
            compose.onNodeWithTag("program_more_button_${alpha.id}").performScrollTo().performClick()
            compose.onNodeWithTag("program_action_delete").performClick()
            compose.onNodeWithTag("program_delete_confirm_dialog").assertIsDisplayed()
            kotlinx.coroutines.runBlocking {
                db.programDao().deleteDaysForProgram(alpha.id)
                db.programDao().deleteCustomProgram(alpha.id)
            }
            compose.onNodeWithTag("program_delete_confirm_button").performClick()
            compose.waitUntil(10_000) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                model.uiState.value.statusBannerMessage.orEmpty().contains("nem található")
            }
            // Beta must remain untouched in Room and UI
            assertNotNull(kotlinx.coroutines.runBlocking { db.programDao().getProgramWithDaysById(beta.id) })
            assertTrue(model.uiState.value.programs.any { it.id == beta.id })
        } finally {
            job.cancel()
            db.close()
            dbField.set(null, null)
        }
    }

    @Test fun programMenuShowsDeleteForCustomProgramsWithActiveProgramProtection() {
        val own = com.example.model.TrainingProgramModel("own-delete", "Saját", "Custom",
            "", "", emptyList(), isCustom = true, isActive = true, days = emptyList())
        val builtIn = own.copy(id = "builtin-delete", nameHu = "Beépített", nameEn = "Built-in",
            isCustom = false, isActive = false)
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.ProgramsListScreen(
                TrainPilotUiState(programs = listOf(own, builtIn), programsTabCustomOnly = false),
                {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("program_more_button_${own.id}").performScrollTo().performClick()
        compose.onNodeWithTag("program_action_delete").assertIsNotEnabled()
        compose.onNodeWithTag("program_action_delete_unavailable_hint").assertExists()
        compose.onNodeWithTag("program_action_sheet_close").performClick()
        compose.onNodeWithTag("program_more_button_${builtIn.id}").performScrollTo().performClick()
        compose.onNodeWithTag("program_action_delete").assertDoesNotExist()
    }

    @Test fun calendarCompletedWorkoutOpensMatchingLoggedWorkoutAndPreservesMonthAndDayOnBack() {
        val model = TrainPilotViewModel()
        val firstDifferentWorkout = LoggedWorkoutEntry(
            id = "older_unrelated_workout",
            scheduleId = "ev_unrelated",
            titleHu = "Másik edzés",
            dateLabelHu = "2026-11-01 10:00",
            savedStartIso = "2026-11-01T10:00:00Z",
            durationMinutes = 30,
            totalVolumeKg = 800
        )
        val matchingWorkout = LoggedWorkoutEntry(
            id = "target_calendar_workout",
            scheduleId = "ev_completed_target",
            titleHu = "Célzott Naptári Edzés",
            dateLabelHu = "2026-11-15 18:00",
            savedStartIso = "2026-11-15T18:00:00Z",
            durationMinutes = 55,
            totalVolumeKg = 2450
        )
        val completedWithLog = com.example.model.CalendarDayEvent(
            id = "ev_completed_target",
            dayOfMonth = 15,
            timeLabel = "18:00",
            titleHu = "Célzott Naptári Edzés",
            exerciseCount = 5,
            durationMinutes = 55,
            status = com.example.model.CalendarEventStatus.COMPLETED,
            splitBadge = "A",
            dateIso = "2026-11-15",
            programId = "prog_1",
            dayId = "day_a"
        )
        val completedWithoutLog = com.example.model.CalendarDayEvent(
            id = "ev_completed_missing_log",
            dayOfMonth = 15,
            timeLabel = "19:30",
            titleHu = "Napló nélküli esemény",
            exerciseCount = 4,
            durationMinutes = 45,
            status = com.example.model.CalendarEventStatus.COMPLETED,
            splitBadge = "B",
            dateIso = "2026-11-15",
            programId = "prog_missing",
            dayId = "day_missing"
        )
        val field = TrainPilotViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(model) as MutableStateFlow<TrainPilotUiState>
        flow.value = model.uiState.value.copy(
            currentTab = MainTab.CALENDAR,
            calendarYearMonth = "2026-11",
            selectedCalendarDay = 15,
            loggedWorkouts = listOf(firstDifferentWorkout, matchingWorkout),
            calendarEvents = listOf(completedWithLog, completedWithoutLog)
        )

        compose.setContent {
            TrainPilotTheme {
                val state by model.uiState.collectAsState()
                val strings = TrainPilotStrings.forLanguage(state.language)
                BackHandler(state.currentSubScreen != SubScreen.NONE) { model.navigateBack() }
                Box(Modifier.size(400.dp, 700.dp)) {
                    TrainPilotRouteContent(state) {
                        if (state.currentSubScreen == SubScreen.LOG_WORKOUT_DETAIL) {
                            LogWorkoutDetailScreen(
                                state,
                                strings,
                                onBack = { model.navigateBack() },
                                onOpenSettings = {},
                                onSelectDetailTab = model::selectLogDetailTab,
                                onToggleExpandedExercise = {},
                                onOpenPhotos = {},
                                onOpenRouteMap = {},
                                onOpenWatchSync = {},
                                onOpenHealthTab = {}
                            )
                        } else {
                            com.example.ui.screens.CalendarPlannerScreen(
                                state = state,
                                onBack = { model.navigateBack() },
                                onOpenSettings = {},
                                onSelectDay = model::selectCalendarDay,
                                onSelectMonth = model::selectCalendarMonth,
                                onOpenReplanModal = model::toggleReplanModal,
                                onApplyReplan = model::applyCalendarReplan,
                                onOpenWorkoutDetail = model::openCalendarEventWorkoutDetail,
                                onUpdatePlanningParams = model::updatePlanningParameters,
                                onPlanMonthlyWorkouts = model::planMonthlyWorkouts,
                                onRequestClearCalendar = model::requestCalendarClear,
                                onConfirmClearCalendar = model::clearCalendarPlans,
                                onCancelClearCalendar = model::cancelCalendarClear,
                                onModifyCalendarEvent = model::modifyCalendarEvent,
                                onMoveCalendarEvent = model::moveCalendarEvent,
                                onDeleteCalendarEvent = model::deleteCalendarEvent,
                                onAddPlannedWorkoutOnDay = model::addPlannedWorkoutOnDay,
                                onUndoCalendarAction = model::undoCalendarAction
                            )
                        }
                    }
                }
            }
        }

        // 1. Missing-log event must NOT open firstOrNull() workout; stays on Calendar with status message
        compose.onNodeWithTag("calendar_day_15").performScrollTo().performClick()
        compose.onNodeWithTag("calendar_open_workout_detail_ev_completed_missing_log")
            .performScrollTo().performClick()
        assertEquals(SubScreen.NONE, model.uiState.value.currentSubScreen)
        assertTrue(model.uiState.value.statusBannerMessage.orEmpty().contains("nem található naplózott edzés"))

        // 2. Event with matching log opens target_calendar_workout (not older_unrelated_workout)
        compose.onNodeWithTag("calendar_day_15").performScrollTo().performClick()
        compose.onNodeWithTag("calendar_open_workout_detail_ev_completed_target")
            .performScrollTo().performClick()
        assertEquals(SubScreen.LOG_WORKOUT_DETAIL, model.uiState.value.currentSubScreen)
        assertEquals("target_calendar_workout", model.uiState.value.selectedLogWorkoutId)

        // 3. Back restores exact calendarYearMonth and selectedCalendarDay
        compose.runOnIdle { model.navigateBack() }
        assertEquals(SubScreen.NONE, model.uiState.value.currentSubScreen)
        assertEquals("2026-11", model.uiState.value.calendarYearMonth)
        assertEquals(15, model.uiState.value.selectedCalendarDay)
    }

    @Test fun settingsTrainingProfileDialogUsesUnifiedPersonalPlannerDropdowns() {
        var savedFields: Map<String, String>? = null
        compose.setContent {
            TrainPilotTheme {
                com.repforge.app.ui.settings.TrainingProfileDialog(
                    profileJson = """{"height":182,"weight":76.5,"daysPerWeek":4,"minutes":60,"goal":"Erőnövelés"}""",
                    language = "hu",
                    saving = false,
                    failed = false,
                    onSave = { savedFields = it },
                    onDismiss = {}
                )
            }
        }
        compose.onNodeWithTag("profile_height_box").assertIsDisplayed().assertTextContains("182 cm")
        compose.onNodeWithTag("profile_field_height").assertDoesNotExist()
        compose.onNodeWithTag("planner_save_button").performClick()
        assertNotNull(savedFields)
        assertEquals("182", savedFields?.get("height"))
        assertEquals("4", savedFields?.get("daysPerWeek"))
        assertEquals("60", savedFields?.get("minutes"))
    }
}

