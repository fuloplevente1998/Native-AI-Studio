package com.example.ui.viewmodel

import java.time.ZoneId
import com.example.model.localWorkoutDate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.repforge.app.HealthJournalStore
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.repforge.app.healthreadings.HealthReadingsProjection
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

fun TrainPilotViewModel.reloadHealthJournal() {
    val ctx = nativeContext
    val scope = nativeScope
    if (ctx == null || scope == null) {
        _uiState.update {
            it.copy(statusBannerMessage = "Az egészségügyi szolgáltatások nem elérhetők.")
        }
        return
    }
    // 2779: values are no longer blanked before every reload: the reload overwrites every field
    // below, so blanking first only made the Coach card and the Today tiles flash "—" and jump.
    // They are still cleared when the provider changed (old provider's data must not show) or
    // when the reload fails.
    fun clearedHealth(st: TrainPilotUiState) = st.copy(
            dailySteps = 0,
            currentHrBpm = 0,
            restingHrBpm = 0,
            maxHrBpm = 0,
            minHrBpm = 0,
            dailyPulseHistory = emptyList(), dailyPulseSampleCount = 0,
            activeCaloriesKcal = 0, totalCaloriesKcal = 0, dailyHealthDate = "", dailyHealthAvailableFields = emptySet(),
            sleepDurationLabel = "—",
            sleepHoursDecimal = "—",
            samsungEnergyScore = 0,
            trainPilotReadinessScore = 0,
            coachReadinessScore = 0,
            todayPulseReading = null,
            todayRecoveryReading = null,
            weeklyHealthDays = emptyList(),
            spo2Percent = 0f,
            vo2Max = 0f,
            bodyFatPercent = 0f,
            hrvRmssdMs = 0f,
            bloodPressureLabel = "—",
            bloodGlucoseLabel = "—",
            respiratoryRateLabel = "—",
            dailyDistanceKm = 0f
    )
    val provider = HealthJournalStore.HC
    if (provider != loadedHealthProvider) {
        loadedHealthProvider = null
        _uiState.update(::clearedHealth)
    }
    healthJournalReloadJob?.cancel()
    healthJournalReloadJob = scope.launch(Dispatchers.IO) {
        try {
            val store = HealthJournalStore.get(ctx)
            val prefs = store.preferences()
            val hcDailySourceRaw = prefs.optString("hcDailySource", "auto")
            val dailySourceModeHu = when (hcDailySourceRaw) {
                "auto", "" -> studioModesText("Automatikus (Health Connect prioritás, duplikáció nélkül)", "Automatic (Health Connect priority, no duplicates)", "Automatisch (Health Connect-Priorität, ohne Duplikate)", "Automat (prioritatea Health Connect, fără duplicate)")
                "priority" -> studioModesText("Prioritásos forrásválasztás", "Source priority", "Quellenpriorität", "Prioritatea surselor")
                com.repforge.app.HealthDailySource.SAMSUNG -> "Rögzített forrás: Samsung Health"
                else -> "Rögzített csomag: $hcDailySourceRaw"
            }
            val generation = store.generation()
            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val today = now.atZone(zone).toLocalDate()
            val projectedProviderDays = mutableListOf<JSONObject>()
            // The HC journal preserves older observations, including across temporary read
            // failures. Only the currently granted, successfully read fields may be shown.
            val privacy = com.repforge.app.NativePrivacyControls
            val primaryEnabled = privacy.enabled(ctx, privacy.HEALTH_CONNECT)
            val hcGranted = com.repforge.app.HealthConnectRepository.READ_PERMS
                .filter { com.repforge.app.HealthConnectRepository.isAllowed(ctx, it) }.toSet()
            fun admitted(dayRow: JSONObject?, fromProvider: String): JSONObject? =
                if (fromProvider != HealthJournalStore.HC) dayRow
                else if (primaryEnabled) com.repforge.app.HealthConnectVisibleDay.project(dayRow, hcGranted)
                else null
            val alternativeProvider = HealthJournalStore.SAMSUNG
            val alternativeEnabled = privacy.enabled(ctx, privacy.SAMSUNG_HEALTH)
            val todayData = if (primaryEnabled)
                admitted(com.repforge.app.NativeHealthProviderData.day(store, provider, today), provider) else null
            val alternateToday = if (alternativeEnabled)
                admitted(com.repforge.app.NativeHealthProviderData.day(store, alternativeProvider, today),
                    alternativeProvider) else null
            fun measuredSteps(row: JSONObject?, fromProvider: String): Double? {
                if (row == null) return null
                // A previously pinned origin cannot answer today's automatic-priority query, or vice versa.
                if (fromProvider == HealthJournalStore.HC &&
                    row.optString("activityOrigin") != com.repforge.app.HealthDailySource.choose("", hcDailySourceRaw, emptySet())) return null
                return row.optDouble("steps", Double.NaN).takeIf { it.isFinite() && it in 0.0..Int.MAX_VALUE.toDouble() }
            }
            val todaysSteps = measuredSteps(todayData, provider) ?: measuredSteps(alternateToday, alternativeProvider)

            val todaySleepMin = com.repforge.app.NativeDailyHealthDetails.sleepMinutes(
                todayData, today, java.time.ZoneId.systemDefault(), Instant.now()) ?: Double.NaN
            val todayPulseHistory = com.repforge.app.NativeDailyHealthDetails.pulseSeries(todayData)
            val hasSleepStages = runCatching {
                val sessions = todayData?.optJSONArray("sleepSessions")
                if (sessions != null && sessions.length() > 0) {
                    (0 until sessions.length()).any { idx ->
                        val s = sessions.optJSONObject(idx)
                        val stg = s?.optJSONArray("stages")
                        stg != null && stg.length() > 0
                    }
                } else false
            }.getOrDefault(false)
            val sleepStagesSummary = if (hasSleepStages) {
                studioModesText("Részletes alvásfázisok rögzítve (mély, REM, könnyű alvás)", "Sleep stages recorded (deep, REM, light)", "Schlafphasen aufgezeichnet (Tief-, REM-, Leichtschlaf)", "Stadii de somn înregistrate (profund, REM, ușor)")
            } else {
                studioModesText("A szolgáltató nem adott át alvásfázisbontást (csak teljes időtartam)", "Provider supplied total sleep duration only", "Anbieter lieferte nur die Gesamtschlafdauer", "Furnizorul a transmis doar durata totală a somnului")
            }
            val availableFields = (setOf("activeCalories", "totalCalories", "averageHeartRate",
                "restingHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples")
                .filter { key -> todayData?.optDouble(key, Double.NaN)?.let { it.isFinite() && it >= 0 } == true } +
                (if (todaysSteps != null) listOf("steps") else emptyList()) +
                (if (todaySleepMin.isFinite() && todaySleepMin > 0) listOf("sleepMinutes") else emptyList()) +
                (if (todayPulseHistory.isNotEmpty()) listOf("heartRateSeries") else emptyList())).toSet()
            val lastProviderSync = store.nativeSyncLastSuccess(provider)
            val stepsDbl = todaysSteps ?: Double.NaN
            val pulseDbl = todayData?.optDouble("averageHeartRate", Double.NaN) ?: Double.NaN
            val restingDbl = todayData?.optDouble("restingHeartRate", Double.NaN) ?: Double.NaN
            val maxHrDbl = todayData?.optDouble("maxHeartRate", Double.NaN) ?: Double.NaN
            val minHrDbl = todayData?.optDouble("minHeartRate", Double.NaN) ?: Double.NaN
            val caloriesDbl = todayData?.optDouble("activeCalories", Double.NaN) ?: Double.NaN
            val sleepMinDbl = todaySleepMin
            val hrvDbl = todayData?.optDouble("hrvRmssdMs", Double.NaN) ?: Double.NaN
            val sysDbl = todayData?.optDouble("bloodPressureSystolic", Double.NaN) ?: Double.NaN
            val diaDbl = todayData?.optDouble("bloodPressureDiastolic", Double.NaN) ?: Double.NaN
            val gluDbl = todayData?.optDouble("bloodGlucoseMmolL", Double.NaN) ?: Double.NaN
            val respDbl = todayData?.optDouble("respiratoryRate", Double.NaN) ?: Double.NaN
            val distMetersDbl = todayData?.optDouble("distanceMeters", Double.NaN) ?: Double.NaN
            val bodyFatDbl = todayData?.optDouble("bodyFatPercent", Double.NaN) ?: Double.NaN
            val energyDbl = todayData?.optDouble("energyScore", Double.NaN) ?: Double.NaN

            val bpLabel = if (sysDbl.isFinite() && sysDbl > 0 && diaDbl.isFinite() && diaDbl > 0) {
                "${sysDbl.roundToInt()}/${diaDbl.roundToInt()} Hgmm"
            } else "—"
            val gluLabel = if (gluDbl.isFinite() && gluDbl > 0) {
                String.format(java.util.Locale.US, "%.1f mmol/l", gluDbl).replace('.', ',')
            } else "—"
            val respLabel = if (respDbl.isFinite() && respDbl > 0) {
                "${respDbl.roundToInt()} /perc"
            } else "—"
            val distKmToday = if (distMetersDbl.isFinite() && distMetersDbl > 0) (distMetersDbl / 1000.0).toFloat() else 0f

            // Build 7-day breakdown & source attribution
            val currentWorkouts = _uiState.value.loggedWorkouts
            val dayFmt = DateTimeFormatter.ofPattern("MM. dd. (EEE)", com.example.localization.StudioLocale.forLanguage(_uiState.value.language))
            val weeklyDaysList = mutableListOf<WeeklyHealthDaySummary>()
            var sumSteps = 0
            var sumKcal = 0
            var sumDistMeters = 0.0
            var sumSleepMin = 0.0
            var sleepDaysCount = 0
            val discoveredSources = linkedSetOf<String>()

            for (offset in 13 downTo 0) {
                val d = today.minusDays(offset.toLong())
                val dayObj = runCatching {
                    admitted(com.repforge.app.NativeHealthProviderData.day(store, provider, d), provider)
                }.getOrNull()
                // Never project another provider's cached row as the selected source.
                val ownDay = dayObj?.takeIf { raw ->
                    raw.optString("provider").let { it.isBlank() || it == provider } &&
                        raw.optString("channel").let { it.isBlank() || it == provider }
                }?.let { JSONObject(it.toString()).put("day", d.toString()) }
                if (ownDay != null) projectedProviderDays.add(ownDay)
                val ownSteps = measuredSteps(dayObj, provider)
                val alternateStepDay = if (ownSteps != null || !alternativeEnabled) null
                    else if (d == today) alternateToday
                    else admitted(com.repforge.app.NativeHealthProviderData.day(store, alternativeProvider, d),
                        alternativeProvider)
                val stepDay = if (ownSteps != null) dayObj else alternateStepDay
                val measuredStepCount = ownSteps ?: measuredSteps(alternateStepDay, alternativeProvider)
                val stepsAvailable = measuredStepCount != null
                val dSteps = measuredStepCount?.toInt() ?: 0
                val dKcal = dayObj?.optDouble("activeCalories", Double.NaN)?.takeIf { it.isFinite() && it >= 0 }?.toInt() ?: 0
                // 2788: measured total energy of the day (resting + active); null when not measured (never 0).
                val dTotalKcal = dayObj?.optDouble("totalCalories", Double.NaN)?.takeIf { it.isFinite() && it > 0 }?.toInt()
                val dActiveKcal = dayObj?.optDouble("activeCalories", Double.NaN)?.takeIf { it.isFinite() && it >= 0 }?.toInt()
                val dDistM = dayObj?.optDouble("distanceMeters", Double.NaN)?.takeIf { it.isFinite() && it >= 0 } ?: 0.0
                val dSleepMin = com.repforge.app.NativeDailyHealthDetails.sleepMinutes(
                    dayObj, d, java.time.ZoneId.systemDefault(), Instant.now()) ?: Double.NaN
                val dSleepLabel = if (dSleepMin.isFinite()) {
                    com.example.localization.StudioLocale.durationMinutes(dSleepMin.toInt(), _uiState.value.language)
                } else "—"

                val workoutsForDay = currentWorkouts.filter { w ->
                    w.localWorkoutDate() == d
                }
                val workoutMinForDay = workoutsForDay.sumOf { it.durationMinutes.coerceAtLeast(0) }
                val workoutCountForDay = workoutsForDay.size

                val hasHealthRow = stepsAvailable || dayObj != null && (dKcal > 0 || dSleepMin.isFinite() ||
                    dayObj.optDouble("averageHeartRate", 0.0) > 0.0 || dayObj.optDouble("restingHeartRate", 0.0) > 0.0)
                val stepOriginPkg = stepDay?.optString("stepsOrigin", "")?.ifBlank { stepDay.optString("activityOrigin", "") }.orEmpty()
                val dayChannel = dayObj?.optString("channel", provider).orEmpty()
                val stepProvider = if (ownSteps != null) provider else alternativeProvider
                val healthSourceLabel = if (stepsAvailable) formatProviderLabelHu(stepProvider, stepOriginPkg)
                    else formatProviderLabelHu(dayChannel, dayObj?.optString("activityOrigin").orEmpty())
                val sourceLabelHu = when {
                    hasHealthRow && workoutCountForDay > 0 -> "${healthSourceLabel} + ${studioModesText("Edzésnapló", "Workout journal", "Trainingstagebuch", "Jurnal de antrenament")}"
                    hasHealthRow -> healthSourceLabel
                    workoutCountForDay > 0 -> studioModesText("Edzésnapló (aktív időszakok)", "Workout journal (active intervals)", "Trainingstagebuch (aktive Zeiträume)", "Jurnal de antrenament (intervale active)")
                    else -> studioModesText("Nincs rögzített adat", "No recorded data", "Keine aufgezeichneten Daten", "Nu există date înregistrate")
                }
                if (offset <= 6) {
                if (hasHealthRow) discoveredSources.add(healthSourceLabel)
                if (workoutCountForDay > 0) discoveredSources.add(studioModesText("Edzésnapló (aktív idő)", "Workout journal (active time)", "Trainingstagebuch (aktive Zeit)", "Jurnal de antrenament (timp activ)"))

                sumSteps += dSteps
                sumKcal += dKcal
                sumDistMeters += dDistM
                if (dSleepMin.isFinite() && dSleepMin > 0) {
                    sumSleepMin += dSleepMin
                    sleepDaysCount++
                }

                }

                weeklyDaysList.add(
                    WeeklyHealthDaySummary(
                        dateIso = d.toString(),
                        dayLabelHu = if (d == today) "Ma (${d.format(dayFmt)})" else d.format(dayFmt),
                        steps = dSteps,
                        activeCaloriesKcal = dKcal,
                        activeCaloriesMeasured = dActiveKcal,
                        totalCaloriesKcal = dTotalKcal,
                        sleepLabel = dSleepLabel,
                        workoutMinutes = workoutMinForDay,
                        workoutCount = workoutCountForDay,
                        stepsAvailable = stepsAvailable,
                        stepsSourceLabelHu = if (stepsAvailable) formatProviderLabelHu(stepProvider, stepOriginPkg) else "Nincs napi lépésadat",
                        stepProvider = stepProvider.takeIf { stepsAvailable },
                        sourceLabelHu = sourceLabelHu,
                        stepOriginPackage = stepOriginPkg,
                        pulse = if (provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG)
                            HealthReadingsProjection.pulseDay(provider, d, ownDay, now, zone) else null
                    )
                )
            }

            val avgSleepHu = if (sleepDaysCount > 0) {
                val avgMin = (sumSleepMin / sleepDaysCount).roundToInt()
                com.example.localization.StudioLocale.durationMinutes(avgMin, _uiState.value.language)
            } else "—"

            val sourcesSummaryHu = if (discoveredSources.isNotEmpty()) {
                discoveredSources.joinToString(" • ")
            } else {
                "Health Connect (nincs szinkronizált mérés az elmúlt 7 napban)"
            }

            // Use the same selected-provider, day-validated projections for Home and Health.
            val todayPulse = if (provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG)
                HealthReadingsProjection.pulseDay(
                    provider, today, projectedProviderDays.firstOrNull { it.optString("day") == today.toString() },
                    now, zone) else null
            val roomCoachHistory = runCatching {
                nativeDatabase?.workoutHistoryDao()?.getCoachRecentHistory(
                    now.minus(java.time.Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli())
                    ?.mapNotNull { raw -> runCatching { JSONObject(raw) }.getOrNull() }
                    ?: emptyList()
            }.getOrDefault(emptyList())
            val todayRecovery = if (provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG) {
                fun recoveryDays(channel: String, enabled: Boolean): List<JSONObject> =
                    if (enabled) com.repforge.app.NativeHealthProviderData.recentRecoveryDays(store, channel, today)
                        .mapNotNull { admitted(it, channel) } else emptyList()
                HealthReadingsProjection.recoveryToday(provider, recoveryDays(provider, primaryEnabled),
                    roomCoachHistory, now, zone, alternateDays = recoveryDays(alternativeProvider, alternativeEnabled))
            } else null

            withContext(Dispatchers.Main) {
                if (store.generation() != generation ||
                    store.preferences().optString("hcDailySource", "auto") != hcDailySourceRaw ||
                    primaryEnabled != privacy.enabled(ctx, privacy.HEALTH_CONNECT) ||
                    alternativeEnabled != privacy.enabled(ctx, privacy.SAMSUNG_HEALTH)) return@withContext
                loadedHealthProvider = provider
                _uiState.update { st ->
                    st.copy(
                        primaryHealthSourceSamsung = false,
                        dailyHealthDate = if (todayData != null || todaysSteps != null) today.toString() else "",
                        dailyHealthAvailableFields = availableFields,
                        dailyHealthSourceLabel = formatProviderLabelHu(provider, todayData?.optString("activityOrigin").orEmpty()),
                        dailyHealthLastSyncIso = lastProviderSync,
                        totalCaloriesKcal = todayData?.optDouble("totalCalories", Double.NaN)?.takeIf { it.isFinite() && it >= 0 }?.toInt() ?: 0,
                        dailySteps = if (stepsDbl.isFinite() && stepsDbl >= 0) stepsDbl.toInt() else 0,
                        currentHrBpm = todayPulse?.averageBpm?.roundToInt() ?: 0,
                        restingHrBpm = todayPulse?.restingBpm?.roundToInt() ?: 0,
                        maxHrBpm = if (maxHrDbl.isFinite() && maxHrDbl > 0) maxHrDbl.toInt() else 0,
                        minHrBpm = if (minHrDbl.isFinite() && minHrDbl > 0) minHrDbl.toInt() else 0,
                        dailyPulseHistory = todayPulseHistory,
                        dailyPulseSampleCount = todayData?.optInt("heartRateSamples", 0)?.coerceAtLeast(0) ?: 0,
                        activeCaloriesKcal = if (caloriesDbl.isFinite() && caloriesDbl > 0) caloriesDbl.toInt() else 0,
                        sleepDurationLabel = if (sleepMinDbl.isFinite() && sleepMinDbl > 0) com.example.localization.StudioLocale.durationMinutes(sleepMinDbl.toInt(), _uiState.value.language) else "—",
                        sleepHoursDecimal = if (sleepMinDbl.isFinite() && sleepMinDbl > 0) "${((sleepMinDbl / 60.0) * 10.0).roundToInt() / 10.0} h" else "—",
                        sleepHasStages = hasSleepStages,
                        sleepStagesSummaryHu = sleepStagesSummary,
                        samsungEnergyScore = todayRecovery?.samsungEnergy?.roundToInt() ?: 0,
                        spo2Percent = todayData?.optDouble("oxygenSaturationPercent", Double.NaN)?.takeIf { it.isFinite() && it > 0 }?.toFloat() ?: 0f,
                        vo2Max = todayData?.optDouble("vo2Max", Double.NaN)?.takeIf { it.isFinite() && it > 0 }?.toFloat() ?: 0f,
                        bodyFatPercent = if (bodyFatDbl.isFinite() && bodyFatDbl > 0) bodyFatDbl.toFloat() else 0f, // Never retain another provider's previous value.
                        hrvRmssdMs = if (hrvDbl.isFinite() && hrvDbl > 0) hrvDbl.toFloat() else 0f,
                        bloodPressureLabel = bpLabel,
                        bloodGlucoseLabel = gluLabel,
                        respiratoryRateLabel = respLabel,
                        dailyDistanceKm = distKmToday,
                        coachReadinessScore = todayRecovery?.coachScore?.roundToInt() ?: 0,
                        trainPilotReadinessScore = todayRecovery?.trainPilotEstimate ?: 0,
                        todayPulseReading = todayPulse,
                        todayRecoveryReading = todayRecovery,
                        weeklyHealthDays = weeklyDaysList.takeLast(7),
                        dailyActivityDays = weeklyDaysList,
                        weeklyTotalSteps = sumSteps,
                        weeklyTotalCaloriesKcal = sumKcal,
                        weeklyTotalDistanceKm = (sumDistMeters / 1000.0).toFloat(),
                        weeklyAvgSleepLabel = avgSleepHu,
                        weeklyDataSourcesSummary = sourcesSummaryHu,
                        dailySourceModeLabel = dailySourceModeHu,
                        statusBannerMessage = st.statusBannerMessage
                    )
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    loadedHealthProvider = null
                    clearedHealth(it).copy(statusBannerMessage = "Hiba az egészségszinkron során: ${e.localizedMessage ?: "szolgáltatói hiba"}")
                }
            }
        }
    }
}
