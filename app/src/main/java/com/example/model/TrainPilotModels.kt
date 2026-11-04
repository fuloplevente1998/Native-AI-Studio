package com.example.model

enum class AppLanguage(val code: String, val label: String, val flag: String) {
    HU("HU", "Magyar", "🇭🇺"),
    EN("EN", "English", "🇬🇧"),
    DE("DE", "Deutsch", "🇩🇪"),
    RO("RO", "Română", "🇷🇴")
}

enum class MainTab {
    HOME,
    PROGRAMS,
    WORKOUT,
    CALENDAR,
    PROGRESS,
    COACH,
    HEALTH,
    LOG
}

/**
 * Sub-screens that can be pushed on top of the main tabs, matching V01..V24 from the mockups and main app.
 * Back navigation pops back to the previous screen or tab while preserving state.
 */
enum class SubScreen {
    NONE,
    PROGRAMS_LIST,          // 1/4 #3 Programok
    PROGRAM_EDITOR,         // 4/4 #6 Program szerkesztése
    CALENDAR_PLANNER,       // 1/4 #4 Naptár + main Újratervezés
    EXERCISE_LIBRARY,       // 1/4 #5 Gyakorlatok
    EXERCISE_DETAIL,        // 1/4 #6 Gyakorlat részletei
    ACTIVE_STRENGTH,        // 2/4 #1 Súlyzós edzés
    ACTIVE_TIMED,           // 2/4 #2 Időalapú edzés (Plank)
    ACTIVE_BILATERAL,       // 2/4 #3 Kétoldali mérés (Oldalsó plank)
    ACTIVE_OUTDOOR_GPS,     // 2/4 #4 Kültéri mérés (Futás GPS)
    INTERVAL_SETTINGS,      // 2/4 #5 Intervallumok beállítása
    WORKOUT_SUMMARY,        // 2/4 #6 Edzés befejezése / összegzése
    LOG_WORKOUT_DETAIL,     // 3/4 #2 Edzés részletei
    LOG_ROUTE_MAP,          // 3/4 #3 Edzésútvonal (Margitsziget)
    COACH_DETAIL,           // 3/4 #6 Coach + main TrainPilot Coach
    SETTINGS_MAIN,          // 4/4 #1 Beállítások + main Beállítások
    WATCH_AND_SYNC,         // 4/4 #2 Óra és szinkron (Wear OS)
    BACKUP_RESTORE,         // 4/4 #3 Mentés és visszaállítás
    PERMISSIONS_SCREEN,     // 4/4 #4 Engedélyek
    PRIVATE_PHOTOS          // 4/4 #5 Privát edzésfotók
}

enum class HomeDemoMode {
    PAUSED_WORKOUT,         // Mockup 1/4 #1: "Folytasd a megkezdett edzést! • Szüneteltetve • Teljes test A"
    PLANNED_NEXT,           // Main app Home: "Aktív program • Következő: Otthoni A/B - Alap A"
    COACH_LIGHT_DAY         // Main app Coach warning: "Könnyített nap • Mai készenlét 54/100"
}

enum class ExerciseCategory(val labelHu: String, val labelEn: String, val labelDe: String, val labelRo: String) {
    ALL("Összes", "All", "Alle", "Toate"),
    LEGS("Láb", "Legs", "Beine", "Picioare"),
    CHEST("Mell", "Chest", "Brust", "Piept"),
    BACK("Hát", "Back", "Rücken", "Spate"),
    SHOULDERS("Váll", "Shoulders", "Schultern", "Umeri"),
    ARMS("Kar", "Arms", "Arme", "Brațe"),
    CORE("Core", "Core", "Core", "Core");

    fun localized(lang: AppLanguage): String = when (lang) {
        AppLanguage.HU -> labelHu
        AppLanguage.EN -> labelEn
        AppLanguage.DE -> labelDe
        AppLanguage.RO -> labelRo
    }
}

data class ExerciseSetItem(
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val rir: Int,
    val completed: Boolean,
    val leftSec: Int = 30,
    val rightSec: Int = 30,
    val leftRecorded: Boolean = false,
    val rightRecorded: Boolean = false,
    val distanceMeters: Double? = null,
    val id: String = "",
    val actualRirProvided: Boolean = false
)

data class ExerciseItem(
    val id: String,
    val nameHu: String,
    val nameEn: String = nameHu,
    val nameDe: String = nameEn,
    val nameRo: String = nameEn,
    val category: ExerciseCategory = ExerciseCategory.CORE,
    val equipmentHu: String = "Saját testsúly",
    val primaryMusclesHu: String = "Teljes test",
    val secondaryMusclesHu: String = "Törzs",
    val defaultSets: Int = 3,
    val defaultReps: Int = 10,
    val defaultWeightKg: Float = 0f,
    val defaultRestSec: Int = 60,
    val isTimed: Boolean = false,
    val isBilateral: Boolean = false,
    val targetSeconds: Int = 45,
    val stepsHu: List<String> = emptyList(),
    val stepsEn: List<String> = emptyList(),
    val stepsDe: List<String> = emptyList(),
    val stepsRo: List<String> = emptyList(),
    val prWeightKg: Float = 0f,
    val prReps: Int = 0,
    val coachStatusHu: String = "Normál terhelés",
    val coachHintHu: String = "Stabil technika, 1–2 RIR tartalékkal.",
    val videoUrl: String = "",
    val isCustom: Boolean = false,
    val loadType: String = "",
    val repUnit: String = "",
    val measurementType: String = "",
    val targetRepsText: String = ""
) {
    fun localizedName(lang: AppLanguage): String = when (lang) {
        AppLanguage.HU -> nameHu
        AppLanguage.EN -> nameEn.ifBlank { nameHu }
        AppLanguage.DE -> nameDe.ifBlank { nameEn.ifBlank { nameHu } }
        AppLanguage.RO -> nameRo.ifBlank { nameEn.ifBlank { nameHu } }
    }

    fun localizedSteps(lang: AppLanguage): List<String> = when (lang) {
        AppLanguage.HU -> stepsHu
        AppLanguage.EN -> stepsEn.ifEmpty { stepsHu }
        AppLanguage.DE -> stepsDe.ifEmpty { stepsEn.ifEmpty { stepsHu } }
        AppLanguage.RO -> stepsRo.ifEmpty { stepsEn.ifEmpty { stepsHu } }
    }

    fun localizedEquipment(lang: AppLanguage): String =
        translateEquipmentLabel(equipmentHu, loadType, lang)

    fun localizedPrimaryMuscles(lang: AppLanguage): String =
        translateMuscleListLabel(primaryMusclesHu, lang)

    fun localizedSecondaryMuscles(lang: AppLanguage): String =
        translateMuscleListLabel(secondaryMusclesHu, lang)

    fun localizedCoachStatus(lang: AppLanguage): String = when (lang) {
        AppLanguage.HU -> coachStatusHu
        AppLanguage.EN -> if (coachStatusHu == "Normál terhelés") "Normal load" else coachStatusHu
        AppLanguage.DE -> if (coachStatusHu == "Normál terhelés") "Normale Belastung" else coachStatusHu
        AppLanguage.RO -> if (coachStatusHu == "Normál terhelés") "Sarcină normală" else coachStatusHu
    }

    fun localizedCoachHint(lang: AppLanguage): String {
        // Execution instructions are not Coach advice. Preserve actual custom/engine feedback.
        if (coachHintHu != "Stabil technika, 1–2 RIR tartalékkal.") return coachHintHu
        return when (lang) {
            AppLanguage.HU -> coachHintHu
            AppLanguage.EN -> "Controlled technique, keep 1–2 RIR in reserve."
            AppLanguage.DE -> "Kontrollierte Technik, 1–2 RIR in Reserve halten."
            AppLanguage.RO -> "Tehnică controlată, păstrează 1–2 RIR în rezervă."
        }
    }
}

private val equipmentDisplayLabels = mapOf(
    "pad" to listOf("bench", "Bank", "bancă"),
    "támasz" to listOf("support", "Stütze", "sprijin"),
    "állítható pad" to listOf("adjustable bench", "verstellbare Bank", "bancă reglabilă"),
    "vízszintes pad" to listOf("flat bench", "Flachbank", "bancă orizontală"),
    "ferde pad" to listOf("incline bench", "Schrägbank", "bancă înclinată"),
    "stabil pad" to listOf("stable bench", "stabile Bank", "bancă stabilă"),
    "stabil támasz" to listOf("stable support", "stabile Stütze", "sprijin stabil"),
    "opcionális stabil támasz" to listOf("optional stable support", "optionale stabile Stütze", "sprijin stabil opțional"),
    "stabil fellépő" to listOf("stable step", "stabiler Step", "platformă stabilă"),
    "stabil fellépő/doboz" to listOf("stable step/box", "stabiler Step/Kasten", "platformă/cutie stabilă"),
    "stabil pad/támasz" to listOf("stable bench/support", "stabile Bank/Stütze", "bancă/sprijin stabil"),
    "biztonságos állvány" to listOf("safety rack", "Sicherheitsständer", "suport de siguranță"),
    "futócipő" to listOf("running shoes", "Laufschuhe", "pantofi de alergare"),
    "gyaloglócipő" to listOf("walking shoes", "Walkingschuhe", "pantofi de mers"),
    "túracipő" to listOf("hiking shoes", "Wanderschuhe", "pantofi de drumeție"),
    "görkorcsolya" to listOf("inline skates", "Inlineskates", "role"),
    "kerékpár" to listOf("bicycle", "Fahrrad", "bicicletă"),
    "alsó csiga" to listOf("low pulley", "unterer Kabelzug", "scripete inferior"),
    "kétoldali csiga" to listOf("dual cable pulleys", "beidseitiger Kabelzug", "scripeți bilaterali"),
    "csigás lehúzó" to listOf("lat pulldown", "Latzug", "aparat de tracțiuni la cablu"),
    "kötél" to listOf("rope", "Seil", "frânghie"),
    "magas rögzítés" to listOf("high anchor", "hohe Befestigung", "ancorare înaltă"),
    "oldalsó rögzítés" to listOf("side anchor", "seitliche Befestigung", "ancorare laterală"),
    "stabil rögzítés" to listOf("secure anchor", "stabile Befestigung", "ancorare stabilă"),
    "hack guggoló gép" to listOf("hack squat machine", "Hackenschmidt-Gerät", "aparat hack squat"),
    "húzódzkodórúd" to listOf("pull-up bar", "Klimmzugstange", "bară de tracțiuni"),
    "lábhajlító gép" to listOf("leg curl machine", "Beinbeuger", "aparat pentru flexia picioarelor"),
    "ülő lábhajlító gép" to listOf("seated leg curl", "sitzender Beinbeuger", "aparat de flexie din șezut"),
    "lábnyújtó gép" to listOf("leg extension machine", "Beinstrecker", "aparat pentru extensia picioarelor"),
    "lábtoló gép" to listOf("leg press", "Beinpresse", "presă pentru picioare"),
    "mellnyomó gép" to listOf("chest press machine", "Brustpresse", "aparat pentru împins la piept"),
    "pec-deck gép" to listOf("pec deck", "Butterfly-Gerät", "aparat pec deck"),
    "rásegítéses húzódzkodó gép" to listOf("assisted pull-up machine", "unterstütztes Klimmzuggerät", "aparat de tracțiuni asistate"),
    "stabil alacsony rúd / heveder" to listOf("stable low bar / straps", "stabile niedrige Stange / Schlingen", "bară joasă stabilă / chingi"),
    "testsúly" to listOf("bodyweight", "Körpergewicht", "greutatea corpului"),
    "tolódzkodó korlát" to listOf("dip bars", "Dip-Barren", "bare paralele"),
    "vállnyomó gép" to listOf("shoulder press machine", "Schulterpresse", "aparat pentru împins deasupra capului"),
    "álló vádligép" to listOf("standing calf raise machine", "stehendes Wadenheben", "aparat pentru gambe din picioare"),
)

fun translateEquipmentLabel(rawHu: String, loadType: String = "", lang: AppLanguage): String {
    if (lang == AppLanguage.HU) return rawHu
    val key = rawHu.trim().lowercase(java.util.Locale.ROOT)
    equipmentDisplayLabels[key]?.let { values ->
        return values[when(lang) { AppLanguage.EN -> 0; AppLanguage.DE -> 1; else -> 2 }]
    }
    val loadKey = if (key.isBlank() || key in setOf("per_hand", "single_dumbbell", "bodyweight", "total", "weight", "dumbbell", "barbell", "machine", "cable", "band", "kettlebell")) loadType.trim().lowercase() else ""
    if (rawHu.contains('+')) return rawHu.split('+').joinToString(" + ") { translateEquipmentLabel(it.trim(), "", lang) }
    return when {
        key == "2 kézisúlyzó" || key == "kézisúlyzó" || loadKey == "per_hand" || loadKey == "dumbbell" -> when (lang) {
            AppLanguage.EN -> "2 dumbbells"
            AppLanguage.DE -> "2 Kurzhanteln"
            AppLanguage.RO -> "2 gantere"
            AppLanguage.HU -> rawHu
        }
        key == "1 kézisúlyzó" || loadKey == "single_dumbbell" -> when (lang) {
            AppLanguage.EN -> "1 dumbbell"
            AppLanguage.DE -> "1 Kurzhantel"
            AppLanguage.RO -> "1 ganteră"
            AppLanguage.HU -> rawHu
        }
        key == "saját testsúly" || loadKey == "bodyweight" -> when (lang) {
            AppLanguage.EN -> "Bodyweight"
            AppLanguage.DE -> "Eigengewicht"
            AppLanguage.RO -> "Greutatea corpului"
            AppLanguage.HU -> rawHu
        }
        key == "kétkezes rúd" || key == "rúd" || loadKey == "barbell" -> when (lang) {
            AppLanguage.EN -> "Barbell"
            AppLanguage.DE -> "Langhantel"
            AppLanguage.RO -> "Haltere / bară"
            AppLanguage.HU -> rawHu
        }
        key == "edzőtermi gép" || key == "gép" || loadKey == "machine" -> when (lang) {
            AppLanguage.EN -> "Gym machine"
            AppLanguage.DE -> "Fitnessgerät"
            AppLanguage.RO -> "Aparat sală"
            AppLanguage.HU -> rawHu
        }
        key == "csigás gép" || key == "csiga" || loadKey == "cable" -> when (lang) {
            AppLanguage.EN -> "Cable machine"
            AppLanguage.DE -> "Kabelzug"
            AppLanguage.RO -> "Aparat cu cablu"
            AppLanguage.HU -> rawHu
        }
        key == "gumiszalag" || loadKey == "band" -> when (lang) {
            AppLanguage.EN -> "Resistance band"
            AppLanguage.DE -> "Widerstandsband"
            AppLanguage.RO -> "Bandă elastică"
            AppLanguage.HU -> rawHu
        }
        key == "kettlebell" || loadKey == "kettlebell" -> "Kettlebell"
        key == "szabadsúly / gép" || loadKey == "total" || loadKey == "weight" -> when (lang) {
            AppLanguage.EN -> "Free weight / Machine"
            AppLanguage.DE -> "Freigewicht / Gerät"
            AppLanguage.RO -> "Greutăți libere / Aparat"
            AppLanguage.HU -> rawHu
        }
        key == "súlyzó / eszköz" -> when (lang) {
            AppLanguage.EN -> "Weights / Equipment"
            AppLanguage.DE -> "Gewichte / Gerät"
            AppLanguage.RO -> "Greutăți / Echipament"
            AppLanguage.HU -> rawHu
        }
        else -> com.example.localization.StudioProfileOptionLabels.label(rawHu, lang)
    }
}

fun translateSingleMuscleLabel(tokenHu: String, lang: AppLanguage): String {
    if (lang == AppLanguage.HU) return tokenHu
    return when (tokenHu.trim().lowercase()) {
        "mell", "mellkas", "chest", "push" -> when (lang) {
            AppLanguage.EN -> "Chest"
            AppLanguage.DE -> "Brust"
            AppLanguage.RO -> "Piept"
            AppLanguage.HU -> tokenHu
        }
        "hát", "back", "lats", "pull" -> when (lang) {
            AppLanguage.EN -> "Back"
            AppLanguage.DE -> "Rücken"
            AppLanguage.RO -> "Spate"
            AppLanguage.HU -> tokenHu
        }
        "hátsó váll" -> when(lang) {
            AppLanguage.EN -> "Rear delts"; AppLanguage.DE -> "Hintere Schultern"; AppLanguage.RO -> "Deltoizi posteriori"; else -> tokenHu
        }
        "lapocka" -> when(lang) {
            AppLanguage.EN -> "Scapular muscles"; AppLanguage.DE -> "Schulterblattmuskeln"; AppLanguage.RO -> "Mușchi scapulari"; else -> tokenHu
        }
        "kondíció" -> when(lang) {
            AppLanguage.EN -> "Conditioning"; AppLanguage.DE -> "Kondition"; AppLanguage.RO -> "Condiționare"; else -> tokenHu
        }
        "váll", "shoulders", "delts" -> when (lang) {
            AppLanguage.EN -> "Shoulders"
            AppLanguage.DE -> "Schultern"
            AppLanguage.RO -> "Umeri"
            AppLanguage.HU -> tokenHu
        }
        "bicepsz", "biceps" -> when (lang) {
            AppLanguage.EN -> "Biceps"
            AppLanguage.DE -> "Bizeps"
            AppLanguage.RO -> "Biceps"
            AppLanguage.HU -> tokenHu
        }
        "tricepsz", "triceps" -> when (lang) {
            AppLanguage.EN -> "Triceps"
            AppLanguage.DE -> "Trizeps"
            AppLanguage.RO -> "Triceps"
            AppLanguage.HU -> tokenHu
        }
        "forearms", "alkar" -> when (lang) {
            AppLanguage.EN -> "Forearms"; AppLanguage.DE -> "Unterarme"; AppLanguage.RO -> "Antebrațe"; else -> tokenHu
        }
        "oldalsó hasizom", "obliques" -> when (lang) {
            AppLanguage.EN -> "Obliques"; AppLanguage.DE -> "Schräge Bauchmuskeln"; AppLanguage.RO -> "Oblici"; else -> tokenHu
        }
        "kar", "arms" -> when (lang) {
            AppLanguage.EN -> "Arms"
            AppLanguage.DE -> "Arme"
            AppLanguage.RO -> "Brațe"
            AppLanguage.HU -> tokenHu
        }
        "láb", "comb", "legs" -> when (lang) {
            AppLanguage.EN -> "Legs"
            AppLanguage.DE -> "Beine"
            AppLanguage.RO -> "Picioare"
            AppLanguage.HU -> tokenHu
        }
        "quad", "quads" -> when (lang) {
            AppLanguage.EN -> "Quads"
            AppLanguage.DE -> "Quadrizeps"
            AppLanguage.RO -> "Cvadriceps"
            AppLanguage.HU -> tokenHu
        }
        "combhajlító", "hamstrings" -> when (lang) {
            AppLanguage.EN -> "Hamstrings"
            AppLanguage.DE -> "Beinbeuger"
            AppLanguage.RO -> "Femurali"
            AppLanguage.HU -> tokenHu
        }
        "farizom", "far", "glutes" -> when (lang) {
            AppLanguage.EN -> "Glutes"
            AppLanguage.DE -> "Gesäß"
            AppLanguage.RO -> "Fesieri"
            AppLanguage.HU -> tokenHu
        }
        "vádli", "calves" -> when (lang) {
            AppLanguage.EN -> "Calves"
            AppLanguage.DE -> "Waden"
            AppLanguage.RO -> "Gambe"
            AppLanguage.HU -> tokenHu
        }
        "törzs / has", "törzs", "has", "hasizom", "core", "abs" -> when (lang) {
            AppLanguage.EN -> "Core / Abs"
            AppLanguage.DE -> "Rumpf / Bauch"
            AppLanguage.RO -> "Trunchi / Abdomen"
            AppLanguage.HU -> tokenHu
        }
        "kardió", "cardio" -> when (lang) {
            AppLanguage.EN -> "Cardio"
            AppLanguage.DE -> "Cardio"
            AppLanguage.RO -> "Cardio"
            AppLanguage.HU -> tokenHu
        }
        "teljes test", "full", "full_body" -> when (lang) {
            AppLanguage.EN -> "Full body"
            AppLanguage.DE -> "Ganzkörper"
            AppLanguage.RO -> "Full body"
            AppLanguage.HU -> tokenHu
        }
        "stabilizáló izmok" -> when (lang) {
            AppLanguage.EN -> "Stabilizer muscles"
            AppLanguage.DE -> "Stützmuskulatur"
            AppLanguage.RO -> "Mușchi stabilizatori"
            AppLanguage.HU -> tokenHu
        }
        else -> tokenHu.trim()
    }
}

fun translateMuscleListLabel(rawHu: String, lang: AppLanguage): String {
    if (lang == AppLanguage.HU || rawHu.isBlank()) return rawHu
    return rawHu.split(Regex("[,/]")).map { it.trim() }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { translateSingleMuscleLabel(it, lang) }.distinct().joinToString(", ")
}

fun translateProgramTag(tagHu: String, lang: AppLanguage): String {
    if (lang == AppLanguage.HU) return tagHu
    return when (tagHu.trim().lowercase()) {
        "otthon" -> when (lang) {
            AppLanguage.EN -> "Home"
            AppLanguage.DE -> "Zuhause"
            AppLanguage.RO -> "Acasă"
            AppLanguage.HU -> tagHu
        }
        "edzőterem", "konditerem" -> when (lang) {
            AppLanguage.EN -> "Gym"
            AppLanguage.DE -> "Fitnessstudio"
            AppLanguage.RO -> "Sală"
            AppLanguage.HU -> tagHu
        }
        "otthon / edzőterem", "vegyes" -> when (lang) {
            AppLanguage.EN -> "Home / Gym"
            AppLanguage.DE -> "Zuhause / Studio"
            AppLanguage.RO -> "Acasă / Sală"
            AppLanguage.HU -> tagHu
        }
        "kültér" -> when (lang) {
            AppLanguage.EN -> "Outdoor"
            AppLanguage.DE -> "Outdoor"
            AppLanguage.RO -> "În aer liber"
            AppLanguage.HU -> tagHu
        }
        "kezdő" -> when (lang) {
            AppLanguage.EN -> "Beginner"
            AppLanguage.DE -> "Anfänger"
            AppLanguage.RO -> "Începător"
            AppLanguage.HU -> tagHu
        }
        "középhaladó" -> when (lang) {
            AppLanguage.EN -> "Intermediate"
            AppLanguage.DE -> "Fortgeschritten"
            AppLanguage.RO -> "Intermediar"
            AppLanguage.HU -> tagHu
        }
        "haladó" -> when (lang) {
            AppLanguage.EN -> "Advanced"
            AppLanguage.DE -> "Profi"
            AppLanguage.RO -> "Avansat"
            AppLanguage.HU -> tagHu
        }
        "egyéni" -> when (lang) {
            AppLanguage.EN -> "Custom"
            AppLanguage.DE -> "Individuell"
            AppLanguage.RO -> "Personalizat"
            AppLanguage.HU -> tagHu
        }
        else -> tagHu
    }
}

fun translateProgramTitle(rawTitle: String, lang: AppLanguage): String {
    if (lang == AppLanguage.HU || rawTitle.isBlank()) return rawTitle
    val trimmed = rawTitle.trim()

    val copySuffixes = listOf(
        " (személyre szabott)" to Triple(" (personalized)", " (personalisiert)", " (personalizat)"),
        " – másolat" to Triple(" – copy", " – Kopie", " – copie"),
        " - másolat" to Triple(" – copy", " – Kopie", " – copie"),
        " (másolat)" to Triple(" (copy)", " (Kopie)", " (copie)"),
        " – copy" to Triple(" – copy", " – Kopie", " – copie"),
        " - copy" to Triple(" – copy", " – Kopie", " – copie"),
        " – Kopie" to Triple(" – copy", " – Kopie", " – copie"),
        " - Kopie" to Triple(" – copy", " – Kopie", " – copie"),
        " – copie" to Triple(" – copy", " – Kopie", " – copie"),
        " - copie" to Triple(" – copy", " – Kopie", " – copie")
    )

    var baseTitle = trimmed
    var localizedSuffix = ""
    for ((suffix, translations) in copySuffixes) {
        if (trimmed.endsWith(suffix, ignoreCase = true)) {
            baseTitle = trimmed.dropLast(suffix.length).trim()
            localizedSuffix = when (lang) {
                AppLanguage.EN -> translations.first
                AppLanguage.DE -> translations.second
                AppLanguage.RO -> translations.third
                AppLanguage.HU -> suffix
            }
            break
        }
    }

    fun normDash(s: String): String = s.replace('–', '-').replace('—', '-').trim()
    val normBase = normDash(baseTitle)

    val catalogProgs = runCatching { com.repforge.app.data.db.NativeBuiltinCatalog.programs.first }.getOrDefault(emptyList())
    catalogProgs.firstOrNull { p ->
        normDash(p.nameHu).equals(normBase, ignoreCase = true) ||
            normDash(p.nameEn).equals(normBase, ignoreCase = true) ||
            normDash(p.nameDe).equals(normBase, ignoreCase = true) ||
            normDash(p.nameRo).equals(normBase, ignoreCase = true) ||
            p.id.equals(normBase, ignoreCase = true)
    }?.let { matched ->
        val translatedBase = when (lang) {
            AppLanguage.HU -> matched.nameHu
            AppLanguage.EN -> matched.nameEn.ifBlank { matched.nameHu }
            AppLanguage.DE -> matched.nameDe.ifBlank { matched.nameEn.ifBlank { matched.nameHu } }
            AppLanguage.RO -> matched.nameRo.ifBlank { matched.nameEn.ifBlank { matched.nameHu } }
        }
        return translatedBase + localizedSuffix
    }

    val personalPlanMatch = Regex("^Saját\\s+(.+?)\\s+terv\\s*\\((.+?)\\)$", RegexOption.IGNORE_CASE).find(baseTitle)
    if (personalPlanMatch != null) {
        val rawGoal = personalPlanMatch.groupValues[1].trim()
        val rawLoc = personalPlanMatch.groupValues[2].trim()
        val goalLabel = when (rawGoal.lowercase()) {
            "erő" -> when (lang) {
                AppLanguage.EN -> "Strength"
                AppLanguage.DE -> "Kraft"
                AppLanguage.RO -> "Forță"
                AppLanguage.HU -> rawGoal
            }
            "fogyás" -> when (lang) {
                AppLanguage.EN -> "Weight loss"
                AppLanguage.DE -> "Abnehmen"
                AppLanguage.RO -> "Slăbire"
                AppLanguage.HU -> rawGoal
            }
            "állóképesség" -> when (lang) {
                AppLanguage.EN -> "Endurance"
                AppLanguage.DE -> "Ausdauer"
                AppLanguage.RO -> "Rezistență"
                AppLanguage.HU -> rawGoal
            }
            else -> com.example.localization.StudioProfileOptionLabels.label(rawGoal, lang)
        }
        val locLabel = translateProgramTag(rawLoc, lang)
        val translatedBase = when (lang) {
            AppLanguage.EN -> "Custom $goalLabel Plan ($locLabel)"
            AppLanguage.DE -> "Eigener $goalLabel-Plan ($locLabel)"
            AppLanguage.RO -> "Plan propriu $goalLabel ($locLabel)"
            AppLanguage.HU -> baseTitle
        }
        return translatedBase + localizedSuffix
    }

    val numberedPlanMatch = Regex("^(?:Edzésterv|Program)\\s*(\\d+)$", RegexOption.IGNORE_CASE).find(baseTitle)
    if (numberedPlanMatch != null) {
        val num = numberedPlanMatch.groupValues[1]
        val translatedBase = when (lang) {
            AppLanguage.EN -> "Training Plan $num"
            AppLanguage.DE -> "Trainingsplan $num"
            AppLanguage.RO -> "Plan de antrenament $num"
            AppLanguage.HU -> baseTitle
        }
        return translatedBase + localizedSuffix
    }

    val translatedBase = when (normBase.lowercase()) {
        "egyedi edzésterv", "saját edzésterv", "saját terv", "egyéni program", "saját program", "custom program", "own" -> when (lang) {
            AppLanguage.EN -> if (normBase.equals("own", ignoreCase = true)) "Own" else "Custom Training Plan"
            AppLanguage.DE -> "Eigener Trainingsplan"
            AppLanguage.RO -> "Plan de antrenament personalizat"
            AppLanguage.HU -> baseTitle
        }
        "4 napos felső / alsó (demo)" -> when (lang) {
            AppLanguage.EN -> "4-Day Upper / Lower (Demo)"
            AppLanguage.DE -> "4-Tage Ober-/Unterkörper (Demo)"
            AppLanguage.RO -> "4 zile Superior / Inferior (Demo)"
            AppLanguage.HU -> baseTitle
        }
        "otthoni 3 napos kézisúlyzós (demo)" -> when (lang) {
            AppLanguage.EN -> "3-Day Home Dumbbell (Demo)"
            AppLanguage.DE -> "3-Tage Kurzhantel Zuhause (Demo)"
            AppLanguage.RO -> "3 zile Gantere Acasă (Demo)"
            AppLanguage.HU -> baseTitle
        }
        else -> baseTitle
    }
    return translatedBase + localizedSuffix
}

fun translateProgramSubtitle(rawSubtitle: String, lang: AppLanguage): String {
    if (lang == AppLanguage.HU || rawSubtitle.isBlank()) return rawSubtitle
    val trimmed = rawSubtitle.trim()

    val locLvlDaysMatch = Regex("^(.+?)\\s*•\\s*(.+?)\\s*•\\s*(\\d+)\\s*(?:edzésnap|workout days|Trainingstage|zile de antrenament)$", RegexOption.IGNORE_CASE).find(trimmed)
    if (locLvlDaysMatch != null) {
        val loc = translateProgramTag(locLvlDaysMatch.groupValues[1].trim(), lang)
        val lvl = translateProgramTag(locLvlDaysMatch.groupValues[2].trim(), lang)
        val days = locLvlDaysMatch.groupValues[3]
        return when (lang) {
            AppLanguage.EN -> "$loc • $lvl • $days workout days"
            AppLanguage.DE -> "$loc • $lvl • $days Trainingstage"
            AppLanguage.RO -> "$loc • $lvl • $days zile de antrenament"
            AppLanguage.HU -> trimmed
        }
    }

    val expDaysMinsMatch = Regex("^(.+?)\\s*•\\s*(\\d+)\\s*nap/hét\\s*•\\s*(\\d+)\\s*perc$", RegexOption.IGNORE_CASE).find(trimmed)
    if (expDaysMinsMatch != null) {
        val exp = translateProgramTag(expDaysMinsMatch.groupValues[1].trim(), lang)
        val days = expDaysMinsMatch.groupValues[2]
        val mins = expDaysMinsMatch.groupValues[3]
        return when (lang) {
            AppLanguage.EN -> "$exp • $days days/week • $mins min"
            AppLanguage.DE -> "$exp • $days Tage/Woche • $mins Min."
            AppLanguage.RO -> "$exp • $days zile/săpt. • $mins min"
            AppLanguage.HU -> trimmed
        }
    }

    return when (trimmed.lowercase()) {
        "saját összeállítású program", "saját egyedi összeállítású edzésterv", "saját program", "egyéni program" -> when (lang) {
            AppLanguage.EN -> "Custom workout program"
            AppLanguage.DE -> "Eigenes Trainingsprogramm"
            AppLanguage.RO -> "Program de antrenament personalizat"
            AppLanguage.HU -> trimmed
        }
        else -> trimmed
    }
}

fun translateProgramDayTitle(rawDayTitle: String, lang: AppLanguage): String {
    if (lang == AppLanguage.HU || rawDayTitle.isBlank()) return rawDayTitle
    val trimmed = rawDayTitle.trim()

    fun normDash(s: String): String = s.replace('–', '-').replace('—', '-').trim()
    val normTrimmed = normDash(trimmed)

    val catalogDays = runCatching { com.repforge.app.data.db.NativeBuiltinCatalog.programs.second }.getOrDefault(emptyList())
    catalogDays.firstOrNull { d ->
        normDash(d.nameHu).equals(normTrimmed, ignoreCase = true) ||
            normDash(d.nameEn).equals(normTrimmed, ignoreCase = true) ||
            normDash(d.nameDe).equals(normTrimmed, ignoreCase = true) ||
            normDash(d.nameRo).equals(normTrimmed, ignoreCase = true)
    }?.let { matched ->
        return when (lang) {
            AppLanguage.HU -> matched.nameHu
            AppLanguage.EN -> matched.nameEn.ifBlank { matched.nameHu }
            AppLanguage.DE -> matched.nameDe.ifBlank { matched.nameEn.ifBlank { matched.nameHu } }
            AppLanguage.RO -> matched.nameRo.ifBlank { matched.nameEn.ifBlank { matched.nameHu } }
        }
    }

    val dayNumMatch = Regex("^(?:Edzésnap|Day|Trainingstag|Ziua de antrenament)\\s*(\\d+)$", RegexOption.IGNORE_CASE).find(trimmed)
    if (dayNumMatch != null) {
        val num = dayNumMatch.groupValues[1]
        return when (lang) {
            AppLanguage.EN -> "Day $num"
            AppLanguage.DE -> "Trainingstag $num"
            AppLanguage.RO -> "Ziua de antrenament $num"
            AppLanguage.HU -> trimmed
        }
    }
    val letterDayMatch = Regex("^([A-Z])\\s*nap$", RegexOption.IGNORE_CASE).find(trimmed)
    if (letterDayMatch != null) {
        val letter = letterDayMatch.groupValues[1].uppercase()
        return when (lang) {
            AppLanguage.EN -> "Day $letter"
            AppLanguage.DE -> "Tag $letter"
            AppLanguage.RO -> "Ziua $letter"
            AppLanguage.HU -> trimmed
        }
    }
    val ordDayMatch = Regex("^(\\d+)\\.?\\s*nap$", RegexOption.IGNORE_CASE).find(trimmed)
    if (ordDayMatch != null) {
        val num = ordDayMatch.groupValues[1]
        return when (lang) {
            AppLanguage.EN -> "Day $num"
            AppLanguage.DE -> "Tag $num"
            AppLanguage.RO -> "Ziua $num"
            AppLanguage.HU -> trimmed
        }
    }
    return when (trimmed) {
        "A nap • Fő fókusz" -> when (lang) {
            AppLanguage.EN -> "Day A • Primary Focus"
            AppLanguage.DE -> "Tag A • Hauptfokus"
            AppLanguage.RO -> "Ziua A • Focus principal"
            AppLanguage.HU -> trimmed
        }
        "B nap • Kiegészítő fókusz" -> when (lang) {
            AppLanguage.EN -> "Day B • Secondary Focus"
            AppLanguage.DE -> "Tag B • Zusatzfokus"
            AppLanguage.RO -> "Ziua B • Focus secundar"
            AppLanguage.HU -> trimmed
        }
        "C nap • Teljes test & Core" -> when (lang) {
            AppLanguage.EN -> "Day C • Full Body & Core"
            AppLanguage.DE -> "Tag C • Ganzkörper & Core"
            AppLanguage.RO -> "Ziua C • Full Body & Core"
            AppLanguage.HU -> trimmed
        }
        "D nap • Felsőtest & Mobilizálás" -> when (lang) {
            AppLanguage.EN -> "Day D • Upper Body & Mobility"
            AppLanguage.DE -> "Tag D • Oberkörper & Mobilität"
            AppLanguage.RO -> "Ziua D • Partea superioară & Mobilitate"
            AppLanguage.HU -> trimmed
        }
        "E nap • Alsótest & Állóképesség" -> when (lang) {
            AppLanguage.EN -> "Day E • Lower Body & Conditioning"
            AppLanguage.DE -> "Tag E • Unterkörper & Ausdauer"
            AppLanguage.RO -> "Ziua E • Partea inferioară & Condiție"
            AppLanguage.HU -> trimmed
        }
        "F nap • Aktív regeneráció" -> when (lang) {
            AppLanguage.EN -> "Day F • Active Recovery"
            AppLanguage.DE -> "Tag F • Aktive Regeneration"
            AppLanguage.RO -> "Ziua F • Recuperare activă"
            AppLanguage.HU -> trimmed
        }
        else -> trimmed
    }
}

data class ProgramDayModel(
    val id: String,
    val dayCode: String, // "A nap", "B nap"
    val titleHu: String,
    val titleEn: String,
    val durationMinutes: Int,
    val exercises: List<ExerciseItem>,
    val titleDe: String = titleEn,
    val titleRo: String = titleEn
) {
    fun localizedTitle(lang: AppLanguage): String {
        if (lang == AppLanguage.HU) return titleHu
        val candidate = when (lang) {
            AppLanguage.HU -> titleHu
            AppLanguage.EN -> titleEn.ifBlank { titleHu }
            AppLanguage.DE -> titleDe.ifBlank { titleEn.ifBlank { titleHu } }
            AppLanguage.RO -> titleRo.ifBlank { titleEn.ifBlank { titleHu } }
        }
        val needsFallback = candidate.isBlank() || candidate == titleHu || (lang != AppLanguage.EN && candidate == titleEn)
        if (!needsFallback) return candidate
        val fromHu = translateProgramDayTitle(titleHu, lang)
        if (fromHu != titleHu) return fromHu
        return translateProgramDayTitle(candidate, lang)
    }

    fun localizedDayCode(lang: AppLanguage): String {
        if (lang == AppLanguage.HU) return dayCode
        val trimmed = dayCode.trim()
        val letterMatch = Regex("^([A-Z])\\s*nap$", RegexOption.IGNORE_CASE).find(trimmed)
        if (letterMatch != null) {
            val letter = letterMatch.groupValues[1].uppercase()
            return when (lang) {
                AppLanguage.EN -> "Day $letter"
                AppLanguage.DE -> "Tag $letter"
                AppLanguage.RO -> "Ziua $letter"
                AppLanguage.HU -> dayCode
            }
        }
        val numMatch = Regex("^(\\d+)\\.?\\s*nap$", RegexOption.IGNORE_CASE).find(trimmed)
        if (numMatch != null) {
            val num = numMatch.groupValues[1]
            return when (lang) {
                AppLanguage.EN -> "Day $num"
                AppLanguage.DE -> "Tag $num"
                AppLanguage.RO -> "Ziua $num"
                AppLanguage.HU -> dayCode
            }
        }
        return dayCode
    }
}

data class TrainingProgramModel(
    val id: String,
    val nameHu: String,
    val nameEn: String,
    val subtitleHu: String,
    val subtitleEn: String,
    val tagsHu: List<String>,
    val isCustom: Boolean,
    val isActive: Boolean,
    val days: List<ProgramDayModel>,
    val nameDe: String = nameEn,
    val nameRo: String = nameEn,
    val subtitleDe: String = subtitleEn,
    val subtitleRo: String = subtitleEn,
    val tagsEn: List<String> = emptyList(),
    val tagsDe: List<String> = emptyList(),
    val tagsRo: List<String> = emptyList()
) {
    fun localizedName(lang: AppLanguage): String {
        if (lang == AppLanguage.HU) return nameHu
        val candidate = when (lang) {
            AppLanguage.HU -> nameHu
            AppLanguage.EN -> nameEn.ifBlank { nameHu }
            AppLanguage.DE -> nameDe.ifBlank { nameEn.ifBlank { nameHu } }
            AppLanguage.RO -> nameRo.ifBlank { nameEn.ifBlank { nameHu } }
        }
        val needsFallback = candidate.isBlank() || candidate == nameHu || (lang != AppLanguage.EN && candidate == nameEn)
        if (!needsFallback) return candidate
        val fromHu = translateProgramTitle(nameHu, lang)
        if (fromHu != nameHu) return fromHu
        return translateProgramTitle(candidate, lang)
    }

    fun localizedSubtitle(lang: AppLanguage): String {
        if (lang == AppLanguage.HU) return subtitleHu
        val candidate = when (lang) {
            AppLanguage.HU -> subtitleHu
            AppLanguage.EN -> subtitleEn.ifBlank { subtitleHu }
            AppLanguage.DE -> subtitleDe.ifBlank { subtitleEn.ifBlank { subtitleHu } }
            AppLanguage.RO -> subtitleRo.ifBlank { subtitleEn.ifBlank { subtitleHu } }
        }
        val needsFallback = candidate.isBlank() || candidate == subtitleHu || (lang != AppLanguage.EN && candidate == subtitleEn)
        if (!needsFallback) return candidate
        val fromHu = translateProgramSubtitle(subtitleHu, lang)
        if (fromHu != subtitleHu) return fromHu
        return translateProgramSubtitle(candidate, lang)
    }

    fun localizedTags(lang: AppLanguage): List<String> = when (lang) {
        AppLanguage.HU -> tagsHu
        AppLanguage.EN -> tagsEn.ifEmpty { tagsHu.map { translateProgramTag(it, AppLanguage.EN) } }
        AppLanguage.DE -> tagsDe.ifEmpty { tagsHu.map { translateProgramTag(it, AppLanguage.DE) } }
        AppLanguage.RO -> tagsRo.ifEmpty { tagsHu.map { translateProgramTag(it, AppLanguage.RO) } }
    }
}

enum class CalendarEventStatus {
    PLANNED,
    COMPLETED,
    MISSED
}

data class CalendarDayEvent(
    val id: String,
    val dayOfMonth: Int,
    val timeLabel: String,
    val titleHu: String,
    val exerciseCount: Int,
    val durationMinutes: Int,
    val status: CalendarEventStatus,
    val splitBadge: String = "A",
    val dateIso: String = ""
) {
    fun localizedTitle(lang: AppLanguage, programs: List<TrainingProgramModel> = emptyList()): String {
        if (lang == AppLanguage.HU) return titleHu
        programs.forEach { prog ->
            prog.days.firstOrNull {
                it.titleHu.equals(titleHu, ignoreCase = true) ||
                    "${prog.nameHu} • ${it.titleHu}".equals(titleHu, ignoreCase = true)
            }?.let { matchedDay ->
                return if (titleHu.contains("•")) {
                    "${prog.localizedName(lang)} • ${matchedDay.localizedTitle(lang)}"
                } else {
                    matchedDay.localizedTitle(lang)
                }
            }
            if (prog.nameHu.equals(titleHu, ignoreCase = true)) {
                return prog.localizedName(lang)
            }
        }
        val dayTranslated = translateProgramDayTitle(titleHu, lang)
        if (dayTranslated != titleHu) return dayTranslated
        val progTranslated = translateProgramTitle(titleHu, lang)
        if (progTranslated != titleHu) return progTranslated
        val firstBullet = titleHu.indexOf('•')
        if (firstBullet > 0) {
            val left = titleHu.substring(0, firstBullet).trim()
            val right = titleHu.substring(firstBullet + 1).trim()
            val pTitle = translateProgramTitle(left, lang)
            val dTitle = translateProgramDayTitle(right, lang)
            if (pTitle != left || dTitle != right) return "$pTitle • $dTitle"
        }
        return when (titleHu.trim()) {
            "Tervezett edzés" -> when (lang) {
                AppLanguage.EN -> "Planned workout"
                AppLanguage.DE -> "Geplantes Training"
                AppLanguage.RO -> "Antrenament planificat"
                AppLanguage.HU -> titleHu
            }
            else -> titleHu
        }
    }
}

data class LoggedWorkoutEntry(
    val id: String,
    val dateLabelHu: String = "",
    val titleHu: String,
    val durationMinutes: Int = 0,
    val workTimeLabel: String = "",
    val restTimeLabel: String = "",
    val totalSets: Int = 0,
    val totalVolumeKg: Int = 0,
    val summaryExercisesHu: String = "",
    val hasPr: Boolean = false,
    val isOutdoorRun: Boolean = false,
    val distanceKm: Float = 0f,
    val paceLabel: String = "",
    val photoCount: Int = 0,
    val sourceWatch: Boolean = false,
    val sourceHealthConnect: Boolean = false,
    val routeKeys: List<String> = emptyList(),
    val exerciseDetails: List<LoggedExerciseDetail> = emptyList(),
    val originDevice: String = "phone",
    val savedStartIso: String = "",
    val savedFinishIso: String = ""
) {
    fun localizedTitle(
        lang: AppLanguage,
        programs: List<TrainingProgramModel> = emptyList(),
        exercises: List<ExerciseItem> = emptyList()
    ): String {
        if (lang == AppLanguage.HU) return titleHu
        val trimmed = titleHu.trim()
        when (trimmed.lowercase()) {
            "edzés" -> return when (lang) {
                AppLanguage.EN -> "Workout"
                AppLanguage.DE -> "Training"
                AppLanguage.RO -> "Antrenament"
                AppLanguage.HU -> titleHu
            }
            "kültéri futás (gps)", "kültéri futás" -> return when (lang) {
                AppLanguage.EN -> "Outdoor Run (GPS)"
                AppLanguage.DE -> "Outdoor-Lauf (GPS)"
                AppLanguage.RO -> "Alergare în aer liber (GPS)"
                AppLanguage.HU -> titleHu
            }
            "szabad edzés" -> return when (lang) {
                AppLanguage.EN -> "Free workout"
                AppLanguage.DE -> "Freies Training"
                AppLanguage.RO -> "Antrenament liber"
                AppLanguage.HU -> titleHu
            }
        }
        programs.forEach { prog ->
            if (prog.nameHu.equals(trimmed, ignoreCase = true)) return prog.localizedName(lang)
            prog.days.forEach { day ->
                if (day.titleHu.equals(trimmed, ignoreCase = true)) return day.localizedTitle(lang)
                if ("${prog.nameHu} • ${day.titleHu}".equals(trimmed, ignoreCase = true)) {
                    return "${prog.localizedName(lang)} • ${day.localizedTitle(lang)}"
                }
            }
        }
        exercises.firstOrNull { it.nameHu.equals(trimmed, ignoreCase = true) }?.let {
            return it.localizedName(lang)
        }
        val dayTranslated = translateProgramDayTitle(trimmed, lang)
        if (dayTranslated != trimmed) return dayTranslated
        val progTranslated = translateProgramTitle(trimmed, lang)
        if (progTranslated != trimmed) return progTranslated
        val firstBullet = trimmed.indexOf('•')
        if (firstBullet > 0) {
            val left = trimmed.substring(0, firstBullet).trim()
            val right = trimmed.substring(firstBullet + 1).trim()
            val pTitle = translateProgramTitle(left, lang)
            val dTitle = translateProgramDayTitle(right, lang)
            if (pTitle != left || dTitle != right) return "$pTitle • $dTitle"
        }
        val catalogEx = runCatching { com.repforge.app.data.db.NativeBuiltinCatalog.exercises }.getOrDefault(emptyList())
        catalogEx.firstOrNull {
            it.nameHu.equals(trimmed, ignoreCase = true) || it.nameEn.equals(trimmed, ignoreCase = true)
        }?.let { ex ->
            return when (lang) {
                AppLanguage.HU -> ex.nameHu
                AppLanguage.EN -> ex.nameEn.ifBlank { ex.nameHu }
                AppLanguage.DE -> ex.nameDe.ifBlank { ex.nameEn.ifBlank { ex.nameHu } }
                AppLanguage.RO -> ex.nameRo.ifBlank { ex.nameEn.ifBlank { ex.nameHu } }
            }
        }
        return titleHu
    }

    fun localizedSummaryExercises(lang: AppLanguage, exercises: List<ExerciseItem> = emptyList()): String {
        if (lang == AppLanguage.HU || summaryExercisesHu.isBlank()) return summaryExercisesHu
        if (summaryExercisesHu == "Rögzített edzés") {
            return when (lang) {
                AppLanguage.EN -> "Recorded workout"
                AppLanguage.DE -> "Aufgezeichnetes Training"
                AppLanguage.RO -> "Antrenament înregistrat"
                AppLanguage.HU -> summaryExercisesHu
            }
        }
        val catalogEx = runCatching { com.repforge.app.data.db.NativeBuiltinCatalog.exercises }.getOrDefault(emptyList())
        val delimiter = if (summaryExercisesHu.contains("•")) " • " else ", "
        val splitRegex = if (summaryExercisesHu.contains("•")) Regex("\\s*•\\s*") else Regex("\\s*,\\s*")
        return summaryExercisesHu.split(splitRegex)
            .map { part ->
                val clean = part.trim()
                exercises.firstOrNull {
                    it.nameHu.equals(clean, ignoreCase = true) ||
                        it.nameEn.equals(clean, ignoreCase = true)
                }?.localizedName(lang)
                    ?: catalogEx.firstOrNull {
                        it.nameHu.equals(clean, ignoreCase = true) ||
                            it.nameEn.equals(clean, ignoreCase = true)
                    }?.let { ex ->
                        when (lang) {
                            AppLanguage.HU -> ex.nameHu
                            AppLanguage.EN -> ex.nameEn.ifBlank { ex.nameHu }
                            AppLanguage.DE -> ex.nameDe.ifBlank { ex.nameEn.ifBlank { ex.nameHu } }
                            AppLanguage.RO -> ex.nameRo.ifBlank { ex.nameEn.ifBlank { ex.nameHu } }
                        }
                    }
                    ?: clean
            }
            .joinToString(delimiter)
    }
}

data class LoggedExerciseDetail(
    val nameHu: String,
    val summaryPrescription: String,
    val sets: List<ExerciseSetItem>,
    val measurementType: String = "",
    val repUnit: String = ""
) {
    fun localizedName(lang: AppLanguage, exercises: List<ExerciseItem> = emptyList()): String {
        if (lang == AppLanguage.HU) return nameHu
        val clean = nameHu.trim()
        exercises.firstOrNull {
            it.nameHu.equals(clean, ignoreCase = true) ||
                it.nameEn.equals(clean, ignoreCase = true)
        }?.let { return it.localizedName(lang) }
        val catalogEx = runCatching { com.repforge.app.data.db.NativeBuiltinCatalog.exercises }.getOrDefault(emptyList())
        catalogEx.firstOrNull {
            it.nameHu.equals(clean, ignoreCase = true) ||
                it.nameEn.equals(clean, ignoreCase = true)
        }?.let { ex ->
            return when (lang) {
                AppLanguage.HU -> ex.nameHu
                AppLanguage.EN -> ex.nameEn.ifBlank { ex.nameHu }
                AppLanguage.DE -> ex.nameDe.ifBlank { ex.nameEn.ifBlank { ex.nameHu } }
                AppLanguage.RO -> ex.nameRo.ifBlank { ex.nameEn.ifBlank { ex.nameHu } }
            }
        }
        return nameHu
    }

    fun localizedSummaryPrescription(lang: AppLanguage): String {
        if (lang == AppLanguage.HU) return summaryPrescription
        val setWord = when (lang) {
            AppLanguage.EN -> "sets"
            AppLanguage.DE -> "Sätze"
            AppLanguage.RO -> "serii"
            AppLanguage.HU -> "sorozat"
        }
        return summaryPrescription.replace("sorozat", setWord)
    }
}

data class PrRecordEntry(
    val id: String,
    val exerciseNameHu: String,
    val typeLabelHu: String,
    val dateLabel: String,
    val previousLabel: String,
    val newLabel: String
) {
    fun localizedExerciseName(lang: AppLanguage, exercises: List<ExerciseItem> = emptyList()): String {
        if (lang == AppLanguage.HU) return exerciseNameHu
        val clean = exerciseNameHu.trim()
        exercises.firstOrNull {
            it.nameHu.equals(clean, ignoreCase = true) ||
                it.nameEn.equals(clean, ignoreCase = true)
        }?.let { return it.localizedName(lang) }
        val catalogEx = runCatching { com.repforge.app.data.db.NativeBuiltinCatalog.exercises }.getOrDefault(emptyList())
        catalogEx.firstOrNull {
            it.nameHu.equals(clean, ignoreCase = true) ||
                it.nameEn.equals(clean, ignoreCase = true)
        }?.let { ex ->
            return when (lang) {
                AppLanguage.HU -> ex.nameHu
                AppLanguage.EN -> ex.nameEn.ifBlank { ex.nameHu }
                AppLanguage.DE -> ex.nameDe.ifBlank { ex.nameEn.ifBlank { ex.nameHu } }
                AppLanguage.RO -> ex.nameRo.ifBlank { ex.nameEn.ifBlank { ex.nameHu } }
            }
        }
        return exerciseNameHu
    }

    fun localizedTypeLabel(lang: AppLanguage): String {
        if (lang == AppLanguage.HU) return typeLabelHu
        if (typeLabelHu == "Idő / ismétlés rekord") {
            return when (lang) {
                AppLanguage.EN -> "Time / rep record"
                AppLanguage.DE -> "Zeit- / Wiederholungsrekord"
                AppLanguage.RO -> "Record timp / repetări"
                AppLanguage.HU -> typeLabelHu
            }
        }
        return typeLabelHu
    }

    fun localizedNewLabel(lang: AppLanguage): String {
        if (lang == AppLanguage.HU) return newLabel
        return when (lang) {
            AppLanguage.EN -> newLabel.replace("ism. / mp", "reps / sec")
            AppLanguage.DE -> newLabel.replace("ism. / mp", "Wdh. / Sek.")
            AppLanguage.RO -> newLabel.replace("ism. / mp", "rep. / sec")
            AppLanguage.HU -> newLabel
        }
    }
}

data class BodyWeightLogItem(
    val id: String,
    val kg: Float,
    val dayKey: String, // YYYY-MM-DD (user-entered measurement date)
    val dateLabelHu: String // YYYY. MM. DD.
)
