package com.example.ui.components

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.MainTab
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.PaletteCategory
import com.example.ui.theme.TrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
            .border(width = 0.5.dp, color = palette.cardBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 10.dp, bottom = 8.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectTab(MainTab.HOME) }
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                        .testTag("top_bar_brand_home")
                ) {
                    TrainPilotMountainLogo(
                        modifier = Modifier.size(28.dp, 28.dp)
                    )
                    Text(
                        text = "TrainPilot",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = palette.primary,
                        maxLines = 1
                    )
                }

                // Right icons: Naptár (Calendar), Coach, Settings Gear
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isCalendarActive = (currentTab == MainTab.CALENDAR || currentSubScreen == SubScreen.CALENDAR_PLANNER) && currentSubScreen != SubScreen.SETTINGS_MAIN
                    val isCoachActive = (currentTab == MainTab.COACH || currentSubScreen == SubScreen.COACH_DETAIL) && currentSubScreen != SubScreen.SETTINGS_MAIN
                    val isSettingsActive = currentSubScreen == SubScreen.SETTINGS_MAIN

                    // 1. Naptár (Calendar) Quick Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCalendarActive) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, if (isCalendarActive) palette.primary else palette.cardBorder, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTab(MainTab.CALENDAR) }
                            .testTag("top_bar_calendar_button")
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
                            .size(44.dp)
                            .border(1.dp, if (isCoachActive) palette.primary else palette.cardBorder, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTab(MainTab.COACH) }
                            .testTag("top_bar_coach_button")
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
                            .size(44.dp)
                            .border(1.dp, if (isSettingsActive) palette.primary else palette.cardBorder, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenSettings() }
                            .testTag("top_bar_settings_button")
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
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val navScrollState = rememberScrollState()

                LaunchedEffect(currentTab, navScrollState.maxValue) {
                    if (navScrollState.maxValue > 0) {
                        val tabIndex = when (currentTab) {
                            MainTab.HOME -> 0
                            MainTab.PROGRAMS -> 1
                            MainTab.WORKOUT -> 2
                            MainTab.PROGRESS, MainTab.LOG -> 3
                            MainTab.HEALTH -> 4
                            else -> 0
                        }
                        val targetScroll = (navScrollState.maxValue * tabIndex) / 4
                        navScrollState.animateScrollTo(targetScroll)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(navScrollState)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val itemModifier = Modifier.widthIn(min = 68.dp)

                    TopNavPillItem(
                        label = strings.navHome,
                        icon = Icons.Default.Home,
                        selected = currentTab == MainTab.HOME && currentSubScreen == SubScreen.NONE,
                        onClick = { onSelectTab(MainTab.HOME) },
                        modifier = itemModifier,
                        allowUnclippedText = true,
                        testTag = "top_nav_home"
                    )
                    TopNavPillItem(
                        label = strings.navPrograms,
                        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                        selected = (currentTab == MainTab.PROGRAMS || currentSubScreen == SubScreen.PROGRAMS_LIST) && currentSubScreen != SubScreen.SETTINGS_MAIN,
                        onClick = { onSelectTab(MainTab.PROGRAMS) },
                        modifier = itemModifier,
                        allowUnclippedText = true,
                        testTag = "top_nav_programs"
                    )
                    TopNavPillItem(
                        label = strings.navWorkout,
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        selected = currentTab == MainTab.WORKOUT && currentSubScreen == SubScreen.NONE,
                        onClick = { onSelectTab(MainTab.WORKOUT) },
                        modifier = itemModifier,
                        allowUnclippedText = true,
                        testTag = "top_nav_workout"
                    )
                    TopNavPillItem(
                        label = strings.navProgress,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        selected = (currentTab == MainTab.PROGRESS || currentTab == MainTab.LOG) && currentSubScreen == SubScreen.NONE,
                        onClick = { onSelectTab(MainTab.PROGRESS) },
                        modifier = itemModifier,
                        allowUnclippedText = true,
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
                        allowUnclippedText = true,
                        testTag = "top_nav_health"
                    )
                }
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
    allowUnclippedText: Boolean = false,
    testTag: String = ""
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.primary.copy(alpha = 0.22f) else palette.surface,
        modifier = modifier
            .heightIn(min = 48.dp)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
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
                vertical = 7.dp,
                horizontal = if (allowUnclippedText) 10.dp else 2.dp
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
                maxLines = 1,
                softWrap = false,
                overflow = if (allowUnclippedText) TextOverflow.Visible else TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TrainPilotRedCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    testTag: String = ""
) {
    val strings = LocalTrainPilotStrings.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEF4444).copy(alpha = 0.16f),
        modifier = modifier
            .size(size)
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.closeAction,
                tint = Color(0xFFFCA5A5),
                modifier = Modifier.size(20.dp)
            )
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
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
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
            .border(width = 1.dp, color = palette.cardBorder.copy(alpha = 0.6f))
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
        if (selectedCategory == null) TrainPilotPalettes.allPalettes
        else TrainPilotPalettes.allPalettes.filter { it.category == selectedCategory }
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
                        color = if (active) palette.primary.copy(alpha = 0.22f) else palette.surfaceElevated,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .border(
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
                    color = if (compactPhoneMode) palette.primary.copy(alpha = 0.22f) else palette.surfaceElevated,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .border(
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

            // Category Filter: Összes (32) | Basic (16) | Vivid (16)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = listOf(
                    null to "${strings.viewAllAction} (32)",
                    PaletteCategory.BASIC to "Basic (16)",
                    PaletteCategory.VIVID to "Vivid (16)"
                )
                cats.forEach { (cat, label) ->
                    val active = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (active) palette.primary else palette.surfaceElevated,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable { onSelectCategory(cat) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp)
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
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = item.surface,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) item.primary else item.cardBorder,
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
                // Swatch circles showing Background, Surface, Primary, and Secondary
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(item.background)
                            .border(1.dp, item.cardBorder, CircleShape)
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
                        color = item.textPrimary
                    )
                    Text(
                        text = if (item.category == PaletteCategory.BASIC) "Basic • WCAG AA" else "Vivid / Neon • WCAG AA",
                        style = MaterialTheme.typography.bodySmall,
                        color = item.textSecondary
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
    currentWeeklyWorkouts: Int,
    currentStepsGoal: Int,
    onSave: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    var workouts by remember { mutableIntStateOf(currentWeeklyWorkouts) }
    var steps by remember { mutableIntStateOf(currentStepsGoal) }

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
                    modifier = Modifier.weight(1f)
                )
                TrainPilotRedCloseButton(onClick = onDismiss)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = strings.weeklyStepsFormat(workouts, steps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Column {
                    Text(
                        text = "${strings.workoutCountTitle}: $workouts",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Slider(
                        value = workouts.toFloat(),
                        onValueChange = { workouts = it.toInt() },
                        valueRange = 3f..25f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary
                        )
                    )
                }
                Column {
                    Text(
                        text = "${strings.stepsTitle}: $steps",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Slider(
                        value = steps.toFloat(),
                        onValueChange = { steps = (it.toInt() / 500) * 500 },
                        valueRange = 4000f..20000f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.stepsAccent,
                            activeTrackColor = palette.stepsAccent
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(workouts, steps) },
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                )
            ) {
                Text(strings.saveBtn, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(strings.cancelBtn, color = palette.textPrimary)
            }
        }
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
    var heightCm by remember { mutableIntStateOf(initialHeightCm) }
    var weightKg by remember { mutableFloatStateOf(initialWeightKg) }
    var selectedGoal by remember { mutableStateOf(initialGoalHu) }
    var selectedExperience by remember { mutableStateOf(initialExperienceHu) }
    var selectedLocation by remember { mutableStateOf(initialLocationHu) }
    var selectedEquipment by remember { mutableStateOf(initialEquipmentHu) }
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
    val equipments = listOf(
        "Kézisúlyzó és pad",
        "Csak saját testsúly",
        "Kézisúlyzó (pad nélkül)",
        "Teljes edzőtermi géppark",
        "Gumiszalag és rúd",
        "Kettlebell és saját testsúly"
    )
    val daysOptions = listOf(2, 3, 4, 5, 6)
    val durationOptions = listOf(30, 45, 60, 75, 90)

    val heightPresets = (150..205 step 1).filter { it % 5 == 0 || it == 178 || it == 179 || it == 182 }
    val weightPresets = (45..130 step 5).map { it.toFloat() }

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
                    .border(
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
                        Text(
                            text = if (isOpen) "▲" else "▼",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.primary
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = isOpen,
                onDismissRequest = { openDropdownKey = null },
                containerColor = palette.background,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.primary.copy(alpha = 0.7f)),
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kezdőprofil és egyéni terv (A01)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )
                    Text(
                        text = "Válaszd ki az opciókat a legördülő listákból (nem kell gépelni).",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onDismiss() }
                        .testTag("planner_skip_btn")
                ) {
                    Text(
                        text = "Kihagyás",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        },
        text = {
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
                        label = "Magasság (cm)",
                        selectedValue = "$heightCm cm",
                        options = heightPresets.map { "$it cm" },
                        onSelect = { idx, _ -> heightCm = heightPresets[idx] },
                        modifier = Modifier.weight(1f),
                        testTag = "profile_height_box"
                    )

                    PlannerDropdownBox(
                        dropdownKey = "weight",
                        label = "Testsúly (kg)",
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
                            Text("🎯", fontSize = 16.sp)
                            Column {
                                Text("Cél és edzettség", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("$selectedGoal • $selectedExperience • $selectedLocation", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            }
                        }

                        PlannerDropdownBox(
                            dropdownKey = "goal",
                            label = "Elsődleges cél",
                            selectedValue = selectedGoal,
                            options = goals,
                            onSelect = { _, value -> selectedGoal = value },
                            testTag = "profile_goal_box"
                        )

                        PlannerDropdownBox(
                            dropdownKey = "experience",
                            label = "Tapasztalati szint",
                            selectedValue = selectedExperience,
                            options = experiences,
                            onSelect = { _, value -> selectedExperience = value },
                            testTag = "profile_experience_box"
                        )

                        PlannerDropdownBox(
                            dropdownKey = "location",
                            label = "Edzés helyszíne",
                            selectedValue = selectedLocation,
                            options = locations,
                            onSelect = { _, value -> selectedLocation = value },
                            testTag = "profile_location_box"
                        )
                    }
                }

                // 2. Felszerelés
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
                            Text("🏋️", fontSize = 16.sp)
                            Column {
                                Text("Felszerelés", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text(selectedEquipment, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            }
                        }

                        PlannerDropdownBox(
                            dropdownKey = "equipment",
                            label = "Elérhető eszközök",
                            selectedValue = selectedEquipment,
                            options = equipments,
                            onSelect = { _, value -> selectedEquipment = value },
                            testTag = "profile_equipment_box"
                        )
                    }
                }

                // 3. Edzésbeosztás
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
                            Text("📅", fontSize = 16.sp)
                            Column {
                                Text("Edzésbeosztás", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("Heti $selectedDays nap • $selectedDuration perc/edzés", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlannerDropdownBox(
                                dropdownKey = "days_per_week",
                                label = "Heti edzésnapok",
                                selectedValue = "Heti $selectedDays nap",
                                options = daysOptions.map { "Heti $it nap" },
                                onSelect = { idx, _ -> selectedDays = daysOptions[idx] },
                                modifier = Modifier.weight(1f),
                                testTag = "profile_days_box"
                            )

                            PlannerDropdownBox(
                                dropdownKey = "workout_duration",
                                label = "Időtartam",
                                selectedValue = "$selectedDuration perc",
                                options = durationOptions.map { "$it perc" },
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
                        .border(1.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
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
                                Text("Korlátozások", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80))
                                val totalExcluded = excludedMuscles.size + excludedExercises.size
                                Text(
                                    text = if (totalExcluded == 0) "Nincs kizárás megadva" else "$totalExcluded kizárás kiválasztva",
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
                                color = Color(0xFF1E0A0E),
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
                                            text = "Kerülendő területek",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF6E6E)
                                        )
                                        Text(
                                            text = if (isMusclesOpen) "▲" else "▼",
                                            color = Color(0xFFFF6E6E),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = if (excludedMuscles.isEmpty()) {
                                            "Nincs kizárt terület (koppints a választáshoz)"
                                        } else {
                                            "Kizárva: ${excludedMuscles.joinToString(", ")}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (excludedMuscles.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                        color = if (excludedMuscles.isEmpty()) palette.textSecondary else Color(0xFFFF8A80)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isMusclesOpen,
                                onDismissRequest = { openDropdownKey = null },
                                containerColor = Color(0xFF1E0A0E),
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
                                                        .border(
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
                                                    text = muscle,
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
                                color = Color(0xFF1E0A0E),
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
                                            text = "Egyedi gyakorlatkizárás",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF6E6E)
                                        )
                                        Text(
                                            text = if (isExercisesOpen) "▲" else "▼",
                                            color = Color(0xFFFF6E6E),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = if (excludedExercises.isEmpty()) {
                                            "Nincs kizárt gyakorlat (koppints a választáshoz)"
                                        } else {
                                            "Kizárva: ${excludedExercises.joinToString(", ")}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (excludedExercises.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                        color = if (excludedExercises.isEmpty()) palette.textSecondary else Color(0xFFFF8A80)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isExercisesOpen,
                                onDismissRequest = { openDropdownKey = null },
                                containerColor = Color(0xFF1E0A0E),
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
                                                        .border(
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
                                                        text = exName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = palette.textPrimary
                                                    )
                                                    Text(
                                                        text = exSub,
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
            Button(
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Terv összeállítása és mentése", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickWorkoutSheet(
    onStartStrengthQuick: () -> Unit,
    onStartTimedQuick: () -> Unit,
    onStartOutdoorGps: () -> Unit,
    onBrowseExercises: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        color = palette.textPrimary
                    )
                    Text(
                        text = strings.quickWorkoutSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                TrainPilotRedCloseButton(onClick = onDismiss)
            }

            QuickOptionRow(
                icon = Icons.Default.FitnessCenter,
                title = strings.modeStrengthLabel,
                subtitle = "Goblet guggolás, Fekvőtámasz, Evezés",
                demoExerciseForVideo = "Goblet guggolás",
                onClick = onStartStrengthQuick
            )
            QuickOptionRow(
                icon = Icons.Default.Timer,
                title = "${strings.modeTimedLabel} / ${strings.modeIntervalsLabel}",
                subtitle = "Plank, Oldalsó plank",
                demoExerciseForVideo = "Plank",
                onClick = onStartTimedQuick
            )
            QuickOptionRow(
                icon = Icons.Default.Park,
                title = strings.outdoorWorkoutTitle,
                subtitle = strings.outdoorWorkoutSub,
                onClick = onStartOutdoorGps
            )
            HorizontalDivider(color = palette.cardBorder)
            QuickOptionRow(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = strings.exercisePickerTitle,
                subtitle = strings.exercisePickerSub,
                onClick = onBrowseExercises
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
            .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (demoExerciseForVideo != null) {
                ExercisePostureMiniIllustration(
                    exerciseName = demoExerciseForVideo,
                    size = 44.dp
                )
            } else {
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

val LocalOpenExerciseVideo = androidx.compose.runtime.compositionLocalOf<(String) -> Unit> { {} }

/**
 * Compact reusable "▶ Videó" button placed on exercise rows in lists, workouts, quick workouts, program editor, and logs.
 * Opens the embedded YouTube exercise demonstration popup dialog.
 */
@Composable
fun SmallExerciseVideoButton(
    exerciseName: String,
    modifier: Modifier = Modifier,
    compactIconOnly: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val openVideo = LocalOpenExerciseVideo.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { openVideo(exerciseName) }
            .semantics { contentDescription = "${strings.videoLabel}: $exerciseName" }
            .testTag("exercise_video_btn_${exerciseName.hashCode()}")
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE53935).copy(alpha = 0.16f),
            modifier = Modifier.border(1.dp, Color(0xFFE53935).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
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
 * Popup Modal Window displaying the embedded YouTube video for the selected exercise.
 */
fun resolveCatalogExerciseVideoAndNotes(queryTitleOrId: String): Pair<String, String> {
    val clean = queryTitleOrId.trim().lowercase()
    val catalog = runCatching { com.repforge.app.data.db.TrainPilotDatabase.defaultExercises() }.getOrDefault(emptyList())
    val matched = catalog.firstOrNull {
        it.id.lowercase() == clean ||
            it.nameHu.lowercase() == clean ||
            it.nameEn.lowercase() == clean ||
            it.nameDe.lowercase() == clean ||
            it.nameRo.lowercase() == clean
    } ?: catalog.firstOrNull {
        it.nameHu.lowercase().contains(clean) ||
            clean.contains(it.nameHu.lowercase()) ||
            it.nameEn.lowercase().contains(clean)
    }

    if (matched != null) {
        val url = matched.videoUrl.orEmpty()
        val idFromUrl = Regex("v=([A-Za-z0-9_-]{11})").find(url)?.groupValues?.getOrNull(1)
        val idFromRaw = runCatching {
            org.json.JSONObject(matched.rawJson).optJSONObject("demo")?.optString("videoId")
        }.getOrNull()?.takeIf { it.length == 11 }
        val videoId = idFromUrl ?: idFromRaw ?: "MeIiIdhvXT4"
        val notes = matched.notesHu.ifBlank {
            "Kontrollált excentrikus szakasz (2 mp leengedés), stabil törzzsel és teljes mozgástartománnyal."
        }
        return videoId to notes
    }

    val fallbackId = when {
        clean.contains("goblet") || clean.contains("guggol") -> "MeIiIdhvXT4"
        clean.contains("fekvőtámasz") || clean.contains("fekvotamasz") -> "IODxDxX7oi4"
        clean.contains("fekvenyom") -> "rT7DgCr-3pg"
        clean.contains("plank") -> "pSHjTRCQxIw"
        clean.contains("húzódzkod") || clean.contains("huzodzkod") -> "eGo4IYlbE5g"
        clean.contains("evez") -> "FWJR5Ve8bnQ"
        clean.contains("kitör") || clean.contains("bolgár") -> "2C-uNgKwPLE"
        clean.contains("váll") || clean.contains("vall") -> "2yjwXTZQDDI"
        clean.contains("tricepsz") || clean.contains("bicepsz") || clean.contains("kar") -> "ykJmrZ5v0Oo"
        else -> "MeIiIdhvXT4"
    }
    return fallbackId to "Kontrollált excentrikus szakasz (2 mp leengedés), stabil törzzsel és teljes mozgástartománnyal."
}

@Composable
fun ExerciseYouTubeVideoModalDialog(
    exerciseTitle: String,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val isRobolectric = remember {
        android.os.Build.FINGERPRINT?.lowercase()?.contains("robolectric") == true
    }
    var isPlaying by remember { mutableStateOf(true) }
    var useEmbeddedWebView by remember { mutableStateOf(!isRobolectric) }

    val (youtubeVideoId, exerciseNotesHu) = remember(exerciseTitle) {
        resolveCatalogExerciseVideoAndNotes(exerciseTitle)
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE53935)
                    ) {
                        Text(
                            text = "▶ YouTube",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = exerciseTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TrainPilotRedCloseButton(
                    onClick = onDismiss
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Embedded YouTube Player Box (16:9 ratio)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0B0E14),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Visual YouTube Embed Preview + WebView layer
                        if (useEmbeddedWebView) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.mediaPlaybackRequiresUserGesture = false
                                        setBackgroundColor(android.graphics.Color.parseColor("#0B0E14"))
                                        webChromeClient = android.webkit.WebChromeClient()
                                        webViewClient = object : android.webkit.WebViewClient() {
                                            override fun onRenderProcessGone(view: android.webkit.WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                                                runCatching {
                                                    val parent = view.parent as? android.view.ViewGroup
                                                    parent?.removeView(view)
                                                    view.stopLoading()
                                                    view.destroy()
                                                }
                                                useEmbeddedWebView = false
                                                return true
                                            }
                                        }
                                        val html = """
                                            <!DOCTYPE html>
                                            <html>
                                            <head>
                                              <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                              <style>
                                                body, html { margin: 0; padding: 0; width: 100%; height: 100%; background: #0B0E14; overflow: hidden; font-family: sans-serif; color: #fff; }
                                                .wrap { position: relative; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
                                                iframe { width: 100%; height: 100%; border: 0; }
                                              </style>
                                            </head>
                                            <body>
                                              <div class="wrap">
                                                <iframe src="https://www.youtube.com/embed/$youtubeVideoId?autoplay=1&rel=0&modestbranding=1&playsinline=1"
                                                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                                        allowfullscreen></iframe>
                                              </div>
                                            </body>
                                            </html>
                                        """.trimIndent()
                                        loadDataWithBaseURL(
                                            "https://www.youtube.com",
                                            html,
                                            "text/html",
                                            "utf-8",
                                            null
                                        )
                                    }
                                },
                                onRelease = { view ->
                                    runCatching {
                                        val parent = view.parent as? android.view.ViewGroup
                                        parent?.removeView(view)
                                        view.stopLoading()
                                        view.destroy()
                                    }
                                }
                            )
                        }

                        // Top overlay badge showing YouTube ID & interactive simulation toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YouTube • ID: $youtubeVideoId",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 32.dp)
                                    .clickable { useEmbeddedWebView = !useEmbeddedWebView }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (useEmbeddedWebView) "Előnézet" else "Beágyazott videó",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary
                                    )
                                }
                            }
                        }

                        if (!useEmbeddedWebView) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE53935),
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clickable { useEmbeddedWebView = true }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isPlaying) "▶" else "⏸",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = exerciseTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Key coaching & execution cues under the video player
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = strings.executionTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                        Text(
                            text = "• $exerciseNotesHu",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "• Teljes mozgástartomány (ROM) megtartása rángatás nélkül.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                )
            ) {
                Text(strings.closeAction, fontWeight = FontWeight.Bold)
            }
        }
    )
}

