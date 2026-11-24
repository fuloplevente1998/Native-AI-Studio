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
    val canAdjustWatch = canControlWatch && (restTarget?.timerKey.isNullOrEmpty() || (home?.controlProtocol367 ?: 0) >= 4)
    val adjustAction: ((Int) -> Unit)? = if ((canAdjustWatch || canControlPhone && (home?.controlProtocol367 ?: 0) >= 3) && restTarget != null && onRestAdjust367 != null && !blocked)
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
                            workout.timerOwner == "watch" && workout.automation?.phase == "rest" && adjustAction != null))
                            WearRestV2(workout, restRemaining, onSkipRest, onMenu, onAdjust = adjustAction, onPhoneControl = phoneControl)
                        else if(workout.automation?.enabled==true && WearAutomaticEngine.timed(workout.exercise))
                            AutomaticTimedSetScreen(workout,restRemaining,health,onPrevious,onNext,onSkipRest,onFinish,onMenu)
                        else if (normalizeUnit(workout.exercise?.repUnit.orEmpty()).startsWith("mp"))
                            TimedSetScreen(
                                workout,
                                restRemaining,
                                health,
                                onChange,
                                onComplete,
                                onPrevious,
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
                                onPrevious,
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
    onMenu: () -> Unit,
    actions: @Composable (WearArcLayout) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = wearArcLayout(maxWidth.value, maxHeight.value)
        FixedRoundPanel(
            workout.exercise?.name.orEmpty(),
            arc = true,
            sweep = 140f,
            headerColor = White,
            footer = {
                Box(Modifier.width(layout.width.dp).height(layout.height.dp)) { actions(layout) }
            },
        ) {
            if(workout.ownershipProtocol193==1&&WearGps193.distance(workout.exercise))LocalGpsShortcut216.current?.let { shortcut ->
                val code=shortcut.state.takeIf {it in listOf("idle","acquiring","recording","paused","stopped","permission","locationOff","storage","unavailable","interrupted","deleted")} ?: "idle"
                Text((if(shortcut.restSeconds>0)WearMenu216.text("rest")+" "+formatSeconds(shortcut.restSeconds)+"\n" else "")+"GPS: "+WearGpsText193.text(code),color=Gold,fontSize=10.sp,textAlign=TextAlign.Center,
                    modifier=Modifier.fillMaxWidth().heightIn(min=40.dp).clickable(role=Role.Button,onClick=shortcut.open)
                        .testTag("wear-gps-shortcut216").padding(4.dp))
            }
            content()
        }
        Box(
            Modifier.align(Alignment.TopCenter)
                .width(minOf(maxWidth, maxHeight) * .7f)
                .height(32.dp)
                .clickable(role = Role.Button, onClick = onMenu)
                .semantics {
                    contentDescription = "${workout.exercise?.name.orEmpty()} · Edzésmenü"
                }
        )
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
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
            fontSize=(7*captionScale).sp,modifier=Modifier.size(if(compact)16.dp else 20.dp).clickable(role=Role.Button,onClick=toggle)
                .testTag("wear-automatic-toggle").semantics { contentDescription=if(automatic?.enabled==true)wearText("Automata mód kikapcsolása") else wearText("Automata mód bekapcsolása") })
        if(touchLock!=null)Box(Modifier.size(24.dp).clickable(role=Role.Button,onClick=touchLock).testTag("wear-touch-lock")
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
internal fun WorkoutControls(workout: WearWorkout, restRemaining: Int, recordingEnabled: Boolean,
                            recordLabel: String = wearText("Rögzítés"), onRecord: () -> Unit,
                            onPrevious: () -> Unit, onNext: () -> Unit, onSkipRest: () -> Unit, onFinish: () -> Unit,
                            layout: WearArcLayout) {
    val automatic=workout.automation
    val command=LocalAutomaticCommand.current
    val undo=LocalUndo.current
    var quickUndo by remember(workout.lastRecord188,workout.exercises) { mutableStateOf(WearUndo188.quick(workout,System.currentTimeMillis())) }
    LaunchedEffect(workout.lastRecord188,workout.exercises) {
        val age=System.currentTimeMillis()-(WearUndo188.record(workout)?.optLong("recordedAt") ?: 0L)
        if(quickUndo){delay((WearUndo188.QUICK_MILLIS-age).coerceAtLeast(0));quickUndo=false}
    }
    CompositionLocalProvider(LocalActionSize provides layout.buttonSize) {
        Box {
            RoundControl("‹", enabled = workout.currentExercise > 0, description = wearText("Előző gyakorlat"), onClick = onPrevious)
        }
        Box(Modifier.offset(x = layout.sideOffset.dp, y = (layout.centerTop).dp)) {
            when {
                quickUndo&&undo!=null&&WearUndo188.available(workout) -> RoundControl("↶",Danger,caption=wearText("Visszavonás"),description=wearText("Utolsó sorozat visszavonása"),onClick={WearUndo188.record(workout)?.optString("recordId")?.let(undo)})
                WearUndo188.blocked(workout) -> RoundControl("…",enabled=false,caption=wearText("Egyeztetés"),description=wearText("Visszavonás egyeztetése"),onClick={})
                automatic?.awaitingPhone==true -> RoundControl("…",enabled=false,caption=wearText("Telefon"),description=wearText("Várakozás a telefonra"),onClick={})
                automatic?.enabled==true&&automatic.paused&&command!=null -> RoundControl("▶",caption=wearText("Folytatás"),description=wearText("Automata folytatása"),onClick={command("resume")})
                restRemaining > 0 -> RoundControl("»", caption = wearText("Kihagyás"), description = wearText("Pihenő kihagyása"), onClick = onSkipRest)
                workout.exerciseComplete -> RoundControl("✓", enabled = false, caption = wearText("Rögzítve"), onClick = {})
                else -> RoundControl("✓", SaveTone, enabled = recordingEnabled, caption = recordLabel,
                    description = if (recordLabel == wearText("Mentés")) wearText("Idő rögzítése") else wearText("Sorozat rögzítése"), onClick = onRecord)
            }
        }
        Box(Modifier.offset(x = (2 * layout.sideOffset).dp)) {
            if (workout.currentExercise == workout.exercises.lastIndex)
                // 2777: finishing is a different action than recording: own glyph and tone, never a second ✓.
                RoundControl("■", Gold,
                    enabled = workout.completedSets > 0 && !WearUndo188.blocked(workout), caption = wearText("Befejezés"), description = wearText("Edzés befejezése"), onClick = onFinish)
            else RoundControl("›", if (workout.exerciseComplete) Gold else Secondary,
                description = wearText("Következő gyakorlat"), onClick = onNext)
        }
    }
}

@Composable
internal fun AutomaticTimedSetScreen(workout: WearWorkout,restRemaining: Int,health: JSONObject?,
    onPrevious: ()->Unit,onNext: ()->Unit,onSkipRest: ()->Unit,onFinish: ()->Unit,onMenu: ()->Unit) {
    val a=workout.automation ?: return
    val command=LocalAutomaticCommand.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bodyHeight=wearFixedPageLayout(maxWidth.value,maxHeight.value,true).bodyHeight
        val tiny=bodyHeight<55f
        val fontScale=LocalDensity.current.fontScale
        val scale=fontScale.coerceAtMost(if(tiny)1f else 1.25f)/fontScale
        val rows=wearLiveWorkoutRows(health,workout.workoutId,System.currentTimeMillis())
        WorkoutPanel(workout,onMenu,actions={layout->WorkoutControls(workout,restRemaining,false,
            onRecord={},onPrevious=onPrevious,onNext=onNext,onSkipRest=onSkipRest,onFinish=onFinish,layout=layout)}) {
            // On the smallest circle, measurements share the heading instead of pushing
            // the actionable countdown below the fixed body. Full metrics remain on page 3.
            SetHeading(workout,restRemaining,detail=if(tiny)rows.firstOrNull() else null,compact=tiny)
            val displayed = if (rows.size <= 2) rows else listOf(rows[0], "${rows[1]} · ${rows[2]}")
            if(!tiny)displayed.take(if(bodyHeight>=96f&&fontScale<1.5f)2 else 1).forEach {
                Text(it,color=Gold,fontSize=(9*scale).sp,lineHeight=(11*scale).sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            val waiting=a.phase=="ready"
            val remaining=if(waiting)a.workSeconds else ((a.remaining(android.os.SystemClock.elapsedRealtime())+999)/1000).toInt()
            val label=if(a.phase=="rest")wearText("Pihenő") else if(a.phase=="complete")wearText("Rögzítve") else wearText("Munka")
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("${if(a.paused||waiting)"▶" else "Ⅱ"} ${formatSeconds(remaining)}",color=White,
                    fontSize=((if(tiny)18 else 24)*scale).sp,lineHeight=((if(tiny)20 else 28)*scale).sp,
                    fontWeight=FontWeight.SemiBold,maxLines=1,modifier=Modifier.weight(1f).testTag("wear-automatic-clock")
                        .clickable(enabled=command!=null&&a.phase!="complete",role=Role.Button){command?.invoke(if(waiting)"start" else if(a.paused)"resume" else "pause")}
                        .semantics { contentDescription=if(waiting)wearText("Automata munkaszakasz indítása") else if(a.paused)wearText("Automata folytatása") else wearText("Automata szüneteltetése") })
                Text(label+if(a.rounds>1)wearFormat(" · %d/%d. kör", if(waiting) 1 else a.round, a.rounds) else "",color=Muted,
                    fontSize=((if(tiny)8 else 9)*scale).sp,lineHeight=((if(tiny)9 else 10)*scale).sp,maxLines=2,
                    textAlign=TextAlign.Center,modifier=Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun LiveWorkoutHealth(workout: WearWorkout, health: JSONObject?) {
    val rows = wearLiveWorkoutRows(health, workout.workoutId, System.currentTimeMillis())
    if (rows.isEmpty()) return
    val displayed = if (rows.size <= 2) rows else listOf(rows[0], "${rows[1]} · ${rows[2]}")
    displayed.take(2).forEach {
        Text(it, color = Gold, fontSize = 10.sp, lineHeight = 12.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun CurrentSetScreen(
    workout: WearWorkout,
    restRemaining: Int,
    health: JSONObject?,
    onEdit: (String) -> Unit,
    onComplete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onMenu: () -> Unit,
    home: WatchHomeSnapshot? = null,
) {
    // Keep the specialized automatic/GPS controls intact; V2 strength layout only applies
    // to an ordinary editable set.
    val set = workout.currentSet
    val automatic = workout.automation
    val editable = set?.done == false && !WearUndo188.blocked(workout) &&
        (automatic?.counting != true || automatic.paused) && automatic?.awaitingPhone != true
    if (automatic?.enabled == true || WearGps193.distance(workout.exercise)) {
        WorkoutPanel(workout, onMenu, actions = { layout ->
            WorkoutControls(workout, restRemaining,
                recordingEnabled = set != null && !set.done && (set.reps.toIntOrNull() ?: 0) > 0,
                onRecord = onComplete, onPrevious = onPrevious, onNext = onNext,
                onSkipRest = onSkipRest, onFinish = onFinish, layout = layout)
        }) {
            SetHeading(workout, restRemaining)
            LiveWorkoutHealth(workout, health)
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
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.workoutId) {
        while (true) { now = System.currentTimeMillis(); delay(1000L) }
    }
    val pulse = wearHomeLivePulse(health, workout.workoutId, true, now)
    val undo = LocalUndo.current
    FixedRoundPanel(workout.exercise?.name.orEmpty(), count = 1, sweep = 140f,
        headerColor = White, onTitleClick = onMenu,
        footer = {
            CompositionLocalProvider(LocalActionSize provides 56f) {
                when {
                    workout.exerciseComplete && workout.currentExercise < workout.exercises.lastIndex ->
                        RoundControl("›", Gold, enabled = !WearUndo188.blocked(workout),
                            caption = wearText("Következő"), description = wearText("Következő gyakorlat"), onClick = onNext)
                    workout.exerciseComplete ->
                        RoundControl("■", Gold, enabled = workout.completedSets > 0 && !WearUndo188.blocked(workout),
                            caption = wearText("Befejezés"), description = wearText("Edzés befejezése"), onClick = onFinish)
                    else -> RoundControl("✓", SaveTone,
                        enabled = editable && restRemaining == 0 && (set?.reps?.toIntOrNull() ?: 0) > 0,
                        caption = wearText("Kész"), description = wearText("Sorozat rögzítése"), onClick = onComplete)
                }
            }
        },
    ) { compact ->
        SetHeading(workout, restRemaining, compact = compact)
        Row(Modifier.fillMaxWidth(.9f), horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (workout.exercise?.loadType != "bodyweight") {
                WearV2Value(set?.weight?.ifBlank { "0" } ?: "0", "kg", Modifier.weight(1f), editable) { onEdit("weight") }
                Text("×", color = Muted, fontSize = 18.sp)
            }
            WearV2Value(set?.reps?.ifBlank { "0" } ?: "0", wearText("ismétlés"), Modifier.weight(1f), editable) { onEdit("reps") }
        }
        Label(wearV2Text("last") + ": " + (previous ?: "–"), size = 10)
        Label("♥ " + (pulse?.let { "$it bpm" } ?: "–"), color = LocalWearHomeTheme.current.secondaryColor, size = 11)
        if (undo != null && WearUndo188.quick(workout, now)) {
            Pill(wearText("Utolsó sorozat visszavonása"), tone = Secondary, size = 10) {
                WearUndo188.record(workout)?.optString("recordId")?.let(undo)
            }
        }
    }
}
