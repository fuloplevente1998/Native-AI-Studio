package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import com.example.ui.theme.LocalTrainPilotPalette
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.localization.LocalTrainPilotStrings
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.ui.components.LocalOpenExerciseVideo
import com.example.ui.screens.ExerciseLibraryScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ExerciseLibraryCompactRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun longLocalizedTitlesAndActionsRemainUsableAtNarrowWidthsAndLargeText() {
        val exercise = ExerciseItem(
            id = "long", nameHu = "Bicepszhajlítás – kétkezes súlyzóval állva",
            nameEn = "Standing barbell biceps curl with both hands",
            nameDe = "Bizepscurls mit der Langhantel im Stehen",
            nameRo = "Flexii pentru biceps cu bara din picioare",
            category = com.example.model.ExerciseCategory.ARMS,
            equipmentHu = "Kétkezes rúd (súlyzórúd)", primaryMusclesHu = "Bicepsz"
        )
        val arnold = ExerciseItem(id = "arnold", nameHu = "Arnold-nyomás", nameDe = "Arnold-Drücken",
            equipmentHu = "2 kézisúlyzó", primaryMusclesHu = "Váll", category = com.example.model.ExerciseCategory.SHOULDERS)
        val bird = ExerciseItem(id = "bird", nameHu = "Bird dog – négykézláb ellentétes kar és láb", nameDe = "Bird Dog im Vierfüßlerstand",
            category = com.example.model.ExerciseCategory.CORE)
        val squat = ExerciseItem(id = "squat", nameHu = "Guggolás", nameDe = "Kniebeuge",
            category = com.example.model.ExerciseCategory.LEGS)
        var language by mutableStateOf(AppLanguage.HU)
        var width by mutableIntStateOf(320)
        var scale by mutableFloatStateOf(1f)
        var favorite by mutableStateOf(false)
        var queued by mutableStateOf(false)
        var selected: String? = null
        var mutationBusy by mutableStateOf(false)
        var started = 0
        var video: Pair<String, String?>? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, scale),
                LocalTrainPilotStrings provides TrainPilotStrings.forLanguage(language),
                LocalOpenExerciseVideo provides { name, id -> video = name to id }
            ) {
                TrainPilotTheme {
                    Box(Modifier.width(width.dp).background(LocalTrainPilotPalette.current.background)) {
                        ExerciseLibraryScreen(
                            state = TrainPilotUiState(language = language, exercises = listOf(exercise, arnold, bird, squat),
                                favoriteExerciseIds = if (favorite) setOf(exercise.id) else emptySet(),
                                libraryMutationBusy = mutationBusy,
                                quickWorkoutQueue = if (queued) listOf(exercise) else emptyList()),
                            onBack = {}, onOpenSettings = {}, onSearchQueryChange = {},
                            onSelectCategory = {}, onSelectExercise = { selected = it },
                            onToggleFavorite = { favorite = !favorite },
                            onAddExerciseToQueue = { queued = true }, onStartExerciseNow = { started++ }
                        )
                    }
                }
            }
        }
        for (w in listOf(320, 360)) for (font in listOf(1f, 1.5f)) for (lang in AppLanguage.entries) {
            compose.runOnIdle { width = w; scale = font; language = lang }
            val name = exercise.localizedName(lang)
            val node = compose.onNodeWithTag("exercise_library_name_long", useUnmergedTree = true)
            node.performScrollTo().assertTextEquals(name)
            val layouts = mutableListOf<TextLayoutResult>()
            val action = node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult]
            assertTrue(action.action!!.invoke(layouts))
            val layout = layouts.single()
            assertFalse("Title clipped: $name", layout.hasVisualOverflow)
            for (line in 0 until layout.lineCount - 1) {
                val end = layout.getLineEnd(line)
                if (end in 1 until name.length) {
                    assertFalse("Word split in $name at $end", name[end - 1].isLetter() && name[end].isLetter())
                }
            }
            for (tag in listOf("exercise_library_favorite_long", "exercise_video_btn_${name.hashCode()}",
                "exercise_library_add_to_queue_long", "exercise_library_start_now_long")) {
                val control = compose.onNodeWithTag(tag).performScrollTo().assertIsEnabled()
                val hit = control.getUnclippedBoundsInRoot()
                assertTrue("$tag width", (hit.right - hit.left) >= 48.dp)
                assertTrue("$tag height", (hit.bottom - hit.top) >= 48.dp)

            }
            val videoBounds = compose.onNodeWithTag("exercise_video_btn_${name.hashCode()}").getUnclippedBoundsInRoot()
            val addBounds = compose.onNodeWithTag("exercise_library_add_to_queue_long").getUnclippedBoundsInRoot()
            val startBounds = compose.onNodeWithTag("exercise_library_start_now_long").getUnclippedBoundsInRoot()
            assertEquals(videoBounds.right, addBounds.left)
            assertEquals(addBounds.right, startBounds.left)
            assertEquals(videoBounds.top, addBounds.top)
            assertEquals(addBounds.top, startBounds.top)
            val group = compose.onNodeWithTag("exercise_library_actions_long", useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue(group.left <= videoBounds.left && group.right >= startBounds.right)
            val titleBounds = node.getUnclippedBoundsInRoot()
            assertTrue("Actions must not overlap the title", group.left >= titleBounds.right || group.top >= titleBounds.bottom)
            val meta = compose.onNodeWithTag("exercise_library_meta_long", useUnmergedTree = true)
            meta.assertTextContains("${exercise.defaultSets}×${exercise.defaultReps}", substring = true)
        }
        // Capture the actual Compose fixture via the Android Canvas, without PixelCopy.
        for (lang in listOf(AppLanguage.HU, AppLanguage.DE)) {
            compose.runOnIdle { width = 360; scale = 1f; language = lang }
            val favoriteFilter = compose.onNodeWithTag("studio_library_favorites_only").performScrollTo().getUnclippedBoundsInRoot()
            val custom = compose.onNodeWithTag("exercise_library_add_custom_button").performScrollTo().getUnclippedBoundsInRoot()
            assertTrue("Header controls should fit on one row", favoriteFilter.bottom > custom.top && custom.bottom > favoriteFilter.top)
            compose.onNodeWithTag("exercise_library_row_long").performScrollTo()
            val shortCard = compose.onNodeWithTag("exercise_library_row_arnold").performScrollTo()
                .getUnclippedBoundsInRoot()
            assertTrue("Short title card must stay compact: ${(shortCard.bottom - shortCard.top)}", (shortCard.bottom - shortCard.top) <= 70.dp)
            val title = compose.onNodeWithTag("exercise_library_name_arnold", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val actions = compose.onNodeWithTag("exercise_library_actions_arnold", useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue("Short title and actions should be inline", title.right <= actions.left && title.bottom > actions.top)
            compose.onNodeWithTag("studio_library_favorites_only").performScrollTo()
            compose.runOnIdle {
                val view = compose.activity.findViewById<View>(android.R.id.content)
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, minOf(360, bitmap.width), minOf(800, bitmap.height))
                val appDir = if (File("app/build.gradle.kts").exists()) File("app") else File(".")
                val output = File(appDir, "build/reports/tests/device-ui/exercise-picker-${lang.code}.png")
                output.parentFile!!.mkdirs()
                output.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
                cropped.recycle()
                bitmap.recycle()
            }
        }
        compose.runOnIdle { mutationBusy = true }
        compose.onNodeWithTag("exercise_library_favorite_long").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { mutationBusy = false }
        compose.onNodeWithTag("exercise_library_favorite_long").performClick().assertIsOn()
        compose.onNodeWithTag("exercise_video_btn_${exercise.localizedName(language).hashCode()}").performScrollTo().performClick()
        assertEquals(exercise.localizedName(language) to exercise.id, video)
        compose.onNodeWithTag("exercise_library_add_to_queue_long").performScrollTo().performClick()
        assertTrue(queued)
        compose.onNodeWithTag("exercise_library_start_now_long").performScrollTo().performClick()
        assertEquals(1, started)
        assertNull("Action segments must not trigger the details click", selected)
        compose.onNodeWithTag("exercise_library_name_long", useUnmergedTree = true).performScrollTo().performClick()
        assertEquals(exercise.id, selected)
    }
}
