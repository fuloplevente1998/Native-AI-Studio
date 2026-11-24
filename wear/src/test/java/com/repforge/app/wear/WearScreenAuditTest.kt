package com.repforge.app.wear

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.util.Locale

/**
 * 2777: screen audit for a Galaxy Watch 7 40 mm class display (192 dp round, xhdpi = 384 px).
 * Renders every route of the real app composable with fixture data and writes PNGs to
 * build/test-results/wear-screens (uploaded with the CI test reports). "tall" shots show a whole
 * list so wrapping and clipped rows are visible; "round" shots show the first visible viewport.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class WearScreenAuditTest {
    private val started = java.time.Instant.now().minusSeconds(14 * 60).toString()

    private fun exercise(id: String, name: String, unit: String, load: String, sets: Int, weight: Double, reps: String = "") =
        JSONObject().put("id", id).put("name", name).put("loadType", load).put("repUnit", unit).put("targetReps", reps)
            .put("sets", JSONArray().apply { repeat(sets) { put(JSONObject().put("set", it + 1).put("weight", weight).put("reps", reps)) } })

    private fun home(): WatchHomeSnapshot {
        val dayA = JSONObject().put("id", "a").put("name", "A nap").put("programId", "home").put("programName", "Otthoni A/B – Alap")
            .put("exercises", JSONArray()
                .put(exercise("goblet", "Goblet guggolás", "ism.", "dumbbell", 3, 12.5, "12"))
                .put(exercise("floor-press", "Földön fekve nyomás kézisúlyzóval", "ism.", "dumbbell", 3, 10.0, "10"))
                .put(exercise("plank", "Oldalsó plank", "mp", "bodyweight", 2, 0.0, "30")))
        val dayB = JSONObject(dayA.toString()).put("id", "b").put("name", "B nap")
        val recent = JSONArray().put(JSONObject().put("workoutId", "w1").put("started", "2026-11-18T08:00:00Z")
            .put("finished", "2026-11-18T08:52:00Z").put("dayName", "A nap")
            .put("exercises", JSONArray().put(JSONObject().put("name", "Goblet guggolás")
                .put("sets", JSONArray().put(JSONObject().put("number", 1).put("reps", "12").put("weight", 12.5))
                    .put(JSONObject().put("number", 2).put("reps", "10").put("weight", 12.5)))))
            .put("wear", JSONObject().put("source", "wear_health_services").put("averageHeartRate", 118).put("maxHeartRate", 151)
                .put("activeCalories", 312).put("steps", 1820).put("activeDurationSeconds", 2900)))
        val calendar = JSONArray()
        val today = java.time.LocalDate.now().toString()
        calendar.put(JSONObject().put("date", today).put("status", "planned").put("scheduleId", "today-a")
            .put("plannedStart", "${today}T10:00:00").put("programId", "home").put("dayId", "a"))
        listOf("2026-11-19" to "planned", "2026-11-20" to "rest", "2026-11-21" to "planned", "2026-11-17" to "completed").forEach { (d, s) ->
            calendar.put(JSONObject().put("date", d).put("status", s).put("scheduleId", "s$d").put("plannedStart", "${d}T10:00:00")
                .put("programId", "home").put("dayId", "a"))
        }
        return WatchHomeStore.parse(JSONObject().put("schema", 1).put("restSeconds", 90)
            .put("activeProgramId", "home").put("activeProgramName", "Otthoni A/B – Alap")
            .put("days", JSONArray().put(dayA).put(dayB))
            .put("recommended", JSONObject().put("scheduleId", "s1").put("plannedStart", "2026-11-19T10:00:00").put("day", dayA))
            .put("calendar", calendar)
            .put("quickExercises", JSONArray()
                .put(exercise("pushup", "Fekvőtámasz", "ism.", "bodyweight", 3, 0.0, "15"))
                .put(exercise("plank", "Plank", "mp", "bodyweight", 1, 0.0, "45"))
                .put(exercise("run", "Futás", "m", "distance", 1, 0.0)))
            .put("journalSchema", 1).put("recentWorkouts", recent)
            .put("ownershipProtocol193", 1)
            .toString())!!
    }

    private fun shoot(name: String, round: Boolean, content: @Composable () -> Unit) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent { content() }
        repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(250)) }
        val view = activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val out = if (round) Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { masked ->
            val canvas = Canvas(masked); canvas.drawColor(Color.rgb(40, 40, 40))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val layer = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            Canvas(layer).apply {
                drawCircle(view.width / 2f, view.height / 2f, minOf(view.width, view.height) / 2f, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN); drawBitmap(bitmap, 0f, 0f, paint)
            }
            canvas.drawBitmap(layer, 0f, 0f, null)
        } else bitmap
        val dir = File("build/test-results/wear-screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        controller.pause().stop().destroy()
    }

    @Composable
    private fun app(route: String, workout: WearWorkout? = null, closure: WearClosure? = null, rest: Int = 0) {
        val h = home()
        TrainPilotWearApp(
            workout = mutableStateOf(workout), home = mutableStateOf(h), closure = mutableStateOf(closure),
            restRemaining = mutableStateOf(rest), onStart = { _, _ -> }, onStartQuick = {}, onAddQuick = {},
            onChange = { _, _ -> }, onComplete = {}, onPrevious = {}, onNext = {}, onSelectExercise = {}, onSkipRest = {},
            onFinish = {}, onDiscard = {}, onResumeWorkout = {}, onRetry = {}, onDismissClosure = {}, onOpenPhone = {},
            onRefreshJournal = {}, health = mutableStateOf(JSONObject().put("heartRate", 128)
                .put("workoutId",workout?.workoutId.orEmpty()).put("source","wear_health_services")
                .put("updatedAt",System.currentTimeMillis()).put("state","active").put("averageHeartRate",118)
                .put("activeCalories",120).put("totalCalories",145)), measuring = mutableStateOf(true),
            gpsEnabled = mutableStateOf(false), requestedRoute = mutableStateOf<String?>(null), onRouteConsumed = {},
            onEnableHealth = {}, onDisableHealth = {}, onToggleGps = {},
            phoneConnected = mutableStateOf(true), batteryLevel = mutableStateOf(64), initialRoute = route)
    }

    private val routes = listOf("home", "menu", "planned216", "calendar", "days", "quick", "journal", "journal-detail",
        "metrics", "settings", "save-sync")
    private val workoutRoutes = listOf("workout", "editor", "timer", "finish", "discard", "ownership193", "gpsRoute193")

    private fun all(round: Boolean, suffix: String) {
        val previousLocale=Locale.getDefault()
        try {
        Locale.setDefault(Locale.forLanguageTag("hu"))
        val h = home()
        val strength = h.createWorkout(h.days.first(), "", started)
        val timed = strength.copy(currentExercise = 2)
        routes.forEach { route -> shoot("$suffix-$route", round) { app(if (route == "journal-detail") "journal" else route) } }
        workoutRoutes.forEach { route -> shoot("$suffix-$route-strength", round) { app(route, strength) } }
        listOf("workout", "timer").forEach { route -> shoot("$suffix-$route-timed", round) { app(route, timed) } }
        shoot("$suffix-summary", round) { app("summary", closure = WearClosure("c", "w1", "finishWorkout", "done", "", 3120, 3, 8)) }
        val written = File("build/test-results/wear-screens").listFiles()?.count { it.name.startsWith(suffix) } ?: 0
        assertTrue("screens written: $written", written >= routes.size + workoutRoutes.size)
        } finally { Locale.setDefault(previousLocale) }
    }

    @Config(sdk = [34], qualifiers = "w192dp-h192dp-round-watch-xhdpi")
    @Test fun roundViewport() = all(round = true, suffix = "round")

    private fun compactAudit(suffix: String, language: String, scale: Float) {
        val previous=Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(language))
            val h=home()
            val workout=h.createWorkout(h.days.first(),"",started)
            val rest=workout.copy(restEndAt=System.currentTimeMillis()+90000L)
            for(route in listOf("home","calendar","metrics","journal","workout","finish")) {
                shoot("$suffix-$route",true) {
                    val density=LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {
                        app(route,if(route in listOf("workout","finish"))workout else null)
                    }
                }
            }
            shoot("$suffix-rest",true) { app("workout",rest,rest=90) }
        } finally { Locale.setDefault(previous) }
    }

    @Config(sdk = [34], qualifiers = "w176dp-h176dp-round-watch-xhdpi")
    @Test fun compactEnglishLargeText()=compactAudit("compact-en-130","en",1.3f)

    @Config(sdk = [34], qualifiers = "w216dp-h216dp-round-watch-xhdpi")
    @Test fun largeGerman()=compactAudit("large-de","de",1f)

    @Config(sdk = [34], qualifiers = "w192dp-h192dp-round-watch-xhdpi")
    @Test fun romanianLargeText()=compactAudit("round-ro-130","ro",1.3f)

    @Config(sdk = [34], qualifiers = "w192dp-h560dp-notround-watch-xhdpi")
    @Test fun tallLists() = all(round = false, suffix = "tall")
}
