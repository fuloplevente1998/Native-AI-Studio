package com.repforge.app.ui.health

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/** A read-only detail overlay: closing it never edits measurements or changes the selected summary. */
@Composable
internal fun HealthRecordBrowser(day: String, channel: String, type: String, language: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val resources = nativeResources(context, language)
    val colors = LocalTrainPilotColors.current
    val langCode = language.lowercase(Locale.ROOT)
    fun tr(hu: String, en: String, de: String, ro: String): String = when (langCode) {
        "en" -> en
        "de" -> de
        "ro" -> ro
        else -> hu
    }

    var query by remember(day, channel, type) { mutableStateOf(HealthRecordQuery(LocalDate.parse(day), channel, type = type)) }
    var previous by remember(day, channel, type) { mutableStateOf(emptyList<HealthRecordQuery>()) }
    var loaded by remember(day, channel, type) { mutableStateOf<Pair<HealthRecordQuery, HealthRecordPage>?>(null) }
    var failure by remember(day, channel, type) { mutableStateOf<Pair<HealthRecordQuery, Boolean>?>(null) }
    var reloadToken by remember(day, channel, type) { mutableStateOf(0) }
    var draftFrom by remember(day, channel, type) { mutableStateOf(LocalDate.parse(day)) }
    var draftTo by remember(day, channel, type) { mutableStateOf(LocalDate.parse(day)) }
    var activePicker by remember { mutableStateOf<DatePickerDialog?>(null) }
    DisposableEffect(context, language, day, channel, type) {
        onDispose { activePicker?.dismiss(); activePicker = null }
    }
    val validRange = !draftFrom.isAfter(draftTo) &&
        HealthJournalStore.validDay(draftFrom.toString()) && HealthJournalStore.validDay(draftTo.toString())
    val rangeChanged = draftFrom != query.fromDay || draftTo != query.toDay
    val page = loaded?.takeIf { it.first == query }?.second
    val error = failure?.takeIf { it.first == query }
    val loading = page == null && error == null

    LaunchedEffect(query, reloadToken) {
        val requested = query
        val revision = reloadToken
        try {
            val result = withContext(Dispatchers.IO) {
                NativeDataReplacement.mutate(context) {
                    NativeHealthRecords(HealthJournalStore.get(context)).load(requested)
                }
            }
            currentCoroutineContext().ensureActive()
            // IO may finish after the user changes filters/reloads. Never commit a stale page.
            if (query == requested && reloadToken == revision) {
                loaded = requested to result
                failure = null
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            if (query == requested && reloadToken == revision)
                failure = requested to (error is NativeHealthRecords.ReplacedJournal)
        }
    }

    fun clearPage() {
        previous = emptyList()
        loaded = null
        failure = null
        reloadToken++
    }

    fun restart(source: String = query.source) {
        // Re-selecting the same query must also trigger a fresh load after a read failure.
        query = query.withFilters(source = source)
        clearPage()
    }

    fun applyRange() {
        if (!validRange || !rangeChanged) return
        query = query.withRange(draftFrom, draftTo)
        clearPage()
    }

    fun pickDate(selected: LocalDate, update: (LocalDate) -> Unit) {
        activePicker?.dismiss()
        // Keep the Activity window token/theme while using the app-selected language.
        val pickerContext = android.view.ContextThemeWrapper(context, context.theme).apply {
            applyOverrideConfiguration(resources.configuration)
        }
        val picker = DatePickerDialog(pickerContext, { _, year, month, pickedDay ->
            runCatching { LocalDate.of(year, month + 1, pickedDay) }
                .getOrNull()?.takeIf { HealthJournalStore.validDay(it.toString()) }?.let(update)
        }, selected.year, selected.monthValue - 1, selected.dayOfMonth)
        picker.setOnDismissListener { if (activePicker === picker) activePicker = null }
        activePicker = picker
        picker.show()
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .testTag("health_record_browser"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header (matches 17:30 mockup: TRAINPILOT / EGÉSZSÉG eyebrow + Részletes egészségmérések + Samsung Health · date)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tr("TRAINPILOT / EGÉSZSÉG", "TRAINPILOT / HEALTH", "TRAINPILOT / GESUNDHEIT", "TRAINPILOT / SĂNĂTATE"),
                    color = colors.accentText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (type == "SleepSessionRecord") {
                            resources.getString(R.string.health_record_sleep)
                        } else {
                            tr(
                                "Részletes egészségmérések",
                                "Detailed Health Measurements",
                                "Detaillierte Gesundheitsmessungen",
                                "Măsurători detaliate de sănătate"
                            )
                        },
                        color = colors.text,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp).testTag("health_records_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = resources.getString(R.string.health_records_close),
                            tint = colors.muted
                        )
                    }
                }
                val dateSubtitle = if (query.fromDay == query.toDay) {
                    formatHeaderDate(query.fromDay, language)
                } else {
                    "${formatHeaderDate(query.fromDay, language)} – ${formatHeaderDate(query.toDay, language)}"
                }
                Text(
                    text = "${healthChannelName(channel, resources)} · $dateSubtitle",
                    color = colors.muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Unified Elevated Filter Card (Kezdőnap -> Zárónap + Source Pills)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DateBoundBox(
                            label = resources.getString(R.string.health_range_from),
                            date = draftFrom,
                            testTag = "health_range_from",
                            onClick = { pickDate(draftFrom) { draftFrom = it } },
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(18.dp).padding(top = 16.dp)
                        )
                        DateBoundBox(
                            label = resources.getString(R.string.health_range_to),
                            date = draftTo,
                            testTag = "health_range_to",
                            onClick = { pickDate(draftTo) { draftTo = it } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!validRange) {
                        Text(
                            text = resources.getString(R.string.health_range_invalid),
                            color = colors.dangerText,
                            fontSize = 12.sp,
                            modifier = Modifier.testTag("health_range_error")
                        )
                    }

                    if (rangeChanged || !validRange) {
                        TrainPilotOutlinedButton(
                            onClick = ::applyRange,
                            enabled = validRange && rangeChanged,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_range_apply")
                        ) {
                            Text(resources.getString(R.string.health_range_apply), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Keep testTag accessible when range is unchanged without visual clutter
                        Box(modifier = Modifier.height(0.dp)) {
                            TextButton(
                                onClick = ::applyRange,
                                enabled = false,
                                modifier = Modifier.testTag("health_range_apply")
                            ) {
                                Text(resources.getString(R.string.health_range_apply))
                            }
                        }
                    }

                    page?.let { current ->
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                SourceFilterPill(
                                    label = tr("Összes forrás", "All sources", "Alle Quellen", "Toate sursele"),
                                    selected = query.source.isEmpty(),
                                    onClick = { restart("") }
                                )
                            }
                            items(current.origins, key = { it.source }) { origin ->
                                SourceFilterPill(
                                    label = friendlyOriginLabel(origin, channel, resources),
                                    selected = query.source == origin.source,
                                    onClick = { restart(origin.source) },
                                    modifier = Modifier.testTag("health_origin_${origin.source}")
                                )
                            }
                        }
                    }
                }
            }

            // Section Header: Mérési eredmények + Gold Refresh Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Mérési eredmények", "Measurement results", "Messergebnisse", "Rezultate măsurători"),
                    color = colors.text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(
                    onClick = { restart() },
                    enabled = !loading,
                    modifier = Modifier.size(48.dp).testTag("health_records_reload")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = resources.getString(R.string.health_records_reload),
                        tint = colors.accentText
                    )
                }
            }

            if (loading) {
                Text(resources.getString(R.string.health_history_loading), color = colors.muted, fontSize = 13.sp)
            }
            if (error != null) {
                Text(
                    text = resources.getString(if (error.second) R.string.health_records_changed else R.string.health_history_error),
                    color = colors.danger,
                    fontSize = 13.sp,
                    modifier = Modifier.testTag("health_records_error")
                )
            }

            page?.let { current ->
                key(query, reloadToken) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).testTag("health_records_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        if (current.rows.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = colors.card,
                                    border = BorderStroke(1.dp, colors.line),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = resources.getString(R.string.health_records_empty),
                                        color = colors.muted,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                        items(current.rows, key = { "${it.channel}:${it.type}:${it.id}" }) { row ->
                            HealthRecordCard(row, language)
                        }
                    }
                }

                // Compact Bottom Pagination Row (‹ 1. oldal ›)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = previous.isNotEmpty(),
                        modifier = Modifier.size(48.dp).testTag("health_records_previous"),
                        onClick = {
                            val last = previous.last()
                            previous = previous.dropLast(1)
                            query = last.copy(generation = current.generation)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = resources.getString(R.string.health_records_newer),
                            tint = if (previous.isNotEmpty()) colors.text else colors.muted.copy(alpha = 0.4f)
                        )
                    }
                    Text(
                        text = resources.getString(R.string.health_records_page, previous.size + 1),
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    IconButton(
                        enabled = current.next != null,
                        modifier = Modifier.size(48.dp).testTag("health_records_next"),
                        onClick = {
                            previous = previous + query
                            query = query.copy(cursor = current.next, generation = current.generation)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = resources.getString(R.string.health_records_older),
                            tint = if (current.next != null) colors.text else colors.muted.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateBoundBox(
    label: String,
    date: LocalDate,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTrainPilotColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .heightIn(min = 52.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = colors.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${date.year}.",
            color = colors.accentText,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = String.format(Locale.ROOT, "%02d.  %d.", date.monthValue, date.dayOfMonth),
            color = colors.text,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SourceFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTrainPilotColors.current
    val shape = RoundedCornerShape(14.dp)
    if (selected) {
        Button(
            onClick = onClick,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            modifier = modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        TrainPilotOutlinedButton(
            onClick = onClick,
            shape = shape,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            modifier = modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = label,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HealthRecordCard(row: HealthRecordRow, language: String) {
    val text = nativeResources(LocalContext.current, language)
    val colors = LocalTrainPilotColors.current
    val langCode = language.lowercase(Locale.ROOT)
    fun tr(hu: String, en: String, de: String, ro: String): String = when (langCode) {
        "en" -> en
        "de" -> de
        "ro" -> ro
        else -> hu
    }

    val data = remember(row.data) { JSONObject(row.data) }
    var expanded by rememberSaveable(row.channel, row.type, row.id) { mutableStateOf(false) }
    var offset by rememberSaveable(row.channel, row.type, row.id) { mutableStateOf(0) }
    val unit = (data.opt("unit") as? String).orEmpty().let { if (it == "steps") text.getString(R.string.health_records_steps) else it }
    val scalar = NativeHealthRecords.number(data, "value")
        ?: if (row.type == "SleepSessionRecord") NativeHealthRecords.number(data, "durationMinutes") else null
    val displayUnit = if (row.type == "SleepSessionRecord" && data.has("durationMinutes")) "min" else unit
    val friendlySource = friendlyPackageOrChannelName(row.source, row.channel, text)
    val count = data.optJSONArray("stages")?.length() ?: data.optJSONArray("samples")?.length() ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.line, RoundedCornerShape(22.dp))
            .testTag("health_record_${row.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Metric Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = healthRecordIcon(row.type),
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = text.getString(NativeHealthRecords.title(row.type)),
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Hero Value (e.g. "4 óra 28 perc" for Sleep, or scalar + unit)
            if (row.type == "BloodPressureRecord") {
                Text(
                    text = "${healthRecordNumber(NativeHealthRecords.number(data, "systolic"), language)} / ${healthRecordNumber(NativeHealthRecords.number(data, "diastolic"), language)} $unit",
                    color = colors.accentText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            } else if (scalar != null) {
                val heroText = if (row.type == "SleepSessionRecord" && data.has("durationMinutes")) {
                    formatSleepDurationHero(scalar, langCode)
                } else {
                    "${healthRecordNumber(scalar, language)} $displayUnit".trim()
                }
                Text(
                    text = heroText,
                    color = colors.accentText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            } else if (count == 0) {
                Text(
                    text = text.getString(R.string.health_history_no_data),
                    color = colors.muted,
                    fontSize = 14.sp
                )
            }

            // Compact Time Range + Friendly Source Subtitle
            val timeRangeLabel = formatRecordTimeWindow(row.startMs, row.endMs)
            Text(
                text = if (timeRangeLabel.isNotBlank()) {
                    "$timeRangeLabel · $friendlySource"
                } else {
                    "$friendlySource · ${tr("részletes mérés", "detailed measurement", "detaillierte Messung", "măsurătoare detaliată")}"
                },
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            HorizontalDivider(color = colors.line)

            // Key-value metadata rows & expandable stages/samples
            if (count > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp)
                        .testTag("health_record_expand_${row.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (row.type == "SleepSessionRecord") {
                            tr("Alvásfázisok", "Sleep stages", "Schlafphasen", "Faze de somn")
                        } else {
                            tr("Mérési adatok", "Measurement data", "Messdaten", "Date măsurători")
                        },
                        color = colors.muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            color = colors.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = colors.accentText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Forrásban módosítva", "Modified in source", "In Quelle geändert", "Modificat în sursă"),
                    color = colors.muted,
                    fontSize = 14.sp
                )
                Text(
                    text = healthRecordShortTime(row.modifiedMs, language),
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (expanded && count > 0) {
                HorizontalDivider(color = colors.line)
                val series = remember(row.data, offset) { NativeHealthRecordSeries.page(data, offset) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    series.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.card2)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (row.type == "SleepSessionRecord") {
                                    text.getString(NativeHealthRecordSeries.stageLabel(item.stage))
                                } else {
                                    "${healthRecordNumber(item.value, language)} $unit".trim()
                                },
                                color = colors.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val start = item.startMs?.let { healthRecordShortTime(it, language) } ?: "—"
                            val end = item.endMs?.let { "–${healthRecordShortTime(it, language)}" }.orEmpty()
                            Text(
                                text = start + end,
                                color = colors.muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (series.total > NativeHealthRecordSeries.PAGE_SIZE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                enabled = offset > 0,
                                onClick = { offset = maxOf(0, offset - NativeHealthRecordSeries.PAGE_SIZE) }
                            ) {
                                Text(text.getString(R.string.health_records_newer))
                            }
                            TextButton(
                                enabled = offset + NativeHealthRecordSeries.PAGE_SIZE < series.total,
                                onClick = { offset += NativeHealthRecordSeries.PAGE_SIZE }
                            ) {
                                Text(text.getString(R.string.health_records_older))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun healthRecordIcon(type: String): ImageVector = when (type) {
    "SleepSessionRecord" -> Icons.Default.Bedtime
    "HeartRateRecord", "RestingHeartRateRecord", "HeartRateVariabilityRmssdRecord" -> Icons.Default.FavoriteBorder
    "StepsRecord", "DistanceRecord", "FloorsClimbedRecord" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "OxygenSaturationRecord", "RespiratoryRateRecord", "Vo2MaxRecord", "BloodPressureRecord" -> Icons.Default.MonitorHeart
    else -> Icons.Default.Timeline
}

private fun formatSleepDurationHero(minutesValue: Double, langCode: String): String {
    val totalMinutes = minutesValue.roundToInt().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when (langCode) {
        "en" -> if (hours > 0) "$hours hr $mins min" else "$mins min"
        "de" -> if (hours > 0) "$hours Std. $mins Min." else "$mins Min."
        "ro" -> if (hours > 0) "$hours ore $mins min" else "$mins min"
        else -> if (hours > 0) "$hours óra $mins perc" else "$mins perc"
    }
}

private fun formatRecordTimeWindow(startMs: Long, endMs: Long): String = runCatching {
    val zone = ZoneId.systemDefault()
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val startZdt = Instant.ofEpochMilli(startMs).atZone(zone)
    val startStr = startZdt.format(timeFmt)
    if (endMs != startMs && endMs > 0L) {
        val endZdt = Instant.ofEpochMilli(endMs).atZone(zone)
        "$startStr–${endZdt.format(timeFmt)}"
    } else {
        startStr
    }
}.getOrDefault("")

private fun healthRecordShortTime(value: Long, language: String): String = runCatching {
    DateTimeFormatter.ofPattern("HH:mm").withLocale(Locale.forLanguageTag(language))
        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(value))
}.getOrDefault("—")

private fun formatHeaderDate(date: LocalDate, language: String): String = runCatching {
    val locale = Locale.forLanguageTag(language)
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)
}.getOrDefault(date.toString())

private fun friendlyOriginLabel(
    origin: HealthRecordOrigin,
    channel: String,
    text: android.content.res.Resources
): String {
    if (origin.name.isNotBlank() && !origin.name.contains('.')) return origin.name
    return friendlyPackageOrChannelName(origin.source, channel, text)
}

private fun friendlyPackageOrChannelName(
    source: String,
    channel: String,
    text: android.content.res.Resources
): String {
    val lower = source.lowercase(Locale.ROOT)
    return when {
        lower.contains("shealth") || lower.contains("samsung") -> "Samsung Health"
        lower.contains("google.android.apps.fitness") -> "Google Fit"
        lower.contains("healthconnect") || lower.contains("healthdata") -> "Health Connect"
        source.isNotBlank() && !source.contains('.') -> source
        else -> healthChannelName(channel, text)
    }
}

private fun healthRecordNumber(value: Double?, language: String): String = value?.let {
    NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 2 }.format(it)
} ?: "—"

private fun healthChannelName(channel: String, text: android.content.res.Resources): String = when (channel) {
    HealthJournalStore.HC -> "Health Connect"
    HealthJournalStore.SAMSUNG -> "Samsung Health"
    HealthJournalStore.BLE -> "BLE"
    else -> text.getString(R.string.health_history_title)
}
