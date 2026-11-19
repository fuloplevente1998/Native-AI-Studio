package com.repforge.app.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.trainPilotClickable
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 2776: records of one time window from one source shown on one line ("12 lépés · 15,42 kcal · 7,53 m").
 * Only different record types are merged; every record keeps its own value and its own detail card.
 */
internal fun groupHealthRecordRows(rows: List<HealthRecordRow>): List<List<HealthRecordRow>> {
    val groups = mutableListOf<MutableList<HealthRecordRow>>()
    rows.forEach { row ->
        val last = groups.lastOrNull()
        val head = last?.first()
        if (last != null && head != null && head.startMs == row.startMs && head.endMs == row.endMs &&
            head.source == row.source && head.channel == row.channel && last.none { it.type == row.type }) last += row
        else groups += mutableListOf(row)
    }
    return groups
}

/** Compact value of one record, the same numbers the detail card shows. */
internal fun healthRecordCompactValue(row: HealthRecordRow, language: String, stepsUnit: String, title: String): String {
    val data = JSONObject(row.data)
    val unit = (data.opt("unit") as? String).orEmpty().let { if (it == "steps") stepsUnit else it }
    if (row.type == "BloodPressureRecord") return "${healthRecordNumber(NativeHealthRecords.number(data, "systolic"), language)} / " +
        "${healthRecordNumber(NativeHealthRecords.number(data, "diastolic"), language)} $unit".trim()
    if (row.type == "SleepSessionRecord" && data.has("durationMinutes")) {
        NativeHealthRecords.number(data, "durationMinutes")?.let { return formatSleepDurationHero(it, language.lowercase(Locale.ROOT)) }
    }
    NativeHealthRecords.number(data, "value")?.let { return "${healthRecordNumber(it, language)} $unit".trim() }
    val count = data.optJSONArray("stages")?.length() ?: data.optJSONArray("samples")?.length() ?: 0
    return if (count > 0) "$title · $count" else title
}

private fun compactWindow(startMs: Long, endMs: Long): String = runCatching {
    val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val a = fmt.format(Instant.ofEpochMilli(startMs)); val b = fmt.format(Instant.ofEpochMilli(endMs))
    if (endMs <= startMs || a == b) a else "$a–$b"
}.getOrDefault("")

@Composable
internal fun HealthRecordCompactRow(group: List<HealthRecordRow>, language: String) {
    // 2778: one line per time window is the whole view; no second level of large cards.
    val colors = LocalTrainPilotColors.current
    val text = nativeResources(LocalContext.current, language)
    val head = group.first()
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("health_record_${head.id}"), verticalAlignment = Alignment.CenterVertically) {
            Text(compactWindow(head.startMs, head.endMs), color = colors.muted, fontSize = 12.sp,
                modifier = Modifier.width(88.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                group.forEachIndexed { index, row ->
                    if (index > 0) Text(" · ", color = colors.muted, fontSize = 13.sp)
                    val title = text.getString(NativeHealthRecords.title(row.type))
                    Text(healthRecordCompactValue(row, language, text.getString(R.string.health_records_steps), title),
                        color = colors.accentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("health_record_value_${row.id}"))
                }
            }
        }
        HorizontalDivider(color = colors.line)
    }
}
