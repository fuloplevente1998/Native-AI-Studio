package com.repforge.app.ui.journal

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.TrainPilotPrimaryButton
import com.example.ui.components.TrainPilotSecondaryButton
import com.example.ui.components.TrainPilotRedCloseButton
import com.example.ui.components.TrainPilotScreenSheet
import com.example.ui.components.TrainPilotFitText
import com.example.ui.screens.calendarDayAppearance
import com.example.ui.screens.calendarDayColumnGap
import com.example.ui.screens.calendarDayRowGap
import com.example.ui.screens.calendarDayShape
import com.example.ui.screens.calendarDayFrame
import com.example.ui.theme.*
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Exact Instant is authoritative; only edited local components are replaced.
 * In an overlap prefer the original offset when it remains valid. A DST gap
 * is not a real local time: leave the draft unchanged and ask for another time.
 */
internal object JournalLocalTime {
    fun edit(original: String, zone: ZoneId, date: LocalDate? = null,
        hour: Int? = null, minute: Int? = null): String? {
        val old = Instant.parse(original).atZone(zone)
        // Build from the complete date, not component-by-component (leap days/month ends).
        val target = LocalDateTime.of(date ?: old.toLocalDate(),
            old.toLocalTime().withHour(hour ?: old.hour).withMinute(minute ?: old.minute))
        if (target == old.toLocalDateTime()) return original
        val offsets = zone.rules.getValidOffsets(target)
        if (offsets.isEmpty()) return null
        return target.toInstant(old.offset.takeIf { it in offsets } ?: offsets.first()).toString()
    }
}

private fun word(language: String, hu: String, en: String, de: String, ro: String) =
    when (language.lowercase()) { "en" -> en; "de" -> de; "ro" -> ro; else -> hu }
private fun locale(language: String) = Locale.forLanguageTag(language.lowercase())
private fun two(n: Int) = n.toString().padStart(2, '0')

@Composable
internal fun JournalDateTimeField(value: String, zone: ZoneId, language: String, label: String,
    tag: String, enabled: Boolean, onDate: () -> Unit, onTime: () -> Unit) {
    val p = LocalTrainPilotPalette.current
    val dateTime = Instant.parse(value).atZone(zone)
    val dateLabel = DateTimeFormatter.ofPattern("yyyy. MM. dd.", locale(language)).format(dateTime)
    val timeLabel = DateTimeFormatter.ofPattern("HH:mm", locale(language)).format(dateTime)
    val shape = if (p.isLiquidGlass) RoundedCornerShape(50) else RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().testTag(tag), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
        Surface(shape = shape, color = if (p.isLiquidGlass) Color.Transparent else p.surfaceElevated,
            modifier = Modifier.fillMaxWidth()
                .then(if (p.isLiquidGlass) Modifier.liquidDropMaterial(p, shape, GlassMaterialRole.Input) else Modifier)
                .trainPilotBorder(1.dp, p.cardBorder, shape)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).heightIn(min = 48.dp).testTag("${tag}_date")
                    .trainPilotClickable(shape, enabled = enabled, onClick = onDate)
                    .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.DateRange, word(language, "Dátum", "Date", "Datum", "Dată"),
                        tint = p.primary, modifier = Modifier.size(18.dp))
                    TrainPilotFitText(dateLabel, color = p.textPrimary, modifier = Modifier.weight(1f))
                }
                Box(Modifier.width(1.dp).height(28.dp).trainPilotBorder(1.dp, p.cardBorder, RoundedCornerShape(1.dp)))
                Row(Modifier.weight(.68f).heightIn(min = 48.dp).testTag("${tag}_time")
                    .trainPilotClickable(shape, enabled = enabled, onClick = onTime)
                    .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TrainPilotFitText(timeLabel, color = p.textPrimary, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Schedule, word(language, "Időpont", "Time", "Uhrzeit", "Oră"),
                        tint = p.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
internal fun JournalDateTimeSheet(value: String, zone: ZoneId, language: String, chooseDate: Boolean,
    onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val p = LocalTrainPilotPalette.current
    val old = Instant.parse(value).atZone(zone)
    var selectedDay by rememberSaveable(value, chooseDate) { mutableStateOf(old.toLocalDate().toString()) }
    var monthKey by rememberSaveable(value, chooseDate) { mutableStateOf(YearMonth.from(old).toString()) }
    val month = YearMonth.parse(monthKey)
    val today = LocalDate.now(zone)
    var error by rememberSaveable { mutableStateOf(false) }
    // A generous finite wheel allows repeated midnight/minute wrap without an infinite item count.
    val hourState = rememberLazyListState(24 * 50 + old.hour)
    val minuteState = rememberLazyListState(60 * 50 + old.minute)
    val hour by remember { derivedStateOf { centeredWheelValue(hourState, 24, old.hour) } }
    val minute by remember { derivedStateOf { centeredWheelValue(minuteState, 60, old.minute) } }
    val tag = if (chooseDate) "journal_date_picker" else "journal_time_picker"
    TrainPilotScreenSheet(onDismiss = onDismiss, testTag = tag) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (chooseDate) word(language, "Dátum kiválasztása", "Select date", "Datum auswählen", "Alege data")
                    else word(language, "Időpont kiválasztása", "Select time", "Uhrzeit auswählen", "Alege ora"),
                    modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                TrainPilotRedCloseButton(onClick = onDismiss, testTag = "${tag}_close")
            }
            if (chooseDate) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TrainPilotOutlinedButton(onClick = { monthKey = month.minusMonths(1).toString() },
                            modifier = Modifier.size(48.dp).testTag("${tag}_previous")) {
                            Icon(Icons.Default.KeyboardArrowLeft, word(language, "Előző hónap", "Previous month", "Vorheriger Monat", "Luna anterioară"))
                        }
                        Text(month.format(DateTimeFormatter.ofPattern("yyyy. MMMM", locale(language))),
                            Modifier.weight(1f), textAlign = TextAlign.Center,
                            color = p.textPrimary, fontWeight = FontWeight.Bold)
                        TrainPilotOutlinedButton(onClick = { monthKey = month.plusMonths(1).toString() },
                            modifier = Modifier.size(48.dp).testTag("${tag}_next")) {
                            Icon(Icons.Default.KeyboardArrowRight, word(language, "Következő hónap", "Next month", "Nächster Monat", "Luna următoare"))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(calendarDayColumnGap)) {
                        DayOfWeek.values().forEach { day ->
                            Text(day.getDisplayName(TextStyle.SHORT, locale(language)),
                                Modifier.weight(1f), textAlign = TextAlign.Center, color = p.textSecondary,
                                style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                    val offset = month.atDay(1).dayOfWeek.value - 1
                    Column(verticalArrangement = Arrangement.spacedBy(calendarDayRowGap)) {
                        for (row in 0..5) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(calendarDayColumnGap)) {
                            for (col in 0..6) {
                                val day = row * 7 + col - offset + 1
                                if (day !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f).height(44.dp))
                                else {
                                    val date = month.atDay(day)
                                    val selected = selectedDay == date.toString()
                                    val style = calendarDayAppearance(p, selected, date == today, planned = false, rangePicker = true)
                                    Surface(shape = calendarDayShape, color = style.fill,
                                        modifier = Modifier.weight(1f).height(44.dp)
                                            .calendarDayFrame(p, style, selected, date == today)
                                            .semantics { this.selected = selected }
                                            .testTag("${tag}_day_$day")
                                            .trainPilotClickable(calendarDayShape) { selectedDay = date.toString(); error = false }) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(day.toString(), color = if (selected && !p.isLiquidGlass) p.onPrimary else p.textPrimary,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    JournalTimeWheel(word(language, "Óra", "Hour", "Stunde", "Oră"), 24, hourState, hour,
                        Modifier.weight(1f), "journal_hour")
                    JournalTimeWheel(word(language, "Perc", "Minute", "Minute", "Minut"), 60, minuteState, minute,
                        Modifier.weight(1f), "journal_minute")
                }
            }
            if (error) Text(word(language, "Ez a helyi időpont nem létezik az óraátállítás miatt. Válassz másikat.",
                "This local time does not exist due to a clock change. Choose another.",
                "Diese Ortszeit existiert wegen der Zeitumstellung nicht. Bitte anders wählen.",
                "Această oră locală nu există din cauza schimbării orei. Alege alta."),
                color = p.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("${tag}_error"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainPilotSecondaryButton(word(language, "Mégse", "Cancel", "Abbrechen", "Anulează"), onDismiss,
                    Modifier.weight(1f).testTag("${tag}_cancel"))
                TrainPilotPrimaryButton(word(language, "Kész", "Done", "Fertig", "Gata"), {
                    val result = if (chooseDate) JournalLocalTime.edit(value, zone, date = LocalDate.parse(selectedDay))
                        else JournalLocalTime.edit(value, zone, hour = hour, minute = minute)
                    if (result == null) error = true else onConfirm(result)
                }, Modifier.weight(1f).testTag("${tag}_done"),
                    enabled = chooseDate || (!hourState.isScrollInProgress && !minuteState.isScrollInProgress))
            }
        }
    }
}

/** Center is the actual measured row, not an independently advanced counter. */
internal fun centeredWheelValue(state: androidx.compose.foundation.lazy.LazyListState, count: Int, fallback: Int): Int {
    val layout = state.layoutInfo
    val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
    return layout.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - center) }
        ?.index?.rem(count) ?: fallback
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun JournalTimeWheel(label: String, count: Int, state: androidx.compose.foundation.lazy.LazyListState,
    value: Int, modifier: Modifier, tag: String) {
    val p = LocalTrainPilotPalette.current
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(if (p.isLiquidGlass) 24.dp else 14.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = p.textPrimary, fontWeight = FontWeight.Bold)
        Surface(shape = shape, color = if (p.isLiquidGlass) p.glassTile(p.surfaceCard) else p.surfaceCard,
            modifier = Modifier.fillMaxWidth().then(if (p.isLiquidGlass)
                Modifier.trainPilotGlassRim(p, shape, GlassMaterialRole.Panel) else Modifier.trainPilotBorder(1.dp, p.cardBorder, shape))) {
            // Exactly five equally tall rows: content padding positions the first
            // visible index at the center, including after a tap-to-scroll.
            val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
            val rowHeight = (44f * fontScale.coerceAtLeast(1f)).dp
            LazyColumn(state = state, flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
                modifier = Modifier.fillMaxWidth().height(rowHeight * 5).testTag(tag),
                contentPadding = PaddingValues(vertical = rowHeight * 2), horizontalAlignment = Alignment.CenterHorizontally) {
                items(count * 101) { index ->
                    val n = index % count
                    val active = n == value
                    val itemShape = if (p.isLiquidGlass) RoundedCornerShape(50) else RoundedCornerShape(12.dp)
                    Box(Modifier.fillMaxWidth().height(rowHeight).padding(horizontal = 5.dp)
                        .then(if (active && p.isLiquidGlass)
                            Modifier.liquidDropMaterial(p, itemShape, GlassMaterialRole.Selection, accent = p.primary)
                        else if (active) Modifier.trainPilotBorder(1.dp, p.primary, itemShape) else Modifier)
                        .semantics { selected = active }
                        .testTag("${tag}_item_$index")
                        .trainPilotClickable(itemShape) { scope.launch { state.animateScrollToItem(index) } },
                        contentAlignment = Alignment.Center) {
                        Text(two(n), color = if (active) p.textPrimary else p.textSecondary,
                            style = if (active) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
