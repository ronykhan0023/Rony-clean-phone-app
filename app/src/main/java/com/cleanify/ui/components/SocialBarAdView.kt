package com.cleanify.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val SOCIAL_BAR_BASE_URL = "https://bancadeltempoidea.org/"
private const val SOCIAL_BAR_SCRIPT_SRC =
    "https://bancadeltempoidea.org/14/25a7aa4d4354a84d64f5d4ed420a0b06"

private val SOCIAL_BAR_HTML = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <style>
            html, body {
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                overflow: hidden;
                background: transparent !important;
            }
        </style>
    </head>
    <body>
        <script data-cfasync="false" src="$SOCIAL_BAR_SCRIPT_SRC"></script>
    </body>
    </html>
""".trimIndent()

private fun isNetworkAvailable(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        false
    }
}

private fun openExternalAdUrl(context: Context, uri: Uri?) {
    if (uri == null) return
    val scheme = uri.scheme?.lowercase() ?: return
    if (scheme != "http" && scheme != "https") return
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SocialBarAdBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }
    var hasLoadError by remember { mutableStateOf(false) }

    val webView = remember {
        try {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowContentAccess = true
                    loadsImagesAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                    useWideViewPort = false
                    loadWithOverviewMode = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                val hostWebView = this
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(hostWebView, true)
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url ?: return false
                        val urlStr = url.toString()
                        if (urlStr == "about:blank" || urlStr == SOCIAL_BAR_BASE_URL) {
                            return false
                        }
                        if (request.hasGesture()) {
                            openExternalAdUrl(context, url)
                            return true
                        }
                        return false
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            hasLoadError = true
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
                        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                        val tempWebView = WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    v: WebView?,
                                    req: WebResourceRequest?
                                ): Boolean {
                                    openExternalAdUrl(context, req?.url)
                                    v?.destroy()
                                    return true
                                }
                            }
                        }
                        transport.webView = tempWebView
                        resultMsg.sendToTarget()
                        return true
                    }
                }

                if (isOnline) {
                    loadDataWithBaseURL(
                        SOCIAL_BAR_BASE_URL,
                        SOCIAL_BAR_HTML,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val nowOnline = isNetworkAvailable(context)
                    if (nowOnline && (!isOnline || hasLoadError)) {
                        isOnline = true
                        hasLoadError = false
                        webView?.loadDataWithBaseURL(
                            SOCIAL_BAR_BASE_URL,
                            SOCIAL_BAR_HTML,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    } else {
                        isOnline = nowOnline
                    }
                    webView?.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webView?.apply {
                stopLoading()
                destroy()
            }
        }
    }

    if (webView != null && isOnline && !hasLoadError) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(84.dp)
                .testTag("social_bar_ad_banner")
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
