package com.local.browser

import android.app.Application
import android.util.Log
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewOutcomeReceiver
import androidx.webkit.WebViewStartUpConfig
import androidx.webkit.WebViewStartUpResult
import androidx.webkit.WebViewStartupException
import java.util.concurrent.Executors

/** Kicks off WebView startup on a background thread as early as possible. */
class BrowserApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // WebView is on the critical path here (MainActivity builds one right away),
        // so per the docs we fire startUpWebView early but do NOT wait for the
        // callback — touching WebView APIs immediately would block the UI thread
        // waiting for init and waste the benefit.
        val startUpConfig = WebViewStartUpConfig.Builder(
            Executors.newSingleThreadExecutor()
        ).build()

        WebViewCompat.startUpWebView(
            this,
            startUpConfig,
            object : WebViewOutcomeReceiver<WebViewStartUpResult, WebViewStartupException> {
                override fun onResult(result: WebViewStartUpResult) {
                    // UI thread. Result also carries getUiThreadBlockingStartUpLocations()
                    // for debugging if implicit startup ever beats us to it.
                    Log.d("LocalBrowser", "WebView async startup finished")
                }

                override fun onError(error: WebViewStartupException) {
                    // Not fatal: the WebView will just fall back to normal startup.
                    Log.w("LocalBrowser", "WebView async startup failed", error)
                }
            }
        )
    }
}
