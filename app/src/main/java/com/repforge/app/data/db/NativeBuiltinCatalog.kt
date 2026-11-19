package com.repforge.app.data.db

import org.json.JSONArray
import org.json.JSONObject

/** Data-only snapshot of the accepted main. No WebView/JavaScript engine runs in the native app. */
internal object NativeBuiltinCatalog {
    private val catalog: JSONObject by lazy {
        val input = requireNotNull(NativeBuiltinCatalog::class.java.getResourceAsStream("/native-catalog-main-7473.json")) {
            "Missing bundled native catalog"
        }
        val json = JSONObject(input.bufferedReader().use { it.readText() })
        check(json.getJSONObject("source").getString("appBlob") == "b5567576bed9a8f93608a6bf1ed8db1dee4723f4")
        json
    }
    fun programMetadata(program: ProgramEntity, field: String, language: String): String {
        val fallback = if (field == "location") program.location else program.level
        if (!program.builtin) return fallback
        val rows = catalog.getJSONArray("programs")
        for (index in 0 until rows.length()) {
            val entry = rows.getJSONObject(index)
            val raw = entry.getJSONObject("raw")
            if (raw.optString("id") == program.id && raw.optString(field) == fallback)
                return entry.getJSONObject(if (field == "location") "locations" else "levels").optString(language, fallback)
        }
        return fallback
    }
    val exercises: List<ExerciseLibraryEntity> by lazy {
        val rows = catalog.getJSONArray("exercises")
        (0 until rows.length()).map { index ->
            val entry = rows.getJSONObject(index)
            val raw = JSONObject(entry.getJSONObject("raw").toString())
            val names = entry.getJSONObject("names"); val notes = entry.getJSONObject("notes")
            val id = raw.getString("id"); val demo = entry.optJSONObject("demo")
            val muscles = entry.getJSONArray("muscles")
            raw.put("measurementType", entry.getString("measurementType"))
            for (language in listOf("hu", "en", "de", "ro")) raw.put(language, names.getString(language))
            raw.put("notes", notes.getString("hu")).put("notesEn", notes.getString("en"))
                .put("notesDe", notes.getString("de")).put("notesRo", notes.getString("ro"))
                .put("muscles", muscles).put("custom", false)
            if (demo != null) raw.put("demo", demo)
            val video = demo?.optString("videoId")?.takeIf { Regex("[A-Za-z0-9_-]{11}").matches(it) }
            val youtube = demo?.optString("provider") == "youtube" && video != null
            ExerciseLibraryEntity(id, names.getString("hu"), names.getString("en"), names.getString("de"), names.getString("ro"),
                raw.getString("loadType"), raw.getString("repUnit"), entry.getString("measurementType"),
                raw.optInt("sets", 3), raw.optString("reps"), raw.optDouble("weight", 0.0),
                raw.optInt("restSeconds", raw.optInt("rest", 90)), muscles.optString(0, "full"), muscles.toString(),
                notes.getString("hu"), notes.getString("en"), notes.getString("de"), notes.getString("ro"),
                if (youtube) "https://www.youtube.com/watch?v=$video" else null, if (youtube) "youtube" else null,
                false, raw.optBoolean("setRir229"), raw.toString())
        }.also { rows -> check(rows.size == 140 && rows.map { it.id }.distinct().size == rows.size) }
    }
    private val reviewedDays = mapOf(
        "home-basic" to mapOf("B" to listOf("reverse-lunge", "one-arm-row", "pushup",
            "db-rdl", "lateral-raise", "oh-triceps", "dead-bug")),
        "calisthenics-ab" to mapOf(
            "A" to listOf("bodyweight-squat", "pullup", "pushup", "glute-bridge", "inverted-row", "hollow-hold"),
            "B" to listOf("single-leg-glute-bridge", "chinup", "pike-pushup", "prisoner-squat",
                "hanging-knee-raise", "superman-hold")),
        "bands-ab" to mapOf(
            "A" to listOf("bodyweight-squat", "band-chest-press", "band-row", "glute-bridge",
                "band-overhead-press", "band-pallof-press"),
            "B" to listOf("prisoner-squat", "band-lat-pulldown", "band-chest-press", "single-leg-glute-bridge",
                "band-face-pull", "band-pallof-press")),
        "gym-ppl" to mapOf("C" to listOf("barbell-back-squat", "db-rdl", "leg-curl", "leg-press",
            "standing-calf-raise", "plank"))
    )
    val reviewedProgramIds: Set<String> get() = reviewedDays.keys

    /** Only exact old stock exercise lists are eligible. An edited day protects the entire program. */
    fun reviewUpdate(current: ProgramWithDays): ProgramWithDays? {
        val old = current.program
        if (!old.builtin || old.generated || old.id !in reviewedDays) return null
        val raw = runCatching { JSONObject(old.rawJson) }.getOrNull() ?: return null
        if (raw.optJSONObject("prescriptions")?.length()?.let { it > 0 } == true) return null
        val previous = originalPrograms.second.filter { it.programId == old.id }.associateBy { it.dayId }
        val updated = programs.second.filter { it.programId == old.id }.associateBy { it.dayId }
        if (current.days.size != previous.size || current.days.any { day ->
                val before = previous[day.dayId] ?: return@any true
                day.exercisesJson != before.exercisesJson && day.exercisesJson != updated.getValue(day.dayId).exercisesJson
            }) return null
        val days = current.days.map { day -> day.copy(exercisesJson = updated.getValue(day.dayId).exercisesJson) }
        if (days == current.days) return null
        val byDay = days.associateBy { it.dayId }
        raw.optJSONArray("days")?.let { rawDays ->
            for (i in 0 until rawDays.length()) rawDays.optJSONObject(i)?.let { row ->
                byDay[row.optString("id")]?.let { row.put("exercises", JSONArray(it.exercisesJson)) }
            }
        }
        return ProgramWithDays(old.copy(rawJson = raw.toString()), days)
    }

    private val legacyHomeBasicDayA = listOf("goblet-squat", "db-floor-press", "one-arm-row", "db-ohp", "db-rdl", "db-curl", "side-plank")

    private fun entryIds(json: String): List<String>? = runCatching {
        val rows = JSONArray(json)
        (0 until rows.length()).map { rows.optJSONObject(it)?.getString("id") ?: rows.getString(it) }
    }.getOrNull()

    /**
     * 2779: the quick-workout "+" appended exercises in place into the built-in Otthoni A/B
     * (day A grew from 7 to 10). Only exercises appended after the complete stock list are
     * removed; the stock entries keep the user's own sets/reps/kg. Any other edit (reorder,
     * removal, swap) leaves the day untouched.
     */
    fun trimAppendedHomeBasic(current: ProgramWithDays): ProgramWithDays? {
        val old = current.program
        if (old.id != "home-basic" || !old.builtin || old.generated) return null
        val stock = (programs.second + originalPrograms.second).filter { it.programId == old.id }
        val days = current.days.map { day ->
            val ids = entryIds(day.exercisesJson) ?: return@map day
            val bases = stock.filter { it.dayId == day.dayId }.mapNotNull { entryIds(it.exercisesJson) } +
                listOfNotNull(legacyHomeBasicDayA.takeIf { day.dayId == "A" })
            val base = bases.firstOrNull { it.isNotEmpty() && ids.size > it.size && ids.take(it.size) == it } ?: return@map day
            val rows = JSONArray(day.exercisesJson)
            day.copy(exercisesJson = JSONArray().apply { for (i in base.indices) put(rows.get(i)) }.toString())
        }
        if (days == current.days) return null
        val raw = runCatching { JSONObject(old.rawJson) }.getOrNull()
        val byDay = days.associateBy { it.dayId }
        raw?.optJSONArray("days")?.let { rawDays ->
            for (i in 0 until rawDays.length()) rawDays.optJSONObject(i)?.let { row ->
                byDay[row.optString("id")]?.let { row.put("exercises", JSONArray(it.exercisesJson)) }
            }
        }
        return ProgramWithDays(if (raw != null) old.copy(rawJson = raw.toString()) else old, days)
    }

    private val originalPrograms by lazy { readPrograms(reviewed = false) }
    val programs by lazy { readPrograms(reviewed = true) }

    private fun readPrograms(reviewed: Boolean): Pair<List<ProgramEntity>, List<ProgramDayEntity>> {
        val rows = catalog.getJSONArray("programs")
        val days = ArrayList<ProgramDayEntity>()
        val programs = (0 until rows.length()).map { index ->
            val entry = rows.getJSONObject(index); val raw = JSONObject(entry.getJSONObject("raw").toString())
            val names = entry.getJSONObject("names"); val id = raw.getString("id")
            for (language in listOf("hu", "en", "de", "ro")) raw.put(language, names.getString(language))
            val sourceDays = entry.getJSONArray("days")
            val rawDays = JSONArray()
            for (dayIndex in 0 until sourceDays.length()) {
                val sourceDay = sourceDays.getJSONObject(dayIndex)
                val day = JSONObject(sourceDay.getJSONObject("raw").toString()); val labels = sourceDay.getJSONObject("names")
                for (language in listOf("hu", "en", "de", "ro")) day.put(language, labels.getString(language))
                val dayId = day.getString("id")
                if (reviewed) reviewedDays[id]?.get(dayId)?.let { day.put("exercises", JSONArray(it)) }
                rawDays.put(day)
                days.add(ProgramDayEntity("$id:$dayId", id, dayId, labels.getString("hu"), labels.getString("en"),
                    labels.getString("de"), labels.getString("ro"), dayIndex, day.getJSONArray("exercises").toString()))
            }
            raw.put("days", rawDays)
            ProgramEntity(id, names.getString("hu"), names.getString("en"), names.getString("de"), names.getString("ro"),
                raw.optString("location"), raw.optString("level"), raw.optString("description"), true, raw.optBoolean("generated"),
                raw.optString("sourceType", "template"), raw.optString("sourceTemplateId", id), raw.optInt("templateVersion", 1),
                raw.optString("accentHex", "#f2bd45"), index, 0L, raw.toString())
        }
        check(programs.size == 13)
        val ids = exercises.map { it.id }.toSet()
        for (day in days) {
            val rows = JSONArray(day.exercisesJson)
            for (index in 0 until rows.length()) check((rows.optJSONObject(index)?.getString("id") ?: rows.getString(index)) in ids)
        }
        return programs to days
    }
}
