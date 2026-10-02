package com.local.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.webkit.*
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast

/** Localhost-only browser. Anything not on 127.0.0.1 / localhost / ::1 is blocked. */
class MainActivity : Activity() {

    private val defaultUrl = "http://127.0.0.1"
    private val allowed = setOf("localhost", "127.0.0.1", "[::1]", "::1")
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var web: WebView
    private lateinit var prefs: SharedPreferences
    private var target = defaultUrl
    private val retry = Runnable { web.loadUrl(target) }

    private fun ok(u: Uri) =
        u.scheme in setOf("data", "blob", "about") || u.host in allowed

    /** "39683" -> http://127.0.0.1:39683/ ; adds http:// if missing ; null if not local. */
    private fun normalize(input: String): String? {
        var t = input.trim()
        if (t.matches(Regex("\\d{2,5}"))) t = "127.0.0.1:$t"
        if (!t.contains("://")) t = "http://$t"
        val u = Uri.parse(t)
        return if ((u.scheme == "http" || u.scheme == "https") && u.host in allowed) t else null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        prefs = getSharedPreferences("lb", MODE_PRIVATE)
        target = prefs.getString("url", defaultUrl) ?: defaultUrl

        web = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            keepScreenOn = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                // Cache-first: serve the last-loaded page instantly from cache,
                // then let the network refresh it in the background when online.
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            }
            // No shouldInterceptRequest: it runs for every resource the page
            // requests and adds overhead. Outside navigation is blocked by
            // shouldOverrideUrlLoading below, and non-loopback cleartext traffic
            // is blocked by the network security config. Trade-off accepted: a
            // menu page may still pull HTTPS subresources from the internet.
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest) = !ok(r.url)

                // Server not up yet? Retry every 250 ms.
                override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                    if (r.isForMainFrame) {
                        handler.removeCallbacks(retry)
                        handler.postDelayed(retry, 250)
                    }
                }
            }
        }

        // Two small translucent buttons, top-right
        val d = resources.displayMetrics.density
        fun btn(label: String, onClick: () -> Unit) = Button(this).apply {
            text = label
            alpha = 0.45f
            minWidth = 0; minimumWidth = 0
            setPadding((10 * d).toInt(), 0, (10 * d).toInt(), 0)
            setOnClickListener { onClick() }
        }
        val bar = LinearLayout(this).apply {
            addView(btn("⟳") { web.clearCache(true); web.reload() })
            addView(btn("URL") { askUrl() })
        }
        val root = FrameLayout(this).apply {
            addView(web, FrameLayout.LayoutParams(-1, -1))
            addView(bar, FrameLayout.LayoutParams(-2, (40 * d).toInt(), Gravity.TOP or Gravity.END))
        }
        setContentView(root)
        web.loadUrl(target)
    }

    private fun askUrl() {
        val input = EditText(this).apply { setText(target); setSingleLine() }
        AlertDialog.Builder(this)
            .setTitle("Local URL or port")
            .setView(input)
            .setPositiveButton("Go") { _, _ ->
                val n = normalize(input.text.toString())
                if (n == null) {
                    Toast.makeText(this, "Local addresses only", Toast.LENGTH_SHORT).show()
                } else {
                    target = n
                    prefs.edit().putString("url", n).apply()
                    web.loadUrl(n)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onResume() { super.onResume(); web.onResume() }
    override fun onPause() { web.onPause(); super.onPause() }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        web.destroy()
        super.onDestroy()
    }
}
