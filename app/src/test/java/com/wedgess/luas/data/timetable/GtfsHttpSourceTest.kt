package com.wedgess.luas.data.timetable

import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.TimetableFeed
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GtfsHttpSourceTest {
    @get:Rule val files = TemporaryFolder()
    private val feed = TimetableFeed(
        TransportType.DART, Instant.EPOCH, Instant.EPOCH, LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13),
        ZoneId.of("Europe/Dublin"), 90_000, "\"server-tag\"", "Sun, 08 Nov 2026 23:06:09 GMT"
    )

    @Test
    fun `request uses official feed conditional validators and no-cache`() = runTest {
        var request: Request? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            request = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(304).message("Not modified").body("".toResponseBody()).build()
        }.build()
        val result = GtfsHttpSource(client, files.newFolder()).download(TransportType.DART, feed)
        assertEquals(GtfsDownload.Unchanged, result)
        assertEquals("https://www.transportforireland.ie/transitData/Data/GTFS_Irish_Rail.zip", request?.url.toString())
        assertEquals("\"server-tag\"", request?.header("If-None-Match"))
        assertEquals(feed.lastModified, request?.header("If-Modified-Since"))
        assertEquals("no-cache", request?.header("Cache-Control"))
    }

    @Test
    fun `fresh Luas download has no validators and returns a temporary archive`() = runTest {
        var request: Request? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            request = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .header("ETag", "tag").body("zip bytes".toResponseBody()).build()
        }.build()
        val source = GtfsHttpSource(client, files.newFolder())
        val result = source.download(TransportType.LUAS, null) as GtfsDownload.Archive
        assertTrue(request?.url.toString().endsWith("GTFS_LUAS.zip"))
        assertEquals(null, request?.header("If-None-Match"))
        assertEquals("zip bytes", result.file.readText())
        assertEquals("tag", result.etag)
    }

    @Test
    fun `HTTP failure leaves no partial archive`() = runTest {
        val directory = files.newFolder()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(503).message("Unavailable").body("".toResponseBody()).build()
        }.build()
        val result = runCatching { GtfsHttpSource(client, directory).download(TransportType.LUAS, null) }
        assertTrue(result.isFailure)
        assertFalse(directory.listFiles().orEmpty().isNotEmpty())
    }
}
