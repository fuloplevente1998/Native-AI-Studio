package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotClickable
import java.time.LocalDate
import java.time.YearMonth

/**
 * Shared period picker of the journal (Progress → Log) and the Health journal: a bottom sheet with
 * two emphasized actions (Today, Clear filter), a Monday-first month grid that always has 6 rows
 * (constant height), sideways swipe to change month, two taps = interval, and a Done button.
 * Cells share the width, so every weekday fits on narrow phones.
 */
@Composable
fun TrainPilotRangeCalendarSheet(
    tagPrefix: String,
    title: String,
    monthTitle: String,
    hint: String,
    weekdayHeaders: List<String>,
    month: YearMonth,
    start: LocalDate?,
    end: LocalDate?,
    today: LocalDate,
    hasMark: (LocalDate) -> Boolean,
    todayLabel: String,
    clearLabel: String,
    doneLabel: String,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val rangeMin = if (start != null && end != null) minOf(start, end) else start
    val rangeMax = if (start != null && end != null) maxOf(start, end) else start
    TrainPilotScreenSheet(onDismiss = onDismiss, testTag = "${tagPrefix}_calendar_sheet") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .pointerInput(month) {
                    val threshold = 56.dp.toPx()
                    var dragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = 0f },
                        onDragCancel = { dragX = 0f },
                        onDragEnd = {
                            if (dragX <= -threshold) onMonthChange(month.plusMonths(1))
                            else if (dragX >= threshold) onMonthChange(month.minusMonths(1))
                            dragX = 0f
                        },
                        onHorizontalDrag = { change, amount -> change.consume(); dragX += amount }
                    )
                }
                .testTag("${tagPrefix}_calendar_swipe_area"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.fillMaxWidth().testTag("${tagPrefix}_interval_calendar_header")) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(monthTitle, style = MaterialTheme.typography.labelLarge, color = palette.textPrimary)
                Text(hint, style = MaterialTheme.typography.labelSmall, color = palette.primary)
            }
            // Emphasized actions: Today = filled primary, Clear = red tonal (2772).
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val shape = RoundedCornerShape(12.dp)
                Surface(shape = shape, color = palette.primary, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .trainPilotClickable(shape) { onToday() }.testTag("${tagPrefix}_jump_today_button")) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Today, null, tint = palette.onPrimary, modifier = Modifier.size(18.dp))
                        Text(" $todayLabel", color = palette.onPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(shape = shape, color = palette.danger.copy(alpha = 0.12f), modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .trainPilotBorder(1.5.dp, palette.danger.copy(alpha = 0.45f), shape)
                    .trainPilotClickable(shape) { onClear() }.testTag("${tagPrefix}_clear_filter_button")) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Close, null, tint = palette.danger, modifier = Modifier.size(18.dp))
                        Text(" $clearLabel", color = palette.danger, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
                            maxLines = 1)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weekdayHeaders.forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = palette.textSecondary,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val offset = month.atDay(1).dayOfWeek.value - 1
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0..5) {
                    Row(Modifier.fillMaxWidth().testTag("${tagPrefix}_calendar_week_$row"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0..6) {
                            val dayNum = row * 7 + col - offset + 1
                            if (dayNum !in 1..month.lengthOfMonth()) {
                                Spacer(Modifier.weight(1f).height(44.dp))
                            } else {
                                val date = month.atDay(dayNum)
                                val isEdge = date == start || date == end
                                val inRange = rangeMin != null && rangeMax != null && date in rangeMin..rangeMax
                                val isToday = date == today
                                val shape = RoundedCornerShape(10.dp)
                                Surface(
                                    shape = shape,
                                    color = when {
                                        isEdge -> palette.primary
                                        inRange -> palette.primary.copy(alpha = 0.28f)
                                        else -> palette.surfaceElevated
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp)
                                        .testTag("${tagPrefix}_calendar_day_$dayNum")
                                        .trainPilotBorder(if (isToday || isEdge) 1.5.dp else 1.dp,
                                            if (isToday || isEdge) palette.primary else palette.cardBorder, shape)
                                        .trainPilotClickable(shape) { onDayClick(date) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$dayNum", style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isEdge || inRange || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isEdge) palette.onPrimary else palette.textPrimary)
                                            if (hasMark(date)) Box(Modifier.size(4.dp).clip(CircleShape)
                                                .background(if (isEdge) palette.onPrimary else palette.primary))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            com.repforge.app.ui.theme.TrainPilotButton(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("${tagPrefix}_calendar_done")
            ) { Text(doneLabel, fontWeight = FontWeight.Bold) }
        }
    }
}
