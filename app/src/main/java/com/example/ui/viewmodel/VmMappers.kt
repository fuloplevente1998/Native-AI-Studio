package com.example.ui.viewmodel

import com.example.model.AppLanguage
import com.example.model.BodyWeightLogItem
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.LoggedWorkoutEntry
import com.example.model.PrRecordEntry
import com.example.model.TrainingProgramModel
import kotlinx.coroutines.flow.map
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.ProgramDayEntity
import com.repforge.app.data.db.ProgramEntity
import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun TrainPilotViewModel.translateMuscleTokenHu(token: String): String {
    return when (token.trim().lowercase()) {
        "chest", "mell", "mellkas", "push" -> "Mell"
        "back", "hát", "lats", "pull" -> "Hát"
        "shoulders", "váll", "delts" -> "Váll"
        "biceps", "bicepsz" -> "Bicepsz"
        "triceps", "tricepsz" -> "Tricepsz"
        "arms", "kar", "forearms" -> "Kar"
        "legs", "quads", "láb", "comb" -> "Láb"
        "hamstrings", "combhajlító" -> "Combhajlító"
        "glutes", "far", "farizom" -> "Farizom"
        "calves", "vádli" -> "Vádli"
        "core", "abs", "has", "törzs" -> "Törzs / Has"
        "cardio", "kardió" -> "Kardió"
        "full", "full_body", "teljes test" -> "Teljes test"
        else -> token.trim().replaceFirstChar { it.uppercase() }
    }
}

internal fun TrainPilotViewModel.translateLoadTypeOrEquipmentHu(rawEquip: String, loadType: String): String {
    val cleanEquip = rawEquip.trim()
    val technicalTokens = setOf(
        "per_hand", "single_dumbbell", "bodyweight", "total", "weight",
        "dumbbell", "barbell", "machine", "cable", "band", "kettlebell"
    )
    if (cleanEquip.isNotBlank() && cleanEquip.lowercase() !in technicalTokens) {
        return cleanEquip.replaceFirstChar { it.uppercase() }
    }
    return when (cleanEquip.ifBlank { loadType }.trim().lowercase()) {
        "per_hand", "dumbbell" -> "2 kézisúlyzó"
        "single_dumbbell" -> "1 kézisúlyzó"
        "bodyweight" -> "Saját testsúly"
        "barbell" -> "Kétkezes rúd"
        "machine" -> "Edzőtermi gép"
        "cable" -> "Csigás gép"
        "band" -> "Gumiszalag"
        "kettlebell" -> "Kettlebell"
        "total", "weight" -> "Szabadsúly / Gép"
        else -> "Súlyzó / Eszköz"
    }
}

internal fun TrainPilotViewModel.mapExerciseEntity(entity: com.repforge.app.data.db.ExerciseLibraryEntity): ExerciseItem {
    val rawObj = runCatching { JSONObject(entity.rawJson) }.getOrNull()
    val rawEquip = rawObj?.optString("equipment").orEmpty()
    val rawTarget = rawObj?.optString("target").orEmpty()
    val parsedMuscleList = runCatching {
        val arr = JSONArray(entity.muscleGroupsJson)
        (0 until arr.length()).mapNotNull { idx ->
            arr.optString(idx)?.takeIf { it.isNotBlank() }?.let { translateMuscleTokenHu(it) }
        }.distinct()
    }.getOrDefault(emptyList())

    val primaryKey = entity.primaryMuscle.lowercase()
    val cat = when (primaryKey) {
        "chest", "mell", "mellkas", "push" -> ExerciseCategory.CHEST
        "back", "hát", "pull" -> ExerciseCategory.BACK
        "shoulders", "váll" -> ExerciseCategory.SHOULDERS
        "arms", "biceps", "triceps", "bicepsz", "tricepsz", "kar" -> ExerciseCategory.ARMS
        "legs", "quads", "hamstrings", "calves", "glutes", "comb", "láb", "far", "cardio" -> ExerciseCategory.LEGS
        "core", "abs", "has", "törzs" -> ExerciseCategory.CORE
        else -> ExerciseCategory.CHEST
    }

    val cleanPrimaryHu = if (rawTarget.isNotBlank() && !rawTarget.startsWith("[")) {
        rawTarget.replaceFirstChar { it.uppercase() }
    } else {
        parsedMuscleList.firstOrNull() ?: translateMuscleTokenHu(entity.primaryMuscle.ifBlank { "Mell" })
    }

    val secondaryList = parsedMuscleList.filterNot {
        it.equals(cleanPrimaryHu, ignoreCase = true)
    }
    val cleanSecondaryHu = if (secondaryList.isNotEmpty()) {
        secondaryList.joinToString(", ")
    } else if (parsedMuscleList.size > 1) {
        parsedMuscleList.drop(1).joinToString(", ")
    } else {
        "Stabilizáló izmok"
    }

    val cleanEquipmentHu = translateLoadTypeOrEquipmentHu(rawEquip, entity.loadType)
    val parsedReps = Regex("\\d+").findAll(entity.defaultReps).lastOrNull()?.value?.toIntOrNull()
        ?: entity.defaultReps.toIntOrNull()
        ?: 10

    fun parseNotesToSteps(raw: String, fallback: String): List<String> = raw
        .split(Regex("(?<=\\.)\\s+|\\n+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { listOf(fallback) }

    val parsedStepsHu = parseNotesToSteps(
        entity.notesHu,
        "Helyes kivitelezés: ügyelj az egyenes hátra és a kontrollált mozgástartományra."
    )
    val parsedStepsEn = parseNotesToSteps(
        entity.notesEn.ifBlank { entity.notesHu },
        "Proper execution: keep a neutral spine and controlled range of motion."
    )
    val parsedStepsDe = parseNotesToSteps(
        entity.notesDe.ifBlank { entity.notesEn.ifBlank { entity.notesHu } },
        "Saubere Ausführung: gerader Rücken und kontrollierter Bewegungsumfang."
    )
    val parsedStepsRo = parseNotesToSteps(
        entity.notesRo.ifBlank { entity.notesEn.ifBlank { entity.notesHu } },
        "Execuție corectă: menține spatele drept și mișcarea controlată."
    )
    val isTimedFlag = entity.measurementType.contains("time", ignoreCase = true) ||
        entity.repUnit.contains("mp", ignoreCase = true) ||
        entity.repUnit.contains("sec", ignoreCase = true)
    val isBilateralFlag = entity.id.contains("side-plank", ignoreCase = true) ||
        entity.nameHu.contains("oldalsó plank", ignoreCase = true) ||
        entity.measurementType.contains("bilateral", ignoreCase = true) ||
        entity.measurementType.contains("side", ignoreCase = true) ||
        entity.repUnit.contains("oldal", ignoreCase = true)
    val defaultTargetSec = if (isTimedFlag || isBilateralFlag) parsedReps.coerceIn(10, 300) else 45
    return ExerciseItem(
        id = entity.id,
        nameHu = entity.nameHu.ifBlank { entity.nameEn }.ifBlank { "Gyakorlat" },
        nameEn = entity.nameEn.ifBlank { entity.nameHu }.ifBlank { "Exercise" },
        nameDe = entity.nameDe.ifBlank { entity.nameEn.ifBlank { entity.nameHu } },
        nameRo = entity.nameRo.ifBlank { entity.nameEn.ifBlank { entity.nameHu } },
        category = cat,
        equipmentHu = cleanEquipmentHu,
        primaryMusclesHu = cleanPrimaryHu,
        secondaryMusclesHu = cleanSecondaryHu,
        defaultSets = entity.defaultSets.coerceAtLeast(1),
        defaultReps = parsedReps.coerceAtLeast(1),
        defaultWeightKg = entity.defaultWeight.toFloat(),
        defaultRestSec = entity.defaultRestSeconds.coerceAtLeast(30),
        isTimed = isTimedFlag || isBilateralFlag,
        isBilateral = isBilateralFlag,
        targetSeconds = defaultTargetSec,
        stepsHu = parsedStepsHu,
        stepsEn = parsedStepsEn,
        stepsDe = parsedStepsDe,
        stepsRo = parsedStepsRo,
        coachStatusHu = "Normál terhelés",
        coachHintHu = parsedStepsHu.firstOrNull() ?: "Kezdj könnyebben; ma ne erőltesd a progressziót.",
        videoUrl = entity.videoUrl.orEmpty(),
        isCustom = entity.custom,
        loadType = entity.loadType, repUnit = entity.repUnit, measurementType = entity.measurementType,
        targetRepsText = entity.defaultReps
    )
}

internal fun TrainPilotViewModel.computePrRecordsFromWorkouts(workouts: List<LoggedWorkoutEntry>): List<PrRecordEntry> {
    data class BestEntry(val weightKg: Float, val reps: Int, val e1Rm: Float, val dateLabelHu: String)
    val bestByExercise = linkedMapOf<String, BestEntry>()
    for (w in workouts) {
        for (exDetail in w.exerciseDetails) {
            for (s in exDetail.sets) {
                if (!s.completed) continue
                val score = if (s.weightKg > 0f) s.weightKg * (1f + s.reps / 30f) else s.reps.toFloat()
                val prev = bestByExercise[exDetail.nameHu]
                if (prev == null || score > prev.e1Rm || (score == prev.e1Rm && s.weightKg > prev.weightKg)) {
                    bestByExercise[exDetail.nameHu] = BestEntry(
                        weightKg = s.weightKg,
                        reps = s.reps,
                        e1Rm = score,
                        dateLabelHu = w.dateLabelHu
                    )
                }
            }
        }
    }
    return bestByExercise.entries
        .sortedByDescending { it.value.e1Rm }
        .mapIndexed { idx, (exName, best) ->
            val valueStr = if (best.weightKg > 0f) {
                "${best.weightKg.toString().removeSuffix(".0")} kg × ${best.reps}"
            } else {
                "${best.reps} ism. / mp"
            }
            val e1RmLabel = if (best.weightKg > 0f) "1RM ≈ ${"%.1f".format(best.e1Rm).replace('.', ',')} kg" else "Idő / ismétlés rekord"
            PrRecordEntry(
                id = "pr_${idx}_${exName.hashCode()}",
                exerciseNameHu = exName,
                typeLabelHu = e1RmLabel,
                dateLabel = best.dateLabelHu,
                previousLabel = "—",
                newLabel = valueStr
            )
        }
}

internal fun TrainPilotViewModel.mapBodyWeightEntitiesToLogItems(entities: List<BodyWeightEntryEntity>): List<BodyWeightLogItem> {
    val dotFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd.")
    return entities
        .sortedWith(compareBy<BodyWeightEntryEntity> { it.dayKey.ifBlank { it.date.take(10) } }.thenBy { it.updatedAt })
        .map { e ->
            val cleanDayKey = e.dayKey.ifBlank { e.date.take(10) }
            val parsedDate = runCatching { LocalDate.parse(cleanDayKey.take(10)) }.getOrElse { LocalDate.now() }
            val photoCount = runCatching {
                val raw = JSONObject(e.rawJson)
                val photos = raw.optJSONArray("photos")
                if (photos == null) 0 else (0 until photos.length()).count { index ->
                    val photo = photos.optJSONObject(index)
                    val id = photo?.optString("id").orEmpty()
                    id.isNotBlank() && !photo.optBoolean("deleted", false)
                }
            }.getOrDefault(0)
            BodyWeightLogItem(
                id = e.id,
                kg = e.kg.toFloat(),
                dayKey = parsedDate.toString(),
                dateLabelHu = parsedDate.format(dotFormatter),
                photoCount = photoCount
            )
        }
}

internal fun TrainPilotViewModel.mapCatalogPrograms(exercisesMap: Map<String, ExerciseItem>): List<TrainingProgramModel> {
    val (programs, days) = TrainPilotDatabase.defaultPrograms()
    return mapProgramRows(programs, days, exercisesMap)
}

internal fun TrainPilotViewModel.mapProgramRows(programs: List<ProgramEntity>, days: List<ProgramDayEntity>,
    exercisesMap: Map<String, ExerciseItem>, activeId: String? = null): List<TrainingProgramModel> {
    val explicitActive = programs.any { runCatching { JSONObject(it.rawJson).optBoolean("isActive") }.getOrDefault(false) }
    val (builtinProgs, builtinDays) = runCatching { TrainPilotDatabase.defaultPrograms() }.getOrElse { emptyList<ProgramEntity>() to emptyList<ProgramDayEntity>() }
    val builtinProgById = builtinProgs.associateBy { it.id }
    val builtinDayByComposite = builtinDays.associateBy { it.compositeId }
    return programs.mapIndexed { pIdx, prog ->
            val catalogProg = if (prog.builtin) builtinProgById[prog.id] else null
            val strippedNameHu = prog.nameHu.trim()
                .removeSuffix(" (személyre szabott)")
                .removeSuffix(" – másolat")
                .removeSuffix(" - másolat")
                .removeSuffix(" (másolat)")
                .trim()
            val homeBasicAliases = setOf(
                "otthoni a/b – alap", "otthoni a/b - alap", "otthoni alap a/b",
                "home a/b – basic", "home a/b - basic", "home a/b – core",
                "zuhause a/b – basis", "zuhause a/b - basis",
                "acasă a/b – bază", "acasă a/b - bază"
            )
            val copiedTemplate = if (!prog.builtin) {
                prog.sourceTemplateId?.let { builtinProgById[it] }
                    ?: builtinProgs.firstOrNull {
                        it.nameHu.equals(strippedNameHu, ignoreCase = true) ||
                            it.nameEn.equals(strippedNameHu, ignoreCase = true) ||
                            it.nameDe.equals(strippedNameHu, ignoreCase = true) ||
                            it.nameRo.equals(strippedNameHu, ignoreCase = true) ||
                            (it.id == "home-basic" && strippedNameHu.lowercase() in homeBasicAliases)
                    }
            } else null
            val localizedTemplate = catalogProg ?: copiedTemplate
            val progDays = days.filter { it.programId == prog.id }.sortedWith(compareBy<ProgramDayEntity> { it.sortOrder }.thenBy { it.dayId }).mapIndexed { dIdx, dayEntity ->
                val catalogDay = if (prog.builtin) builtinDayByComposite[dayEntity.compositeId]
                    else localizedTemplate?.let { tpl ->
                        builtinDayByComposite["${tpl.id}:${dayEntity.dayId}"]
                            ?: builtinDays.firstOrNull { it.programId == tpl.id && it.nameHu.equals(dayEntity.nameHu, ignoreCase = true) }
                    } ?: builtinDays.firstOrNull { it.nameHu.equals(dayEntity.nameHu, ignoreCase = true) }
                val dayExercises = runCatching {
                    val recipes = JSONObject(prog.rawJson).optJSONObject("prescriptions") ?: JSONObject()
                    val arr = JSONArray(dayEntity.exercisesJson)
                    (0 until arr.length()).mapNotNull { i ->
                        val item = arr.get(i)
                        val id = if (item is JSONObject) item.optString("id") else item.toString()
                        val source = exercisesMap[id] ?: return@mapNotNull null
                        val rx = JSONObject(recipes.optJSONObject(id)?.toString() ?: "{}")
                        if (item is JSONObject) item.keys().forEach { key -> if (key != "id") rx.put(key, item.get(key)) }
                        com.repforge.app.NativeProgramPrescription.applyBuiltinTrial(prog.builtin, rx, source.loadType)
                        val target = rx.optString("reps", source.targetRepsText.ifBlank { source.defaultReps.toString() })
                        source.copy(defaultSets = rx.optInt("sets", source.defaultSets),
                            defaultReps = Regex("[0-9]+").find(target)?.value?.toIntOrNull() ?: source.defaultReps,
                            targetRepsText = target,
                            defaultWeightKg = rx.optDouble("weight", rx.optDouble("weightKg", source.defaultWeightKg.toDouble())).toFloat(),
                            defaultRestSec = rx.optInt("rest", rx.optInt("restSec", source.defaultRestSec)))
                    }
                }.getOrElse { emptyList() }
                val resolvedDayHu = dayEntity.nameHu.ifBlank { catalogDay?.nameHu.orEmpty() }.ifBlank { "Edzésnap ${dIdx + 1}" }
                val rawDayEn = (catalogDay?.nameEn?.takeIf { dayEntity.nameHu == catalogDay.nameHu && (dayEntity.nameEn.isBlank() || dayEntity.nameEn == dayEntity.nameHu) }
                    ?: dayEntity.nameEn.takeIf { it.isNotBlank() && it != dayEntity.nameHu })
                    .orEmpty()
                val resolvedDayEn = rawDayEn.ifBlank { com.example.model.translateProgramDayTitle(resolvedDayHu, AppLanguage.EN) }
                val rawDayDe = (catalogDay?.nameDe?.takeIf { dayEntity.nameHu == catalogDay.nameHu && (dayEntity.nameDe.isBlank() || dayEntity.nameDe == dayEntity.nameHu || dayEntity.nameDe == dayEntity.nameEn) }
                    ?: dayEntity.nameDe.takeIf { it.isNotBlank() && it != dayEntity.nameHu && it != dayEntity.nameEn })
                    .orEmpty()
                val resolvedDayDe = rawDayDe.ifBlank { com.example.model.translateProgramDayTitle(resolvedDayHu, AppLanguage.DE) }
                val rawDayRo = (catalogDay?.nameRo?.takeIf { dayEntity.nameHu == catalogDay.nameHu && (dayEntity.nameRo.isBlank() || dayEntity.nameRo == dayEntity.nameHu || dayEntity.nameRo == dayEntity.nameEn) }
                    ?: dayEntity.nameRo.takeIf { it.isNotBlank() && it != dayEntity.nameHu && it != dayEntity.nameEn })
                    .orEmpty()
                val resolvedDayRo = rawDayRo.ifBlank { com.example.model.translateProgramDayTitle(resolvedDayHu, AppLanguage.RO) }
                com.example.model.ProgramDayModel(
                    id = dayEntity.compositeId,
                    dayCode = if (dIdx < 26) "${'A' + dIdx} nap" else "${dIdx + 1}. nap",
                    titleHu = resolvedDayHu,
                    titleEn = resolvedDayEn,
                    durationMinutes = 60,
                    exercises = dayExercises,
                    titleDe = resolvedDayDe,
                    titleRo = resolvedDayRo
                )
            }
            val (locHu, lvlHu) = com.example.model.resolveProgramLocationLevel(
                builtin = prog.builtin,
                storedLocation = prog.location,
                storedLevel = prog.level,
                templateLocation = localizedTemplate?.location,
                templateLevel = localizedTemplate?.level
            )
            val metaSource = localizedTemplate ?: prog
            val locEn = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "location", "en")
                else com.example.model.translateProgramTag(locHu, AppLanguage.EN)
            val locDe = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "location", "de")
                else com.example.model.translateProgramTag(locHu, AppLanguage.DE)
            val locRo = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "location", "ro")
                else com.example.model.translateProgramTag(locHu, AppLanguage.RO)
            val lvlEn = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "level", "en")
                else com.example.model.translateProgramTag(lvlHu, AppLanguage.EN)
            val lvlDe = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "level", "de")
                else com.example.model.translateProgramTag(lvlHu, AppLanguage.DE)
            val lvlRo = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "level", "ro")
                else com.example.model.translateProgramTag(lvlHu, AppLanguage.RO)

            val isAutoGeneratedDesc = prog.description.isBlank() ||
                prog.description == localizedTemplate?.description ||
                Regex("^.+?\\s*•\\s*.+?\\s*•\\s*\\d+\\s*(?:edzésnap|workout days|Trainingstage|zile de antrenament)$", RegexOption.IGNORE_CASE).matches(prog.description.trim())
            val customUserDesc = prog.description.takeIf {
                !prog.builtin && !isAutoGeneratedDesc
            }
            val builtSubtitleHu = customUserDesc ?: "$locHu • $lvlHu • ${progDays.size} edzésnap"
            val builtSubtitleEn = customUserDesc?.let { com.example.model.translateProgramSubtitle(it, AppLanguage.EN) }
                ?: "$locEn • $lvlEn • ${progDays.size} workout days"
            val builtSubtitleDe = customUserDesc?.let { com.example.model.translateProgramSubtitle(it, AppLanguage.DE) }
                ?: "$locDe • $lvlDe • ${progDays.size} Trainingstage"
            val builtSubtitleRo = customUserDesc?.let { com.example.model.translateProgramSubtitle(it, AppLanguage.RO) }
                ?: "$locRo • $lvlRo • ${progDays.size} zile de antrenament"

            val effectiveProgNameHu = if (prog.builtin && prog.id == "home-basic" && prog.nameHu.trim().lowercase() in homeBasicAliases) {
                catalogProg?.nameHu ?: "Otthoni A/B"
            } else {
                prog.nameHu
            }
            val resolvedNameHu = effectiveProgNameHu.ifBlank { catalogProg?.nameHu.orEmpty() }.ifBlank { prog.nameEn }.ifBlank { "Edzésterv ${pIdx + 1}" }
            val rawNameEn = (localizedTemplate?.nameEn?.takeIf { effectiveProgNameHu == localizedTemplate.nameHu && (prog.nameEn.isBlank() || prog.nameEn == prog.nameHu || prog.nameEn.trim().lowercase() in homeBasicAliases) }
                ?: prog.nameEn.takeIf { it.isNotBlank() && it != prog.nameHu && !(prog.builtin && prog.id == "home-basic" && it.trim().lowercase() in homeBasicAliases) })
                .orEmpty()
            val resolvedNameEn = rawNameEn.ifBlank { com.example.model.translateProgramTitle(resolvedNameHu, AppLanguage.EN) }
            val rawNameDe = (localizedTemplate?.nameDe?.takeIf { effectiveProgNameHu == localizedTemplate.nameHu && (prog.nameDe.isBlank() || prog.nameDe == prog.nameHu || prog.nameDe == prog.nameEn || prog.nameDe.trim().lowercase() in homeBasicAliases) }
                ?: prog.nameDe.takeIf { it.isNotBlank() && it != prog.nameHu && it != prog.nameEn && !(prog.builtin && prog.id == "home-basic" && it.trim().lowercase() in homeBasicAliases) })
                .orEmpty()
            val resolvedNameDe = rawNameDe.ifBlank { com.example.model.translateProgramTitle(resolvedNameHu, AppLanguage.DE) }
            val rawNameRo = (localizedTemplate?.nameRo?.takeIf { effectiveProgNameHu == localizedTemplate.nameHu && (prog.nameRo.isBlank() || prog.nameRo == prog.nameHu || prog.nameRo == prog.nameEn || prog.nameRo.trim().lowercase() in homeBasicAliases) }
                ?: prog.nameRo.takeIf { it.isNotBlank() && it != prog.nameHu && it != prog.nameEn && !(prog.builtin && prog.id == "home-basic" && it.trim().lowercase() in homeBasicAliases) })
                .orEmpty()
            val resolvedNameRo = rawNameRo.ifBlank { com.example.model.translateProgramTitle(resolvedNameHu, AppLanguage.RO) }

            TrainingProgramModel(
                id = prog.id,
                nameHu = resolvedNameHu,
                nameEn = resolvedNameEn,
                subtitleHu = builtSubtitleHu,
                subtitleEn = builtSubtitleEn,
                tagsHu = listOf(locHu, lvlHu),
                isCustom = !prog.builtin,
                isActive = if (activeId != null && programs.any { it.id == activeId }) prog.id == activeId
                    else if (explicitActive) JSONObject(prog.rawJson).optBoolean("isActive") else pIdx == 0,
                days = progDays,
                nameDe = resolvedNameDe,
                nameRo = resolvedNameRo,
                subtitleDe = builtSubtitleDe,
                subtitleRo = builtSubtitleRo,
                tagsEn = listOf(locEn, lvlEn),
                tagsDe = listOf(locDe, lvlDe),
                tagsRo = listOf(locRo, lvlRo)
            )
        }
}
