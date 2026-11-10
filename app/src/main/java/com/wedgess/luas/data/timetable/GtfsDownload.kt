package com.wedgess.luas.data.timetable

import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.TimetableFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

private const val MAX_DOWNLOAD_BYTES = 20L * 1024 * 1024
private const val DOWNLOAD_TIMEOUT_SECONDS = 90L
private const val READ_TIMEOUT_SECONDS = 30L
private const val HTTP_NOT_MODIFIED = 304

sealed interface GtfsDownload {
    data object Unchanged : GtfsDownload
    data class Archive(val file: File, val etag: String?, val lastModified: String?) : GtfsDownload
}

fun interface GtfsSource {
    suspend fun download(transport: TransportType, previous: TimetableFeed?): GtfsDownload
}

/** Downloads to a bounded temporary file, never to the active database. Parsing ignores shapes entirely. */
class GtfsHttpSource(client: OkHttpClient, private val directory: File) : GtfsSource {
    private val client = client.newBuilder()
        .followSslRedirects(false)
        .callTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    override suspend fun download(transport: TransportType, previous: TimetableFeed?): GtfsDownload =
        withContext(Dispatchers.IO) {
            client.newCall(request(transport, previous)).execute().use { response ->
                if (response.code == HTTP_NOT_MODIFIED) {
                    require(previous != null) { "No saved timetable for unchanged response" }
                    GtfsDownload.Unchanged
                } else {
                    if (!response.isSuccessful) throw IOException("Timetable download failed (HTTP ${response.code})")
                    saveArchive(response)
                }
            }
        }

    private fun request(transport: TransportType, previous: TimetableFeed?): Request {
        val name = if (transport == TransportType.LUAS) "LUAS" else "Irish_Rail"
        return Request.Builder()
            .url("https://www.transportforireland.ie/transitData/Data/GTFS_$name.zip")
            .header("User-Agent", "LuasDart/1.0 (offline timetables)")
            .header("Cache-Control", "no-cache")
            .apply {
                previous?.etag?.let { header("If-None-Match", it) }
                previous?.lastModified?.let { header("If-Modified-Since", it) }
            }.build()
    }

    private suspend fun saveArchive(response: Response): GtfsDownload.Archive {
        val body = response.body ?: throw IOException("Empty timetable download")
        require(body.contentLength() <= MAX_DOWNLOAD_BYTES) { "Timetable download too large" }
        directory.mkdirs()
        val file = File.createTempFile("gtfs-", ".zip", directory)
        var complete = false
        try {
            body.byteStream().use { input ->
                file.outputStream().buffered().use { output ->
                    copyBounded(input, output)
                }
            }
            complete = true
            return GtfsDownload.Archive(file, response.header("ETag"), response.header("Last-Modified"))
        } finally {
            if (!complete) file.delete()
        }
    }
    private suspend fun copyBounded(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            coroutineContext.ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_DOWNLOAD_BYTES) { "Timetable download too large" }
            output.write(buffer, 0, count)
        }
    }
}
