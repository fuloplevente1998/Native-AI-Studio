package com.repforge.app.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import org.json.JSONObject

@Composable
internal fun TrainPilotWearApp(
    workout: State<WearWorkout?>,
    home: State<WatchHomeSnapshot?>,
    closure: State<WearClosure?>,
    restRemaining: State<Int>,
    onStart: (WatchHomeDay, String) -> Unit,
    onStartQuick: (WatchHomeExercise) -> Unit,
    onAddQuick: (WatchHomeExercise) -> Unit,
    onChange: (String, Double) -> Unit,
    onComplete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectExercise: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onResumeWorkout: () -> Unit,
    onRetry: () -> Unit,
    onDismissClosure: () -> Unit,
    onOpenPhone: () -> Unit,
    onRefreshJournal: () -> Unit,
    health: State<JSONObject?>,
    measuring: State<Boolean>,
    gpsEnabled: State<Boolean>,
    requestedRoute: State<String?>,
    onRouteConsumed: () -> Unit,
    onEnableHealth: () -> Unit,
    onDisableHealth: () -> Unit,
    onToggleGps: () -> Unit,
    phoneConnected: State<Boolean?>? = null,
    batteryLevel: State<Int?>? = null,
    onToggleAutomatic: (() -> Unit)? = null,
    onAutomaticCommand: ((String) -> Unit)? = null,
    onEditorSave: ((String,Double,String)->Boolean)? = null,
    onWorkoutFeedback: ((WatchWorkoutFeedback, String?) -> Unit)? = null,
    saveSync: State<WearSaveSyncState?>? = null,
    onRetrySync: ((String) -> Unit)? = null,
    onUndo: ((String) -> Unit)? = null,
    onRequestOwnership193: (() -> Unit)? = null,
    onRetryOwnership193: (() -> Unit)? = null,
    gpsRoute193: State<JSONObject?>? = null,
    onStartGpsRoute193: (() -> Unit)? = null,
    onStopGpsRoute193: (() -> Unit)? = null,
    touchClock189: ()->Long = { android.os.SystemClock.uptimeMillis() },
    /** Start route; only the screen audit passes another one (production always starts at "home"). */
    initialRoute: String = "home",
    onRestAdjust367: ((WearRestTarget367, Int) -> Unit)? = null,
    onPauseWorkout367: ((WearPauseTarget367) -> Unit)? = null,
) {
    var route by rememberSaveable { mutableStateOf(initialRoute) }
    val context = LocalContext.current
    val daily = rememberWearDailyHealth()
    var pendingGps216 by remember { mutableStateOf<GpsLaunch216?>(null) }
    var gpsBack216 by rememberSaveable { mutableStateOf("menu") }
    var daysBack216 by rememberSaveable { mutableStateOf("home") }
    fun startWithGps216(exercise: WatchHomeExercise?, start: () -> Unit) {
        if (WearMenu216.eligible(exercise)) {
            pendingGps216=GpsLaunch216(exercise!!.id,start);gpsBack216=route;route="gpsStart216"
        } else { start();if(workout.value!=null)route="workout" }
    }
    var answeredWorkout by rememberSaveable { mutableStateOf("") }
    val localFeedback = closure.value?.takeIf { it.needsFeedback && it.status != "error" }?.let {
        WatchWorkoutFeedback.parse(JSONObject().put("workoutId", it.workoutId).put("started", it.started))
    }
    val pendingFeedback = (home.value?.pendingFeedback ?: localFeedback)?.takeUnless {
        it.workoutId == answeredWorkout || WearWorkoutFeedback.answered(context, it.workoutId)
    }
    LaunchedEffect(route) { if (route == "journal" || route == "metrics") onRefreshJournal() }
    LaunchedEffect(requestedRoute.value) {
        requestedRoute.value?.let {
            route = it
            onRouteConsumed()
        }
    }
    LaunchedEffect(pendingFeedback?.workoutId, workout.value?.workoutId, requestedRoute.value) {
        if (pendingFeedback != null && workout.value == null && onWorkoutFeedback != null) route = "feedback"
        else if (route == "feedback") route = "home"
    }
    LaunchedEffect(workout.value?.automation?.enabled, route) {
        if (workout.value?.automation?.enabled == true && route == "timer") route = "workout"
    }
    var editorField by rememberSaveable { mutableStateOf("reps") }
    var miniJournalId by rememberSaveable { mutableStateOf("") }
    var miniJournalBackRoute by rememberSaveable { mutableStateOf("journal") }
    var journalBackRoute by rememberSaveable { mutableStateOf("menu") }
    var calendarBackRoute by rememberSaveable { mutableStateOf("menu") }
    var calendarDetail by remember { mutableStateOf(false) }
    var syncBackRoute by rememberSaveable { mutableStateOf("home") }
    var metricsBackRoute by rememberSaveable { mutableStateOf("menu") }
    var settingsBackRoute by rememberSaveable { mutableStateOf("home") }
    var returnWorkoutPage by rememberSaveable { mutableStateOf(0) }
    var discardBackRoute by rememberSaveable { mutableStateOf("home") }
    val touchIdentity=WearTouchLock189.identity(workout.value)
    var touchLocked by remember(touchIdentity) { mutableStateOf(WearTouchLock189.locked(context,workout.value)) }
    val touchCopy=WearTouchCopy189.language(java.util.Locale.getDefault().language)
    LaunchedEffect(touchIdentity) { WearTouchLock189.retain(context,workout.value) }
    fun lockTouches() {
        val active=workout.value ?: return
        if(WearTouchLock189.set(context,active,true)) { returnWorkoutPage=0;route="workout";touchLocked=true }
    }
    BackHandler(enabled=touchLocked) { /* System Home/Power remain available; Back cannot open a hidden app action. */ }
    fun openMeasurement(item: MiniJournalWorkout, origin: String) {
        miniJournalId = item.workoutId
        miniJournalBackRoute = origin
        if (origin == "workout") returnWorkoutPage = 2
        route = "journal-detail"
    }
    fun openJournal(origin: String) {
        journalBackRoute = origin
        if (origin == "workout") returnWorkoutPage = 2
        route = "journal"
    }
    BackHandler(enabled = route != "home" && !touchLocked) {
        route =
            when (route) {
                "editor",
                "timer",
                "finish" -> "workout"
                "feedback" -> {
                    answeredWorkout = pendingFeedback?.workoutId.orEmpty()
                    pendingFeedback?.let { onWorkoutFeedback?.invoke(it, null) }
                    "home"
                }
                "gpsStart216" -> { pendingGps216=null;gpsBack216 }
                "gpsRoute193" -> "workout"
                "planned216", "quick", "ownership193" -> "menu"
                "calendar" -> calendarBackRoute
                "days" -> daysBack216
                "save-sync" -> syncBackRoute
                "journal-detail" -> miniJournalBackRoute
                "journal" -> journalBackRoute
                "metrics" -> metricsBackRoute
                "daily-details" -> "metrics"
                "settings" -> settingsBackRoute
                "discard" -> discardBackRoute
                else -> "home"
            }
    }
    CompositionLocalProvider(
        LocalGpsShortcut216 provides GpsShortcut216(gpsRoute193?.value?.optString("state","idle") ?: "idle",restRemaining.value) { route="gpsRoute193" },
        LocalWearHomeTheme provides (home.value?.theme ?: WearHomeTheme.DEFAULT),
        LocalAutomaticToggle provides onToggleAutomatic,
        LocalAutomaticCommand provides onAutomaticCommand,
        LocalEditorSave provides onEditorSave,
        LocalUndo provides onUndo,
        LocalTouchLock189 provides if(workout.value!=null)({lockTouches()})else null,
        LocalSaveSync provides saveSync?.value,
        LocalOwnership193 provides workout.value?.takeIf { it.ownershipProtocol193==1 },
        LocalOpenSaveSync provides { syncBackRoute=route;route="save-sync" }
    ) {
        val syncedTheme = LocalWearHomeTheme.current
        MaterialTheme(colorScheme = syncedTheme.materialColors(MaterialTheme.colorScheme)) {
            val mainRoute = if (workout.value != null || touchLocked) null else when {
                route == "home" -> route
                route == "journal" && journalBackRoute == "home" -> route
                route == "metrics" && metricsBackRoute == "home" -> route
                route == "calendar" && calendarBackRoute == "home" && !calendarDetail -> route
                else -> null
            }
            WearMainNavigation(mainRoute, onPage = { destination ->
                when (destination) {
                    "journal" -> journalBackRoute = "home"
                    "metrics" -> metricsBackRoute = "home"
                    "calendar" -> calendarBackRoute = "home"
                }
                route = destination
            }) {
                val active = workout.value
                when {
                    touchLocked && active != null -> WearTouchLockedScreen189(active,restRemaining.value,health.value,touchCopy,onUnlock={
                        if(WearTouchLock189.set(context,active,false)){touchLocked=false;returnWorkoutPage=0;route="workout"}
                    },nowMillis=touchClock189)
                    route == "feedback" && pendingFeedback != null ->
                        WorkoutFeedbackScreen { effort ->
                            onWorkoutFeedback?.invoke(pendingFeedback, effort)
                            answeredWorkout = pendingFeedback.workoutId
                            route = "home"
                        }
                    route == "settings" ->
                        SettingsScreen(
                            measuring.value,
                            gpsEnabled.value,
                            onEnableHealth,
                            onDisableHealth,
                            onToggleGps,
                            onMetrics = {
                                metricsBackRoute = "settings"
                                route = "metrics"
                            },
                            onOpenPhone = onOpenPhone,
                            backLabel = if (settingsBackRoute == "home") wearText("‹ Kezdőlap") else wearText("‹ Főmenü"),
                            onBack = { route = settingsBackRoute },
                        )
                    route == "daily-details" || (route == "metrics" && metricsBackRoute != "home") ->
                        HealthScreen(
                            health.value,
                            measuring.value,
                            gpsEnabled.value,
                            active != null,
                            onEnableHealth,
                            onDisableHealth,
                            onToggleGps,
                            home.value,
                            onRefreshJournal,
                            onOpenJournal = { openJournal("metrics") },
                            onSelectMeasurement = { openMeasurement(it, "metrics") },
                            onBack = { route = if (route == "daily-details") "metrics" else metricsBackRoute },
                        )
                    route == "metrics" ->
                        WearDailyHealthScreen(daily, home.value?.dailyStepsGoal, health.value, active,
                            onDetails = { route = "daily-details" },
                            onBack = { route = metricsBackRoute })
                    route == "save-sync" -> SaveSyncScreen(saveSync?.value,
                        onRetry = { saveSync?.value?.let { onRetrySync?.invoke(it.workoutId) } },
                        onBack = { route=syncBackRoute })
                    route == "ownership193" && active?.ownershipProtocol193==1 ->
                        OwnershipScreen193(active,phoneConnected?.value,onRequestOwnership193,onRetryOwnership193,
                            onResumeWorkout,onOpenPhone,onBack={route="menu"},onGps={route="gpsRoute193"})
                    route == "gpsRoute193" && active?.ownershipProtocol193==1 ->
                        GpsRouteScreen193(active,gpsRoute193?.value,onStartGpsRoute193,onStopGpsRoute193,onBack={route="workout"})
                    route == "gpsStart216" && pendingGps216!=null -> GpsStartScreen216(pendingGps216!!.activityId,
                        onStart={ val request=pendingGps216;pendingGps216=null;request?.start?.invoke();route=if(workout.value!=null)"workout" else gpsBack216 },
                        onCancel={pendingGps216=null;route=gpsBack216})
                    route == "planned216" -> RoundList(WearMenu216.text("planned")) {
                        centerItem { Pill(WearMenu216.text("calendar")) {calendarBackRoute="menu";route="calendar"} }
                        centerItem { Pill(WearMenu216.text("days"),tone=Card) {daysBack216="planned216";route="days"} }
                        centerItem { Pill(WearMenu216.text("back"),tone=Secondary) {route="menu"} }
                    }
                    route == "menu" ->
                        MainMenuScreen(active != null, active?.quickWorkout == true, onOpenPhone) {
                            if (it == "journal") openJournal("menu")
                            else {
                                if (it == "metrics") metricsBackRoute = "menu"
                                if (it == "calendar") calendarBackRoute = "menu"
                                if (it == "settings") settingsBackRoute = "menu"
                                if (it == "discard") discardBackRoute = "menu"
                                route = it
                            }
                        }
                    route == "journal" ->
                        MiniJournalScreen(
                            home.value,
                            onRefresh = onRefreshJournal,
                            onOpenPhone = onOpenPhone,
                            backLabel =
                                when (journalBackRoute) {
                                    "menu" -> wearText("‹ Főmenü")
                                    "home" -> wearText("‹ Kezdőlap")
                                    else -> wearText("‹ Mérések")
                                },
                            onBack = { route = journalBackRoute },
                            mainPage = journalBackRoute == "home",
                        ) { selected ->
                            miniJournalId = selected.workoutId
                            miniJournalBackRoute = "journal"
                            route = "journal-detail"
                        }
                    route == "journal-detail" ->
                        MiniJournalDetailScreen(
                            home.value?.recentWorkouts?.firstOrNull {
                                it.workoutId == miniJournalId
                            },
                            backLabel =
                                if (miniJournalBackRoute == "journal") wearText("‹ Napló") else wearText("‹ Mérések"),
                            onBack = { route = miniJournalBackRoute },
                        )
                    route == "quick" ->
                        QuickWorkoutPicker(
                            home.value,
                            active,
                            active == null &&
                                home.value?.hasDraft == false &&
                                closure.value?.status !in listOf("pending", "error"),
                            onBack = { route = "menu" },
                        ) { exercise ->
                            if (active?.quickWorkout == true) onAddQuick(exercise)
                            else if (active == null) startWithGps216(exercise) {onStartQuick(exercise)}
                            if (workout.value?.quickWorkout == true) route = "workout"
                        }
                    route == "calendar" ->
                        CalendarScreen(
                            home.value,
                            active == null &&
                                home.value?.hasDraft == false &&
                                closure.value?.status !in listOf("pending", "error"),
                            onBack = { route = calendarBackRoute },
                            onDays = { daysBack216 = "calendar"; route = "days" },
                            mainPage = calendarBackRoute == "home",
                            onDetailChange = { calendarDetail = it },
                        ) { entry ->
                            entry.day?.let {
                                startWithGps216(it.exercises.firstOrNull()) {onStart(it, entry.scheduleId)}
                                if (workout.value != null) route = "workout"
                            }
                        }
                    route == "summary" && closure.value != null ->
                        SummaryScreen(closure.value!!, health.value, onRetry) {
                            onDismissClosure()
                            route = "home"
                        }
                    active?.controlIntent367 != null && route in listOf("workout", "editor", "timer", "finish", "discard") ->
                        RoundList(wearSaveTitle()) {
                            centerItem { Label(wearSaveLabel(WearSavePhase.ERROR), lines = 2) }
                            centerItem { Pill(wearSaveRetry()) { onRetrySync?.invoke(active.workoutId) } }
                            centerItem { Pill(WearMenu216.text("back"), tone = Secondary) { route = "home" } }
                        }
                    route == "discard" && active != null ->
                        ConfirmationScreen(true, active.completedSets) {
                            if (it) {
                                onDiscard()
                                if (workout.value == null || closure.value?.let { it.workoutId == active.workoutId && it.status == "error" } == true)
                                    route = "summary"
                            } else route = discardBackRoute
                        }
                    route == "finish" && active != null ->
                        WearFinishPreview(active, health.value, onBack = { route = "workout" }) {
                            onFinish()
                            // A rejected close (for example pending ownership) must not navigate to success.
                            if (workout.value == null)
                                route = if (onWorkoutFeedback != null) "feedback" else "summary"
                            else if (closure.value?.let { it.workoutId == active.workoutId && it.status == "error" } == true)
                                route = "summary"
                        }
                    route == "editor" && active != null ->
                        ValueEditor(active, editorField, onChange) { route = "workout" }
                    route == "timer" && active != null ->
                        TimedSetToolsScreen(
                            active,
                            onChange,
                            onComplete,
                            onEdit = {
                                editorField = it
                                route = "editor"
                            },
                            onBack = { route = "workout" },
                        )
                    route == "days" && home.value != null ->
                        ProgramDays(home.value!!, onBack = { route = daysBack216 }) { day ->
                            startWithGps216(day.exercises.firstOrNull()) {onStart(day, "")}
                            if (workout.value != null) route = "workout"
                        }
                    route in listOf("workout","editor","timer") && active?.workoutPaused==true ->
                        RoundList(if(active.ownershipProtocol193==1)WearOwnershipText193.text("pausedTitle")else wearText("Edzés szünetel")) {
                            centerItem { Text(active.exercise?.name.orEmpty(),color=White,fontSize=14.sp,
                                textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth()) }
                            centerItem { Text(if(active.ownershipProtocol193==1)WearOwnershipText193.text("pausedDetail")else wearText("A mérés és az időzítő szünetel."),color=Muted,fontSize=11.sp,textAlign=TextAlign.Center) }
                            if(active.ownershipProtocol193==1&&WearOwnership193.pending(active)) {
                                centerItem { Text(WearOwnershipText193.text("pending"),color=Muted,fontSize=11.sp,textAlign=TextAlign.Center) }
                                centerItem { Pill(WearOwnershipText193.text("retry"),tone=Secondary) { onRetryOwnership193?.invoke() } }
                            }else if(active.ownershipProtocol193==1&&(active.timerOwner=="watch" || active.timerOwner=="phone" && active.nativeExecutionProtocol==2 && (home.value?.controlProtocol367 ?: 0)>=2)) {
                                centerItem { Pill(WearOwnershipText193.text("resume"),tone=Gold) { onResumeWorkout() } }
                            }else centerItem { Pill(wearText("Folytatás telefonon"),tone=Gold) { onOpenPhone() } }
                            centerItem { Pill(wearText("Edzés befejezése"), enabled = active.completedSets > 0 && !WearUndo188.blocked(active) && !WearOwnership193.pending(active)) { route="finish" } }
                            centerItem { Pill(WearMenu216.text("discard"), tone = Danger, enabled = !WearUndo188.blocked(active) && !WearOwnership193.pending(active)) { discardBackRoute="workout"; route="discard" } }
                            centerItem { Pill(if(active.ownershipProtocol193==1)WearOwnershipText193.text("home")else wearText("Kezdőlap"),tone=Secondary) { route="home" } }
                        }
                    route == "workout" && active != null ->
                        WorkoutPager(
                            active,
                            restRemaining.value,
                            health.value,
                            measuring.value,
                            gpsEnabled.value,
                            onEnableHealth,
                            onDisableHealth,
                            onToggleGps,
                            onChange,
                            onComplete,
                            onPrevious,
                            onNext,
                            onSelectExercise,
                            onSkipRest,
                            home = home.value,
                            onRefreshJournal = onRefreshJournal,
                            onOpenJournal = { openJournal("workout") },
                            onSelectMeasurement = { openMeasurement(it, "workout") },
                            initialPage = returnWorkoutPage,
                            onReturnPageConsumed = { returnWorkoutPage = 0 },
                            onEdit = {
                                editorField = it
                                route = "editor"
                            },
                            onTimerTools = { route = "timer" },
                            onFinish = { route = "finish" },
                            onHome = { route = "home" },
                            onDiscard = { discardBackRoute = "workout"; route = "discard" },
                            onRestAdjust367 = onRestAdjust367,
                            onPauseWorkout367 = onPauseWorkout367,
                            onPhoneControl = onOpenPhone,
                        )
                    else ->
                        WearHomeDashboard(
                            home.value,
                            active,
                            closure.value,
                            health.value,
                            measuring.value,
                            phoneConnected?.value,
                            batteryLevel?.value,
                            onStart = { day, scheduleId ->
                                startWithGps216(day.exercises.firstOrNull()) {onStart(day, scheduleId)}
                                if (workout.value != null) route = "workout"
                            },
                            onResume = {
                                if(active?.workoutPaused==true&&!(active.ownershipProtocol193==1&&(active.timerOwner=="watch" || active.timerOwner=="phone" && active.nativeExecutionProtocol==2 && (home.value?.controlProtocol367 ?: 0)>=2)))onOpenPhone()else onResumeWorkout()
                                route = "workout"
                            },
                            onDays = { daysBack216="home";route = "days" },
                            onResult = { route = "summary" },
                            onJournal = { openJournal("home") },
                            onDiscard = { discardBackRoute = "home"; route = "discard" },
                            onSettings = {
                                settingsBackRoute = "home"
                                route = "settings"
                            },
                            onMenu = { route = "menu" },
                            onOpenPhone = onOpenPhone,
                            dailyHealth = daily.data,
                            dailyPulse = daily.pulse,
                        )
                }
            }
        }
    }
}
