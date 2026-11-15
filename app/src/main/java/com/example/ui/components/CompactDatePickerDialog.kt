package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.HealthJournalStore
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import com.repforge.app.ui.theme.trainPilotBorder
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The app calendar's Monday-first rounded cells, inside its existing compact dialog. */
@Composable
internal fun CompactDatePickerDialog(
    initialDate: LocalDate, language: String, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val locale = Locale.forLanguageTag(language)
    fun label(hu: String, en: String, de: String, ro: String) = when (language.lowercase(Locale.ROOT)) {
        "en" -> en; "de" -> de; "ro" -> ro; else -> hu
    }
    var monthIso by rememberSaveable(initialDate) { mutableStateOf(YearMonth.from(initialDate).toString()) }
    var selectedIso by rememberSaveable(initialDate) { mutableStateOf(initialDate.toString()) }
    val month = YearMonth.parse(monthIso)
    val selected = LocalDate.parse(selectedIso)
    CompactCalendarDialog(label("Dátum választása", "Select date", "Datum auswählen", "Selectează data"),
        "health_date_picker", onDismiss) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { monthIso = month.minusYears(1).toString() }, enabled = month.year > 1,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("health_date_previous_year")
                    .semantics { contentDescription = label("Előző év", "Previous year", "Vorheriges Jahr", "Anul anterior") }) { Text("«") }
            Text(month.year.toString(), color = palette.textPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = { monthIso = month.plusYears(1).toString() }, enabled = month.year < 9999,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("health_date_next_year")
                    .semantics { contentDescription = label("Következő év", "Next year", "Nächstes Jahr", "Anul următor") }) { Text("»") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { monthIso = month.minusMonths(1).toString() }, enabled = month.year > 1 || month.monthValue > 1,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("health_date_previous_month")
                    .semantics { contentDescription = label("Előző hónap", "Previous month", "Vorheriger Monat", "Luna anterioară") }) { Text("‹") }
            Text(month.month.getDisplayName(TextStyle.FULL, locale), color = palette.textPrimary,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = { monthIso = month.plusMonths(1).toString() }, enabled = month.year < 9999 || month.monthValue < 12,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("health_date_next_month")
                    .semantics { contentDescription = label("Következő hónap", "Next month", "Nächster Monat", "Luna următoare") }) { Text("›") }
        }
        Row(Modifier.fillMaxWidth()) {
            (1..7).forEach { day -> Text(java.time.DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, locale),
                color = palette.textSecondary, style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        val offset = month.atDay(1).dayOfWeek.value - 1
        repeat((offset + month.lengthOfMonth() + 6) / 7) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(7) { weekday ->
                    val number = week * 7 + weekday - offset + 1
                    if (number !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f).height(48.dp))
                    else {
                        val date = month.atDay(number)
                        val chosen = date == selected
                        val shape = RoundedCornerShape(10.dp)
                        Surface(onClick = { selectedIso = date.toString() }, shape = shape,
                            color = if (chosen) palette.primary.copy(alpha = 0.16f) else palette.surface,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .trainPilotBorder(1.dp, if (chosen) palette.primary else palette.cardBorder, shape)
                                .testTag("health_date_day_$number").semantics {
                                    this.selected = chosen
                                    contentDescription = date.toString()
                                }) {
                            Box(contentAlignment = Alignment.Center) { Text(number.toString(), color = palette.textPrimary) }
                        }
                    }
                }
            }
        }
        Text(selected.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(locale)),
            color = palette.primary, modifier = Modifier.testTag("health_date_selected"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainPilotOutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("health_date_cancel")) {
                Text(label("Mégse", "Cancel", "Abbrechen", "Anulează"))
            }
            TrainPilotOutlinedButton(onClick = { onConfirm(selected) }, enabled = HealthJournalStore.validDay(selectedIso),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("health_date_confirm")) { Text("OK") }
        }
    }
}
