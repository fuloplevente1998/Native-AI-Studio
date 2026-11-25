package com.repforge.app.wear

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.sizeIn
import androidx.wear.compose.material3.Text

@Composable
internal fun CurvedHeader(text: String, color: Color = Gold, sweep: Float = WEAR_HEADER_SWEEP) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxSize()) {
        CurvedLayout(
            Modifier.align(Alignment.Center).size(minOf(maxWidth, maxHeight) - 16.dp).semantics {
                contentDescription = text
            }
        ) {
            basicCurvedText(
                text,
                modifier = CurvedModifier.sizeIn(maxSweepDegrees = sweep),
                style =
                    CurvedTextStyle(
                        color = color,
                        fontSize = (12.5f * fontScale.coerceAtMost(1.1f) / fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Short pages keep their controls fixed; only content that actually overflows can scroll. */
@Composable
internal fun FixedRoundPanel(
    title: String,
    arc: Boolean = false,
    count: Int = 2,
    gap: Float = 7f,
    headerColor: Color = Gold,
    sweep: Float = WEAR_HEADER_SWEEP,
    onTitleClick: (() -> Unit)? = null,
    titleDescription: String = wearText("Megnyitás telefonon"),
    footerSize: Float = 48f,
    rimMargin: Float = 22f,
    captionBand: Float = 14f,
    pinned: (@Composable () -> Unit)? = null,
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.(Boolean) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("wear-round-list")) {
        val diameter = minOf(maxWidth, maxHeight).value
        val layout = wearFixedPageLayout(maxWidth.value, maxHeight.value, arc, count, gap,
            footerSize, rimMargin, captionBand, if (pinned != null) WEAR_PINNED_ROW_DP else 0f)
        val compact = layout.bodyHeight < 72f * LocalDensity.current.fontScale
        CurvedHeader(title, headerColor, sweep)
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(y = layout.bodyTop.dp)
                .width((diameter - 24f).dp)
                .height(layout.bodyHeight.dp)
                .testTag("wear-fixed-body"),
            contentAlignment = Alignment.Center,
        ) {
            val scroll = rememberScrollState()
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = layout.bodyHeight.dp)
                    .verticalScroll(scroll, enabled = scroll.maxValue > 0),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                content(compact)
            }
        }
        if (pinned != null)
            Box(
                Modifier.align(Alignment.TopCenter)
                    .offset(y = layout.pinnedTop.dp)
                    .width((diameter - 48f).dp)
                    .height(layout.pinnedHeight.dp)
                    .testTag("wear-pinned-row"),
                contentAlignment = Alignment.Center,
            ) { pinned() }
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(y = layout.footerTop.dp)
                .width(layout.footerWidth.dp)
                .height(layout.footerHeight.dp)
                .testTag("wear-fixed-actions"),
            contentAlignment = Alignment.TopCenter,
        ) {
            footer()
        }
        if (onTitleClick != null)
            Box(
                Modifier.align(Alignment.TopCenter)
                    .width((diameter * .7f).dp)
                    .height(32.dp)
                    .clickable(role = Role.Button, onClick = onTitleClick)
                    .semantics { contentDescription = titleDescription }
            )
    }
}

@Composable
internal fun ActionPanel(
    title: String,
    count: Int = 2,
    gap: Float = 7f,
    actions: @Composable (WearActionLayout) -> Unit,
    content: @Composable ColumnScope.(Boolean) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = wearActionLayout(maxWidth.value, maxHeight.value, count, gap)
        FixedRoundPanel(
            title,
            count = count,
            gap = gap,
            footer = { actions(layout) },
            content = content,
        )
    }
}

@Composable
internal fun Label(text: String, color: Color = Muted, size: Int = 11, lines: Int = 1) {
    Text(
        text,
        color = color,
        fontSize = size.sp,
        lineHeight = (size + 2).sp,
        textAlign = TextAlign.Center,
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun Title(text: String, size: Int = 19, lines: Int = 2) {
    Text(text, color = White, fontSize = size.sp, lineHeight = (size + 2).sp,
        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = lines, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun Pill(text: String, modifier: Modifier = Modifier, tone: Color = Gold,
                 enabled: Boolean = true, height: Int = 48, size: Int = 14,
                 lines: Int = 2, onClick: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val captionScale = fontScale.coerceAtMost(1.35f) / fontScale
    val shape = if (lines == Int.MAX_VALUE) RoundedCornerShape(18.dp) else CircleShape
    Box(modifier.fillMaxWidth().heightIn(min = height.dp)
        .background(if (enabled) tone else Secondary, shape)
        .then(if (tone == Danger || tone == SaveTone) Modifier.border(1.dp, if (tone == Danger) Color(0xFFEF6670) else Color(0xFF3ECF87), shape) else Modifier)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = text }.padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (!enabled) Muted else if (tone == Gold) LocalWearHomeTheme.current.onAccentColor else if (tone == Danger) DangerText else if (tone == SaveTone) SaveText else White,
            fontSize = (size * captionScale).sp, lineHeight = ((size + 2) * captionScale).sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            maxLines = lines, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun RoundList(title: String, content: LazyListScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("wear-round-list")) {
        val state = rememberLazyListState()
        // No half-screen spacers or initial center offsets: opening starts at the first row.
        LazyColumn(
            Modifier.fillMaxSize().padding(top = 32.dp).hierarchicalFocusGroup(active = true),
            state = state,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 40.dp), // 2777: rows stay inside the round edge
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        CurvedHeader(title)
    }
}

/** 2777: secondary explanations stay one tap away instead of filling the round screen. */
internal fun LazyListScope.detailsItem(vararg texts: String) {
    item {
        var open by rememberSaveable { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Pill((if (open) "▴ " else "ⓘ ") + wearText("Részletek"), tone = Secondary, height = 44, size = 12,
                modifier = Modifier.testTag("wear-details")) { open = !open }
            if (open) texts.forEach {
                Text(it, color = Muted, fontSize = 11.sp, lineHeight = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("wear-details-text"))
            }
        }
    }
}

/** Long pages scroll individual rows, without moving the fixed title. */
internal fun LazyListScope.centerItem(content: @Composable ColumnScope.() -> Unit) {
    item {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/** Status lives in the existing menus, leaving workout controls and timers intact. */
internal fun LazyListScope.saveSyncItem() {
    centerItem {
        val status=LocalSaveSync.current
        val open=LocalOpenSaveSync.current
        if(status!=null && open!=null) {
            Text(wearSaveLabel(status.phase),color=if(status.phase==WearSavePhase.ERROR)DangerText else Gold,
                fontSize=11.sp,textAlign=TextAlign.Center,
                modifier=Modifier.fillMaxWidth().heightIn(min=44.dp).clickable(role=Role.Button,onClick=open)
                    .padding(horizontal=12.dp,vertical=8.dp).testTag("wear-save-status")
                    .semantics { contentDescription=wearSaveTitle()+": "+wearSaveLabel(status.phase) })
        }
    }
}
