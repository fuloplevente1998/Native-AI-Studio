package com.example.model

/** Unsaved fields from one program day, captured by the explicit program Save action. */
data class StudioProgramPrescriptionDraft(val dayKey: String, val exerciseId: String,
    val sets: Int, val reps: Int, val weightKg: Float, val restSec: Int)
