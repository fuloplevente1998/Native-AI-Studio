package com.repforge.app.wear

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun MiniJournalScreen(
    home: WatchHomeSnapshot?,
    onRefresh: () -> Unit,
    onOpenPhone: () -> Unit,
    backLabel: String,
    onBack: () -> Unit,
    onSelect: (MiniJournalWorkout) -> Unit,
) {
    val items = home?.recentWorkouts.orEmpty()
    RoundList(wearText("Mini napló")) {
        centerItem { JournalText(wearText("Mentett telefonos edzések · offline másolat"), size = 10) }

        if (items.isEmpty()) {
            val message =
                when {
                    home == null -> wearText("Várakozás a telefonos napló szinkronizálására.")
                    !home.journalAvailable ->
                        wearText("A telefon legutóbbi adatcsomagja még nem tartalmaz Mini naplót. Frissítsd és nyisd meg a telefonos TrainPilotot.")
                    else -> wearText("A telefonos naplóban még nincs átadható, befejezett edzés.")
                }
            centerItem {
                JournalText(message, size = 12)
            }
        }
        if (home?.phoneVersion?.isNotBlank() == true) {
            centerItem { JournalText("Telefon: ${home.phoneVersion}", size = 10) }
        }
        items.forEach { item ->
            centerItem {
                Pill(
                    "${miniJournalDate(item.started)}\n${item.dayName}",
                    modifier = Modifier.fillMaxWidth(.84f).testTag("wear-journal-entry"),
                    tone = Card,
                    size = 12,
                    height = 47,
                    lines = Int.MAX_VALUE,
                ) {
                    onSelect(item)
                }
            }
        }

        centerItem { Pill(wearText("Frissítés"), tone = Secondary, onClick = onRefresh) }

        centerItem { Pill(wearText("Telefon megnyitása"), tone = Secondary, onClick = onOpenPhone) }

        centerItem { Pill(backLabel, tone = Secondary, onClick = onBack) }
    }
}

internal fun LazyListScope.MiniJournalMetrics(
    title: String,
    metrics: MiniJournalMetric,
    watch: Boolean,
) {
    centerItem { JournalText(title, Gold, 12) }
    val avg = miniNumber(metrics.averageHeartRate, 1)
    val max = miniNumber(metrics.maxHeartRate)
    if (avg != null || max != null) {
        centerItem {
            JournalText(
                "Pulzus: ${avg ?: "—"} / ${max ?: "—"} bpm",
                color = White,
                size = 12,
            )
        }
    }
    if (watch) {
        miniNumber(metrics.totalCalories)?.let { value ->
            centerItem {
                JournalText(
                    "Összes mért energia: $value kcal",
                    color = White,
                    size = 12,
                )
            }
        }
        miniNumber(metrics.activeDurationSeconds)?.let { value ->
            centerItem { JournalText("Órás mérési idő: $value s", size = 11) }
        }
    } else {
        miniNumber(metrics.workoutCalories)?.let { value ->
            centerItem {
                JournalText(
                    "Edzéskalória: $value kcal",
                    color = White,
                    size = 12,
                )
            }
        }
        miniNumber(metrics.totalCalories)?.let { value ->
            centerItem { JournalText("Ablak összenergiája: $value kcal", size = 11) }
        }
        miniNumber(metrics.activeCalories)?.let { value ->
            centerItem { JournalText("Aktív energia: $value kcal", size = 11) }
        }
        miniNumber(metrics.exerciseMinutes)?.let { value ->
            centerItem { JournalText("Egészségapp edzésideje: $value perc", size = 11) }
        }
    }
    miniNumber(metrics.steps)?.let { centerItem { JournalText("Lépések: $it", size = 11) } }
    if (metrics.partial) centerItem { JournalText(wearText("Részleges órás mérés"), Gold, 10) }
}

@Composable
internal fun MiniJournalDetailScreen(
    item: MiniJournalWorkout?,
    backLabel: String,
    onBack: () -> Unit,
) {
    RoundList(wearText("Edzésnapló")) {
        if (item == null) {
            centerItem {
                JournalText(
                    wearText("Ez az edzés már nincs a szinkronizált listában."),
                    size = 12,
                )
            }
        } else {
            centerItem { JournalText(item.dayName, White, 16, FontWeight.SemiBold) }
            centerItem { JournalText(miniJournalDate(item.started), size = 11) }
            centerItem {
                JournalText(
                    "${item.durationSeconds / 60} perc · ${item.exercises.size} gyakorlat",
                    size = 11,
                )
            }

            if (item.wear != null) {
                MiniJournalMetrics(wearText("TrainPilot óra · Health Services"), item.wear, true)
            } else centerItem { JournalText(wearText("Nem érkezett órás mérési összesítés"), size = 11) }
            if (item.healthConnect != null) {
                MiniJournalMetrics(
                    if (item.healthConnect.source == "samsung_health") "Samsung Health"
                    else "Health Connect",
                    item.healthConnect,
                    false,
                )

                centerItem { JournalText(wearText("A két mérés nem adódik össze"), size = 10) }
            }

            centerItem { JournalText(wearText("Elvégzett gyakorlatok"), Gold, 12) }
            if (item.exercises.isEmpty()) {
                centerItem { JournalText(wearText("Nincs rögzített sorozat"), size = 11) }
            }
            item.exercises.forEach { exercise ->
                centerItem {
                    JournalText(
                        exercise.name,
                        color = White,
                        size = 13,
                        weight = FontWeight.SemiBold,
                    )
                }
                exercise.sets.forEach { set ->
                    val details = buildList {
                        if (set.reps.isNotBlank()) add("${set.reps} ${wearText("ism.")}")
                        if (set.weight != null && set.weight > 0.0)
                            add("${miniNumber(set.weight, 1)} kg")
                        if (
                            set.leftSeconds != null &&
                                set.rightSeconds != null &&
                                set.leftSeconds > 0.0 &&
                                set.rightSeconds > 0.0
                        )
                            add(
                                "${miniNumber(set.leftSeconds)} / ${miniNumber(set.rightSeconds)} s"
                            )
                        if (set.distanceMeters != null && set.distanceMeters > 0.0)
                            add("${miniNumber(set.distanceMeters)} m")
                    }
                    centerItem {
                        JournalText(
                            wearFormat("%d. sorozat • %s", set.number, details.joinToString(" · ").ifBlank { wearText("Kész") }),
                            size = 11,
                        )
                    }
                }
            }
        }

        centerItem { Pill(backLabel, tone = Secondary, onClick = onBack) }
    }
}
