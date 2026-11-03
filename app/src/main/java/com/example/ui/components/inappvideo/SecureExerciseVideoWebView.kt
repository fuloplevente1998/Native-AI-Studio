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
    onLoad: (ExerciseVideoLoad) -> Unit,
    onLocationChanged: (String) -> Unit,
    onHistoryChanged: (Boolean) -> Unit,
    onViewChanged: (WebView?) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestLoad by rememberUpdatedState(onLoad)
    val latestLocation by rememberUpdatedState(onLocationChanged)
    val latestHistory by rememberUpdatedState(onHistoryChanged)
    val latestView by rememberUpdatedState(onViewChanged)
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
                        mediaPlaybackRequiresUserGesture = true
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
                        val next = exerciseVideoPlayerEvent(playerLoad, value.removePrefix("trainpilot-video:"))
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
                        override fun shouldOverrideUrlLoading(
                            view: WebView, request: WebResourceRequest
                        ): Boolean {
                            if (!request.isForMainFrame) return false
                            val next = request.url.toString()
                            if (mode == ExerciseVideoMode.EMBED ||
                                !allowedInAppVideoNavigation(next)) {
                                failure("A lejátszó nem engedélyezett navigációt próbált megnyitni.")
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
                                failure("Hálózati vagy lejátszóhiba. Próbáld a belső böngészőt.")
                            }
                        }
                        override fun onReceivedHttpError(
                            view: WebView, request: WebResourceRequest, response: WebResourceResponse
                        ) {
                            if ((request.isForMainFrame || isYouTubeEmbedFrame(mode, request)) &&
                                response.statusCode >= 400) {
                                failure("A videóoldal HTTP " + response.statusCode +
                                    " hibát adott. Próbáld a belső böngészőt.")
                            }
                        }
                        override fun onReceivedSslError(
                            view: WebView, handler: SslErrorHandler, error: SslError
                        ) {
                            handler.cancel()
                            failure("Hibás HTTPS-tanúsítvány. A videó betöltését megszakítottuk.")
                        }
                    }

                    if (mode == ExerciseVideoMode.EMBED && youtubeId != null) {
                        loadDataWithBaseURL(
                            "$EXERCISE_VIDEO_ORIGIN/",
                            exerciseYouTubeEmbedHtml(youtubeId), "text/html", "UTF-8", null
                        )
                    } else if (mode == ExerciseVideoMode.BROWSER && allowedInAppVideoNavigation(url)) {
                        loadUrl(url)
                    } else {
                        failure("Nem engedélyezett vagy hiányzó HTTPS-videóhivatkozás.")
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

private fun isYouTubeEmbedFrame(
    mode: ExerciseVideoMode, request: WebResourceRequest
): Boolean = mode == ExerciseVideoMode.EMBED &&
    request.url.host.orEmpty().equals("www.youtube-nocookie.com", ignoreCase = true) &&
    request.url.path.orEmpty().startsWith("/embed/")

private interface VideoCleanup {
    fun closeFullscreen()
}
