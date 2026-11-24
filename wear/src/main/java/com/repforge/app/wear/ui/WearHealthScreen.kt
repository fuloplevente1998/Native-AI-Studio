package com.repforge.app.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONObject

@Composable
internal fun HealthScreen(
    data: JSONObject?,
    enabled: Boolean,
    gps: Boolean,
    hasWorkout: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onGps: () -> Unit,
    home: WatchHomeSnapshot?,
    onRefresh: () -> Unit,
    onOpenJournal: () -> Unit,
    onSelectMeasurement: (MiniJournalWorkout) -> Unit,
    onBack: () -> Unit,
) {
    fun value(key: String, unit: String, divisor: Double = 1.0): String {
        val number = data?.takeIf { it.has(key) && !it.isNull(key) }?.optDouble(key)
        return if (number == null || !number.isFinite()) "—"
        else "${trimWearNumber(number/divisor)} $unit"
    }
    RoundList(wearText("Órás mérések")) {
        if (!hasWorkout && data != null) centerItem { Label(wearText("Legutóbbi edzés mérése"), size = 10) }
        centerItem {
            Text(
                wearRecordingStatus(data, enabled, hasWorkout),
                color = Gold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
        // Put the actual reason beside the failure, before the empty metric rows.
        data?.optString("message")?.takeIf { it.isNotBlank() }?.let { message ->
            centerItem {
                Text(message, color = Gold, fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp).testTag("wear-health-message"))
            }
        }
        data
            ?.optLong("updatedAt")
            ?.takeIf { it > 0L }
            ?.let {
                centerItem {
                    Label(
                        "Frissítve: ${Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss"))}",
                        size = 10,
                    )
                }
            }
        if (wearRecordingNeedsRetry(data, enabled, hasWorkout)) {

            centerItem { Pill(wearRecordingRetryLabel(data), tone = Gold, onClick = onEnable) }
        }

        val fresh =
            data?.optString("state") == "active" &&
                data.optString("heartRateAvailability") !in listOf("acquiring", "unavailable") &&
                System.currentTimeMillis() -
                    (data.optLong("lastSampleAt", data.optLong("updatedAt"))) < 15000
        centerItem { Label(if (fresh) wearText("Élő pulzus") else wearText("Pulzus · utolsó mérés"), size = 10) }
        centerItem { Title(value("heartRate", "bpm"), 28) }
        for ((title, metric, unit) in
            listOf(
                Triple(wearText("Átlagpulzus"), "averageHeartRate", "bpm"),
                Triple(wearText("Max. pulzus"), "maxHeartRate", "bpm"),
                Triple(wearText("Összes energia"), "totalCalories", "kcal"),
                Triple(wearText("Lépések"), "steps", wearText("lépés")),
                Triple(wearText("Aktív mérési idő"), "activeDurationSeconds", "s"),
                Triple(wearText("Távolság"), "distanceMeters", "m"),
                Triple(wearText("Sebesség"), "speedMps", "m/s"),
            )) {
            centerItem { Label(title, size = 10) }
            centerItem { Title(value(metric, unit), 17) }
        }
        val speed = data?.optDouble("speedMps", 0.0) ?: 0.0
        if (speed > 0 && speed.isFinite()) {
            centerItem { Label(wearText("Aktuális tempó")) }
            centerItem { Title("${formatSeconds((1000/speed).toInt())} /km", 17) }
        }
        detailsItem(wearText("A hiányzó adatot az óra még nem adta át. Az összes kcal az alapanyagcserét is tartalmazza."))
        if (data?.optBoolean("partial") == true)
            centerItem {
                Text(
                    wearText("Részleges mérés: a rögzítés edzés közben indult újra."),
                    color = Gold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        centerItem {
            Pill(
                if (enabled) wearText("Mérés kikapcsolása") else wearText("Mérések engedélyezése"),
                tone = if (enabled) Secondary else Gold,
                onClick = if (enabled) onDisable else onEnable,
            )
        }
        centerItem {
            Pill(if (gps) wearText("GPS kikapcsolása") else wearText("GPS engedélyezése"), tone = Card, onClick = onGps)
        }
        centerItem {
            Text(
                wearText("GPS csak mozgásos edzésnél használható, a következő mérés indításától. Az óra az engedélyezett edzésmérést a háttérben is folytatja."),
                color = Muted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }

        centerItem { Label(wearText("Korábbi órás mérések"), Gold, 13) }
        centerItem { Label(wearText("Mentett edzések · TrainPilot óra"), size = 10) }
        val previous = WatchMiniJournal.measurementHistory(home?.recentWorkouts.orEmpty()).take(3)
        if (previous.isEmpty()) {
            centerItem {
                Text(
                    if (home?.journalAvailable == true)
                        wearText("A szinkronizált edzések között még nincs órás mérési összesítés.")
                    else wearText("Az előzményekhez nyisd meg a telefonos TrainPilotot, majd frissíts."),
                    color = Muted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        previous.forEach { item ->
            centerItem {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Card, RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button) { onSelectMeasurement(item) }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        item.dayName,
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Label(miniJournalDate(item.started), size = 10)
                    val duration =
                        if (item.durationSeconds < 60) "${item.durationSeconds} s"
                        else "${item.durationSeconds / 60} perc"
                    Label("$duration · ${item.exercises.size} gyakorlat", size = 10)
                    item.exercises.take(2).forEach { Label(it.name, size = 10) }
                    val metrics = item.wear!!
                    miniNumber(metrics.totalCalories)?.let {
                        Label("Összes mért energia: $it kcal", Gold, 11)
                    }
                    miniNumber(metrics.averageHeartRate, 1)?.let {
                        Label("Átlagpulzus: $it bpm", size = 11)
                    }
                    if (metrics.partial) Label(wearText("Részleges mérés"), Gold, 10)
                    Label(wearText("Edzés részletei ›"), size = 10)
                }
            }
        }
        centerItem { Pill(wearText("Mini napló megnyitása"), tone = Card, onClick = onOpenJournal) }
        centerItem { Pill(wearText("Előzmények frissítése"), tone = Secondary, onClick = onRefresh) }
        centerItem { Pill("‹ Vissza", tone = Secondary, onClick = onBack) }
    }
}
