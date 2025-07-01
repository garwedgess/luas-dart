package com.wedgess.luas.presentation.news.compose

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.wedgess.luas.presentation.components.LoadingContent

private const val LUAS_TRAVEL_UPDATES_URL = "https://luas.ie/travel-updates/"

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility", "ComposeModifierMissing")
@Composable
fun TravelUpdatesWebViewContent(
    modifier: Modifier = Modifier,
    url: String = LUAS_TRAVEL_UPDATES_URL
) {
    var isLoading by remember { mutableStateOf(true) }

    Surface(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = false
                    isFocusable = false
                    isFocusableInTouchMode = false
//                    setOnTouchListener { _, event -> event.action == MotionEvent.ACTION_MOVE }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            return !isLoading
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }
                    }
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(isLoading) {
            LoadingContent(
                title = "Loading Travel Updates...",
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
            )
        }
    }
}
