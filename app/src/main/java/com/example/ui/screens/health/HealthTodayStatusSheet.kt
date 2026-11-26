package com.example.ui.screens

import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.glassStateTile
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import com.example.ui.components.trainPilotAnimatedExpansion

import com.example.model.workoutsOn

import com.example.ui.components.trainPilotAnimatedExpansion
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.example.ui.components.healthDetailsToggle
import com.example.ui.components.trainPilotChartTap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.SubScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@Composable
internal fun StudioTodayStatusDetailSheet(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    activeKey: String,
    today: java.time.LocalDate,
    onDismiss: () -> Unit,
    onOpenJournal: (Pair<String, String>?) -> Unit,
    onTogglePulseTrend: () -> Unit,
    onOpenWeekly: () -> Unit,
    onOpenLoggedWorkout: (String) -> Unit,
    onNavigateToSubScreen: (SubScreen) -> Unit,
    fitnessReading: BodyFitnessReading? = null
) {
    val palette = LocalTrainPilotPalette.current
    val maxDetailHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.66f).dp
    com.example.ui.components.TrainPilotScreenSheet(
        onDismiss = onDismiss,
        testTag = "health_today_tile_sheet",
        staticGlass = true
    ) {
        val currentDaily = uiState.dailyHealthDate == today.toString()
        val activeEnergy = if (currentDaily && "activeCalories" in uiState.dailyHealthAvailableFields)
            "${uiState.activeCaloriesKcal} kcal" else "—"
        val totalEnergy = if (currentDaily && "totalCalories" in uiState.dailyHealthAvailableFields)
            "${uiState.totalCaloriesKcal} kcal" else "—"
        val heartRange = if (currentDaily && uiState.minHrBpm > 0 && uiState.maxHrBpm > 0)
            "${uiState.minHrBpm}–${uiState.maxHrBpm} bpm" else tr(uiState.language, "nincs lekért napi tartomány", "no daily range fetched", "kein Tagesbereich geladen", "fără interval zilnic preluat")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.glassTile(palette.background.copy(alpha = 0.75f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                // Reserve the same bounded viewport before any of the six tiles load their data.
                .height(maxDetailHeight)
                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                .testTag("health_today_tile_expanded_card")
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val openHealthJournal: () -> Unit = {
                    val recordType = when (activeKey) {
                        "sleep" -> "SleepSessionRecord"
                        "steps" -> com.repforge.app.ui.health.NativeDetailedHealthMetrics.recordType("steps")
                        "calories" -> com.repforge.app.ui.health.NativeDetailedHealthMetrics.recordType("activeCalories")
                        "bodyFat" -> "BodyFatRecord"
                        "spo2" -> "OxygenSaturationRecord"
                        "vo2" -> "Vo2MaxRecord"
                        else -> null
                    }
                    val recordDay = fitnessReading?.atMs?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    } ?: today
                    onOpenJournal(recordType?.let { recordDay.toString() to it })
                }
                val (titleStr, onActionClick) = when (activeKey) {
                    "calories" -> listOf(
                        tr(uiState.language, "Aktív kalória részletek • $activeEnergy", "Active Calories Details • $activeEnergy", "Aktive Kalorien Details • $activeEnergy", "Detalii calorii active • $activeEnergy"),
                        if (currentDaily && "activeCalories" in uiState.dailyHealthAvailableFields) {
                            tr(uiState.language, "Aktív energia (edzés & mozgás): $activeEnergy • Teljes napi energia (nyugalmival): $totalEnergy",
                                "Active energy (workout & activity): $activeEnergy • Total daily energy (with resting): $totalEnergy",
                                "Aktive Energie: $activeEnergy • Tagesgesamtenergie: $totalEnergy",
                                "Energie activă: $activeEnergy • Energie zilnică totală: $totalEnergy")
                        } else {
                            tr(uiState.language, "A kiválasztott szolgáltatótól (${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}) ma még nem érkezett aktív kalória adat. A nyugalmi energia külön kerül elszámolásra.",
                                "No active calories fetched yet today from ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}. Resting energy is accounted separately.",
                                "Noch keine aktiven Kalorien von ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} geladen. Ruheenergie wird separat berechnet.",
                                "Fără calorii active de la ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} azi. Energia de repaus este calculată separat.")
                        },
                        tr(uiState.language, "Teljes egészségnapló", "Full health journal", "Gesamtes Gesundheitstagebuch", "Jurnalul de sănătate complet")
                    ) to openHealthJournal
                    "pulse" -> {
                        val pulseReading = uiState.todayPulseReading
                        val pulseMain = pulseReading?.mainLabelHu ?: if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm} bpm" else "—"
                        listOf(
                            tr(uiState.language, "Mai pulzus • $pulseMain", "Today's Heart Rate • $pulseMain", "Heutiger Puls • $pulseMain", "Pulsul de azi • $pulseMain"),
                            if (pulseReading != null && pulseReading.state == com.repforge.app.healthreadings.ReadingState.AVAILABLE) {
                                tr(uiState.language, "Pontos típus: ${pulseReading.averageDescriptionHu ?: "Időbélyeges mérés"} • Nyugalmi pulzus: ${if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"} • Tartomány: $heartRange",
                                    "Reading type: ${pulseReading.averageDescriptionHu ?: "Timestamped reading"} • Resting HR: ${if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"} • Range: $heartRange",
                                    "Messart: ${pulseReading.averageDescriptionHu ?: "Zeitstempel-Messung"} • Ruhepuls: ${if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"} • Bereich: $heartRange",
                                    "Tip măsurătoare: ${pulseReading.averageDescriptionHu ?: "Măsurătoare"} • Puls repaus: ${if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"} • Interval: $heartRange")
                            } else {
                                pulseReading?.explanationHu ?: tr(uiState.language, "Nincs lekért pulzusmérés a kiválasztott szolgáltatótól.", "No heart rate reading fetched from the selected provider.", "Keine Pulsmessung vom ausgewählten Dienst geladen.", "Nicio măsurătoare de puls preluată de la furnizorul selectat.")
                            },
                            tr(uiState.language, "Korábbi mai pulzusmérések megtekintése lent", "View earlier pulse readings below", "Frühere Pulsmessungen unten ansehen", "Vezi măsurătorile anterioare de puls mai jos")
                        ) to onTogglePulseTrend
                    }
                    "steps" -> listOf(
                        tr(uiState.language, "Lépések részletek • ${if ("steps" in uiState.dailyHealthAvailableFields) "${uiState.dailySteps}" else "—"} / ${uiState.stepsGoal}", "Steps Details • ${if ("steps" in uiState.dailyHealthAvailableFields) "${uiState.dailySteps}" else "—"} / ${uiState.stepsGoal}", "Schritte Details • ${if ("steps" in uiState.dailyHealthAvailableFields) "${uiState.dailySteps}" else "—"} / ${uiState.stepsGoal}", "Detalii pași • ${if ("steps" in uiState.dailyHealthAvailableFields) "${uiState.dailySteps}" else "—"} / ${uiState.stepsGoal}"),
                        if ("steps" in uiState.dailyHealthAvailableFields) {
                            tr(uiState.language, "Teljesítés: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Becsült táv: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                "Completion: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Est. distance: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                "Erfüllung: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Gesch. Distanz: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km",
                                "Progres: ${((uiState.dailySteps.toFloat() / uiState.stepsGoal.coerceAtLeast(1)) * 100).toInt()}% • Dist. est.: ${"%.1f".format(uiState.dailySteps * 0.00075f)} km")
                        } else {
                            tr(uiState.language, "Ma még nem érkezett lépésadat a kiválasztott szolgáltatótól (${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}). Napi cél: ${uiState.stepsGoal} lépés.",
                                "No steps data fetched yet today from ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}. Daily target: ${uiState.stepsGoal} steps.",
                                "Noch keine Schrittdaten von ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} geladen. Tagesziel: ${uiState.stepsGoal} Schritte.",
                                "Fără date despre pași de la ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} azi. Obiectiv zilnic: ${uiState.stepsGoal} pași.")
                        },
                        tr(uiState.language, "Teljes egészségnapló", "Full health journal", "Gesamtes Gesundheitstagebuch", "Jurnalul de sănătate complet")
                    ) to openHealthJournal
                    "workout" -> {
                        val todaysWorkouts = uiState.loggedWorkouts.workoutsOn(today)
                        val latestTodayTitle = todaysWorkouts.firstOrNull()?.localizedTitle(uiState.language, uiState.programs, uiState.exercises)
                        listOf(
                            tr(uiState.language, "Mai edzések • ${todaysWorkouts.size} rögzített edzés", "Today's Workouts • ${todaysWorkouts.size} logged", "Heutige Trainings • ${todaysWorkouts.size} erfasst", "Antrenamente azi • ${todaysWorkouts.size} înregistrate"),
                            if (todaysWorkouts.isNotEmpty()) {
                                tr(uiState.language, "Utolsó mai edzés: ${latestTodayTitle ?: "Edzés"}",
                                    "Latest workout today: ${latestTodayTitle ?: "Workout"}",
                                    "Letztes Training heute: ${latestTodayTitle ?: "Training"}",
                                    "Ultimul antrenament azi: ${latestTodayTitle ?: "Antrenament"}")
                            } else {
                                tr(uiState.language, "Ma még nincs elmentett edzés.",
                                    "No saved workout yet today.",
                                    "Heute noch kein gespeichertes Training.",
                                    "Niciun antrenament salvat azi.")
                            },
                            if (todaysWorkouts.isNotEmpty()) {
                                tr(uiState.language, "Edzésnapló részletek megnyitása >", "Open Workout Log details >", "Trainingsprotokoll-Details öffnen >", "Deschide detaliile jurnalului >")
                            } else {
                                tr(uiState.language, "Edzésprogramok megnyitása >", "Open Workout Programs >", "Trainingsprogramme öffnen >", "Deschide programele de antrenament >")
                            }
                        ) to {
                            val firstId = todaysWorkouts.firstOrNull()?.id
                            if (firstId != null) onOpenLoggedWorkout(firstId) else onNavigateToSubScreen(SubScreen.PROGRAMS_LIST)
                        }
                    }
                    "bodyFat", "spo2", "vo2" -> {
                        val title = when (activeKey) {
                            "bodyFat" -> tr(uiState.language, "Testzsír", "Body fat", "Körperfett", "Grăsime")
                            "spo2" -> "SpO₂"
                            else -> "VO₂max"
                        }
                        val reading = fitnessReading
                        val providerLabel = if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"
                        val quantity = reading?.let {
                            String.format(java.util.Locale.getDefault(), "%.1f", it.value) +
                                (if (activeKey == "vo2") " ml/kg/min" else " %")
                        } ?: "—"
                        val measurementLabel = reading?.let {
                            val at = java.time.Instant.ofEpochMilli(it.atMs).atZone(java.time.ZoneId.systemDefault())
                            "$providerLabel · " + at.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        } ?: tr(uiState.language, "Nincs elérhető, naplózott mérés a kiválasztott szolgáltatótól.",
                            "No recorded measurement available from the selected provider.",
                            "Keine gespeicherte Messung vom gewählten Dienst verfügbar.",
                            "Nicio măsurătoare înregistrată de la furnizorul ales.")
                        listOf("$title • $quantity", measurementLabel,
                            tr(uiState.language, "Egészségnapló megnyitása", "Open health journal",
                                "Gesundheitstagebuch öffnen", "Deschide jurnalul de sănătate")) to openHealthJournal
                    }
                    "recovery" -> listOf(
                        tr(uiState.language, "Regeneráció", "Recovery", "Erholung", "Recuperare"),
                        "",
                        tr(uiState.language, "TrainPilot Coach megnyitása", "Open TrainPilot Coach",
                            "TrainPilot Coach öffnen", "Deschide TrainPilot Coach")
                    ) to { onNavigateToSubScreen(SubScreen.COACH_DETAIL) }
                    else -> {
                        val hasSleep = currentDaily && "sleepMinutes" in uiState.dailyHealthAvailableFields && uiState.sleepDurationLabel != "—"
                        val stageInfo = if (!hasSleep) {
                            tr(uiState.language, "Ma még nem érkezett alvásadat a kiválasztott szolgáltatótól (${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}).",
                                "No sleep data fetched yet today from ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"}.",
                                "Noch keine Schlafdaten von ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} geladen.",
                                "Fără date despre somn de la ${if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"} azi.")
                        } else if (uiState.sleepHasStages) {
                            uiState.sleepStagesSummaryHu
                        } else {
                            tr(uiState.language, "Alvásfázisok: a szolgáltató nem adott át fázisbontást (csak teljes időtartam: ${uiState.sleepDurationLabel}). Nem generálunk fiktív fázisokat.",
                                "Sleep stages: the provider did not supply stage breakdown (total duration only: ${uiState.sleepDurationLabel}). Fabricated stages are not generated.",
                                "Schlafphasen: der Dienst hat keine Phasenaufteilung geliefert (nur Gesamtdauer: ${uiState.sleepDurationLabel}). Keine fiktiven Phasen.",
                                "Faze somn: furnizorul nu a transmis detalii de fază (doar durata totală: ${uiState.sleepDurationLabel}).")
                        }
                        listOf(
                            tr(uiState.language, "Alvás részletek • ${uiState.sleepDurationLabel} (${uiState.sleepHoursDecimal})", "Sleep Details • ${uiState.sleepDurationLabel} (${uiState.sleepHoursDecimal})", "Schlaf Details • ${uiState.sleepDurationLabel} (${uiState.sleepHoursDecimal})", "Detalii somn • ${uiState.sleepDurationLabel} (${uiState.sleepHoursDecimal})"),
                            stageInfo,
                            tr(uiState.language, "Teljes egészségnapló", "Full health journal", "Gesamtes Gesundheitstagebuch", "Jurnalul de sănătate complet")
                        ) to openHealthJournal
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = titleStr[0],
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary,
                        modifier = Modifier.weight(1f).padding(top = 12.dp)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).testTag("health_today_tile_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = strings.closeAction, tint = palette.danger)
                    }
                }
                if (activeKey == "recovery") CompactRecoveryReadiness(uiState)
                if (activeKey != "pulse" && activeKey != "recovery") Text(
                    text = titleStr[1],
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary
                )
                if (activeKey == "pulse") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = palette.glassTile(palette.background.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotAnimatedExpansion()
                            .trainPilotBorder(
                                width = 1.dp,
                                color = if (uiState.expandedHealthPulseTrend) palette.primary.copy(alpha = 0.55f) else palette.cardBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .healthDetailsToggle(uiState.expandedHealthPulseTrend, uiState.language, RoundedCornerShape(14.dp)) {
                                        onTogglePulseTrend()
                                    }
                                    .testTag("health_pulse_trend_header"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = palette.glassStateTile(Color(0xFFEF4444), Color(0xFFEF4444).copy(alpha = 0.16f)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(uiState.language, "24 órás pulzustrend", "24h Heart Rate Trend", "24-Stunden-Pulstrend", "Tendință puls 24h"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    val restLbl = if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—"
                                    val peakLbl = if (uiState.maxHrBpm > 0) "${uiState.maxHrBpm} bpm" else "—"
                                    Text(
                                        text = tr(uiState.language, "Nyugalmi: $restLbl • Csúcs: $peakLbl", "Resting: $restLbl • Peak: $peakLbl", "Ruhe: $restLbl • Spitze: $peakLbl", "Repaus: $restLbl • Vârf: $peakLbl"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = if (uiState.expandedHealthPulseTrend) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = palette.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            var selectedPulse by remember(uiState.dailyPulseHistory) {
                                androidx.compose.runtime.mutableIntStateOf(uiState.dailyPulseHistory.lastIndex)
                            }
                            // Full-width dark sparkline box with red wave curve (Screenshot 9)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.glassTile(palette.surfaceCard.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .trainPilotBorder(1.dp, palette.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    .trainPilotClickable(RoundedCornerShape(14.dp)) { onTogglePulseTrend() }
                            ) {
                                if (uiState.dailyPulseHistory.size < 2) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            if (uiState.dailyPulseHistory.isEmpty()) {
                                                tr(uiState.language, "Nincs lekért pulzus-idősor", "No heart rate time series fetched", "Keine Puls-Zeitreihe geladen", "Fără serie temporală de puls")
                                            } else {
                                                tr(uiState.language, "Egy időbélyeges pulzusmérés érhető el", "One timestamped heart rate reading available", "Eine Pulsmessung mit Zeitstempel verfügbar", "O măsurătoare de puls disponibilă")
                                            },
                                            color = palette.textSecondary, style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else {
                                    val readings = uiState.dailyPulseHistory
                                    // 2785: tap → nearest reading highlighted; time + bpm in the info line below.
                                    val fractions = remember(readings) { pulseTimeFractions(readings) }
                                    val pulseLine = Color(0xFFEF4444)
                                    val pulseDotCenter = palette.surfaceCard
                                    val selectedLabel = readings.getOrNull(selectedPulse)?.let {
                                        "${pulseClockLabel(it.timeIso)} · ${it.bpm} bpm"
                                    }
                                    Canvas(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp)
                                        .testTag("health_pulse_real_trend")
                                        .trainPilotChartTap(fractions, { x, w -> com.example.ui.components.chartNearestByFraction(x, w, fractions) },
                                            selectedLabel = selectedLabel) { selectedPulse = it }) {
                                        val first = java.time.Instant.parse(readings.first().timeIso).toEpochMilli()
                                        val last = java.time.Instant.parse(readings.last().timeIso).toEpochMilli()
                                        val delta = (last - first).coerceAtLeast(1L).toFloat()
                                        val lowest = readings.minOf { it.bpm }.toFloat()
                                        val spread = (readings.maxOf { it.bpm } - readings.minOf { it.bpm })
                                            .toFloat().coerceAtLeast(1f)
                                        val line = Path()
                                        var prev: Offset? = null
                                        readings.forEach { sample ->
                                            val time = java.time.Instant.parse(sample.timeIso).toEpochMilli()
                                            val x = ((time - first) / delta).coerceIn(0f, 1f) * size.width
                                            val y = size.height - (sample.bpm - lowest) / spread * size.height
                                            // 2784: curved 24 h line.
                                            val p = prev
                                            if (p == null) line.moveTo(x, y) else { val mx = (p.x + x) / 2f; line.cubicTo(mx, p.y, mx, y, x, y) }
                                            prev = Offset(x, y)
                                        }
                                        drawPath(line, color = pulseLine,
                                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                                        readings.getOrNull(selectedPulse)?.let { sample ->
                                            val x = (fractions.getOrElse(selectedPulse) { 1f }) * size.width
                                            val y = size.height - (sample.bpm - lowest) / spread * size.height
                                            drawLine(palette.primary.copy(alpha = 0.55f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
                                            drawCircle(palette.primary, radius = 5.dp.toPx(), center = Offset(x, y))
                                            drawCircle(pulseDotCenter, radius = 2.dp.toPx(), center = Offset(x, y))
                                        }
                                    }
                                }
                            }
                            uiState.dailyPulseHistory.getOrNull(selectedPulse)?.takeIf { uiState.dailyPulseHistory.size >= 2 }?.let { sample ->
                                Spacer(modifier = Modifier.height(4.dp))
                                com.example.ui.components.TrainPilotChartInfoLine("${pulseClockLabel(sample.timeIso)} · ${sample.bpm} bpm",
                                    Modifier.testTag("health_pulse_chart_info"))
                            }

                            if (uiState.expandedHealthPulseTrend) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = palette.cardBorder)
                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.glassTile(palette.surfaceCard),
                                    modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(16.dp))
                                        .fillMaxWidth()
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = tr(uiState.language, "Mai pulzus", "Today's Heart Rate", "Heutiger Puls", "Pulsul de azi"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PulseInnerBox(
                                                label = tr(uiState.language, "Átlagpulzus", "Avg HR", "Ø Puls", "Puls mediu"),
                                                value = if (uiState.currentHrBpm > 0) "${uiState.currentHrBpm} bpm" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = strings.restingHrTitle,
                                                value = if (uiState.restingHrBpm > 0) "${uiState.restingHrBpm} bpm" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PulseInnerBox(
                                                label = tr(uiState.language, "Max. pulzus", "Max HR", "Max. Puls", "Puls max."),
                                                value = if (uiState.maxHrBpm > 0) "${uiState.maxHrBpm} bpm" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PulseInnerBox(
                                                label = tr(uiState.language, "Min. pulzus", "Min HR", "Min. Puls", "Puls min."),
                                                value = if (uiState.minHrBpm > 0) "${uiState.minHrBpm} bpm" else "—",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .trainPilotClickable(RoundedCornerShape(12.dp)) { onDismiss(); onNavigateToSubScreen(SubScreen.COACH_DETAIL) }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(uiState.language, "Regenerációs előzmények", "Recovery History", "Erholungsverlauf", "Istoric recuperare"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = palette.primary
                                    )
                                }
                            }
                        }
                    }
                }
                // 2788: the sheet is about TODAY; the 7-day steps/workout history lives only in Weekly goals.
                TodayTileSheetExtras(activeKey, uiState, today, onOpenWeekly = {
                    onDismiss()
                    onOpenWeekly()
                })
                if (activeKey != "pulse") Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.glassStateTile(palette.primary, palette.primary.copy(alpha = 0.16f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .trainPilotClickable(RoundedCornerShape(10.dp)) { onDismiss(); onActionClick() }
                        .testTag("health_expanded_action_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = titleStr[2],
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
