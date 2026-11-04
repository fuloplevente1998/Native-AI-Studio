package com.example.ui.components.inappvideo

import com.example.ui.components.safeExternalExerciseVideoUrl

/**
 * An embedded player uses a fixed video ID; browser mode only accepts an
 * explicitly selected HTTPS URL. Neither mode dispatches implicit intents.
 */
enum class ExerciseVideoMode { EMBED, BROWSER }

sealed interface ExerciseVideoLoad {
    data object Loading : ExerciseVideoLoad
    data object Ready : ExerciseVideoLoad
    data object Starting : ExerciseVideoLoad
    data object Playing : ExerciseVideoLoad
    data class Failed(val message: String) : ExerciseVideoLoad
}

fun allowedInAppVideoNavigation(url: String): Boolean =
    safeExternalExerciseVideoUrl(url) == url

fun embeddedYouTubeError(code: Int, language: String = "hu"): String = when (language.lowercase()) {
    "en" -> when (code) {
        2 -> "YouTube reported an invalid video ID."
        5 -> "The embedded HTML5 player cannot play this video on this device."
        100 -> "This video was removed or is unavailable."
        101, 150 -> "The video owner does not allow embedded playback."
        153 -> "YouTube rejected the embedded player identification."
        else -> "The YouTube player reported an error (code: $code)."
    }
    "de" -> when (code) {
        2 -> "YouTube hat eine ungültige Video-ID gemeldet."
        5 -> "Der eingebettete Player kann dieses Video auf diesem Gerät nicht abspielen."
        100 -> "Das Video wurde entfernt oder ist nicht verfügbar."
        101, 150 -> "Der Videoeigentümer erlaubt keine eingebettete Wiedergabe."
        153 -> "YouTube hat die Identifizierung des eingebetteten Players abgelehnt."
        else -> "Der YouTube-Player hat einen Fehler gemeldet (Code: $code)."
    }
    "ro" -> when (code) {
        2 -> "YouTube a raportat un ID video invalid."
        5 -> "Playerul încorporat nu poate reda acest videoclip pe dispozitiv."
        100 -> "Videoclipul a fost eliminat sau nu este disponibil."
        101, 150 -> "Proprietarul videoclipului nu permite redarea încorporată."
        153 -> "YouTube a respins identificarea playerului încorporat."
        else -> "Playerul YouTube a raportat o eroare (cod: $code)."
    }
    else -> when (code) {
        2 -> "A YouTube hibás videóazonosítót jelzett."
        5 -> "A készülék beágyazott lejátszója nem tudja lejátszani ezt a videót."
        100 -> "A videó eltávolításra került vagy nem elérhető."
        101, 150 -> "A videó tulajdonosa nem engedélyezi a beágyazott lejátszást."
        153 -> "A YouTube nem fogadta el a beágyazott lejátszó azonosítását."
        else -> "A YouTube lejátszója hibát jelzett (kód: $code)."
    }
}

/** HTTPS app identity used for both the WebView referrer and IFrame API origin. */
const val EXERCISE_VIDEO_ORIGIN = "https://com.trainpilot.com"

fun shouldAutoFallbackToBrowser(
    mode: ExerciseVideoMode,
    load: ExerciseVideoLoad,
    externalUrl: String?
): Boolean = mode == ExerciseVideoMode.EMBED &&
    load is ExerciseVideoLoad.Failed &&
    !externalUrl.isNullOrBlank() &&
    allowedInAppVideoNavigation(externalUrl)

/** A late API-ready/paused callback must not hide a network/player failure. */
fun exerciseVideoPlayerEvent(
    current: ExerciseVideoLoad,
    event: String,
    language: String = "hu"
): ExerciseVideoLoad {
    if (current is ExerciseVideoLoad.Failed) return current
    return when {
        event == "frame-ready" -> current
        event == "ready" -> if (current == ExerciseVideoLoad.Loading) ExerciseVideoLoad.Ready else current
        event == "starting" -> ExerciseVideoLoad.Starting
        event == "playing" -> ExerciseVideoLoad.Playing
        event == "paused" -> if (current == ExerciseVideoLoad.Playing || current == ExerciseVideoLoad.Starting)
            ExerciseVideoLoad.Ready else current
        event == "blocked" -> ExerciseVideoLoad.Failed(
            when (language.lowercase()) {
                "en" -> "YouTube blocked playback start. Try the in-app browser."
                "de" -> "YouTube hat den Wiedergabestart blockiert. Versuche den In-App-Browser."
                "ro" -> "YouTube a blocat pornirea redării. Încearcă browserul intern."
                else -> "A YouTube blokkolta az indítást. Próbáld a belső böngészőt."
            }
        )
        event == "observer-unavailable" -> ExerciseVideoLoad.Failed(
            when (language.lowercase()) {
                "en" -> "Embedded YouTube player API is unavailable. Switching to in-app browser."
                "de" -> "Eingebettete YouTube-Player-API nicht verfügbar. Wechsel zum In-App-Browser."
                "ro" -> "API-ul playerului YouTube încorporat nu este disponibil. Se comută la browserul intern."
                else -> "A beágyazott YouTube lejátszó nem érhető el. Váltás a belső böngészőre."
            }
        )
        event.startsWith("error:") -> ExerciseVideoLoad.Failed(
            embeddedYouTubeError(event.removePrefix("error:").toIntOrNull() ?: 0, language)
        )
        else -> current
    }
}

/**
 * Same direct YouTube iframe/native controls as TrainPilot WebView main
 * 5f026a5, app.js openDemo. JS only observes state and pauses on lifecycle exit;
 * it never calls playVideo or overlays a second play button across the iframe.
 */
fun exerciseYouTubeEmbedHtml(videoId: String): String {
    require(Regex("^[A-Za-z0-9_-]{11}$").matches(videoId))
    return """
        <!doctype html>
        <html lang="hu">
        <head>
          <meta charset="utf-8">
          <meta name="referrer" content="strict-origin-when-cross-origin">
          <meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
          <style>
            html,body { margin:0; width:100%; height:100%; background:#000; overflow:hidden; }
            #player { display:block; width:100%; height:100%; border:0; }
          </style>
        </head>
        <body>
          <iframe id="player" title="Oktatóvideó"
            src="https://www.youtube.com/embed/$videoId?playsinline=1&amp;rel=0&amp;controls=1&amp;autoplay=0&amp;enablejsapi=1&amp;origin=https%3A%2F%2Fcom.trainpilot.com"
            allow="autoplay; encrypted-media; fullscreen; picture-in-picture"
            referrerpolicy="strict-origin-when-cross-origin" onload="tpReport('frame-ready')" allowfullscreen></iframe>
          <script>
            var tpPlayer = null;
            var tpFailed = false;
            function tpReport(event) {
              if (tpFailed) return;
              if (event.indexOf("error:") === 0 || event === "blocked") tpFailed = true;
              document.title = "trainpilot-video:" + event;
              console.log("trainpilot-video:" + event);
            }
            function onYouTubeIframeAPIReady() {
              try {
                tpPlayer = new YT.Player("player", { events: {
                  onReady: function () { tpReport("ready"); },
                  onStateChange: function (event) {
                    if (event.data === 3) tpReport("starting");
                    if (event.data === 1) tpReport("playing");
                    if (event.data === 0 || event.data === 2) tpReport("paused");
                  },
                  onAutoplayBlocked: function () { tpReport("blocked"); },
                  onError: function (event) { tpReport("error:" + event.data); }
                }});
              } catch (e) { tpReport("observer-unavailable"); }
            }
            function tpPause() {
              try {
                if (tpPlayer && tpPlayer.pauseVideo) tpPlayer.pauseVideo();
                else document.getElementById("player").contentWindow.postMessage(
                  JSON.stringify({ event: "command", func: "pauseVideo", args: [] }), "https://www.youtube.com");
              } catch(e) {}
            }
          </script>
          <script src="https://www.youtube.com/iframe_api" onerror="tpReport('observer-unavailable')"></script>
        </body>
        </html>
    """.trimIndent()
}
