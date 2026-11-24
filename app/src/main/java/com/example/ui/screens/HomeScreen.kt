package com.example.ui.screens
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.trainPilotGlassScene
import com.example.ui.theme.LocalGlassTopInset
import com.example.ui.components.PageLayoutHorizontalRow
import com.example.ui.components.PageLayoutCard
import androidx.compose.runtime.key
import com.example.ui.components.rememberPageLayout
import com.example.ui.components.PageLayoutTile
import com.example.ui.components.PageLayoutFooter
import com.example.ui.components.PageLayoutFrame
import com.example.ui.components.PageLayoutBlock

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotSoftGlow

import com.example.model.localWorkoutDate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings
import com.example.model.HomeDemoMode
import com.example.ui.components.TrainPilotMountainLogo
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.selectionColors
import com.example.ui.theme.readablePrimaryLabel
import com.example.ui.viewmodel.TrainPilotUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: TrainPilotUiState,
    strings: LocalizedStrings,
    onStartOrContinueWorkout: () -> Unit,
    onOpenProgramDayDetails: () -> Unit,
    onOpenQuickWorkout: () -> Unit,
    onOpenWatchAndSync: () -> Unit,
    onOpenCoachReadiness: () -> Unit,
    onOpenGoalsDialog: () -> Unit,
    onOpenPersonalPlanner: () -> Unit,
    onDiscardWorkout: (String) -> Unit = {},
    onOpenHealthJournal: (Pair<String, String>?) -> Unit = {},
    onTogglePulseTrend: () -> Unit = {},
    onOpenLoggedWorkout: (String) -> Unit = {},
    onNavigateFromToday: (com.example.model.SubScreen) -> Unit = {},
    /** 2786: Home weekly goals → Health with the weekly view open. */
    onOpenWeeklyGoals: () -> Unit = onOpenGoalsDialog,
    inlineProgramEditor: (@Composable () -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    var todayTile by remember { mutableStateOf<String?>(null) }
    val target = state.homeWorkoutTarget
    val plannedEvent = state.calendarEvents.firstOrNull { it.id == target?.scheduleId }
    val activeProgram = state.programs.firstOrNull { it.id == target?.programId }
        ?: state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()
    val currentDay = activeProgram?.days?.firstOrNull {
        it.id == target?.dayId || it.id == "${activeProgram.id}:${target?.dayId}"
    } ?: activeProgram?.days?.firstOrNull()
    fun tr(hu: String, en: String, de: String, ro: String) = when (state.language) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }

    val scrollState = rememberScrollState()
    val blocks = listOf(
        PageLayoutBlock("profile", strings.profileAndNewPlanBtn),
        PageLayoutBlock("program", tr("Aktív program", "Active program", "Aktives Programm", "Program activ")),
        PageLayoutBlock("advice", tr("Mai javaslat", "Today’s advice", "Heutige Empfehlung", "Recomandarea zilei")),
        PageLayoutBlock("quick_watch", tr("Gyors edzés és óra", "Quick workout and watch", "Schnelltraining und Uhr", "Antrenament rapid și ceas")),
        PageLayoutBlock("daily_health", tr("Mai állapot", "Today’s status", "Heutiger Status", "Starea de azi")),
        PageLayoutBlock("weekly", tr("Heti célok", "Weekly goals", "Wochenziele", "Obiective săptămânale"))
    )
    val layout = rememberPageLayout("home", blocks.map { it.id })
    PageLayoutFrame(layout, state.language, blocks, scrollState, 5.dp) {
    com.example.ui.components.TrainPilotScrollableBox(
        scrollState = scrollState,
        modifier = Modifier.fillMaxSize().background(palette.pageBrush).trainPilotGlassScene(palette)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter)
                .padding(top = 4.dp + LocalGlassTopInset.current, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
        layout.order.forEach { id ->
            key(id) {
                PageLayoutTile(layout, id, hasHorizontalTiles = id == "quick_watch" || id == "daily_health") {
                    when (id) {
                        "profile" -> {
                            // A kompakt Profil és új terv belépő mindig elérhető a Kezdőlapon.
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
                                modifier = Modifier
                                    .fillMaxWidth().trainPilotGlassMaterial(palette, RoundedCornerShape(14.dp))
                                    .trainPilotSoftGlow(palette.primary, RoundedCornerShape(14.dp), 3.dp)
                                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                                    .trainPilotClickable(RoundedCornerShape(14.dp)) { onOpenPersonalPlanner() }
                                    .testTag("home_personal_planner_mini_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = palette.tintedSurface(0.16f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = palette.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = strings.profileAndNewPlanBtn,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = palette.textPrimary
                                        )
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = strings.profileAndNewPlanBtn,
                                        tint = palette.primary, modifier = Modifier.size(24.dp).testTag("home_profile_new_plan_button"))
                                }
                            }
                        }
                        "program" -> {
                            // 3. HERO WORKOUT CARD (Mockup 1/4 #1: "Szüneteltetve • Teljes test A • 8 gyakorlat • kb. 60 perc • ▶ Folytatás")
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
                                modifier = Modifier
                                    .fillMaxWidth().trainPilotGlassMaterial(palette, RoundedCornerShape(20.dp))
                                    .trainPilotSoftGlow(palette.primary, RoundedCornerShape(20.dp))
                                    .trainPilotBorder(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                palette.primary.copy(alpha = 0.55f),
                                                palette.cardBorder
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .testTag("home_hero_workout_card")
                            ) {
                              Column(modifier = Modifier.fillMaxWidth()) {
                                // Header tap expands the active program's editor inside this card (or returns to a live workout).
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .trainPilotClickable(RoundedCornerShape(20.dp)) { onOpenProgramDayDetails() }
                                        .testTag("home_hero_workout_header")
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    palette.surfaceElevated.copy(alpha = 0.65f),
                                                    palette.surface
                                                )
                                            )
                                        )
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left Dumbbell Icon in warm dark gold container
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = palette.tintedSurface(0.14f),
                                            modifier = Modifier
                                                .size(40.dp)
                                                .trainPilotBorder(
                                                    width = 1.dp,
                                                    color = palette.tintedSurface(0.3f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.FitnessCenter,
                                                    contentDescription = null,
                                                    tint = palette.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(1.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (state.isWorkoutSessionActive && state.homeDemoMode == HomeDemoMode.PAUSED_WORKOUT) {
                                                    Icon(
                                                        imageVector = Icons.Default.Pause,
                                                        contentDescription = null,
                                                        tint = palette.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = strings.pausedBadge,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = palette.primary
                                                    )
                                                } else {
                                                    Text(
                                                        text = if (state.isWorkoutSessionActive) tr("Aktív edzés", "Active Workout", "Aktives Training", "Antrenament activ") else if (activeProgram != null) {
                                                            "${strings.activeProgramBadge} • ${activeProgram.localizedName(state.language)}"
                                                        } else {
                                                            strings.activeProgramBadge
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = palette.primary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = palette.selectionColors().surface,
                                                modifier = Modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                                            ) {
                                                Text(
                                                    text = if (state.isWorkoutSessionActive) {
                                                        com.example.model.LoggedWorkoutEntry(id = "active", titleHu = state.activeWorkoutTitleHu)
                                                            .localizedTitle(state.language, state.programs, state.exercises)
                                                    } else currentDay?.localizedDayCode(state.language) ?: tr("A nap", "Day A", "Tag A", "Ziua A"),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold,
                                                        fontSize = MaterialTheme.typography.titleSmall.fontSize * 1.1f),
                                                    color = palette.selectionColors().foreground,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).testTag("home_planned_day")
                                                )
                                            }

                                            Text(
                                                text = if (state.isWorkoutSessionActive) {
                                                    tr(
                                                        "${state.activeWorkoutExercises.size} gyakorlat • Aktív edzés",
                                                        "${state.activeWorkoutExercises.size} exercises • Active workout",
                                                        "${state.activeWorkoutExercises.size} Übungen • Aktives Training",
                                                        "${state.activeWorkoutExercises.size} exerciții • Antrenament activ"
                                                    )
                                                } else {
                                                    tr(
                                                        "${currentDay?.exercises?.size ?: 8} gyakorlat • kb. ${plannedEvent?.durationMinutes ?: currentDay?.durationMinutes ?: 60} perc",
                                                        "${currentDay?.exercises?.size ?: 8} exercises • ~${plannedEvent?.durationMinutes ?: currentDay?.durationMinutes ?: 60} min",
                                                        "${currentDay?.exercises?.size ?: 8} Übungen • ca. ${plannedEvent?.durationMinutes ?: currentDay?.durationMinutes ?: 60} Min.",
                                                        "${currentDay?.exercises?.size ?: 8} exerciții • aprox. ${plannedEvent?.durationMinutes ?: currentDay?.durationMinutes ?: 60} min"
                                                    )
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary
                                            )
                                            // 2773: when is it planned (today / still doable today / next date / next in cycle).
                                            if (!state.isWorkoutSessionActive && target != null) {
                                                val line = com.example.ui.components.homePlannedDateLine(target.startIso,
                                                    java.time.ZonedDateTime.now(), state.language)
                                                Text(
                                                    text = line.text,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = when (line.timing) {
                                                        com.example.ui.components.HomePlanTiming.TODAY_UPCOMING -> palette.success
                                                        com.example.ui.components.HomePlanTiming.TODAY_PASSED -> palette.warning
                                                        else -> palette.textSecondary
                                                    },
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.testTag("home_planned_date_line")
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = strings.detailsAction,
                                            tint = palette.textSecondary
                                        )
                                    }

                                    // Full-width gold primary CTA Button ("▶ Folytatás" / "▶ Tervezett edzés indítása")
                                    if (state.isWorkoutSessionActive && state.activeWorkoutId.isNotBlank()) {
                                        com.example.ui.components.StudioPausedWorkoutActions(state.activeWorkoutId, state.phoneControlsWorkout,
                                            state.isSavingWorkout, strings.continueBtn, onStartOrContinueWorkout, onDiscardWorkout)
                                    } else Button(
                                        onClick = onStartOrContinueWorkout,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.primary,
                                            contentColor = palette.readablePrimaryLabel()
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 42.dp)
                                            .testTag("home_continue_workout_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (state.homeDemoMode == HomeDemoMode.PAUSED_WORKOUT) {
                                                strings.continueBtn
                                            } else {
                                                strings.startPlannedBtn
                                            },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                                if (inlineProgramEditor != null && !state.isWorkoutSessionActive &&
                                    state.inlineEditorHost == com.example.ui.viewmodel.INLINE_EDITOR_HOST_HOME && state.inlineEditorProgramId != null
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 10.dp)
                                            .testTag("home_inline_program_editor"),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        androidx.compose.material3.HorizontalDivider(color = palette.cardBorder.copy(alpha = 0.65f))
                                        inlineProgramEditor()
                                    }
                                }
                              }
                            }
                        }
                        "advice" -> {
                            com.example.ui.components.HomeCoachAdviceCard(state, strings, onOpenCoachReadiness)
                        }
                        "quick_watch" -> {
                            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val sideBySide = maxWidth >= 300.dp && androidx.compose.ui.platform.LocalDensity.current.fontScale <= 1.3f
                                val quick: @Composable (Modifier) -> Unit = { card ->
                                    HomeCompactShortcut(card.testTag("home_quick_workout_card"), Icons.AutoMirrored.Filled.DirectionsRun,
                                        strings.quickWorkoutTitle, "", onOpenQuickWorkout) // 2778: title is enough
                                }
                                val watch: @Composable (Modifier) -> Unit = { card ->
                                    val status = when {
                                        state.watchInfoLoading -> tr("Óraadatok lekérése…", "Fetching watch info…", "Uhrendaten werden geladen…", "Se preiau datele ceasului…")
                                        state.watchAppAvailable && state.watchBatteryPercent in 0..100 -> strings.connectedWithBattery(state.watchBatteryPercent)
                                        state.watchAppAvailable -> tr("TrainPilot óraapp elérhető", "TrainPilot watch app available", "TrainPilot-Uhren-App verfügbar", "Aplicația de ceas TrainPilot disponibilă")
                                        state.watchConnected -> tr("Wear OS kapcsolat • óraapp nem igazolt", "Wear OS connected • watch app unverified", "Wear OS verbunden • Uhren-App unbestätigt", "Wear OS conectat • aplicație ceas neverificată")
                                        else -> tr("Nincs órakapcsolat", "No watch connected", "Keine Uhrenverbindung", "Nicio conexiune cu ceasul")
                                    }
                                    HomeCompactShortcut(card.testTag("home_watch_status_card"), Icons.Default.Watch,
                                        if (state.watchAppAvailable) state.watchName else strings.watchConnectedSub, status, onOpenWatchAndSync,
                                        enabled = !state.watchInfoLoading, connected = state.watchAppAvailable)
                                }
                                val shortcuts = listOf(PageLayoutBlock("quick", strings.quickWorkoutTitle),
                                    PageLayoutBlock("watch", tr("Óra", "Watch", "Uhr", "Ceas")))
                                if (sideBySide) PageLayoutHorizontalRow(layout, "shortcuts", shortcuts,
                                    TrainPilotCardSpacing.CardRowGap) { shortcut, modifier ->
                                    if (shortcut == "quick") quick(modifier) else watch(modifier)
                                } else Column(verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)) {
                                    layout.childOrder("shortcuts", shortcuts.map { it.id }).forEach { shortcut ->
                                        PageLayoutCard {
                                            if (shortcut == "quick") quick(Modifier.fillMaxWidth()) else watch(Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                            }
                        }
                        "daily_health" -> {
                            val todayShape = RoundedCornerShape(20.dp)
                            Surface(
                                shape = todayShape,
                                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.cardPaint,
                                modifier = Modifier.fillMaxWidth().trainPilotGlassMaterial(palette, todayShape)
                                    .trainPilotSoftGlow(palette.primary, todayShape)
                                    .border(com.repforge.app.ui.theme.TrainPilotFrames.page(palette.sectionBorder, palette.primary), todayShape)
                                    .testTag("home_today_status_group")
                            ) {
                                Box(Modifier.padding(8.dp)) {
                                    StudioTodayStatusGrid(state, compact = true, layout = layout) { key ->
                                        todayTile = key
                                        if (key == "pulse") onTogglePulseTrend()
                                    }
                                }
                            }
                        }
                        "weekly" -> {
                            // 8. MAIN BRANCH HOME CARDS (Célok beállítása, Mai javaslat / Coach, Személyes edzéstervező, Aktív kalória)
                            Column {
                                Column(verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)) {
                                    // 2786: shared weekly goals (same data as Health); tap opens Health with the big view.
                                    com.example.ui.components.WeeklyGoalsCompact(state, framed = true,
                                        onOpen = onOpenWeeklyGoals, onGoals = onOpenGoalsDialog,
                                        modifier = Modifier.testTag("home_goals_row_card"))

                                }
                            }
                        }
                    }
                }
            }
        }
        PageLayoutFooter(layout, state.language)
    }
}
    }
    todayTile?.let { tile ->
        StudioTodayStatusDetailSheet(
            state, strings, tile, java.time.LocalDate.now(),
            onDismiss = { todayTile = null },
            onOpenJournal = onOpenHealthJournal,
            onTogglePulseTrend = onTogglePulseTrend,
            onOpenWeekly = onOpenWeeklyGoals,
            onOpenLoggedWorkout = onOpenLoggedWorkout,
            onNavigateToSubScreen = onNavigateFromToday
        )
    }
}

@Composable
private fun HomeCompactShortcut(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true, connected: Boolean? = null) {
    val p = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(14.dp)
    Surface(color = p.surface, shape = shape,
        modifier = modifier.trainPilotSoftGlow(p.primary, shape, 3.dp)
            .trainPilotBorder(1.dp, Brush.linearGradient(listOf(p.sectionBorder, p.cardBorder)), shape)
            .trainPilotClickable(shape, enabled = enabled, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 9.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = p.primary, modifier = Modifier.size(18.dp))
                Text(title, color = p.textPrimary, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (connected != null) Box(Modifier.size(7.dp).clip(CircleShape)
                    .background(if (connected) p.success else p.warning))
            }
            if (subtitle.isNotBlank()) Text(subtitle, color = p.textSecondary, style = MaterialTheme.typography.labelSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
