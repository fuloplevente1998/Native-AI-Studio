package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.HealthJournalStore
import com.repforge.app.HealthConnectRepository
import com.repforge.app.HealthConnectLegacyBridge
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeHealthProviderData
import com.repforge.app.NativePrivacyControls
import com.repforge.app.SamsungHealthRepository
import com.repforge.app.ui.health.hourlySteps
import com.repforge.app.ui.health.HourlySteps
import com.repforge.app.ui.health.NativeHealthRecords
import com.repforge.app.ui.health.readHourlySteps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
internal fun StudioHourlyStepsChart(day: LocalDate, provider: String, origin: String, revision: Long, language: AppLanguage) {
    val context = LocalContext.current.applicationContext
    val palette = LocalTrainPilotPalette.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val visible = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val snapshot = key(day, provider, origin, revision, visible) {
        produceState<Pair<String, List<HourlySteps>>?>(null) {
            if (!visible) return@produceState
            try {
                val ticket = NativeDataReplacement.continuationForCurrentData(context)
                val result = withContext(Dispatchers.IO) {
                    val zone = ZoneId.systemDefault()
                    val now = Instant.now()
                    val store = HealthJournalStore.get(context)
                    val alternative = if (provider == HealthJournalStore.SAMSUNG) HealthJournalStore.HC
                        else HealthJournalStore.SAMSUNG
                    listOf(provider, alternative).firstNotNullOfOrNull { candidate ->
                        val privacy = if (candidate == HealthJournalStore.SAMSUNG) NativePrivacyControls.SAMSUNG_HEALTH
                            else NativePrivacyControls.HEALTH_CONNECT
                        val guard = {
                            coroutineContext.ensureActive()
                            ticket.requireCurrent(context)
                            check(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                            NativePrivacyControls.requireEnabled(context, privacy)
                        }
                        val candidateRows = try {
                            guard()
                            val readOrigin = if (candidate == provider) origin
                                else NativeHealthProviderData.day(store, candidate, day)?.let {
                                    it.optString("stepsOrigin").ifBlank { it.optString("activityOrigin") }
                                }.orEmpty()
                            when {
                                candidate == HealthJournalStore.SAMSUNG && now.atZone(zone).toLocalDate() == day -> {
                                    val measured = SamsungHealthRepository.readHourlySteps(
                                        context, day, now.atZone(zone).toLocalDateTime(), guard)
                                    hourlySteps(day, zone, now, emptyList()).map { it.copy(count = measured[it.hour]) }
                                }
                                candidate == HealthJournalStore.HC &&
                                    !day.isBefore(now.atZone(zone).toLocalDate().minusDays(29)) &&
                                    HealthConnectRepository.isSupported(context) -> {
                                    // One Health Connect priority model, regardless of Android version.
                                    val measured = if (android.os.Build.VERSION.SDK_INT >= 34)
                                        HealthConnectRepository.readHourlyStepsSuspend(context, day, readOrigin, guard)
                                    else HealthConnectLegacyBridge.readHourlySteps(context, day, readOrigin, guard)
                                    hourlySteps(day, zone, now, emptyList()).map { it.copy(count = measured[it.hour]) }
                                }
                                else -> readHourlySteps(NativeHealthRecords(store), day, candidate,
                                    readOrigin, zone, now)
                            }.also { guard() }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { emptyList() }
                        candidateRows.takeIf { list -> list.any { it.count != null } }?.let { candidate to it }
                    }
                }
                ticket.requireCurrent(context)
                value = result ?: (provider to emptyList())
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { value = provider to emptyList() }
        }.value
    }
    fun word(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }
    Text(word("Lépések óránként", "Steps by hour", "Schritte pro Stunde", "Pași pe oră"),
        color = palette.textPrimary, style = MaterialTheme.typography.titleSmall)
    val values = snapshot?.second
    if (values.isNullOrEmpty()) {
        Text(if (values == null) word("Betöltés…", "Loading…", "Wird geladen…", "Se încarcă…")
            else word("Nincs óránkénti adat.", "No hourly data.", "Keine Stundendaten.", "Fără date orare."),
            color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
        return
    }
    val actualProvider = snapshot?.first ?: provider
    Text(if (actualProvider == HealthJournalStore.SAMSUNG) "Samsung Health" else "Health Connect",
        color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
    var selection by remember(day, actualProvider, origin) { mutableIntStateOf(-1) }
    val active = selection.takeIf { it in 0..23 } ?: (values.indexOfLast { it.count != null }.takeIf { it >= 0 }
        ?: values.indexOfLast { !it.future }.coerceAtLeast(0))
    val maximum = (values.maxOfOrNull { it.count ?: 0L } ?: 0L).coerceAtLeast(1)
    val entry = values[active]
    val hourLabel = "%02d:00–%02d:00".format(Locale.ROOT, active, active + 1)
    val valueText = when {
        entry.future -> word("Később", "Upcoming", "Später", "Mai târziu")
        entry.count == null -> word("Nincs adat", "No data", "Keine Daten", "Fără date")
        else -> "${entry.count} " + word("lépés", "steps", "Schritte", "pași")
    }
    Canvas(Modifier.fillMaxWidth().height(72.dp).testTag("health_hourly_steps_chart")
        .trainPilotChartTap(day, { x, width -> (x / width * 24f).toInt().coerceIn(0, 23) },
            "$hourLabel · $valueText") { selection = it }
        .semantics {
            customActions = listOf(
                CustomAccessibilityAction(word("Előző óra", "Previous hour", "Vorherige Stunde", "Ora anterioară")) {
                    selection = (active - 1).coerceAtLeast(0); true
                },
                CustomAccessibilityAction(word("Következő óra", "Next hour", "Nächste Stunde", "Ora următoare")) {
                    selection = (active + 1).coerceAtMost(23); true
                }
            )
        }) {
        val pitch = size.width / values.size
        for (row in values) {
            val h = if (row.count == null) (if (row.future) 1.dp else 2.dp).toPx()
                else (size.height * row.count.toDouble() / maximum).toFloat().coerceAtLeast(4.dp.toPx())
            val color = when {
                row.hour == active -> palette.primary
                row.count != null -> trainPilotChartMarkColor(false, palette.primary, palette.textSecondary)
                row.future -> palette.cardBorder
                else -> palette.textSecondary.copy(alpha = 0.38f)
            }
            drawRect(color, Offset(pitch * row.hour + pitch * 0.16f, size.height - h),
                Size(pitch * 0.68f, h))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("00", "06", "12", "18", "24").forEach {
            Text(it, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
    TrainPilotChartInfoLine("$hourLabel · $valueText")
}
