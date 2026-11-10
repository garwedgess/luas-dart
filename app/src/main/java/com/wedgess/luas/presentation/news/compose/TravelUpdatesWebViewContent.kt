package com.wedgess.luas.presentation.news.compose

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.wedgess.luas.R
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.LoadingContent
import kotlinx.coroutines.delay

private const val PAGE_TIMEOUT_MILLIS = 20_000L

@SuppressLint("SetJavaScriptEnabled") // The official Luas site renders current status through JavaScript.
@Composable
fun TravelUpdatesWebViewContent(modifier: Modifier = Modifier, url: String = LUAS_TRAVEL_UPDATES_URL) {
    var attempt by remember(url) { mutableIntStateOf(0) }
    var loading by remember(url, attempt) { mutableStateOf(true) }
    var failed by remember(url, attempt) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(url, attempt, loading) {
        if (loading) {
            delay(PAGE_TIMEOUT_MILLIS)
            if (loading) failed = true
        }
    }
    Surface(modifier = modifier.fillMaxSize()) {
        Box {
            key(url, attempt) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            webViewClient = TravelUpdatesWebViewClient(
                                onLoading = { loading = it },
                                onError = {
                                    failed = true
                                    loading = false
                                },
                                onExternalLink = uriHandler::openUri
                            )
                            loadUrl(url)
                        }
                    },
                    onRelease = {
                        it.stopLoading()
                        it.destroy()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (failed) {
                Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ErrorContent(
                        stringResource(R.string.news_load_failed),
                        onRetry = { attempt++ },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.news_open_browser))
                    }
                }
            } else if (loading) {
                LoadingContent(
                    stringResource(R.string.news_loading),
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}
