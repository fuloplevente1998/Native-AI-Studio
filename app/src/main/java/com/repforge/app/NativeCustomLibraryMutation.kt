package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore

/** Bounded custom-library writes against the dataset on which the action was initiated. */
internal object NativeCustomLibraryMutation {
    suspend fun addExercise(
        context: Context, db: TrainPilotDatabase, entity: ExerciseLibraryEntity,
        programId: String?, dayId: String?, language: String,
        ticket: NativeDataReplacement.Continuation
    ): String? = NativeDataReplacement.mutate(context) {
        ticket.requireCurrent(context)
        require(entity.custom && entity.id.isNotBlank())
        require(programId != null || dayId == null)
        db.withTransaction {
            val existing = db.exerciseLibraryDao().getExerciseById(entity.id)
            require(existing == null || existing.custom) { "Cannot overwrite a built-in exercise" }
            val opened = programId?.let {
                requireNotNull(db.programDao().getProgramWithDaysById(it)) { "Program changed; reopen editor" }
            }
            val selected = opened?.let { program ->
                requireNotNull(program.days.singleOrNull { it.dayId == dayId || it.compositeId == dayId }) {
                    "Program day changed; reopen editor"
                }
            }
            db.exerciseLibraryDao().upsertExercise(entity)
            if (opened == null) null else {
                val draft = NativeProgramEditor.from(opened, language)
                val changed = draft.copy(days = draft.days.map { day ->
                    if (day.id == selected!!.dayId) NativeProgramEditor.addExercise(day, entity.id) else day
                })
                NativeProgramEditor.save(context, db, opened, changed, language)
            }
        }
    }

    suspend fun createProgram(
        context: Context, db: TrainPilotDatabase, settings: TrainPilotSettingsStore,
        name: String, description: String, language: String,
        starterDayA: List<String>, starterDayB: List<String>,
        ticket: NativeDataReplacement.Continuation
    ): String = NativeDataReplacement.mutate(context) {
        ticket.requireCurrent(context)
        val draft = NativeProgramEditor.Draft(name, description, listOf(
            NativeProgramEditor.DayDraft("A", "A nap", emptyList()),
            NativeProgramEditor.DayDraft("B", "B nap", emptyList())
        ).mapIndexed { index, day ->
            (if (index == 0) starterDayA else starterDayB).fold(day, NativeProgramEditor::addExercise)
        })
        val id = NativeProgramEditor.save(context, db, null, draft, language)
        settings.setActiveProgramId(id)
        id
    }
}
