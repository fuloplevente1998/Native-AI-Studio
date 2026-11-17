package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore

/** Ordinary editor saves keep the active program pointing at the user's edited copy. */
internal object NativeEditedProgramSave {
    suspend fun save(
        context: Context,
        db: TrainPilotDatabase,
        settings: TrainPilotSettingsStore,
        opened: ProgramWithDays,
        draft: NativeProgramEditor.Draft,
        language: String
    ): String = NativeDataReplacement.mutate(context) {
        // The same metadata lease also protects active-program writes. Do not activate a
        // copy of a template that the user was only browsing or explicitly copying.
        val replacesActiveTemplate = opened.program.builtin &&
            settings.current().activeProgramId == opened.program.id
        val savedId = NativeProgramEditor.save(context, db, opened, draft, language)
        if (replacesActiveTemplate && settings.current().activeProgramId == opened.program.id) {
            settings.setActiveProgramId(savedId)
        }
        savedId
    }
}
