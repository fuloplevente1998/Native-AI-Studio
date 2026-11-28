package com.example.ui.screens

import com.example.ui.components.TrainPilotIconButton

import com.example.ui.theme.liquidDropMaterial
import com.example.model.StudioCalendarDates
import com.repforge.app.ui.theme.TrainPilotGlassPrimaryButton

import com.example.ui.theme.glassCardFill
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.trainPilotGlassRim
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import com.repforge.app.ui.theme.trainPilotClickable
import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.TrainPilotFrames

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.components.StudioAdaptiveControlRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEventStatus
import com.example.model.AppLanguage
import com.example.model.SubScreen
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

internal val calendarDurationOptions = (15..240 step 15).map(Int::toString)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CalendarPlannerScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onOpenReplanModal: (Boolean) -> Unit,
    onApplyReplan: (String, String, String) -> Unit,
    onOpenWorkoutDetail: (String) -> Unit = {},
    onUpdatePlanningParams: (String, String, String, String, Int, Int) -> Unit = { _, _, _, _, _, _ -> },
    onPlanMonthlyWorkouts: () -> Unit = {},
    onModifyCalendarEvent: (String, String, String, String, Int) -> Unit = { _, _, _, _, _ -> },
    onMoveCalendarEvent: (String, Int, Boolean) -> Unit = { _, _, _ -> },
    onDeleteCalendarEvent: (String) -> Unit = {},
    onAddPlannedWorkoutOnDay: (Int, String, String, Int) -> Unit = { _, _, _, _ -> },
    onUndoCalendarAction: () -> Unit = {},
    onSelectMonth: (String) -> Unit = {},
    onActivateProgram: (String) -> Unit = {},
    onRequestClearCalendar: () -> Unit = {},
    onConfirmClearCalendar: () -> Unit = {},
    onCancelClearCalendar: () -> Unit = {},
    onStartPlannedWorkout: ((String) -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val today = remember { java.time.LocalDate.now() }
    val displayedYearMonth = java.time.YearMonth.parse(state.calendarYearMonth)

    val huMonthNames = when (state.language) {
        com.example.model.AppLanguage.HU -> listOf("Január", "Február", "Március", "Április", "Május", "Június", "Július", "Augusztus", "Szeptember", "Október", "November", "December")
        com.example.model.AppLanguage.EN -> listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        com.example.model.AppLanguage.DE -> listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
        com.example.model.AppLanguage.RO -> listOf("Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie", "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie")
    }
    val monthHeaderText = "${displayedYearMonth.year}. ${huMonthNames[displayedYearMonth.monthValue - 1]}"
    var openedDay by androidx.compose.runtime.saveable.rememberSaveable(state.calendarYearMonth) { mutableStateOf<Int?>(null) }
    val selectedDay = openedDay ?: state.selectedCalendarDay
    val dayEvents = state.calendarEvents.filter { it.dayOfMonth == selectedDay && it.dateIso.startsWith(displayedYearMonth.toString()) }
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()
    val activeProgramTitle = activeProgram?.localizedName(state.language) ?: trW(state.language, "Otthoni A/B (személyre szabott)", "Home A/B (personalized)", "Zuhause A/B (personalisiert)", "Acasă A/B (personalizat)")

    var editingEvent by remember { mutableStateOf<com.example.model.CalendarDayEvent?>(null) }
    var movingEvent by remember { mutableStateOf<com.example.model.CalendarDayEvent?>(null) }
    var activeFieldDialog by remember { mutableStateOf<String?>(null) }

    val localizeCadenceLabel: (String) -> String = { raw ->
        when {
            raw.contains("Minden második", ignoreCase = true) -> trW(state.language, "Minden második nap", "Every second day", "Jeden zweiten Tag", "La fiecare două zile")
            raw.contains("Heti 3", ignoreCase = true) -> trW(state.language, "Heti 3 nap (H-Sze-P)", "3 days/week (M-W-F)", "3 Tage/Woche (Mo-Mi-Fr)", "3 zile/săpt. (L-Mi-V)")
            raw.contains("Heti 4", ignoreCase = true) -> trW(state.language, "Heti 4 nap", "4 days/week", "4 Tage/Woche", "4 zile/săpt.")
            raw.contains("Minden nap", ignoreCase = true) -> trW(state.language, "Minden nap", "Every day", "Jeden Tag", "În fiecare zi")
            else -> raw
        }
    }
    val localizeFirstDayLabel: (String) -> String = { raw ->
        when {
            raw.contains("Automatikus", ignoreCase = true) -> trW(state.language, "Automatikus", "Automatic", "Automatisch", "Automat")
            raw.contains("B", ignoreCase = true) -> trW(state.language, "B nap", "Day B", "Tag B", "Ziua B")
            else -> trW(state.language, "A nap", "Day A", "Tag A", "Ziua A")
        }
    }

    // 1. Modify Event Dialog
    editingEvent?.let { ev ->
        var splitSel by remember(ev.id) { mutableStateOf(ev.splitBadge) }
        var timeSel by remember(ev.id) { mutableStateOf(ev.timeLabel) }
        var durSel by remember(ev.id) { mutableIntStateOf(ev.durationMinutes) }
        com.example.ui.components.TrainPilotFormSheet(
            onDismissRequest = { editingEvent = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            title = {
                Text(
                    text = trW(
                        state.language,
                        "Edzésnap módosítása (${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${ev.dayOfMonth}.)",
                        "Edit workout day (${displayedYearMonth.year}-${displayedYearMonth.monthValue.toString().padStart(2, '0')}-${ev.dayOfMonth.toString().padStart(2, '0')})",
                        "Trainingstag bearbeiten (${ev.dayOfMonth.toString().padStart(2, '0')}.${displayedYearMonth.monthValue.toString().padStart(2, '0')}.${displayedYearMonth.year})",
                        "Modifică ziua de antrenament (${ev.dayOfMonth.toString().padStart(2, '0')}.${displayedYearMonth.monthValue.toString().padStart(2, '0')}.${displayedYearMonth.year})"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(trW(state.language, "Edzésnap típusa (A / B):", "Workout day type (A / B):", "Trainingstag-Typ (A / B):", "Tipul zilei de antrenament (A / B):"), style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    if (palette.isLiquidGlass) {
                        val splits = listOf("A", "B")
                        com.example.ui.components.LiquidSegmentedTabs(splits.map { sp -> trW(state.language, "Alap $sp", "Day $sp", "Tag $sp", "Ziua $sp") },
                            splits.indexOf(splitSel), { splitSel = splits[it] }, "calendar_split_track") { _, label, _ ->
                            com.example.ui.components.TrainPilotFitText(label)
                        }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A", "B").forEach { sp ->
                            val isSel = splitSel == sp
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .trainPilotClickable(RoundedCornerShape(10.dp)) { splitSel = sp }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text(trW(state.language, "Alap $sp", "Day $sp", "Tag $sp", "Ziua $sp"), fontWeight = FontWeight.Bold, color = if (isSel) palette.onPrimary else palette.textPrimary)
                                }
                            }
                        }
                    }
                    Text(trW(state.language, "Időpont:", "Time:", "Uhrzeit:", "Ora:"), style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    if (palette.isLiquidGlass) {
                        val times = listOf("08:00", "10:00", "16:00", "18:00")
                        com.example.ui.components.LiquidSegmentedTabs(times, times.indexOf(timeSel),
                            { timeSel = times[it] }, "calendar_time_track") { _, label, _ ->
                            com.example.ui.components.TrainPilotFitText(label, style = MaterialTheme.typography.labelSmall)
                        }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("08:00", "10:00", "16:00", "18:00").forEach { tm ->
                            val isSel = timeSel == tm
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .trainPilotClickable(RoundedCornerShape(8.dp)) { timeSel = tm }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(tm, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSel) palette.onPrimary else palette.textPrimary)
                                }
                            }
                        }
                    }
                    PlannerFieldTile(
                        label = trW(state.language, "Időtartam (perc)", "Duration (min)", "Dauer (Min.)", "Durată (min)"),
                        value = durSel.toString(),
                        dropdownOptions = calendarDurationOptions,
                        onSelectOption = { durSel = it.toInt() }
                    )
                }
            },
            confirmButton = {
                com.repforge.app.ui.theme.TrainPilotSaveButton(
                    onClick = {
                        onModifyCalendarEvent(ev.id, "$activeProgramTitle • ${trW(state.language, "Alap", "Day", "Tag", "Ziua")} $splitSel", splitSel, timeSel, durSel)
                        editingEvent = null
                    }) {
                    Text(strings.saveBtn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(onClick = { editingEvent = null }) {
                    Text(strings.cancelBtn, color = palette.textPrimary)
                }
            }
        )
    }

    // 2. Move / Reschedule Day Event Dialog
    movingEvent?.let { ev ->
        var targetDay by remember(ev.id) { mutableIntStateOf((ev.dayOfMonth + 1).coerceAtMost(displayedYearMonth.lengthOfMonth())) }
        var shiftAllFollowing by remember(ev.id) { mutableStateOf(true) }
        com.example.ui.components.TrainPilotFormSheet(
            onDismissRequest = { movingEvent = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            title = {
                Text(
                    text = trW(
                        state.language,
                        "Edzés áthelyezése (${ev.dayOfMonth}. napról)",
                        "Move workout (from day ${ev.dayOfMonth})",
                        "Training verschieben (von Tag ${ev.dayOfMonth})",
                        "Mută antrenamentul (din ziua ${ev.dayOfMonth})"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(trW(state.language, "Válaszd ki az új napot ebben a hónapban:", "Select the new day in this month:", "Wähle den neuen Tag in diesem Monat:", "Selectează noua zi din această lună:"), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    EditorStepperBox(
                        label = trW(state.language, "Új naptári nap (1–${displayedYearMonth.lengthOfMonth()})", "New calendar day (1–${displayedYearMonth.lengthOfMonth()})", "Neuer Kalendertag (1–${displayedYearMonth.lengthOfMonth()})", "Zi calendaristică nouă (1–${displayedYearMonth.lengthOfMonth()})"),
                        valueText = "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${targetDay.toString().padStart(2, '0')}.",
                        onMinus = { targetDay = (targetDay - 1).coerceAtLeast(1) },
                        onPlus = { targetDay = (targetDay + 1).coerceAtMost(displayedYearMonth.lengthOfMonth()) }
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.glassTile(palette.surfaceElevated, elevated = true),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .trainPilotClickable(RoundedCornerShape(10.dp)) { shiftAllFollowing = !shiftAllFollowing }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (shiftAllFollowing) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                tint = palette.primary
                            )
                            Text(
                                text = trW(
                                    state.language,
                                    "Következő tervezett edzések automatikus eltolása",
                                    "Automatically shift following planned workouts",
                                    "Folgende geplante Trainings automatisch verschieben",
                                    "Decalează automat următoarele antrenamente planificate"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMoveCalendarEvent(ev.id, targetDay, shiftAllFollowing)
                        movingEvent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
                ) {
                    Text(trW(state.language, "Áthelyezés", "Move", "Verschieben", "Mută"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(onClick = { movingEvent = null }) {
                    Text(strings.cancelBtn, color = palette.textPrimary)
                }
            }
        )
    }

    // 3. Planner Field Picker Dialog
    activeFieldDialog?.let { fieldKey ->
        com.example.ui.components.TrainPilotFormSheet(scrollText = false,
            onDismissRequest = { activeFieldDialog = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            title = {
                Text(
                    text = when (fieldKey) {
                        "cadence" -> trW(state.language, "Tervezési ritmus", "Planning cadence", "Planungsrhythmus", "Ritm de planificare")
                        "firstDay" -> trW(state.language, "Első edzésnap", "First workout day", "Erster Trainingstag", "Prima zi de antrenament")
                        "startDate" -> trW(state.language, "Kezdőnap kiválasztása", "Select start date", "Startdatum wählen", "Selectează data de început")
                        "time" -> trW(state.language, "Edzés időpontja", "Workout time", "Trainingszeit", "Ora antrenamentului")
                        "duration" -> trW(state.language, "Időtartam (perc)", "Duration (min)", "Dauer (Min.)", "Durată (min)")
                        else -> trW(state.language, "Hetek száma", "Number of weeks", "Anzahl der Wochen", "Număr de săptămâni")
                    },
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                )
            },
            text = {
                val options: List<Pair<String, String>> = when (fieldKey) {
                    "cadence" -> listOf("Minden második nap", "Heti 3 nap (H-Sze-P)", "Heti 4 nap", "Minden nap").map { it to localizeCadenceLabel(it) }
                    "firstDay" -> listOf("Alap A", "Alap B").map { it to localizeFirstDayLabel(it) }
                    "startDate" -> (1..displayedYearMonth.lengthOfMonth()).map { d ->
                        val dateStr = "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${d.toString().padStart(2, '0')}."
                        dateStr to dateStr
                    }
                    "time" -> listOf("07:00", "08:30", "10:00", "16:00", "17:30", "18:00", "19:30").map { it to it }
                    "duration" -> calendarDurationOptions.map { it to it }
                    else -> listOf("1", "2", "4", "6", "8", "12").map { it to it }
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.forEach { (canonicalVal, displayLabel) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.glassTile(palette.surfaceElevated, elevated = true),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .trainPilotClickable(RoundedCornerShape(10.dp)) {
                                    when (fieldKey) {
                                        "cadence" -> onUpdatePlanningParams(canonicalVal, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                                        "firstDay" -> onUpdatePlanningParams(state.planningCadenceHu, canonicalVal, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                                        "startDate" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, canonicalVal, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                                        "time" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, canonicalVal, state.planningDurationMin, state.planningWeeksCount)
                                        "duration" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, canonicalVal.toIntOrNull() ?: 60, state.planningWeeksCount)
                                        "weeks" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, canonicalVal.toIntOrNull() ?: 4)
                                    }
                                    activeFieldDialog = null
                                }
                        ) {
                            Text(
                                text = displayLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TrainPilotOutlinedButton(onClick = { activeFieldDialog = null }) {
                    Text(trW(strings.language, "Bezárás", "Close", "Schließen", "Închide"), color = palette.textPrimary)
                }
            }
        )
    }

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        // Always 6 week rows (empty trailing row for shorter months): month changes never resize the page.
        val calendarWeeks = 6
        // Reserve space for the complete three-row planner, its actions and month header.
        // Compact day grid; planner action buttons retain 48 dp. Large fonts use the taller
        // cells and the existing page scroll rather than truncating the bottom controls.
        val largeFont = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
        val calendarCellHeight = ((maxHeight - 410.dp) / calendarWeeks.toFloat()).coerceIn(
            if (largeFont) 48.dp else 40.dp, if (largeFont) 52.dp else 44.dp)
        val calendarPageHeight = maxHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 4.dp)
                .testTag("calendar_compact_page"),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Column(
            Modifier.fillMaxWidth().heightIn(min = calendarPageHeight - 8.dp).testTag("calendar_pinned_layout"),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            // 1. EXACT SCREENSHOT 7 CALENDAR CARD
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.glassCardFill(palette.surfaceCard),
                modifier = Modifier
                    .fillMaxWidth().testTag("calendar_month_panel")
                    .trainPilotGlassMaterial(palette, RoundedCornerShape(22.dp))
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
                    // Horizontal swipe changes month (left = next, right = previous); the arrows stay.
                    // Only horizontal drags past the touch slop are consumed: day taps and vertical
                    // page scrolling keep working.
                    .pointerInput(displayedYearMonth) {
                        val threshold = 56.dp.toPx()
                        var dragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragX = 0f },
                            onDragCancel = { dragX = 0f },
                            onDragEnd = {
                                when {
                                    dragX <= -threshold -> { openedDay = null; onSelectMonth(displayedYearMonth.plusMonths(1).toString()) }
                                    dragX >= threshold -> { openedDay = null; onSelectMonth(displayedYearMonth.minusMonths(1).toString()) }
                                }
                                dragX = 0f
                            },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                dragX += amount
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Header row: [ < ]   2026. Október 01.   [ > ]   [ X ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceElevated,
                            modifier = Modifier
                                .size(if (palette.isLiquidGlass) 48.dp else 36.dp)
                                .liquidDropMaterial(palette, RoundedCornerShape(12.dp), GlassMaterialRole.Button)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                                .trainPilotClickable(RoundedCornerShape(10.dp)) { openedDay = null; onSelectMonth(displayedYearMonth.minusMonths(1).toString()) }
                                .testTag("calendar_prev_month")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Előző hónap",
                                    tint = palette.textPrimary
                                )
                            }
                        }

                        Text(
                            text = monthHeaderText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceElevated,
                                modifier = Modifier
                                    .size(if (palette.isLiquidGlass) 48.dp else 36.dp)
                                .liquidDropMaterial(palette, RoundedCornerShape(12.dp), GlassMaterialRole.Button)
                                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                                    .trainPilotClickable(RoundedCornerShape(10.dp)) { openedDay = null; onSelectMonth(displayedYearMonth.plusMonths(1).toString()) }
                                    .testTag("calendar_next_month")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Következő hónap",
                                        tint = palette.textPrimary
                                    )
                                }
                            }

                            if (state.currentSubScreen == SubScreen.CALENDAR_PLANNER) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.danger.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.dp, palette.danger.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .trainPilotClickable(RoundedCornerShape(12.dp)) { onBack() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "✕",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Weekday headers
                    val weekdayShortHeaders = when (state.language) {
                        com.example.model.AppLanguage.HU -> listOf("H", "K", "Sze", "Cs", "P", "Szo", "V")
                        com.example.model.AppLanguage.EN -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        com.example.model.AppLanguage.DE -> listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                        com.example.model.AppLanguage.RO -> listOf("Lun", "Mar", "Mie", "Joi", "Vin", "Sâm", "Dum")
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekdayShortHeaders.forEach { d ->
                            Text(
                                text = d,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Dynamic month grid with rounded dark cells matching Screenshot 7
                    val daysInMonth = displayedYearMonth.lengthOfMonth()
                    val firstDayOffset = displayedYearMonth.atDay(1).dayOfWeek.value - 1 // Monday=0 .. Sunday=6
                    val totalCells = calendarWeeks * 7

                    Column(verticalArrangement = Arrangement.spacedBy(calendarDayRowGap)) {
                        for (weekIdx in 0 until (totalCells / 7)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(calendarDayColumnGap)
                            ) {
                                for (dow in 0..6) {
                                    val cellIndex = weekIdx * 7 + dow
                                    val dayNumber = cellIndex - firstDayOffset + 1
                                    if (dayNumber !in 1..daysInMonth) {
                                        Spacer(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(calendarCellHeight)
                                        )
                                    } else {
                                        val isSelected = dayNumber == state.selectedCalendarDay
                                        val evs = state.calendarEvents.filter { it.dayOfMonth == dayNumber && it.dateIso.startsWith(displayedYearMonth.toString()) }
                                        val completedEv = evs.firstOrNull { it.status == CalendarEventStatus.COMPLETED }
                                        val plannedEv = evs.firstOrNull { it.status == CalendarEventStatus.PLANNED }
                                        val isToday = displayedYearMonth.atDay(dayNumber) == today
                                        val dayStyle = calendarDayAppearance(palette, isSelected, isToday, plannedEv != null)
                                        Surface(
                                            shape = calendarDayShape,
                                            color = dayStyle.fill,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(calendarCellHeight)
                                                .calendarDayFrame(palette, dayStyle, isSelected, isToday)
                                                .then(if (palette.isLiquidGlass) Modifier.semantics {
                                                    selected = isSelected
                                                    if (isToday) stateDescription = trW(state.language, "Ma", "Today", "Heute", "Astăzi")
                                                } else Modifier)
                                                .testTag("calendar_day_$dayNumber")
                                                .trainPilotClickable(calendarDayShape) { onSelectDay(dayNumber); openedDay = dayNumber }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "$dayNumber",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    textDecoration = if (dayStyle.underlineToday) TextDecoration.Underline else TextDecoration.None,
                                                    color = if (palette.isLiquidGlass && isToday) palette.primary else palette.textPrimary
                                                )
                                                when {
                                                    completedEv != null -> {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Teljesítve",
                                                            tint = palette.primary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    plannedEv != null -> {
                                                        Text(
                                                            text = plannedEv.splitBadge,
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = palette.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 3. Calendar planning pinned to the bottom (stays below the month on short windows).
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.glassCardFill(palette.surfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("calendar_planning_block")
                    .trainPilotGlassMaterial(palette, RoundedCornerShape(22.dp))
                    .trainPilotBorder(TrainPilotFrames.panelWidth, palette.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(activeProgramTitle, color = palette.textPrimary,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${localizeCadenceLabel(state.planningCadenceHu)} • ${state.planningTime}",
                                color = palette.textSecondary, style = MaterialTheme.typography.labelSmall,
                                maxLines = if (palette.isLiquidGlass) 2 else 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (state.calendarUndoStack.isNotEmpty()) TrainPilotIconButton(onClick = onUndoCalendarAction,
                            modifier = Modifier.size(48.dp).testTag("calendar_undo_action")) {
                            Icon(Icons.AutoMirrored.Filled.Undo,
                                contentDescription = trW(state.language, "Visszavonás (${state.calendarUndoStack.size})", "Undo (${state.calendarUndoStack.size})", "Rückgängig (${state.calendarUndoStack.size})", "Anulează (${state.calendarUndoStack.size})"),
                                tint = palette.textSecondary)
                        }
                        CalendarPrimaryGlassButton(
                            onClick = { onOpenReplanModal(true) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("calendar_replan_action"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = trW(state.language, "Újratervezés", "Replan", "Neu planen", "Replanificare"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    val monthDates = remember(displayedYearMonth) {
                        (1..displayedYearMonth.lengthOfMonth()).map { d ->
                            "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${d.toString().padStart(2, '0')}."
                        }
                    }

                    val cadenceMap = listOf("Minden második nap", "Heti 3 nap (H-Sze-P)", "Heti 4 nap", "Minden nap").associateBy { localizeCadenceLabel(it) }
                    val firstDayMap = listOf("Alap A", "Alap B").associateBy { localizeFirstDayLabel(it) }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_cadence"),
                            compact = true,
                            label = trW(state.language, "Tervezési ritmus", "Planning cadence", "Planungsrhythmus", "Ritm de planificare"),
                            value = localizeCadenceLabel(state.planningCadenceHu),
                            showChevron = true,
                            dropdownOptions = cadenceMap.keys.toList(),
                            onSelectOption = { opt ->
                                val canonical = cadenceMap[opt] ?: opt
                                onUpdatePlanningParams(canonical, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_first_day"),
                            compact = true,
                            label = trW(state.language, "Első edzésnap", "First workout day", "Erster Trainingstag", "Prima zi de antrenament"),
                            value = localizeFirstDayLabel(state.planningFirstDayHu),
                            showChevron = true,
                            dropdownOptions = firstDayMap.keys.toList(),
                            onSelectOption = { opt ->
                                val canonical = firstDayMap[opt] ?: opt
                                onUpdatePlanningParams(state.planningCadenceHu, canonical, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_start_date"),
                            compact = true,
                            label = trW(state.language, "Kezdőnap", "Start date", "Startdatum", "Data de început"),
                            value = state.planningStartDate,
                            showCalendarIcon = true,
                            dropdownOptions = monthDates,
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, opt, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_time"),
                            compact = true,
                            label = trW(state.language, "Időpont", "Time", "Uhrzeit", "Ora"),
                            value = state.planningTime,
                            dropdownOptions = listOf("07:00", "08:30", "10:00", "16:00", "17:30", "18:00", "19:30"),
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, opt, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_duration"),
                            compact = true,
                            label = trW(state.language, "Időtartam (perc)", "Duration (min)", "Dauer (Min.)", "Durată (min)"),
                            value = "${state.planningDurationMin}",
                            dropdownOptions = calendarDurationOptions,
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, opt.toIntOrNull() ?: 60, state.planningWeeksCount)
                            }
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_weeks"),
                            compact = true,
                            label = trW(state.language, "Hetek száma", "Weeks count", "Anzahl Wochen", "Număr săptămâni"),
                            value = "${state.planningWeeksCount}",
                            dropdownOptions = listOf("1", "2", "4", "6", "8", "12"),
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, opt.toIntOrNull() ?: 4)
                            }
                        )
                    }

                    StudioAdaptiveControlRow(minimumWidth = 88.dp) {
                        com.repforge.app.ui.theme.TrainPilotNegativeButton(
                            onClick = onRequestClearCalendar, enabled = !state.calendarClearBusy,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .trainPilotGlassRim(palette, RoundedCornerShape(12.dp), GlassMaterialRole.Button)
                                .semantics { contentDescription = trW(state.language, "Naptár ürítése", "Clear calendar", "Kalender leeren", "Șterge calendarul") }.testTag("calendar_clear_action")) {
                            Text(trW(state.language, "Ürítés", "Clear", "Leeren", "Șterge"), maxLines = 1)
                        }
                        CalendarPrimaryGlassButton(onClick = onPlanMonthlyWorkouts, enabled = !state.calendarClearBusy,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("calendar_plan_action")) {
                            Text(trW(state.language, "Tervezés", "Plan workouts", "Trainings planen", "Planifică"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
                  }
}
    }
    if (state.showCalendarClearDialog) {
        AlertDialog(onDismissRequest = onCancelClearCalendar,
            title = { Text(trW(state.language, "Naptár ürítése?", "Clear calendar?", "Kalender leeren?", "Ștergi calendarul?")) },
            text = { Text(trW(state.language,
                "Az összes tervezett és kihagyott edzés törlődik a naptárból. A befejezett edzések és a napló megmaradnak. Ez nem vonható vissza.",
                "All planned and skipped workouts will be removed from the calendar. Completed workouts and your log will be kept. This cannot be undone.",
                "Alle geplanten und ausgelassenen Trainings werden aus dem Kalender entfernt. Abgeschlossene Trainings und das Protokoll bleiben erhalten. Dies kann nicht rückgängig gemacht werden.",
                "Toate antrenamentele planificate și omise vor fi șterse din calendar. Antrenamentele finalizate și jurnalul rămân. Acțiunea nu poate fi anulată.")) },
            confirmButton = { com.repforge.app.ui.theme.TrainPilotNegativeButton(
                onClick = onConfirmClearCalendar, enabled = !state.calendarClearBusy,
                modifier = Modifier.testTag("calendar_clear_confirm")) {
                Text(trW(state.language, "Ürítés", "Clear", "Leeren", "Șterge"))
            } },
            dismissButton = { com.repforge.app.ui.theme.TrainPilotTextButton(
                onClick = onCancelClearCalendar, modifier = Modifier.testTag("calendar_clear_cancel")) {
                Text(trW(state.language, "Mégse", "Cancel", "Abbrechen", "Anulează"))
            } })
    }
    if (openedDay != null) {
        com.example.ui.components.CompactCalendarDialog(
            title = "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${selectedDay.toString().padStart(2, '0')}.",
            tag = "calendar_day_dialog", onClose = { openedDay = null }, asSheet = true
        ) {
            // 2. INTERACTIVE DAY ACTIONS CARD (Módosítás, Áthelyezés, Törlés, Visszavonás)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.glassCardFill(palette.surfaceCard),
                modifier = Modifier
                    .fillMaxWidth().testTag("calendar_day_actions")
                    .trainPilotGlassMaterial(palette, RoundedCornerShape(18.dp))
                    .trainPilotBorder(TrainPilotFrames.panelWidth, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (dayEvents.isEmpty()) {
                        Text(trW(state.language, "Nincs tervezett edzés ezen a napon.", "No planned workout on this day.", "Kein geplantes Training an diesem Tag.", "Niciun antrenament planificat în această zi."), style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary, modifier = Modifier.fillMaxWidth().testTag("calendar_empty_day"))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (split in listOf("A", "B")) {
                                TrainPilotOutlinedButton(onClick = {
                                    onAddPlannedWorkoutOnDay(selectedDay, split, state.planningTime, state.planningDurationMin)
                                }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("calendar_add_$split")) {
                                    Text(trW(state.language, "+ Alap $split", "+ Day $split", "+ Tag $split", "+ Ziua $split"))
                                }
                            }
                        }
                    } else {
                        dayEvents.forEach { ev ->
                            val isDone = ev.status == CalendarEventStatus.COMPLETED
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.glassTile(palette.surfaceElevated, elevated = true))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ev.localizedTitle(state.language, state.programs),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = "${ev.timeLabel} • ~${ev.durationMinutes} ${trW(state.language, "perc", "min", "Min.", "min")} • ${if (isDone) trW(state.language, "Teljesítve ✓", "Completed ✓", "Abgeschlossen ✓", "Finalizat ✓") else trW(state.language, "Tervezett (${ev.splitBadge} nap)", "Planned (Day ${ev.splitBadge})", "Geplant (Tag ${ev.splitBadge})", "Planificat (Ziua ${ev.splitBadge})")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDone) palette.success else palette.primary
                                        )
                                    }
                                    if (isDone) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = palette.primary.copy(alpha = 0.16f),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .trainPilotClickable(RoundedCornerShape(8.dp)) {
                                                    openedDay = null
                                                    onOpenWorkoutDetail(ev.id)
                                                }
                                                .testTag("calendar_open_workout_detail_${ev.id}")
                                        ) {
                                            Text(
                                                text = "${strings.navLog} >",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = palette.primary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                if (onStartPlannedWorkout != null && StudioCalendarDates.canStart(ev)) {
                                    TrainPilotGlassPrimaryButton(
                                        onClick = {
                                            openedDay = null
                                            onStartPlannedWorkout(ev.id)
                                        },
                                        enabled = !state.isWorkoutSessionActive && !state.isSavingWorkout,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            .testTag("calendar_start_planned_${ev.id}")
                                    ) {
                                        Text(strings.startPlannedBtn)
                                    }
                                }

                                // 4-button Action row: Módosítás | Áthelyezés | Törlés | Visszavonás
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CalendarActionChip(
                                        label = trW(state.language, "Módosítás", "Modify", "Ändern", "Modifică"),
                                        color = palette.primary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { openedDay = null; editingEvent = ev }
                                    )
                                    CalendarActionChip(
                                        label = trW(state.language, "Áthelyezés", "Move", "Verschieben", "Mută"),
                                        color = palette.textPrimary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { openedDay = null; movingEvent = ev }
                                    )
                                    CalendarActionChip(
                                        label = trW(state.language, "Törlés", "Delete", "Löschen", "Șterge"),
                                        color = palette.danger,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onDeleteCalendarEvent(ev.id) }, negative = true
                                    )
                                    CalendarActionChip(
                                        label = trW(state.language, "Visszavonás", "Undo", "Rückgängig", "Anulează"),
                                        color = if (state.calendarUndoStack.isNotEmpty()) palette.secondary else palette.textSecondary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onUndoCalendarAction() }, negative = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // 4. EXACT SCREENSHOT 8 "Edzések újratervezése" MODAL DIALOG
    if (state.showReplanModal) {
        var selectedProgId by remember { mutableStateOf(activeProgram?.id ?: "") }
        var selectedFirstDay by remember { mutableStateOf("Automatikus") }
        val defaultTomorrow = remember {
            today.plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy. MM. dd."))
        }
        var selectedNewStartDate by remember { mutableStateOf(defaultTomorrow) }

        val selectedProgObj = state.programs.firstOrNull { it.id == selectedProgId } ?: activeProgram
        val selectedProgName = selectedProgObj?.localizedName(state.language) ?: activeProgramTitle
        val monthDatesForReplan = remember(displayedYearMonth) {
            (1..displayedYearMonth.lengthOfMonth()).map { d ->
                "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${d.toString().padStart(2, '0')}."
            }
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { onOpenReplanModal(false) },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CalendarReplanSurface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Edzések újratervezése + red X close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = trW(state.language, "Edzések újratervezése", "Replan workouts", "Trainings neu planen", "Replanificare antrenamente"),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        com.example.ui.components.TrainPilotRedCloseButton(
                            onClick = { onOpenReplanModal(false) }, size = 36.dp, testTag = "calendar_replan_close"
                        )
                    }

                    Text(
                        text = trW(
                            state.language,
                            "A naplózott edzések megmaradnak.",
                            "Logged workouts are kept.",
                            "Protokollierte Trainings bleiben erhalten.",
                            "Antrenamentele salvate rămân."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )

                    // Keep selection local until the plan is applied. Disambiguate duplicate names
                    // so each option still maps to its stable program ID.
                    val programNames = state.programs.map { it.localizedName(state.language) }
                    val hasDuplicateProgramNames = programNames.distinct().size != programNames.size
                    val programOptions = state.programs.mapIndexed { index, program ->
                        val name = programNames[index]
                        val label = if (hasDuplicateProgramNames) "${index + 1}. $name" else name
                        label to program.id
                    }
                    PlannerFieldTile(
                        modifier = Modifier.testTag("calendar_replan_program"),
                        label = trW(state.language, "Program", "Program", "Programm", "Program"),
                        value = selectedProgName,
                        showChevron = true,
                        dropdownOptions = programOptions.map { it.first },
                        onSelectOption = { label ->
                            programOptions.firstOrNull { it.first == label }?.let { selectedProgId = it.second }
                        }
                    )

                    // Első edzésnap selector (floating dropdown)
                    val firstDayOptionsMap = listOf("Automatikus", "Alap A", "Alap B").associateBy { localizeFirstDayLabel(it) }
                    PlannerFieldTile(
                        label = trW(state.language, "Első edzésnap", "First workout day", "Erster Trainingstag", "Prima zi de antrenament"),
                        value = localizeFirstDayLabel(selectedFirstDay),
                        showChevron = true,
                        dropdownOptions = firstDayOptionsMap.keys.toList(),
                        onSelectOption = { selectedFirstDay = firstDayOptionsMap[it] ?: it }
                    )

                    // Új kezdőnap selector (floating dropdown)
                    PlannerFieldTile(
                        label = trW(state.language, "Új kezdőnap", "New start date", "Neues Startdatum", "Noua dată de început"),
                        value = selectedNewStartDate,
                        showCalendarIcon = true,
                        showChevron = true,
                        dropdownOptions = monthDatesForReplan,
                        onSelectOption = { selectedNewStartDate = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${trW(state.language, "Program", "Program", "Programm", "Program")}: $selectedProgName",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )

                    // Shifted sessions preview list with dividers matching Screenshot 8
                    val startD = selectedNewStartDate
                        .split(".")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .lastOrNull() ?: today.dayOfMonth
                    val previewItems = listOf(
                        localizeFirstDayLabel("Alap B") to "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD - 2).coerceAtLeast(1).toString().padStart(2, '0')}. ${state.planningTime} → $selectedNewStartDate ${state.planningTime}",
                        localizeFirstDayLabel("Alap A") to "$selectedNewStartDate ${state.planningTime} → ${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD + 2).coerceAtMost(31).toString().padStart(2, '0')}. ${state.planningTime}",
                        localizeFirstDayLabel("Alap B") to "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD + 2).coerceAtMost(31).toString().padStart(2, '0')}. ${state.planningTime} → ${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD + 4).coerceAtMost(31).toString().padStart(2, '0')}. ${state.planningTime}"
                    )
                    previewItems.forEach { (splitTitle, shiftText) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = splitTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary
                            )
                            Text(
                                text = shiftText,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = palette.cardBorder)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    TrainPilotGlassPrimaryButton(
                        onClick = { onApplyReplan(selectedNewStartDate, selectedFirstDay, selectedProgObj?.id ?: "") },
                        enabled = selectedProgObj != null && !state.libraryMutationBusy,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp).testTag("calendar_replan_apply")
                    ) {
                        Text(
                            text = trW(state.language, "Terv frissítése", "Update plan", "Plan aktualisieren", "Actualizează planul"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
