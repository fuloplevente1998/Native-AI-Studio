package com.example.ui.screens

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.example.model.volumeAxisMaximum

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import com.example.model.workoutsInWeekEnding
import com.example.model.StudioProgressWindow
import com.example.model.progressPeriodDays

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import com.example.localization.LocalizedStrings
import com.example.model.LoggedWorkoutEntry
import com.example.model.SubScreen
import com.example.ui.components.AnatomicalMuscleHeatmapCard
import com.example.ui.components.ExercisePostureMiniIllustration
import com.example.ui.components.NativeWorkoutRouteMapBox
import com.example.ui.components.MockupHealthConnectBadge
import com.example.ui.components.MockupWatchGraphicBadge
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 1. NAPLÓ (Mockup 3/4 #1 + main Edzésnapló)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogTabScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onSearchChange: (String) -> Unit,
    onOpenWorkoutDetail: (String) -> Unit,
    onOpenRouteMap: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPalettePicker: () -> Unit,
    onDeletedWorkout: (String) -> Unit = {}
) {
    val deleteModel: com.repforge.app.ui.journal.JournalViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key = "studio-history-list-deletion")
    val deleteState by deleteModel.uiState.collectAsState()
    com.example.ui.components.StudioWorkoutDeleteConfirmation(null, onDeletedWorkout, deleteModel)
    if (deleteState.operationFailed && deleteState.workoutToDelete == null) AlertDialog(
        onDismissRequest = deleteModel::clearOperationError,
        title = { Text("Az edzés adatai nem érhetők el") },
        text = { Text("A napló közben frissülhetett vagy adatcsere van folyamatban. Nyisd meg újra a műveletet.") },
        confirmButton = { TextButton(onClick = deleteModel::clearOperationError) { Text("Rendben") } },
        modifier = Modifier.testTag("journal_list_delete_error")
    )

    val palette = LocalTrainPilotPalette.current
    val today = remember { java.time.LocalDate.now() }
    var currentYearMonth by remember { mutableStateOf(java.time.YearMonth.from(today)) }
    var filterAllMonths by remember { mutableStateOf(true) }
    var intervalStartDay by remember { mutableStateOf<Int?>(null) }
    var intervalEndDay by remember { mutableStateOf<Int?>(null) }
    var showInlineCalendar by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val huMonthNames = listOf(
        "január", "február", "március", "április", "május", "június",
        "július", "augusztus", "szeptember", "október", "november", "december"
    )
    val huMonthShort = listOf(
        "jan.", "febr.", "márc.", "ápr.", "máj.", "jún.",
        "júl.", "aug.", "szept.", "okt.", "nov.", "dec."
    )
    val monthFullHu = "${currentYearMonth.year}. ${huMonthNames[currentYearMonth.monthValue - 1]}"
    val monthPrefixHu = huMonthShort[currentYearMonth.monthValue - 1]

    val rangeMinDay = if (intervalStartDay != null && intervalEndDay != null) {
        minOf(intervalStartDay!!, intervalEndDay!!)
    } else intervalStartDay
    val rangeMaxDay = if (intervalStartDay != null && intervalEndDay != null) {
        maxOf(intervalStartDay!!, intervalEndDay!!)
    } else intervalStartDay

    val currentFilterLabel = when {
        intervalStartDay != null && intervalEndDay != null ->
            "$monthPrefixHu ${rangeMinDay}. – $monthPrefixHu ${rangeMaxDay}."
        intervalStartDay != null ->
            "Innentől: $monthPrefixHu ${intervalStartDay}. (válassz zárónapot)"
        filterAllMonths ->
            "Összes hónap (${today.year})"
        else ->
            monthFullHu
    }

    val filteredLogs = uiState.loggedWorkouts.filter { entry ->
        val matchesSearch = uiState.logSearchQuery.isBlank() ||
            entry.titleHu.contains(uiState.logSearchQuery, ignoreCase = true) ||
            entry.summaryExercisesHu.contains(uiState.logSearchQuery, ignoreCase = true)

        val matchesIntervalOrMonth = when {
            rangeMinDay != null && rangeMaxDay != null -> {
                val monthMatches = entry.dateLabelHu.contains(monthPrefixHu, ignoreCase = true) ||
                    entry.dateLabelHu.contains(". ${"%02d".format(currentYearMonth.monthValue)}.")
                val dayNum = Regex("(\\d{1,2})\\.\\s*$").find(entry.dateLabelHu.trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: Regex("\\.\\s*(\\d{1,2})\\.").findAll(entry.dateLabelHu).lastOrNull()?.groupValues?.getOrNull(1)?.toIntOrNull()
                monthMatches && dayNum != null && dayNum in rangeMinDay..rangeMaxDay
            }
            filterAllMonths -> true
            else -> entry.dateLabelHu.contains(monthPrefixHu, ignoreCase = true) ||
                entry.dateLabelHu.contains("${currentYearMonth.year}. ${"%02d".format(currentYearMonth.monthValue)}")
        }
        matchesSearch && matchesIntervalOrMonth
    }

    fun handleDayClick(dayNum: Int) {
        filterAllMonths = false
        if (intervalStartDay == null || (intervalStartDay != null && intervalEndDay != null)) {
            intervalStartDay = dayNum
            intervalEndDay = null
        } else {
            intervalEndDay = dayNum
        }
    }

    if (showDatePickerDialog) {
        AlertDialog(
            onDismissRequest = { showDatePickerDialog = false },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = "📅 Napló intervallum-szűrő",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. koppintás = Innentől, 2. koppintás = Idáig.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "Aktuális szűrő: $currentFilterLabel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDatePickerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
                ) {
                    Text("Rendben", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(
                    onClick = {
                        intervalStartDay = null
                        intervalEndDay = null
                        filterAllMonths = true
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Szűrő törlése", color = palette.textPrimary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("log_tab_screen"),
        contentPadding = PaddingValues(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = uiState.logSearchQuery,
                onValueChange = onSearchChange,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                },
                placeholder = { Text(strings.logSearchHint, color = palette.textMuted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_search_input")
            )
        }

        // Interactive Month & Date selector row (< | 📅 2026. október v | >)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .size(40.dp)
                        .trainPilotBorder(1.dp, palette.cardBorder, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            filterAllMonths = false
                            intervalStartDay = null
                            intervalEndDay = null
                            currentYearMonth = currentYearMonth.minusMonths(1)
                        }
                        .testTag("log_prev_month_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Előző hónap",
                            tint = palette.primary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showInlineCalendar = !showInlineCalendar }
                        .testTag("log_month_picker_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = currentFilterLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                        Icon(
                            imageVector = if (showInlineCalendar) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Dátumválasztó megnyitása",
                            tint = palette.primary
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .size(40.dp)
                        .trainPilotBorder(1.dp, palette.cardBorder, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            filterAllMonths = false
                            intervalStartDay = null
                            intervalEndDay = null
                            currentYearMonth = currentYearMonth.plusMonths(1)
                        }
                        .testTag("log_next_month_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Következő hónap",
                            tint = palette.primary
                        )
                    }
                }
            }
        }

        // Expandable Interval Calendar Filter Card (1. nap = Innentől, 2. nap = Idáig + Szűrő törlése + Mai napra ugrás)
        item {
            AnimatedVisibility(visible = showInlineCalendar) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
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
                            Column {
                                Text(
                                    text = monthFullHu,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = when {
                                        intervalStartDay == null -> "1. nyomás: Innentől • 2. nyomás: Idáig"
                                        intervalEndDay == null -> "Innentől: $monthPrefixHu ${intervalStartDay}. → válassz 2. napot (Idáig)"
                                        else -> "Intervallum: $monthPrefixHu ${rangeMinDay}. – $monthPrefixHu ${rangeMaxDay}."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.surfaceElevated,
                                    modifier = Modifier
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            currentYearMonth = java.time.YearMonth.from(today)
                                            filterAllMonths = false
                                            intervalStartDay = today.dayOfMonth
                                            intervalEndDay = today.dayOfMonth
                                        }
                                        .testTag("log_jump_today_button")
                                ) {
                                    Text(
                                        text = "Mai napra ugrás",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.surfaceElevated,
                                    modifier = Modifier
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            intervalStartDay = null
                                            intervalEndDay = null
                                            filterAllMonths = true
                                        }
                                        .testTag("log_clear_filter_button")
                                ) {
                                    Text(
                                        text = "Szűrő törlése",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("H", "K", "Sze", "Cs", "P", "Szo", "V").forEach { dHdr ->
                                Text(
                                    text = dHdr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textSecondary,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        val daysInMonth = currentYearMonth.lengthOfMonth()
                        val firstDayOffset = currentYearMonth.atDay(1).dayOfWeek.value - 1
                        val isCurrentMonthToday = currentYearMonth.year == today.year && currentYearMonth.monthValue == today.monthValue

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            var dayCounter = 1
                            for (row in 0..5) {
                                if (dayCounter > daysInMonth) break
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (col in 0..6) {
                                        if ((row == 0 && col < firstDayOffset) || dayCounter > daysInMonth) {
                                            Spacer(modifier = Modifier.size(36.dp))
                                        } else {
                                            val dayNum = dayCounter
                                            val isStartOrEnd = dayNum == intervalStartDay || dayNum == intervalEndDay
                                            val isInInterval = rangeMinDay != null && rangeMaxDay != null && dayNum in rangeMinDay..rangeMaxDay
                                            val isToday = isCurrentMonthToday && dayNum == today.dayOfMonth
                                            val hasLoggedWorkout = uiState.loggedWorkouts.any {
                                                it.dateLabelHu.contains(monthPrefixHu, ignoreCase = true) &&
                                                    it.dateLabelHu.contains(" $dayNum.")
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = when {
                                                    isStartOrEnd -> palette.primary
                                                    isInInterval -> palette.primary.copy(alpha = 0.28f)
                                                    else -> palette.surfaceElevated
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .trainPilotBorder(
                                                        width = if (isToday || isStartOrEnd) 1.5.dp else 1.dp,
                                                        color = when {
                                                            isStartOrEnd || isToday -> palette.primary
                                                            isInInterval -> palette.primary.copy(alpha = 0.65f)
                                                            else -> palette.cardBorder
                                                        },
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { handleDayClick(dayNum) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = "$dayNum",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isStartOrEnd || isInInterval || isToday) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isStartOrEnd) palette.onPrimary else palette.textPrimary
                                                        )
                                                        if (hasLoggedWorkout) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isStartOrEnd) palette.onPrimary else palette.primary)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            dayCounter++
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick shortcut banner to Edzés részletei & Óra és szinkron
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currentFilterLabel • ${filteredLogs.size} rögzített edzés",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                if (filteredLogs.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.primary.copy(alpha = 0.16f),
                        modifier = Modifier
                            .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val firstStrength = filteredLogs.firstOrNull { !it.isOutdoorRun } ?: filteredLogs.firstOrNull()
                                if (firstStrength != null) {
                                    onOpenWorkoutDetail(firstStrength.id)
                                }
                            }
                            .testTag("log_open_latest_detail_shortcut")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Edzés részletei",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Nincs rögzített edzés erre a szűrésre",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "A kiválasztott időszakra ($currentFilterLabel) még nem található naplózott edzés.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        if (!filterAllMonths || intervalStartDay != null) {
                            Button(
                                onClick = {
                                    intervalStartDay = null
                                    intervalEndDay = null
                                    filterAllMonths = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
                            ) {
                                Text("Szűrő törlése", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        items(filteredLogs, key = { it.id }) { logEntry ->
            LogWorkoutCard(
                entry = logEntry,
                onClick = {
                    if (logEntry.isOutdoorRun) {
                        onOpenRouteMap()
                    } else {
                        onOpenWorkoutDetail(logEntry.id)
                    }
                },
                onOpenDetailAlways = { onOpenWorkoutDetail(logEntry.id) },
                onDelete = { deleteModel.requestDeleteWorkout(logEntry.id) }, deleteEnabled = !deleteState.deleting
            )
        }
    }
}

@Composable
private fun LogWorkoutCard(
    entry: LoggedWorkoutEntry,
    onClick: () -> Unit,
    onOpenDetailAlways: () -> Unit,
    onDelete: () -> Unit,
    deleteEnabled: Boolean
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenDetailAlways() }
            .testTag("log_card_${entry.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry.isOutdoorRun) palette.success.copy(alpha = 0.16f)
                        else palette.primary.copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.isOutdoorRun) Icons.AutoMirrored.Filled.DirectionsRun else Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = if (entry.isOutdoorRun) palette.success else palette.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.dateLabelHu,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.titleHu,
                        modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = palette.success,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = if (entry.isOutdoorRun) {
                        "${entry.distanceKm} km • ${entry.workTimeLabel} • ${entry.paceLabel}"
                    } else {
                        "${entry.durationMinutes} perc • ${entry.totalSets} sorozat • ${entry.totalVolumeKg} kg"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary
                )
                Text(
                    text = entry.summaryExercisesHu,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            com.example.ui.components.TrainPilotDeleteIcon(onDelete, deleteEnabled, "journal_list_delete_${entry.id}")

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (entry.hasPr) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.primary
                    ) {
                        Text(
                            text = "PR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (entry.isOutdoorRun && entry.routeKeys.isNotEmpty()) {
                    // An icon, not an invented route thumbnail; tapping opens the stored route.
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF121C26))
                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = "Rögzített útvonal megnyitása",
                            tint = palette.success, modifier = Modifier.size(26.dp))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenDetailAlways() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Részletek",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2. EDZÉS RÉSZLETEI (Mockup 3/4 #2)
 */
@Composable
fun LogWorkoutDetailScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectDetailTab: (Int) -> Unit,
    onToggleExpandedExercise: (Int) -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenRouteMap: () -> Unit,
    onOpenWatchSync: () -> Unit,
    onOpenHealthTab: () -> Unit,
    workoutHealth: @Composable () -> Unit = {},
    deleteActions: (@Composable () -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val workout = uiState.loggedWorkouts.find { it.id == uiState.selectedLogWorkoutId }

    if (workout == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            SubScreenTopBar(
                title = strings.workoutDetailTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Nincs kiválasztott edzés",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Válassz egy befejezett edzést a Naplóból a részletek megtekintéséhez.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("log_workout_detail_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        // Summary Header Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
                    .testTag("journal_detail_header")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${strings.navProgress} · ${strings.navLog}", style = MaterialTheme.typography.labelSmall,
                                color = palette.primary, modifier = Modifier.testTag("journal_detail_location"))
                            Text(workout.titleHu, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = palette.textPrimary,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(workout.dateLabelHu, style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        com.example.ui.components.TrainPilotRedCloseButton(onClick = onBack, testTag = "subscreen_back_button")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = palette.cardBorder)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${workout.durationMinutes} perc", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("edzésidő", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                        Column {
                            Text(workout.workTimeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("munkaidő", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                        Column {
                            Text(workout.restTimeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("pihenőidő", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                    }
                }
            }
        }

        if (deleteActions != null) item { deleteActions() }

        // Sub-tabs: Gyakorlatok (5) | Statisztikák | Forrás
        item {
            val tabTitles = listOf(
                if (workout.isOutdoorRun) "Aktivitás" else "Gyakorlatok (${workout.exerciseDetails.size})",
                "Statisztikák",
                "Forrás"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabTitles.forEachIndexed { idx, label ->
                    val selected = uiState.logDetailTabIndex == idx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.primary else palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectDetailTab(idx) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) palette.onPrimary else palette.textSecondary,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        // Tab-specific content for Gyakorlatok (0) | Statisztikák (1) | Forrás (2)
        if (uiState.logDetailTabIndex == 0) {
            val details = workout.exerciseDetails
            items(details.size) { index ->
                val exDetail = details[index]
                val expanded = uiState.expandedLogExerciseIndex == index
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onToggleExpandedExercise(index) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExercisePostureMiniIllustration(
                                exerciseNameOrId = exDetail.nameHu,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exDetail.nameHu,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = exDetail.summaryPrescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SmallExerciseVideoButton(exerciseName = exDetail.nameHu)
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = palette.textSecondary
                                )
                            }
                        }

                        if (expanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = palette.cardBorder)
                            Spacer(modifier = Modifier.height(8.dp))
                            exDetail.sets.forEach { setRow ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (workout.isOutdoorRun || exDetail.measurementType == "distance") "${setRow.setNumber}. mérés" else "${setRow.setNumber}. sorozat",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = com.example.model.StudioWorkoutJournalDisplay.setLabel(setRow, exDetail.measurementType, exDetail.repUnit, workout.isOutdoorRun),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    if (setRow.completed) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Teljesített mérés",
                                            tint = palette.success,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text("Nem teljesített", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (uiState.logDetailTabIndex == 1) {
            item {
                if (workout.isOutdoorRun) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier.fillMaxWidth().trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Kültéri edzés mért adatai", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text("Aktív idő: ${workout.workTimeLabel}", color = palette.textPrimary)
                            Text("Távolság: ${if (workout.distanceKm > 0f) "%.2f km".format(java.util.Locale.forLanguageTag("hu-HU"), workout.distanceKm) else "Nincs GPS-adat"}",
                                color = palette.textPrimary)
                            Text("Átlagtempó: ${workout.paceLabel.takeUnless { it.isBlank() || it == "—" } ?: "Nincs adat"}",
                                color = palette.textPrimary)
                            Text("Pulzus és kalória: csak hiteles órás/Health Connect mérés esetén", color = palette.textSecondary)
                        }
                    }
                } else {
                    AnatomicalMuscleHeatmapCard(
                        title = "Edzés izomterhelése (${workout.titleHu})",
                        subtitle = "Elöl- és hátulnézeti anatómiai terhelés ezen az edzésen",
                        realMuscleLoads = uiState.logWorkoutMuscleLoads,
                        periodDays = null
                    )
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Adatforrás és szinkron napló", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("• Rögzítő eszköz: ${if (workout.originDevice == "watch") "Óra" else "Telefon"}", color = palette.textPrimary)
                        Text("• Wear OS adatok: ${if (workout.sourceWatch) "Edzéshez rögzítve" else "Nem érhető el"}", color = palette.textSecondary)
                        Text("• Health Connect adatok: ${if (workout.sourceHealthConnect) "Edzéshez rögzítve" else "Nem érhető el"}", color = palette.textSecondary)
                        Text("• Pulzus és kalória: csak hiteles edzésmérésből jeleníthető meg", color = palette.textSecondary)
                    }
                }
            }
        }

        // The health controller lives outside LazyColumn; scrolling never restarts provider reads.
        item(key = "workout_health") { workoutHealth() }

        // Fotók (2) & Útvonal megtekintése buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrainPilotOutlinedButton(
                    onClick = onOpenPhotos,
                    enabled = workout.photoCount > 0,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("log_detail_photos_button")
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fotók (${workout.photoCount})", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                }
                TrainPilotOutlinedButton(
                    onClick = onOpenRouteMap,
                    enabled = workout.routeKeys.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("log_detail_route_button")
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (workout.routeKeys.isEmpty()) "Nincs útvonal" else "Útvonal megtekintése", color = palette.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }

        // Egészségügyi forrás: TrainPilot óra & Health Connect (Mockup 3/4 #2 custom icons)
        item {
            Text(
                text = "Egészségügyi forrás",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .weight(1f)
                        .trainPilotBorder(1.5.dp, palette.primary, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenWatchSync() }
                        .testTag("log_detail_open_watch_sync")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MockupWatchGraphicBadge(modifier = Modifier.size(34.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TrainPilot WearOS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = if (workout.sourceWatch) "Órás edzésadat rögzítve" else "Nincs órás mérés",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.success
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .weight(1f)
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenHealthTab() }
                        .testTag("log_detail_open_health_connect")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MockupHealthConnectBadge(modifier = Modifier.size(34.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Egészségadatok",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Samsung Health / Health Connect",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. EDZÉSÚTVONAL (Mockup 3/4 #3 - Margitsziget 1,62 km)
 */
@Composable
fun LogRouteMapScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val workout = uiState.loggedWorkouts.firstOrNull { it.id == uiState.selectedLogWorkoutId }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp)
            .testTag("log_route_map_screen"),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.routeMapTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            com.example.ui.components.TrainPilotRedCloseButton(onClick = onBack, testTag = "close_route_map_button")
        }

        // High-precision Margitsziget + Duna + Vizafogó + Újlipótváros + Margit híd Map Canvas
        NativeWorkoutRouteMapBox(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            interactiveControls = true, routeKeys = workout?.routeKeys.orEmpty()
        )

        // Bottom info card + Km Splits matching Mockup 3/4 #3
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.surfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = palette.success,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = workout?.distanceKm?.takeIf { it > 0 }?.let { "%.2f km".format(it) } ?: "— km",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.success.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = workout?.paceLabel?.takeIf(String::isNotBlank) ?: "— /km",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.success,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Text(
                    text = if (workout?.routeKeys.isNullOrEmpty()) "Ehhez az edzéshez nincs rögzített útvonal." else "Privát, rögzített GPS-útvonal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = workout?.dateLabelHu ?: "Nincs dátum",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                HorizontalDivider(color = palette.cardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hitelesített részidőadat nem érhető el", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Text("—", style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 4. FEJLŐDÉS – natív fejlődés, napló és statisztikák
 * Minden fő kártya ráfér a képernyőre, és koppintásra lenyílik / becsukódik!
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressTabScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onSelectMainSection: (Int) -> Unit,
    onSelectExercise: (String) -> Unit,
    onSelectRange: (Int) -> Unit,
    onSelectVolumeBar: (Int) -> Unit,
    onSelectExpandedStatsCard: (Int) -> Unit,
    onSelectStatsSubTab: (Int) -> Unit,
    onToggleCardioFilterStepsMode: (Boolean) -> Unit,
    onOpenWorkoutDetail: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPalettePicker: () -> Unit,
    onSearchChange: (String) -> Unit = {},
    onOpenRouteMap: () -> Unit = {},
    onRefreshRecords: () -> Unit = {},
    onDeletedWorkout: (String) -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    // 3 al-oldal a Fejlődés oldalon belül: 1. 📈 Fejlődés | 2. 📄 Napló | 3. 📊 Statisztikák
    val statisticsLabel = com.repforge.app.ui.text.nativeResources(androidx.compose.ui.platform.LocalContext.current,
        uiState.language.name.lowercase()).getString(com.repforge.app.R.string.journal_statistics)
    val sections = listOf("📈 ${strings.navProgress}", "📄 ${strings.navLog}", "📊 $statisticsLabel")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("progress_tab_screen")
    ) {
        // Compact Top Sub-Tab Bar: 📈 Fejlődés | 📄 Napló | 🏋️ Erő & 1RM
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.surfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sections.forEachIndexed { idx, label ->
                    val selected = uiState.progressMainSection == idx
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) palette.primary.copy(alpha = 0.22f) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .trainPilotBorder(
                                width = if (selected) 1.dp else 0.dp,
                                color = if (selected) palette.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectMainSection(idx) }
                            .testTag("progress_section_tab_$idx")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) palette.primary else palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        when (uiState.progressMainSection) {
            // 1. AL-OLDAL (0): 📈 FEJLŐDÉS (Összes volumen, 4 lenyíló statisztika ablak, Izomcsoport-terhelés, Rekordok & Kardió)
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter, top = 2.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
                ) {
                    item {
                        MainProgressVolumeAndMuscleSection(
                            uiState = uiState,
                            onSelectRange = onSelectRange,
                            onSelectVolumeBar = onSelectVolumeBar,
                            onSelectExpandedStatsCard = onSelectExpandedStatsCard,
                            onOpenWorkoutDetail = onOpenWorkoutDetail
                        )
                    }
                }
            }

            // 2. AL-OLDAL (1): 📄 NAPLÓ (Teljes edzésnapló keresővel, alapból csukott intervallum-naptárral, edzésrészletekkel)
            1 -> {
                LogTabScreen(
                    uiState = uiState,
                    strings = strings,
                    onSearchChange = onSearchChange,
                    onOpenWorkoutDetail = onOpenWorkoutDetail,
                    onOpenRouteMap = onOpenRouteMap,
                    onOpenSettings = onOpenSettings,
                    onOpenPalettePicker = onOpenPalettePicker,
                    onDeletedWorkout = onDeletedWorkout
                )
            }

            // Statistics replaces the old standalone Strength/1RM tab; estimates remain in record details.
            else -> StudioStatisticsScreen(uiState, onSelectStatsSubTab,
                onToggleCardioFilterStepsMode, onRefreshRecords)
        }
    }
}

/**
 * Exact Compact `main` Fejlődés Layout (`Screenshot_2026-10-27-15-00-06-481_com.repforge.app.jpg`):
 * 1. Összes volumen (20 800 kg) with side-by-side 2x2 period selector (1 nap, 7 nap, 30 nap, 3 hó) + bar chart, expandable on tap!
 * 2. 2x2 Compact Expandable Metric Cards (PR rekordok 45, Edzések száma 25, Átlagos volumen 904,3 kg, Átl. edzésidő 45 min)
 * 3. Compact Side-by-Side Izomcsoport-terhelés (Front & Back bodies on left + 6 muscle bars on right)
 * 4. Compact Expandable Fejlődési javaslat card at the bottom
 */
@Composable
internal fun MainProgressVolumeAndMuscleSection(
    uiState: TrainPilotUiState,
    onSelectRange: (Int) -> Unit,
    onSelectVolumeBar: (Int) -> Unit,
    onSelectExpandedStatsCard: (Int) -> Unit,
    onOpenWorkoutDetail: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = com.example.localization.LocalTrainPilotStrings.current
    var expandedVolumeDetails by remember { mutableStateOf(false) }
    var expandedAdviceDetails by remember { mutableStateOf(false) }

    val today = remember { java.time.LocalDate.now() }
    val huFmt = remember { java.time.format.DateTimeFormatter.ofPattern("yyyy. MMM d.", java.util.Locale("hu", "HU")) }
    val shortFmt = remember { java.time.format.DateTimeFormatter.ofPattern("MMM d.", java.util.Locale("hu", "HU")) }

    val period = StudioProgressWindow(progressPeriodDays(uiState.selectedProgressRangeIndex), today)
    val periodWorkouts = remember(uiState.loggedWorkouts, period) { period.workouts(uiState.loggedWorkouts) }
    val volumeBars = remember(uiState.loggedWorkouts, period) { period.volumes(uiState.loggedWorkouts) }
    val expandedWeeklyBars = volumeBars
    val loggedTotalKg = periodWorkouts.sumOf { it.totalVolumeKg }
    val volumeChange = period.volumeChangePercent(uiState.loggedWorkouts)
    val totalVolumeText = "%,d kg".format(java.util.Locale.US, loggedTotalKg).replace(',', ' ')
    val dateSubtitle = "${period.start.format(huFmt)} – ${period.end.format(huFmt)}"
    val prCountValue = uiState.prRecords.count {
        period.contains(runCatching { java.time.LocalDate.parse(it.dateLabel.take(10)) }.getOrNull())
    }.toString()
    val workoutCountValue = periodWorkouts.size.toString()
    val avgVolumeValue = if (periodWorkouts.isEmpty()) "0 kg" else
        "${"%.1f".format(loggedTotalKg.toFloat() / periodWorkouts.size).replace('.', ',')} kg"
    val avgDurationValue = if (periodWorkouts.isEmpty()) "0 min" else
        "${periodWorkouts.sumOf { it.durationMinutes } / periodWorkouts.size} min"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 1. ÖSSZES VOLUMEN CARD: transforms into full-width Screenshot 6 "Volumen trend" chart when expanded!
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .testTag("progress_total_volume_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header row (clickable to expand/collapse between compact & Screenshot 6 large Volumen trend)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .testTag("progress_volume_expand").clickable { expandedVolumeDetails = !expandedVolumeDetails }
                        .testTag("progress_total_volume_header"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = strings.totalVolumeTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Text(
                            text = totalVolumeText,
                            modifier = Modifier.testTag("progress_volume_total"),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = dateSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    volumeChange == null -> "—"
                                    volumeChange > 0 -> "▲ +$volumeChange%"
                                    volumeChange < 0 -> "▼ $volumeChange%"
                                    else -> "0%"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = when { volumeChange == null || volumeChange == 0L -> palette.textSecondary
                                    volumeChange > 0 -> palette.success else -> palette.danger },
                                modifier = Modifier.testTag("progress_volume_change")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (expandedVolumeDetails) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Volumen grafikon lenyitása",
                                tint = palette.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.comparedToPrevPeriod,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val rangeLabels = listOf("1 nap", "7 nap", "30 nap", "3 hó")

                if (expandedVolumeDetails) {
                    // SCREENSHOT 6: EXPANDED FULL-WIDTH "Volumen trend" VIEW
                    // 1. Horizontal 4-button Period Selector Row (1 nap | 7 nap | 30 nap | 3 hó)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rangeLabels.forEachIndexed { rIdx, rLabel ->
                            val isSel = uiState.selectedProgressRangeIndex == rIdx
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .trainPilotBorder(
                                        width = 1.dp,
                                        color = if (isSel) palette.primary else palette.cardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("progress_range_$rIdx").clickable { onSelectRange(rIdx) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = rLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) palette.onPrimary else palette.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. "Volumen trend" Title + "Heti bontás" Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Volumen trend",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = if (period.days >= 30) "Heti bontás" else "Napi bontás",
                                modifier = Modifier.testTag("progress_volume_bucket_unit").padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    StudioVolumeChart(volumeBars, period, uiState.selectedVolumeBarIndex, true, onSelectVolumeBar)

                    Spacer(modifier = Modifier.height(10.dp))

                    val selectedBar = expandedWeeklyBars.getOrNull(uiState.selectedVolumeBarIndex)
                    Text(
                        text = if (selectedBar != null) {
                            "Kiválasztott ${if (period.days == 90) "hét" else "nap"} (${selectedBar.first}): ${selectedBar.second} kg összvolumen"
                        } else {
                            "Válassz egy oszlopot a részletekhez."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedBar != null) palette.primary else palette.textSecondary
                    )
                } else {
                    // COMPACT SIDE-BY-SIDE VIEW: Left = 2x2 Period Buttons, Right = Mini Y-Axis + Bar Chart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(0.45f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0, 1).forEach { rIdx ->
                                    val isSel = uiState.selectedProgressRangeIndex == rIdx
                                    Surface(
                                        shape = RoundedCornerShape(11.dp),
                                        color = if (isSel) palette.primary else palette.surfaceElevated,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .trainPilotBorder(
                                                width = 1.dp,
                                                color = if (isSel) palette.primary else palette.cardBorder,
                                                shape = RoundedCornerShape(11.dp)
                                            )
                                            .clip(RoundedCornerShape(11.dp))
                                            .testTag("progress_range_$rIdx").clickable { onSelectRange(rIdx) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = rangeLabels[rIdx],
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) palette.onPrimary else palette.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(2, 3).forEach { rIdx ->
                                    val isSel = uiState.selectedProgressRangeIndex == rIdx
                                    Surface(
                                        shape = RoundedCornerShape(11.dp),
                                        color = if (isSel) palette.primary else palette.surfaceElevated,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .trainPilotBorder(
                                                width = 1.dp,
                                                color = if (isSel) palette.primary else palette.cardBorder,
                                                shape = RoundedCornerShape(11.dp)
                                            )
                                            .clip(RoundedCornerShape(11.dp))
                                            .testTag("progress_range_$rIdx").clickable { onSelectRange(rIdx) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = rangeLabels[rIdx],
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) palette.onPrimary else palette.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box(Modifier.weight(0.55f).height(96.dp)) {
                            StudioVolumeChart(volumeBars, period, uiState.selectedVolumeBarIndex, false) { index ->
                                onSelectVolumeBar(index)
                                expandedVolumeDetails = true
                            }
                        }
                    }
                }
            }
        }

        // 2. 2x2 COMPACT EXPANDABLE METRIC CARDS (Live data from uiState)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpandableStatSummaryTile(
                icon = Icons.Default.EmojiEvents,
                title = strings.prRecordsTitle,
                value = prCountValue,
                dashSuffix = "—",
                subtitle = null,
                selected = uiState.expandedStatsCardIndex == 0,
                onClick = { onSelectExpandedStatsCard(0) },
                modifier = Modifier.weight(1f)
            )
            ExpandableStatSummaryTile(
                icon = Icons.Default.CalendarMonth,
                title = strings.workoutCountTitle,
                value = workoutCountValue,
                dashSuffix = "—",
                subtitle = null,
                selected = uiState.expandedStatsCardIndex == 1,
                onClick = { onSelectExpandedStatsCard(1) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpandableStatSummaryTile(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = strings.avgVolumeTitle,
                value = avgVolumeValue,
                dashSuffix = "—",
                subtitle = "súlyzós edzésenként",
                selected = uiState.expandedStatsCardIndex == 2,
                onClick = { onSelectExpandedStatsCard(2) },
                modifier = Modifier.weight(1f)
            )
            ExpandableStatSummaryTile(
                icon = Icons.Default.Schedule,
                title = strings.avgDurationTitle,
                value = avgDurationValue,
                dashSuffix = null,
                subtitle = null,
                selected = uiState.expandedStatsCardIndex == 3,
                onClick = { onSelectExpandedStatsCard(3) },
                modifier = Modifier.weight(1f)
            )
        }

        // Expandable Dropdown Card when any of the 4 tiles is tapped (collapsed by default so everything fits on screen!)
        AnimatedVisibility(visible = uiState.expandedStatsCardIndex in 0..3) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (uiState.expandedStatsCardIndex) {
                        0 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PR rekordok ($prCountValue)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.clickable { onSelectExpandedStatsCard(0) }
                                ) {
                                    Text("Bezárás", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                    Icon(imageVector = Icons.Default.ExpandLess, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (uiState.prRecords.isEmpty()) {
                                Text(
                                    text = "Még nincs rögzített PR rekord. Fejezz be egy edzést a PR-ek számításához!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            } else {
                                uiState.prRecords.take(4).forEach { pr ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(palette.surfaceElevated)
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(pr.exerciseNameHu, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                            Text(pr.typeLabelHu, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                        }
                                        SmallExerciseVideoButton(exerciseName = pr.exerciseNameHu, compactIconOnly = true)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${pr.previousLabel} → ${pr.newLabel}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                                    }
                                }
                            }
                        }
                        1 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Edzések száma ($workoutCountValue)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.clickable { onSelectExpandedStatsCard(1) }
                                ) {
                                    Text("Bezárás", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                    Icon(imageVector = Icons.Default.ExpandLess, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (periodWorkouts.isEmpty()) {
                                Text(
                                    text = "Még nincs naplózott edzés.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            } else {
                                periodWorkouts.take(4).forEach { w ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(palette.surfaceElevated)
                                            .clickable { onOpenWorkoutDetail(w.id) }
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${w.dateLabelHu} • ${w.titleHu}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("${w.durationMinutes} min >", style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        2 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Átlagos volumen • $avgVolumeValue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.clickable { onSelectExpandedStatsCard(2) }
                                ) {
                                    Text("Bezárás", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                    Icon(imageVector = Icons.Default.ExpandLess, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (periodWorkouts.isEmpty()) {
                                Text("Még nincs rögzített súlyzós volumen.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            } else {
                                periodWorkouts.take(4).forEach { w ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(palette.surfaceElevated)
                                            .clickable { onOpenWorkoutDetail(w.id) }
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${w.dateLabelHu} • ${w.titleHu}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                        Text("${w.totalVolumeKg} kg >", style = MaterialTheme.typography.labelMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Átlagos edzésidő • $avgDurationValue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.clickable { onSelectExpandedStatsCard(3) }
                                ) {
                                    Text("Bezárás", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                    Icon(imageVector = Icons.Default.ExpandLess, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (periodWorkouts.isEmpty()) {
                                Text("Még nincs rögzített edzésidő.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            } else {
                                periodWorkouts.take(4).forEach { w ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(palette.surfaceElevated)
                                            .clickable { onOpenWorkoutDetail(w.id) }
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${w.dateLabelHu} • ${w.titleHu}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                        Text("${w.durationMinutes} min >", style = MaterialTheme.typography.labelMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. COMPACT SIDE-BY-SIDE IZOMCSOPORT-TERHELÉS CARD (Front & Back bodies on left + 6 muscle bars on right)
        AnatomicalMuscleHeatmapCard(
            title = strings.muscleLoadTitle,
            subtitle = "A színek és arányok az időszak legterheltebb izomcsoportjához viszonyítanak, nem fáradtságot mérnek.",
            realMuscleLoads = uiState.realMuscleLoads,
            periodDays = progressPeriodDays(uiState.selectedProgressRangeIndex),
            onPeriodSelected = { periodDays ->
                val rIdx = when (periodDays) {
                    1 -> 0
                    7 -> 1
                    30 -> 2
                    else -> 3
                }
                onSelectRange(rIdx)
            },
            onOpenExerciseDetail = onOpenWorkoutDetail
        )

        // 4. COMPACT EXPANDABLE "Fejlődési javaslat" CARD AT BOTTOM (`Screenshot_2026-10-27-15-00-06-481_com.repforge.app.jpg`)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(
                    width = 1.dp,
                    color = if (expandedAdviceDetails) palette.primary else palette.cardBorder,
                    shape = RoundedCornerShape(18.dp)
                )
                .clip(RoundedCornerShape(18.dp))
                .clickable { expandedAdviceDetails = !expandedAdviceDetails }
                .testTag("progress_advice_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val topMuscle = uiState.realMuscleLoads.maxByOrNull { it.percentage }?.takeIf { it.percentage > 0 }
                        Text(
                            text = strings.progressAdviceTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (topMuscle != null) {
                                "A legtöbb sorozatterhelést ez az izomcsoport kapta: ${topMuscle.nameHu} (${topMuscle.setsText})."
                            } else {
                                "Rögzíts edzéseket az izomcsoport-arányok és egyéni fejlődési javaslatok kiszámításához."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = if (expandedAdviceDetails) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.primary
                    )
                }

                AnimatedVisibility(visible = expandedAdviceDetails) {
                    val lowMuscle = uiState.realMuscleLoads.minByOrNull { it.percentage }
                    val latestPr = uiState.prRecords.firstOrNull()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HorizontalDivider(color = palette.cardBorder)
                        Text(
                            text = if (lowMuscle != null && periodWorkouts.isNotEmpty()) {
                                "• Egyensúly javaslat: A(z) ${lowMuscle.nameHu} (${lowMuscle.setsText}) terhelését érdemes +2 munkasorozattal növelni a következő héten."
                            } else {
                                "• Egyensúly javaslat: Még nincs rögzített izomcsoport-terhelés az elmúlt időszakban."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (latestPr != null) {
                                "• Legutóbbi csúcsteljesítmény: ${latestPr.exerciseNameHu} (${latestPr.newLabel} • ${latestPr.typeLabelHu})."
                            } else {
                                "• Progresszió: A rögzített sorozatok után itt jelenik meg a következő súlyemelési javaslat."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableStatSummaryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    dashSuffix: String?,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surfaceCard,
        modifier = modifier
            .trainPilotBorder(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) palette.primary else palette.cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(palette.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1
                    )
                    if (dashSuffix != null) {
                        Text(
                            text = dashSuffix,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = if (selected) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
