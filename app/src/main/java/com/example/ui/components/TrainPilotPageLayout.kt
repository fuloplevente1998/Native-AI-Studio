package com.example.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotTextButton

internal data class PageLayoutBlock(val id: String, val title: String)

internal fun reconcilePageLayout(saved: List<String>, defaults: List<String>): List<String> =
    (saved.filter { it in defaults } + defaults).distinct()

internal fun movePageLayoutBlock(ids: List<String>, from: Int, to: Int): List<String> =
    ids.toMutableList().apply { add(to, removeAt(from)) }

internal class PageLayoutState(val page: String, val defaults: List<String>, initial: List<String>) {
    var saved by mutableStateOf(initial)
        private set
    var draft by mutableStateOf<List<String>?>(null)
        private set

    val order: List<String> get() = draft ?: saved

    fun open() { if (draft == null) draft = saved }
    fun cancel() { draft = null }
    fun reset() { draft = defaults }
    fun move(from: Int, to: Int) { draft = draft?.let { movePageLayoutBlock(it, from, to) } }

    fun save(context: Context): Boolean {
        val next = draft ?: return false
        val written = context.getSharedPreferences("trainpilot-page-layout", Context.MODE_PRIVATE)
            .edit().putString(page, next.joinToString(",")).commit()
        if (written) {
            saved = next
            draft = null
        }
        return written
    }
}

@Composable
internal fun rememberPageLayout(page: String, defaults: List<String>): PageLayoutState {
    val context = LocalContext.current.applicationContext
    return remember(context, page, defaults) {
        val previous = context.getSharedPreferences("trainpilot-page-layout", Context.MODE_PRIVATE)
            .getString(page, null).orEmpty().split(',')
        PageLayoutState(page, defaults, reconcilePageLayout(previous, defaults))
    }
}

private fun AppLanguage.pageLayoutText(hu: String, en: String, de: String, ro: String): String = when (this) {
    AppLanguage.HU -> hu
    AppLanguage.EN -> en
    AppLanguage.DE -> de
    AppLanguage.RO -> ro
}

private class PageLayoutDrag(
    val state: TrainPilotReorderState,
    val titles: Map<String, String>,
    val language: AppLanguage,
    val gap: androidx.compose.ui.unit.Dp
)

private val LocalPageLayoutDrag = staticCompositionLocalOf<PageLayoutDrag?> { null }

@Composable
internal fun PageLayoutTile(layout: PageLayoutState?, id: String, content: @Composable () -> Unit) {
    if (layout == null) {
        content()
        return
    }
    val editor = LocalPageLayoutDrag.current
    if (editor == null) {
        Box(Modifier.fillMaxWidth().testTag("page_${layout.page}_block_$id")
            .pointerInput(layout) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.y > 48.dp.toPx()) return@awaitEachGesture
                    awaitLongPressOrCancellation(down.id)?.let {
                        it.consume()
                        layout.open()
                    }
                }
            }) { content() }
        return
    }

    val palette = LocalTrainPilotPalette.current
    val drag = editor.state
    val ids = layout.order
    val index = ids.indexOf(id)
    val target = drag.dropTarget(drag.draggingIndex, ids.lastIndex, drag.draggingStepPx)
    val markDrop = drag.draggingKey != null && index == target && target != drag.draggingIndex
    Row(Modifier.fillMaxWidth().trainPilotReorderItem(drag, id, index)
        .drawWithContent {
            drawContent()
            if (markDrop) {
                val direction = if (target < drag.draggingIndex) -1 else 1
                val y = if (direction < 0) 1.dp.toPx() else size.height - 1.dp.toPx()
                drawLine(palette.primary, Offset(0f, y), Offset(size.width, y),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 3.dp.toPx())))
            }
        }.testTag("page_layout_editor_block_$id"),
        verticalAlignment = Alignment.Top) {
        Box(Modifier.weight(1f)) {
            // Keep the real card visuals. The cover takes taps in edit mode but leaves
            // vertical drag gestures to the existing page scroll container.
            Box(Modifier.fillMaxWidth().clearAndSetSemantics { }) { content() }
            Box(Modifier.matchParentSize().pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    waitForUpOrCancellation()?.consume()
                }
            })
        }
        TrainPilotReorderHandle(drag, id, index, ids.lastIndex, editor.gap, editor.titles.getValue(id),
            editor.language.pageLayoutText("Mozgatás fel", "Move up", "Nach oben", "Mută în sus"),
            editor.language.pageLayoutText("Mozgatás le", "Move down", "Nach unten", "Mută în jos"),
            "page_layout_handle_$id", layout::move, icon = Icons.Default.DragIndicator, immediateDrag = true)
    }
}

@Composable
internal fun PageLayoutFooter(layout: PageLayoutState, language: AppLanguage) {
    if (layout.draft != null) return
    val palette = LocalTrainPilotPalette.current
    TrainPilotTextButton(onClick = layout::open,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("page_customize_${layout.page}")) {
        Icon(Icons.Default.DragHandle, null, tint = palette.primary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(language.pageLayoutText("Oldal testreszabása", "Customize page", "Seite anpassen", "Personalizează pagina"),
            style = MaterialTheme.typography.labelLarge, color = palette.primary)
    }
}

@Composable
internal fun PageLayoutFrame(
    layout: PageLayoutState,
    language: AppLanguage,
    blocks: List<PageLayoutBlock>,
    scrollable: ScrollableState,
    gap: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    val ids = layout.draft
    if (ids == null) {
        content()
        return
    }
    val context = LocalContext.current
    val palette = LocalTrainPilotPalette.current
    val density = LocalDensity.current
    val drag = rememberTrainPilotReorderState()
    val gapPx = with(density) { gap.toPx() }
    SideEffect { drag.measuredOrder = ids; drag.gapPx = gapPx }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    var saveFailed by rememberSaveable { mutableStateOf(false) }
    val edge = with(density) { 64.dp.toPx() }
    val maxSpeed = with(density) { 12.dp.toPx() }
    LaunchedEffect(drag.draggingKey, scrollable) {
        if (drag.draggingKey == null) return@LaunchedEffect
        while (drag.draggingKey != null) {
            withFrameNanos { }
            val edgeSize = minOf(edge, viewport.height / 3)
            if (edgeSize <= 0f || drag.draggingKey == null) continue
            val top = (viewport.top + edgeSize - drag.pointerY).coerceIn(0f, edgeSize)
            val bottom = (drag.pointerY - viewport.bottom + edgeSize).coerceIn(0f, edgeSize)
            val scroll = (bottom - top) / edgeSize * maxSpeed
            if (scroll != 0f) drag.offsetY += scrollable.dispatchRawDelta(scroll)
        }
    }

    BackHandler { layout.cancel() }
    Column(Modifier.fillMaxSize().background(palette.background).testTag("page_layout_editor_${layout.page}")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(language.pageLayoutText("Csempék rendezése", "Arrange tiles", "Kacheln anordnen", "Aranjează cardurile"),
                modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = palette.textPrimary)
            TrainPilotTextButton(onClick = layout::reset, modifier = Modifier.testTag("page_layout_reset")) {
                Text(language.pageLayoutText("Alaphelyzet", "Reset", "Zurücksetzen", "Restabilește"), color = palette.primary)
            }
        }
        val titles = blocks.associate { it.id to it.title }
        Box(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { viewport = it.boundsInRoot() }) {
            CompositionLocalProvider(LocalPageLayoutDrag provides PageLayoutDrag(drag, titles, language, gap)) {
                content()
            }
        }
        HorizontalDivider(color = palette.cardBorder)
        if (saveFailed) Text(language.pageLayoutText("A sorrend mentése sikertelen.", "Couldn't save the layout.",
            "Die Reihenfolge konnte nicht gespeichert werden.", "Ordinea nu a putut fi salvată."),
            color = palette.danger, modifier = Modifier.padding(horizontal = 12.dp))
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainPilotSecondaryButton(language.pageLayoutText("Mégse", "Cancel", "Abbrechen", "Anulează"),
                modifier = Modifier.weight(1f).testTag("page_layout_cancel"), onClick = layout::cancel)
            TrainPilotPrimaryButton(language.pageLayoutText("Mentés", "Save", "Speichern", "Salvează"),
                modifier = Modifier.weight(1f).testTag("page_layout_save"), onClick = { saveFailed = !layout.save(context) })
        }
    }
}
