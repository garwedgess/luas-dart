package com.wedgess.luas.data.timetable

import com.wedgess.luas.data.utils.extensions.resultOf
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.TimetableFeed
import com.wedgess.luas.domain.timetable.TimetableRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

private val UpdateInterval = Duration.ofHours(24)

class TimetableRepositoryImpl(
    private val dao: TimetableDao,
    private val source: GtfsSource,
    private val parser: GtfsParser,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TimetableRepository {
    private val locks = TransportType.entries.associateWith { Mutex() }

    override suspend fun feed(transport: TransportType) = withContext(dispatcher) { dao.feed(transport) }
    override suspend fun stations(transport: TransportType) = withContext(dispatcher) { dao.stations(transport) }
    override suspend fun day(transport: TransportType, stationId: String, date: LocalDate) =
        withContext(dispatcher) { dao.day(transport, stationId, date) }

    override suspend fun refresh(transport: TransportType, force: Boolean): Result<Unit> = withContext(dispatcher) {
        locks.getValue(transport).withLock {
            resultOf {
                val previous = dao.feed(transport)
                val now = clock.instant()
                if (!force && previous != null) {
                    val age = Duration.between(previous.checkedAt, now)
                    if (!age.isNegative && age < UpdateInterval) return@resultOf
                }
                when (val download = source.download(transport, previous)) {
                    GtfsDownload.Unchanged -> dao.checked(transport, now)
                    is GtfsDownload.Archive -> try {
                        val parsed = parser.parse(download.file, transport)
                        coroutineContext.ensureActive()
                        val dates = parsed.schedule.services.dates
                        val feed = TimetableFeed(
                            transport, now, now, dates.min(), dates.max(), parsed.zone,
                            parsed.schedule.times.maxOf { it.seconds },
                            download.etag,
                            download.lastModified,
                            parsed.version
                        )
                        val context = coroutineContext
                        dao.replace(parsed, feed) { context.ensureActive() }
                    } finally {
                        download.file.delete()
                    }
                }
            }
        }
    }
}
