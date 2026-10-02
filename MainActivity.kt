package com.local.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.*
import java.io.ByteArrayInputStream

class MainActivity : Activity() {
    // Change this to your client's menu URL
    private val start = "http://127.0.0.1:39683/resource/liquidbounce/"
    private val allowed = setOf("localhost", "127.0.0.1", "[::1]", "::1")
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var web: WebView

    private fun ok(u: Uri) =
        u.scheme in setOf("data", "blob", "about") || u.host in allowed

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        web = WebView(this)
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        web.keepScreenOn = true
        setContentView(web)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest) =
                !ok(r.url)

            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? =
                if (ok(r.url)) null else WebResourceResponse(
                    "text/plain", "utf-8", 403, "Blocked", emptyMap(),
                    ByteArrayInputStream(ByteArray(0))
                )

            // Server not up yet? Retry every 250 ms.
            override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                if (r.isForMainFrame) {
                    handler.postDelayed({ v.loadUrl(r.url.toString()) }, 250)
                }
            }
        }
        web.loadUrl(start)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        web.destroy()
        super.onDestroy()
    }
}
