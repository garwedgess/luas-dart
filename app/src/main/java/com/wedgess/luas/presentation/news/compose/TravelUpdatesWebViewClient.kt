package com.wedgess.luas.presentation.news.compose

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

internal const val LUAS_TRAVEL_UPDATES_URL = "https://www.luas.ie/travel-update/"

internal class TravelUpdatesWebViewClient(
    private val onLoading: (Boolean) -> Unit,
    private val onError: () -> Unit,
    private val onExternalLink: (String) -> Unit
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
        val uri = request.url
        if (uri.scheme != "https") return true
        if (uri.host == "www.luas.ie") return false
        if (request.isForMainFrame) onExternalLink(uri.toString())
        return true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) = onLoading(true)

    override fun onPageFinished(view: WebView?, url: String?) = onLoading(false)

    override fun onReceivedError(view: WebView?, request: WebResourceRequest, error: WebResourceError?) {
        if (request.isForMainFrame) onError()
    }

    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame && errorResponse.statusCode >= HTTP_ERROR_MIN) onError()
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler, error: SslError?) {
        handler.cancel()
        onError()
    }

    private companion object {
        const val HTTP_ERROR_MIN = 400
    }
}
