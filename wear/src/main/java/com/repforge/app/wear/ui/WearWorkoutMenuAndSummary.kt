package com.repforge.app.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import org.json.JSONObject

@Composable
internal fun WorkoutMenu(
    workout: WearWorkout,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (Int) -> Unit,
    onEdit: (String) -> Unit,
    onTimerTools: () -> Unit,
    restRemaining: Int,
    onFinish: () -> Unit,
    onHome: () -> Unit,
    onDiscard: () -> Unit = {},
    onBack: () -> Unit = onHome,
    onHealth: (() -> Unit)? = null,
    onPauseWorkout: (() -> Unit)? = null,
    onPhoneControl: (() -> Unit)? = null,
) {
    val automaticToggle=LocalAutomaticToggle.current
    val automaticCommand=LocalAutomaticCommand.current
    val status=LocalSaveSync.current
    val undo=LocalUndo.current
    val touchLock=LocalTouchLock189.current
    RoundList(wearText("Edzésmenü")) {
        centerItem { Pill(WearMenu216.text("back"), tone = Secondary, onClick = onBack) }
        if (onHealth != null) centerItem { Pill(WearMenu216.text("metrics"), tone = Card, onClick = onHealth) }
        if (onPauseWorkout != null) centerItem { Pill(wearV2Text("pauseWorkout"), onClick = onPauseWorkout) }
        else if (onPhoneControl != null) centerItem { Pill(wearV2Text("phoneControl"), tone = Secondary, onClick = onPhoneControl) }
        centerItem { Pill(WearMenu216.text("discard"), tone = Danger, enabled = !WearUndo188.blocked(workout) && !WearOwnership193.pending(workout), onClick = onDiscard) }
        touchLock?.let { lock -> centerItem { Pill(WearTouchCopy189.language(java.util.Locale.getDefault().language).lock,tone=Secondary,onClick=lock) } }
        if(undo!=null&&WearUndo188.available(workout))centerItem {
            Pill(wearText("Utolsó sorozat visszavonása"),tone=Gold) { WearUndo188.record(workout)?.optString("recordId")?.let(undo) }
        }
        if(WearUndo188.marker(workout)?.optString("status")=="conflict")centerItem { Label(wearText("Eltérő módosítás. Ellenőrizd a telefonon.")) }
        if(status!=null)saveSyncItem()
        centerItem { Pill(wearText("‹ Kezdőlap"), tone = Secondary, onClick = onHome) }
        if(automaticToggle!=null) centerItem {
            Text(wearFormat("Automata mód: %s", wearText(if(workout.automation?.enabled==true) "Be" else "Ki")),color=Gold,fontSize=11.sp,
                modifier=Modifier.fillMaxWidth().heightIn(min=30.dp).clickable(role=Role.Button,onClick=automaticToggle)
                    .padding(6.dp).semantics { contentDescription=wearText("Automata mód kapcsoló") },textAlign=TextAlign.Center)
        }
        if(workout.automation?.enabled==true&&automaticCommand!=null) {
            for((name,seconds,field) in listOf(Triple(wearText("Munka"),workout.automation.workSeconds,"work"),Triple(wearText("Pihenő"),workout.automation.restSeconds,"rest"),Triple(wearText("Kör"),workout.automation.rounds,"rounds"))) centerItem {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("−",color=White,modifier=Modifier.size(30.dp).clickable(enabled=!workout.automation.counting,role=Role.Button){automaticCommand(field+"Minus")}.semantics { contentDescription=wearFormat("%s csökkentése", name) })
                    Text(if(field == "rounds") "$name $seconds" else "$name $seconds ${wearText("mp")}",color=Muted,fontSize=10.sp)
                    Text("+",color=White,modifier=Modifier.size(30.dp).clickable(enabled=!workout.automation.counting,role=Role.Button){automaticCommand(field+"Plus")}.semantics { contentDescription=wearFormat("%s növelése", name) })
                }
            }
            centerItem { Text(wearFormat("Rezgés: %s", wearText(if(workout.automation.vibration) "Be" else "Ki")),color=Muted,fontSize=10.sp,
                modifier=Modifier.fillMaxWidth().heightIn(min=30.dp).clickable(role=Role.Button){automaticCommand("vibration")}.padding(6.dp)) }
        }
        if (workout.exercise?.loadType != "bodyweight") {
            centerItem {
                Pill(
                    wearFormat("Súly: %s kg", workout.currentSet?.weight?.ifBlank { "0" } ?: "0"),
                    tone = Secondary,
                    enabled = !workout.exerciseComplete,
                ) {
                    onEdit("weight")
                }
            }
        }
        if (normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp")&&!(workout.automation?.enabled==true&&WearAutomaticEngine.timed(workout.exercise))) {
            centerItem {
                Pill(
                    wearText("Stopper és oldalak"),
                    tone = Secondary,
                    enabled = restRemaining == 0 && !workout.exerciseComplete,
                    onClick = onTimerTools,
                )
            }
        }
        centerItem {
            Pill(
                wearText("Edzés befejezése"),
                tone = Danger,
                enabled = workout.completedSets > 0 && !WearUndo188.blocked(workout),
                onClick = onFinish,
            )
        }
        if (workout.completedSets == 0) centerItem { Label(wearText("Előbb rögzíts egy sorozatot")) }
        centerItem { Label("GYAKORLATOK", Gold) }
        workout.exercises.forEachIndexed { index, exercise ->
            centerItem {
                Column(
                    Modifier.fillMaxWidth()
                        .background(
                            if (index == workout.currentExercise) Secondary else Card,
                            RoundedCornerShape(20.dp),
                        )
                        .clickable(role = Role.Button) { onSelect(index) }
                        .padding(12.dp)
                ) {
                    Text(exercise.name, color = White, fontSize = 13.sp, maxLines = 2)
                    Text(
                        wearFormat("%d/%d sorozat", exercise.sets.count { it.done }, exercise.sets.size),
                        color = if (index == workout.currentExercise) Gold else Muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        centerItem {
            Pill(
                wearText("‹ Előző"),
                tone = Secondary,
                enabled = workout.currentExercise > 0,
                onClick = onPrevious,
            )
        }

        centerItem {
            Pill(
                wearText("Következő ›"),
                tone = Secondary,
                enabled = workout.currentExercise < workout.exercises.lastIndex,
                onClick = onNext,
            )
        }
    }
}

@Composable
internal fun ConfirmationScreen(discard: Boolean, completed: Int, onAnswer: (Boolean) -> Unit) {
    ActionPanel(
        if (discard) wearText("Félbehagyott edzés") else wearText("Edzés befejezése"),
        count = 2,
        actions = { layout ->
            CompositionLocalProvider(LocalActionSize provides layout.buttonSize, LocalCaptionBelow provides true) {
                Row(horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
                    RoundControl(
                        "×",
                        caption = wearText("Mégse"),
                        description = wearText("Vissza, az edzés megőrzése"),
                    ) {
                        onAnswer(false)
                    }
                    RoundControl(
                        if (discard) "✕" else "✓",
                        if (discard) Danger else SaveTone,
                        caption = if (discard) wearText("Törlés") else wearText("Befejezés"),
                        description =
                            if (discard) wearText("Edzés törlése") else wearText("Edzés befejezése és mentése"),
                    ) {
                        onAnswer(true)
                    }
                }
            }
        },
    ) { compact ->
        run {
            Title(
                if (discard) wearText("Törlöd az edzést?") else wearText("Befejezed az edzést?"),
                if (compact) 14 else 17,
            )
        }

        run {
            Text(
                if (discard) wearText("A félbehagyott edzés elvész.\nA napló megmarad.")
                else wearFormat("%d sorozat bekerül a naplóba.", completed),
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun WorkoutFeedbackScreen(onAnswer: (String?) -> Unit) {
    RoundList(wearText("Milyen volt az edzés?")) {
        WearWorkoutFeedback.choices.forEachIndexed { index, (value, label) ->
            centerItem { Pill("${index + 1} · ${wearText(label)}", tone = Card) { onAnswer(value) } }
        }
        centerItem { Pill(wearText("Fájdalom / kellemetlenség"), tone = Danger) { onAnswer("pain") } }
        centerItem { Pill(wearText("Most kihagyom"), tone = Secondary) { onAnswer(null) } }
    }
}

@Composable
internal fun SummaryScreen(
    result: WearClosure,
    health: JSONObject?,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    val saved = result.status == if (result.action == "discardWorkout") "discarded" else "saved"
    ActionPanel(
        if (result.status == "error") wearText("Szinkronizálás")
        else if (result.action == "discardWorkout") (if (saved) wearText("Edzés törölve") else wearText("Várakozás a telefonra"))
        else wearText("Edzés kész"), // 2777: the result is ready on the watch; the save state is the line below
        count = if (result.status == "error") 2 else 1,
        actions = { layout ->
            CompositionLocalProvider(LocalActionSize provides layout.buttonSize, LocalCaptionBelow provides true) {
                Row(horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
                    RoundControl(
                        if (saved) "✓" else "‹",
                        if (saved) Gold else Secondary,
                        caption = if (saved) wearText("Kész") else wearText("Kezdőlap"),
                        onClick = onDone,
                    )
                    if (result.status == "error")
                        RoundControl(
                            "↻",
                            Gold,
                            caption = wearText("Újrapróba"),
                            description = wearText("Mentés újrapróbálása"),
                            onClick = onRetry,
                        )
                }
            }
        },
    ) { compact ->
        if (result.status == "error") run { Title(wearText("Még nem menthető"), if (compact) 14 else 19) }
        if (result.status == "error") {
            run {
                Text(result.message, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else if (result.action == "finishWorkout") {
            result.overview?.let { Title(it.name, if (compact) 12 else 16) }
            run {
                // 2777: smaller numbers so the save state line below always fits above the button.
                Text(
                    formatSeconds(result.durationSeconds),
                    color = White,
                    fontSize = if (compact) 24.sp else 28.sp,
                    lineHeight = if (compact) 26.sp else 30.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            run {
                Row(horizontalArrangement = Arrangement.spacedBy(19.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Title("${result.exerciseCount}", 17)
                        Label(wearText("gyakorlat"))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Title("${result.completedSets}", 17)
                        Label(wearText("sorozat"))
                    }
                }
            }
            result.overview?.let { overview ->
                Label(wearSummaryText("volume") + ": " + wearSummaryVolume(overview.volumeKg), size = 10)
            }
            run {
                Label(
                    if (saved) wearText("Mentve a telefonos naplóba")
                    else "⏳ " + wearText("Telefonra vár"),
                    lines = 2,
                )
            }
        } else
            run {
                Label(
                    if (saved) wearText("A félbehagyott edzés törölve")
                    else wearText("A telefonos törlés még várakozik"),
                    lines = 3,
                )
            }
        if (result.action == "finishWorkout") {
            result.overview?.exercises?.forEach { exercise ->
                Label(exercise.name, size = 11, lines = 2)
                Label("${exercise.sets} ${wearSummaryText("sets")} · ${wearSummaryVolume(exercise.volumeKg)}", size = 10)
            }
            val metrics = wearMeasuredTotals(health, result.workoutId)
            if (metrics.isNotEmpty()) {
                run { Label(wearText("ÓRÁN MÉRT ADATOK"), Gold, size = 10) }
                for (pair in metrics.chunked(2)) {

                    run {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            for (metric in pair) Column(
                                Modifier.weight(1f)
                                    .background(Card, RoundedCornerShape(12.dp))
                                    .padding(vertical = 7.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Label(wearText(metric.label), size = 9)
                                Title(metric.value, 14)
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                run {
                    Text(
                        wearText("Az összes kcal az alapanyagcserét is tartalmazza."),
                        color = Muted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (health?.optString("workoutId") == result.workoutId) {
                if (health.optString("state") in listOf("starting", "active", "ending")) {
                    run {
                        Text(
                            wearText("A mérés lezárása folyamatban; az utolsó adatok még frissülhetnek."),
                            color = Gold,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (health.optBoolean("partial"))
                    run {
                        Text(
                            wearText("Részleges mérés"),
                            color = Gold,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
            }
        }
    }
}
