package com.repforge.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(
    entities = [
        ProgramEntity::class,
        ProgramDayEntity::class,
        ExerciseLibraryEntity::class,
        WorkoutHistoryEntity::class,
        WorkoutHistoryExerciseEntity::class,
        WorkoutHistorySetEntity::class,
        WorkoutPhotoEntity::class,
        BodyWeightEntryEntity::class,
        CalendarScheduleEntity::class,
        HistoryTombstoneEntity::class,
        KeyValueDocumentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TrainPilotDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun exerciseLibraryDao(): ExerciseLibraryDao
    abstract fun workoutHistoryDao(): WorkoutHistoryDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun calendarScheduleDao(): CalendarScheduleDao
    abstract fun keyValueDocumentDao(): KeyValueDocumentDao

    companion object {
        const val DATABASE_NAME = "trainpilot_native.db"

        @Volatile
        private var INSTANCE: TrainPilotDatabase? = null

        @Volatile
        private var createdInstance: TrainPilotDatabase? = null

        @Volatile
        private var createdContext: Context? = null

        @JvmStatic
        fun getInstance(context: Context): TrainPilotDatabase {
            val app = context.applicationContext ?: context
            val current = INSTANCE
            if (current != null && (current !== createdInstance || createdContext === app)) {
                return current
            }
            return synchronized(this) {
                val again = INSTANCE
                if (again != null && (again !== createdInstance || createdContext === app)) {
                    again
                } else {
                    runCatching { again?.close() }
                    Room.databaseBuilder(
                        app,
                        TrainPilotDatabase::class.java,
                        DATABASE_NAME
                    )
                        .build()
                        .also {
                            createdInstance = it
                            createdContext = app
                            INSTANCE = it
                        }
                }
            }
        }

        suspend fun ensureBuiltinCatalogSeeded(db: TrainPilotDatabase, context: Context) {
            // Routine snapshot publication is a read when the complete catalog already exists.
            // Do not acquire a metadata write lease merely to run no-op INSERT checks: that
            // would make a following restore spuriously busy. Actual missing-row writes still
            // use the original admission fence and recheck their IDs inside the transaction.
            val legacyHomeBasicDayA = """["goblet-squat","db-floor-press","one-arm-row","db-ohp","db-rdl","db-curl","side-plank"]"""
            val ticket = runCatching {
                com.repforge.app.NativeDataReplacement.continuationForCurrentData(context)
            }.getOrNull()
            if (ticket != null) {
                val exercises = db.exerciseLibraryDao().getExerciseIds().toSet()
                val programs = db.programDao().getProgramIds().toSet()
                val homeBasicWithDays = db.programDao().getProgramWithDaysById("home-basic")
                val needsHomeBasicDayAMigration = homeBasicWithDays != null &&
                    homeBasicWithDays.program.builtin &&
                    !homeBasicWithDays.program.generated &&
                    homeBasicWithDays.days.any { it.dayId == "A" && it.exercisesJson == legacyHomeBasicDayA }
                if (!needsHomeBasicDayAMigration &&
                    defaultExercises().all { it.id in exercises } &&
                    defaultPrograms().first.all { it.id in programs }
                ) {
                    ticket.requireCurrent(context)
                    return
                }
            }
            // Missing-only seeding preserves imported/customized rows, including old native IDs.
            com.repforge.app.NativeDataReplacement.mutate(context) {
                db.withTransaction {
                    val knownExercises = db.exerciseLibraryDao().getExerciseIds().toSet()
                    db.exerciseLibraryDao().insertMissingExercises(defaultExercises().filter { it.id !in knownExercises })
                    val knownPrograms = db.programDao().getProgramIds().toSet()
                    val (programs, days) = defaultPrograms()
                    for (program in programs.filter { it.id !in knownPrograms }) {
                        if (db.programDao().insertMissingProgram(program) != -1L)
                            db.programDao().upsertProgramDays(days.filter { it.programId == program.id })
                    }
                    val existingHomeBasic = db.programDao().getProgramWithDaysById("home-basic")
                    if (existingHomeBasic != null &&
                        existingHomeBasic.program.builtin &&
                        !existingHomeBasic.program.generated &&
                        existingHomeBasic.days.any { it.dayId == "A" && it.exercisesJson == legacyHomeBasicDayA }
                    ) {
                        val defaultHomeBasicProg = programs.firstOrNull { it.id == "home-basic" }
                        val defaultHomeBasicDayA = days.firstOrNull { it.programId == "home-basic" && it.dayId == "A" }
                        if (defaultHomeBasicProg != null && defaultHomeBasicDayA != null) {
                            db.programDao().upsertProgram(defaultHomeBasicProg.copy(updatedAt = existingHomeBasic.program.updatedAt))
                            db.programDao().upsertProgramDays(listOf(defaultHomeBasicDayA))
                        }
                    }
                }
            }
        }

        fun defaultExercises(): List<ExerciseLibraryEntity> = NativeBuiltinCatalog.exercises
        fun defaultPrograms(): Pair<List<ProgramEntity>, List<ProgramDayEntity>> = NativeBuiltinCatalog.programs
    }
}
