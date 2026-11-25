package com.repforge.app.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun CalendarScreen(
    home: WatchHomeSnapshot?,
    canStart: Boolean,
    onBack: () -> Unit,
    onDays: () -> Unit = {},
    mainPage: Boolean = false,
    onDetailChange: (Boolean) -> Unit = {},
    onStart: (WatchCalendarEntry) -> Unit,
) {
    val today = LocalDate.now()
    val entries = home?.calendar.orEmpty().filter { wearCalendarDate(it) != null }
    val weeks = wearCalendarWeeks(entries, today)
    var weekKey by rememberSaveable { mutableStateOf(wearCalendarMonday(today).toString()) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val week = runCatching { LocalDate.parse(weekKey) }.getOrDefault(wearCalendarMonday(today))
    val detailCallback by rememberUpdatedState(onDetailChange)
    LaunchedEffect(selectedKey) { detailCallback(selectedKey != null) }
    DisposableEffect(Unit) { onDispose { detailCallback(false) } }
    BackHandler(enabled = selectedKey != null) { selectedKey = null }
    selectedKey?.let { key ->
        val selected = LocalDate.parse(key)
        CalendarDayScreen(
            selected,
            entries.filter { wearCalendarDate(it) == selected },
            today,
            canStart,
            onBack = { selectedKey = null },
            onStart = onStart,
        )
        return
    }
    if (home == null || entries.isEmpty()) {
        RoundList(wearText("Naptár")) {
            centerItem {
                Text(
                    wearText("A naptár frissítéséhez nyisd meg a TrainPilotot a telefonon."),
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
            centerItem { Pill(WearMenu216.text("days"), tone = Card, onClick = onDays) }
            centerItem { Pill(if (mainPage) wearText("‹ Kezdőlap") else wearText("‹ Főmenü"), tone = Secondary, onClick = onBack) }
        }
        return
    }
    if (mainPage) {
        // 2808: the swipe page shows the pre-#368 week grid again (user preference). The main
        // pager owns horizontal gestures, so this page shows the current week; all cached weeks
        // stay swipeable in the calendar opened from the menu.
        CalendarWeekPage(wearCalendarMonday(today), entries, today, 1, 1, onBack, onDays,
            backDescription = WearMenu216.text("home")) { selectedKey = it.toString() }
        return
    }
    // Native paging consumes a horizontal drag before it can select a day.
    // Keep only cached weeks, as requested; no unknown future/history pages.
    key(weeks) {
        val pager =
            rememberPagerState(
                initialPage =
                    weeks.indexOf(week).takeIf { it >= 0 }
                        ?: weeks.indices.minByOrNull {
                            kotlin.math.abs(
                                java.time.temporal.ChronoUnit.DAYS.between(weeks[it], today)
                            )
                        }
                        ?: 0,
                pageCount = { weeks.size },
            )
        LaunchedEffect(pager.settledPage) { weekKey = weeks[pager.settledPage].toString() }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            key = { weeks[it].toString() },
        ) { page ->
            CalendarWeekPage(weeks[page], entries, today, page + 1, weeks.size, onBack, onDays) {
                selectedKey = it.toString()
            }
        }
    }
}

@Composable
internal fun CalendarWeekPage(
    week: LocalDate,
    entries: List<WatchCalendarEntry>,
    today: LocalDate,
    pageNumber: Int,
    pageCount: Int,
    onBack: () -> Unit,
    onDays: () -> Unit,
    backDescription: String = wearText("Vissza a főmenübe"),
    onSelect: (LocalDate) -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    val captionScale = fontScale.coerceAtMost(1.25f)
    fun textSize(size: Float) = (size * captionScale / fontScale).sp
    val locale = java.util.Locale.getDefault()
    BoxWithConstraints(Modifier.fillMaxSize().testTag("wear-calendar-week")) {
        val layout = wearCalendarLayout(maxWidth.value, maxHeight.value)
        val caption = wearCalendarCaption(week, today, locale)
        Text(
            caption.month,
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .offset(y = layout.monthTop.dp)
                    .width((minOf(maxWidth, maxHeight) * .76f)),
            color = White,
            fontSize = textSize(if (minOf(maxWidth, maxHeight).value < 180f) 16f else 20f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            caption.dates,
            color = Muted,
            fontSize = textSize(10f),
            textAlign = TextAlign.Center,
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .offset(y = layout.weekTop.dp)
                    .width(minOf(maxWidth, maxHeight) * .76f)
                    .semantics {
                        contentDescription =
                            "${caption.dates} · $pageNumber / $pageCount"
                    },
            maxLines = 1,
        )
        val dayScale = minOf(1f, layout.buttonSize / 40f)
        val labels = (0..6).map { wearCalendarDayLabel(it,locale.language) }
        layout.days.forEachIndexed { dayIndex, point ->
            val date = week.plusDays(dayIndex.toLong())
            val records = entries.filter { wearCalendarDate(it) == date }
            val current = date == today
            val color =
                if (current) Black
                else if (records.isEmpty()) Muted else if (dayIndex == 6) DangerText else White
            Column(
                Modifier.align(Alignment.TopCenter)
                    .offset(x = point.x.dp, y = (point.y - layout.buttonSize / 2f).dp)
                    .size(layout.buttonSize.dp)
                    .background(
                        if (current) Gold else if (records.isEmpty()) Card else Secondary,
                        CircleShape,
                    )
                    .clickable(role = Role.Button) { onSelect(date) }
                    .semantics {
                        contentDescription =
                            date.format(DateTimeFormatter.ofPattern("MMMM d., EEEE", locale)) +
                                if (current) ", " + wearText("Ma") else ""
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    labels[dayIndex],
                    color = color,
                    fontSize = textSize(9f * dayScale),
                    lineHeight = textSize(10f * dayScale),
                    maxLines = 1,
                )
                Text(
                    "${date.dayOfMonth}",
                    color = color,
                    fontSize = textSize(17f * dayScale),
                    lineHeight = textSize(19f * dayScale),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            val status = records.map { it.status }
            val marker =
                when {
                    "planned" in status -> Gold
                    // 2809: the completed mint stays >= 3:1 on light watch palettes too.
                    "completed" in status -> wearEnsureContrast(Color(0xFF7FD4BA), listOf(Black), WEAR_CONTROL_CONTRAST)
                    "skipped" in status -> Muted
                    else -> null
                }
            if (marker != null)
                Box(
                    Modifier.align(Alignment.TopCenter)
                        .offset(x = point.x.dp, y = (point.y + layout.buttonSize / 2f + 2f).dp)
                        .size(4.dp)
                        .background(marker, CircleShape)
                )
        }
        // 2777 (A): "‹" back to the main menu + "Programnapok" replace the old "Menü" word.
        Row(
            Modifier.align(Alignment.TopCenter).offset(y = (layout.menuTop - 4f).dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(26.dp).background(Card, CircleShape)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = backDescription }
                    .testTag("wear-calendar-back"),
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = White, fontSize = textSize(14f), maxLines = 1) }
            Box(
                Modifier.height(26.dp).background(Card, RoundedCornerShape(13.dp))
                    .clickable(role = Role.Button, onClick = onDays)
                    .semantics { contentDescription = WearMenu216.text("days") }
                    .testTag("wear-calendar-days")
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(WearMenu216.text("days"), color = White, fontSize = textSize(10f), maxLines = 1) }
        }
    }
}

internal fun wearCalendarStatus367(status: String): String = when(status) {
    "completed" -> wearText("✓ Teljesítve")
    "skipped" -> wearText("Kihagyva")
    "rest" -> wearText("Pihenőnap")
    "planned" -> wearText("Tervezett")
    else -> "–"
}

@Composable
internal fun CalendarDayScreen(
    date: LocalDate,
    entries: List<WatchCalendarEntry>,
    today: LocalDate,
    canStart: Boolean,
    onBack: () -> Unit,
    onStart: (WatchCalendarEntry) -> Unit,
) {
    RoundList(
        date.format(
            DateTimeFormatter.ofPattern("MMM d., EEEE", java.util.Locale.getDefault())
        )
    ) {
        if (entries.isEmpty()) {
            centerItem {
                Text(
                    wearText("Ehhez a naphoz nincs adat az órán. A telefonos naptárban találod meg."),
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        entries.forEach { entry ->
            centerItem {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Card, RoundedCornerShape(18.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    entry.day?.let { day ->
                        Label(day.programName)
                        Title(day.name, 16)
                        Label(wearText("${day.exercises.size} gyakorlat"), size = 10)
                    }
                    Label(
                        wearCalendarStatus367(entry.status),
                        size = 11,
                    )
                    val time =
                        runCatching {
                                Instant.parse(entry.plannedStart)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                            }
                            .getOrNull()
                    if (time != null) Label(time, size = 10)
                    if (wearCalendarCanStart(entry, today, canStart)) {
                        Spacer(Modifier.height(6.dp))
                        Pill(wearText("▶ Edzés indítása")) { onStart(entry) }
                    } else if (
                        !canStart &&
                            entry.status == "planned" &&
                            entry.day != null &&
                            !date.isBefore(today)
                    ) {
                        Text(
                            wearText("Az aktív edzés vagy a függő szinkron lezárása után indítható."),
                            color = Muted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        centerItem { Pill(wearText("‹ Heti naptár"), tone = Secondary, onClick = onBack) }
    }
}
