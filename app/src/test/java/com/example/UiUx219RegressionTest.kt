package com.example

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.*
import com.example.ui.screens.*
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.HealthJournalStore
import com.repforge.app.healthreadings.PulseDayReading
import com.repforge.app.healthreadings.ReadingState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate

class GcsMirrorRobolectricTestRunner(testClass: Class<*>) : RobolectricTestRunner(
    testClass.also {
        System.setProperty(
            "robolectric.dependency.repo.url",
            "https://maven-central.storage-download.googleapis.com/maven2"
        )
    }
)

@RunWith(GcsMirrorRobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class UiUx219RegressionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun programCardsHaveCompactActivationButtonWith48dpHitTargetAndGenerousTitleWidth() {
        val activeProg = TrainingProgramModel(
            id = "home-basic",
            nameHu = "Otthoni A/B",
            nameEn = "Home A/B",
            subtitleHu = "Otthon • Kezdő • 2 edzésnap",
            subtitleEn = "Home • Beginner • 2 days",
            tagsHu = emptyList(),
            isCustom = false,
            isActive = true,
            days = listOf(ProgramDayModel("d1", "A", "A nap", "Day A", 60, emptyList()))
        )
        val level2Prog = TrainingProgramModel(
            id = "home-level2",
            nameHu = "Otthoni A/B – 2. szint",
            nameEn = "Home A/B – Level 2",
            subtitleHu = "Otthon • Középhaladó • 2 edzésnap",
            subtitleEn = "Home • Intermediate • 2 days",
            tagsHu = emptyList(),
            isCustom = false,
            isActive = false,
            days = listOf(
                ProgramDayModel("d1", "A", "A nap", "Day A", 60, emptyList()),
                ProgramDayModel("d2", "B", "B nap", "Day B", 60, emptyList())
            )
        )
        var activatedId: String? = null
        val state = TrainPilotUiState(
            language = AppLanguage.HU,
            programs = listOf(activeProg, level2Prog)
        )

        compose.setContent {
            TrainPilotTheme {
                Box(Modifier.width(354.dp)) {
                    ProgramsListScreen(
                        state = state,
                        onBack = {},
                        onOpenSettings = {},
                        onToggleCustomTab = {},
                        onActivateProgram = { activatedId = it },
                        onOpenProgramEditor = {},
                        onOpenExerciseLibrary = {}
                    )
                }
            }
        }

        val hitTarget = compose.onNodeWithTag("program_activate_button_home-level2").getUnclippedBoundsInRoot()
        val visualPill = compose.onNodeWithTag("program_activate_visual_home-level2", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val cardBounds = compose.onNodeWithTag("program_card_home-level2").getUnclippedBoundsInRoot()

        assertTrue("Activation button touch target width must be >= 48dp", hitTarget.right - hitTarget.left >= 48.dp)
        assertTrue("Activation button touch target height must be >= 48dp", hitTarget.bottom - hitTarget.top >= 48.dp)
        assertTrue("Visible activation pill must be compact (< 88dp wide)", visualPill.right - visualPill.left < 88.dp)
        assertTrue("Program card height must remain compact (< 130dp)", cardBounds.bottom - cardBounds.top < 130.dp)

        compose.onNodeWithText("Otthoni A/B – 2. szint").assertIsDisplayed()
        compose.onNodeWithTag("program_activate_button_home-level2").performClick()
        assertEquals("home-level2", activatedId)
    }

    @Test
    fun programCardsSupportCollapsibleDetailsThreeZoneLayoutThemesAndLocalization() {
        val multiEquipExercises = listOf(
            ExerciseItem(id = "bench-press", nameHu = "Fekvenyomás", equipmentHu = "Kétkezes rúd + pad", loadType = "barbell"),
            ExerciseItem(id = "db-curl", nameHu = "Bicepsz", equipmentHu = "Kézisúlyzó", loadType = "dumbbell"),
            ExerciseItem(id = "band-row", nameHu = "Evezés", equipmentHu = "Gumiszalag", loadType = "band"),
            ExerciseItem(id = "leg-press", nameHu = "Lábtoló", equipmentHu = "Lábtoló gép", loadType = "machine"),
            ExerciseItem(id = "lat-pulldown", nameHu = "Lehúzás", equipmentHu = "Csigás gép", loadType = "cable"),
            ExerciseItem(id = "pushup", nameHu = "Fekvőtámasz", equipmentHu = "Saját testsúly", loadType = "bodyweight")
        )
        val activeProg = TrainingProgramModel(
            id = "home-basic",
            nameHu = "Otthoni A/B",
            nameEn = "Home A/B",
            subtitleHu = "Otthon • Kezdő • 2 edzésnap",
            subtitleEn = "Home • Beginner • 2 workout days",
            tagsHu = listOf("Otthon", "Kezdő"),
            isCustom = false,
            isActive = true,
            days = listOf(ProgramDayModel("d1", "A", "A nap", "Day A", 60, listOf(multiEquipExercises[1], multiEquipExercises[5])))
        )
        val gymProg = TrainingProgramModel(
            id = "gym-strength-abc",
            nameHu = "Konditermi erő A/B/C – Hosszú összetett programnév teszt",
            nameEn = "Gym Strength A/B/C – Long compound program name test",
            subtitleHu = "Konditerem • Haladó • 3 edzésnap",
            subtitleEn = "Gym • Advanced • 3 workout days",
            tagsHu = listOf("Konditerem", "Haladó"),
            isCustom = false,
            isActive = false,
            days = listOf(
                ProgramDayModel("g1", "A", "A nap", "Day A", 60, multiEquipExercises),
                ProgramDayModel("g2", "B", "B nap", "Day B", 60, multiEquipExercises),
                ProgramDayModel("g3", "C", "C nap", "Day C", 60, multiEquipExercises)
            )
        )
        val thirdProg = TrainingProgramModel(
            id = "home-quick-30",
            nameHu = "Gyors 30 perces",
            nameEn = "Quick 30-min",
            subtitleHu = "Otthon • Kezdő • 2 edzésnap",
            subtitleEn = "Home • Beginner • 2 workout days",
            tagsHu = listOf("Otthon", "Kezdő"),
            isCustom = false,
            isActive = false,
            days = listOf(ProgramDayModel("q1", "A", "A nap", "Day A", 30, listOf(multiEquipExercises[5])))
        )

        var activatedId: String? = null
        var editorOpenedId: String? = null
        var movedPair: Pair<Int, Int>? = null
        var currentLang by mutableStateOf(AppLanguage.HU)
        val darkPalette = com.example.ui.theme.TrainPilotPalettes.allPalettes.first { it.isDark }
        val lightPalette = com.example.ui.theme.TrainPilotPalettes.allPalettes.first { !it.isDark }
        val altDarkPalette = com.example.ui.theme.TrainPilotPalettes.allPalettes.filter { it.isDark }.getOrElse(2) { darkPalette }
        var currentPalette by mutableStateOf(darkPalette)
        var viewportWidth by mutableStateOf(320.dp)

        compose.setContent {
            TrainPilotTheme(palette = currentPalette) {
                Box(Modifier.width(viewportWidth)) {
                    ProgramsListScreen(
                        state = TrainPilotUiState(
                            language = currentLang,
                            selectedPaletteId = currentPalette.id,
                            programs = listOf(activeProg, gymProg, thirdProg)
                        ),
                        onBack = {},
                        onOpenSettings = {},
                        onToggleCustomTab = {},
                        onActivateProgram = { activatedId = it },
                        onOpenProgramEditor = { editorOpenedId = it },
                        onOpenExerciseLibrary = {},
                        onMoveProgramOrder = { from, to -> movedPair = from to to }
                    )
                }
            }
        }

        // 1. Collapsed state by default: details panels are hidden, active program has badge (no activate button), inactive has activate button
        compose.onNodeWithTag("program_details_panel_gym-strength-abc", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("program_details_panel_home-basic", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("program_active_badge_home-basic", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("program_activate_button_home-basic").assertDoesNotExist()
        compose.onNodeWithTag("program_activate_button_gym-strength-abc").assertIsDisplayed()

        // 2. Small screen (320dp) + long title: 3 zones do not overlap, 3-dot menu is at right edge with >= 48x48dp hit target
        val cardBoundsCollapsed = compose.onNodeWithTag("program_card_gym-strength-abc").getUnclippedBoundsInRoot()
        val titleBounds = compose.onNodeWithTag("program_title_gym-strength-abc", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val activateBounds = compose.onNodeWithTag("program_activate_button_gym-strength-abc").getUnclippedBoundsInRoot()
        val detailsToggleBounds = compose.onNodeWithTag("program_details_toggle_gym-strength-abc").getUnclippedBoundsInRoot()
        val moreBounds = compose.onNodeWithTag("program_more_button_gym-strength-abc").getUnclippedBoundsInRoot()

        assertTrue("Title must not overlap activation column", titleBounds.right <= activateBounds.left + 1.dp)
        assertTrue("Details toggle must be directly below activation button", detailsToggleBounds.top >= activateBounds.bottom - 2.dp)
        assertTrue("Three-dot menu must have >= 48dp width", moreBounds.right - moreBounds.left >= 48.dp)
        assertTrue("Three-dot menu must have >= 48dp height", moreBounds.bottom - moreBounds.top >= 48.dp)
        assertTrue("Three-dot menu must not overlap activation button or details toggle",
            moreBounds.left >= activateBounds.right - 1.dp && moreBounds.left >= detailsToggleBounds.right - 1.dp)
        assertTrue("Three-dot menu must align to the right edge of the card (2-4dp inner margin)",
            cardBoundsCollapsed.right - moreBounds.right <= 6.dp)

        // 3. Opening Details expands full-width panel below main row without activating or opening editor
        compose.onNodeWithTag("program_details_toggle_gym-strength-abc")
            .assertTextEquals("Részletek ▼")
        compose.onNodeWithTag("program_details_toggle_gym-strength-abc").performClick()
        assertNull("Opening details must not activate the program", activatedId)
        assertNull("Opening details must not open the program editor", editorOpenedId)

        compose.onNodeWithTag("program_details_toggle_gym-strength-abc")
            .assertTextEquals("Részletek ▲")
        compose.onNodeWithTag("program_details_panel_gym-strength-abc", useUnmergedTree = true).assertIsDisplayed()
        // Bound to program ID: other cards remain collapsed
        compose.onNodeWithTag("program_details_panel_home-quick-30", useUnmergedTree = true).assertDoesNotExist()

        val cardBoundsExpanded = compose.onNodeWithTag("program_card_gym-strength-abc").getUnclippedBoundsInRoot()
        val panelBounds = compose.onNodeWithTag("program_details_panel_gym-strength-abc", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Card height must grow naturally when long equipment details expand",
            cardBoundsExpanded.bottom - cardBoundsExpanded.top > cardBoundsCollapsed.bottom - cardBoundsCollapsed.top)
        assertTrue("Details panel must span full card width below the main row",
            panelBounds.right - panelBounds.left >= (cardBoundsExpanded.right - cardBoundsExpanded.left) - 30.dp &&
                panelBounds.top >= detailsToggleBounds.bottom)

        compose.onNodeWithTag("program_details_location_gym-strength-abc", useUnmergedTree = true)
            .assertTextContains("Helyszín: Konditermi", substring = true)
        compose.onNodeWithTag("program_details_equipment_gym-strength-abc", useUnmergedTree = true)
            .assertTextContains("Felszerelések:", substring = true)
            .assertTextContains("biztonságos állvány", substring = true)
            .assertTextContains("csigás gép", substring = true)

        // 4. Localization (EN, DE, RO) and theme switches (Light / Dark palettes) preserve expanded state
        compose.runOnIdle {
            currentLang = AppLanguage.EN
            currentPalette = lightPalette
        }
        compose.onNodeWithTag("program_details_toggle_gym-strength-abc")
            .assertTextEquals("Details ▲")
        compose.onNodeWithTag("program_details_location_gym-strength-abc", useUnmergedTree = true)
            .assertTextContains("Location: Gym", substring = true)
        compose.onNodeWithTag("program_details_equipment_gym-strength-abc", useUnmergedTree = true)
            .assertTextContains("Equipment:", substring = true)

        compose.runOnIdle {
            currentLang = AppLanguage.DE
            currentPalette = altDarkPalette
        }
        compose.onNodeWithTag("program_details_location_gym-strength-abc", useUnmergedTree = true)
            .assertTextContains("Ort: Fitnessstudio", substring = true)
        compose.onNodeWithTag("program_details_equipment_gym-strength-abc", useUnmergedTree = true)
            .assertTextContains("Ausrüstung:", substring = true)

        compose.runOnIdle { currentLang = AppLanguage.RO }
        compose.onNodeWithTag("program_details_toggle_gym-strength-abc")
            .assertTextEquals("Detalii ▲")
        compose.onNodeWithTag("program_details_location_gym-strength-abc", useUnmergedTree = true)
            .assertTextContains("Locație: Sală", substring = true)
        compose.onNodeWithTag("program_details_equipment_gym-strength-abc", useUnmergedTree = true)
            .assertTextContains("Echipament:", substring = true)

        // 5. Closing Details collapses the panel back to Részletek ▼
        compose.runOnIdle { currentLang = AppLanguage.HU }
        compose.onNodeWithTag("program_details_toggle_gym-strength-abc").performClick()
        compose.onNodeWithTag("program_details_panel_gym-strength-abc", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("program_details_toggle_gym-strength-abc")
            .assertTextEquals("Részletek ▼")

        // 6. Active program card also has working Részletek toggle
        compose.onNodeWithTag("program_details_toggle_home-basic").performClick()
        compose.onNodeWithTag("program_details_panel_home-basic", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("program_details_location_home-basic", useUnmergedTree = true)
            .assertTextContains("Helyszín: Otthoni", substring = true)

        // 7. Reorder controls, 3-dot menu, and long-press continue to work for the exact program ID
        compose.onNodeWithTag("program_move_down_gym-strength-abc").performClick()
        assertEquals(0 to 1, movedPair)

        compose.onNodeWithTag("program_more_button_gym-strength-abc").performClick()
        compose.onNodeWithTag("program_action_sheet").assertIsDisplayed()
        compose.onNodeWithTag("program_action_sheet_title")
            .assertTextContains("Konditermi erő A/B/C", substring = true)
        compose.onNodeWithTag("program_action_sheet_close").performClick()
        compose.onNodeWithTag("program_action_sheet").assertDoesNotExist()

        compose.onNodeWithTag("program_card_home-quick-30").performScrollTo().performTouchInput { longClick() }
        compose.onNodeWithTag("program_action_sheet").assertIsDisplayed()
        compose.onNodeWithTag("program_action_sheet_title").assertTextEquals("Gyors 30 perces")
        compose.onNodeWithTag("program_action_sheet_close").performClick()
    }

    @Test
    fun exercisePickerRemovesStickFiguresAndReclaimsWidthForNamesAndEquipment() {
        val arnold = ExerciseItem(
            id = "arnold-press",
            nameHu = "Arnold-nyomás",
            nameEn = "Arnold Press",
            category = ExerciseCategory.SHOULDERS,
            equipmentHu = "2 kézisúlyzó",
            primaryMusclesHu = "Váll",
            secondaryMusclesHu = "Tricepsz",
            defaultSets = 3,
            defaultReps = 10
        )
        var selectedId: String? = null
        var queuedId: String? = null
        var startedId: String? = null
        val state = TrainPilotUiState(
            language = AppLanguage.HU,
            exercises = listOf(arnold)
        )

        compose.setContent {
            TrainPilotTheme {
                Box(Modifier.width(354.dp)) {
                    ExerciseLibraryScreen(
                        state = state,
                        onBack = {},
                        onOpenSettings = {},
                        onSearchQueryChange = {},
                        onSelectCategory = {},
                        onSelectExercise = { selectedId = it },
                        onStartExerciseNow = { startedId = it.id },
                        onAddExerciseToQueue = { queuedId = it.id }
                    )
                }
            }
        }

        val rowBounds = compose.onNodeWithTag("exercise_library_row_arnold-press").getUnclippedBoundsInRoot()
        val nameBounds = compose.onNodeWithTag("exercise_library_name_arnold-press", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // The approved compact layout uses this space for the favorite control,
        // without restoring the old decorative stick figure or an extra gutter.
        val favoriteBounds = compose.onNodeWithTag("exercise_library_favorite_arnold-press").getUnclippedBoundsInRoot()
        assertEquals(rowBounds.left, favoriteBounds.left)
        assertEquals(favoriteBounds.right, nameBounds.left)
        compose.onNodeWithTag("exercise_library_name_arnold-press", useUnmergedTree = true).assertTextEquals("Arnold-nyomás")
        compose.onNodeWithTag("exercise_library_meta_arnold-press", useUnmergedTree = true)
            .assertTextContains("Váll • 2 kézisúlyzó • 3×10")

        compose.onNodeWithTag("exercise_library_add_to_queue_arnold-press").performClick()
        assertEquals("arnold-press", queuedId)
        compose.onNodeWithTag("exercise_library_start_now_arnold-press").performClick()
        assertEquals("arnold-press", startedId)
        compose.onNodeWithTag("exercise_library_name_arnold-press", useUnmergedTree = true).performClick()
        assertEquals("arnold-press", selectedId)
    }

    @Test
    fun exerciseDetailRemovesAnatomicalExecutionStickFigurePanelAndMapsCredibleMuscleBody() {
        val arnold = ExerciseItem(
            id = "arnold-press",
            nameHu = "Arnold-nyomás",
            nameEn = "Arnold Press",
            category = ExerciseCategory.SHOULDERS,
            equipmentHu = "2 kézisúlyzó",
            primaryMusclesHu = "Váll",
            secondaryMusclesHu = "Tricepsz",
            defaultSets = 3,
            defaultReps = 10
        )
        val state = TrainPilotUiState(
            language = AppLanguage.HU,
            exercises = listOf(arnold),
            selectedExerciseId = "arnold-press"
        )

        compose.setContent {
            TrainPilotTheme {
                Box(Modifier.width(354.dp)) {
                    ExerciseDetailScreen(
                        state = state,
                        onBack = {},
                        onOpenSettings = {},
                        onAddExerciseToWorkout = {},
                        onStartDemoWithExercise = {}
                    )
                }
            }
        }

        // Crude "Anatómiai kivitelezés" panel and duplicate video chip must not exist
        compose.onNodeWithText("Anatómiai kivitelezés", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("exercise_detail_popup_video_chip").assertDoesNotExist()

        // Metadata card and credible anatomical muscle heatmap card remain and show accurate target/secondary loads
        compose.onNodeWithTag("exercise_detail_metadata_card").assertIsDisplayed()
        compose.onNodeWithTag("anatomical_muscle_heatmap_card").assertExists()
        compose.onNodeWithTag("muscle_chip_shoulders", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("100%", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("50%", useUnmergedTree = true).assertExists()

        // Wait for the bitmap anatomy layers to load and verify both shoulders (primary) and arms (secondary) light up
        compose.waitUntil(30_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithTag("progress_anatomy_layer_shoulders", useUnmergedTree = true).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("progress_anatomy_layer_shoulders", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("progress_anatomy_layer_arms", useUnmergedTree = true).assertExists()
    }

    @Test
    fun homeAndProgressDefaultCollapsedContentFitsPhoneViewportAndRetainsScrollSafety() {
        val program = TrainingProgramModel(
            id = "home-basic",
            nameHu = "Otthoni A/B",
            nameEn = "Home A/B",
            subtitleHu = "Otthon • Kezdő",
            subtitleEn = "Home • Beginner",
            tagsHu = emptyList(),
            isCustom = false,
            isActive = true,
            days = listOf(ProgramDayModel("d1", "A", "A", "A", 60, emptyList()))
        )
        val loads = listOf(
            com.example.ui.components.MuscleLoadItem("chest", "Mellizom", "4 sorozat", 49, 0.49f, "Közepes", emptyList(), "", setsCount = 4),
            com.example.ui.components.MuscleLoadItem("back", "Hátizom", "6 sorozat", 76, 0.76f, "Nagyobb", emptyList(), "", setsCount = 6),
            com.example.ui.components.MuscleLoadItem("shoulders", "Vállizom", "4 sorozat", 56, 0.56f, "Közepes", emptyList(), "", setsCount = 4),
            com.example.ui.components.MuscleLoadItem("arms", "Karizmok", "5 sorozat", 63, 0.63f, "Közepes", emptyList(), "", setsCount = 5),
            com.example.ui.components.MuscleLoadItem("core", "Törzs", "3 sorozat", 32, 0.32f, "Kisebb", emptyList(), "", setsCount = 3),
            com.example.ui.components.MuscleLoadItem("legs", "Lábizmok", "8 sorozat", 100, 1.0f, "Nagyobb", emptyList(), "", setsCount = 8)
        )
        val state = TrainPilotUiState(
            profileComplete = true,
            programs = listOf(program),
            watchName = "Galaxy Watch7 (ZW4J)",
            watchConnected = true,
            watchAppAvailable = true,
            dailySteps = 4314,
            activeCaloriesKcal = 154,
            dailyHealthAvailableFields = setOf("steps", "activeCalories"),
            todayPulseReading = PulseDayReading(
                day = LocalDate.now(),
                provider = HealthJournalStore.HC,
                state = ReadingState.AVAILABLE,
                averageBpm = 63.0,
                averageDescriptionHu = "Napi átlag",
                explanationHu = "Napi átlag"
            ),
            realMuscleLoads = loads
        )

        var showProgress by mutableStateOf(false)
        var smallViewport by mutableStateOf(false)

        compose.setContent {
            TrainPilotTheme {
                val width = if (smallViewport) 320.dp else 354.dp
                val height = if (smallViewport) 420.dp else 620.dp
                Box(Modifier.size(width, height).clipToBounds().testTag("phone_viewport")) {
                    if (!showProgress) {
                        HomeScreen(
                            state = state,
                            strings = TrainPilotStrings.forLanguage(AppLanguage.HU),
                            onStartOrContinueWorkout = {},
                            onOpenProgramDayDetails = {},
                            onOpenQuickWorkout = {},
                            onOpenWatchAndSync = {},
                            onOpenHealthSleep = {},
                            onOpenHealthHeart = {},
                            onOpenHealthSteps = {},
                            onOpenCoachReadiness = {},
                            onOpenGoalsDialog = {},
                            onOpenPersonalPlanner = {}
                        )
                    } else {
                        ProgressTabScreen(
                            uiState = state,
                            strings = TrainPilotStrings.forLanguage(AppLanguage.HU),
                            onSelectMainSection = {},
                            onSelectExercise = {},
                            onSelectRange = {},
                            onSelectVolumeBar = {},
                            onSelectExpandedStatsCard = {},
                            onSelectStatsSubTab = {},
                            onToggleCardioFilterStepsMode = {},
                            onOpenWorkoutDetail = {},
                            onOpenSettings = {},
                            onOpenPalettePicker = {}
                        )
                    }
                }
            }
        }

        // 1. Default Home viewport check (354dp x 620dp content area)
        val homeViewport = compose.onNodeWithTag("phone_viewport").getUnclippedBoundsInRoot()
        val goalsBounds = compose.onNodeWithTag("home_goals_row_card").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(
            "Home goals card must fit completely inside default phone viewport without scrolling (bottom=${goalsBounds.bottom}, viewport=${homeViewport.bottom})",
            goalsBounds.bottom <= homeViewport.bottom
        )

        // 2. Default Progress viewport check (354dp x 620dp content area)
        compose.runOnIdle { showProgress = true }
        val progressViewport = compose.onNodeWithTag("phone_viewport").getUnclippedBoundsInRoot()
        val adviceBounds = compose.onNodeWithTag("progress_advice_card").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(
            "Progress advice card must fit completely inside default phone viewport without scrolling (bottom=${adviceBounds.bottom}, viewport=${progressViewport.bottom})",
            adviceBounds.bottom <= progressViewport.bottom
        )

        // 3. Scroll-safety check on small viewport (320dp x 420dp): scrolling remains functional
        compose.runOnIdle { smallViewport = true; showProgress = false }
        compose.onNodeWithTag("home_goals_row_card").performScrollTo().assertIsDisplayed()

        compose.runOnIdle { showProgress = true }
        compose.onNodeWithTag("progress_advice_card").performScrollTo().assertIsDisplayed()
    }
}
