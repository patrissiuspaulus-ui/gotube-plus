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
    AndroidView(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mediaPlaybackRequiresUserGesture = true
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                tag = videoId
                addJavascriptInterface(
                    IFrameErrorBridge(onEmbeddingError),
                    "GoTubePlayerBridge"
                )
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    playerHtml(videoId),
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
                    "https://www.youtube.com",
                    playerHtml(videoId),
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

private fun playerHtml(videoId: String): String {
    val safeVideoId = videoId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
    return """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
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
                        playerVars: { playsinline: 1, rel: 0, origin: 'https://www.youtube.com' },
                        events: {
                            onError: function(event) {
                                GoTubePlayerBridge.onPlayerError(event.data);
                            }
                        }
                    });
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}