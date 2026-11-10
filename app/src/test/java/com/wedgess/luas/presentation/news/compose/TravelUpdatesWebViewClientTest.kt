package com.wedgess.luas.presentation.news.compose

import android.net.Uri
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TravelUpdatesWebViewClientTest {
    private var loading = true
    private var failed = false
    private var external: String? = null
    private val client = TravelUpdatesWebViewClient({ loading = it }, { failed = true }, { external = it })

    private fun request(url: String = LUAS_TRAVEL_UPDATES_URL, main: Boolean = true) = mockk<WebResourceRequest> {
        every { this@mockk.url } returns Uri.parse(url)
        every { isForMainFrame } returns main
    }

    @Test
    fun `current HTTPS Luas links stay in the WebView and external links open separately`() {
        assertEquals("www.luas.ie", Uri.parse(LUAS_TRAVEL_UPDATES_URL).host)
        assertFalse(client.shouldOverrideUrlLoading(null, request()))
        assertTrue(client.shouldOverrideUrlLoading(null, request("https://www.transportforireland.ie/")))
        assertEquals("https://www.transportforireland.ie/", external)
        assertTrue(client.shouldOverrideUrlLoading(null, request("http://www.luas.ie/")))
    }

    @Test
    fun `main document errors produce a visible failure but failed assets do not replace the page`() {
        client.onReceivedError(null, request(main = false), null)
        assertFalse(failed)
        client.onReceivedError(null, request(), null)
        assertTrue(failed)
    }

    @Test
    fun `HTTP document failures are reported instead of leaving a blank screen`() {
        val response = mockk<WebResourceResponse> { every { statusCode } returns 404 }
        client.onReceivedHttpError(null, request(), response)
        assertTrue(failed)
    }

    @Test
    fun `TLS errors cancel the request and display failure`() {
        val handler = mockk<SslErrorHandler>(relaxed = true)
        client.onReceivedSslError(null, handler, null)
        verify { handler.cancel() }
        verify(exactly = 0) { handler.proceed() }
        assertTrue(failed)
    }

    @Test
    fun `page completion ends the loading state`() {
        client.onPageStarted(null, LUAS_TRAVEL_UPDATES_URL, null)
        assertTrue(loading)
        client.onPageFinished(null, LUAS_TRAVEL_UPDATES_URL)
        assertFalse(loading)
    }
}
