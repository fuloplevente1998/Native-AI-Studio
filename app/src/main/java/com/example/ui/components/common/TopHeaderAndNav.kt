package com.example.ui.components

import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.rememberGlassMotionEnabled
import com.example.ui.theme.glassNavigationFade
import com.repforge.app.ui.theme.trainPilotClickable

import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassHeaderMaterial
import com.example.ui.theme.glassHeaderControl
import com.example.ui.theme.LocalGlassOpticsEnabled
import com.example.ui.theme.glassEffectsActive
import com.repforge.app.ui.theme.trainPilotPressIndication
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotSoftGlow

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.MainTab
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.selectionColors

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import com.example.localization.LocalTrainPilotStrings
import com.example.model.SubScreen

@Composable
fun TrainPilotMountainLogo(
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier.size(28.dp, 28.dp)
) {
    Image(
        painter = painterResource(id = com.repforge.app.R.drawable.trainpilot_launcher_foreground),
        contentDescription = "TrainPilot Logo",
        modifier = modifier
    )
}

/**
 * Global Top Header + 5-Button Top Navigation Bar:
 * - Properly padded below the Android status bar (WindowInsets.statusBars + extra top margin)
 * - Brand followed by Coach, Journal, Calendar and Settings.
 * - Single row of the 5 primary navigation buttons at the top: Kezdőlap, Programok, Edzés, Fejlődés, Egészség
 */
@Composable
fun TrainPilotGlobalTopHeaderAndNav(
    currentTab: MainTab,
    currentSubScreen: SubScreen,
    currentLanguage: AppLanguage,
    selectedPaletteId: Int,
    strings: LocalizedStrings,
    onSelectTab: (MainTab) -> Unit,
    onNavigateToSubScreen: (SubScreen, MainTab?) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenPalettePicker: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val selection = remember(palette) { palette.selectionColors() }
    var showLangMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
    if (palette.isLiquidGlass) {
        // Only the optical background is feathered. The foreground stays sharp and
        // interactive; the shared dark upper scene is visible at the lower edge.
        Box(Modifier.matchParentSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Black, .65f to Color.Black,
                    1f to Color.Transparent), blendMode = BlendMode.DstIn)
            }
            .glassHeaderMaterial(palette, RoundedCornerShape(0.dp), preserveOriginalMaterial = true)
            .glassNavigationFade(palette, currentTab, currentSubScreen))
    }
    Surface(
        color = if (palette.isLiquidGlass) Color.Transparent else palette.customCardPaint ?: palette.background,
        modifier = Modifier.fillMaxWidth()
            .then(if (palette.isLiquidGlass) Modifier else Modifier.trainPilotBorder(
                width = 0.5.dp, color = palette.cardBorder.copy(alpha = 0.5f)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 2.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Brand and shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .trainPilotClickable(RoundedCornerShape(10.dp)) { onSelectTab(MainTab.HOME) }
                        .padding(horizontal = 4.dp)
                        .testTag("top_bar_brand_home"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val compact = maxWidth < 126.dp
                    val brandStyle = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (compact) 14.sp else 18.sp, letterSpacing = (-0.2).sp
                    )
                    if (compact) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TrainPilotMountainLogo(modifier = Modifier.size(18.dp))
                            TrainPilotFitText("TrainPilot", style = brandStyle, color = palette.primary,
                                fontWeight = FontWeight.Bold, minSize = 9.sp)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TrainPilotMountainLogo(modifier = Modifier.size(32.dp))
                            TrainPilotFitText("TrainPilot", style = brandStyle, color = palette.primary,
                                fontWeight = FontWeight.Bold, minSize = 11.sp)
                        }
                    }
                }

                // Labelled shortcuts followed by icon-only actions.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val isCalendarActive = (currentTab == MainTab.CALENDAR || currentSubScreen == SubScreen.CALENDAR_PLANNER) && currentSubScreen != SubScreen.SETTINGS_MAIN
                    val isCoachActive = (currentTab == MainTab.COACH || currentSubScreen == SubScreen.COACH_DETAIL) && currentSubScreen != SubScreen.SETTINGS_MAIN
                    val isSettingsActive = currentSubScreen == SubScreen.SETTINGS_MAIN

                    TopNavPillItem(
                        label = strings.navCoach,
                        icon = TrainPilotCoachIcon,
                        selected = isCoachActive,
                        onClick = { onSelectTab(MainTab.COACH) },
                        modifier = Modifier.width(54.dp),
                        testTag = "top_bar_coach_button",
                        compact = true
                    )
                    TopNavPillItem(
                        label = strings.navLog,
                        icon = Icons.Default.Description,
                        selected = currentTab == MainTab.LOG && currentSubScreen != SubScreen.SETTINGS_MAIN,
                        onClick = { onSelectTab(MainTab.LOG) },
                        modifier = Modifier.width(54.dp),
                        testTag = "top_bar_journal_button",
                        compact = true
                    )

                    val iconShortcutShape = if (palette.isLiquidGlass) liquidTopShortcutShape else RoundedCornerShape(10.dp)
                    Surface(
                        shape = iconShortcutShape,
                        color = if (palette.isLiquidGlass) Color.Transparent else if (isCalendarActive) selection.surface else palette.surfaceElevated,
                        modifier = Modifier
                            .size(48.dp)
                            .then(if (palette.isLiquidGlass) Modifier.glassHeaderControl(palette, iconShortcutShape,
                                if (isCalendarActive) GlassMaterialRole.Selection else GlassMaterialRole.Button) else Modifier)
                            .trainPilotClickable(iconShortcutShape) { onSelectTab(MainTab.CALENDAR) }
                            .testTag("top_bar_calendar_button")
                            .trainPilotBorder(1.dp, if (isCalendarActive) selection.border else palette.cardBorder, iconShortcutShape)
                            .clip(iconShortcutShape)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = strings.navCalendar,
                                tint = if (isCalendarActive) selection.foreground else palette.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 3. Beállítások (Settings) Icon Button
                    Surface(
                        shape = iconShortcutShape,
                        color = if (palette.isLiquidGlass) Color.Transparent else if (isSettingsActive) selection.surface else palette.surfaceElevated,
                        modifier = Modifier
                            .size(48.dp)
                            .then(if (palette.isLiquidGlass) Modifier.glassHeaderControl(palette, iconShortcutShape,
                                if (isSettingsActive) GlassMaterialRole.Selection else GlassMaterialRole.Button) else Modifier)
                            .trainPilotClickable(iconShortcutShape) { onOpenSettings() }
                            .testTag("top_bar_settings_button")
                            .trainPilotBorder(1.dp, if (isSettingsActive) selection.border else palette.cardBorder, iconShortcutShape)
                            .clip(iconShortcutShape)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = strings.settingsTitle,
                                tint = if (isSettingsActive) selection.foreground else palette.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Fő navigációs sor: Kezdőlap | Programok | Edzés | Fejlődés | Egészség (5 optimális fül)
            // Liquid Glass: one shared glass pill slides between the tabs (spring, liquid stretch).
            val glassNav = palette.glassEffectsActive(LocalGlassOpticsEnabled.current)
            val pillTrack = if (glassNav) remember { GlassPillTrack() } else null
            val tabSelected = listOf(
                currentTab == MainTab.HOME && currentSubScreen == SubScreen.NONE,
                (currentTab == MainTab.PROGRAMS || currentSubScreen == SubScreen.PROGRAMS_LIST) && currentSubScreen != SubScreen.SETTINGS_MAIN,
                isWorkoutTabSelected(currentTab, currentSubScreen),
                currentTab == MainTab.PROGRESS && currentSubScreen == SubScreen.NONE,
                currentTab == MainTab.HEALTH && currentSubScreen == SubScreen.NONE
            )
            val trackShape = RoundedCornerShape(50)
            val navRow: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .then(if (palette.isLiquidGlass && pillTrack == null) Modifier.glassHeaderControl(palette, trackShape, GlassMaterialRole.Navigation)
                        .padding(2.dp) else Modifier)
                    .testTag("top_nav_row")
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(if (palette.isLiquidGlass) 2.dp else 6.dp)
            ) {
                val itemModifier = Modifier.weight(1f).fillMaxHeight()

                TopNavPillItem(
                    label = strings.navHome,
                    icon = Icons.Default.Home,
                    selected = tabSelected[0],
                    onClick = { onSelectTab(MainTab.HOME) },
                    modifier = itemModifier,
                    testTag = "top_nav_home",
                    pillTrack = pillTrack, pillIndex = 0
                )
                TopNavPillItem(
                    label = strings.navPrograms,
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    selected = tabSelected[1],
                    onClick = { onSelectTab(MainTab.PROGRAMS) },
                    modifier = itemModifier,
                    testTag = "top_nav_programs",
                    pillTrack = pillTrack, pillIndex = 1
                )
                TopNavPillItem(
                    label = strings.navWorkout,
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    selected = tabSelected[2],
                    onClick = { onSelectTab(MainTab.WORKOUT) },
                    modifier = itemModifier,
                    testTag = "top_nav_workout",
                    pillTrack = pillTrack, pillIndex = 2
                )
                TopNavPillItem(
                    label = strings.navProgress,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    selected = tabSelected[3],
                    onClick = { onSelectTab(MainTab.PROGRESS) },
                    modifier = itemModifier,
                    testTag = "top_nav_progress",
                    pillTrack = pillTrack, pillIndex = 3
                )
                TopNavPillItem(
                    label = strings.navHealth,
                    icon = if (tabSelected[4]) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    selected = tabSelected[4],
                    onClick = { onSelectTab(MainTab.HEALTH) },
                    modifier = itemModifier,
                    testTag = "top_nav_health",
                    pillTrack = pillTrack, pillIndex = 4
                )
            }
            }
            if (pillTrack != null) {
                Box(Modifier.fillMaxWidth().glassHeaderControl(palette, trackShape, GlassMaterialRole.Navigation)
                    .padding(2.dp)) {
                    GlassSelectionPill(pillTrack, tabSelected.indexOf(true), trackShape, palette, selection.border)
                    navRow()
                }
            } else navRow()
        }
    }
    }
}

private val liquidTopShortcutShape = RoundedCornerShape(50)

@Composable
internal fun TopNavPillItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    compact: Boolean = false,
    /** Non-null: a shared [GlassSelectionPill] paints the selection; the item stays transparent. */
    pillTrack: GlassPillTrack? = null,
    pillIndex: Int = -1
) {
    val palette = LocalTrainPilotPalette.current
    val selection = remember(palette) { palette.selectionColors() }
    // Material family is independent from the expensive optics gate (active measurement).
    val glass = palette.isLiquidGlass
    val hosted = glass && pillTrack != null
    val legacyShape = RoundedCornerShape(12.dp)
    val itemShape = if (glass) liquidTopShortcutShape else legacyShape
    val glowModifier = if (selected && !glass) Modifier.trainPilotSoftGlow(palette.primary, legacyShape) else Modifier
    val fill = if (glass) Color.Transparent else if (selected) selection.surface else palette.surface
    val source = remember { MutableInteractionSource() }
    val ownGlass = !hosted && (selected || compact)
    val observeDrag = if (hosted && selected && rememberGlassMotionEnabled()) pillTrack?.drag?.observe() ?: Modifier else Modifier
    val selectedModifier = if (glass) observeDrag.trainPilotPressIndication(itemShape, source)
        .selectable(selected, interactionSource = source,
            indication = null, role = Role.Tab, onClick = onClick)
        .then(if (ownGlass) Modifier.glassHeaderControl(palette, itemShape,
            if (selected) GlassMaterialRole.Selection else GlassMaterialRole.Button) else Modifier)
    else Modifier.selectable(selected = selected, enabled = true, role = Role.Tab, onClick = onClick)

    Surface(
        shape = itemShape,
        color = fill,
        modifier = modifier
            .glassPillSlot(if (hosted) pillTrack else null, pillIndex)
            .then(glowModifier)
            .heightIn(min = 48.dp)
            .then(if (glass) Modifier else Modifier.trainPilotBorder(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) selection.border else palette.cardBorder.copy(alpha = if (glass) .45f else palette.cardBorder.alpha),
                shape = itemShape
            ))
            .clip(itemShape)
            .then(selectedModifier)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(
                vertical = if (compact) 3.dp else 5.dp,
                horizontal = 2.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) selection.foreground else palette.textSecondary,
                modifier = Modifier.size(if (compact) 16.dp else 18.dp)
            )
            Spacer(modifier = Modifier.height(if (compact) 2.dp else 3.dp))
            val labelStyle = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp
            )
            val labelColor = if (selected) selection.foreground else palette.textPrimary
            if (compact) TrainPilotFitText(label, style = labelStyle, color = labelColor,
                minSize = 8.sp, textAlign = TextAlign.Center)
            else Text(label, style = labelStyle, color = labelColor, maxLines = 2,
                textAlign = TextAlign.Center, softWrap = true, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TrainPilotRedCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    testTag: String = "",
    enabled: Boolean = true,
    closeDescription: String? = null
) {
    val strings = LocalTrainPilotStrings.current
    val palette = LocalTrainPilotPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed = if (palette.isLiquidGlass) false else interaction.collectIsPressedAsState().value
    val colors = com.repforge.app.ui.theme.TrainPilotActionAppearance
    val shape = if (palette.isLiquidGlass) RoundedCornerShape(50) else RoundedCornerShape(11.dp)
    val width = if (palette.isLiquidGlass) size + 10.dp else size
    val height = if (palette.isLiquidGlass) (size - 10.dp).coerceAtLeast(minOf(size, 24.dp)) else size
    val fill = lerp(colors.negativeBackground, colors.negativeForeground, if (pressed) .10f else 0f)
    // The visual capsule sits inside the same accessible hit target.
    Box(modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
        .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier), contentAlignment = Alignment.Center) {
        Surface(shape = shape, color = if (palette.isLiquidGlass) fill.copy(alpha = .78f) else fill,
            modifier = Modifier.size(width, height).clip(shape)
                .then(if (palette.isLiquidGlass) Modifier.trainPilotPressIndication(shape, interaction, enabled) else Modifier.indication(interaction, ripple()))
                .trainPilotGlassRim(palette, shape, GlassMaterialRole.Button)
                .border(1.dp, colors.negativeOutline.copy(alpha = 0.7f), shape)
                .then(if (testTag.isNotEmpty()) Modifier.testTag("${testTag}_visual") else Modifier)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.Close, contentDescription = closeDescription ?: strings.closeAction,
                    tint = colors.negativeForeground, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    rightExtraContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val palette = LocalTrainPilotPalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (leadingIcon != null) Icon(leadingIcon, null, tint = palette.primary, modifier = Modifier.padding(end = 8.dp).size(22.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (rightExtraContent != null) {
                rightExtraContent()
            }
            TrainPilotRedCloseButton(
                onClick = onBack,
                testTag = "subscreen_back_button"
            )
        }
    }
}

@Composable
fun TrainPilotBottomNavigationBar(
    currentTab: MainTab,
    strings: LocalizedStrings,
    onSelectTab: (MainTab) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        color = if (palette.isLiquidGlass) Color.Transparent else palette.cardPaint,
        tonalElevation = if (palette.isLiquidGlass) 0.dp else 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotGlassMaterial(palette, RoundedCornerShape(0.dp))
            .trainPilotBorder(width = 1.dp, color = palette.cardBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(MainTab.HOME, strings.navHome, Icons.Default.Home),
                Triple(MainTab.WORKOUT, strings.navWorkout, Icons.AutoMirrored.Filled.DirectionsRun),
                Triple(MainTab.LOG, strings.navLog, Icons.Default.Description),
                Triple(MainTab.PROGRESS, strings.navProgress, Icons.AutoMirrored.Filled.TrendingUp),
                Triple(
                    MainTab.HEALTH,
                    strings.navHealth,
                    if (currentTab == MainTab.HEALTH) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                )
            )

            items.forEach { (tab, label, icon) ->
                val isSelected = currentTab == tab
                val selection = remember(palette) { palette.selectionColors() }
                val tint = if (isSelected) selection.foreground else palette.textSecondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) selection.surface else Color.Transparent, RoundedCornerShape(12.dp))
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelectTab(tab) })
                        .padding(vertical = 6.dp)
                        .testTag("bottom_nav_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
