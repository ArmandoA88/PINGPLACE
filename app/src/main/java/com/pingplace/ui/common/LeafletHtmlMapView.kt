package com.pingplace.ui.common

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletHtmlMapView(
    modifier: Modifier = Modifier,
    html: String
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.loadsImagesAutomatically = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d("PingPlaceMap", "page finished: $url")
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Log.e(
                            "PingPlaceMap",
                            "resource error: ${request?.url} code=${error?.errorCode} desc=${error?.description}"
                        )
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        Log.e(
                            "PingPlaceMap",
                            "http error: ${request?.url} status=${errorResponse?.statusCode}"
                        )
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        Log.d(
                            "PingPlaceMap",
                            "${consoleMessage.messageLevel()}: ${consoleMessage.message()} " +
                                "@${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}"
                        )
                        return true
                    }
                }
                setBackgroundColor(Color.TRANSPARENT)
                loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
        }
    )
}
