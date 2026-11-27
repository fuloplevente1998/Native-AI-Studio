package com.example.ui.screens

import com.example.ui.components.TrainPilotListDivider
import com.example.ui.components.trainPilotListPanel

import com.example.ui.theme.liquidDropMaterial

import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.glassTile

import com.example.ui.theme.glassHeaderMaterial
import com.example.ui.theme.trainPilotScreenBackdrop
import androidx.compose.runtime.key
import androidx.compose.foundation.lazy.rememberLazyListState

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotFrame

import com.example.model.localWorkoutDate

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import com.example.ui.theme.glassTintedTile
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import com.example.localization.LocalizedStrings
import com.example.model.LoggedWorkoutEntry
import com.example.ui.theme.LocalGlassTopInset
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
    onOpenRouteMap: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPalettePicker: () -> Unit,
    onDeletedWorkout: (String) -> Unit = {}
) {
    val deleteModel: com.repforge.app.ui.journal.JournalViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key = "studio-history-list-deletion")
    val deleteState by deleteModel.uiState.collectAsState()
    com.example.ui.components.StudioWorkoutDeleteConfirmation(null, onDeletedWorkout, deleteModel)
    if (deleteState.operationFailed && deleteState.workoutToDelete == null) AlertDialog(
        onDismissRequest = deleteModel::clearOperationError,
        title = { Text(trL(uiState.language, "Az edzés adatai nem érhetők el", "Workout data unavailable", "Trainingsdaten nicht verfügbar", "Datele antrenamentului nu sunt disponibile")) },
        text = { Text(trL(uiState.language, "A napló közben frissülhetett vagy adatcsere van folyamatban. Nyisd meg újra a műveletet.", "The log may have updated or data replacement is in progress. Please reopen the action.", "Das Protokoll wurde aktualisiert oder ein Datenaustausch läuft. Öffne die Aktion erneut.", "Jurnalul s-a actualizat sau un schimb de date este în curs. Redeschide acțiunea.")) },
        confirmButton = { TextButton(onClick = deleteModel::clearOperationError) { Text(trL(uiState.language, "Rendben", "OK", "OK", "OK")) } },
        modifier = Modifier.testTag("journal_list_delete_error")
    )

    val palette = LocalTrainPilotPalette.current
    val today = remember { java.time.LocalDate.now() }
    var currentYearMonth by androidx.compose.runtime.saveable.rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver<java.time.YearMonth, String>(
            save = { it.toString() }, restore = { java.time.YearMonth.parse(it) })
    ) { mutableStateOf(java.time.YearMonth.from(today)) }
    var filterAllMonths by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    var intervalStartDay by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Int?>(null) }
    var intervalEndDay by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Int?>(null) }
    var showInlineCalendar by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val monthNames = when (uiState.language) {
        com.example.model.AppLanguage.HU -> listOf("január", "február", "március", "április", "május", "június", "július", "augusztus", "szeptember", "október", "november", "december")
        com.example.model.AppLanguage.EN -> listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        com.example.model.AppLanguage.DE -> listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
        com.example.model.AppLanguage.RO -> listOf("ianuarie", "februarie", "martie", "aprilie", "mai", "iunie", "iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie")
    }
    val huMonthShort = listOf(
        "jan.", "febr.", "márc.", "ápr.", "máj.", "jún.",
        "júl.", "aug.", "szept.", "okt.", "nov.", "dec."
    )
    val monthShortDisplay = when (uiState.language) {
        com.example.model.AppLanguage.HU -> huMonthShort
        com.example.model.AppLanguage.EN -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        com.example.model.AppLanguage.DE -> listOf("Jan.", "Feb.", "Mär.", "Apr.", "Mai", "Jun.", "Jul.", "Aug.", "Sep.", "Okt.", "Nov.", "Dez.")
        com.example.model.AppLanguage.RO -> listOf("ian.", "feb.", "mar.", "apr.", "mai", "iun.", "iul.", "aug.", "sep.", "oct.", "nov.", "dec.")
    }
    val monthFullHu = "${currentYearMonth.year}. ${monthNames[currentYearMonth.monthValue - 1]}"
    val monthPrefixHu = huMonthShort[currentYearMonth.monthValue - 1]
    val monthPrefixDisplay = monthShortDisplay[currentYearMonth.monthValue - 1]

    val rangeMinDay = if (intervalStartDay != null && intervalEndDay != null) {
        minOf(intervalStartDay!!, intervalEndDay!!)
    } else intervalStartDay
    val rangeMaxDay = if (intervalStartDay != null && intervalEndDay != null) {
        maxOf(intervalStartDay!!, intervalEndDay!!)
    } else intervalStartDay

    val currentFilterLabel = when {
        intervalStartDay != null && intervalEndDay != null ->
            "$monthPrefixDisplay ${rangeMinDay}. – $monthPrefixDisplay ${rangeMaxDay}."
        intervalStartDay != null ->
            trL(uiState.language, "Innentől: $monthPrefixDisplay ${intervalStartDay}. (válassz zárónapot)", "From: $monthPrefixDisplay ${intervalStartDay}. (select end day)", "Ab: $monthPrefixDisplay ${intervalStartDay}. (Endtag wählen)", "De la: $monthPrefixDisplay ${intervalStartDay}. (alege ziua finală)")
        filterAllMonths ->
            trL(uiState.language, "Összes hónap (${today.year})", "All months (${today.year})", "Alle Monate (${today.year})", "Toate lunile (${today.year})")
        else ->
            monthFullHu
    }

    val filteredLogs = remember(uiState.loggedWorkouts, uiState.logSearchQuery, uiState.language,
        uiState.programs, uiState.exercises, rangeMinDay, rangeMaxDay, currentYearMonth, filterAllMonths, monthPrefixHu) {
        uiState.loggedWorkouts.filter { entry ->
        val matchesSearch = uiState.logSearchQuery.isBlank() ||
            entry.titleHu.contains(uiState.logSearchQuery, ignoreCase = true) ||
            entry.localizedTitle(uiState.language, uiState.programs, uiState.exercises).contains(uiState.logSearchQuery, ignoreCase = true) ||
            entry.summaryExercisesHu.contains(uiState.logSearchQuery, ignoreCase = true) ||
            entry.localizedSummaryExercises(uiState.language, uiState.exercises).contains(uiState.logSearchQuery, ignoreCase = true)

        val workoutDate = entry.localWorkoutDate()
        val matchesIntervalOrMonth = when {
            rangeMinDay != null && rangeMaxDay != null -> {
                val startDate = currentYearMonth.atDay(rangeMinDay.coerceIn(1, currentYearMonth.lengthOfMonth()))
                val endDate = currentYearMonth.atDay(rangeMaxDay.coerceIn(1, currentYearMonth.lengthOfMonth()))
                workoutDate != null && !workoutDate.isBefore(startDate) && !workoutDate.isAfter(endDate)
            }
            filterAllMonths -> true
            else -> if (workoutDate != null) {
                workoutDate.year == currentYearMonth.year && workoutDate.monthValue == currentYearMonth.monthValue
            } else {
                entry.dateLabelHu.contains(monthPrefixHu, ignoreCase = true) ||
                    entry.dateLabelHu.contains("${currentYearMonth.year}. ${"%02d".format(currentYearMonth.monthValue)}")
            }
        }
        matchesSearch && matchesIntervalOrMonth
        }
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
                    text = trL(uiState.language, "Napló intervallum-szűrő", "Log interval filter", "Protokoll-Intervallfilter", "Filtru interval jurnal"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = trL(uiState.language, "Jelölj ki két napot az időszak megtekintéséhez.", "Select two days to view a date range.", "Wähle zwei Tage, um den Zeitraum anzuzeigen.", "Selectează două zile pentru a vedea intervalul."),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "${trL(uiState.language, "Aktuális szűrő", "Current filter", "Aktueller Filter", "Filtru curent")}: $currentFilterLabel",
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
                    Text(trL(uiState.language, "Rendben", "OK", "OK", "OK"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                com.repforge.app.ui.theme.TrainPilotNegativeButton(
                    onClick = {
                        intervalStartDay = null
                        intervalEndDay = null
                        filterAllMonths = true
                        showDatePickerDialog = false
                    }
                ) {
                    Text(trL(uiState.language, "Szűrő törlése", "Clear filter", "Filter löschen", "Șterge filtrul"), color = androidx.compose.material3.LocalContentColor.current)
                }
            }
        )
    }

    if (showInlineCalendar) {
        // Shared period sheet (same as the Health journal): Today / Clear emphasized, 6 rows, swipe months.
        val logWeekdayHeaders = when (uiState.language) {
            com.example.model.AppLanguage.HU -> listOf("H", "K", "Sze", "Cs", "P", "Szo", "V")
            com.example.model.AppLanguage.EN -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            com.example.model.AppLanguage.DE -> listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
            com.example.model.AppLanguage.RO -> listOf("Lun", "Mar", "Mie", "Joi", "Vin", "Sâm", "Dum")
        }
        val loggedDates = remember(uiState.loggedWorkouts) { uiState.loggedWorkouts.mapNotNull { it.localWorkoutDate() }.toSet() }
        com.example.ui.components.TrainPilotRangeCalendarSheet(
            tagPrefix = "log",
            title = trL(uiState.language, "Napló időszak", "Log period", "Protokollzeitraum", "Perioada jurnalului"),
            monthTitle = monthFullHu,
            hint = when {
                intervalStartDay == null -> trL(uiState.language, "Jelölj ki két napot az időszak megtekintéséhez.", "Select two days to view a date range.", "Wähle zwei Tage, um den Zeitraum anzuzeigen.", "Selectează două zile pentru a vedea intervalul.")
                intervalEndDay == null -> trL(uiState.language, "Kezdőnap: $monthPrefixDisplay ${intervalStartDay}. • Jelöld ki a zárónapot.", "From: $monthPrefixDisplay ${intervalStartDay}. • Select the end day.", "Ab: $monthPrefixDisplay ${intervalStartDay}. • Wähle den Endtag.", "De la: $monthPrefixDisplay ${intervalStartDay}. • Selectează ziua finală.")
                else -> "${trL(uiState.language, "Intervallum", "Interval", "Intervall", "Interval")}: $monthPrefixDisplay ${rangeMinDay}. – $monthPrefixDisplay ${rangeMaxDay}."
            },
            weekdayHeaders = logWeekdayHeaders,
            month = currentYearMonth,
            start = intervalStartDay?.let { currentYearMonth.atDay(it.coerceIn(1, currentYearMonth.lengthOfMonth())) },
            end = intervalEndDay?.let { currentYearMonth.atDay(it.coerceIn(1, currentYearMonth.lengthOfMonth())) },
            today = today,
            hasMark = { it in loggedDates },
            todayLabel = trL(uiState.language, "Mai nap", "Today", "Heute", "Azi"),
            clearLabel = trL(uiState.language, "Szűrő törlése", "Clear filter", "Filter löschen", "Șterge filtrul"),
            doneLabel = trL(uiState.language, "Rendben", "OK", "OK", "OK"),
            onMonthChange = {
                filterAllMonths = false
                intervalStartDay = null
                intervalEndDay = null
                currentYearMonth = it
            },
            onDayClick = { handleDayClick(it.dayOfMonth) },
            onToday = {
                currentYearMonth = java.time.YearMonth.from(today)
                filterAllMonths = false
                intervalStartDay = today.dayOfMonth
                intervalEndDay = today.dayOfMonth
            },
            onClear = {
                intervalStartDay = null
                intervalEndDay = null
                filterAllMonths = true
            },
            onDismiss = { showInlineCalendar = false }
        )
    }

    val logListState = androidx.compose.foundation.lazy.rememberLazyListState()

    com.example.ui.theme.GlassScrollChrome(
        modifier = Modifier.fillMaxSize().trainPilotScreenBackdrop(palette)
            .padding(start = 6.dp, end = 6.dp, top = LocalGlassTopInset.current)
            .clipToBounds().testTag("log_fixed_chrome"),
        rootInset = 0.dp,
        localWindow = true,
        top = {
            val shape = RoundedCornerShape(18.dp)
            Surface(
                shape = shape,
                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceCard,
                modifier = Modifier.fillMaxWidth()
                    .glassHeaderMaterial(palette, shape, softenUnderlap = true)
                    .trainPilotBorder(1.dp, palette.cardBorder, shape)
                    .testTag("log_fixed_header")
            ) {
                Column(Modifier.padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 6.dp)) {
                    Box {
                        Box(Modifier.padding(bottom = TrainPilotCardSpacing.CardGap)) {
                            CompactJournalSearchField(uiState.logSearchQuery, onSearchChange, strings.logSearchHint)
                        }
                    }

                    // Interactive Month & Date selector row (< | 2026. október v | >)
                    Box {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = TrainPilotCardSpacing.CardGap),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceCard,
                                modifier = Modifier
                                    .size(48.dp)
                                    .liquidDropMaterial(palette, CircleShape, GlassMaterialRole.Button)
                                    .trainPilotBorder(1.dp, palette.cardBorder, CircleShape)
                                    .trainPilotClickable(CircleShape) {
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
                                        contentDescription = trL(uiState.language, "Előző hónap", "Previous month", "Vorheriger Monat", "Luna precedentă"),
                                        tint = palette.primary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.glassTintedTile(palette.surfaceCard),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                                    .trainPilotGlassRim(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Input)
                                    .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .trainPilotClickable(RoundedCornerShape(14.dp)) { showInlineCalendar = !showInlineCalendar }
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
                                        contentDescription = trL(uiState.language, "Dátumválasztó megnyitása", "Open date picker", "Datumsauswahl öffnen", "Deschide selectorul de dată"),
                                        tint = palette.primary
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceCard,
                                modifier = Modifier
                                    .size(48.dp)
                                    .liquidDropMaterial(palette, CircleShape, GlassMaterialRole.Button)
                                    .trainPilotBorder(1.dp, palette.cardBorder, CircleShape)
                                    .trainPilotClickable(CircleShape) {
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
                                        contentDescription = trL(uiState.language, "Következő hónap", "Next month", "Nächster Monat", "Luna următoare"),
                                        tint = palette.primary
                                    )
                                }
                            }
                        }
                    }

                    Box {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = TrainPilotCardSpacing.CardGap),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trL(
                                    uiState.language,
                                    "$currentFilterLabel • ${filteredLogs.size} rögzített edzés",
                                    "$currentFilterLabel • ${filteredLogs.size} logged workouts",
                                    "$currentFilterLabel • ${filteredLogs.size} protokollierte Trainings",
                                    "$currentFilterLabel • ${filteredLogs.size} antrenamente înregistrate"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            // 2779: one tap back to the whole list, without opening the calendar (also after a month change).
                            if (!filterAllMonths) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.glassTile(palette.surfaceCard),
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                                        .trainPilotClickable(RoundedCornerShape(10.dp)) {
                                            intervalStartDay = null
                                            intervalEndDay = null
                                            filterAllMonths = true
                                            currentYearMonth = java.time.YearMonth.from(today)
                                        }
                                        .testTag("log_clear_filter_chip")
                                ) {
                                    Text(
                                        text = "✕ " + trL(uiState.language, "Szűrő törlése", "Clear filter", "Filter löschen", "Șterge filtrul"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { scrollModifier, topInset, bottomInset ->
        com.example.ui.components.TrainPilotScrollableListBox(
            lazyListState = logListState,
            modifier = scrollModifier
        ) {
            LazyColumn(
                state = logListState,
                modifier = Modifier.fillMaxSize().trainPilotListPanel().testTag("log_tab_screen"),
                contentPadding = PaddingValues(start = TrainPilotCardSpacing.ScreenGutter,
                    end = TrainPilotCardSpacing.ScreenGutter, top = 4.dp + topInset, bottom = 24.dp + bottomInset)
            ) {
        if (filteredLogs.isEmpty()) {
            item(key = "log_empty", contentType = "empty") {
                Text(
                    text = trL(
                        uiState.language,
                        "Nincs rögzített edzés a kiválasztott időszakban.",
                        "No logged workouts in the selected period.",
                        "Keine protokollierten Trainings im ausgewählten Zeitraum.",
                        "Niciun antrenament înregistrat în perioada selectată."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .testTag("log_empty_period_message")
                )
            }
        }

        itemsIndexed(filteredLogs, key = { _, entry -> "workout_${entry.id}" }, contentType = { _, _ -> "workout" }) { index, logEntry ->
            Column(Modifier.fillMaxWidth()) {
                if (index > 0) TrainPilotListDivider()
                LogWorkoutCard(
                    entry = logEntry,
                    onClick = { onOpenRouteMap(logEntry.id) },
                    onOpenDetailAlways = { onOpenWorkoutDetail(logEntry.id) },
                    onDelete = { deleteModel.requestDeleteWorkout(logEntry.id) },
                    deleteEnabled = !deleteState.deleting,
                    programs = uiState.programs,
                    exercises = uiState.exercises
                )
            }
        }
            }
        }
    }
}

/** Compact, independently testable search that keeps the keyboard text-input semantics. */
@Composable
internal fun CompactJournalSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val palette = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(14.dp)
    var focused by remember { mutableStateOf(false) }
    val edge = remember(palette.cardBorder, focused) {
        Brush.verticalGradient(listOf(
            palette.cardBorder.copy(alpha = if (focused) .95f else .65f),
            palette.cardBorder.copy(alpha = if (focused) .65f else .30f)
        ))
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp)
            .clip(shape)
            .background(palette.glassTintedTile(palette.surfaceCard))
            .then(if (palette.isLiquidGlass) Modifier.border(1.dp, edge, shape)
                else if (focused) Modifier.trainPilotFrame(1.dp, palette.primary, shape)
                else Modifier.trainPilotFrame(1.dp, palette.cardBorder, shape))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).height(48.dp)
                .onFocusChanged { focused = it.isFocused }.testTag("log_search_input"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            cursorBrush = SolidColor(palette.primary),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.textPrimary),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = palette.textMuted,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
internal fun LogWorkoutCard(
    entry: LoggedWorkoutEntry,
    onClick: () -> Unit,
    onOpenDetailAlways: () -> Unit,
    onDelete: () -> Unit,
    deleteEnabled: Boolean,
    programs: List<com.example.model.TrainingProgramModel> = emptyList(),
    exercises: List<com.example.model.ExerciseItem> = emptyList()
) {
    val palette = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
            .trainPilotClickable(RoundedCornerShape(8.dp), stretchOnDrag = true) { onOpenDetailAlways() }
            .testTag("log_card_${entry.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(if (entry.isOutdoorRun) palette.success.copy(alpha = 0.16f)
                        else palette.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.isOutdoorRun) Icons.AutoMirrored.Filled.DirectionsRun
                        else Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = if (entry.isOutdoorRun) palette.success else palette.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                val lang = com.example.localization.LocalTrainPilotStrings.current.language
                Text(entry.dateLabelHu,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(remember(entry, lang, programs, exercises) { entry.localizedTitle(lang, programs, exercises) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (entry.originDevice == "external") {
                        // #359: the source is shown only in the workout's Forrás tab, never in this line.
                        "${entry.durationMinutes} ${trL(lang, "perc", "min", "Min.", "min")}"
                    } else if (entry.isOutdoorRun) {
                        "${entry.durationMinutes} ${trL(lang, "perc", "min", "Min.", "min")} · ${entry.distanceKm} km · ${entry.paceLabel}"
                    } else {
                        "${entry.durationMinutes} ${trL(lang, "perc", "min", "Min.", "min")} · ${entry.totalSets} ${trL(lang, "sorozat", "sets", "Sätze", "serii")} · ${entry.totalVolumeKg} kg"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 2
                )
            }
            // A route action belongs to the card content, before the trailing destructive action.
            if (entry.isOutdoorRun && entry.routeKeys.isNotEmpty()) {
                val lang = com.example.localization.LocalTrainPilotStrings.current.language
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(48.dp).testTag("journal_route_map_${entry.id}")
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = trL(lang, "Rögzített útvonal megnyitása", "Open recorded route", "Aufgezeichnete Route öffnen", "Deschide traseul înregistrat"),
                        tint = palette.success,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            // Photo indicator badge for workouts that have attached photos
            if (entry.photoCount > 0) {
                val lang = com.example.localization.LocalTrainPilotStrings.current.language
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.glassTile(palette.surfaceElevated, elevated = true),
                    modifier = Modifier
                        .trainPilotClickable(RoundedCornerShape(8.dp)) { onOpenDetailAlways() }
                        .testTag("journal_photos_badge_${entry.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = trL(lang, "Csatolt fotók", "Attached photos", "Angehängte Fotos", "Fotografii atașate"),
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${entry.photoCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                    }
                }
            }
            // The whole row opens details on a separate screen; a trailing arrow is redundant.
            com.example.ui.components.TrainPilotDeleteIcon(
                onDelete, deleteEnabled, "journal_list_delete_${entry.id}", compactVisual = true
            )
        }
    }
}
