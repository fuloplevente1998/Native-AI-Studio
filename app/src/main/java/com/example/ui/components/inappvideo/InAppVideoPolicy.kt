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
    data object Playing : ExerciseVideoLoad
    data class Failed(val message: String) : ExerciseVideoLoad
}

fun allowedInAppVideoNavigation(url: String): Boolean =
    safeExternalExerciseVideoUrl(url) == url

fun embeddedYouTubeError(code: Int): String = when (code) {
    2 -> "A YouTube hibás videóazonosítót jelzett."
    5 -> "A készülék beágyazott lejátszója nem tudja lejátszani ezt a videót."
    100 -> "A videó eltávolításra került vagy nem elérhető."
    101, 150 -> "A videó tulajdonosa nem engedélyezi a beágyazott lejátszást."
    153 -> "A YouTube nem fogadta el a beágyazott lejátszó azonosítását."
    else -> "A YouTube lejátszója hibát jelzett (kód: $code)."
}

/**
 * No JavaScriptInterface/native bridge: the owned document only reports
 * status through its page title. Video ID is validated before interpolation.
 */
fun exerciseYouTubeEmbedHtml(videoId: String): String {
    require(Regex("^[A-Za-z0-9_-]{11}$").matches(videoId))
    return """
        <!doctype html>
        <html lang="hu">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
          <style>
            html,body,#player { margin:0; width:100%; height:100%; background:#000; overflow:hidden; }
          </style>
        </head>
        <body>
          <iframe id="player"
            src="https://www.youtube-nocookie.com/embed/$videoId?enablejsapi=1&amp;playsinline=1&amp;origin=https%3A%2F%2Fappassets.androidplatform.net"
            title="Oktatóvideó" frameborder="0" allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
            allowfullscreen></iframe>
          <script>
            var tpPlayer = null;
            function tpReport(event) {
              document.title = "trainpilot-video:" + event;
              console.log("trainpilot-video:" + event);
            }
            function onYouTubeIframeAPIReady() {
              try {
                tpPlayer = new YT.Player("player", { events: {
                  onReady: function () { tpReport("ready"); },
                  onStateChange: function (event) {
                    if (event.data === 1) tpReport("playing");
                    if (event.data === 2) tpReport("paused");
                  },
                  onError: function (event) { tpReport("error:" + event.data); }
                }});
              } catch (e) { tpReport("error:0"); }
            }
            function tpPause() {
              try { if (tpPlayer && tpPlayer.pauseVideo) tpPlayer.pauseVideo(); } catch(e) {}
            }
          </script>
          <script src="https://www.youtube.com/iframe_api"></script>
        </body>
        </html>
    """.trimIndent()
}
