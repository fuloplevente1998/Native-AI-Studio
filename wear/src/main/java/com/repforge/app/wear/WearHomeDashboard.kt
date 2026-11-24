package com.repforge.app.wear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.*
import androidx.wear.compose.material3.Text
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import org.json.JSONObject

private val HomeWhite: Color
    @Composable get() = LocalWearHomeTheme.current.textColor
private val HomeMuted: Color
    @Composable get() = LocalWearHomeTheme.current.mutedColor
private val HomeLine: Color
    @Composable get() = LocalWearHomeTheme.current.outlineColor

@Composable
internal fun WearHomeDashboard(
    home: WatchHomeSnapshot?,
    active: WearWorkout?,
    closure: WearClosure?,
    health: JSONObject?,
    enabled: Boolean,
    phoneConnected: Boolean?,
    battery: Int?,
    onStart: (WatchHomeDay, String) -> Unit,
    onResume: () -> Unit,
    onDays: () -> Unit,
    onResult: () -> Unit,
    onJournal: () -> Unit,
    onDiscard: () -> Unit,
    onSettings: () -> Unit,
    onMenu: () -> Unit,
    onOpenPhone: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active?.workoutId) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L - now % 1000L)
        }
    }
    val saveStatus=LocalSaveSync.current
    val model = wearHomeDashboardState(home, active, closure, health, enabled, now)
    val theme = home?.theme ?: LocalWearHomeTheme.current
    val HomeWhite = theme.textColor
    val HomeMuted = theme.mutedColor
    val HomeLine = theme.outlineColor
    val onPrimary = {
        when (model.action) {
            HomeAction.START -> home?.recommended?.let { onStart(it.day, it.scheduleId) }
            HomeAction.RESUME -> onResume()
            HomeAction.DAYS -> onDays()
            HomeAction.RESULT -> onResult()
            HomeAction.PHONE -> onOpenPhone()
        }
        Unit
    }
    val primaryDescription =
        when (model.action) {
            HomeAction.START -> wearText("Edzés indítása")
            HomeAction.RESUME -> wearText("Edzés folytatása")
            HomeAction.DAYS -> "Program napjai"
            HomeAction.RESULT -> wearText("Edzés mentésének állapota")
            HomeAction.PHONE -> wearText("Telefon megnyitása a folytatáshoz")
        }
    val pulse = wearHomeLivePulse(health, active?.workoutId, enabled, now)
    val clock = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(now))
    val language = java.util.Locale.getDefault().language
    val phoneCaption = when (language) { "hu" -> "Telefon"; "de" -> "Telefon"; "ro" -> "Telefon"; else -> "Phone" }
    val settingsCaption = when (language) { "hu" -> "Beáll."; "de" -> "Einst."; "ro" -> "Setări"; else -> "Settings" }
    val connection = when (phoneConnected) { true -> "●"; false -> "○"; null -> "…" }
    val batteryColor = if (battery != null && battery <= 15) Color(0xFFEF5350) else theme.accentColor
    val status = saveStatus?.let { wearSaveLabel(it.phase) } ?: wearText(model.source)
    BoxWithConstraints(
        Modifier.fillMaxSize().background(theme.backgroundColor).testTag("wear-round-list")
    ) {
        val diameter = minOf(maxWidth, maxHeight)
        val origin = (maxHeight - diameter) / 2
        val compact = diameter < 190.dp
        val primarySize = if (diameter >= 172.dp) 56.dp else 48.dp
        val side = 48.dp
        val gap = if (diameter >= 190.dp) 5.dp else 2.dp
        val actionsWidth = side * 2 + primarySize + gap * 2
        val actionsTop = origin + (diameter - primarySize - 47.dp).coerceAtLeast(48.dp)
        val bodyTop = origin + diameter * .095f
        val bodyHeight = (actionsTop - bodyTop - 2.dp).coerceAtLeast(1.dp)
        Canvas(Modifier.align(Alignment.Center).size(diameter)) {
            val inset = 7.dp.toPx()
            val topLeft = Offset(inset, inset)
            val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
            drawArc(HomeLine, 135f, 90f, false, topLeft, arcSize, style = stroke)
            drawArc(HomeLine, -45f, 90f, false, topLeft, arcSize, style = stroke)
            val fraction = (battery?.coerceIn(0, 100) ?: 0) / 100f
            if (fraction > 0f) drawArc(batteryColor, 135f, 90f * fraction, false, topLeft, arcSize, style = stroke)
            // No daily total/goal in the current snapshot: the right arc is intentionally empty.
        }
        Column(
            Modifier.align(Alignment.TopCenter).offset(y = bodyTop)
                .width(diameter * .72f).height(bodyHeight).testTag("wear-fixed-body"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeFitText("$connection $phoneCaption ↗", Modifier.fillMaxWidth().height(12.dp),
                10f, if (phoneConnected == true) theme.secondaryColor else HomeMuted)
            HomeFitText(clock, Modifier.fillMaxWidth().weight(1f).testTag("wear-home-clock"),
                if (compact) 28f else 38f, HomeWhite, weight = FontWeight.Bold, mono = true)
            HomeFitText("${wearText(model.title)} · $status", Modifier.fillMaxWidth().height(13.dp).testTag("wear-home-title"),
                10f, HomeMuted)
            HomeFitText(
                if (pulse != null) "♥ $pulse bpm · ${wearText("Mérés aktív")}" else "♥ – · ${wearText(model.measurement)}",
                Modifier.fillMaxWidth().height(13.dp).testTag("wear-home-measurement"),
                10f, theme.secondaryColor, weight = FontWeight.SemiBold,
            )
        }
        Row(
            Modifier.align(Alignment.TopCenter).offset(y = actionsTop).width(actionsWidth)
                .height(primarySize).testTag("wear-fixed-actions"),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeQuickButton(R.drawable.ic_tp_phone, phoneCaption, WearMenu216.text("phone"), theme, false, onOpenPhone)
            HomeQuickButton(
                if (model.action == HomeAction.PHONE) R.drawable.ic_tp_phone else R.drawable.ic_tp_workout,
                wearText(model.actionLabel), primaryDescription, theme, true, onPrimary, size = primarySize,
            )
            HomeQuickButton(R.drawable.ic_tp_settings, settingsCaption, WearMenu216.text("settings"), theme, false, onSettings)
        }
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = actionsTop + primarySize)
                .width(diameter * .42f).height(48.dp)
                .clickable(role = Role.Button, onClick = onMenu)
                .semantics { contentDescription = wearText("Főmenü") },
            contentAlignment = Alignment.TopCenter,
        ) {
            HomeFitText(wearText("··· Menü"), Modifier.fillMaxWidth().height(20.dp), 11f, HomeWhite)
        }
        Row(
            Modifier.align(Alignment.Center).offset(x = -diameter * .29f, y = diameter * .31f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_tp_battery), null, Modifier.size(11.dp), colorFilter = ColorFilter.tint(batteryColor))
            Text(battery?.let { "${it.coerceIn(0, 100)}%" } ?: "–", color = batteryColor, fontSize = 9.sp)
        }
        Row(
            Modifier.align(Alignment.Center).offset(x = diameter * .29f, y = diameter * .31f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_tp_steps), null, Modifier.size(11.dp), colorFilter = ColorFilter.tint(HomeMuted))
            Text("–", color = HomeMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun HomeQuickButton(
    icon: Int,
    caption: String,
    description: String,
    theme: WearHomeTheme,
    primary: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Column(
        Modifier.size(size)
            .background(
                if (danger) Color(0xFF3B232A) else if (primary) theme.accentColor.copy(alpha = .24f) else theme.elevatedColor,
                CircleShape,
            )
            .border(
                1.dp,
                if (danger) Color(0xFF7D4653) else if (primary) theme.accentColor else theme.outlineColor,
                CircleShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painterResource(icon),
            null,
            Modifier.size(if (primary) 27.dp else 22.dp),
            colorFilter = ColorFilter.tint(
                if (danger) Color(0xFFEFBCC3) else theme.textColor
            ),
        )
        HomeFitText(caption, Modifier.width(size - 4.dp).height(13.dp), if (primary) 10f else 8.5f, theme.textColor, weight = FontWeight.SemiBold)
    }
}

/** Fit actual measured text into a bounded slot, retaining full accessibility text. */
@Composable
private fun HomeFitText(
    text: String,
    modifier: Modifier,
    requested: Float,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    mono: Boolean = false,
    maxLines: Int = 1,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val font = if (mono) FontFamily.Monospace else FontFamily.SansSerif
        val fitted =
            remember(
                text,
                constraints.maxWidth,
                constraints.maxHeight,
                density,
                requested,
                weight,
                font,
                maxLines,
            ) {
                var size = requested * density.fontScale.coerceAtMost(1.1f)
                repeat(8) {
                    val result =
                        measurer.measure(
                            text,
                            TextStyle(
                                fontSize = (size / density.fontScale).sp,
                                lineHeight = (size * 1.12f / density.fontScale).sp,
                                fontWeight = weight,
                                fontFamily = font,
                            ),
                            constraints = Constraints(maxWidth = constraints.maxWidth),
                            maxLines = maxLines,
                            softWrap = maxLines > 1,
                        )
                    if (result.hasVisualOverflow || result.size.height > constraints.maxHeight)
                        size *= .88f
                }
                size.coerceAtLeast(5.5f)
            }
        Text(
            text,
            color = color,
            fontSize = (fitted / density.fontScale).sp,
            lineHeight = (fitted * 1.12f / density.fontScale).sp,
            fontFamily = font,
            fontWeight = weight,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
