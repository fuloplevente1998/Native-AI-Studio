package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette

/**
 * Opening in the installed YouTube app/browser is the primary path: WebView
 * iframe failures (including provider blocking) must not appear as a black
 * player. This dialog never changes workout, pause, timer or navigation state.
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
    val context = LocalContext.current
    val details = remember(exerciseTitle, exerciseId, storedVideoUrl, isCustom, selectedNotesHu, allowCatalogByTitle) {
        resolveExerciseVideoAndNotes(
            exerciseTitle, exerciseId, storedVideoUrl, isCustom, selectedNotesHu, allowCatalogByTitle
        )
    }
    var openError by remember(details.link.externalUrl) { mutableStateOf<String?>(null) }
    var copied by remember(details.link.externalUrl) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Oktatóvideó • $exerciseTitle",
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TrainPilotRedCloseButton(onClick = onDismiss)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = when (details.link.state) {
                                ExerciseVideoState.YOUTUBE ->
                                    "A gyakorlat videója a YouTube alkalmazásban vagy a böngészőben nyílik meg."
                                ExerciseVideoState.EXTERNAL_ONLY ->
                                    "A mentett hivatkozás nem támogatott YouTube-videóformátum. Külső megnyitás érhető el."
                                ExerciseVideoState.MISSING ->
                                    "Ehhez a gyakorlathoz nincs oktatóvideó hozzárendelve."
                                ExerciseVideoState.INVALID ->
                                    "A gyakorlathoz mentett videóhivatkozás hibás vagy nem biztonságos."
                            },
                            color = palette.textPrimary
                        )
                        val url = details.link.externalUrl
                        if (url != null) {
                            Button(
                                onClick = {
                                    openError = null
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                        )
                                    }.onFailure {
                                        openError = "Nem sikerült megnyitni a videót. Másold a hivatkozást, és nyisd meg böngészőben."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary
                                )
                            ) {
                                Text("Videó megnyitása", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    if (clipboard != null) {
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Oktatóvideó", url))
                                        copied = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) { Text(if (copied) "Hivatkozás másolva" else "Hivatkozás másolása") }
                        }
                        if (openError != null) Text(openError!!, color = palette.danger)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            strings.executionTitle,
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(details.notesHu, color = palette.textPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                )
            ) { Text(strings.closeAction, fontWeight = FontWeight.Bold) }
        }
    )
}
