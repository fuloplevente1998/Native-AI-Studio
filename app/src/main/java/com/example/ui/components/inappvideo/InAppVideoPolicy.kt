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

fun embeddedYouTubeError(code: Int): String = when (code) {
    2 -> "A YouTube hibás videóazonosítót jelzett."
    5 -> "A készülék beágyazott lejátszója nem tudja lejátszani ezt a videót."
    100 -> "A videó eltávolításra került vagy nem elérhető."
    101, 150 -> "A videó tulajdonosa nem engedélyezi a beágyazott lejátszást."
    153 -> "A YouTube nem fogadta el a beágyazott lejátszó azonosítását."
    else -> "A YouTube lejátszója hibát jelzett (kód: $code)."
}

/** HTTPS app identity used for both the WebView referrer and IFrame API origin. */
const val EXERCISE_VIDEO_ORIGIN = "https://com.trainpilot.com"

/** A late API-ready/paused callback must not hide a network/player failure. */
fun exerciseVideoPlayerEvent(current: ExerciseVideoLoad, event: String): ExerciseVideoLoad {
    if (current is ExerciseVideoLoad.Failed) return current
    return when {
        event == "ready" -> if (current == ExerciseVideoLoad.Loading) ExerciseVideoLoad.Ready else current
        event == "starting" -> ExerciseVideoLoad.Starting
        event == "playing" -> ExerciseVideoLoad.Playing
        event == "paused" -> if (current == ExerciseVideoLoad.Playing || current == ExerciseVideoLoad.Starting)
            ExerciseVideoLoad.Ready else current
        event == "blocked" -> ExerciseVideoLoad.Failed(
            "A YouTube blokkolta az indítást. Próbáld a belső böngészőt."
        )
        event.startsWith("error:") -> ExerciseVideoLoad.Failed(
            embeddedYouTubeError(event.removePrefix("error:").toIntOrNull() ?: 0)
        )
        else -> current
    }
}

/**
 * No native JavaScript bridge. The selected video gets a visible, trusted HTML
 * play gesture; API-ready alone never means media is playing. The API constructs
 * the iframe after its script is ready (rather than attaching to a racing iframe).
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
            #player { position:absolute; inset:0; width:100%; height:100%; border:0; }
            #cover { position:absolute; inset:0; display:flex; align-items:center;
              justify-content:center; background:#111 url('https://i.ytimg.com/vi/$videoId/hqdefault.jpg') center/cover; }
            #cover[hidden] { display:none; }
            #play { font:600 18px sans-serif; color:#fff; background:#a30000;
              border:2px solid #fff; border-radius:24px; padding:14px 22px; cursor:pointer; }
            #play:disabled { background:#333; cursor:wait; }
          </style>
        </head>
        <body>
          <div id="player"></div>
          <div id="cover"><button id="play" type="button" disabled>Betöltés…</button></div>
          <script>
            var tpPlayer = null;
            var tpFailed = false;
            var tpButton = document.getElementById("play");
            function tpReport(event) {
              if (tpFailed) return;
              if (event.indexOf("error:") === 0 || event === "blocked") {
                tpFailed = true;
                tpButton.disabled = true;
                tpButton.textContent = "A videó nem indítható";
              }
              document.title = "trainpilot-video:" + event;
              console.log("trainpilot-video:" + event);
            }
            tpButton.addEventListener("click", function () {
              if (!tpPlayer || tpFailed) return;
              tpButton.disabled = true;
              tpButton.textContent = "Indítás…";
              tpReport("starting");
              try { tpPlayer.playVideo(); } catch (e) { tpReport("error:0"); }
            });
            function onYouTubeIframeAPIReady() {
              try {
                tpPlayer = new YT.Player("player", {
                  host: "https://www.youtube-nocookie.com",
                  width: "100%", height: "100%", videoId: "$videoId",
                  playerVars: { playsinline: 1, controls: 1, origin: "$EXERCISE_VIDEO_ORIGIN" },
                  events: {
                    onReady: function () {
                      if (tpFailed) return;
                      tpButton.disabled = false;
                      tpButton.textContent = "▶ Lejátszás";
                      tpReport("ready");
                    },
                    onStateChange: function (event) {
                      if (tpFailed) return;
                      if (event.data === 1) {
                        document.getElementById("cover").hidden = true;
                        tpReport("playing");
                      }
                      if (event.data === 0 || event.data === 2) {
                        if (!document.getElementById("cover").hidden) {
                          tpButton.disabled = false;
                          tpButton.textContent = "▶ Lejátszás";
                        }
                        tpReport("paused");
                      }
                    },
                    onAutoplayBlocked: function () { tpReport("blocked"); },
                    onError: function (event) { tpReport("error:" + event.data); }
                  }
                });
              } catch (e) { tpReport("error:0"); }
            }
            function tpPause() {
              try { if (tpPlayer && tpPlayer.pauseVideo) tpPlayer.pauseVideo(); } catch(e) {}
            }
          </script>
          <script src="https://www.youtube.com/iframe_api" onerror="tpReport('error:0')"></script>
        </body>
        </html>
    """.trimIndent()
}
