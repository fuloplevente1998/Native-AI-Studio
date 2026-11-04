package com.example.ui.components.inappvideo

import android.app.Dialog
import android.graphics.Color
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.CookieManager
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Sandboxed Android WebView. No external intents or native JS bridge. */
@Composable
fun SecureExerciseVideoWebView(
    mode: ExerciseVideoMode,
    url: String,
    youtubeId: String?,
    retryKey: Int,
    modifier: Modifier = Modifier,
    language: String = "hu",
    onLoad: (ExerciseVideoLoad) -> Unit,
    onLocationChanged: (String) -> Unit,
    onHistoryChanged: (Boolean) -> Unit,
    onViewChanged: (WebView?) -> Unit,
    onSwitchToBrowser: (String) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestLoad by rememberUpdatedState(onLoad)
    val latestLocation by rememberUpdatedState(onLocationChanged)
    val latestHistory by rememberUpdatedState(onHistoryChanged)
    val latestView by rememberUpdatedState(onViewChanged)
    val latestSwitchToBrowser by rememberUpdatedState(onSwitchToBrowser)
    val latestLang by rememberUpdatedState(language)
    var currentView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(lifecycleOwner, currentView, mode) {
        val webView = currentView
        if (webView == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                        if (mode == ExerciseVideoMode.EMBED) {
                            webView.evaluateJavascript("tpPause();", null)
                        } else {
                            webView.evaluateJavascript(
                                "document.querySelectorAll('video').forEach(function(v){v.pause()});",
                                null
                            )
                        }
                        webView.onPause()
                    }
                    Lifecycle.Event.ON_RESUME -> webView.onResume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    key(mode, url, youtubeId, retryKey) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.BLACK)
                    settings.apply {
                        javaScriptEnabled = true // Needed by YouTube IFrame API.
                        domStorageEnabled = true
                        // Match Capacitor Bridge.setupWebView() used by the working WebView app.
                        // The owned embed has autoplay=0 and never calls playVideo itself.
                        mediaPlaybackRequiresUserGesture = mode != ExerciseVideoMode.EMBED
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        textZoom = 100
                        setSupportZoom(mode == ExerciseVideoMode.BROWSER)
                        builtInZoomControls = mode == ExerciseVideoMode.BROWSER
                        displayZoomControls = false
                        allowFileAccess = false
                        allowContentAccess = false
                        allowFileAccessFromFileURLs = false
                        allowUniversalAccessFromFileURLs = false
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        safeBrowsingEnabled = true
                    }

                    // The selected video's iframe is a third-party origin. Without this,
                    // YouTube consent/session cookies may fail in Android WebView.
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    var playerLoad: ExerciseVideoLoad = ExerciseVideoLoad.Loading
                    var loadFailed = false
                    fun failure(message: String) {
                        loadFailed = true
                        playerLoad = ExerciseVideoLoad.Failed(message)
                        latestLoad(playerLoad)
                    }
                    fun processPlayerEvent(value: String) {
                        if (mode != ExerciseVideoMode.EMBED ||
                            !value.startsWith("trainpilot-video:")) return
                        val next = exerciseVideoPlayerEvent(
                            playerLoad,
                            value.removePrefix("trainpilot-video:"),
                            latestLang
                        )
                        if (next != playerLoad) {
                            playerLoad = next
                            if (next is ExerciseVideoLoad.Failed) loadFailed = true
                            latestLoad(next)
                        }
                    }

                    var fullscreenDialog: Dialog? = null
                    webChromeClient = object : WebChromeClient(), VideoCleanup {
                        override fun closeFullscreen() { fullscreenDialog?.dismiss() }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            if (title != null) processPlayerEvent(title)
                        }
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.message()?.let(::processPlayerEvent)
                            return true
                        }
                        override fun onPermissionRequest(request: PermissionRequest) {
                            request.deny()
                        }
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?, callback: GeolocationPermissions.Callback?
                        ) {
                            callback?.invoke(origin, false, false)
                        }
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (view == null || callback == null || fullscreenDialog != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            (view.parent as? ViewGroup)?.removeView(view)
                            val container = FrameLayout(context).apply {
                                setBackgroundColor(Color.BLACK)
                                addView(view, FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                ))
                            }
                            val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                            dialog.setContentView(container)
                            dialog.setOnDismissListener {
                                container.removeView(view)
                                callback.onCustomViewHidden()
                                fullscreenDialog = null
                            }
                            fullscreenDialog = dialog
                            dialog.show()
                        }
                        override fun onHideCustomView() { closeFullscreen() }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            if (mode == ExerciseVideoMode.EMBED &&
                                youtubeId != null &&
                                request.url.toString().trimEnd('/') == EXERCISE_VIDEO_ORIGIN
                            ) {
                                return WebResourceResponse(
                                    "text/html",
                                    "UTF-8",
                                    exerciseYouTubeEmbedHtml(youtubeId).byteInputStream(Charsets.UTF_8)
                                )
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView, request: WebResourceRequest
                        ): Boolean {
                            val next = request.url.toString()
                            val scheme = request.url.scheme?.lowercase(java.util.Locale.ROOT).orEmpty()
                            if (mode == ExerciseVideoMode.EMBED) {
                                if (next.trimEnd('/') == EXERCISE_VIDEO_ORIGIN) return false
                                if (!request.isForMainFrame && isYouTubeEmbedFrame(mode, request)) return false
                                if (allowedInAppVideoNavigation(next)) {
                                    latestSwitchToBrowser(next)
                                    return true
                                }
                                if (scheme in setOf("intent", "vnd.youtube") && allowedInAppVideoNavigation(url)) {
                                    latestSwitchToBrowser(url)
                                    return true
                                }
                                failure(localizedWebViewError("disallowed_nav", latestLang))
                                return true
                            }
                            if (!request.isForMainFrame) {
                                return !allowedInAppVideoNavigation(next)
                            }
                            if (scheme in setOf("intent", "vnd.youtube")) {
                                return true
                            }
                            if (!allowedInAppVideoNavigation(next)) {
                                failure(localizedWebViewError("disallowed_nav", latestLang))
                                return true
                            }
                            latestLocation(next)
                            return false
                        }
                        override fun onPageStarted(
                            view: WebView, url: String?, favicon: android.graphics.Bitmap?
                        ) {
                            if (mode == ExerciseVideoMode.BROWSER) {
                                loadFailed = false
                                latestLoad(ExerciseVideoLoad.Loading)
                                latestHistory(view.canGoBack())
                                if (url != null && allowedInAppVideoNavigation(url)) latestLocation(url)
                            }
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            if (mode == ExerciseVideoMode.BROWSER) {
                                latestHistory(view.canGoBack())
                                if (url != null && allowedInAppVideoNavigation(url)) latestLocation(url)
                                if (!loadFailed) latestLoad(ExerciseVideoLoad.Ready)
                            }
                        }
                        override fun onReceivedError(
                            view: WebView, request: WebResourceRequest, error: WebResourceError
                        ) {
                            if (request.isForMainFrame || isYouTubeEmbedFrame(mode, request)) {
                                failure(localizedWebViewError("network", latestLang))
                            }
                        }
                        override fun onReceivedHttpError(
                            view: WebView, request: WebResourceRequest, response: WebResourceResponse
                        ) {
                            if ((request.isForMainFrame || isYouTubeEmbedFrame(mode, request)) &&
                                response.statusCode >= 400) {
                                failure(localizedWebViewError("http", latestLang, response.statusCode))
                            }
                        }
                        override fun onReceivedSslError(
                            view: WebView, handler: SslErrorHandler, error: SslError
                        ) {
                            handler.cancel()
                            failure(localizedWebViewError("ssl", latestLang))
                        }
                    }

                    if (mode == ExerciseVideoMode.EMBED && youtubeId != null) {
                        loadUrl("$EXERCISE_VIDEO_ORIGIN/")
                    } else if (mode == ExerciseVideoMode.BROWSER && allowedInAppVideoNavigation(url)) {
                        loadUrl(url)
                    } else {
                        failure(localizedWebViewError("invalid_url", latestLang))
                    }
                    currentView = this
                    latestView(this)
                }
            },
            onRelease = { webView ->
                if (currentView === webView) {
                    currentView = null
                    latestView(null)
                }
                (webView.webChromeClient as? VideoCleanup)?.closeFullscreen()
                webView.stopLoading()
                webView.onPause()
                webView.removeAllViews()
                webView.destroy()
            }
        )
    }
}

private fun localizedWebViewError(kind: String, language: String, statusCode: Int = 0): String =
    when (language.lowercase(java.util.Locale.ROOT)) {
        "en" -> when (kind) {
            "disallowed_nav" -> "The player attempted an unpermitted navigation."
            "network" -> "Network or player error. Try the in-app browser."
            "http" -> "The video page returned HTTP $statusCode. Try the in-app browser."
            "ssl" -> "Invalid HTTPS certificate. Video loading was aborted."
            else -> "Unpermitted or missing HTTPS video link."
        }
        "de" -> when (kind) {
            "disallowed_nav" -> "Der Player hat eine nicht erlaubte Navigation versucht."
            "network" -> "Netzwerk- oder Playerfehler. Versuche den In-App-Browser."
            "http" -> "Die Videoseite meldete HTTP $statusCode. Versuche den In-App-Browser."
            "ssl" -> "Ungültiges HTTPS-Zertifikat. Das Laden des Videos wurde abgebrochen."
            else -> "Nicht erlaubter oder fehlender HTTPS-Videolink."
        }
        "ro" -> when (kind) {
            "disallowed_nav" -> "Playerul a încercat o navigare nepermisă."
            "network" -> "Eroare de rețea sau player. Încearcă browserul intern."
            "http" -> "Pagina video a returnat HTTP $statusCode. Încearcă browserul intern."
            "ssl" -> "Certificat HTTPS invalid. Încărcarea videoclipului a fost oprită."
            else -> "Link video HTTPS nepermis sau lipsă."
        }
        else -> when (kind) {
            "disallowed_nav" -> "A lejátszó nem engedélyezett navigációt próbált megnyitni."
            "network" -> "Hálózati vagy lejátszóhiba. Próbáld a belső böngészőt."
            "http" -> "A videóoldal HTTP $statusCode hibát adott. Próbáld a belső böngészőt."
            "ssl" -> "Hibás HTTPS-tanúsítvány. A videó betöltését megszakítottuk."
            else -> "Nem engedélyezett vagy hiányzó HTTPS-videóhivatkozás."
        }
    }

private fun isYouTubeEmbedFrame(
    mode: ExerciseVideoMode, request: WebResourceRequest
): Boolean = mode == ExerciseVideoMode.EMBED &&
    request.url.host.orEmpty().lowercase(java.util.Locale.ROOT) in setOf("www.youtube.com", "www.youtube-nocookie.com") &&
    request.url.path.orEmpty().startsWith("/embed/")

private interface VideoCleanup {
    fun closeFullscreen()
}
