package com.example

import com.example.model.ExerciseCategory
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.viewmodel.TrainPilotViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationOriginRegressionTest {
    @Test fun logDetailsRestoreTheOriginSectionAndNestedWorkoutPhotoAndMapFocus() {
        for (section in 0..2) {
            val model = TrainPilotViewModel()
            model.selectTab(MainTab.PROGRESS)
            model.selectProgressMainSection(section)
            model.selectStatsSubTab(1)
            model.selectExpandedStatsCard(2)
            model.updateLogSearch("preserved search")
            val origin = model.uiState.value
            model.openLogWorkoutDetail("workout-a")
            val openedHistory = model.uiState.value.backStack
            model.openLogWorkoutDetail("workout-a")
            assertEquals("Double opening the same detail must not add another back step", openedHistory, model.uiState.value.backStack)
            model.selectLogDetailTab(2)
            model.openLogWorkoutPhotos()
            model.navigateToSubScreen(SubScreen.WATCH_AND_SYNC)
            assertTrue(model.navigateBack())
            assertEquals(SubScreen.PRIVATE_PHOTOS, model.uiState.value.currentSubScreen)
            assertEquals("workout-a", model.uiState.value.privatePhotoWorkoutId)
            model.navigateBack()
            assertEquals(SubScreen.LOG_WORKOUT_DETAIL, model.uiState.value.currentSubScreen)
            assertNull(model.uiState.value.privatePhotoWorkoutId)
            assertEquals(2, model.uiState.value.logDetailTabIndex)
            model.navigateToSubScreen(SubScreen.LOG_ROUTE_MAP)
            model.openLogWorkoutDetail("workout-b")
            model.selectLogDetailTab(0)
            model.navigateBack()
            assertEquals(SubScreen.LOG_ROUTE_MAP, model.uiState.value.currentSubScreen)
            assertEquals("workout-a", model.uiState.value.selectedLogWorkoutId)
            model.navigateBack()
            assertEquals(SubScreen.LOG_WORKOUT_DETAIL, model.uiState.value.currentSubScreen)
            assertEquals(2, model.uiState.value.logDetailTabIndex)
            model.navigateBack()
            val returned = model.uiState.value
            assertEquals(MainTab.PROGRESS, returned.currentTab)
            assertEquals(SubScreen.NONE, returned.currentSubScreen)
            assertEquals(section, returned.progressMainSection)
            assertEquals(origin.statsSubTab, returned.statsSubTab)
            assertEquals(origin.expandedStatsCardIndex, returned.expandedStatsCardIndex)
            assertEquals(origin.logSearchQuery, returned.logSearchQuery)
            assertEquals(origin.backStack, returned.backStack)
            assertEquals(origin.activeSetsByExerciseId, returned.activeSetsByExerciseId)
        }
    }

    @Test fun exerciseDetailsAndSettingsPopOneRealOriginWithoutAddingHistory() {
        val model = TrainPilotViewModel()
        model.selectTab(MainTab.WORKOUT)
        model.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY)
        model.updateExerciseSearch("guggolás")
        model.selectExerciseCategory(ExerciseCategory.LEGS)
        val library = model.uiState.value
        val exercises = library.exercises.take(2)
        model.openExerciseDetail(exercises[0].id)
        val openedHistory = model.uiState.value.backStack
        model.openExerciseDetail(exercises[0].id)
        assertEquals(openedHistory, model.uiState.value.backStack)
        model.openExerciseDetail(exercises[1].id)
        model.navigateBack()
        assertEquals(exercises[0].id, model.uiState.value.selectedExerciseId)
        assertEquals(SubScreen.EXERCISE_DETAIL, model.uiState.value.currentSubScreen)
        model.navigateBack()
        assertEquals(SubScreen.EXERCISE_LIBRARY, model.uiState.value.currentSubScreen)
        assertEquals(library.exerciseSearchQuery, model.uiState.value.exerciseSearchQuery)
        assertEquals(library.selectedExerciseCategory, model.uiState.value.selectedExerciseCategory)
        assertEquals(library.backStack, model.uiState.value.backStack)
        model.navigateToSubScreen(SubScreen.SETTINGS_MAIN)
        val settingsHistory = model.uiState.value.backStack
        model.navigateToSubScreen(SubScreen.SETTINGS_MAIN)
        assertEquals(settingsHistory, model.uiState.value.backStack)
        model.navigateToSubScreen(SubScreen.BACKUP_RESTORE)
        model.navigateToSubScreen(SubScreen.PERMISSIONS_SCREEN)
        listOf(SubScreen.BACKUP_RESTORE, SubScreen.SETTINGS_MAIN, SubScreen.EXERCISE_LIBRARY).forEach { expected ->
            val size = model.uiState.value.backStack.size
            model.navigateBack()
            assertEquals(expected, model.uiState.value.currentSubScreen)
            assertEquals(size - 1, model.uiState.value.backStack.size)
            assertEquals(MainTab.WORKOUT, model.uiState.value.currentTab)
        }
    }
}
