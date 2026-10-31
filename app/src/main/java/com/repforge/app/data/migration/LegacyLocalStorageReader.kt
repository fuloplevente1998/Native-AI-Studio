package com.repforge.app.data.migration

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Read the old Capacitor origin through Chromium, including updates and deletions.
 * This offscreen reader is migration-only; the application UI remains Compose.
 * No network, JavaScript bridge, or write to localStorage is permitted here.
 */
internal class LegacyLocalStorageReader(private val context: Context) {
    suspend fun read(): Map<String, String> = withContext(Dispatchers.Main) {
        val dataDir = context.applicationInfo.dataDir
        val hasLegacyWebStorage = if (dataDir != null) {
            val dir1 = java.io.File(dataDir, "app_webview/Default/Local Storage/leveldb")
            val dir2 = java.io.File(dataDir, "app_webview/Local Storage/leveldb")
            (dir1.exists() && (dir1.list()?.isNotEmpty() == true)) ||
            (dir2.exists() && (dir2.list()?.isNotEmpty() == true))
        } else false

        if (!hasLegacyWebStorage) {
            return@withContext emptyMap()
        }

        try {
            val view = WebView(context)
            try {
                view.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                view.settings.javaScriptEnabled = true
                view.settings.domStorageEnabled = true
                view.settings.allowFileAccess = false
                view.settings.allowContentAccess = false
                withTimeout(8_000) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        view.webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(v: WebView, request: WebResourceRequest): WebResourceResponse =
                                WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                            override fun onPageFinished(v: WebView, url: String) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                            override fun onRenderProcessGone(v: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                                runCatching {
                                    val parent = v.parent as? android.view.ViewGroup
                                    parent?.removeView(v)
                                    v.stopLoading()
                                    v.destroy()
                                }
                                if (continuation.isActive) {
                                    continuation.resumeWithException(IllegalStateException("WebView renderer process terminated"))
                                }
                                return true
                            }
                        }
                        view.loadDataWithBaseURL("https://localhost/", "<html></html>", "text/html", "UTF-8", null)
                    }
                    val keys = JSONArray(evaluate(view, "JSON.stringify(Object.keys(localStorage).filter(k=>k.startsWith('repforge:')||k.startsWith('trainpilot:')))") as String)
                    val out = linkedMapOf<String, String>()
                    for (i in 0 until keys.length()) {
                        val key = keys.getString(i)
                        val length = (evaluate(view, "localStorage.getItem(${org.json.JSONObject.quote(key)})?.length??-1") as Number).toInt()
                        if (length < 0) continue
                        val value = StringBuilder(length)
                        var offset = 0
                        while (offset < length) {
                            value.append(evaluate(view, "localStorage.getItem(${org.json.JSONObject.quote(key)}).slice($offset,${offset + 16384})") as String)
                            offset += 16384
                        }
                        out[key] = value.toString()
                    }
                    out
                }
            } finally {
                runCatching {
                    val parent = view.parent as? android.view.ViewGroup
                    parent?.removeView(view)
                    view.stopLoading()
                    view.destroy()
                }
            }
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    private suspend fun evaluate(view: WebView, script: String): Any = suspendCancellableCoroutine { continuation ->
        view.evaluateJavascript(script) { encoded ->
            if (continuation.isActive) {
                try {
                    val value = JSONArray("[$encoded]").get(0)
                    check(value !== org.json.JSONObject.NULL) { "Unable to read legacy storage" }
                    continuation.resume(value)
                } catch (failure: Exception) {
                    continuation.resumeWithException(failure)
                }
            }
        }
    }
}
