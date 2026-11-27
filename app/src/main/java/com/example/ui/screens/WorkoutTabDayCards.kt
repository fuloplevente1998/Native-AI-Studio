package com.example.ui.screens
import com.example.ui.theme.glassCardFill
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.components.PageLayoutState
import com.example.ui.components.PageLayoutBlock
import com.example.ui.components.PageLayoutHorizontalRow

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalTrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.CalendarDayEvent
import com.example.model.CalendarEventStatus
import com.example.model.TrainingProgramModel
import com.example.ui.components.HomePlanTiming
import com.example.ui.components.ProgramTypeEmblemIcon
import com.example.ui.components.programEquipmentSummary
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.home.HomeWorkoutTarget
import com.repforge.app.ui.theme.TrainPilotGlassPrimaryButton
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotClickable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private fun sameDay(programId: String, dayModelId: String, rawDayId: String): Boolean =
    rawDayId.isNotBlank() && (dayModelId == rawDayId || dayModelId == "$programId:$rawDayId")

/**
 * 2779: next planned start of every day of the program, index → ISO instant. The highlighted day
 * uses the Home/Coach target; the others come from the saved calendar (earliest planned entry from
 * today on). A day without a planned entry has no date (nothing is invented).
 */
internal fun workoutDayNextStarts(
    program: TrainingProgramModel,
    events: List<CalendarDayEvent>,
    target: HomeWorkoutTarget?,
    now: ZonedDateTime
): Map<Int, String> {
    val today = now.toLocalDate()
    return program.days.mapIndexedNotNull { index, day ->
        val fromTarget = target?.takeIf { it.programId == program.id && sameDay(program.id, day.id, it.dayId) }?.startIso
        val fromCalendar = events.asSequence()
            .filter { it.status == CalendarEventStatus.PLANNED && it.programId == program.id && sameDay(program.id, day.id, it.dayId) }
            .mapNotNull { event ->
                val date = runCatching { LocalDate.parse(event.dateIso) }.getOrNull() ?: return@mapNotNull null
                if (date.isBefore(today)) return@mapNotNull null
                val time = runCatching { LocalTime.parse(event.timeLabel) }.getOrDefault(LocalTime.of(10, 0))
                date.atTime(time).atZone(now.zone).toInstant()
            }
            .minOrNull()?.toString()
        (fromTarget ?: fromCalendar)?.let { index to it }
    }.toMap()
}

/** Short date line that fits a third of the screen: "📅 Ma, 10:00", "📅 Holnap, 10:00", "📅 Szo 11.21. · 10:00". */
internal fun workoutDayDateLine(startIso: String, now: ZonedDateTime, language: AppLanguage): Pair<String, HomePlanTiming>? {
    val start = runCatching { Instant.parse(startIso).atZone(now.zone) }.getOrNull() ?: return null
    val time = start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    val day = start.toLocalDate(); val today = now.toLocalDate()
    fun t(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
    }
    return when {
        day == today -> t("📅 Ma, $time", "📅 Today, $time", "📅 Heute, $time", "📅 Azi, $time") to
            (if (start.isAfter(now)) HomePlanTiming.TODAY_UPCOMING else HomePlanTiming.TODAY_PASSED)
        day == today.plusDays(1) -> t("📅 Holnap, $time", "📅 Tomorrow, $time", "📅 Morgen, $time", "📅 Mâine, $time") to HomePlanTiming.LATER
        else -> {
            val weekday = start.format(DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag(language.code))).trimEnd('.')
                .replaceFirstChar { it.titlecase(Locale.forLanguageTag(language.code)) }
            "📅 $weekday ${start.format(DateTimeFormatter.ofPattern("MM.dd."))} · $time" to HomePlanTiming.LATER
        }
    }
}

/**
 * 2779: all days of the active program as equal-height cards. Up to three sit side by side;
 * four or more scroll sideways with three visible, starting at the next day.
 */
@Composable
internal fun WorkoutTabDayCards(
    program: TrainingProgramModel,
    events: List<CalendarDayEvent>,
    target: HomeWorkoutTarget?,
    language: AppLanguage,
    onOpenProgramEditor: (String, Int) -> Unit,
    onStartWorkoutDay: (Int) -> Unit,
    layout: PageLayoutState? = null
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val now = remember { ZonedDateTime.now() }
    val starts = remember(program, events, target) { workoutDayNextStarts(program, events, target, now) }
    val days = program.days
    if (days.isEmpty()) return
    val ownTarget = target?.takeIf { it.programId == program.id }
    val nextIndex = days.indexOfFirst { ownTarget != null && sameDay(program.id, it.id, ownTarget.dayId) }.let { if (it < 0) 0 else it }
    val dayTiles = days.map { PageLayoutBlock(it.id, it.localizedDayCode(language)) }
    val group = "program_days:${program.id}"
    val orderedDays = layout?.childOrder(group, dayTiles.map { it.id }) ?: dayTiles.map { it.id }
    val upcomingPosition = orderedDays.indexOf(days[nextIndex].id).coerceAtLeast(0)
    val scrolls = days.size > 3
    val gap = 8.dp
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("workout_tab_day_cards")) {
        val columns = days.size.coerceIn(1, 3)
        val cardWidth = (maxWidth - gap * (columns - 1)) / columns
        val scroll = rememberScrollState()
        val stepPx = with(LocalDensity.current) { (cardWidth + gap).toPx() }
        LaunchedEffect(upcomingPosition, scrolls, stepPx, layout?.draft == null) {
            if (scrolls && layout?.draft == null) scroll.scrollTo((upcomingPosition.coerceAtMost(days.size - 3) * stepPx).roundToInt())
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PageLayoutHorizontalRow(layout, group, dayTiles, gap,
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    .then(if (scrolls) Modifier.horizontalScroll(scroll) else Modifier),
                cardWidth = cardWidth
            ) { dayId, modifier ->
                    val index = days.indexOfFirst { it.id == dayId }
                    val day = days[index]
                    val primary = index == nextIndex
                    val shape = RoundedCornerShape(18.dp)
                    Surface(
                        shape = shape,
                        color = palette.glassCardFill(palette.surface),
                        modifier = modifier.fillMaxHeight()
                            .trainPilotGlassMaterial(palette, shape)
                            .trainPilotBorder(if (primary) 1.5.dp else 1.dp, if (primary) palette.primary else palette.cardBorder, shape)
                            .trainPilotClickable(shape) { onOpenProgramEditor(program.id, index) }
                            .testTag("workout_tab_day_card_$index")
                    ) {
                        Column(Modifier.fillMaxHeight().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(day.localizedDayCode(language), style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold, color = if (primary) palette.primary else palette.textPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                ProgramTypeEmblemIcon(programTitle = "${program.nameHu} ${day.titleHu}", isHighlighted = primary, size = 28.dp)
                            }
                            com.example.ui.components.secondaryWorkoutDayTitle(day.localizedDayCode(language), day.localizedTitle(language))?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = palette.glassTile(palette.surfaceElevated, elevated = true)) {
                                Text(programEquipmentSummary(day.exercises.ifEmpty { days.flatMap { it.exercises } },
                                    program.tagsHu.firstOrNull().orEmpty(), language),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = palette.primary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text(trW(language, "${day.exercises.size} gyak. • ~${day.durationMinutes} p",
                                "${day.exercises.size} ex. • ~${day.durationMinutes} min",
                                "${day.exercises.size} Üb. • ~${day.durationMinutes} Min",
                                "${day.exercises.size} ex. • ~${day.durationMinutes} min"),
                                style = MaterialTheme.typography.labelSmall, color = palette.textSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            starts[index]?.let { workoutDayDateLine(it, now, language) }?.let { (text, timing) ->
                                Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                    color = when (timing) {
                                        HomePlanTiming.TODAY_UPCOMING -> palette.success
                                        HomePlanTiming.TODAY_PASSED -> palette.warning
                                        else -> palette.textSecondary
                                    },
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("workout_tab_day_date_$index"))
                            }
                            // Buttons line up at the bottom of equal-height cards.
                            Spacer(Modifier.weight(1f))
                            TrainPilotGlassPrimaryButton(
                                onClick = { onStartWorkoutDay(index) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (primary) palette.primary else palette.surfaceElevated,
                                    contentColor = if (primary) palette.onPrimary else palette.textPrimary),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("workout_tab_day_start_$index")
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(strings.startAction, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
            }
            if (scrolls) {
                val first by remember(stepPx) { derivedStateOf { (scroll.value / stepPx).roundToInt() } }
                Row(Modifier.fillMaxWidth().testTag("workout_tab_day_dots"), horizontalArrangement = Arrangement.Center) {
                    days.indices.forEach { i ->
                        Box(Modifier.padding(horizontal = 3.dp).size(6.dp)
                            .background(if (i in first until first + 3) palette.primary else palette.cardBorder, CircleShape))
                    }
                }
            }
        }
    }
}
