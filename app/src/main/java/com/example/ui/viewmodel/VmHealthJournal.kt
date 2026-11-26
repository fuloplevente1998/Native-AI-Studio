package com.example.ui.viewmodel

import com.repforge.app.ui.health.HealthDaySources
import java.time.ZoneId
import com.example.model.localWorkoutDate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.repforge.app.HealthJournalStore
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.repforge.app.healthreadings.HealthReadingsProjection
import com.repforge.app.healthreadings.ReadingState
import java.time.Instant
import java.time.format.DateTimeFormatter
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
            // The HC journal preserves older observations, including across temporary read
            // failures. Only the currently granted, successfully read fields may be shown.
            val privacy = com.repforge.app.NativePrivacyControls
            val primaryEnabled = privacy.enabled(ctx, privacy.HEALTH_CONNECT)
            val hcGranted = if (primaryEnabled) com.repforge.app.HealthConnectRepository.READ_PERMS
                .filter { com.repforge.app.HealthConnectRepository.isAllowed(ctx, it) }.toSet() else emptySet()
            fun primaryDay(day: java.time.LocalDate): JSONObject? = if (primaryEnabled)
                com.repforge.app.HealthConnectVisibleDay.project(
                    com.repforge.app.NativeHealthProviderData.day(store, provider, day), hcGranted
                ) else null
            val alternativeProvider = HealthJournalStore.SAMSUNG
            val alternativeEnabled = privacy.enabled(ctx, privacy.SAMSUNG_HEALTH)
            fun supplementDay(day: java.time.LocalDate): JSONObject? =
                if (alternativeEnabled) com.repforge.app.NativeHealthProviderData.day(store, alternativeProvider, day) else null
            val todayData = primaryDay(today)
            val alternateToday = supplementDay(today)
            val preferredActivityOrigin = com.repforge.app.HealthDailySource.choose("", hcDailySourceRaw, emptySet())
            fun stepOriginMatches(row: JSONObject, fromProvider: String): Boolean =
                fromProvider != provider || row.optString("activityOrigin") == preferredActivityOrigin

            val todaySources = HealthDaySources(today, todayData, alternateToday, now, zone)
            val todaysSteps = todaySources.metric("steps", ::stepOriginMatches)
            val todaySleep = todaySources.sleep()
            val sleepData = todaySleep?.let { todaySources.row(it.provider) }
            val todaySleepMin = todaySleep?.number ?: Double.NaN
            val todayPulse = todaySources.pulse()
            val todayPulseHistory = todayPulse.displayedSamples.map {
                com.repforge.app.NativeDailyHealthDetails.PulsePoint(it.at.toString(), it.bpm, it.origin)
            }
            val activeCalories = todaySources.metric("activeCalories")
            val totalCalories = todaySources.metric("totalCalories")
            val hasSleepStages = runCatching {
                val sessions = sleepData?.optJSONArray("sleepSessions")
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
            val availableFields = buildSet {
                if (activeCalories != null) add("activeCalories")
                if (totalCalories != null) add("totalCalories")
                if (todaysSteps != null) add("steps")
                if (todaySleep != null) add("sleepMinutes")
                if (todayPulse.averageBpm != null) add("averageHeartRate")
                if (todayPulse.restingBpm != null) add("restingHeartRate")
                if (todayPulse.observedMinBpm != null) add("minHeartRate")
                if (todayPulse.observedMaxBpm != null) add("maxHeartRate")
                if (todayPulse.totalSamples != null) add("heartRateSamples")
                if (todayPulseHistory.isNotEmpty()) add("heartRateSeries")
            }
            val lastProviderSync = store.nativeSyncLastSuccess(provider)
            val hrvDbl = todaySources.metric("hrvRmssdMs")?.number ?: 0.0
            val sysDbl = todayData?.optDouble("bloodPressureSystolic", Double.NaN) ?: Double.NaN
            val diaDbl = todayData?.optDouble("bloodPressureDiastolic", Double.NaN) ?: Double.NaN
            val gluDbl = todaySources.metric("bloodGlucoseMmolL")?.number ?: 0.0
            val respDbl = todaySources.metric("respiratoryRate")?.number ?: 0.0
            val distMetersDbl = todaySources.metric("distanceMeters")?.number ?: 0.0
            val bodyFatDbl = todaySources.metric("bodyFatPercent")?.number ?: 0.0

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
                val daySources = if (d == today) todaySources else HealthDaySources(d, primaryDay(d), supplementDay(d), now, zone)
                val step = daySources.metric("steps", ::stepOriginMatches)
                val active = daySources.metric("activeCalories")
                val total = daySources.metric("totalCalories")
                val distance = daySources.metric("distanceMeters")
                val sleep = daySources.sleep()
                val pulse = if (d == today) todayPulse else daySources.pulse()
                val dSteps = step?.number?.toInt() ?: 0
                val dActiveKcal = active?.number?.toInt()
                val dKcal = dActiveKcal ?: 0
                val dDistM = distance?.number ?: 0.0
                val dSleepMin = sleep?.number ?: Double.NaN
                val dSleepLabel = if (dSleepMin.isFinite()) {
                    com.example.localization.StudioLocale.durationMinutes(dSleepMin.toInt(), _uiState.value.language)
                } else "—"

                val workoutsForDay = currentWorkouts.filter { w ->
                    w.localWorkoutDate() == d
                }
                val workoutMinForDay = workoutsForDay.sumOf { it.durationMinutes.coerceAtLeast(0) }
                val workoutCountForDay = workoutsForDay.size

                val healthProviders = listOfNotNull(step, active, total, distance, sleep).map { it.provider }.toMutableSet()
                if (pulse.state == ReadingState.AVAILABLE) healthProviders.add(pulse.provider)
                val hasHealthRow = healthProviders.isNotEmpty()
                val stepDay = step?.let { daySources.row(it.provider) }
                val stepOriginPkg = stepDay?.optString("stepsOrigin", "")?.ifBlank { stepDay.optString("activityOrigin", "") }.orEmpty()
                val healthSourceLabel = healthProviders.joinToString(" + ") { formatProviderLabelHu(it, "") }
                val sourceLabelHu = when {
                    hasHealthRow && workoutCountForDay > 0 -> "${healthSourceLabel} + ${studioModesText("Edzésnapló", "Workout journal", "Trainingstagebuch", "Jurnal de antrenament")}"
                    hasHealthRow -> healthSourceLabel
                    workoutCountForDay > 0 -> studioModesText("Edzésnapló (aktív időszakok)", "Workout journal (active intervals)", "Trainingstagebuch (aktive Zeiträume)", "Jurnal de antrenament (intervale active)")
                    else -> studioModesText("Nincs rögzített adat", "No recorded data", "Keine aufgezeichneten Daten", "Nu există date înregistrate")
                }
                if (offset <= 6) {
                    healthProviders.mapTo(discoveredSources) { formatProviderLabelHu(it, "") }
                    if (workoutCountForDay > 0) discoveredSources.add(studioModesText("Edzésnapló (aktív idő)", "Workout journal (active time)", "Trainingstagebuch (aktive Zeit)", "Jurnal de antrenament (timp activ)"))
                    sumSteps += dSteps
                    sumKcal += dKcal
                    sumDistMeters += dDistM
                    if (sleep != null) {
                        sumSleepMin += sleep.number
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
                        totalCaloriesKcal = total?.number?.toInt(),
                        sleepLabel = dSleepLabel,
                        workoutMinutes = workoutMinForDay,
                        workoutCount = workoutCountForDay,
                        stepsAvailable = step != null,
                        stepsSourceLabelHu = step?.let { formatProviderLabelHu(it.provider, stepOriginPkg) } ?: "Nincs napi lépésadat",
                        stepProvider = step?.provider,
                        activeCaloriesProvider = active?.provider,
                        totalCaloriesProvider = total?.provider,
                        sleepProvider = sleep?.provider,
                        sourceLabelHu = sourceLabelHu,
                        stepOriginPackage = stepOriginPkg,
                        pulse = pulse
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

            val roomCoachHistory = runCatching {
                nativeDatabase?.workoutHistoryDao()?.getCoachRecentHistory(
                    now.minus(java.time.Duration.ofDays(14)).toEpochMilli(), now.toEpochMilli())
                    ?.mapNotNull { raw -> runCatching { JSONObject(raw) }.getOrNull() }
                    ?: emptyList()
            }.getOrDefault(emptyList())
            val primaryRecoveryDays = if (primaryEnabled) {
                com.repforge.app.NativeHealthProviderData.recentRecoveryDays(store, provider, today)
                    .mapNotNull { com.repforge.app.HealthConnectVisibleDay.project(it, hcGranted) }
            } else emptyList()
            val supplementRecoveryDays = if (alternativeEnabled) {
                com.repforge.app.NativeHealthProviderData.recentRecoveryDays(store, alternativeProvider, today)
            } else emptyList()
            val todayRecovery = HealthReadingsProjection.recoveryToday(provider, primaryRecoveryDays,
                roomCoachHistory, now, zone, alternateDays = supplementRecoveryDays)

            withContext(Dispatchers.Main) {
                if (store.generation() != generation ||
                    store.preferences().optString("hcDailySource", "auto") != hcDailySourceRaw ||
                    primaryEnabled != privacy.enabled(ctx, privacy.HEALTH_CONNECT) ||
                    alternativeEnabled != privacy.enabled(ctx, privacy.SAMSUNG_HEALTH)) return@withContext
                loadedHealthProvider = provider
                _uiState.update { st ->
                    st.copy(
                        primaryHealthSourceSamsung = false,
                        dailyHealthDate = if (todayData != null || availableFields.isNotEmpty()) today.toString() else "",
                        dailyHealthAvailableFields = availableFields,
                        dailyHealthSourceLabel = formatProviderLabelHu(provider, todayData?.optString("activityOrigin").orEmpty()),
                        dailyHealthLastSyncIso = lastProviderSync,
                        totalCaloriesKcal = totalCalories?.number?.toInt() ?: 0,
                        dailySteps = todaysSteps?.number?.toInt() ?: 0,
                        currentHrBpm = todayPulse.averageBpm?.roundToInt() ?: 0,
                        restingHrBpm = todayPulse.restingBpm?.roundToInt() ?: 0,
                        maxHrBpm = todayPulse.observedMaxBpm?.roundToInt() ?: 0,
                        minHrBpm = todayPulse.observedMinBpm?.roundToInt() ?: 0,
                        dailyPulseHistory = todayPulseHistory,
                        dailyPulseSampleCount = todayPulse.totalSamples?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0,
                        activeCaloriesKcal = activeCalories?.number?.toInt() ?: 0,
                        sleepDurationLabel = todaySleep?.let { com.example.localization.StudioLocale.durationMinutes(it.number.toInt(), st.language) } ?: "—",
                        sleepHoursDecimal = if (todaySleepMin.isFinite()) "${((todaySleepMin / 60.0) * 10.0).roundToInt() / 10.0} h" else "—",
                        sleepHasStages = hasSleepStages,
                        sleepStagesSummaryHu = sleepStagesSummary,
                        samsungEnergyScore = todayRecovery.samsungEnergy?.roundToInt() ?: 0,
                        spo2Percent = todaySources.metric("oxygenSaturationPercent")?.number?.toFloat() ?: 0f,
                        vo2Max = todaySources.metric("vo2Max")?.number?.toFloat() ?: 0f,
                        bodyFatPercent = if (bodyFatDbl.isFinite() && bodyFatDbl > 0) bodyFatDbl.toFloat() else 0f,
                        hrvRmssdMs = if (hrvDbl.isFinite() && hrvDbl > 0) hrvDbl.toFloat() else 0f,
                        bloodPressureLabel = bpLabel,
                        bloodGlucoseLabel = gluLabel,
                        respiratoryRateLabel = respLabel,
                        dailyDistanceKm = distKmToday,
                        coachReadinessScore = todayRecovery.coachScore?.roundToInt() ?: 0,
                        trainPilotReadinessScore = todayRecovery.trainPilotEstimate ?: 0,
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
