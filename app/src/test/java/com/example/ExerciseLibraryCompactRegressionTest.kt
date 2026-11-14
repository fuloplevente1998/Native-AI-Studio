package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
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
            nameRo = "Flexii pentru biceps cu bara din picioare"
        )
        val arnold = ExerciseItem(id = "arnold", nameHu = "Arnold-nyomás", nameDe = "Arnold-Drücken",
            equipmentHu = "2 kézisúlyzó", primaryMusclesHu = "Váll", category = com.example.model.ExerciseCategory.SHOULDERS)
        var language by mutableStateOf(AppLanguage.HU)
        var width by mutableIntStateOf(320)
        var scale by mutableFloatStateOf(1f)
        var favorite by mutableStateOf(false)
        var queued by mutableStateOf(false)
        var selected = 0
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
                    Box(Modifier.width(width.dp)) {
                        ExerciseLibraryScreen(
                            state = TrainPilotUiState(language = language, exercises = listOf(exercise, arnold),
                                favoriteExerciseIds = if (favorite) setOf(exercise.id) else emptySet(),
                                quickWorkoutQueue = if (queued) listOf(exercise) else emptyList()),
                            onBack = {}, onOpenSettings = {}, onSearchQueryChange = {},
                            onSelectCategory = {}, onSelectExercise = { selected++ },
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
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue("Title should use the card width", bounds.width >= (w - 60).dp)
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
                assertTrue("$tag width", hit.width >= 48.dp)
                assertTrue("$tag height", hit.height >= 48.dp)
                if (!tag.startsWith("exercise_library_favorite_")) {
                    val visual = compose.onNodeWithTag("${tag}_visual", useUnmergedTree = true).getUnclippedBoundsInRoot()
                    assertEquals(32.dp, visual.width)
                    assertEquals(32.dp, visual.height)
                }
            }
        }
        // Capture the actual Compose fixture via the Android Canvas, without PixelCopy.
        for (lang in listOf(AppLanguage.HU, AppLanguage.DE)) {
            compose.runOnIdle { width = 360; scale = 1f; language = lang }
            compose.onNodeWithTag("exercise_library_row_long").performScrollTo()
            val shortCard = compose.onNodeWithTag("exercise_library_row_arnold").performScrollTo()
                .getUnclippedBoundsInRoot()
            assertTrue("Short title card must stay compact: ${shortCard.height}", shortCard.height <= 100.dp)
            compose.onNodeWithTag("exercise_library_row_long").performScrollTo()
            compose.runOnIdle {
                val view = compose.activity.findViewById<View>(android.R.id.content)
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, minOf(360, bitmap.width), bitmap.height)
                val appDir = if (File("app/build.gradle.kts").exists()) File("app") else File(".")
                val output = File(appDir, "build/reports/tests/device-ui/exercise-picker-${lang.code}.png")
                output.parentFile!!.mkdirs()
                output.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
                cropped.recycle()
                bitmap.recycle()
            }
        }
        compose.onNodeWithTag("exercise_library_favorite_long").performClick().assertIsOn()
        compose.onNodeWithTag("exercise_video_btn_${exercise.localizedName(language).hashCode()}").performClick()
        assertEquals(exercise.localizedName(language) to exercise.id, video)
        compose.onNodeWithTag("exercise_library_add_to_queue_long").performClick()
        assertTrue(queued)
        compose.onNodeWithTag("exercise_library_start_now_long").performScrollTo().performClick()
        assertEquals(1, started)
        compose.onNodeWithTag("exercise_library_name_long", useUnmergedTree = true).performScrollTo().performClick()
        assertEquals(1, selected)
    }
}
