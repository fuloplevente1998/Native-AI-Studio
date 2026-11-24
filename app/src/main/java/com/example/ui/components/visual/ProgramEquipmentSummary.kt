package com.example.ui.components

import com.example.model.AppLanguage

fun programEquipmentSummary(
    program: com.example.model.TrainingProgramModel,
    lang: AppLanguage = AppLanguage.HU
): String {
    val allExercises = program.days.flatMap { it.exercises }
    val locHu = program.tagsHu.firstOrNull().orEmpty()
    return programEquipmentSummary(allExercises, locHu, lang)
}

fun programEquipmentSummary(
    exercises: List<com.example.model.ExerciseItem>,
    locationHu: String = "",
    lang: AppLanguage = AppLanguage.HU
): String {
    val pairs = exercises.map { equipmentWithSafetyRequirements(it.id, it.equipmentHu) to it.loadType }
    return buildEquipmentSummaryFromPairs(pairs, locationHu, lang)
}

internal fun equipmentWithSafetyRequirements(exerciseId: String, equipment: String): String =
    when (exerciseId) {
        "bench-press", "incline-bench-press", "close-grip-bench" -> "$equipment + biztonságos állvány"
        else -> equipment
    }

internal fun buildEquipmentSummaryFromPairs(
    equipmentAndLoadPairs: List<Pair<String, String>>,
    locationHu: String = "",
    lang: AppLanguage = AppLanguage.HU
): String {
    val locPrefix = when (locationHu.trim().lowercase()) {
        "otthon", "home", "zuhause", "acasă", "acasa" -> when (lang) {
            AppLanguage.HU -> "Otthoni"
            AppLanguage.EN -> "Home"
            AppLanguage.DE -> "Zuhause"
            AppLanguage.RO -> "Acasă"
        }
        "konditerem", "edzőterem", "gym", "fitnessstudio", "sală", "sala" -> when (lang) {
            AppLanguage.HU -> "Konditermi"
            AppLanguage.EN -> "Gym"
            AppLanguage.DE -> "Fitnessstudio"
            AppLanguage.RO -> "Sală"
        }
        "szabadtér", "kültéri", "outdoor" -> when (lang) {
            AppLanguage.HU -> "Szabadtéri"
            AppLanguage.EN -> "Outdoor"
            AppLanguage.DE -> "Outdoor"
            AppLanguage.RO -> "Exterior"
        }
        "saját program", "egyedi", "custom" -> when (lang) {
            AppLanguage.HU -> "Saját program"
            AppLanguage.EN -> "Custom"
            AppLanguage.DE -> "Eigenes"
            AppLanguage.RO -> "Personalizat"
        }
        else -> ""
    }

    var hasDumbbell = false
    var hasBarbell = false
    var hasKettlebell = false
    var hasBand = false
    var hasAnchor = false
    var hasMachine = false
    var hasCable = false
    var hasPullupBar = false
    var hasLowBar = false
    var hasDipBars = false
    var hasBenchSupport = false
    var hasSafetyRack = false
    var hasStep = false
    var hasBodyweight = false
    var hasOutdoor = false
    val customLabels = linkedSetOf<String>()

    for ((rawEquip, rawLoad) in equipmentAndLoadPairs) {
        val parts = rawEquip.split('+').map { it.trim() }.filter { it.isNotBlank() }
        val tokensToCheck = if (parts.isNotEmpty()) parts else listOf(rawLoad.trim())
        val loadLower = rawLoad.trim().lowercase()
        if (loadLower in setOf("per_hand", "single_dumbbell", "dumbbell")) hasDumbbell = true
        if (loadLower == "barbell") hasBarbell = true
        if (loadLower == "kettlebell") hasKettlebell = true
        if (loadLower == "band") hasBand = true
        if (loadLower == "machine") hasMachine = true
        if (loadLower == "cable") hasCable = true
        if (loadLower == "bodyweight") hasBodyweight = true

        for (part in tokensToCheck) {
            val low = part.lowercase()
            when {
                low.contains("kézisúlyzó") || low.contains("kézi súlyzó") || low == "dumbbell" || low == "per_hand" || low == "single_dumbbell" -> hasDumbbell = true
                low.contains("kétkezes rúd") || low.contains("ez-rúd") || low.contains("t-rúd") || low.contains("landmine") || low == "rúd" || low == "barbell" -> hasBarbell = true
                low.contains("kettlebell") -> hasKettlebell = true
                low.contains("gumiszalag") || low == "band" -> hasBand = true
                low.contains("rögzítés") || low.contains("anchor") -> hasAnchor = true
                low.contains("csiga") || low.contains("csigás") || low == "cable" -> hasCable = true
                low.contains("kötél") -> hasCable = true
                low.contains("gép") || low.contains("lábtoló") || low.contains("lábnyújtó") || low.contains("lábhajlító") ||
                    low.contains("mellnyomó") || low.contains("pec-deck") || low.contains("vállnyomó") ||
                    low.contains("hack guggoló") || low.contains("vádligép") || low == "machine" -> hasMachine = true
                low.contains("alacsony rúd") -> hasLowBar = true
                low.contains("húzódzkodórúd") || low.contains("heveder") -> hasPullupBar = true
                low.contains("tolódzkodó") || low.contains("korlát") -> hasDipBars = true
                low.contains("állvány") -> hasSafetyRack = true
                low.contains("fellépő") || low.contains("doboz") -> hasStep = true
                low.contains("pad") || low.contains("támasz") || low.contains("tartó/segítő") -> hasBenchSupport = true
                low.contains("testsúly") || low == "bodyweight" -> hasBodyweight = true
                low.contains("futócipő") || low.contains("gyaloglócipő") || low.contains("túracipő") ||
                    low.contains("görkorcsolya") || low.contains("kerékpár") -> hasOutdoor = true
                low.isNotBlank() && low !in setOf("total", "weight", "súlyzó / eszköz", "szabadsúly / gép") -> {
                    customLabels.add(com.example.model.translateEquipmentLabel(part, rawLoad, lang))
                }
            }
        }
    }

    val labels = mutableListOf<String>()
    fun addLabel(hu: String, en: String, de: String, ro: String) {
        labels += when (lang) {
            AppLanguage.HU -> hu
            AppLanguage.EN -> en
            AppLanguage.DE -> de
            AppLanguage.RO -> ro
        }
    }

    if (hasDumbbell) addLabel("kézisúlyzó", "dumbbells", "Kurzhanteln", "gantere")
    if (hasBarbell) addLabel("kétkezes rúd", "barbell", "Langhantel", "bară")
    if (hasSafetyRack) addLabel("biztonságos állvány", "safety rack", "Sicherheitsständer", "suport de siguranță")
    if (hasKettlebell) addLabel("kettlebell", "kettlebell", "Kettlebell", "kettlebell")
    if (hasBand) addLabel("gumiszalag", "resistance band", "Widerstandsband", "bandă elastică")
    if (hasAnchor) addLabel("stabil rögzítés", "secure anchor", "stabile Befestigung", "ancorare stabilă")
    if (hasMachine) addLabel("edzőtermi gépek", "gym machines", "Fitnessgeräte", "aparate sală")
    if (hasCable) addLabel("csigás gép", "cable machine", "Kabelzug", "aparat cu cablu")
    if (hasPullupBar) addLabel("húzódzkodórúd", "pull-up bar", "Klimmzugstange", "bară de tracțiuni")
    if (hasLowBar) addLabel("stabil alacsony rúd", "stable low bar", "feste niedrige Stange", "bară joasă stabilă")
    if (hasDipBars) addLabel("tolódzkodó korlát", "dip bars", "Dip-Barren", "bare paralele")
    if (hasBenchSupport) addLabel("pad/támasz", "bench/support", "Bank/Stütze", "bancă/sprijin")
    if (hasStep) addLabel("fellépő", "step/box", "Step/Kasten", "platformă/cutie")
    if (hasOutdoor) addLabel("szabadtéri felszerelés", "outdoor gear", "Outdoor-Ausrüstung", "echipament exterior")
    for (custom in customLabels) {
        if (labels.none { it.equals(custom, ignoreCase = true) }) {
            labels += custom
        }
    }
    if (hasBodyweight) {
        if (labels.isEmpty()) {
            addLabel("Saját testsúlyos • Eszköz nélkül", "Bodyweight • No equipment", "Eigengewicht • Ohne Geräte", "Greutatea corpului • Fără echipament")
        } else {
            addLabel("saját testsúly", "bodyweight", "Körpergewicht", "greutatea corpului")
        }
    }

    if (labels.isEmpty()) {
        addLabel("Saját összeállítás", "Custom equipment", "Eigene Ausrüstung", "Echipament propriu")
    }

    val joined = labels.joinToString(", ").replaceFirstChar { it.uppercase() }
    return if (locPrefix.isNotBlank() && !joined.startsWith(locPrefix, ignoreCase = true)) {
        "$locPrefix • $joined"
    } else {
        joined
    }
}

fun programEquipmentCategoryLabel(programNameOrId: String, lang: AppLanguage = AppLanguage.HU): String {
    val trimmed = programNameOrId.trim()
    val norm = trimmed.replace('–', '-').replace('—', '-').lowercase()
    val homeBasicAliases = setOf(
        "home-basic", "otthoni a/b", "otthoni a/b - alap", "otthoni alap a/b",
        "home a/b", "home a/b - basic", "zuhause a/b", "zuhause a/b - basis",
        "acasă a/b", "acasă a/b - bază"
    )
    runCatching {
        val (progs, days) = com.repforge.app.data.db.NativeBuiltinCatalog.programs
        val exById = com.repforge.app.data.db.NativeBuiltinCatalog.exercises.associateBy { it.id }
        val matchedProg = progs.firstOrNull { p ->
            p.id.equals(trimmed, ignoreCase = true) ||
                p.nameHu.replace('–', '-').equals(norm, ignoreCase = true) ||
                p.nameEn.replace('–', '-').equals(norm, ignoreCase = true) ||
                p.nameDe.replace('–', '-').equals(norm, ignoreCase = true) ||
                p.nameRo.replace('–', '-').equals(norm, ignoreCase = true) ||
                (p.id == "home-basic" && norm in homeBasicAliases)
        }
        if (matchedProg != null) {
            val progDays = days.filter { it.programId == matchedProg.id }
            val pairs = progDays.flatMap { d ->
                val arr = org.json.JSONArray(d.exercisesJson)
                (0 until arr.length()).mapNotNull { idx ->
                    val item = arr.get(idx)
                    val exId = if (item is org.json.JSONObject) item.optString("id") else item.toString()
                    val ex = exById[exId] ?: return@mapNotNull null
                    val rawEq = runCatching { org.json.JSONObject(ex.rawJson).optString("equipment") }.getOrDefault("")
                    equipmentWithSafetyRequirements(exId, rawEq) to ex.loadType
                }
            }
            if (pairs.isNotEmpty()) {
                return buildEquipmentSummaryFromPairs(pairs, matchedProg.location, lang)
            }
        }
    }
    val lower = trimmed.lowercase()
    return when {
        lower.contains("gumiszalag") || lower.contains("band") -> when (lang) {
            AppLanguage.HU -> "Otthoni • Gumiszalag, stabil rögzítés, saját testsúly"
            AppLanguage.EN -> "Home • Resistance band, secure anchor, bodyweight"
            AppLanguage.DE -> "Zuhause • Widerstandsband, stabile Befestigung, Körpergewicht"
            AppLanguage.RO -> "Acasă • Bandă elastică, ancorare stabilă, greutatea corpului"
        }
        lower.contains("kettlebell") -> when (lang) {
            AppLanguage.HU -> "Otthoni • Kettlebell, saját testsúly"
            AppLanguage.EN -> "Home • Kettlebell, bodyweight"
            AppLanguage.DE -> "Zuhause • Kettlebell, Körpergewicht"
            AppLanguage.RO -> "Acasă • Kettlebell, greutatea corpului"
        }
        lower.contains("testsúly") || lower.contains("bodyweight") || lower.contains("eigengewicht") || lower.contains("calisthen") -> when (lang) {
            AppLanguage.HU -> "Otthoni • Saját testsúly, húzódzkodórúd, tolódzkodó korlát"
            AppLanguage.EN -> "Home • Bodyweight, pull-up bar, dip bars"
            AppLanguage.DE -> "Zuhause • Körpergewicht, Klimmzugstange, Dip-Barren"
            AppLanguage.RO -> "Acasă • Greutatea corpului, bară de tracțiuni, bare paralele"
        }
        lower.contains("otthon") || lower.contains("home") || lower.contains("zuhause") || lower.contains("acasă") -> when (lang) {
            AppLanguage.HU -> "Otthoni • Kézisúlyzó, kétkezes rúd, pad/támasz, saját testsúly"
            AppLanguage.EN -> "Home • Dumbbells, barbell, bench/support, bodyweight"
            AppLanguage.DE -> "Zuhause • Kurzhanteln, Langhantel, Bank/Stütze, Körpergewicht"
            AppLanguage.RO -> "Acasă • Gantere, bară, bancă/sprijin, greutatea corpului"
        }
        else -> when (lang) {
            AppLanguage.HU -> "Konditermi • Kétkezes rúd, kézisúlyzó, edzőtermi gépek"
            AppLanguage.EN -> "Gym • Barbell, dumbbells, gym machines"
            AppLanguage.DE -> "Fitnessstudio • Langhantel, Kurzhanteln, Fitnessgeräte"
            AppLanguage.RO -> "Sală • Bară, gantere, aparate sală"
        }
    }
}
