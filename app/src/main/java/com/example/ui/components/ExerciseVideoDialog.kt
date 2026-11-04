package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.components.inappvideo.ExerciseVideoLoad
import com.example.ui.components.inappvideo.ExerciseVideoMode
import com.example.ui.components.inappvideo.SecureExerciseVideoWebView
import com.repforge.app.ui.theme.trainPilotSoftGlow
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import kotlinx.coroutines.delay

/**
 * In-app exercise video; no ACTION_VIEW or workout/session state mutations.
 * Existing callers and the exact exercise URL selection remain unchanged.
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
    val youtubeId = details.link.youtubeId
    var mode by remember(url) {
        mutableStateOf(if (youtubeId != null) ExerciseVideoMode.EMBED else ExerciseVideoMode.BROWSER)
    }
    var retryKey by remember(url) { mutableStateOf(0) }
    var load by remember(url, mode, retryKey) { mutableStateOf<ExerciseVideoLoad>(ExerciseVideoLoad.Loading) }
    var location by remember(url, mode, retryKey) { mutableStateOf(url.orEmpty()) }
    var webView by remember(url, mode, retryKey) { mutableStateOf<WebView?>(null) }
    var canBrowserGoBack by remember(url, mode, retryKey) { mutableStateOf(false) }
    var copied by remember(url) { mutableStateOf(false) }
    var copyError by remember(url) { mutableStateOf(false) }
    var autoFallbackNotice by remember(url) { mutableStateOf<String?>(null) }

    val tutorialLabel = when (langCode) {
        "en" -> "Tutorial video"
        "de" -> "Übungsvideo"
        "ro" -> "Videoclip demonstrativ"
        else -> "Oktatóvideó"
    }
    val embedModeLabel = when (langCode) {
        "en" -> "Embedded"
        "de" -> "Eingebettet"
        "ro" -> "Încorporat"
        else -> "Beágyazott"
    }
    val browserModeLabel = when (langCode) {
        "en" -> "In-app browser"
        "de" -> "In-App-Browser"
        "ro" -> "Browser intern"
        else -> "Alkalmazáson belüli böngésző"
    }
    val openInBrowserLabel = when (langCode) {
        "en" -> "Open in in-app browser"
        "de" -> "Im In-App-Browser öffnen"
        "ro" -> "Deschide în browserul intern"
        else -> "Megnyitás a belső böngészőben"
    }
    val reloadLabel = when (langCode) {
        "en" -> "Reload"
        "de" -> "Neu laden"
        "ro" -> "Reîncarcă"
        else -> "Újratöltés"
    }
    val copyLinkLabel = when (langCode) {
        "en" -> if (copied) "Copied" else "Copy link"
        "de" -> if (copied) "Kopiert" else "Link kopieren"
        "ro" -> if (copied) "Copiat" else "Copiază linkul"
        else -> if (copied) "Másolva" else "Link másolása"
    }
    val loadingLabel = when (langCode) {
        "en" -> "Loading video…"
        "de" -> "Video wird geladen…"
        "ro" -> "Se încarcă videoclipul…"
        else -> "Videó betöltése…"
    }
    val playingLabel = when (langCode) {
        "en" -> "Playback in progress."
        "de" -> "Wiedergabe läuft."
        "ro" -> "Redare în curs."
        else -> "Lejátszás folyamatban."
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
    val copyFailedLabel = when (langCode) {
        "en" -> "Failed to copy video link."
        "de" -> "Videolink konnte nicht kopiert werden."
        "ro" -> "Nu s-a putut copia linkul video."
        else -> "Nem sikerült a hivatkozás másolása."
    }

    LaunchedEffect(url, mode, retryKey, load) {
        if (load is ExerciseVideoLoad.Failed && mode == ExerciseVideoMode.EMBED) {
            // A late media start after the deadline must not play behind the error panel.
            webView?.evaluateJavascript("tpPause();", null)
            if (com.example.ui.components.inappvideo.shouldAutoFallbackToBrowser(mode, load, url)) {
                val reason = (load as? ExerciseVideoLoad.Failed)?.message.orEmpty()
                autoFallbackNotice = when (langCode) {
                    "en" -> "$reason Switched automatically to in-app browser."
                    "de" -> "$reason Automatisch zum In-App-Browser gewechselt."
                    "ro" -> "$reason S-a comutat automat la browserul intern."
                    else -> "$reason Automatikusan átváltva a belső böngészőre."
                }
                mode = ExerciseVideoMode.BROWSER
                return@LaunchedEffect
            }
        }
        if (url != null && (load == ExerciseVideoLoad.Loading || load == ExerciseVideoLoad.Starting)) {
            val starting = load == ExerciseVideoLoad.Starting
            val timeoutMs = if (mode == ExerciseVideoMode.EMBED) 6_000L else 18_000L
            delay(timeoutMs)
            load = ExerciseVideoLoad.Failed(
                when (langCode) {
                    "en" -> if (starting) "Video did not start in embedded mode." else "Embedded player timed out."
                    "de" -> if (starting) "Video ist im eingebetteten Modus nicht gestartet." else "Zeitüberschreitung beim eingebetteten Player."
                    "ro" -> if (starting) "Videoclipul nu a pornit în modul încorporat." else "Timpul de încărcare a playerului încorporat a expirat."
                    else -> if (starting) "A videó nem indult el. Próbáld a belső böngészőt."
                    else "A videó betöltése túl sokáig tart. Ellenőrizd az internetet, vagy próbáld a belső böngészőt."
                }
            )
        }
    }

    val backFromBrowser: () -> Unit = {
        when {
            mode == ExerciseVideoMode.BROWSER && webView?.canGoBack() == true -> webView?.goBack()
            else -> onDismiss()
        }
    }

    Dialog(
        onDismissRequest = backFromBrowser,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(color = palette.surface, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f)
                .trainPilotSoftGlow(palette.primary, RoundedCornerShape(16.dp))) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("$tutorialLabel • $exerciseTitle", color = palette.textPrimary, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    TrainPilotRedCloseButton(onClick = onDismiss)
                }
                if (youtubeId != null && url != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val embedSelected = mode == ExerciseVideoMode.EMBED
                        Button(
                            onClick = {
                                autoFallbackNotice = null
                                mode = ExerciseVideoMode.EMBED
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (embedSelected) palette.primary else palette.surfaceElevated,
                                contentColor = if (embedSelected) palette.onPrimary else palette.textPrimary
                            )
                        ) {
                            Text(embedModeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = { mode = ExerciseVideoMode.BROWSER },
                            modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!embedSelected) palette.primary else palette.surfaceElevated,
                                contentColor = if (!embedSelected) palette.onPrimary else palette.textPrimary
                            )
                        ) {
                            Text(browserModeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (autoFallbackNotice != null && mode == ExerciseVideoMode.BROWSER) {
                    Text(
                        text = autoFallbackNotice!!,
                        color = palette.primary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                // One player instance. Browser mode owns all remaining height instead of
                // being a wide desktop page inside a 16:9 exercise illustration card.
                val videoContent: @Composable (Modifier) -> Unit = { area ->
                    if (url != null) Box(area.background(Color.Black), contentAlignment = Alignment.Center) {
                        SecureExerciseVideoWebView(
                            mode = mode,
                            url = url,
                            youtubeId = youtubeId,
                            retryKey = retryKey,
                            modifier = Modifier.fillMaxSize(),
                            language = langCode,
                            onLoad = { next -> if (mode != ExerciseVideoMode.EMBED || load !is ExerciseVideoLoad.Failed) load = next },
                            onLocationChanged = { location = it },
                            onHistoryChanged = { canBrowserGoBack = it },
                            onViewChanged = { webView = it },
                            onSwitchToBrowser = { nextUrl ->
                                location = nextUrl
                                mode = ExerciseVideoMode.BROWSER
                            }
                        )
                        when (val value = load) {
                            is ExerciseVideoLoad.Failed -> Text(value.message, color = Color.White,
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.9f)).padding(12.dp))
                            ExerciseVideoLoad.Loading -> Text(loadingLabel, color = Color.White)
                            else -> Unit
                        }
                    }
                }
                val copyLink: () -> Unit = {
                    copyError = runCatching {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(tutorialLabel, url.orEmpty()))
                        copied = true
                    }.isFailure
                }
                if (mode == ExerciseVideoMode.BROWSER && url != null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (canBrowserGoBack) TrainPilotOutlinedButton(onClick = { webView?.goBack() },
                            modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(when (langCode) { "en" -> "Back"; "de" -> "Zurück"; "ro" -> "Înapoi"; else -> "Vissza" })
                        }
                        Text(Uri.parse(location).host ?: tutorialLabel, color = palette.textSecondary,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 6.dp))
                    }
                    videoContent(Modifier.fillMaxWidth().weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (youtubeId != null) TrainPilotOutlinedButton(onClick = {
                            autoFallbackNotice = null
                            mode = ExerciseVideoMode.EMBED
                        }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(embedModeLabel) }
                        TrainPilotOutlinedButton(onClick = { retryKey++ },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(reloadLabel) }
                        TrainPilotOutlinedButton(onClick = copyLink,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(copyLinkLabel) }
                    }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (details.link.state) {
                            ExerciseVideoState.YOUTUBE, ExerciseVideoState.EXTERNAL_ONLY -> if (url != null) {
                                videoContent(Modifier.fillMaxWidth().heightIn(min = 200.dp).aspectRatio(16f / 9f))
                                if (load == ExerciseVideoLoad.Playing) Text(playingLabel, color = palette.textSecondary)
                                if (mode == ExerciseVideoMode.EMBED) Button(onClick = { mode = ExerciseVideoMode.BROWSER },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)) {
                                    Text(openInBrowserLabel)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TrainPilotOutlinedButton(onClick = { retryKey++ }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(reloadLabel) }
                                    TrainPilotOutlinedButton(onClick = copyLink, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                        Text(copyLinkLabel)
                                    }
                                }
                            }
                            ExerciseVideoState.MISSING -> Text(missingVideoLabel, color = palette.textPrimary)
                            ExerciseVideoState.INVALID -> Text(invalidVideoLabel, color = palette.danger)
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = palette.surfaceElevated,
                            modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(strings.executionTitle, color = palette.primary, fontWeight = FontWeight.Bold)
                                Text(details.notesHu, color = palette.textPrimary)
                            }
                        }
                    }
                }
                if (copyError) Text(copyFailedLabel, color = palette.danger)
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)) {
                    Text(strings.closeAction, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
