package com.gotube.plus

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
@SuppressLint("SetJavaScriptEnabled")
fun YouTubeIframeView(
    videoId: String,
    onEmbeddingError: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val origin = "https://www.youtube.com"

    AndroidView(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mediaPlaybackRequiresUserGesture = false
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false

                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                tag = videoId
                addJavascriptInterface(
                    IFrameErrorBridge(onEmbeddingError),
                    "GoTubePlayerBridge"
                )
                loadDataWithBaseURL(
                    origin,
                    playerHtml(videoId, origin),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            if (webView.tag != videoId) {
                webView.tag = videoId
                webView.loadDataWithBaseURL(
                    origin,
                    playerHtml(videoId, origin),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        onRelease = WebView::destroy
    )
}

private class IFrameErrorBridge(private val onError: (Int) -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPlayerError(errorCode: Int) {
        mainHandler.post { onError(errorCode) }
    }
}

private fun playerHtml(videoId: String, origin: String): String {
    val safeVideoId = videoId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
    return """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>html,body,#player{margin:0;width:100%;height:100%;background:#000;overflow:hidden}</style>
        </head>
        <body>
            <div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
                function onYouTubeIframeAPIReady() {
                    new YT.Player('player', {
                        width: '100%',
                        height: '100%',
                        videoId: '$safeVideoId',
                        playerVars: {
                            playsinline: 1,
                            rel: 0,
                            controls: 1,
                            autoplay: 0,
                            origin: '$origin',
                            enablejsapi: 1
                        },
                        events: {
                            onReady: function() {},
                            onError: function(event) {
                                if (window.GoTubePlayerBridge && typeof window.GoTubePlayerBridge.onPlayerError === 'function') {
                                    window.GoTubePlayerBridge.onPlayerError(event.data);
                                }
                            }
                        }
                    });
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
