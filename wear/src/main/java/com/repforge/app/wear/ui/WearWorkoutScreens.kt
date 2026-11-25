package com.repforge.app.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.material3.Text
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun ProgramDays(
    home: WatchHomeSnapshot,
    onBack: () -> Unit,
    onSelect: (WatchHomeDay) -> Unit,
) {
    RoundList("Program napjai") {
        centerItem {
            Text(
                home.activeProgramName,
                color = Gold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }

        home.days.forEach { day ->
            centerItem {
                Row(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .background(Card, RoundedCornerShape(22.dp))
                        .clickable(role = Role.Button) { onSelect(day) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(36.dp).background(Gold.copy(alpha = .16f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            wearDayInitial(day.name),
                            color = Gold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(day.name, color = White, fontSize = 14.sp, maxLines = 2)
                        Text("${day.exercises.size} gyakorlat", color = Muted, fontSize = 11.sp)
                    }
                    Text("›", color = Gold)
                }
            }
        }
        centerItem { Pill(wearText("‹ Kezdőlap"), tone = Secondary, onClick = onBack) }
    }
}

@Composable
internal fun WorkoutPager(
    workout: WearWorkout,
    restRemaining: Int,
    health: JSONObject?,
    measuring: Boolean,
    gps: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onGps: () -> Unit,
    onChange: (String, Double) -> Unit,
    onComplete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onEdit: (String) -> Unit,
    onTimerTools: () -> Unit,
    home: WatchHomeSnapshot?,
    onRefreshJournal: () -> Unit,
    onOpenJournal: () -> Unit,
    onSelectMeasurement: (MiniJournalWorkout) -> Unit,
    initialPage: Int,
    onReturnPageConsumed: () -> Unit,
    onFinish: () -> Unit,
    onHome: () -> Unit,
    onDiscard: () -> Unit = {},
    onRestAdjust367: ((WearRestTarget367, Int) -> Unit)? = null,
    onPauseWorkout367: ((WearPauseTarget367) -> Unit)? = null,
    onPhoneControl: (() -> Unit)? = null,
) {
    val pager = rememberPagerState(initialPage = initialPage, pageCount = { 3 })
    LaunchedEffect(Unit) { onReturnPageConsumed() }
    LaunchedEffect(pager.currentPage) { if (pager.currentPage == 2) onRefreshJournal() }
    val scope = rememberCoroutineScope()
    var controls by remember(workout.workoutId) { mutableStateOf(false) }
    val pauseTarget = wearPauseTarget367(workout)
    val restTarget = wearRestTarget367(workout, System.currentTimeMillis())
    val blocked = WearUndo188.blocked(workout) || WearOwnership193.pending(workout) || workout.controlIntent367 != null
    val canControlWatch = (home?.controlProtocol367 ?: 0) >= 1 && workout.timerOwner == "watch"
    val canControlPhone = (home?.controlProtocol367 ?: 0) >= 2 && workout.timerOwner == "phone" && workout.nativeExecutionProtocol == 2 && workout.generation > 0
    val pauseAction: (() -> Unit)? = if ((canControlWatch || canControlPhone) && pauseTarget != null && onPauseWorkout367 != null && !blocked)
        ({ onPauseWorkout367(pauseTarget) }) else null
    val canAdjustWatch = canControlWatch && (restTarget?.timerKey.isNullOrEmpty() ||
        (home?.controlProtocol367 ?: 0) >= if(workout.timerCheckpoint193!=null) 6 else 4)
    val adjustAction: ((Int) -> Unit)? = if ((canAdjustWatch || canControlPhone && ((home?.controlProtocol367 ?: 0) >= 5 || workout.automation?.restOnly == true && (home?.controlProtocol367 ?: 0) >= 3)) && restTarget != null && onRestAdjust367 != null && !blocked)
        ({ delta -> onRestAdjust367(restTarget, delta) }) else null
    val phoneControl = onPhoneControl?.takeIf { workout.timerOwner == "phone" && !blocked }
    val command = LocalAutomaticCommand.current
    // Only automatic timer pause is currently exposed to this UI; never pretend it pauses the whole workout.
    val automatic = workout.automation
    val timerPause: (() -> Unit)? = if (automatic?.enabled == true && automatic.counting && command != null)
        ({ command(if (automatic.paused) "resume" else "pause") }) else null
    BackHandler(enabled = controls || pager.currentPage != 0) {
        if (controls) controls = false else scope.launch { pager.scrollToPage(0) }
    }
    val gesture = wearWorkoutGesture(
        enabled = !controls,
        page = pager.currentPage,
        onTap = { controls = true },
        onPage = { page -> scope.launch { pager.scrollToPage(page) } },
    )
    Box(Modifier.fillMaxSize().then(gesture)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), userScrollEnabled = false) { page ->
            Box(
                Modifier.fillMaxSize()
                    .hierarchicalFocusGroup(active = pager.currentPage == page)
                    .then(
                        if (pager.currentPage == page) Modifier
                        else Modifier.clearAndSetSemantics {}
                    )
            ) {
                if (page == 0) {
                    Box(Modifier.fillMaxSize()) {
                        if (restRemaining > 0) RestRing(workout, restRemaining)
                        val onMenu = {
                            controls = true
                            Unit
                        }
                        if (restRemaining > 0 && (workout.automation?.enabled != true || workout.automation?.restOnly == true ||
                            workout.automation?.phase == "rest" && adjustAction != null))
                            WearRestV2(workout, restRemaining, onSkipRest, onMenu, onAdjust = adjustAction, onPhoneControl = phoneControl)
                        else if(workout.automation?.enabled==true && WearAutomaticEngine.timed(workout.exercise))
                            AutomaticTimedSetScreen(workout,restRemaining,health,onNext,onSkipRest,onFinish,onMenu)
                        else if (normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp"))
                            TimedSetScreen(
                                workout,
                                restRemaining,
                                health,
                                onChange,
                                onComplete,
                                onNext,
                                onSkipRest,
                                onFinish,
                                onMenu,
                            )
                        else
                            CurrentSetScreen(
                                workout,
                                restRemaining,
                                health,
                                onEdit,
                                onComplete,
                                onNext,
                                onSkipRest,
                                onFinish,
                                onMenu,
                                home = home,
                            )
                    }
                } else if (page == 2)
                    HealthScreen(
                        health,
                        measuring,
                        gps,
                        true,
                        onEnable,
                        onDisable,
                        onGps,
                        home,
                        onRefreshJournal,
                        onOpenJournal,
                        onSelectMeasurement,
                    ) {
                        scope.launch { pager.animateScrollToPage(0) }
                    }
                else
                    WorkoutMenu(
                        workout,
                        {
                            onPrevious()
                            scope.launch { pager.animateScrollToPage(0) }
                        },
                        {
                            onNext()
                            scope.launch { pager.animateScrollToPage(0) }
                        },
                        { index ->
                            onSelect(index)
                            scope.launch { pager.animateScrollToPage(0) }
                        },
                        onEdit,
                        onTimerTools,
                        restRemaining,
                        onFinish,
                        onHome,
                        onDiscard = onDiscard,
                        onBack = { scope.launch { pager.scrollToPage(0) } },
                        onHealth = { scope.launch { pager.scrollToPage(2) } },
                        onPauseWorkout = pauseAction,
                        onPhoneControl = phoneControl,
                    )
            }
        }
        Box(
            Modifier.fillMaxSize().padding(top = 6.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                Modifier.testTag("wear-page-indicator"),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier.width(if (pager.currentPage == index) 12.dp else 4.dp)
                            .height(2.dp)
                            .background(
                                if (pager.currentPage == index) Gold else Muted.copy(alpha = .45f),
                                CircleShape,
                            )
                    )
                }
            }
        }
        if (controls && pager.currentPage == 0) {
            WearWorkoutControlOverlay(
                workout = workout,
                timerPause = timerPause,
                workoutPause = pauseAction,
                onPhoneControl = phoneControl,
                onPrevious = { controls = false; onPrevious() },
                onNext = { controls = false; onNext() },
                onFinish = { controls = false; onFinish() },
                onDiscard = { controls = false; onDiscard() },
                onMore = { controls = false; scope.launch { pager.scrollToPage(1) } },
                onDismiss = { controls = false },
            )
        }
    }
}

@Composable
internal fun RestRing(workout: WearWorkout, seconds: Int) {
    val color = Gold
    val remainingFraction=wearRestProgress(workout,seconds)
    Canvas(Modifier.fillMaxSize().padding(8.dp).testTag("wear-rest-ring")
        .semantics { progressBarRangeInfo=ProgressBarRangeInfo(remainingFraction,0f..1f) }) {
        drawArc(color.copy(alpha = .18f), -90f, 360f, false, style = Stroke(4.dp.toPx()))
        drawArc(
            color,
            -90f,
            360f * remainingFraction,
            false,
            style = Stroke(4.dp.toPx()),
        )
    }
}

@Composable
internal fun WorkoutPanel(
    workout: WearWorkout,
    health: JSONObject?,
    onMenu: () -> Unit,
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.(Boolean, Boolean) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bodyHeight = wearWorkoutPageLayout(maxWidth.value, maxHeight.value).bodyHeight
        val showPrevious = wearShowPreviousRow(bodyHeight, LocalDensity.current.fontScale)
        // 367 (3.1/3.3/3.4): every workout screen has the same fixed bottom action and heart-rate row.
        FixedRoundPanel(
            workout.exercise?.name.orEmpty(),
            count = 1,
            sweep = 140f,
            headerColor = White,
            onTitleClick = onMenu,
            titleDescription = "${workout.exercise?.name.orEmpty()} · " + wearText("Edzésmenü"),
            footerSize = WEAR_FOOTER_BUTTON_DP,
            rimMargin = WEAR_FOOTER_RIM_MARGIN_DP,
            captionBand = 0f,
            pinned = { WearPinnedPulse(workout, health) },
            footer = footer,
        ) { compact ->
            if(workout.ownershipProtocol193==1&&WearGps193.distance(workout.exercise))LocalGpsShortcut216.current?.let { shortcut ->
                val code=shortcut.state.takeIf {it in listOf("idle","acquiring","recording","paused","stopped","permission","locationOff","storage","unavailable","interrupted","deleted")} ?: "idle"
                Text((if(shortcut.restSeconds>0)WearMenu216.text("rest")+" "+formatSeconds(shortcut.restSeconds)+"\n" else "")+"GPS: "+WearGpsText193.text(code),color=Gold,fontSize=10.sp,textAlign=TextAlign.Center,
                    modifier=Modifier.fillMaxWidth().heightIn(min=40.dp).clickable(role=Role.Button,onClick=shortcut.open)
                        .testTag("wear-gps-shortcut216").padding(4.dp))
            }
            content(compact, showPrevious)
        }
    }
}

@Composable
internal fun SetHeading(workout: WearWorkout, restRemaining: Int, detail: String? = null, compact: Boolean = false) {
    val completed = workout.exercise?.sets?.count { it.done } ?: 0
    val count = workout.exercise?.sets?.size ?: 0
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60000L - now % 60000L)
        }
    }
    val clock = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    val automatic=workout.automation
    val command=LocalAutomaticCommand.current
    val toggle=LocalAutomaticToggle.current
    val touchLock=LocalTouchLock189.current
    val heading = when {
        automatic?.awaitingPhone==true -> wearText("Telefonra vár")
        automatic?.enabled==true && automatic.interrupted -> wearText("Megszakadt · folytatható")
        automatic?.enabled==true && automatic.paused -> wearFormat("Szünet · %s", formatSeconds((automatic.remaining(android.os.SystemClock.elapsedRealtime())/1000).toInt()))
        automatic?.enabled==true && automatic.phase=="rest" -> wearFormat("Pihenő %s", formatSeconds(restRemaining))
        restRemaining > 0 -> wearFormat("Pihenő %s · %d/%d", formatSeconds(restRemaining), completed, count)
        workout.exerciseComplete -> wearFormat("✓ %d/%d sorozat rögzítve", completed, count)
        else -> detail ?: wearFormat("%d/%d · Sorozat %d/%d", workout.currentExercise + 1, workout.exercises.size, workout.currentSetIndex + 1, count)
    }
    val fontScale = LocalDensity.current.fontScale
    val captionScale = fontScale.coerceAtMost(if(compact)1f else 1.35f) / fontScale
    // 367: a fixed heading row; the 24 dp lock and 20 dp automatic toggle keep their touch size but no
    // longer make the row taller than its text (that pushed the heart rate under the action).
    Row(Modifier.fillMaxWidth().height(if (compact) 14.dp else 16.dp), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        Text(heading, modifier = Modifier.weight(1f, fill = false).then(if(automatic?.enabled==true&&automatic.counting&&command!=null)
            Modifier.clickable(role=Role.Button){command(if(automatic.paused)"resume" else "pause")}else Modifier), color = Gold,
            fontSize = (8 * captionScale).sp, lineHeight = (10 * captionScale).sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(" · ", color = Muted, fontSize = (7 * captionScale).sp,
            lineHeight = (10 * captionScale).sp, maxLines = 1)
        Text(clock, modifier = Modifier.testTag("wear-workout-clock")
            .semantics { contentDescription = wearFormat("Pontos idő: %s", clock) },
            color = Muted, fontSize = (7 * captionScale).sp,
            lineHeight = (10 * captionScale).sp, maxLines = 1)
        if(toggle!=null) Text(if(automatic?.enabled==true)" A" else " M",color=if(automatic?.enabled==true)Gold else Muted,
            fontSize=(7*captionScale).sp,modifier=Modifier.requiredSize(if(compact)16.dp else 20.dp).clickable(role=Role.Button,onClick=toggle)
                .testTag("wear-automatic-toggle").semantics { contentDescription=if(automatic?.enabled==true)wearText("Automata mód kikapcsolása") else wearText("Automata mód bekapcsolása") })
        if(touchLock!=null)Box(Modifier.requiredSize(24.dp).clickable(role=Role.Button,onClick=touchLock).testTag("wear-touch-lock")
            .semantics { contentDescription=WearTouchCopy189.language(java.util.Locale.getDefault().language).lock },contentAlignment=Alignment.Center) {
            WearLockIcon189(Muted,Modifier.size(14.dp))
        }
    }
}

@Composable
internal fun ValueTile(value: String, label: String, modifier: Modifier, enabled: Boolean = true,
                      onClick: () -> Unit) {
    Column(modifier.heightIn(min = 32.dp).background(Card, RoundedCornerShape(10.dp))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(value, color = White, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Label(if (label == wearText("ismétlés")) "ism." else label, size = 8)
    }
}

@Composable
internal fun WearWorkoutFooter(
    workout: WearWorkout,
    restRemaining: Int,
    canRecord: Boolean,
    recordLabel: String,
    onRecord: () -> Unit,
    onSkipRest: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val automatic = workout.automation
    val command = LocalAutomaticCommand.current
    val state = wearFooterState(
        exerciseComplete = workout.exerciseComplete,
        lastExercise = workout.currentExercise >= workout.exercises.lastIndex,
        completedSets = workout.completedSets,
        restRemaining = restRemaining,
        canRecord = canRecord,
        syncBlocked = WearUndo188.blocked(workout),
        awaitingPhone = automatic?.awaitingPhone == true,
        autoPausedWithCommand = automatic?.enabled == true && automatic.paused && command != null,
    )
    WearPrimaryFooter(state, recordLabel, onRecord, onSkipRest, onNext, onFinish,
        onResumeAuto = { command?.invoke("resume") })
}

/** Eight-second undo of the last recorded set, one place for every workout screen. */
@Composable
internal fun WearQuickUndo(workout: WearWorkout) {
    val undo = LocalUndo.current ?: return
    var quick by remember(workout.lastRecord188, workout.exercises) { mutableStateOf(WearUndo188.quick(workout, System.currentTimeMillis())) }
    LaunchedEffect(workout.lastRecord188, workout.exercises) {
        val age = System.currentTimeMillis() - (WearUndo188.record(workout)?.optLong("recordedAt") ?: 0L)
        if (quick) { delay((WearUndo188.QUICK_MILLIS - age).coerceAtLeast(0)); quick = false }
    }
    if (quick && WearUndo188.available(workout)) {
        Pill(wearText("Utolsó sorozat visszavonása"), tone = Secondary, size = 10) {
            WearUndo188.record(workout)?.optString("recordId")?.let(undo)
        }
    }
}

@Composable
internal fun AutomaticTimedSetScreen(workout: WearWorkout,restRemaining: Int,health: JSONObject?,
    onNext: ()->Unit,onSkipRest: ()->Unit,onFinish: ()->Unit,onMenu: ()->Unit) {
    val a=workout.automation ?: return
    val command=LocalAutomaticCommand.current
    WorkoutPanel(workout,health,onMenu,footer={
        WearWorkoutFooter(workout,restRemaining,false,wearText("Mentés"),{},onSkipRest,onNext,onFinish)
    }) { compact,_ ->
        SetHeading(workout,restRemaining,compact=compact)
        val waiting=a.phase=="ready"
        val remaining=if(waiting)a.workSeconds else ((a.remaining(android.os.SystemClock.elapsedRealtime())+999)/1000).toInt()
        val label=if(a.phase=="rest")wearText("Pihenő") else if(a.phase=="complete")wearText("Rögzítve") else wearText("Munka")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            // The face has one fixed size; only the drawn glyph follows start/pause (3.6).
            WearTimerFace(wearTimerGlyph(a.phase=="complete",!(a.paused||waiting)),formatSeconds(remaining),White,
                Modifier.weight(1f,fill=false).testTag("wear-automatic-clock")
                    .clickable(enabled=command!=null&&a.phase!="complete",role=Role.Button){command?.invoke(if(waiting)"start" else if(a.paused)"resume" else "pause")}
                    .semantics { contentDescription=if(waiting)wearText("Automata munkaszakasz indítása") else if(a.paused)wearText("Automata folytatása") else wearText("Automata szüneteltetése") })
            Text(label+if(a.rounds>1)wearFormat(" · %d/%d. kör", if(waiting) 1 else a.round, a.rounds) else "",color=Muted,
                fontSize=9.sp,lineHeight=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis,
                textAlign=TextAlign.Center,modifier=Modifier.weight(1f))
        }
        WearQuickUndo(workout)
    }
}

@Composable
internal fun CurrentSetScreen(
    workout: WearWorkout,
    restRemaining: Int,
    health: JSONObject?,
    onEdit: (String) -> Unit,
    onComplete: () -> Unit,
    onNext: () -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onMenu: () -> Unit,
    home: WatchHomeSnapshot? = null,
) {
    // The automatic/GPS value tiles keep their specialised layout; everything else shares the
    // same footer, heart-rate row and heading, so first/middle/last exercises look the same.
    val set = workout.currentSet
    val automatic = workout.automation
    val editable = set?.done == false && !WearUndo188.blocked(workout) &&
        (automatic?.counting != true || automatic.paused) && automatic?.awaitingPhone != true
    val specialised = automatic?.enabled == true || WearGps193.distance(workout.exercise)
    val footer: @Composable () -> Unit = {
        WearWorkoutFooter(workout, restRemaining,
            canRecord = if (specialised) set != null && !set.done && (set.reps.toIntOrNull() ?: 0) > 0
                else editable && (set?.reps?.toIntOrNull() ?: 0) > 0,
            recordLabel = wearText("Kész"), onRecord = onComplete, onSkipRest = onSkipRest, onNext = onNext, onFinish = onFinish)
    }
    if (specialised) {
        WorkoutPanel(workout, health, onMenu, footer) { compact, _ ->
            SetHeading(workout, restRemaining, compact = compact)
            wearLiveMovementRow(health, workout.workoutId)?.let {
                Text(it, color = Gold, fontSize = 10.sp, lineHeight = 12.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            WearQuickUndo(workout)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (workout.exercise?.loadType != "bodyweight")
                    ValueTile(set?.weight?.ifBlank { "0" } ?: "0", "kg", Modifier.weight(1f), editable) { onEdit("weight") }
                ValueTile(set?.reps?.ifBlank { "0" } ?: "0", wearText("ismétlés"), Modifier.weight(1f), editable) { onEdit("reps") }
            }
        }
        return
    }
    val previous = wearPreviousSetV2(home?.recentWorkouts.orEmpty(), workout.workoutId,
        workout.exercise?.name.orEmpty(), set?.number ?: 0)
    WorkoutPanel(workout, health, onMenu, footer) { compact, showPrevious ->
        SetHeading(workout, restRemaining, compact = compact)
        Row(Modifier.fillMaxWidth(.9f), horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (workout.exercise?.loadType != "bodyweight") {
                WearV2Value(set?.weight?.ifBlank { "0" } ?: "0", "kg", Modifier.weight(1f), editable) { onEdit("weight") }
                Text("×", color = Muted, fontSize = 18.sp)
            }
            WearV2Value(set?.reps?.ifBlank { "0" } ?: "0", wearText("ismétlés"), Modifier.weight(1f), editable) { onEdit("reps") }
        }
        if (showPrevious) Label(wearV2Text("last") + ": " + (previous ?: "–"), size = 10)
        WearQuickUndo(workout)
    }
}
