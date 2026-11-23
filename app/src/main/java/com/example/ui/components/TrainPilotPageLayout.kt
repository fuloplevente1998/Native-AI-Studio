package com.example.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.TrainPilotTextButton
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

internal data class PageLayoutBlock(val id: String, val title: String)

internal fun reconcilePageLayout(saved: List<String>, defaults: List<String>): List<String> =
    (saved.filter { it in defaults } + defaults).distinct()

internal fun movePageLayoutBlock(ids: List<String>, from: Int, to: Int): List<String> =
    ids.toMutableList().apply { add(to, removeAt(from)) }

internal class PageLayoutState(val page: String, val defaults: List<String>, initial: List<String>,
    initialChildren: Map<String, List<String>> = emptyMap()) {
    var saved by mutableStateOf(initial)
        private set
    var draft by mutableStateOf<List<String>?>(null)
        private set
    private var savedChildren by mutableStateOf(initialChildren)
    private var draftChildren by mutableStateOf(initialChildren)

    val order: List<String> get() = draft ?: saved

    fun open() {
        if (draft != null) return
        draftChildren = savedChildren
        draft = saved
    }
    fun cancel() { draft = null }
    fun reset() { draftChildren = emptyMap(); draft = defaults }
    fun move(from: Int, to: Int) { draft = draft?.let { movePageLayoutBlock(it, from, to) } }
    fun childOrder(group: String, defaults: List<String>, legacy: Map<String, List<String>> = emptyMap()): List<String> {
        val groups = if (draft != null) draftChildren else savedChildren
        return reconcilePageLayout(groups[group] ?: legacy.flatMap { (id, row) -> reconcilePageLayout(groups[id].orEmpty(), row) }, defaults)
    }

    fun moveChild(group: String, defaults: List<String>, from: Int, to: Int, legacy: Map<String, List<String>> = emptyMap()) {
        if (draft != null) {
            val order = movePageLayoutBlock(childOrder(group, defaults, legacy), from, to)
            draftChildren = (draftChildren - legacy.keys) + (group to order)
        }
    }

    fun save(context: Context): Boolean {
        val next = draft ?: return false
        val childrenJson = JSONObject().apply {
            draftChildren.forEach { (group, ids) -> put(group, JSONArray(ids)) }
        }
        val written = context.getSharedPreferences("trainpilot-page-layout", Context.MODE_PRIVATE)
            .edit().putString(page, next.joinToString(",")).putString("${page}_children", childrenJson.toString()).commit()
        if (written) {
            saved = next
            savedChildren = draftChildren
            draft = null
        }
        return written
    }
}

@Composable
internal fun rememberPageLayout(
    page: String, defaults: List<String>, newBlockPlacements: List<Pair<String, String>> = emptyList()
): PageLayoutState {
    val context = LocalContext.current.applicationContext
    return remember(context, page, defaults, newBlockPlacements) {
        val prefs = context.getSharedPreferences("trainpilot-page-layout", Context.MODE_PRIVATE)
        val previous = prefs.getString(page, null).orEmpty().split(',')
        val children = runCatching { JSONObject(prefs.getString("${page}_children", "{}") ?: "{}") }
            .getOrDefault(JSONObject())
        val savedChildren = children.keys().asSequence().mapNotNull { group ->
            val values = children.optJSONArray(group) ?: return@mapNotNull null
            group to List(values.length()) { values.optString(it) }
        }.toMap()
        val initial = newBlockPlacements.fold(reconcilePageLayout(previous, defaults)) { order, (block, anchor) ->
            if (block !in previous && block in order && anchor in order) {
                (order - block).toMutableList().apply { add(indexOf(anchor) + 1, block) }
            } else order
        }
        PageLayoutState(page, defaults, initial, savedChildren)
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
internal fun PageLayoutTile(layout: PageLayoutState?, id: String, hasHorizontalTiles: Boolean = false,
    overlayHandle: Boolean = false, content: @Composable () -> Unit) {
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
    val blockModifier = Modifier.fillMaxWidth().trainPilotReorderItem(drag, id, index)
        .drawWithContent {
            drawContent()
            if (markDrop) {
                val direction = if (target < drag.draggingIndex) -1 else 1
                val y = if (direction < 0) 1.dp.toPx() else size.height - 1.dp.toPx()
                drawLine(palette.primary, Offset(0f, y), Offset(size.width, y),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 3.dp.toPx())))
            }
        }.testTag("page_layout_editor_block_$id")
    val card: @Composable () -> Unit = {
        if (hasHorizontalTiles) content() else PageLayoutCard(content)
    }
    val handle: @Composable (Modifier) -> Unit = { modifier ->
        TrainPilotReorderHandle(drag, id, index, ids.lastIndex, editor.gap, editor.titles.getValue(id),
            editor.language.pageLayoutText("Mozgatás fel", "Move up", "Nach oben", "Mută în sus"),
            editor.language.pageLayoutText("Mozgatás le", "Move down", "Nach unten", "Mută în jos"),
            "page_layout_handle_$id", layout::move, modifier = modifier,
            icon = Icons.Default.DragIndicator, immediateDrag = true)
    }
    if (overlayHandle) {
        Box(blockModifier) {
            card()
            handle(Modifier.align(Alignment.TopEnd))
        }
    } else {
        Row(blockModifier, verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { card() }
            handle(Modifier)
        }
    }
}

@Composable
internal fun PageLayoutCard(content: @Composable () -> Unit) {
    if (LocalPageLayoutDrag.current == null) {
        content()
        return
    }
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().clearAndSetSemantics { }) { content() }
        Box(Modifier.matchParentSize().pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                waitForUpOrCancellation()?.consume()
            }
        })
    }
}

/** Reorder measured grid cells across both axes, with the same draft and save as the page. */
@Composable
internal fun PageLayoutGrid(
    layout: PageLayoutState?,
    group: String,
    tiles: List<PageLayoutBlock>,
    gap: Dp,
    legacy: Map<String, List<String>> = emptyMap(),
    modifier: Modifier = Modifier,
    compactEditor: Boolean = false,
    tileContent: @Composable (String, Modifier) -> Unit
) {
    val defaults = tiles.map { it.id }
    val ids = layout?.childOrder(group, defaults, legacy) ?: defaults
    val editor = LocalPageLayoutDrag.current
    val palette = LocalTrainPilotPalette.current
    val direction = LocalLayoutDirection.current
    val fontScale = LocalDensity.current.fontScale
    val bounds = remember { mutableMapOf<String, Rect>() }
    var dragged by remember { mutableStateOf<String?>(null) }
    var destination by remember { mutableStateOf<String?>(null) }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth < 220.dp || fontScale >= 1.8f -> 1
            maxWidth < 300.dp || fontScale >= 1.4f -> 2
            else -> 3
        }
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            ids.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { id ->
                        key(id) {
                            val cell = Modifier.weight(1f)
                            if (editor == null || layout == null) tileContent(id, cell)
                            else {
                                val index = ids.indexOf(id)
                                val shape = RoundedCornerShape(12.dp)
                                val color = if (destination == id && dragged != id) palette.primary
                                    else if (dragged == id) palette.primary.copy(alpha = .5f) else null
                                Box(
                                    cell.onGloballyPositioned { bounds[id] = it.boundsInRoot() }
                                        .then(if (color != null) Modifier.border(2.dp, color, shape) else Modifier),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Box(Modifier.padding(top = if (compactEditor) 32.dp else 48.dp)) {
                                        PageLayoutCard { tileContent(id, Modifier.fillMaxWidth()) }
                                    }
                                    val title = tiles.first { it.id == id }.title
                                    fun action(target: Int, name: String): CustomAccessibilityAction? =
                                        target.takeIf { it in ids.indices }?.let { to ->
                                            CustomAccessibilityAction(name) {
                                                layout.moveChild(group, defaults, index, to, legacy)
                                                true
                                            }
                                        }

                                    Box(Modifier.size(48.dp).pointerInput(id, ids, columns) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val start = bounds[id]?.center ?: return@awaitEachGesture
                                            down.consume()
                                            dragged = id
                                            destination = id
                                            var traveled = Offset.Zero
                                            try {
                                                val completed = drag(down.id) { change ->
                                                    traveled += change.positionChange()
                                                    change.consume()
                                                    val pointer = start + traveled
                                                    destination = ids.minByOrNull { candidate ->
                                                        bounds[candidate]?.center?.let { (it - pointer).getDistanceSquared() }
                                                            ?: Float.POSITIVE_INFINITY
                                                    }
                                                }
                                                val to = destination?.let(ids::indexOf) ?: -1
                                                if (completed && to in ids.indices && to != index) {
                                                    layout.moveChild(group, defaults, index, to, legacy)
                                                }
                                            } finally {
                                                dragged = null
                                                destination = null
                                            }
                                        }
                                    }.semantics {
                                        contentDescription = title
                                        val nextColumn = if (direction == LayoutDirection.Ltr) index + 1 else index - 1
                                        val previousColumn = if (direction == LayoutDirection.Ltr) index - 1 else index + 1
                                        val lang = editor.language
                                        customActions = listOfNotNull(
                                            action(index - columns, lang.pageLayoutText("Mozgatás fel", "Move up", "Nach oben", "Mută în sus")),
                                            action(index + columns, lang.pageLayoutText("Mozgatás le", "Move down", "Nach unten", "Mută în jos")),
                                            previousColumn.takeIf { it in ids.indices && it / columns == index / columns }
                                                ?.let { action(it, lang.pageLayoutText("Mozgatás balra", "Move left", "Nach links", "Mută la stânga")) },
                                            nextColumn.takeIf { it in ids.indices && it / columns == index / columns }
                                                ?.let { action(it, lang.pageLayoutText("Mozgatás jobbra", "Move right", "Nach rechts", "Mută la dreapta")) }
                                        )
                                    }.testTag("page_layout_grid_${group}_$id"), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DragIndicator, null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** One existing grid row; moving here changes its card positions without moving the outer block. */
@Composable
internal fun PageLayoutHorizontalRow(
    layout: PageLayoutState?,
    group: String,
    tiles: List<PageLayoutBlock>,
    gap: Dp,
    modifier: Modifier = Modifier.fillMaxWidth(),
    cardWidth: Dp? = null,
    tileContent: @Composable (String, Modifier) -> Unit
) {
    val defaults = tiles.map { it.id }
    val ids = layout?.childOrder(group, defaults) ?: defaults
    val editor = LocalPageLayoutDrag.current
    val palette = LocalTrainPilotPalette.current
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1f else -1f
    val gapPx = with(LocalDensity.current) { gap.toPx() }
    var slotWidth by remember { mutableIntStateOf(0) }
    var from by remember { mutableIntStateOf(-1) }
    var offset by remember { mutableFloatStateOf(0f) }
    val step = slotWidth + gapPx
    val target = if (from < 0 || step <= 0f) from else
        (from + (offset * direction / step).roundToInt()).coerceIn(ids.indices)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        ids.forEachIndexed { index, id ->
            key(id) {
                val size = if (cardWidth == null) Modifier.weight(1f) else Modifier.width(cardWidth)
                if (layout == null || editor == null) {
                    tileContent(id, size)
                } else if (ids.size < 2) {
                    Box(size) { PageLayoutCard { tileContent(id, Modifier.fillMaxWidth()) } }
                } else {
                    val displacement = when {
                        index == from -> offset
                        target > from && index in (from + 1)..target -> -step * direction
                        target < from && index in target until from -> step * direction
                        else -> 0f
                    }
                    val markDrop = from >= 0 && index == target && target != from
                    Column(size.onSizeChanged { slotWidth = it.width }
                        .zIndex(if (index == from) 1f else 0f)
                        .graphicsLayer { translationX = displacement }
                        .drawWithContent {
                            drawContent()
                            if (markDrop) {
                                val nearStart = (target < from) == (direction > 0)
                                val x = if (nearStart) 1.dp.toPx() else this.size.width - 1.dp.toPx()
                                drawLine(palette.primary, Offset(x, 0f), Offset(x, this.size.height),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())))
                            }
                        }, horizontalAlignment = Alignment.CenterHorizontally) {
                        val title = tiles.first { it.id == id }.title
                        Box(Modifier.size(width = 48.dp, height = 32.dp)
                            .pointerInput(id, ids, step, direction) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    from = index
                                    offset = 0f
                                    try {
                                        val completed = drag(down.id) { change ->
                                            offset += change.positionChange().x
                                            change.consume()
                                        }
                                        val to = if (step <= 0f) index else
                                            (index + (offset * direction / step).roundToInt()).coerceIn(ids.indices)
                                        if (completed && to != index) layout.moveChild(group, defaults, index, to)
                                    } finally {
                                        from = -1
                                        offset = 0f
                                    }
                                }
                            }.semantics {
                                contentDescription = title
                                customActions = listOfNotNull(
                                    if (index > 0) CustomAccessibilityAction(editor.language.pageLayoutText(
                                        "Mozgatás balra", "Move left", "Nach links", "Mută la stânga")) {
                                        layout.moveChild(group, defaults, index, index - 1); true
                                    } else null,
                                    if (index < ids.lastIndex) CustomAccessibilityAction(editor.language.pageLayoutText(
                                        "Mozgatás jobbra", "Move right", "Nach rechts", "Mută la dreapta")) {
                                        layout.moveChild(group, defaults, index, index + 1); true
                                    } else null
                                )
                            }.testTag("page_layout_horizontal_${group}_$id"),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DragIndicator, null, tint = palette.primary,
                                modifier = Modifier.size(18.dp))
                        }
                        PageLayoutCard { tileContent(id, Modifier.fillMaxWidth()) }
                    }
                }
            }
        }
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
