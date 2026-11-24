package com.example.localization

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.model.AppLanguage

val LocalTrainPilotStrings = staticCompositionLocalOf {
    TrainPilotStrings.forLanguage(AppLanguage.HU)
}

data class LocalizedStrings(
    val language: AppLanguage = AppLanguage.HU,
    val navHome: String,
    val navWorkout: String,
    val navLog: String,
    val navProgress: String,
    val navHealth: String,
    val greetingTitle: String,
    val greetingPausedSub: String,
    val greetingPlannedSub: String,
    val greetingLightSub: String,
    val pausedBadge: String,
    val continueBtn: String,
    val startPlannedBtn: String,
    val quickWorkoutTitle: String,
    val quickWorkoutSub: String,
    val watchConnectedSub: String,
    val sleepTitle: String,
    val sleepQualityGood: String,
    val restingHrTitle: String,
    val restingHrNormal: String,
    val stepsTitle: String,
    val stepsGoalPrefix: String,
    val samsungEnergyTitle: String,
    val energyGoodState: String,
    val sourceSamsungHealth: String,
    val personalPlannerTitle: String,
    val personalPlannerSub: String,
    val profileAndNewPlanBtn: String,
    val activeProgramBadge: String,
    val nextWorkoutLabel: String,
    val todayStateTitle: String,
    val activeCaloriesTitle: String,
    val todayWorkoutTitle: String,
    val regenerationTitle: String,
    val goalsSetupTitle: String,
    val coachSuggestionHeader: String,
    val coachLightDayTitle: String,
    val coachLightDayBody: String,
    val coachNormalDayTitle: String,
    val coachNormalDayBody: String,
    val readinessTodayLabel: String,
    val demoModeLabel: String,
    val settingsTitle: String,
    val themesTitle: String,
    val languageTitle: String,
    val restTimerTitle: String,
    val soundAlertTitle: String,
    val connectedDevicesTitle: String,
    val backupRestoreTitle: String,
    val privacyPermissionsTitle: String,
    val saveBtn: String,
    val cancelBtn: String,
    val backBtn: String
) {
    val saveAction: String get() = saveBtn
    val cancelAction: String get() = cancelBtn
    val appearanceAndLang: String get() = themesTitle
    val themes32Title: String get() = themesTitle
    val watchAndSyncTitle: String get() = connectedDevicesTitle
    val permissionsTitle: String get() = privacyPermissionsTitle

    val privatePhotosTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Privát edzésfotók"
            AppLanguage.EN -> "Private Workout Photos"
            AppLanguage.DE -> "Private Trainingsfotos"
            AppLanguage.RO -> "Fotografii private"
        }

    val logSearchHint: String
        get() = when (language) {
            AppLanguage.HU -> "Edzések keresése..."
            AppLanguage.EN -> "Search workouts..."
            AppLanguage.DE -> "Trainings suchen..."
            AppLanguage.RO -> "Caută antrenamente..."
        }

    val workoutDetailTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Edzés részletei"
            AppLanguage.EN -> "Workout Details"
            AppLanguage.DE -> "Trainingsdetails"
            AppLanguage.RO -> "Detalii antrenament"
        }

    val routeMapTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Edzésútvonal"
            AppLanguage.EN -> "Workout Route"
            AppLanguage.DE -> "Trainingsroute"
            AppLanguage.RO -> "Traseu antrenament"
        }

    // Accessibility & common controls
    val closeAction: String
        get() = when (language) {
            AppLanguage.HU -> "Bezárás"
            AppLanguage.EN -> "Close"
            AppLanguage.DE -> "Schließen"
            AppLanguage.RO -> "Închide"
        }

    val decreaseValueDesc: String
        get() = when (language) {
            AppLanguage.HU -> "Érték csökkentése"
            AppLanguage.EN -> "Decrease value"
            AppLanguage.DE -> "Wert verringern"
            AppLanguage.RO -> "Scade valoarea"
        }

    val increaseValueDesc: String
        get() = when (language) {
            AppLanguage.HU -> "Érték növelése"
            AppLanguage.EN -> "Increase value"
            AppLanguage.DE -> "Wert erhöhen"
            AppLanguage.RO -> "Crește valoarea"
        }

    val zoomInDesc: String
        get() = when (language) {
            AppLanguage.HU -> "Nagyítás"
            AppLanguage.EN -> "Zoom in"
            AppLanguage.DE -> "Vergrößern"
            AppLanguage.RO -> "Mărește"
        }

    val zoomOutDesc: String
        get() = when (language) {
            AppLanguage.HU -> "Kicsinyítés"
            AppLanguage.EN -> "Zoom out"
            AppLanguage.DE -> "Verkleinern"
            AppLanguage.RO -> "Micșorează"
        }

    val recenterMapDesc: String
        get() = when (language) {
            AppLanguage.HU -> "Térkép középre igazítása"
            AppLanguage.EN -> "Recenter map"
            AppLanguage.DE -> "Karte zentrieren"
            AppLanguage.RO -> "Recentrează harta"
        }

    val videoLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Videó"
            AppLanguage.EN -> "Video"
            AppLanguage.DE -> "Video"
            AppLanguage.RO -> "Video"
        }

    val fontSizePreviewLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Betűméret előnézet (Kisegítő lehetőségek)"
            AppLanguage.EN -> "Font Scale Preview (Accessibility)"
            AppLanguage.DE -> "Schriftgröße Vorschau (Barrierefreiheit)"
            AppLanguage.RO -> "Previzualizare dimensiune text"
        }

    val colorPalettes32Title: String
        get() = when (language) {
            AppLanguage.HU -> "32 Színpaletta & Betűméret"
            AppLanguage.EN -> "32 Color Palettes & Font Size"
            AppLanguage.DE -> "32 Farbpaletten & Schriftgröße"
            AppLanguage.RO -> "32 Palete de culori și text"
        }

    val basicPalettesHeader: String
        get() = when (language) {
            AppLanguage.HU -> "ALAP PALETTÁK (16)"
            AppLanguage.EN -> "BASIC PALETTES (16)"
            AppLanguage.DE -> "BASIS-PALETTEN (16)"
            AppLanguage.RO -> "PALETE DE BAZĂ (16)"
        }

    val vividPalettesHeader: String
        get() = when (language) {
            AppLanguage.HU -> "ÉLÉNK PALETTÁK (16)"
            AppLanguage.EN -> "VIVID PALETTES (16)"
            AppLanguage.DE -> "LEBENDIGE PALETTEN (16)"
            AppLanguage.RO -> "PALETE VII (16)"
        }

    // Home screen functional strings
    fun activeCaloriesAndWorkoutsToday(kcal: Int, workouts: Int): String = when (language) {
        AppLanguage.HU -> "$kcal aktív kcal • $workouts edzés ma"
        AppLanguage.EN -> "$kcal active kcal • $workouts workouts today"
        AppLanguage.DE -> "$kcal aktive kcal • $workouts Trainings heute"
        AppLanguage.RO -> "$kcal kcal active • $workouts antrenamente azi"
    }

    fun connectedWithBattery(battery: Int): String = when (language) {
        AppLanguage.HU -> "Csatlakoztatva • $battery%"
        AppLanguage.EN -> "Connected • $battery%"
        AppLanguage.DE -> "Verbunden • $battery%"
        AppLanguage.RO -> "Conectat • $battery%"
    }

    fun stepsGoalWithCount(stepsGoal: Int): String = when (language) {
        AppLanguage.HU -> "Cél: $stepsGoal"
        AppLanguage.EN -> "Goal: $stepsGoal"
        AppLanguage.DE -> "Ziel: $stepsGoal"
        AppLanguage.RO -> "Țintă: $stepsGoal"
    }

    fun nextWorkoutWithDay(dayLabel: String): String = when (language) {
        AppLanguage.HU -> "$nextWorkoutLabel: $dayLabel"
        AppLanguage.EN -> "$nextWorkoutLabel: $dayLabel"
        AppLanguage.DE -> "$nextWorkoutLabel: $dayLabel"
        AppLanguage.RO -> "$nextWorkoutLabel: $dayLabel"
    }

    fun weeklyStepsFormat(workouts: Int, steps: Int): String = when (language) {
        AppLanguage.HU -> "Heti $workouts edzés • Napi $steps lépés"
        AppLanguage.EN -> "$workouts workouts/wk • $steps steps/day"
        AppLanguage.DE -> "$workouts Trainings/Wo. • $steps Schritte/Tag"
        AppLanguage.RO -> "$workouts antr./săpt. • $steps pași/zi"
    }

    val programsShortcutTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Programok"
            AppLanguage.EN -> "Programs"
            AppLanguage.DE -> "Programme"
            AppLanguage.RO -> "Programe"
        }

    val programsShortcutSub: String
        get() = when (language) {
            AppLanguage.HU -> "Tervek és sablonok"
            AppLanguage.EN -> "Plans & templates"
            AppLanguage.DE -> "Pläne & Vorlagen"
            AppLanguage.RO -> "Planuri și șabloane"
        }

    val calendarShortcutTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Naptár"
            AppLanguage.EN -> "Calendar"
            AppLanguage.DE -> "Kalender"
            AppLanguage.RO -> "Calendar"
        }

    val calendarShortcutSub: String
        get() = when (language) {
            AppLanguage.HU -> "Heti beosztás"
            AppLanguage.EN -> "Weekly schedule"
            AppLanguage.DE -> "Wochenplan"
            AppLanguage.RO -> "Program săptămânal"
        }

    val coachShortcutTitle: String
        get() = "Coach"

    val coachShortcutSub: String
        get() = when (language) {
            AppLanguage.HU -> "Terhelés és tanácsok"
            AppLanguage.EN -> "Load & readiness"
            AppLanguage.DE -> "Belastung & Rat"
            AppLanguage.RO -> "Sarcină și sfaturi"
        }

    // Workout & Planning screen functional strings
    val exercisePickerTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Gyakorlatválasztó"
            AppLanguage.EN -> "Exercise Library"
            AppLanguage.DE -> "Übungsauswahl"
            AppLanguage.RO -> "Bibliotecă exerciții"
        }

    val exercisePickerSub: String
        get() = when (language) {
            AppLanguage.HU -> "Keresés, szűrés, csere"
            AppLanguage.EN -> "Search, filter, swap"
            AppLanguage.DE -> "Suchen, filtern, tauschen"
            AppLanguage.RO -> "Căutare, filtrare, schimb"
        }

    val outdoorWorkoutTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Kültéri edzés"
            AppLanguage.EN -> "Outdoor Workout"
            AppLanguage.DE -> "Outdoor-Training"
            AppLanguage.RO -> "Antrenament exterior"
        }

    val outdoorWorkoutSub: String
        get() = when (language) {
            AppLanguage.HU -> "Futás, séta, kerékpár GPS-szel"
            AppLanguage.EN -> "Run, walk, cycle with GPS"
            AppLanguage.DE -> "Laufen, Gehen, Radfahren mit GPS"
            AppLanguage.RO -> "Alergare, mers, ciclism cu GPS"
        }

    val currentProgramTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Jelenlegi program"
            AppLanguage.EN -> "Current Program"
            AppLanguage.DE -> "Aktuelles Programm"
            AppLanguage.RO -> "Program curent"
        }

    val viewAllAction: String
        get() = when (language) {
            AppLanguage.HU -> "Összes"
            AppLanguage.EN -> "All"
            AppLanguage.DE -> "Alle"
            AppLanguage.RO -> "Toate"
        }

    val startAction: String
        get() = when (language) {
            AppLanguage.HU -> "Indítás"
            AppLanguage.EN -> "Start"
            AppLanguage.DE -> "Starten"
            AppLanguage.RO -> "Start"
        }

    val activeBadgeLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Aktív"
            AppLanguage.EN -> "Active"
            AppLanguage.DE -> "Aktiv"
            AppLanguage.RO -> "Activ"
        }

    val activateAction: String
        get() = when (language) {
            AppLanguage.HU -> "Aktiválás"
            AppLanguage.EN -> "Activate"
            AppLanguage.DE -> "Aktivieren"
            AppLanguage.RO -> "Activează"
        }

    val myProgramsTab: String
        get() = when (language) {
            AppLanguage.HU -> "Saját programok"
            AppLanguage.EN -> "My Programs"
            AppLanguage.DE -> "Meine Programme"
            AppLanguage.RO -> "Programele mele"
        }

    val communityProgramsTab: String
        get() = when (language) {
            AppLanguage.HU -> "Közösségi / alap"
            AppLanguage.EN -> "Community / Base"
            AppLanguage.DE -> "Community / Basis"
            AppLanguage.RO -> "Comunitate / Bază"
        }

    val moreProgramsTitle: String
        get() = when (language) {
            AppLanguage.HU -> "További programok"
            AppLanguage.EN -> "More Programs"
            AppLanguage.DE -> "Weitere Programme"
            AppLanguage.RO -> "Alte programe"
        }

    val createOrEditProgramAction: String
        get() = when (language) {
            AppLanguage.HU -> "+ Új program létrehozása / Szerkesztő"
            AppLanguage.EN -> "+ Create New Program / Editor"
            AppLanguage.DE -> "+ Neues Programm / Editor"
            AppLanguage.RO -> "+ Program nou / Editor"
        }

    val editProgramTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Program szerkesztése"
            AppLanguage.EN -> "Edit Program"
            AppLanguage.DE -> "Programm bearbeiten"
            AppLanguage.RO -> "Editează programul"
        }

    val detailsAction: String
        get() = when (language) {
            AppLanguage.HU -> "Részletek"
            AppLanguage.EN -> "Details"
            AppLanguage.DE -> "Details"
            AppLanguage.RO -> "Detalii"
        }

    val setsLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Sorozat"
            AppLanguage.EN -> "Sets"
            AppLanguage.DE -> "Sätze"
            AppLanguage.RO -> "Serii"
        }

    val repsLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Ismétlés"
            AppLanguage.EN -> "Reps"
            AppLanguage.DE -> "Wdh."
            AppLanguage.RO -> "Repetări"
        }

    val weightLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Súly"
            AppLanguage.EN -> "Weight"
            AppLanguage.DE -> "Gewicht"
            AppLanguage.RO -> "Greutate"
        }

    val restLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Pihenő"
            AppLanguage.EN -> "Rest"
            AppLanguage.DE -> "Pause"
            AppLanguage.RO -> "Pauză"
        }

    val addExerciseAction: String
        get() = when (language) {
            AppLanguage.HU -> "+ Gyakorlat hozzáadása"
            AppLanguage.EN -> "+ Add Exercise"
            AppLanguage.DE -> "+ Übung hinzufügen"
            AppLanguage.RO -> "+ Adaugă exercițiu"
        }

    val deleteProgramDemoAction: String
        get() = when (language) {
            AppLanguage.HU -> "Program törlése (DEMO)"
            AppLanguage.EN -> "Delete Program (DEMO)"
            AppLanguage.DE -> "Programm löschen (DEMO)"
            AppLanguage.RO -> "Șterge programul (DEMO)"
        }

    val replanActionLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Újratervezés"
            AppLanguage.EN -> "Reschedule"
            AppLanguage.DE -> "Umplanen"
            AppLanguage.RO -> "Replanificare"
        }

    val executionTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Végrehajtás"
            AppLanguage.EN -> "Execution"
            AppLanguage.DE -> "Ausführung"
            AppLanguage.RO -> "Execuție"
        }

    val addToWorkoutAction: String
        get() = when (language) {
            AppLanguage.HU -> "Hozzáadás az edzéshez"
            AppLanguage.EN -> "Add to Workout"
            AppLanguage.DE -> "Zum Training hinzufügen"
            AppLanguage.RO -> "Adaugă la antrenament"
        }

    // Active Measurement functional strings
    val modeStrengthLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Súlyzós"
            AppLanguage.EN -> "Strength"
            AppLanguage.DE -> "Kraft"
            AppLanguage.RO -> "Forță"
        }

    val modeTimedLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Időalapú"
            AppLanguage.EN -> "Timed"
            AppLanguage.DE -> "Zeitbasiert"
            AppLanguage.RO -> "Cronometrat"
        }

    val modeBilateralLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Kétoldalas"
            AppLanguage.EN -> "Bilateral"
            AppLanguage.DE -> "Beidseitig"
            AppLanguage.RO -> "Bilateral"
        }

    val modeOutdoorGpsLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Kültéri GPS"
            AppLanguage.EN -> "Outdoor GPS"
            AppLanguage.DE -> "Outdoor GPS"
            AppLanguage.RO -> "GPS exterior"
        }

    val modeIntervalsLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Intervallum"
            AppLanguage.EN -> "Intervals"
            AppLanguage.DE -> "Intervalle"
            AppLanguage.RO -> "Intervale"
        }

    val modeSummaryLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Összegzés"
            AppLanguage.EN -> "Summary"
            AppLanguage.DE -> "Übersicht"
            AppLanguage.RO -> "Sumar"
        }

    val undoAction: String
        get() = when (language) {
            AppLanguage.HU -> "Visszavonás"
            AppLanguage.EN -> "Undo"
            AppLanguage.DE -> "Rückgängig"
            AppLanguage.RO -> "Anulare"
        }

    val skipAction: String
        get() = when (language) {
            AppLanguage.HU -> "Kihagyás"
            AppLanguage.EN -> "Skip"
            AppLanguage.DE -> "Überspringen"
            AppLanguage.RO -> "Omite"
        }

    val finishWorkoutAction: String
        get() = when (language) {
            AppLanguage.HU -> "Edzés befejezése"
            AppLanguage.EN -> "Finish Workout"
            AppLanguage.DE -> "Training beenden"
            AppLanguage.RO -> "Finalizează antrenamentul"
        }

    val pauseAction: String
        get() = when (language) {
            AppLanguage.HU -> "Megállítás"
            AppLanguage.EN -> "Pause"
            AppLanguage.DE -> "Pause"
            AppLanguage.RO -> "Pauză"
        }

    val resetAction: String
        get() = when (language) {
            AppLanguage.HU -> "Nullázás"
            AppLanguage.EN -> "Reset"
            AppLanguage.DE -> "Zurücksetzen"
            AppLanguage.RO -> "Resetare"
        }

    val leftSideLabel: String
        get() = when (language) {
            AppLanguage.HU -> "BAL OLDAL"
            AppLanguage.EN -> "LEFT SIDE"
            AppLanguage.DE -> "LINKE SEITE"
            AppLanguage.RO -> "PARTEA STÂNGĂ"
        }

    val rightSideLabel: String
        get() = when (language) {
            AppLanguage.HU -> "JOBB OLDAL"
            AppLanguage.EN -> "RIGHT SIDE"
            AppLanguage.DE -> "RECHTE SEITE"
            AppLanguage.RO -> "PARTEA DREAPTĂ"
        }

    val switchSideAction: String
        get() = when (language) {
            AppLanguage.HU -> "Oldal váltása"
            AppLanguage.EN -> "Switch Side"
            AppLanguage.DE -> "Seite wechseln"
            AppLanguage.RO -> "Schimbă partea"
        }

    val distanceLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Távolság"
            AppLanguage.EN -> "Distance"
            AppLanguage.DE -> "Distanz"
            AppLanguage.RO -> "Distanță"
        }

    val avgPaceLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Átlagtempó"
            AppLanguage.EN -> "Avg Pace"
            AppLanguage.DE -> "Ø Tempo"
            AppLanguage.RO -> "Ritm mediu"
        }

    val startIntervalsAction: String
        get() = when (language) {
            AppLanguage.HU -> "Intervallum indítása"
            AppLanguage.EN -> "Start Intervals"
            AppLanguage.DE -> "Intervalle starten"
            AppLanguage.RO -> "Start intervale"
        }

    val workoutSummaryTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Edzés összegzés"
            AppLanguage.EN -> "Workout Summary"
            AppLanguage.DE -> "Trainingsübersicht"
            AppLanguage.RO -> "Sumar antrenament"
        }

    val pauseAndExitAction: String
        get() = when (language) {
            AppLanguage.HU -> "Szüneteltetés és kilépés"
            AppLanguage.EN -> "Pause & Exit"
            AppLanguage.DE -> "Pausieren & Beenden"
            AppLanguage.RO -> "Pauză și ieșire"
        }

    val discardWorkoutAction: String
        get() = when (language) {
            AppLanguage.HU -> "Edzés elvetése"
            AppLanguage.EN -> "Discard Workout"
            AppLanguage.DE -> "Training verwerfen"
            AppLanguage.RO -> "Renunță la antrenament"
        }

    // Log & Progress functional strings
    val statsTabLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Statisztika"
            AppLanguage.EN -> "Stats"
            AppLanguage.DE -> "Statistik"
            AppLanguage.RO -> "Statistici"
        }

    val sourceTabLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Forrás"
            AppLanguage.EN -> "Source"
            AppLanguage.DE -> "Quelle"
            AppLanguage.RO -> "Sursă"
        }

    val photosBtnLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Fotók (2)"
            AppLanguage.EN -> "Photos (2)"
            AppLanguage.DE -> "Fotos (2)"
            AppLanguage.RO -> "Fotografii (2)"
        }

    val viewRouteAction: String
        get() = when (language) {
            AppLanguage.HU -> "Útvonal megtekintése"
            AppLanguage.EN -> "View Route"
            AppLanguage.DE -> "Route anzeigen"
            AppLanguage.RO -> "Vezi traseul"
        }

    val totalVolumeTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Összes volumen"
            AppLanguage.EN -> "Total Volume"
            AppLanguage.DE -> "Gesamtvolumen"
            AppLanguage.RO -> "Volum total"
        }

    val comparedToPrevPeriod: String
        get() = when (language) {
            AppLanguage.HU -> "az előző időszakhoz képest"
            AppLanguage.EN -> "compared to previous period"
            AppLanguage.DE -> "im Vergleich zum Vorzeitraum"
            AppLanguage.RO -> "față de perioada anterioară"
        }

    val prRecordsTitle: String
        get() = when (language) {
            AppLanguage.HU -> "PR rekordok"
            AppLanguage.EN -> "PR Records"
            AppLanguage.DE -> "PR-Rekorde"
            AppLanguage.RO -> "Recorduri PR"
        }

    val workoutCountTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Edzések száma"
            AppLanguage.EN -> "Workouts"
            AppLanguage.DE -> "Trainingsanzahl"
            AppLanguage.RO -> "Număr antrenamente"
        }

    val avgVolumeTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Átlagos volumen"
            AppLanguage.EN -> "Avg Volume"
            AppLanguage.DE -> "Ø Volumen"
            AppLanguage.RO -> "Volum mediu"
        }

    val avgDurationTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Átl. edzésidő"
            AppLanguage.EN -> "Avg Duration"
            AppLanguage.DE -> "Ø Dauer"
            AppLanguage.RO -> "Durată medie"
        }

    val muscleLoadTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Izomcsoport-terhelés"
            AppLanguage.EN -> "Muscle Group Load"
            AppLanguage.DE -> "Muskelgruppen-Belastung"
            AppLanguage.RO -> "Încărcare grupe musculare"
        }

    val progressAdviceTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Fejlődési javaslat"
            AppLanguage.EN -> "Progression Advice"
            AppLanguage.DE -> "Trainingsempfehlung"
            AppLanguage.RO -> "Recomandare progres"
        }

    val strength1RmTab: String
        get() = when (language) {
            AppLanguage.HU -> "Erő & 1RM"
            AppLanguage.EN -> "Strength & 1RM"
            AppLanguage.DE -> "Kraft & 1RM"
            AppLanguage.RO -> "Forță & 1RM"
        }

    val personalRecordsSubTab: String
        get() = when (language) {
            AppLanguage.HU -> "Egyéni csúcsok"
            AppLanguage.EN -> "Personal Records"
            AppLanguage.DE -> "Bestleistungen"
            AppLanguage.RO -> "Recorduri personale"
        }

    val cardioStatsSubTab: String
        get() = when (language) {
            AppLanguage.HU -> "Kardió statisztika"
            AppLanguage.EN -> "Cardio Stats"
            AppLanguage.DE -> "Cardio-Statistik"
            AppLanguage.RO -> "Statistici cardio"
        }

    // Health, Coach & Settings functional strings
    val healthOverviewSub: String
        get() = when (language) {
            AppLanguage.HU -> "Napi egészségügyi áttekintés"
            AppLanguage.EN -> "Daily health overview"
            AppLanguage.DE -> "Tägliche Gesundheitsübersicht"
            AppLanguage.RO -> "Rezumat zilnic sănătate"
        }

    val pulseLabel: String
        get() = when (language) {
            AppLanguage.HU -> "Pulzus"
            AppLanguage.EN -> "Heart Rate"
            AppLanguage.DE -> "Puls"
            AppLanguage.RO -> "Puls"
        }

    val pulseTrendTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Pulzustrend"
            AppLanguage.EN -> "Heart Rate Trend"
            AppLanguage.DE -> "Pulstrend"
            AppLanguage.RO -> "Tendință puls"
        }

    val weightLogTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Testsúlynapló"
            AppLanguage.EN -> "Weight Log"
            AppLanguage.DE -> "Gewichtsprotokoll"
            AppLanguage.RO -> "Jurnal greutate"
        }

    val bodyAndFitnessTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Test és fittség"
            AppLanguage.EN -> "Body & Fitness"
            AppLanguage.DE -> "Körper & Fitness"
            AppLanguage.RO -> "Corp și fitness"
        }

    val newMeasurementAction: String
        get() = when (language) {
            AppLanguage.HU -> "+ Új mérés"
            AppLanguage.EN -> "+ New Entry"
            AppLanguage.DE -> "+ Neue Messung"
            AppLanguage.RO -> "+ Măsurare nouă"
        }

    val notEnoughRecentData: String
        get() = when (language) {
            AppLanguage.HU -> "Nincs elég friss adat"
            AppLanguage.EN -> "Not enough recent data"
            AppLanguage.DE -> "Nicht genügend aktuelle Daten"
            AppLanguage.RO -> "Nu există suficiente date recente"
        }

    val recentMeasurementsTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Legutóbbi mérések"
            AppLanguage.EN -> "Recent Measurements"
            AppLanguage.DE -> "Letzte Messungen"
            AppLanguage.RO -> "Măsurători recente"
        }

    val editAction: String
        get() = when (language) {
            AppLanguage.HU -> "Szerkesztés"
            AppLanguage.EN -> "Edit"
            AppLanguage.DE -> "Bearbeiten"
            AppLanguage.RO -> "Editează"
        }

    val deleteAction: String
        get() = when (language) {
            AppLanguage.HU -> "Törlés"
            AppLanguage.EN -> "Delete"
            AppLanguage.DE -> "Löschen"
            AppLanguage.RO -> "Șterge"
        }

    val moreHealthDataTitle: String
        get() = when (language) {
            AppLanguage.HU -> "További egészségadatok"
            AppLanguage.EN -> "More Health Data"
            AppLanguage.DE -> "Weitere Gesundheitsdaten"
            AppLanguage.RO -> "Mai multe date de sănătate"
        }

    val healthJournalTitle: String
        get() = when (language) {
            AppLanguage.HU -> "Egészségnapló"
            AppLanguage.EN -> "Health Journal"
            AppLanguage.DE -> "Gesundheitstagebuch"
            AppLanguage.RO -> "Jurnal de sănătate"
        }

    val syncAction: String
        get() = when (language) {
            AppLanguage.HU -> "Szinkronizálás"
            AppLanguage.EN -> "Sync"
            AppLanguage.DE -> "Synchronisieren"
            AppLanguage.RO -> "Sincronizare"
        }

    val permissionsAction: String
        get() = when (language) {
            AppLanguage.HU -> "Engedélyek"
            AppLanguage.EN -> "Permissions"
            AppLanguage.DE -> "Berechtigungen"
            AppLanguage.RO -> "Permisiuni"
        }

    val handoverToWatchAction: String
        get() = when (language) {
            AppLanguage.HU -> "Átadás órának"
            AppLanguage.EN -> "Handover to Watch"
            AppLanguage.DE -> "An Uhr übergeben"
            AppLanguage.RO -> "Transferă pe ceas"
        }

    val reclaimToPhoneAction: String
        get() = when (language) {
            AppLanguage.HU -> "Visszavétel telefonra"
            AppLanguage.EN -> "Reclaim to Phone"
            AppLanguage.DE -> "Auf Telefon zurückholen"
            AppLanguage.RO -> "Preia pe telefon"
        }
}

object TrainPilotStrings {
    private val hu = LocalizedStrings(
        language = AppLanguage.HU,
        navHome = "Kezdőlap",
        navWorkout = "Edzés",
        navLog = "Napló",
        navProgress = "Fejlődés",
        navHealth = "Egészség",
        greetingTitle = "Szia, Levente!",
        greetingPausedSub = "Folytasd a megkezdett edzést!",
        greetingPlannedSub = "A mai terved készen áll az indításra.",
        greetingLightSub = "A mai jelek alapján kímélő edzés javasolt.",
        pausedBadge = "Szüneteltetve",
        continueBtn = "Folytatás",
        startPlannedBtn = "Tervezett edzés indítása",
        quickWorkoutTitle = "Gyors edzés",
        quickWorkoutSub = "Indíts egy rövid edzést most",
        watchConnectedSub = "Csatlakoztatva • 92%",
        sleepTitle = "Alvás",
        sleepQualityGood = "Jó minőség",
        restingHrTitle = "Nyugalmi pulzus",
        restingHrNormal = "Normális",
        stepsTitle = "Lépések",
        stepsGoalPrefix = "Cél: 10 000",
        samsungEnergyTitle = "Samsung energia",
        energyGoodState = "Jó állapot",
        sourceSamsungHealth = "Forrás: Samsung Health",
        personalPlannerTitle = "Személyes edzéstervező",
        personalPlannerSub = "Állítsunk össze egy hozzád illő, követhető tervet.",
        profileAndNewPlanBtn = "Profil és új terv",
        activeProgramBadge = "Aktív program",
        nextWorkoutLabel = "Következő",
        todayStateTitle = "Mai állapot",
        activeCaloriesTitle = "Aktív kalória",
        todayWorkoutTitle = "Mai edzés",
        regenerationTitle = "Regeneráció",
        goalsSetupTitle = "Célok beállítása",
        coachSuggestionHeader = "MAI JAVASLAT",
        coachLightDayTitle = "Könnyített nap",
        coachLightDayBody = "A mai jelek alapján ne erőltesd a progressziót. Könnyített edzés vagy pihenő is megfelelő választás lehet. A közelmúltban fájdalom / kellemetlenség jelzés volt. Az érintett gyakorlatnál ne emelj terhelést.",
        coachNormalDayTitle = "Optimális terhelés",
        coachNormalDayBody = "Az alvás és a Samsung energiapontszám (87/100) alapján a szervezeted készen áll a tervezett munkasorozatokra. Tartsd az 1–2 RIR tartalékot.",
        readinessTodayLabel = "Mai készenlét",
        demoModeLabel = "DEMO MINTAADAT",
        settingsTitle = "Beállítások",
        themesTitle = "Témák",
        languageTitle = "Nyelv",
        restTimerTitle = "Edzés időzítő",
        soundAlertTitle = "Hangjelzés",
        connectedDevicesTitle = "Kapcsolt eszközök",
        backupRestoreTitle = "Mentés és visszaállítás",
        privacyPermissionsTitle = "Adatvédelem és engedélyek",
        saveBtn = "Mentés",
        cancelBtn = "Mégse",
        backBtn = "Vissza"
    )

    private val en = LocalizedStrings(
        language = AppLanguage.EN,
        navHome = "Home",
        navWorkout = "Workout",
        navLog = "Log",
        navProgress = "Progress",
        navHealth = "Health",
        greetingTitle = "Hi, Levente!",
        greetingPausedSub = "Continue your paused workout!",
        greetingPlannedSub = "Your planned workout is ready to start.",
        greetingLightSub = "Based on today's signals, a light session is advised.",
        pausedBadge = "Paused",
        continueBtn = "Continue",
        startPlannedBtn = "Start Planned Workout",
        quickWorkoutTitle = "Quick Workout",
        quickWorkoutSub = "Start a short session right now",
        watchConnectedSub = "Connected • 92%",
        sleepTitle = "Sleep",
        sleepQualityGood = "Good quality",
        restingHrTitle = "Resting HR",
        restingHrNormal = "Normal",
        stepsTitle = "Steps",
        stepsGoalPrefix = "Goal: 10,000",
        samsungEnergyTitle = "Samsung Energy",
        energyGoodState = "Good state",
        sourceSamsungHealth = "Source: Samsung Health",
        personalPlannerTitle = "Personal Workout Planner",
        personalPlannerSub = "Let's build a tailored, trackable program for you.",
        profileAndNewPlanBtn = "Profile & New Plan",
        activeProgramBadge = "Active Program",
        nextWorkoutLabel = "Up Next",
        todayStateTitle = "Today's State",
        activeCaloriesTitle = "Active Calories",
        todayWorkoutTitle = "Today's Workout",
        regenerationTitle = "Recovery",
        goalsSetupTitle = "Set Goals",
        coachSuggestionHeader = "TODAY'S ADVICE",
        coachLightDayTitle = "Light / Deload Day",
        coachLightDayBody = "Based on today's readiness signals, do not force progression. A lighter session or active recovery is ideal. Pain/discomfort was recently logged—keep weights steady.",
        coachNormalDayTitle = "Optimal Readiness",
        coachNormalDayBody = "Your sleep and Samsung Energy Score (87/100) show strong recovery. Proceed with planned working sets at 1–2 RIR.",
        readinessTodayLabel = "Today's Readiness",
        demoModeLabel = "DEMO SAMPLE DATA",
        settingsTitle = "Settings",
        themesTitle = "Themes",
        languageTitle = "Language",
        restTimerTitle = "Workout Timer",
        soundAlertTitle = "Sound Alerts",
        connectedDevicesTitle = "Connected Devices",
        backupRestoreTitle = "Backup & Restore",
        privacyPermissionsTitle = "Privacy & Permissions",
        saveBtn = "Save",
        cancelBtn = "Cancel",
        backBtn = "Back"
    )

    private val de = LocalizedStrings(
        language = AppLanguage.DE,
        navHome = "Start",
        navWorkout = "Training",
        navLog = "Tagebuch",
        navProgress = "Fortschritt",
        navHealth = "Gesundheit",
        greetingTitle = "Hallo, Levente!",
        greetingPausedSub = "Setze dein pausiertes Training fort!",
        greetingPlannedSub = "Dein heutiges Training ist startbereit.",
        greetingLightSub = "Heute wird ein leichteres Training empfohlen.",
        pausedBadge = "Pausiert",
        continueBtn = "Fortsetzen",
        startPlannedBtn = "Geplantes Training starten",
        quickWorkoutTitle = "Schnelltraining",
        quickWorkoutSub = "Starte jetzt eine kurze Einheit",
        watchConnectedSub = "Verbunden • 92%",
        sleepTitle = "Schlaf",
        sleepQualityGood = "Gute Qualität",
        restingHrTitle = "Ruhepuls",
        restingHrNormal = "Normal",
        stepsTitle = "Schritte",
        stepsGoalPrefix = "Ziel: 10.000",
        samsungEnergyTitle = "Samsung Energie",
        energyGoodState = "Guter Zustand",
        sourceSamsungHealth = "Quelle: Samsung Health",
        personalPlannerTitle = "Persönlicher Trainingsplaner",
        personalPlannerSub = "Erstelle einen passenden Trainingsplan für dich.",
        profileAndNewPlanBtn = "Profil & Neuer Plan",
        activeProgramBadge = "Aktives Programm",
        nextWorkoutLabel = "Nächstes",
        todayStateTitle = "Heutiger Status",
        activeCaloriesTitle = "Aktive Kalorien",
        todayWorkoutTitle = "Heutiges Training",
        regenerationTitle = "Erholung",
        goalsSetupTitle = "Ziele festlegen",
        coachSuggestionHeader = "HEUTIGE EMPFEHLUNG",
        coachLightDayTitle = "Leichter Tag",
        coachLightDayBody = "Erzwinge heute keine Steigerung. Ein leichteres Training oder Pause ist sinnvoll. Zuletzt wurde Schmerz/Unbehagen gemeldet.",
        coachNormalDayTitle = "Optimale Bereitschaft",
        coachNormalDayBody = "Schlaf und Samsung Energiewert (87/100) zeigen gute Erholung. Reguläre Arbeitssätze mit 1–2 RIR empfohlen.",
        readinessTodayLabel = "Heutige Bereitschaft",
        demoModeLabel = "DEMO-BEISPIELDATEN",
        settingsTitle = "Einstellungen",
        themesTitle = "Themen",
        languageTitle = "Sprache",
        restTimerTitle = "Pausen-Timer",
        soundAlertTitle = "Tonhinweise",
        connectedDevicesTitle = "Verbundene Geräte",
        backupRestoreTitle = "Sichern & Wiederherstellen",
        privacyPermissionsTitle = "Datenschutz & Berechtigungen",
        saveBtn = "Speichern",
        cancelBtn = "Abbrechen",
        backBtn = "Zurück"
    )

    private val ro = LocalizedStrings(
        language = AppLanguage.RO,
        navHome = "Acasă",
        navWorkout = "Antrenament",
        navLog = "Jurnal",
        navProgress = "Progres",
        navHealth = "Sănătate",
        greetingTitle = "Salut, Levente!",
        greetingPausedSub = "Continuă antrenamentul întrerupt!",
        greetingPlannedSub = "Antrenamentul planificat este gata de start.",
        greetingLightSub = "Astăzi este recomandat un antrenament ușor.",
        pausedBadge = "În pauză",
        continueBtn = "Continuă",
        startPlannedBtn = "Începe antrenamentul",
        quickWorkoutTitle = "Antrenament rapid",
        quickWorkoutSub = "Începe o sesiune scurtă acum",
        watchConnectedSub = "Conectat • 92%",
        sleepTitle = "Somn",
        sleepQualityGood = "Calitate bună",
        restingHrTitle = "Puls repaus",
        restingHrNormal = "Normal",
        stepsTitle = "Pași",
        stepsGoalPrefix = "Țintă: 10.000",
        samsungEnergyTitle = "Energie Samsung",
        energyGoodState = "Stare bună",
        sourceSamsungHealth = "Sursă: Samsung Health",
        personalPlannerTitle = "Planificator personal",
        personalPlannerSub = "Construiește un plan adaptat obiectivelor tale.",
        profileAndNewPlanBtn = "Profil și plan nou",
        activeProgramBadge = "Program activ",
        nextWorkoutLabel = "Urmează",
        todayStateTitle = "Starea de azi",
        activeCaloriesTitle = "Calorii active",
        todayWorkoutTitle = "Antrenament azi",
        regenerationTitle = "Recuperare",
        goalsSetupTitle = "Setare obiective",
        coachSuggestionHeader = "RECOMANDAREA ZILEI",
        coachLightDayTitle = "Zi ușoară",
        coachLightDayBody = "Nu forța progresia astăzi. O sesiune ușoară sau odihna este potrivită. Recent a fost raportat disconfort.",
        coachNormalDayTitle = "Pregătire optimă",
        coachNormalDayBody = "Somnul și scorul Samsung Energy (87/100) indică o recuperare excelentă.",
        readinessTodayLabel = "Pregătirea de azi",
        demoModeLabel = "DATE DEMO",
        settingsTitle = "Setări",
        themesTitle = "Teme",
        languageTitle = "Limbă",
        restTimerTitle = "Cronometru pauză",
        soundAlertTitle = "Alerte sonore",
        connectedDevicesTitle = "Dispozitive conectate",
        backupRestoreTitle = "Backup și restaurare",
        privacyPermissionsTitle = "Confidențialitate și permisiuni",
        saveBtn = "Salvează",
        cancelBtn = "Anulează",
        backBtn = "Înapoi"
    )

    fun forLanguage(lang: AppLanguage): LocalizedStrings = when (lang) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }
}
