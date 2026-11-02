package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.LocalizedStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.progress.NativeProgressPhotoGallery
import com.repforge.app.ui.progress.ProgressViewModel
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotExtendedColors

/** The existing native, metadata-indexed photo viewer behind the Studio screen. */
@Composable
internal fun StudioPrivatePhotos(state: TrainPilotUiState, strings: LocalizedStrings,
    back: () -> Unit, settings: () -> Unit,
    model: ProgressViewModel = viewModel(key = "studio-private-photos")) {
    val all by model.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) model.loadProgressData()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); model.closePhoto() }
    }
    val palette = LocalTrainPilotPalette.current
    val nativeColors = remember(palette) {
        TrainPilotExtendedColors(bg = palette.background, card = palette.surfaceCard, card2 = palette.surfaceElevated,
            line = palette.cardBorder, text = palette.textPrimary, muted = palette.textSecondary,
            accent = palette.primary, accent2 = palette.secondary, onAccent = palette.onPrimary,
            dangerText = palette.danger, success = palette.success, emphasisBorder = palette.sectionBorder)
    }
    val photos = all.progressPhotos.filter { state.privatePhotoWorkoutId == null || it.workoutId == state.privatePhotoWorkoutId }
    CompositionLocalProvider(LocalTrainPilotColors provides nativeColors) {
        LazyColumn(Modifier.fillMaxSize().background(palette.background).testTag("private_photos_screen"),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SubScreenTopBar(strings.privatePhotosTitle, back, settings) }
            item { Text("${photos.size} • ${strings.privatePhotosTitle}", color = palette.textSecondary,
                modifier = Modifier.testTag("private_photos_count")) }
            item { NativeProgressPhotoGallery(all.copy(progressPhotos = photos, language = state.language.name.lowercase()),
                model::openPhoto, model::closePhoto) }
        }
    }
}
