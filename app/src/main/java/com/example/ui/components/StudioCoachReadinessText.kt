package com.example.ui.components

import com.example.model.AppLanguage
import com.repforge.app.ui.coach.NativeCoachReadiness

/** Presentation of the canonical result; never infer new Health or training measurements. */
internal fun studioCoachReadinessText(
    readiness: NativeCoachReadiness.Result?,
    language: AppLanguage = AppLanguage.HU
): String {
    fun tr(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }
    return when {
        readiness?.pain == true -> tr(
            "Fájdalomjelzés miatt ne emeld a terhelést. Nézd meg a gyakorlatonkénti korlátozásokat.",
            "Do not increase load due to reported pain. Check the per-exercise restrictions.",
            "Wegen Schmerzsignalen Belastung nicht erhöhen. Siehe die übungsbezogenen Einschränkungen.",
            "Nu crește sarcina din cauza semnalării durerii. Verifică restricțiile pe exerciții."
        )
        readiness?.score == null -> tr(
            "Nincs elegendő friss adat a készenlét megítéléséhez. Frissítsd a kiválasztott egészségforrást.",
            "Not enough recent data to evaluate readiness. Refresh your selected health source.",
            "Nicht genügend aktuelle Daten zur Bewertung der Bereitschaft. Aktualisiere deine Gesundheitsquelle.",
            "Nu există suficiente date recente pentru evaluarea pregătirii. Actualizează sursa de sănătate selectată."
        )
        readiness.mode == "light" -> tr(
            "Könnyített terhelés javasolt. A gyakorlatoknál láthatók az előzmények alapján számolt részletek.",
            "Lighter load is advised. Per-exercise details calculated from history are shown below.",
            "Leichtere Belastung empfohlen. Bei den Übungen sind die verlaufsbasierten Details sichtbar.",
            "Se recomandă o sarcină mai ușoară. Detaliile calculate din istoric sunt afișate la exerciții."
        )
        readiness.mode == "reduced" -> tr(
            "Visszafogott terhelés javasolt; most ne erőltesd az emelést.",
            "Moderate load is advised; do not force weight increases right now.",
            "Reduzierte Belastung empfohlen; erzwinge jetzt keine Steigerung.",
            "Se recomandă o sarcină moderată; nu forța creșterea greutății acum."
        )
        !readiness.hasRecoverySignal -> tr(
            "Terhelésalapú becslés. Nincs elegendő friss alvás/HRV-adat a regeneráció külön megítéléséhez.",
            "Load-based estimate. Not enough recent sleep/HRV data to evaluate recovery separately.",
            "Belastungsbasierte Schätzung. Nicht genügend aktuelle Schlaf-/HRV-Daten für separate Erholung.",
            "Estimare bazată pe sarcină. Nu există suficiente date recente de somn/HRV pentru recuperare."
        )
        else -> tr(
            "A Coach szerint tartható a megszokott terhelés. Kövesd a gyakorlatonkénti javaslatokat.",
            "Coach indicates normal working loads can be maintained. Follow the per-exercise recommendations.",
            "Laut Coach kann die gewohnte Belastung gehalten werden. Folge den Übungsempfehlungen.",
            "Conform Coach, sarcina obișnuită poate fi menținută. Urmează recomandările pe exerciții."
        )
    }
}
