package com.repforge.app.wear

import android.view.ViewConfiguration
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

internal val LocalActionSize = compositionLocalOf { 48f }
/** 2777: two-button footers (confirm, editor) show the caption under the circle, never squeezed inside. */
internal val LocalCaptionBelow = compositionLocalOf { false }

@Composable
internal fun RoundControl(text: String, tone: Color = Secondary, enabled: Boolean = true,
                         caption: String? = null, description: String = caption ?: text, onClick: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val glyph = if (caption == null) 24f else 22f
    val captionScale = fontScale.coerceAtMost(1.35f)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val actionSize = LocalActionSize.current
    val captionStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val captionSize = remember(caption, captionScale, density, textMeasurer, actionSize, captionStyle) {
        val requested = 8.5f * captionScale / fontScale
        val measured = caption?.let { textMeasurer.measure(it, captionStyle.copy(fontSize = requested.sp)).size.width } ?: 0
        val available = with(density) { (actionSize - 4f).dp.toPx() }
        requested * if (measured > available) available / measured else 1f
    }
    if (caption != null && LocalCaptionBelow.current) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompositionLocalProvider(LocalCaptionBelow provides false) {
                RoundControl(text, tone, enabled, caption = null, description = description, onClick = onClick)
            }
            Text(caption, color = if (enabled) White else Muted, fontSize = (10f * captionScale / fontScale).sp,
                lineHeight = (12f * captionScale / fontScale).sp, maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = (actionSize + 16f).dp).padding(top = 2.dp))
        }
        return
    }
    Column(Modifier.size(LocalActionSize.current.dp).background(if (enabled) tone else Card, CircleShape)
        .then(if (tone == Danger || tone == SaveTone) Modifier.border(1.dp, if (tone == Danger) Color(0xFFEF6670) else Color(0xFF3ECF87), CircleShape) else Modifier)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val color = if (!enabled) Muted else if (tone == Gold) LocalWearHomeTheme.current.onAccentColor else if (tone == Danger) DangerText else if (tone == SaveTone) SaveText else White
        Text(text, color = color, fontSize = (glyph / fontScale).sp,
            lineHeight = ((glyph + 2) / fontScale).sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (caption != null) Text(caption, modifier = Modifier.widthIn(max = (actionSize - 4f).dp), style = captionStyle,
            color = color, fontSize = captionSize.sp, lineHeight = (captionSize * 1.18f).sp,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ValueEditor(
    workout: WearWorkout,
    field: String,
    onChange: (String, Double) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val set = workout.currentSet ?: return
    // Capture once for this opened editor; neither sync nor automatic set changes
    // may silently retarget its unsaved draft.
    val target = rememberSaveable(field) { WearRotaryEditor212.target(workout) }
    val editable = WearRotaryEditor212.editable(workout, target)
    val save = LocalEditorSave.current
    var edited by rememberSaveable(target, field) { mutableDoubleStateOf(fieldValue(set, field)) }
    val step = WearRotaryEditor212.step(field, workout.exercise?.repUnit.orEmpty())
    val rotary = remember(target, field) { WearRotaryEditor212() }
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val owner = LocalView.current.findViewTreeLifecycleOwner()
    var foreground by remember(owner) { mutableStateOf(owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != false) }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val pixelsPerDetent = remember(context) { ViewConfiguration.get(context).scaledVerticalScrollFactor }
    DisposableEffect(owner, rotary) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) { foreground = false; rotary.reset() }
            if (event == Lifecycle.Event.ON_RESUME) { foreground = true; rotary.reset() }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { rotary.reset(); owner?.lifecycle?.removeObserver(observer) }
    }
    LaunchedEffect(editable, foreground, windowFocused) {
        rotary.reset()
        if (editable && foreground && windowFocused) focus.requestFocus()
    }
    val title =
        when (field) {
            "weight" -> wearText("Súly")
            "leftSeconds" -> wearText("Bal oldal")
            "rightSeconds" -> wearText("Jobb oldal")
            else -> if (step == 5.0) wearText("Idő") else wearText("Ismétlés")
        }
    Box(Modifier.fillMaxSize().testTag("wear-value-editor")
        .focusRequester(focus)
        .onFocusChanged { focused = it.hasFocus; if (!it.hasFocus) rotary.reset() }
        .onPreRotaryScrollEvent { event ->
            if (!editable || !foreground || !windowFocused || !focused) { rotary.reset(); false }
            else {
                edited = WearRotaryEditor212.adjust(edited, step, rotary.steps(event.verticalScrollPixels, pixelsPerDetent))
                true
            }
        }.focusable()) {
    ActionPanel(
        title,
        actions = { layout ->
            CompositionLocalProvider(LocalActionSize provides layout.buttonSize, LocalCaptionBelow provides true) {
                Row(horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
                    RoundControl("×", caption = wearText("Mégse"), description = wearText("Mégse"), onClick = onBack)
                    RoundControl("✓", SaveTone, enabled=editable && foreground,caption = wearText("Mentés"), description = wearText("✓ Mentés")) {
                        if(save!=null&&!save(field,edited,target))return@RoundControl
                        if (
                            field.endsWith("Seconds") ||
                                (field == "reps" &&
                                    normalizeUnit(workout.exercise?.repUnit.orEmpty())
                                        .startsWith("mp"))
                        ) {
                            // A manually entered duration must not be overwritten by
                            // the old paused stopwatch when the set is recorded.
                            WearStopwatchStore.save(
                                context,
                                WearStopwatch(
                                    "${workout.workoutId}:${workout.exercise?.id}:${set.number}:$field"
                                ),
                            )
                        }
                        if(save==null)onChange(field, edited - fieldValue(workout.currentSet ?: set, field))
                        onBack()
                    }
                }
            }
        },
    ) { compact ->
        val fontScale = LocalDensity.current.fontScale
        val series = if(editable) wearFormat("Sorozat %d/%d", workout.currentSetIndex + 1, workout.exercise?.sets?.size ?: 0)else wearText("A sorozat közben változott")
        if (compact) Text(series, color = Muted, fontSize = (8f / fontScale).sp,
            lineHeight = (10f / fontScale).sp, textAlign = TextAlign.Center)
        else Label(series, size = 9)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RoundControl("−", enabled=editable,description = wearFormat("%s csökkentése", title)) {
                rotary.reset(); edited = WearRotaryEditor212.adjust(edited, step, -1)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val measurer = rememberTextMeasurer()
                    val valueSize = if (compact) 26f / fontScale else 26f
                    val valueLine = if (compact) 28f / fontScale else 28f
                    val style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = valueSize.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                    val naturalWidth = measurer.measure(numberText(edited), style).size.width
                    val available = with(LocalDensity.current) { (maxWidth - 4.dp).toPx() }
                    val fit = (available / naturalWidth.coerceAtLeast(1)).coerceAtMost(1f)
                    Text(numberText(edited), style = style, color = White,
                        fontSize = (valueSize * fit).sp, lineHeight = (valueLine * fit).sp,
                        maxLines = 1, modifier = Modifier.testTag("wear-edited-value"))
                }
                val unit = if (field == "weight") "kg" else if (step == 5.0) "mp" else "ism."
                if (compact) Text(unit, color = Muted, fontSize = (9f / fontScale).sp,
                    lineHeight = (11f / fontScale).sp, textAlign = TextAlign.Center)
                else Label(unit, size = 9)
            }
            RoundControl("+", enabled=editable,description = wearFormat("%s növelése", title)) { rotary.reset(); edited = WearRotaryEditor212.adjust(edited, step, 1) }
        }
        if (!compact) Label(wearFormat("Lépésköz: %s", numberText(step)), size = 9)
    }
}
}
