package com.example.ui.components

import com.repforge.app.ui.coach.NativeCoachReadiness

/** Presentation of the canonical result; never infer new Health or training measurements. */
internal fun studioCoachReadinessText(readiness: NativeCoachReadiness.Result?): String = when {
    readiness?.pain == true -> "Fájdalomjelzés miatt ne emeld a terhelést. Nézd meg a gyakorlatonkénti korlátozásokat."
    readiness?.score == null -> "Nincs elegendő friss adat a készenlét megítéléséhez. Frissítsd a kiválasztott egészségforrást."
    readiness.mode == "light" -> "Könnyített terhelés javasolt. A gyakorlatoknál láthatók az előzmények alapján számolt részletek."
    readiness.mode == "reduced" -> "Visszafogott terhelés javasolt; most ne erőltesd az emelést."
    !readiness.hasRecoverySignal -> "Terhelésalapú becslés. Nincs elegendő friss alvás/HRV-adat a regeneráció külön megítéléséhez."
    else -> "A Coach szerint tartható a megszokott terhelés. Kövesd a gyakorlatonkénti javaslatokat."
}
