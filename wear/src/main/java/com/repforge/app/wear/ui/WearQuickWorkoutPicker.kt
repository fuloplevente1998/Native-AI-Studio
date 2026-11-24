package com.repforge.app.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text

@Composable
internal fun QuickWorkoutPicker(
    home: WatchHomeSnapshot?,
    active: WearWorkout?,
    canStart: Boolean,
    onBack: () -> Unit,
    onSelect: (WatchHomeExercise) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val adding = active?.quickWorkout == true
    val catalog = home?.quickExercises.orEmpty()
    val matches =
        remember(catalog, query, active?.exercises) {
            val needle = query.trim()
            catalog.filter { item ->
                (needle.isBlank() ||
                    item.name.contains(needle, ignoreCase = true) ||
                    item.id.contains(needle, ignoreCase = true)) &&
                    (!adding || active?.exercises?.none { it.id == item.id } == true)
            }
        }
    val pageCount = ((matches.size + 11) / 12).coerceAtLeast(1)
    val selectedPage = page.coerceIn(0, pageCount - 1)
    RoundList(if (adding) "+ Gyakorlat" else wearText("Gyors edzés")) {
        if (active != null && !adding) {
            centerItem {
                Text(
                    wearText("Előbb zárd le a jelenlegi edzést."),
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (!adding && !canStart) {
            centerItem {
                Text(
                    wearText("A telefonos edzésvázlat vagy a függő szinkron lezárása után indítható."),
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (catalog.isEmpty()) {
            centerItem {
                Text(
                    wearText("A gyakorlatok szinkronizálásához nyisd meg a TrainPilotot a telefonon."),
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            centerItem {
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it.take(60)
                        page = 0
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 13.sp),
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(Secondary, RoundedCornerShape(14.dp))
                            .padding(10.dp),
                    decorationBox = { content ->
                        Box {
                            if (query.isEmpty())
                                Text(wearText("Gyakorlat keresése…"), color = Muted, fontSize = 13.sp)
                            content()
                        }
                    },
                )
            }

            if (matches.isEmpty())
                centerItem {
                    Text(
                        wearText("Nincs további választható gyakorlat."),
                        color = Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            matches.drop(selectedPage * 12).take(12).forEach { exercise ->
                centerItem {
                    Pill(exercise.name, tone = Card, height = 48, size = 12) { onSelect(exercise) }
                }
            }
            if (pageCount > 1) {
                centerItem { Label("${selectedPage + 1} / $pageCount") }
                if (selectedPage > 0)
                    centerItem { Pill(wearText("‹ Előző"), tone = Secondary) { page = selectedPage - 1 } }
                if (selectedPage + 1 < pageCount)
                    centerItem {
                        Pill(wearText("További gyakorlatok ›"), tone = Secondary) { page = selectedPage + 1 }
                    }
            }
        }

        centerItem { Pill("‹ Vissza", tone = Secondary, onClick = onBack) }
    }
}
