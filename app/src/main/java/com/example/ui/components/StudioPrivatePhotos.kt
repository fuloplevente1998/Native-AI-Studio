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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.repforge.app.ui.theme.TrainPilotCardSpacing
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.WorkoutPhotoRepository
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.NativeJournalPhotos
import com.repforge.app.StoredPhotoMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun trL(lang: com.example.model.AppLanguage, hu: String, en: String, de: String, ro: String): String = when (lang) {
    com.example.model.AppLanguage.HU -> hu
    com.example.model.AppLanguage.EN -> en
    com.example.model.AppLanguage.DE -> de
    com.example.model.AppLanguage.RO -> ro
}

/** The native, metadata-indexed photo viewer with photo capture support for logged workouts. */
@Composable
internal fun StudioPrivatePhotos(state: TrainPilotUiState, strings: LocalizedStrings,
    back: () -> Unit, settings: () -> Unit,
    model: ProgressViewModel = viewModel(key = "studio-private-photos")) {
    val all by model.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoRepo = remember(context) { WorkoutPhotoRepository(context) }
    val db = remember(context) { TrainPilotDatabase.getInstance(context) }

    var selectedCategory by remember { mutableStateOf("after") } // "before", "after", "other"

    var pendingPhoto by remember { mutableStateOf<NativeJournalPhotos.Request?>(null) }
    var photoError by remember { mutableStateOf(false) }

    fun receivePhoto(importPhoto: suspend () -> Result<StoredPhotoMetadata?>) {
        val request = pendingPhoto ?: return
        pendingPhoto = null
        scope.launch {
            try {
                val photo = importPhoto().getOrThrow() ?: return@launch
                withContext(Dispatchers.IO) { NativeJournalPhotos.attach(context, db, request, photo) }
                model.loadProgressData()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                photoError = true
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res -> receivePhoto { photoRepo.handleCameraResult(res.resultCode, res.data) } }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res -> receivePhoto {
        if (res.resultCode == android.app.Activity.RESULT_OK) photoRepo.importPickedUri(res.data?.data)
        else Result.success(null)
    } }

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
    val lang = state.language
    CompositionLocalProvider(LocalTrainPilotColors provides nativeColors) {
        LazyColumn(Modifier.fillMaxSize().background(palette.background).testTag("private_photos_screen"),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SubScreenTopBar(strings.privatePhotosTitle, back, settings) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${photos.size} • ${strings.privatePhotosTitle}", color = palette.textSecondary,
                        modifier = Modifier.testTag("private_photos_count"))
                }
            }

            // If viewing photos for a specific workout, provide photo attachment controls
            if (state.privatePhotoWorkoutId != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                            .testTag("journal_photo_attach_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = trL(lang, "Fotó hozzáadása az edzéshez", "Add photo to workout", "Foto zum Training hinzufügen", "Adaugă fotografie la antrenament"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )

                            if (photoError) {
                                Text(trL(lang, "A fotó hozzáadása nem sikerült. Próbáld újra.",
                                    "Could not add photo. Please try again.",
                                    "Foto konnte nicht hinzugefügt werden. Bitte erneut versuchen.",
                                    "Fotografia nu a putut fi adăugată. Încearcă din nou."),
                                    color = palette.danger, modifier = Modifier.testTag("journal_photo_error"))
                            }

                            // Category selection chips: Edzés előtt / Edzés után / Egyéb
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val categories: List<Pair<String, String>> = listOf(
                                    Pair("before", trL(lang, "Edzés előtt", "Before", "Vorher", "Înainte")),
                                    Pair("after", trL(lang, "Edzés után", "After", "Nachher", "După")),
                                    Pair("other", trL(lang, "Egyéb", "Other", "Sonstiges", "Altele"))
                                )
                                categories.forEach { (catKey, catLabel) ->
                                    val isSelected = selectedCategory == catKey
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) palette.primary else palette.surfaceElevated,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedCategory = catKey }
                                            .testTag("photo_cat_chip_$catKey")
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = catLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) palette.onPrimary else palette.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Camera and Gallery buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val workoutId = state.privatePhotoWorkoutId ?: return@Button
                                            pendingPhoto = NativeJournalPhotos.prepare(context, workoutId, selectedCategory)
                                            photoError = false
                                            cameraLauncher.launch(photoRepo.prepareCameraCaptureIntent())
                                        } catch (_: Exception) {
                                            pendingPhoto = null
                                            photoError = true
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.surfaceElevated,
                                        contentColor = palette.textPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("journal_camera_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(trL(lang, "Kamera", "Camera", "Kamera", "Cameră"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val workoutId = state.privatePhotoWorkoutId ?: return@Button
                                            pendingPhoto = NativeJournalPhotos.prepare(context, workoutId, selectedCategory)
                                            photoError = false
                                            galleryLauncher.launch(photoRepo.createPickImageIntent())
                                        } catch (_: Exception) {
                                            pendingPhoto = null
                                            photoError = true
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.surfaceElevated,
                                        contentColor = palette.textPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("journal_gallery_button")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(trL(lang, "Galéria", "Gallery", "Galerie", "Galerie"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            item { NativeProgressPhotoGallery(all.copy(progressPhotos = photos, language = state.language.name.lowercase()),
                model::openPhoto, model::closePhoto) }
        }
    }
}
