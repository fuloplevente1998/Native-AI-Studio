package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import com.repforge.app.ui.theme.trainPilotSoftGlow

/**
 * Fallback exercise video dialog shown only when an exercise has no video,
 * an invalid URL, or when launching the external browser / YouTube app fails.
 */
@Composable
fun ExerciseYouTubeVideoModalDialog(
    exerciseTitle: String,
    onDismiss: () -> Unit,
    exerciseId: String? = null,
    storedVideoUrl: String? = null,
    isCustom: Boolean = false,
    selectedNotesHu: String? = null,
    allowCatalogByTitle: Boolean = true
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val langCode = strings.language.code.lowercase()
    val context = androidx.compose.ui.platform.LocalContext.current
    val details = remember(
        exerciseTitle, exerciseId, storedVideoUrl, isCustom, selectedNotesHu, allowCatalogByTitle, langCode
    ) {
        resolveExerciseVideoAndNotes(
            exerciseTitle, exerciseId, storedVideoUrl, isCustom, selectedNotesHu, allowCatalogByTitle, langCode
        )
    }
    val url = details.link.externalUrl
    var copied by remember(url) { mutableStateOf(false) }
    var copyError by remember(url) { mutableStateOf(false) }
    var launchFailed by remember(url) { mutableStateOf(false) }

    val tutorialLabel = when (langCode) {
        "en" -> "Tutorial video"
        "de" -> "Übungsvideo"
        "ro" -> "Videoclip demonstrativ"
        else -> "Oktatóvideó"
    }
    val openExternalLabel = when (langCode) {
        "en" -> "Open in external browser / YouTube"
        "de" -> "Im externen Browser / YouTube öffnen"
        "ro" -> "Deschide în browser extern / YouTube"
        else -> "Megnyitás külső böngészőben / YouTube-on"
    }
    val copyLinkLabel = when (langCode) {
        "en" -> if (copied) "Copied" else "Copy link"
        "de" -> if (copied) "Kopiert" else "Link kopieren"
        "ro" -> if (copied) "Copiat" else "Copiază linkul"
        else -> if (copied) "Másolva" else "Link másolása"
    }
    val missingVideoLabel = when (langCode) {
        "en" -> "No tutorial video is assigned to this exercise."
        "de" -> "Dieser Übung ist kein Übungsvideo zugeordnet."
        "ro" -> "Acestui exercițiu nu îi este asociat niciun videoclip."
        else -> "Ehhez a gyakorlathoz nincs oktatóvideó hozzárendelve."
    }
    val invalidVideoLabel = when (langCode) {
        "en" -> "The saved video link is invalid or unsafe."
        "de" -> "Der gespeicherte Videolink ist ungültig oder unsicher."
        "ro" -> "Linkul video salvat este invalid sau nesigur."
        else -> "A mentett videóhivatkozás hibás vagy nem biztonságos."
    }
    val launchErrorLabel = when (langCode) {
        "en" -> "Could not open external browser or YouTube app."
        "de" -> "Externer Browser oder YouTube-App konnte nicht geöffnet werden."
        "ro" -> "Nu s-a putut deschide browserul extern sau aplicația YouTube."
        else -> "Nem sikerült megnyitni a külső böngészőt vagy a YouTube alkalmazást."
    }
    val copyFailedLabel = when (langCode) {
        "en" -> "Failed to copy video link."
        "de" -> "Videolink konnte nicht kopiert werden."
        "ro" -> "Nu s-a putut copia linkul video."
        else -> "Nem sikerült a hivatkozás másolása."
    }

    val copyLink: () -> Unit = {
        copyError = runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(tutorialLabel, url.orEmpty()))
            copied = true
        }.isFailure
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .trainPilotSoftGlow(palette.primary, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$tutorialLabel • $exerciseTitle",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TrainPilotRedCloseButton(onClick = onDismiss)
                }

                when (details.link.state) {
                    ExerciseVideoState.YOUTUBE, ExerciseVideoState.EXTERNAL_ONLY -> if (url != null) {
                        Button(
                            onClick = {
                                val opened = openExternalExerciseVideoUrl(context, url)
                                if (opened) {
                                    onDismiss()
                                } else {
                                    launchFailed = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            )
                        ) {
                            Text(openExternalLabel, fontWeight = FontWeight.Bold)
                        }
                        TrainPilotOutlinedButton(
                            onClick = copyLink,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text(copyLinkLabel)
                        }
                    }
                    ExerciseVideoState.MISSING -> Text(missingVideoLabel, color = palette.textPrimary)
                    ExerciseVideoState.INVALID -> Text(invalidVideoLabel, color = palette.danger)
                }

                if (launchFailed) Text(launchErrorLabel, color = palette.danger)
                if (copyError) Text(copyFailedLabel, color = palette.danger)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.executionTitle, color = palette.primary, fontWeight = FontWeight.Bold)
                        Text(details.notesHu, color = palette.textPrimary)
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    )
                ) {
                    Text(strings.closeAction, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
