package com.example.ui.components

import com.repforge.app.ui.theme.trainPilotClickable

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
    var showLangMenu by remember { mutableStateOf(false) }

    Surface(
        color = palette.background,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotBorder(width = 0.5.dp, color = palette.cardBorder.copy(alpha = 0.5f))
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

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCalendarActive) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .size(48.dp)
                            .trainPilotClickable(RoundedCornerShape(10.dp)) { onSelectTab(MainTab.CALENDAR) }
                            .testTag("top_bar_calendar_button")
                            .trainPilotBorder(1.dp, if (isCalendarActive) palette.primary else palette.cardBorder, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = strings.navCalendar,
                                tint = if (isCalendarActive) palette.onPrimary else palette.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 3. Beállítások (Settings) Icon Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSettingsActive) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .size(48.dp)
                            .trainPilotClickable(RoundedCornerShape(10.dp)) { onOpenSettings() }
                            .testTag("top_bar_settings_button")
                            .trainPilotBorder(1.dp, if (isSettingsActive) palette.primary else palette.cardBorder, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = strings.settingsTitle,
                                tint = if (isSettingsActive) palette.onPrimary else palette.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Fő navigációs sor: Kezdőlap | Programok | Edzés | Fejlődés | Egészség (5 optimális fül)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .testTag("top_nav_row")
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val itemModifier = Modifier.weight(1f).fillMaxHeight()

                TopNavPillItem(
                    label = strings.navHome,
                    icon = Icons.Default.Home,
                    selected = currentTab == MainTab.HOME && currentSubScreen == SubScreen.NONE,
                    onClick = { onSelectTab(MainTab.HOME) },
                    modifier = itemModifier,
                    testTag = "top_nav_home"
                )
                TopNavPillItem(
                    label = strings.navPrograms,
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    selected = (currentTab == MainTab.PROGRAMS || currentSubScreen == SubScreen.PROGRAMS_LIST) && currentSubScreen != SubScreen.SETTINGS_MAIN,
                    onClick = { onSelectTab(MainTab.PROGRAMS) },
                    modifier = itemModifier,
                    testTag = "top_nav_programs"
                )
                TopNavPillItem(
                    label = strings.navWorkout,
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    selected = isWorkoutTabSelected(currentTab, currentSubScreen),
                    onClick = { onSelectTab(MainTab.WORKOUT) },
                    modifier = itemModifier,
                    testTag = "top_nav_workout"
                )
                TopNavPillItem(
                    label = strings.navProgress,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    selected = currentTab == MainTab.PROGRESS && currentSubScreen == SubScreen.NONE,
                    onClick = { onSelectTab(MainTab.PROGRESS) },
                    modifier = itemModifier,
                    testTag = "top_nav_progress"
                )
                TopNavPillItem(
                    label = strings.navHealth,
                    icon = if (currentTab == MainTab.HEALTH && currentSubScreen == SubScreen.NONE) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    selected = currentTab == MainTab.HEALTH && currentSubScreen == SubScreen.NONE,
                    onClick = { onSelectTab(MainTab.HEALTH) },
                    modifier = itemModifier,
                    testTag = "top_nav_health"
                )
            }
        }
    }
}

@Composable
internal fun TopNavPillItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    compact: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    val glowModifier = if (selected) Modifier.trainPilotSoftGlow(palette.primary, RoundedCornerShape(12.dp)) else Modifier

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.tintedSurface(if (palette.isDark) 0.22f else 0.16f) else palette.surface,
        modifier = modifier
            .then(glowModifier)
            .heightIn(min = 48.dp)
            .trainPilotBorder(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.primary else palette.cardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = selected,
                enabled = true,
                role = Role.Tab,
                onClick = onClick
            )
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
                tint = if (selected) palette.primary else palette.textSecondary,
                modifier = Modifier.size(if (compact) 16.dp else 18.dp)
            )
            Spacer(modifier = Modifier.height(if (compact) 2.dp else 3.dp))
            val labelStyle = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp
            )
            val labelColor = if (selected) palette.primary else palette.textPrimary
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
    enabled: Boolean = true
) {
    val strings = LocalTrainPilotStrings.current
    val palette = LocalTrainPilotPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val iconColor = com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeForeground
    // The painted control shrinks ~10%; its independent hit target remains 48 dp.
    Box(modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
        .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(11.dp), color = lerp(
            com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeBackground,
            com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeForeground, if (pressed) 0.10f else 0f),
            modifier = Modifier.size(size).clip(RoundedCornerShape(11.dp))
                .indication(interaction, ripple())
                .border(1.dp, com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeOutline.copy(alpha = 0.7f), RoundedCornerShape(11.dp))
                .then(if (testTag.isNotEmpty()) Modifier.testTag("${testTag}_visual") else Modifier)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.Close, contentDescription = strings.closeAction,
                    tint = iconColor, modifier = Modifier.size(18.dp))
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
        color = palette.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
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
                val tint = if (isSelected) palette.primary else palette.textSecondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .trainPilotClickable(RoundedCornerShape(12.dp)) { onSelectTab(tab) }
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
