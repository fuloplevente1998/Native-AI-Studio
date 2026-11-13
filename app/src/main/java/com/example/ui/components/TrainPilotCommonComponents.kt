package com.example.ui.components

import androidx.compose.material.icons.filled.Flag
import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.TrainPilotFrames

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.PaletteCategory
import com.example.ui.theme.TrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.localization.LocalTrainPilotStrings
import com.example.model.ExerciseItem
import com.example.model.SubScreen
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush

private fun tr(lang: AppLanguage, hu: String, en: String, de: String, ro: String): String = when (lang) {
    AppLanguage.HU -> hu
    AppLanguage.EN -> en
    AppLanguage.DE -> de
    AppLanguage.RO -> ro
}

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
 * - Thin top bar: TrainPilot dumbbell logo + app name + Language selector icon + Theme selector icon + Settings icon
 * - Single row of the 5 primary navigation buttons at the top: Kezdőlap, Edzés, Napló, Fejlődés, Egészség
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
            // 1. Vékony sáv: App logó + TrainPilot név + Nyelvválasztó + Témaválasztó + Beállítások ikon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectTab(MainTab.HOME) }
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                        .testTag("top_bar_brand_home")
                ) {
                    TrainPilotMountainLogo(
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "TrainPilot",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = palette.primary,
                        maxLines = 1
                    )
                }

                // Right icons: Naptár (Calendar), Coach, Settings Gear
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val isCalendarActive = (currentTab == MainTab.CALENDAR || currentSubScreen == SubScreen.CALENDAR_PLANNER) && currentSubScreen != SubScreen.SETTINGS_MAIN
                    val isCoachActive = (currentTab == MainTab.COACH || currentSubScreen == SubScreen.COACH_DETAIL) && currentSubScreen != SubScreen.SETTINGS_MAIN
                    val isSettingsActive = currentSubScreen == SubScreen.SETTINGS_MAIN

                    // 1. Naptár (Calendar) Quick Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCalendarActive) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTab(MainTab.CALENDAR) }
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

                    // 2. Coach Quick Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCoachActive) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTab(MainTab.COACH) }
                            .testTag("top_bar_coach_button")
                            .trainPilotBorder(1.dp, if (isCoachActive) palette.primary else palette.cardBorder, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = strings.navCoach,
                                tint = if (isCoachActive) palette.onPrimary else palette.textPrimary,
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
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenSettings() }
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
                    selected = currentTab == MainTab.WORKOUT && currentSubScreen == SubScreen.NONE,
                    onClick = { onSelectTab(MainTab.WORKOUT) },
                    modifier = itemModifier,
                    testTag = "top_nav_workout"
                )
                TopNavPillItem(
                    label = strings.navProgress,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    selected = (currentTab == MainTab.PROGRESS || currentTab == MainTab.LOG) &&
                        currentSubScreen in setOf(SubScreen.NONE, SubScreen.LOG_WORKOUT_DETAIL, SubScreen.LOG_ROUTE_MAP),
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
private fun TopNavPillItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val palette = LocalTrainPilotPalette.current
    val glowModifier = if (selected && palette.hasGlow) {
        Modifier.drawBehind {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(palette.primary.copy(alpha = 0.42f), Color.Transparent),
                    radius = size.maxDimension * 0.85f
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
        }
    } else Modifier

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
                vertical = 5.dp,
                horizontal = 2.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) palette.primary else palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 10.sp
                ),
                color = if (selected) palette.primary else palette.textPrimary,
                maxLines = 2,
                textAlign = TextAlign.Center,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TrainPilotRedCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    testTag: String = ""
) {
    val strings = LocalTrainPilotStrings.current
    val palette = LocalTrainPilotPalette.current
    val interaction = remember { MutableInteractionSource() }
    val iconColor = com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeForeground
    // The painted control shrinks ~10%; its independent hit target remains 48 dp.
    Box(modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
        .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(11.dp), color = com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeBackground,
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
    rightExtraContent: (@Composable () -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTab(tab) }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PalettePickerBottomSheet(
    selectedPaletteId: Int,
    selectedCategory: PaletteCategory?,
    currentLanguage: AppLanguage,
    fontScaleMultiplier: Float,
    compactPhoneMode: Boolean,
    onSelectPalette: (Int) -> Unit,
    onSelectCategory: (PaletteCategory?) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectFontScale: (Float) -> Unit,
    onToggleCompactMode: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filteredPalettes = remember(selectedCategory) {
        when (selectedCategory) {
            null, PaletteCategory.ALL -> TrainPilotPalettes.allPalettes
            PaletteCategory.MATTE, PaletteCategory.CONTRAST, PaletteCategory.GLOW, PaletteCategory.LIGHT ->
                TrainPilotPalettes.allPalettes.filter { it.subGroup == selectedCategory }
            else -> TrainPilotPalettes.allPalettes.filter { it.category == selectedCategory }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = palette.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = strings.colorPalettes32Title,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = strings.fontSizePreviewLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                TrainPilotRedCloseButton(
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Language & Accessibility Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.entries.forEach { lang ->
                    val active = currentLanguage == lang
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable { onSelectLanguage(lang) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${lang.flag} ${lang.code}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) palette.onPrimary else palette.textPrimary
                            )
                        }
                    }
                }

                listOf(
                    1.0f to "100%",
                    1.15f to "115%",
                    1.28f to "130%",
                    2.0f to "200% (2.0x)"
                ).forEach { (scale, label) ->
                    val active = kotlin.math.abs(fontScaleMultiplier - scale) < 0.03f
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) palette.tintedSurface(0.22f) else palette.surfaceElevated,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .trainPilotBorder(
                                width = 1.dp,
                                color = if (active) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectFontScale(scale) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) palette.primary else palette.textSecondary
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (compactPhoneMode) palette.tintedSurface(0.22f) else palette.surfaceElevated,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .trainPilotBorder(
                            width = 1.dp,
                            color = if (compactPhoneMode) palette.primary else palette.cardBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onToggleCompactMode(!compactPhoneMode) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (compactPhoneMode) "📱 320dp: ON" else "📱 320dp: OFF",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (compactPhoneMode) palette.primary else palette.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter: Összes (32) | Matt (8) | Kontrasztos (8) | Fényes/Glow (8) | Világos (8)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = listOf(
                    null to "${strings.viewAllAction} (32)",
                    PaletteCategory.MATTE to tr(currentLanguage, "🌑 Matt (8)", "🌑 Matte (8)", "🌑 Matt (8)", "🌑 Mat (8)"),
                    PaletteCategory.CONTRAST to tr(currentLanguage, "⚡ Kontrasztos (8)", "⚡ Contrast (8)", "⚡ Kontrast (8)", "⚡ Contrast (8)"),
                    PaletteCategory.GLOW to tr(currentLanguage, "✨ Fényes / Glow (8)", "✨ Glow (8)", "✨ Glow (8)", "✨ Glow (8)"),
                    PaletteCategory.LIGHT to tr(currentLanguage, "☀️ Világos (8)", "☀️ Light (8)", "☀️ Hell (8)", "☀️ Luminos (8)")
                )
                cats.forEach { (cat, label) ->
                    val active = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (active) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .trainPilotBorder(
                                width = 1.dp,
                                color = if (active) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelectCategory(cat) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) palette.onPrimary else palette.textPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPalettes, key = { it.id }) { item ->
                    PaletteRowCard(
                        item = item,
                        isSelected = item.id == selectedPaletteId,
                        langCode = currentLanguage.code,
                        onClick = { onSelectPalette(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteRowCard(
    item: TrainPilotPalette,
    isSelected: Boolean,
    langCode: String,
    onClick: () -> Unit
) {
    val lang = AppLanguage.entries.firstOrNull { it.code.equals(langCode, ignoreCase = true) } ?: AppLanguage.HU
    val familyBadge = when (item.subGroup) {
        PaletteCategory.MATTE -> tr(lang, "Matt sötét felület • Bársonyos színek", "Matte dark surface • Velvety tones", "Matte dunkle Oberfläche • Samtige Farben", "Suprafață închisă mată • Tonuri catifelate")
        PaletteCategory.CONTRAST -> tr(lang, "Magas kontraszt • Mélyfekete & éles keret", "High contrast • Deep black & sharp border", "Hoher Kontrast • Tiefschwarz & scharfer Rahmen", "Contrast ridicat • Negru intens și chenar clar")
        PaletteCategory.GLOW -> tr(lang, "✨ Fényes / Neon Glow effekt", "✨ Bright / Neon Glow effect", "✨ Leuchtender / Neon-Glow-Effekt", "✨ Efect luminos / Neon Glow")
        PaletteCategory.LIGHT -> tr(lang, "☀️ Világos háttér • Színes keretek", "☀️ Light background • Colored borders", "☀️ Heller Hintergrund • Farbige Rahmen", "☀️ Fundal luminos • Chenare colorate")
        else -> tr(lang, "Prémium téma", "Premium theme", "Premium-Thema", "Temă premium")
    }
    val glowMod = if (item.hasGlow) {
        Modifier.drawBehind {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(item.primary.copy(alpha = 0.25f), item.secondary.copy(alpha = 0.18f))
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
        }
    } else Modifier

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = item.surface,
        modifier = Modifier
            .fillMaxWidth()
            .then(glowMod)
            .heightIn(min = 48.dp)
            .border(
                width = if (isSelected || item.hasGlow) 2.dp else 1.dp,
                color = if (isSelected) item.primary else if (item.hasGlow) item.primary.copy(alpha = 0.7f) else item.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("palette_option_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(item.background)
                            .border(1.5.dp, item.cardBorder, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(item.primary)
                            .border(1.dp, item.background, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(item.tertiary)
                            .border(1.dp, item.background, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.localizedName(langCode),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = item.textPrimary
                    )
                    Text(
                        text = familyBadge,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.hasGlow) item.primary else item.textSecondary
                    )
                }
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = item.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = item.onPrimary,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GoalsEditDialog(
    currentWorkouts: Int,
    currentPeriod: com.example.model.WorkoutGoalPeriod = com.example.model.WorkoutGoalPeriod.WEEKLY,
    currentStepsGoal: Int,
    onSave: (Int, com.example.model.WorkoutGoalPeriod, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    var period by remember { mutableStateOf(currentPeriod) }
    val minWorkouts = 1
    val maxWorkouts = if (period == com.example.model.WorkoutGoalPeriod.WEEKLY) 14 else 60
    var workouts by remember(period) {
        val initial = if (period == currentPeriod) currentWorkouts
        else if (period == com.example.model.WorkoutGoalPeriod.WEEKLY) (currentWorkouts / 4).coerceIn(1, 14)
        else (currentWorkouts * 4).coerceIn(1, 60)
        mutableIntStateOf(initial.coerceIn(minWorkouts, maxWorkouts))
    }
    var steps by remember { mutableIntStateOf(currentStepsGoal.coerceIn(2000, 30000)) }
    var workoutsInputText by remember(workouts) { mutableStateOf(workouts.toString()) }
    var stepsInputText by remember(steps) { mutableStateOf(steps.toString()) }

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
                    text = strings.goalsSetupTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TrainPilotRedCloseButton(onClick = onDismiss)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Period Selector (Heti vs Havi cél)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isWeekly = period == com.example.model.WorkoutGoalPeriod.WEEKLY
                    val isMonthly = period == com.example.model.WorkoutGoalPeriod.MONTHLY
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isWeekly) palette.primary else palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .trainPilotBorder(1.dp, if (isWeekly) palette.primary else palette.cardBorder, RoundedCornerShape(12.dp))
                            .clickable { period = com.example.model.WorkoutGoalPeriod.WEEKLY }
                            .testTag("goal_period_weekly_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)) {
                            Text(
                                text = when (strings.language) {
                                    com.example.model.AppLanguage.HU -> "Heti cél (1–14)"
                                    com.example.model.AppLanguage.EN -> "Weekly (1–14)"
                                    com.example.model.AppLanguage.DE -> "Wöchentlich (1–14)"
                                    com.example.model.AppLanguage.RO -> "Săptămânal (1–14)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isWeekly) FontWeight.Bold else FontWeight.Medium,
                                color = if (isWeekly) palette.onPrimary else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMonthly) palette.primary else palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .trainPilotBorder(1.dp, if (isMonthly) palette.primary else palette.cardBorder, RoundedCornerShape(12.dp))
                            .clickable { period = com.example.model.WorkoutGoalPeriod.MONTHLY }
                            .testTag("goal_period_monthly_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)) {
                            Text(
                                text = when (strings.language) {
                                    com.example.model.AppLanguage.HU -> "Havi cél (1–60)"
                                    com.example.model.AppLanguage.EN -> "Monthly (1–60)"
                                    com.example.model.AppLanguage.DE -> "Monatlich (1–60)"
                                    com.example.model.AppLanguage.RO -> "Lunar (1–60)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isMonthly) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMonthly) palette.onPrimary else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2. Dynamic summary card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.primary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.periodStepsFormat(workouts, period == com.example.model.WorkoutGoalPeriod.MONTHLY, steps),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                        Text(
                            text = when (strings.language) {
                                com.example.model.AppLanguage.HU -> "Heti ~${steps * 7} lépés ekvivalens • ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "$workouts edzés/hónap" else "$workouts edzés/hét"}"
                                com.example.model.AppLanguage.EN -> "Weekly ~${steps * 7} steps equivalent • ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "$workouts workouts/month" else "$workouts workouts/week"}"
                                com.example.model.AppLanguage.DE -> "Wöchentlich ~${steps * 7} Schritte • ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "$workouts Trainings/Monat" else "$workouts Trainings/Woche"}"
                                com.example.model.AppLanguage.RO -> "Echivalent ~${steps * 7} pași/săpt. • ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "$workouts antrenamente/lună" else "$workouts antrenamente/săpt."}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                // 3. Workouts target slider & numeric input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val countLabel = if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) {
                            when (strings.language) {
                                com.example.model.AppLanguage.HU -> "Havi edzésszám: $workouts edzés/hónap"
                                com.example.model.AppLanguage.EN -> "Monthly workouts: $workouts/mo"
                                com.example.model.AppLanguage.DE -> "Monatliche Trainings: $workouts/Monat"
                                com.example.model.AppLanguage.RO -> "Antrenamente lunare: $workouts/lună"
                            }
                        } else {
                            when (strings.language) {
                                com.example.model.AppLanguage.HU -> "Heti edzésszám: $workouts edzés/hét"
                                com.example.model.AppLanguage.EN -> "Weekly workouts: $workouts/wk"
                                com.example.model.AppLanguage.DE -> "Wöchentliche Trainings: $workouts/Woche"
                                com.example.model.AppLanguage.RO -> "Antrenamente săpt.: $workouts/săpt."
                            }
                        }
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = workoutsInputText,
                            onValueChange = { input ->
                                workoutsInputText = input.filter { it.isDigit() }.take(2)
                                input.toIntOrNull()?.let { num ->
                                    workouts = num.coerceIn(minWorkouts, maxWorkouts)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.width(68.dp).heightIn(min = 44.dp).testTag("workout_count_numeric_input"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                    Slider(
                        value = workouts.toFloat(),
                        onValueChange = {
                            workouts = it.toInt()
                            workoutsInputText = workouts.toString()
                        },
                        valueRange = minWorkouts.toFloat()..maxWorkouts.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary
                        ),
                        modifier = Modifier.testTag("workout_goal_slider")
                    )
                }

                // 4. Daily steps slider & numeric input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stepsLabel = when (strings.language) {
                            com.example.model.AppLanguage.HU -> "Napi lépéscél: $steps lépés/nap"
                            com.example.model.AppLanguage.EN -> "Daily steps: $steps steps/day"
                            com.example.model.AppLanguage.DE -> "Tägliche Schritte: $steps/Tag"
                            com.example.model.AppLanguage.RO -> "Pași zilnici: $steps pași/zi"
                        }
                        Text(
                            text = stepsLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = stepsInputText,
                            onValueChange = { input ->
                                stepsInputText = input.filter { it.isDigit() }.take(5)
                                input.toIntOrNull()?.let { num ->
                                    steps = num.coerceIn(2000, 30000)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.width(88.dp).heightIn(min = 44.dp).testTag("steps_count_numeric_input"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                    Slider(
                        value = steps.toFloat(),
                        onValueChange = {
                            steps = (it.toInt() / 500) * 500
                            stepsInputText = steps.toString()
                        },
                        valueRange = 2000f..30000f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.stepsAccent,
                            activeTrackColor = palette.stepsAccent
                        ),
                        modifier = Modifier.testTag("steps_goal_slider")
                    )
                }
            }
        },
        confirmButton = {
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = { onSave(workouts, period, steps) },
                modifier = Modifier.heightIn(min = 48.dp)) {
                Text(strings.saveBtn, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TrainPilotOutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(strings.cancelBtn, color = palette.textPrimary)
            }
        }
    )
}

@Composable
fun GoalsEditDialog(
    currentWeeklyWorkouts: Int,
    currentStepsGoal: Int,
    onSave: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    GoalsEditDialog(
        currentWorkouts = currentWeeklyWorkouts,
        currentPeriod = com.example.model.WorkoutGoalPeriod.WEEKLY,
        currentStepsGoal = currentStepsGoal,
        onSave = { workouts, _, steps -> onSave(workouts, steps) },
        onDismiss = onDismiss
    )
}

@Composable
fun PersonalPlannerModalDialog(
    initialHeightCm: Int = 179,
    initialWeightKg: Float = 65f,
    initialGoalHu: String = "Izomépítés",
    initialExperienceHu: String = "Kezdő",
    initialLocationHu: String = "Otthon",
    initialEquipmentHu: String = "Kézisúlyzó és pad",
    initialDaysPerWeek: Int = 3,
    initialDurationMin: Int = 45,
    initialExcludedMuscles: Set<String> = emptySet(),
    initialExcludedExercises: Set<String> = emptySet(),
    onSaveAndOpenPlan: (
        heightCm: Int,
        weightKg: Float,
        goalHu: String,
        experienceHu: String,
        locationHu: String,
        equipmentHu: String,
        daysPerWeek: Int,
        durationMin: Int,
        excludedMuscles: Set<String>,
        excludedExercises: Set<String>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val lang = strings.language
    fun option(value: String) = com.example.localization.StudioProfileOptionLabels.label(value, lang)
    fun detail(value: String) = com.example.localization.StudioProfileOptionLabels.description(value, lang)
    var heightCm by remember { mutableIntStateOf(initialHeightCm) }
    var weightKg by remember { mutableFloatStateOf(initialWeightKg) }
    var selectedGoal by remember { mutableStateOf(initialGoalHu) }
    var selectedExperience by remember { mutableStateOf(initialExperienceHu) }
    var selectedLocation by remember { mutableStateOf(initialLocationHu) }
    val equipmentCatalog = listOf(
        "Saját testsúly" to "Alap saját testsúlyos gyakorlatok (fekvőtámasz, plank, guggolás)",
        "Egykezes kézisúlyzó (1 db)" to "1 db kézisúlyzós gyakorlatok (pl. goblet guggolás, egykezes evezés)",
        "Kézisúlyzó pár (2 db)" to "Páros egykezes súlyzós gyakorlatok (nyomások, kitörések, kar)",
        "Kétkezes rúd (Súlyzórúd)" to "Kétkezes rudas alapgyakorlatok (guggolás, felhúzás, evezés, nyomás)",
        "Pad / Szék / Kanapé" to "Támasz pl. bolgár guggoláshoz, fekvenyomáshoz vagy csípőemeléshez",
        "Stabil támaszték / Fal / Szék" to "Kéztámasz pl. egykezes evezéshez vagy vádliemeléshez",
        "Fellépő / Doboz / Lépcső" to "Magasítás fellépéshez (step-up) és felugráshoz",
        "Húzódzkodó rúd" to "Húzódzkodás, függeszkedés és lábemelések",
        "Tolódzkodó korlát" to "Tolódzkodás mellre és tricepszre",
        "Gumiszalag / Ellenállás-szalag" to "Gumiszalagos evezések, lehúzások és izolációk",
        "Kettlebell" to "Kettlebell swing, goblet guggolás és nyomás",
        "Edzőtermi gépek / Csigás gépek" to "Lábtoló, hátlehúzó, csigás és vezetett gépek"
    )
    val equipments = equipmentCatalog.map { it.first }

    fun parseInitialEquipments(raw: String): Set<String> {
        val tokens = raw.split(',').map { it.trim() }.filter { it.isNotBlank() }
        val result = linkedSetOf<String>()
        for (token in tokens) {
            val lower = token.lowercase()
            when {
                token in equipments -> result.add(token)
                lower == "csak saját testsúly" || lower.contains("saját testsúly") || lower == "bodyweight" -> {
                    result.add("Saját testsúly")
                }
                lower == "kézisúlyzó és pad" -> {
                    result.add("Saját testsúly")
                    result.add("Egykezes kézisúlyzó (1 db)")
                    result.add("Kézisúlyzó pár (2 db)")
                    result.add("Pad / Szék / Kanapé")
                    result.add("Stabil támaszték / Fal / Szék")
                }
                lower == "kézisúlyzó (pad nélkül)" -> {
                    result.add("Saját testsúly")
                    result.add("Egykezes kézisúlyzó (1 db)")
                    result.add("Kézisúlyzó pár (2 db)")
                }
                lower == "teljes edzőtermi géppark" || lower == "machines" -> {
                    result.addAll(equipments)
                }
                lower == "gumiszalag és rúd" -> {
                    result.add("Saját testsúly")
                    result.add("Gumiszalag / Ellenállás-szalag")
                    result.add("Húzódzkodó rúd")
                    result.add("Kétkezes rúd (Súlyzórúd)")
                }
                lower == "kettlebell és saját testsúly" || lower == "kettlebell" -> {
                    result.add("Saját testsúly")
                    result.add("Kettlebell")
                }
                lower == "dumbbell" || lower.contains("1 kézisúlyzó") || lower.contains("egykezes") -> {
                    result.add("Egykezes kézisúlyzó (1 db)")
                }
                lower == "dumbbells" || lower.contains("kézisúlyzó") -> {
                    result.add("Egykezes kézisúlyzó (1 db)")
                    result.add("Kézisúlyzó pár (2 db)")
                }
                lower == "barbell" || lower.contains("kétkezes") || lower.contains("súlyzórúd") -> {
                    result.add("Kétkezes rúd (Súlyzórúd)")
                }
                lower == "bench" || lower.contains("pad") || lower.contains("kanapé") -> {
                    result.add("Pad / Szék / Kanapé")
                }
                lower == "support" || lower.contains("támasz") || lower.contains("szék") -> {
                    result.add("Stabil támaszték / Fal / Szék")
                    result.add("Pad / Szék / Kanapé")
                }
                lower == "step" || lower.contains("fellépő") || lower.contains("lépcső") -> {
                    result.add("Fellépő / Doboz / Lépcső")
                }
                lower == "pullupbar" || lower.contains("húzódzkodó") -> {
                    result.add("Húzódzkodó rúd")
                }
                lower == "dipbars" || lower.contains("tolódzkodó") -> {
                    result.add("Tolódzkodó korlát")
                }
                lower == "bands" || lower.contains("gumiszalag") -> {
                    result.add("Gumiszalag / Ellenállás-szalag")
                }
                else -> result.add(token)
            }
        }
        if (result.isEmpty()) {
            result.add("Saját testsúly")
            result.add("Egykezes kézisúlyzó (1 db)")
            result.add("Kézisúlyzó pár (2 db)")
            result.add("Pad / Szék / Kanapé")
        }
        return result
    }

    var selectedEquipments by remember(initialEquipmentHu) {
        mutableStateOf(parseInitialEquipments(initialEquipmentHu))
    }
    val selectedEquipment = remember(selectedEquipments) {
        selectedEquipments.joinToString(", ").ifBlank { "Saját testsúly" }
    }
    var selectedDays by remember { mutableIntStateOf(initialDaysPerWeek) }
    var selectedDuration by remember { mutableIntStateOf(initialDurationMin) }
    var excludedMuscles by remember { mutableStateOf(initialExcludedMuscles) }
    var excludedExercises by remember { mutableStateOf(initialExcludedExercises) }

    // Individual floating dropdown state (opens on top of other fields without pushing them down)
    var openDropdownKey by remember { mutableStateOf<String?>(null) }

    val goals = listOf(
        "Izomépítés",
        "Fogyás / Szálkásítás",
        "Erőnövelés",
        "Általános fittség",
        "Állóképesség és tónus"
    )
    val experiences = listOf("Kezdő", "Újrakezdő", "Középhaladó", "Haladó")
    val locations = listOf(
        "Otthon",
        "Edzőterem",
        "Vegyes (Otthon + Terem)",
        "Szabadtér / Street workout"
    )
    val daysOptions = listOf(2, 3, 4, 5, 6)
    val durationOptions = listOf(30, 45, 60, 75, 90)

    val heightPresets = (150..205).toList()
    val weightPresets = (45..130).map { it.toFloat() }

    val muscleOptions = listOf("Láb", "Mell", "Hát", "Váll", "Kar", "Törzs", "Alsó hát", "Térdkímélő")
    val exerciseOptions = listOf(
        "Bolgár guggolás" to "Láb • Kézisúlyzó",
        "Egykezes evezés" to "Hát • Kézisúlyzó",
        "Fekvenyomás" to "Mell • Rúd",
        "Román felhúzás" to "Láb • Rúd",
        "Húzódzkodás" to "Hát • Saját testsúly",
        "Tolódzkodás" to "Mell • Saját testsúly",
        "Kitörés sétálva" to "Láb • Kézisúlyzó",
        "Vállból nyomás" to "Váll • Kézisúlyzó"
    )

    @Composable
    fun PlannerDropdownBox(
        dropdownKey: String,
        label: String,
        selectedValue: String,
        options: List<String>,
        onSelect: (Int, String) -> Unit,
        modifier: Modifier = Modifier,
        testTag: String = ""
    ) {
        val isOpen = openDropdownKey == dropdownKey
        Box(modifier = modifier) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(
                        1.dp,
                        if (isOpen) palette.primary else palette.cardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        openDropdownKey = if (isOpen) null else dropdownKey
                    }
                    .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedValue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = isOpen,
                onDismissRequest = { openDropdownKey = null },
                containerColor = palette.background,
                shape = RoundedCornerShape(12.dp),
                border = TrainPilotFrames.stroke(1.dp, palette.primary.copy(alpha = 0.7f)),
                modifier = Modifier
                    .widthIn(min = 210.dp, max = 290.dp)
                    .heightIn(max = 260.dp)
            ) {
                options.forEachIndexed { index, optionText ->
                    val isSelected = optionText == selectedValue
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) palette.primary else palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelect(index, optionText)
                            openDropdownKey = null
                        },
                        modifier = Modifier.background(
                            if (isSelected) palette.primary.copy(alpha = 0.16f)
                            else Color.Transparent
                        )
                    )
                }
            }
        }
    }

    TrainPilotFullHeightFormSheet(
        onDismiss = onDismiss,
        title = tr(lang, "Profil és új terv", "Profile and new plan", "Profil und neuer Plan", "Profil și plan nou"),
        closeTestTag = "planner_skip_btn",
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top row: Magasság (cm) & Testsúly (kg) — each opens its own floating dropdown list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    PlannerDropdownBox(
                        dropdownKey = "height",
                        label = tr(lang, "Magasság (cm)", "Height (cm)", "Größe (cm)", "Înălțime (cm)"),
                        selectedValue = "$heightCm cm",
                        options = heightPresets.map { "$it cm" },
                        onSelect = { idx, _ -> heightCm = heightPresets[idx] },
                        modifier = Modifier.weight(1f),
                        testTag = "profile_height_box"
                    )

                    PlannerDropdownBox(
                        dropdownKey = "weight",
                        label = tr(lang, "Testsúly (kg)", "Body weight (kg)", "Körpergewicht (kg)", "Greutate (kg)"),
                        selectedValue = "${weightKg.toInt()} kg",
                        options = weightPresets.map { "${it.toInt()} kg" },
                        onSelect = { idx, _ -> weightKg = weightPresets[idx] },
                        modifier = Modifier.weight(1f),
                        testTag = "profile_weight_box"
                    )
                }

                // 1. Cél és edzettség
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Column {
                                Text(tr(lang, "Cél és edzettség", "Goal & fitness level", "Ziel & Fitnesslevel", "Obiectiv și nivel de fitness"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("${option(selectedGoal)} • ${option(selectedExperience)} • ${option(selectedLocation)}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            }
                        }

                        PlannerDropdownBox(
                            dropdownKey = "goal",
                            label = tr(lang, "Elsődleges cél", "Primary goal", "Primäres Ziel", "Obiectiv principal"),
                            selectedValue = option(selectedGoal),
                            options = goals.map(::option),
                            onSelect = { index, _ -> selectedGoal = goals[index] },
                            testTag = "profile_goal_box"
                        )

                        PlannerDropdownBox(
                            dropdownKey = "experience",
                            label = tr(lang, "Tapasztalati szint", "Experience level", "Erfahrungsstufe", "Nivel de experiență"),
                            selectedValue = option(selectedExperience),
                            options = experiences.map(::option),
                            onSelect = { index, _ -> selectedExperience = experiences[index] },
                            testTag = "profile_experience_box"
                        )

                        PlannerDropdownBox(
                            dropdownKey = "location",
                            label = tr(lang, "Edzés helyszíne", "Workout location", "Trainingsort", "Locație antrenament"),
                            selectedValue = option(selectedLocation),
                            options = locations.map(::option),
                            onSelect = { index, _ -> selectedLocation = locations[index] },
                            testTag = "profile_location_box"
                        )
                    }
                }

                // 2. Felszerelés (Több opció egyidejű kijelölése)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🏋️", fontSize = 16.sp)
                            Column {
                                Text(tr(lang, "Felszerelés (több is választható)", "Equipment (multi-select)", "Ausrüstung (Mehrfachauswahl)", "Echipament (selecție multiplă)"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text(
                                    text = tr(
                                        lang,
                                        "${selectedEquipments.size} eszköz kiválasztva • A terv ezek alapján épül fel",
                                        "${selectedEquipments.size} items selected • Plan is built from these",
                                        "${selectedEquipments.size} Geräte ausgewählt • Plan basiert darauf",
                                        "${selectedEquipments.size} echipamente selectate • Planul se bazează pe acestea"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        val isEquipmentOpen = openDropdownKey == "equipment"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .trainPilotBorder(
                                        1.dp,
                                        if (isEquipmentOpen) palette.primary else palette.cardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        openDropdownKey = if (isEquipmentOpen) null else "equipment"
                                    }
                                    .testTag("profile_equipment_box")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr(lang, "Elérhető eszközök (koppints a listához)", "Available equipment (tap to open list)", "Verfügbare Geräte (tippen zum Öffnen)", "Echipamente disponibile (atinge pentru listă)"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                        Icon(
                                            imageVector = if (isEquipmentOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = selectedEquipments.joinToString(", ") { option(it) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isEquipmentOpen,
                                onDismissRequest = { openDropdownKey = null },
                                containerColor = palette.background,
                                shape = RoundedCornerShape(12.dp),
                                border = TrainPilotFrames.stroke(1.dp, palette.primary.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .widthIn(min = 250.dp, max = 320.dp)
                                    .heightIn(max = 320.dp)
                            ) {
                                equipmentCatalog.forEachIndexed { idx, (eqName, eqDesc) ->
                                    val isChecked = eqName in selectedEquipments
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .trainPilotBorder(
                                                            1.5.dp,
                                                            if (isChecked) palette.primary else palette.textSecondary,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .background(if (isChecked) palette.primary else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isChecked) {
                                                        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.onPrimary)
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = option(eqName),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isChecked) palette.primary else palette.textPrimary
                                                    )
                                                    Text(
                                                        text = option(eqDesc),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.textSecondary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            val updated = if (isChecked) {
                                                if (selectedEquipments.size > 1) selectedEquipments - eqName else selectedEquipments
                                            } else {
                                                selectedEquipments + eqName
                                            }
                                            selectedEquipments = updated
                                        },
                                        modifier = Modifier
                                            .background(if (isChecked) palette.primary.copy(alpha = 0.14f) else Color.Transparent)
                                            .testTag("profile_equipment_option_$idx")
                                    )
                                }
                            }
                        }


                    }
                }

                // 3. Edzésbeosztás
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Column {
                                Text(tr(lang, "Edzésbeosztás", "Workout schedule", "Trainingsplan", "Program de antrenament"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text(
                                    text = tr(
                                        lang,
                                        "Heti $selectedDays nap • $selectedDuration perc/edzés",
                                        "$selectedDays days/week • $selectedDuration min/workout",
                                        "$selectedDays Tage/Woche • $selectedDuration Min./Training",
                                        "$selectedDays zile/săptămână • $selectedDuration min/antrenament"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlannerDropdownBox(
                                dropdownKey = "days_per_week",
                                label = tr(lang, "Heti edzésnapok", "Weekly workout days", "Trainingstage pro Woche", "Zile de antrenament pe săptămână"),
                                selectedValue = tr(lang, "Heti $selectedDays nap", "$selectedDays days/week", "$selectedDays Tage/Woche", "$selectedDays zile/săpt."),
                                options = daysOptions.map { tr(lang, "Heti $it nap", "$it days/week", "$it Tage/Woche", "$it zile/săpt.") },
                                onSelect = { idx, _ -> selectedDays = daysOptions[idx] },
                                modifier = Modifier.weight(1f),
                                testTag = "profile_days_box"
                            )

                            PlannerDropdownBox(
                                dropdownKey = "workout_duration",
                                label = tr(lang, "Időtartam", "Duration", "Dauer", "Durată"),
                                selectedValue = tr(lang, "$selectedDuration perc", "$selectedDuration min", "$selectedDuration Min.", "$selectedDuration min"),
                                options = durationOptions.map { tr(lang, "$it perc", "$it min", "$it Min.", "$it min") },
                                onSelect = { idx, _ -> selectedDuration = durationOptions[idx] },
                                modifier = Modifier.weight(1f),
                                testTag = "profile_duration_box"
                            )
                        }
                    }
                }

                // 4. Korlátozások (Piros kiemeléssel, fölé nyíló legördülő menükkel)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚙️", fontSize = 16.sp)
                            Column {
                                Text(tr(lang, "Korlátozások", "Restrictions", "Einschränkungen", "Restricții"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                                val totalExcluded = excludedMuscles.size + excludedExercises.size
                                Text(
                                    text = if (totalExcluded == 0) {
                                        tr(lang, "Nincs kizárás megadva", "No exclusions set", "Keine Ausschlüsse festgelegt", "Nicio excludere setată")
                                    } else {
                                        tr(lang, "$totalExcluded kizárás kiválasztva", "$totalExcluded exclusions selected", "$totalExcluded Ausschlüsse ausgewählt", "$totalExcluded excluderi selectate")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        // 4.1 Kerülendő területek floating dropdown box
                        val isMusclesOpen = openDropdownKey == "excluded_muscles"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isMusclesOpen) Color(0xFFFF6E6E) else Color(0xFF7A1B28),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        openDropdownKey = if (isMusclesOpen) null else "excluded_muscles"
                                    }
                                    .testTag("profile_excluded_muscles_box")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr(lang, "Kerülendő területek", "Areas to avoid", "Zu vermeidende Bereiche", "Zone de evitat"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF6E6E)
                                        )
                                        Icon(
                                            imageVector = if (isMusclesOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = Color(0xFFFF6E6E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = if (excludedMuscles.isEmpty()) {
                                            tr(lang, "Nincs kizárt terület (koppints a választáshoz)", "No excluded areas (tap to select)", "Keine ausgeschlossenen Bereiche (tippen zur Auswahl)", "Nicio zonă exclusă (atinge pentru a selecta)")
                                        } else {
                                            tr(lang, "Kizárva: ${excludedMuscles.joinToString(", ") { option(it) }}", "Excluded: ${excludedMuscles.joinToString(", ") { option(it) }}", "Ausgeschlossen: ${excludedMuscles.joinToString(", ") { option(it) }}", "Excluse: ${excludedMuscles.joinToString(", ") { option(it) }}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (excludedMuscles.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                        color = if (excludedMuscles.isEmpty()) palette.textSecondary else palette.primary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isMusclesOpen,
                                onDismissRequest = { openDropdownKey = null },
                                containerColor = palette.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6E6E)),
                                modifier = Modifier
                                    .widthIn(min = 240.dp, max = 300.dp)
                                    .heightIn(max = 280.dp)
                            ) {
                                muscleOptions.forEach { muscle ->
                                    val isChecked = muscle in excludedMuscles
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .trainPilotBorder(
                                                            1.5.dp,
                                                            if (isChecked) palette.primary else palette.textSecondary,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .background(if (isChecked) palette.primary else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isChecked) {
                                                        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.onPrimary)
                                                    }
                                                }
                                                Text(
                                                    text = option(muscle),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = palette.textPrimary
                                                )
                                            }
                                        },
                                        onClick = {
                                            excludedMuscles = if (isChecked) excludedMuscles - muscle else excludedMuscles + muscle
                                        }
                                    )
                                }
                            }
                        }

                        // 4.2 Egyedi gyakorlatkizárás floating dropdown box
                        val isExercisesOpen = openDropdownKey == "excluded_exercises"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isExercisesOpen) Color(0xFFFF6E6E) else Color(0xFF7A1B28),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        openDropdownKey = if (isExercisesOpen) null else "excluded_exercises"
                                    }
                                    .testTag("profile_excluded_exercises_box")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tr(lang, "Egyedi gyakorlatkizárás", "Custom exercise exclusions", "Eigene Übungsausschlüsse", "Excluderi individuale de exerciții"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF6E6E)
                                        )
                                        Icon(
                                            imageVector = if (isExercisesOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = Color(0xFFFF6E6E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = if (excludedExercises.isEmpty()) {
                                            tr(lang, "Nincs kizárt gyakorlat (koppints a választáshoz)", "No excluded exercises (tap to select)", "Keine ausgeschlossenen Übungen (tippen zur Auswahl)", "Niciun exercițiu exclus (atinge pentru a selecta)")
                                        } else {
                                            tr(lang, "Kizárva: ${excludedExercises.joinToString(", ") { option(it) }}", "Excluded: ${excludedExercises.joinToString(", ") { option(it) }}", "Ausgeschlossen: ${excludedExercises.joinToString(", ") { option(it) }}", "Excluse: ${excludedExercises.joinToString(", ") { option(it) }}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (excludedExercises.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                        color = if (excludedExercises.isEmpty()) palette.textSecondary else palette.primary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isExercisesOpen,
                                onDismissRequest = { openDropdownKey = null },
                                containerColor = palette.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6E6E)),
                                modifier = Modifier
                                    .widthIn(min = 250.dp, max = 310.dp)
                                    .heightIn(max = 280.dp)
                            ) {
                                exerciseOptions.forEach { (exName, exSub) ->
                                    val isChecked = exName in excludedExercises
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .trainPilotBorder(
                                                            1.5.dp,
                                                            if (isChecked) palette.primary else palette.textSecondary,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .background(if (isChecked) palette.primary else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isChecked) {
                                                        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.onPrimary)
                                                    }
                                                }
                                                Column {
                                                    Text(
                                                        text = option(exName),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = palette.textPrimary
                                                    )
                                                    Text(
                                                        text = detail(exSub),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = palette.textSecondary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            excludedExercises = if (isChecked) excludedExercises - exName else excludedExercises + exName
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = {
                    onSaveAndOpenPlan(
                        heightCm,
                        weightKg,
                        selectedGoal,
                        selectedExperience,
                        selectedLocation,
                        selectedEquipment,
                        selectedDays,
                        selectedDuration,
                        excludedMuscles,
                        excludedExercises
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    tr(
                        lang,
                        "Terv összeállítása és mentése",
                        "Build and save plan",
                        "Plan erstellen und speichern",
                        "Construiește și salvează planul"
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickWorkoutSheet(
    exercises: List<ExerciseItem> = emptyList(),
    queuedExercises: List<ExerciseItem> = emptyList(),
    onStartStrengthQuick: () -> Unit,
    onStartStrengthWithExercise: (ExerciseItem) -> Unit = { onStartStrengthQuick() },
    onStartTimedQuick: () -> Unit,
    onStartTimedWithExercise: (ExerciseItem) -> Unit = { onStartTimedQuick() },
    onStartQueuedWorkout: () -> Unit = { onStartStrengthQuick() },
    onStartOutdoorGps: () -> Unit,
    onBrowseExercises: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current

    val strengthList = remember(exercises) {
        exercises.filter { !it.isTimed && !it.isBilateral && it.measurementType != "distance" }.ifEmpty { exercises }
    }
    val timedList = remember(exercises) {
        exercises.filter { it.isTimed || it.isBilateral || it.measurementType == "time" }.distinctBy { it.id }
    }

    var selectedStrengthEx by remember(strengthList) {
        mutableStateOf(
            strengthList.firstOrNull { it.nameHu.contains("Fekvenyomás", ignoreCase = true) }
                ?: strengthList.firstOrNull()
        )
    }
    var strengthDropdownOpen by remember { mutableStateOf(false) }

    var selectedTimedEx by remember(timedList) {
        mutableStateOf(timedList.firstOrNull())
    }
    var timedDropdownOpen by remember { mutableStateOf(false) }

    // Start fully expanded, including when the selected/queued content is short.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.trainPilotSheetTopSafeArea().fillMaxHeight().testTag("quick_workout_sheet"),
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.quickWorkoutTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = tr(
                            strings.language,
                            "Válassz gyakorlatot a listából vagy nyisd meg a teljes Gyakorlatkönyvtárat!",
                            "Choose an exercise from the list or open the full Exercise Library!",
                            "Wähle eine Übung aus der Liste oder öffne die gesamte Übungsbibliothek!",
                            "Alege un exercițiu din listă sau deschide întreaga bibliotecă de exerciții!"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                TrainPilotRedCloseButton(onClick = onDismiss)
            }

            // If user added exercises from ExerciseLibraryScreen, show immediate launch card at the top!
            if (queuedExercises.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.primary.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.5.dp, palette.primary, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = tr(
                                strings.language,
                                "Kiválasztott edzésterv / gyakorlatok (${queuedExercises.size} db):",
                                "Selected workout / exercises (${queuedExercises.size}):",
                                "Ausgewählter Trainingsplan / Übungen (${queuedExercises.size}):",
                                "Antrenament / exerciții selectate (${queuedExercises.size}):"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                        Text(
                            text = queuedExercises.joinToString(" → ") { it.localizedName(strings.language) },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                        Button(
                            onClick = onStartQueuedWorkout,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                tr(
                                    strings.language,
                                    "▶ Kiválasztott gyakorlatok indítása most",
                                    "▶ Start selected exercises now",
                                    "▶ Ausgewählte Übungen jetzt starten",
                                    "▶ Pornește exercițiile selectate acum"
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 1. Browse Full Exercise Library (106+ exercises) — placed prominently first!
            QuickOptionRow(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = tr(
                    strings.language,
                    "${strings.exercisePickerTitle} (${exercises.size} gyakorlat)",
                    "${strings.exercisePickerTitle} (${exercises.size} exercises)",
                    "${strings.exercisePickerTitle} (${exercises.size} Übungen)",
                    "${strings.exercisePickerTitle} (${exercises.size} exerciții)"
                ),
                subtitle = tr(
                    strings.language,
                    "Bármely gyakorlat kiválasztása, hozzáadása és azonnali indítása",
                    "Pick, queue, and launch any exercise immediately",
                    "Beliebige Übung auswählen, hinzufügen und sofort starten",
                    "Selectează, adaugă și pornește imediat orice exercițiu"
                ),
                onClick = onBrowseExercises
            )

            HorizontalDivider(color = palette.cardBorder)

            // 2. Súlyzós gyors edzés with floating exercise selector dropdown
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column {
                                Text(
                                    text = strings.modeStrengthLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = tr(
                                        strings.language,
                                        "Válaszd ki az indítandó gyakorlatot:",
                                        "Select the exercise to start:",
                                        "Wähle die zu startende Übung:",
                                        "Selectează exercițiul de pornit:"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        SmallExerciseVideoButton(
                            exerciseName = selectedStrengthEx?.localizedName(strings.language) ?: "Fekvenyomás",
                            exerciseId = selectedStrengthEx?.id
                        )
                    }

                    // Floating Dropdown to pick any strength exercise
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { strengthDropdownOpen = true }
                                .testTag("quick_strength_picker")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedStrengthEx?.let { "${it.localizedName(strings.language)} (${it.category.localized(strings.language)})" } ?: tr(
                                        strings.language,
                                        "Válassz gyakorlatot...",
                                        "Choose an exercise...",
                                        "Übung auswählen...",
                                        "Alege un exercițiu..."
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(tr(strings.language, "Váltás", "Switch", "Wechseln", "Schimbă"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.primary)
                                    Icon(
                                        imageVector = if (strengthDropdownOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = strengthDropdownOpen,
                            onDismissRequest = { strengthDropdownOpen = false },
                            containerColor = palette.surface,
                            modifier = Modifier
                                .widthIn(min = 260.dp, max = 340.dp)
                                .heightIn(max = 280.dp)
                        ) {
                            strengthList.forEach { ex ->
                                val isSel = ex.id == selectedStrengthEx?.id
                                DropdownMenuItem(
                                    modifier = Modifier.testTag("quick_strength_option_${ex.id}"),
                                    text = {
                                        Text(
                                            text = "${ex.localizedName(strings.language)} • ${ex.category.localized(strings.language)}",
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) palette.primary else palette.textPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedStrengthEx = ex
                                        strengthDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val chosen = selectedStrengthEx
                            if (chosen != null) onStartStrengthWithExercise(chosen) else onStartStrengthQuick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("quick_strength_start")
                    ) {
                        Text(
                            text = tr(
                                strings.language,
                                "▶ Indítás: ${selectedStrengthEx?.localizedName(strings.language) ?: strings.modeStrengthLabel}",
                                "▶ Start: ${selectedStrengthEx?.localizedName(strings.language) ?: strings.modeStrengthLabel}",
                                "▶ Start: ${selectedStrengthEx?.localizedName(strings.language) ?: strings.modeStrengthLabel}",
                                "▶ Start: ${selectedStrengthEx?.localizedName(strings.language) ?: strings.modeStrengthLabel}"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Időalapú / Intervallum gyors edzés with floating timed exercise selector dropdown
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column {
                                Text(
                                    text = "${strings.modeTimedLabel} / ${strings.modeIntervalsLabel}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = tr(
                                        strings.language,
                                        "Válassz időalapú gyakorlatot (nem csak Plank):",
                                        "Select a timed exercise (not just Plank):",
                                        "Wähle eine zeitbasierte Übung (nicht nur Plank):",
                                        "Selectează un exercițiu cronometrat (nu doar Plank):"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        SmallExerciseVideoButton(
                            exerciseName = selectedTimedEx?.localizedName(strings.language) ?: "Plank",
                            exerciseId = selectedTimedEx?.id
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { timedDropdownOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val secUnit = tr(strings.language, "mp", "s", "Sek.", "sec")
                                Text(
                                    text = selectedTimedEx?.let { "${it.localizedName(strings.language)} (${it.targetSeconds} $secUnit)" } ?: "Plank",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(tr(strings.language, "Váltás", "Switch", "Wechseln", "Schimbă"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.primary)
                                    Icon(
                                        imageVector = if (timedDropdownOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = timedDropdownOpen,
                            onDismissRequest = { timedDropdownOpen = false },
                            containerColor = palette.surface,
                            modifier = Modifier
                                .widthIn(min = 260.dp, max = 340.dp)
                                .heightIn(max = 260.dp)
                        ) {
                            val secUnit = tr(strings.language, "mp", "s", "Sek.", "sec")
                            timedList.forEach { ex ->
                                val isSel = ex.id == selectedTimedEx?.id
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${ex.localizedName(strings.language)} (${ex.targetSeconds} $secUnit)",
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) palette.primary else palette.textPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedTimedEx = ex
                                        timedDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val chosen = selectedTimedEx
                            if (chosen != null) onStartTimedWithExercise(chosen) else onStartTimedQuick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr(
                                strings.language,
                                "▶ Indítás: ${selectedTimedEx?.localizedName(strings.language) ?: "Időalapú edzés"}",
                                "▶ Start: ${selectedTimedEx?.localizedName(strings.language) ?: strings.modeTimedLabel}",
                                "▶ Start: ${selectedTimedEx?.localizedName(strings.language) ?: strings.modeTimedLabel}",
                                "▶ Start: ${selectedTimedEx?.localizedName(strings.language) ?: strings.modeTimedLabel}"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Szabadtéri GPS edzés
            QuickOptionRow(
                icon = Icons.Default.Park,
                title = strings.outdoorWorkoutTitle,
                subtitle = strings.outdoorWorkoutSub,
                onClick = onStartOutdoorGps
            )
        }
    }
}

@Composable
private fun QuickOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    demoExerciseForVideo: String? = null,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.primary.copy(alpha = 0.16f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
            if (demoExerciseForVideo != null) {
                SmallExerciseVideoButton(exerciseName = demoExerciseForVideo)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
    }
}

val LocalOpenExerciseVideo = androidx.compose.runtime.compositionLocalOf<(String, String?) -> Unit> { { _, _ -> } }

/**
 * Compact reusable "▶ Videó" button placed on exercise rows in lists, workouts, quick workouts, program editor, and logs.
 * Opens the embedded YouTube exercise demonstration popup dialog.
 */
@Composable
fun SmallExerciseVideoButton(
    exerciseName: String,
    modifier: Modifier = Modifier,
    compactIconOnly: Boolean = false,
    exerciseId: String? = null
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val interaction = remember { MutableInteractionSource() }
    val openVideo = LocalOpenExerciseVideo.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null) { openVideo(exerciseName, exerciseId) }
            .semantics { contentDescription = "${strings.videoLabel}: $exerciseName" }
            .testTag("exercise_video_btn_${exerciseName.hashCode()}")
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE53935).copy(alpha = 0.16f),
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).indication(interaction, ripple()).border(1.dp, Color(0xFFE53935).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compactIconOnly) 8.dp else 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(7.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.22f, size.height * 0.15f)
                            lineTo(size.width * 0.88f, size.height * 0.50f)
                            lineTo(size.width * 0.22f, size.height * 0.85f)
                            close()
                        }
                        drawPath(path = path, color = Color.White)
                    }
                }
                if (!compactIconOnly) {
                    Text(
                        text = strings.videoLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = palette.textPrimary
                    )
                }
            }
        }
    }
}

/**
 * Reusable, theme-following scroll-more indicator for vertical full-page scrollable screens (#198).
 * Renders a subtle downward chevron/indicator when content extends below the visible viewport,
 * disappearing automatically when the user reaches the bottom or when all content fits.
 * Strictly non-blocking: pointer-transparent and never obscures bottom action buttons.
 */
@Composable
fun TrainPilotScrollMoreIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String = "Görgess lejjebb a további tartalomért"
) {
    val palette = LocalTrainPilotPalette.current
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = palette.surfaceElevated.copy(alpha = 0.88f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.45f), CircleShape)
                .testTag("trainpilot_scroll_more_indicator")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = contentDescription,
                    tint = palette.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TrainPilotScrollableBox(
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
    indicatorBottomPadding: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val canScrollForward by remember {
        androidx.compose.runtime.derivedStateOf {
            scrollState.maxValue > 0 && scrollState.value < (scrollState.maxValue - 8)
        }
    }
    Box(modifier = modifier) {
        content()
        TrainPilotScrollMoreIndicator(
            visible = canScrollForward,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = indicatorBottomPadding)
        )
    }
}

@Composable
fun TrainPilotScrollableListBox(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
    indicatorBottomPadding: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val canScrollForward by remember {
        androidx.compose.runtime.derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total == 0) false
            else {
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                if (lastVisible == null) false
                else (lastVisible.index < total - 1) || (lastVisible.offset + lastVisible.size > layoutInfo.viewportEndOffset)
            }
        }
    }
    Box(modifier = modifier) {
        content()
        TrainPilotScrollMoreIndicator(
            visible = canScrollForward,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = indicatorBottomPadding)
        )
    }
}

