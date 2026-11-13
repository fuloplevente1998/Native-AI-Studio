package com.example.ui.viewmodel

import androidx.compose.ui.graphics.toArgb
import androidx.room.withTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.YearMonth
import java.time.ZoneId
import java.time.LocalTime
import androidx.lifecycle.ViewModel
import com.example.data.DemoDataProvider
import com.example.model.AppLanguage
import com.example.model.BodyWeightLogItem
import com.example.model.CalendarDayEvent
import com.example.model.CalendarEventStatus
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.HomeDemoMode
import com.example.model.LoggedWorkoutEntry
import com.example.model.workoutsInCalendarWeekToDate
import com.example.model.workoutsInCalendarMonthToDate
import com.example.model.workoutsInPeriodToDate
import com.example.model.localWorkoutDate
import com.example.model.StudioProgressWindow
import com.example.model.progressPeriodDays
import com.example.model.MainTab
import com.example.model.PrRecordEntry
import com.example.model.TrainingProgramModel
import com.example.model.SubScreen
import com.example.ui.theme.PaletteCategory
import com.example.ui.theme.TrainPilotPalettes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.HealthBackgroundJob
import com.repforge.app.HealthJournalStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.WearSyncRepository
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.ProgramDayEntity
import com.repforge.app.data.db.ProgramEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.model.watchinfo.WatchInfoCardData
import com.example.model.watchinfo.WatchInfoStatus
import com.repforge.app.healthreadings.HealthReadingsProjection
import com.repforge.app.healthreadings.PulseDayReading
import com.repforge.app.healthreadings.RecoveryDayReading
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import com.repforge.app.TrainPilotWorkoutFinalization
import com.repforge.app.NativeWorkoutSession
import com.repforge.app.PhoneOwnership193
import com.repforge.app.BackupRestoreManager
import com.repforge.app.ui.progress.NativeProgressQuery
import com.repforge.app.ui.progress.MuscleLoadUiModel
import android.net.Uri
import kotlin.math.roundToInt

data class NavRoute(
    val tab: MainTab,
    val subScreen: SubScreen = SubScreen.NONE
)

data class WeeklyHealthDaySummary(
    val dateIso: String,
    val dayLabelHu: String,
    val steps: Int,
    val activeCaloriesKcal: Int,
    val sleepLabel: String,
    val workoutMinutes: Int,
    val workoutCount: Int,
    val sourceLabelHu: String,
    val stepOriginPackage: String = "",
    val stepsAvailable: Boolean = false,
    val stepsSourceLabelHu: String = "Nincs napi lépésadat",
    val pulse: PulseDayReading? = null
)

/** Read-only comparison for the exact saved native program and mode proposal. */
data class StudioTrainingModeRow(
    val day: String,
    val exercise: String,
    val beforeSets: Int?,
    val afterSets: Int?,
    val beforeReps: String,
    val afterReps: String,
    val restSeconds: Int?,
    val beforeWeight: Double?,
    val afterWeight: Double?,
    val rir: String
)

data class StudioTrainingModeReview(
    val beforeStyle: String,
    val beforeProgression: String,
    val afterStyle: String,
    val afterProgression: String,
    val inheritGlobal: Boolean,
    val programName: String,
    val rows: List<StudioTrainingModeRow>
)

data class TrainPilotUiState(
    // Theme & Localization & Accessibility
    val selectedPaletteId: Int = 1, // Default: 01 • Grafit & Arany
    val paletteFilterCategory: PaletteCategory? = null, // null = All 32
    val showPalettePickerSheet: Boolean = false,
    val language: AppLanguage = AppLanguage.HU,
    val fontScaleMultiplier: Float = 1.0f,
    val compactPhoneMode: Boolean = false,

    // Navigation & Backstack
    val currentTab: MainTab = MainTab.HOME,
    val currentSubScreen: SubScreen = SubScreen.NONE,
    val backStack: List<NavRoute> = emptyList(),

    // Home Screen State (Mockup 1/4 #1 + main Home)
    val homeDemoMode: HomeDemoMode = HomeDemoMode.PLANNED_NEXT,
    val workoutGoalPeriod: com.example.model.WorkoutGoalPeriod = com.example.model.WorkoutGoalPeriod.WEEKLY,
    val workoutGoalTarget: Int = 15,
    val workoutGoalCompleted: Int = 0,
    val weeklyGoalCompleted: Int = 0,
    val weeklyGoalTarget: Int = 15,
    val showGoalsDialog: Boolean = false,
    val showPersonalPlannerModal: Boolean = false,
    val showQuickWorkoutSheet: Boolean = false,
    val sleepDurationLabel: String = "—",
    val sleepHoursDecimal: String = "—",
    val sleepHasStages: Boolean = false,
    val sleepStagesSummaryHu: String = "",
    val restingHrBpm: Int = 0,
    val currentHrBpm: Int = 0,
    val maxHrBpm: Int = 0,
    val minHrBpm: Int = 0,
    val dailyPulseHistory: List<com.repforge.app.NativeDailyHealthDetails.PulsePoint> = emptyList(),
    val dailyPulseSampleCount: Int = 0,
    val hrvRmssdMs: Float = 0f,
    val bloodPressureLabel: String = "—",
    val bloodGlucoseLabel: String = "—",
    val respiratoryRateLabel: String = "—",
    val dailyDistanceKm: Float = 0f,
    val dailySteps: Int = 0,
    val stepsGoal: Int = 10000,
    val activeCaloriesKcal: Int = 0,
    val totalCaloriesKcal: Int = 0,
    val dailyHealthDate: String = "",
    val dailyHealthAvailableFields: Set<String> = emptySet(),
    val dailyHealthSourceLabel: String = "Health Connect",
    val dailyHealthLastSyncIso: String = "—",
    val healthLastSyncByProvider: Map<String, String> = emptyMap(),
    val healthRefreshRevision: Long = 0L,
    val healthAutoOnOpen: Boolean = true,
    val healthBackgroundOptedIn: Boolean = false,
    val healthBackgroundScheduled: Boolean = false,
    val healthGrantedCount: Int = 0,
    val healthPermissionCount: Int = 0,
    val lastHealthSyncLabel: String = "—",
    val healthProviderStatus: String = "A szolgáltató állapota még nem ismert",
    val healthSyncMessage: String = "", val healthSyncBusy: Boolean = false,
    val healthSyncPartial: Boolean = false, val healthSyncFailed: Boolean = false,
    val samsungGrantedCount: Int = -1, val samsungPermissionCount: Int = com.repforge.app.SamsungHealthRepository.PERMISSION_KEYS.size,
    val lastHealthReadLabel: String = "—",
    val samsungEnergyScore: Int = 0,
    val trainPilotReadinessScore: Int = 0,
    val coachReadinessScore: Int = 0,
    val todayPulseReading: PulseDayReading? = null,
    val todayRecoveryReading: RecoveryDayReading? = null,

    // Watch & Sync State (Mockup 4/4 #2 + main)
    val watchName: String = "Nincs csatlakoztatott Wear OS óra",
    val watchConnected: Boolean = false,
    val watchAppAvailable: Boolean = false,
    val watchBatteryPercent: Int = -1,
    val watchInfoLoading: Boolean = false,
    val watchInfoMessage: String = "",
    val watchDeviceDetails: String = "",
    val watchInfoCardData: WatchInfoCardData = WatchInfoCardData(),
    val phoneControlsWorkout: Boolean = true,
    val watchControlPending: Boolean = false,
    val offlineContinuationEnabled: Boolean = true,
    val lastWatchSyncLabel: String = "—",
    val autoDriveSync: Boolean = false,
    val autoCalendarSync: Boolean = false,
    val lastDriveSyncLabel: String = "—",
    val lastCalendarSyncLabel: String = "—",

    // Programs & Program Editor State (Mockup 1/4 #3 + 4/4 #6 + main)
    val programs: List<TrainingProgramModel> = emptyList(),
    val programsTabCustomOnly: Boolean = false,
    val editingProgramId: String = "",
    val editingDayIndex: Int = 0,
    val expandedEditorExerciseId: String? = null,
    val showProgressionModal: Boolean = false,
    val trainingStyleHu: String = "Normál hipertrófia",
    val progressionRuleHu: String = "Dupla progresszió",
    val beginnerGentleProfileActive: Boolean = false,
    val trainingStyleCode: String = "normal",
    val progressionModeCode: String = "double",
    val programTrainingStyleCode: String = "normal",
    val programProgressionModeCode: String = "double",
    val programTrainingModeSource: String = "legacy",
    val activeTrainingModeProgramId: String = "",
    val globalTrainingModeUndo: Boolean = false,
    val programTrainingModeUndo: Boolean = false,
    val progressionModeReview: StudioTrainingModeReview? = null,
    val progressionModeBusy: Boolean = false,

    // Calendar & Replan State (Mockup 1/4 #4 + main Naptár & Újratervezés)
    val calendarYearMonth: String = YearMonth.now().toString(),
    val selectedCalendarDay: Int = LocalDate.now().dayOfMonth,
    val calendarEvents: List<CalendarDayEvent> = emptyList(),
    val calendarUndoStack: List<List<CalendarDayEvent>> = emptyList(),
    val showReplanModal: Boolean = false,
    val planningCadenceHu: String = "Minden második nap",
    val planningFirstDayHu: String = "Alap A",
    val planningStartDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")),
    val planningTime: String = "10:00",
    val planningDurationMin: Int = 60,
    val planningWeeksCount: Int = 4,

    // Exercise Library & Detail State (Mockup 1/4 #5 & #6)
    val exercises: List<ExerciseItem> = emptyList(),
    val exerciseSearchQuery: String = "",
    val selectedExerciseCategory: ExerciseCategory = ExerciseCategory.ALL,
    val selectedExerciseId: String = "",
    val showAddCustomExerciseDialog: Boolean = false,
    val libraryMutationBusy: Boolean = false,
    val favoriteExerciseIds: Set<String> = emptySet(),
    val favoriteExercisesOnly: Boolean = false,

    // Active Workout & Measurement State (Mockup 2/4 #1..#6)
    val isSavingWorkout: Boolean = false,
    val isWorkoutSessionActive: Boolean = false,
    val activeWorkoutId: String = "",
    val activeWorkoutGeneration: Long = 0L,
    val workoutStartedEpochMs: Long = 0L,
    val activeWorkoutSubScreen: SubScreen = SubScreen.ACTIVE_STRENGTH,
    val activeWorkoutTitleHu: String = "Otthoni A/B • A nap",
    val activeWorkoutExercises: List<ExerciseItem> = emptyList(),
    val activeExerciseIndex: Int = 0,
    val activeSets: List<ExerciseSetItem> = listOf(
        ExerciseSetItem(setNumber = 1, weightKg = 0f, reps = 10, rir = 2, completed = false),
        ExerciseSetItem(setNumber = 2, weightKg = 0f, reps = 10, rir = 2, completed = false),
        ExerciseSetItem(setNumber = 3, weightKg = 0f, reps = 10, rir = 2, completed = false)
    ),
    val activeSetsByExerciseId: Map<String, List<ExerciseSetItem>> = emptyMap(),
    val quickWorkoutQueue: List<ExerciseItem> = emptyList(),
    val autoModeEnabled: Boolean = false,
    val profileComplete: Boolean = false,
    val userHeightCm: Int = 179,
    val userWeightKg: Float = 65f,
    val userFitnessGoalHu: String = "Izomépítés",
    val userExperienceLevelHu: String = "Kezdő",
    val userWorkoutLocationHu: String = "Otthon",
    val userSelectedEquipmentHu: String = "Kézisúlyzó és pad",
    val userScheduleDaysPerWeek: Int = 3,
    val userWorkoutDurationMin: Int = 45,
    val userExcludedMusclesHu: Set<String> = emptySet(),
    val userExcludedExercisesHu: Set<String> = emptySet(),
    val restTimerRemainingSec: Int = 90,
    val restTimerRunning: Boolean = false,
    val restTimerEndRealtimeMs: Long = 0L,
    val restTimerEndEpochMs: Long = 0L,
    val restTimerLastTickRealtimeMs: Long = 0L,
    val defaultRestSec: Int = 90,
    val soundAlertEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoStepIntervals: Boolean = true,
    val intervalWorkSec: Int = 30,
    val intervalRestSec: Int = 60,
    val intervalRounds: Int = 3,
    val timedCurrentSec: Int = 0,
    val bilateralLeftSec: Int = 0,
    val bilateralRightSec: Int = 0,
    val outdoorGpsEnabled: Boolean = true,
    val outdoorRouteKey: String = "",
    val outdoorDistanceKm: Float = 0f,
    val outdoorElapsedLabel: String = "00:00",
    val outdoorPaceLabel: String = "0:00",
    val workoutSummaryMoodIndex: Int = -1, // No feedback until explicitly chosen.
    val workoutSummaryNote: String = "",
    val summaryFeedbackWorkoutId: String = "",
    val exerciseFeedbackWorkoutId: String = "",
    val exerciseFeedbackChoices: Map<String, Int> = emptyMap(),
    val exerciseFeedbackNotes: Map<String, String> = emptyMap(),

    // Log / Diary State (Mockup 3/4 #1..#3 + main)
    val logSearchQuery: String = "",
    val selectedLogMonth: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy. MMMM", Locale("hu", "HU"))),
    val loggedWorkouts: List<LoggedWorkoutEntry> = emptyList(),
    val selectedLogWorkoutId: String = "",
    val logDetailTabIndex: Int = 0, // 0 = Gyakorlatok, 1 = Statisztikák, 2 = Forrás
    val expandedLogExerciseIndex: Int = 0,

    // Progress / Fejlődés State (Mockup 3/4 #4 + full main Fejlődés & Statisztikák)
    val progressMainSection: Int = 0, // 0 = Kompakt Fejlődés (main), 1 = Statisztikák (Rekordok & Kardió), 2 = 1RM Erőnlét
    val selectedProgressExercise: String = "Fekvenyomás",
    val selectedProgressRangeIndex: Int = 3, // 0 = 1 nap, 1 = 7 nap, 2 = 30 nap, 3 = 3 hó
    val selectedVolumeBarIndex: Int = -1,
    val expandedStatsCardIndex: Int = -1,
    val statsSubTab: Int = 0, // 0 = Személyes rekordok, 1 = Kardióstatisztika
    val cardioFilterStepsMode: Boolean = true, // false = Minden mozgás, true = Napi aktivitás / lépések
    val prRecords: List<PrRecordEntry> = emptyList(),
    val savedExerciseBests: List<com.repforge.app.SavedExerciseBest> = emptyList(),
    val savedRecordsLoading: Boolean = true,
    val savedRecordsError: Boolean = false,
    val videoModalExerciseTitle: String? = null,
    val videoModalExerciseId: String? = null,
    val realMuscleLoads: List<com.example.ui.components.MuscleLoadItem> = emptyList(),
    val muscleLoadPeriodDays: Int = 90,
    val logWorkoutMuscleLoads: List<com.example.ui.components.MuscleLoadItem> = emptyList(),

    // Health / Egészség State (Mockup 3/4 #5 + full main Egészség)
    val expandedWeeklySteps: Boolean = false,
    val expandedWeeklyWorkouts: Boolean = false,
    val expandedHealthPulseTrend: Boolean = false,
    val expandedHealthWeightLog: Boolean = false,
    val expandedHealthExtraData: Boolean = false,
    val expandedHealthJournal: Boolean = false,
    val expandedHealthConnect: Boolean = false,
    val weeklyHealthDays: List<WeeklyHealthDaySummary> = emptyList(),
    val dailyActivityDays: List<WeeklyHealthDaySummary> = emptyList(),
    val weeklyTotalSteps: Int = 0,
    val weeklyTotalCaloriesKcal: Int = 0,
    val weeklyTotalDistanceKm: Float = 0f,
    val weeklyAvgSleepLabel: String = "—",
    val weeklyDataSourcesSummary: String = "Health Connect / Samsung Health + Room edzésnapló",
    val dailySourceModeLabel: String = "Automatikus (legteljesebb napi lépésforrás)",
    val selectedWeightRangeIndex: Int = 1, // 0=7d, 1=30d, 2=3m, 3=1y, 4=All
    val currentBodyWeightKg: Float = 0f,
    val bodyWeightLogs: List<BodyWeightLogItem> = emptyList(),
    val bodyFatPercent: Float = 0f,
    val spo2Percent: Float = 0f,
    val vo2Max: Float = 0f,
    val showAddWeightDialog: Boolean = false,
    val primaryHealthSourceSamsung: Boolean = true,

    // Permissions & Backup State (Mockup 4/4 #3, #4, #5)
    val selectedBackupFormat: String = "JSON", // JSON, ZIP, DRIVE
    val includeHealthInBackup: Boolean = false,
    val backupHealthConsentSaving: Boolean = false,
    val permissionGpsEnabled: Boolean = false,
    val permissionCentralLocEnabled: Boolean = false,
    val permissionNotificationsEnabled: Boolean = false,
    val permissionHealthConnectEnabled: Boolean = false,
    val permissionSamsungHealthEnabled: Boolean = false,
    val privatePhotoCount: Int = 0,
    val privatePhotoWorkoutId: String? = null,

    // Toast / Feedback banner for immediate visual confirmation of actions
    val statusBannerMessage: String? = null
)

