package com.repforge.app.ui.health

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Always composed with its day; folding the day keeps the current bounded page in memory. */
@Composable
internal fun HealthRecordDayDetails(day: LocalDate, provider: String, source: String, type: String,
    language: String, generation: Long, refreshKey: String, visible: Boolean) {
    val context = LocalContext.current
    val text = nativeResources(context, language)
    var cursors by rememberSaveable(day, provider, source, type, generation, refreshKey) { mutableStateOf(listOf("")) }
    var cached by remember(day, provider, source, type, generation, refreshKey) { mutableStateOf<HealthRecordPage?>(null) }
    var error by remember(day, provider, source, type, generation, refreshKey) { mutableStateOf(false) }
    val cursor = cursors.last().takeIf { it.isNotEmpty() }?.split(":")?.let { HealthRecordCursor(it[0].toLong(), it[1].toLong()) }
    val query = HealthRecordQuery(day, provider, source, type, cursor, generation)
    // Folding and unfolding retain the current page; measurement JSON never enters saved-instance state.
    val isVisible by rememberUpdatedState(visible)
    LaunchedEffect(query, refreshKey) {
        if (cached != null || error) return@LaunchedEffect
        // Wait for the first expansion, but do not cancel an in-flight SQLite read on folding.
        // Its result remains cached, so opening the same day again never repeats that read.
        snapshotFlow { isVisible }.first { it }
        try {
            val ticket = NativeDataReplacement.continuationForCurrentData(context)
            val page = withContext(Dispatchers.IO) {
                NativeDataReplacement.mutate(context) { NativeHealthRecords(HealthJournalStore.get(context)).load(query) }
            }
            ticket.requireCurrent(context)
            cached = page
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = true }
    }
    if (!visible) return
    if (error) Text(text.getString(R.string.health_history_error), color = MaterialTheme.colorScheme.error)
    else if (cached == null) LinearProgressIndicator(Modifier.fillMaxWidth())
    else {
        val page = cached!!
        // At most 25 record cards, with the existing 20-item sample/stage pagination.
        if (page.rows.isEmpty()) Text(text.getString(R.string.health_records_empty))
        page.rows.forEach { row -> key(row.channel, row.type, row.id) {
            HealthRecordCard(row, language, page.origins.firstOrNull { it.source == row.source }?.name.orEmpty())
        } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { cursors = cursors.dropLast(1); cached = null; error = false }, enabled = cursors.size > 1,
                modifier = Modifier.weight(1f).testTag("journal_records_previous_$day")) { Text(text.getString(R.string.health_records_newer)) }
            Text(text.getString(R.string.health_records_page, cursors.size))
            TextButton(onClick = { cursors = cursors + "${page.next!!.id}:${page.next.time}"; cached = null; error = false },
                enabled = page.next != null, modifier = Modifier.weight(1f).testTag("journal_records_next_$day")) {
                Text(text.getString(R.string.health_records_older))
            }
        }
    }
}
