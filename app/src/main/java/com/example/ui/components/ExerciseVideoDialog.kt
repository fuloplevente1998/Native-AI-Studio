package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val details = remember(
        exerciseTitle, exerciseId, storedVideoUrl, isCustom, selectedNotesHu, allowCatalogByTitle
    ) {
        resolveExerciseVideoAndNotes(
            exerciseTitle, exerciseId, storedVideoUrl, isCustom, selectedNotesHu, allowCatalogByTitle
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

    LaunchedEffect(url, mode, retryKey, load) {
        if (url != null && (load == ExerciseVideoLoad.Loading || load == ExerciseVideoLoad.Starting)) {
            val starting = load == ExerciseVideoLoad.Starting
            delay(18_000)
            load = ExerciseVideoLoad.Failed(
                if (starting) "A videó nem indult el. Próbáld a belső böngészőt."
                else "A videó betöltése túl sokáig tart. Ellenőrizd az internetet, " +
                    "vagy próbáld a belső böngészőt."
            )
        }
    }

    val backFromBrowser: () -> Unit = {
        when {
            mode == ExerciseVideoMode.BROWSER && webView?.canGoBack() == true -> webView?.goBack()
            mode == ExerciseVideoMode.BROWSER && youtubeId != null -> mode = ExerciseVideoMode.EMBED
            else -> onDismiss()
        }
    }

    Dialog(
        onDismissRequest = backFromBrowser,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f)
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val browserHeight = (maxHeight * 0.65f).coerceAtLeast(240.dp)
                Column(modifier = Modifier.padding(12.dp)) {
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

                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (details.link.state) {
                                    ExerciseVideoState.YOUTUBE, ExerciseVideoState.EXTERNAL_ONLY -> {
                                        Text(
                                            if (mode == ExerciseVideoMode.EMBED)
                                                "YouTube-lejátszó az alkalmazáson belül"
                                            else "Belső böngésző • " +
                                                (Uri.parse(location).host ?: "HTTPS-videóoldal"),
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (url != null) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().then(
                                                    if (mode == ExerciseVideoMode.BROWSER) Modifier.height(browserHeight)
                                                    else Modifier.heightIn(min = 200.dp).aspectRatio(16f / 9f)
                                                ).background(Color.Black),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                SecureExerciseVideoWebView(
                                                    mode = mode,
                                                    url = url,
                                                    youtubeId = youtubeId,
                                                    retryKey = retryKey,
                                                    modifier = Modifier.fillMaxSize(),
                                                    onLoad = { next ->
                                                        // API callbacks arriving after the startup timeout
                                                        // cannot turn the failed embed back into "ready".
                                                        if (mode != ExerciseVideoMode.EMBED ||
                                                            load !is ExerciseVideoLoad.Failed) load = next
                                                    },
                                                    onLocationChanged = { location = it },
                                                    onHistoryChanged = { canBrowserGoBack = it },
                                                    onViewChanged = { webView = it }
                                                )
                                                when (val state = load) {
                                                    is ExerciseVideoLoad.Failed ->
                                                        Text(
                                                            text = state.message,
                                                            color = Color.White,
                                                            modifier = Modifier.background(
                                                                Color.Black.copy(alpha = 0.86f)
                                                            ).padding(16.dp)
                                                        )
                                                    ExerciseVideoLoad.Loading ->
                                                        Text("Videó betöltése…", color = Color.White)
                                                    else -> Unit
                                                }
                                            }
                                            when (load) {
                                                ExerciseVideoLoad.Starting ->
                                                    Text("Videó indítása…", color = palette.textSecondary)
                                                ExerciseVideoLoad.Playing ->
                                                    Text("Lejátszás folyamatban.", color = palette.textSecondary)
                                                ExerciseVideoLoad.Ready ->
                                                    Text(
                                                        if (mode == ExerciseVideoMode.EMBED)
                                                            "Az indításhoz koppints a videón a Lejátszás gombra."
                                                        else "A böngészőoldal betöltött; a videót az oldalon indíthatod.",
                                                        color = palette.textSecondary
                                                    )
                                                else -> Unit
                                            }

                                            if (mode == ExerciseVideoMode.EMBED) {
                                                Button(
                                                    onClick = { mode = ExerciseVideoMode.BROWSER },
                                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = palette.primary,
                                                        contentColor = palette.onPrimary
                                                    )
                                                ) {
                                                    Text("Megnyitás a belső böngészőben")
                                                }
                                            } else {
                                                if (canBrowserGoBack) {
                                                    TrainPilotOutlinedButton(
                                                        onClick = { webView?.goBack() },
                                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                                    ) { Text("Vissza a böngészőben") }
                                                }
                                                if (youtubeId != null) {
                                                    TrainPilotOutlinedButton(
                                                        onClick = { mode = ExerciseVideoMode.EMBED },
                                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                                    ) { Text("Beágyazott lejátszó újrapróbálása") }
                                                } else {
                                                    Text(
                                                        "Ez a hivatkozás nem támogatott YouTube-beágyazás; " +
                                                            "a saját HTTPS-oldala a belső böngészőben nyílt meg.",
                                                        color = palette.textSecondary
                                                    )
                                                }
                                            }
                                            TrainPilotOutlinedButton(
                                                onClick = { retryKey++ },
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            ) { Text("Videó újratöltése") }
                                            TrainPilotOutlinedButton(
                                                onClick = {
                                                    copyError = runCatching {
                                                        val clipboard = context.getSystemService(
                                                            Context.CLIPBOARD_SERVICE
                                                        ) as ClipboardManager
                                                        clipboard.setPrimaryClip(
                                                            ClipData.newPlainText("Oktatóvideó", url)
                                                        )
                                                        copied = true
                                                    }.isFailure
                                                },
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            ) { Text(if (copied) "Hivatkozás másolva" else "Hivatkozás másolása") }
                                            if (copyError) Text(
                                                "Nem sikerült a hivatkozás másolása.", color = palette.danger
                                            )
                                        }
                                    }
                                    ExerciseVideoState.MISSING -> Text(
                                        "Ehhez a gyakorlathoz nincs oktatóvideó hozzárendelve.",
                                        color = palette.textPrimary
                                    )
                                    ExerciseVideoState.INVALID -> Text(
                                        "A gyakorlathoz mentett videóhivatkozás hibás vagy nem biztonságos.",
                                        color = palette.danger
                                    )
                                }
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

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary, contentColor = palette.onPrimary
                        )
                    ) { Text(strings.closeAction, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
