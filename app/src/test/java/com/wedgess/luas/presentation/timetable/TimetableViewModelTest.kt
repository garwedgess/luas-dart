package com.wedgess.luas.presentation.timetable

import androidx.lifecycle.SavedStateHandle
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.TimetableDay
import com.wedgess.luas.domain.timetable.TimetableFeed
import com.wedgess.luas.domain.timetable.TimetableRepository
import com.wedgess.luas.domain.timetable.TimetableStation
import com.wedgess.luas.domain.timetable.TimetableZone
import com.wedgess.luas.domain.usecase.FetchSelectedTransportTypeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<TimetableRepository>()
    private val transport = MutableStateFlow(TransportType.LUAS)
    private val transportUseCase = mockk<FetchSelectedTransportTypeUseCase>()
    private val now = Instant.parse("2026-11-10T09:00:00Z")
    private val date = LocalDate.of(2026, 11, 10)
    private val feed = TimetableFeed(TransportType.LUAS, now, now, date, date.plusDays(30), TimetableZone, 90_000)
    private val first = TimetableStation("first", "First station")
    private val second = TimetableStation("second", "Second station")
    private lateinit var viewModel: TimetableViewModel
    private lateinit var saved: SavedStateHandle

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { transportUseCase() } returns transport
        coEvery { repository.feed(any()) } answers { feed.copy(transport = firstArg()) }
        coEvery { repository.stations(any()) } returns listOf(first, second)
        coEvery { repository.day(any(), any(), any()) } returns TimetableDay(true, emptyList())
        coEvery { repository.refresh(any(), any()) } returns Result.success(Unit)
        saved = SavedStateHandle()
        viewModel = TimetableViewModel(repository, transportUseCase, Clock.fixed(now, TimetableZone), saved)
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `station selection is retained separately for each transport`() = runTest {
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.selectStation(second)
        assertEquals(second, viewModel.uiState.value.station)
        transport.value = TransportType.DART
        assertEquals(first, viewModel.uiState.value.station)
        transport.value = TransportType.LUAS
        assertEquals(second, viewModel.uiState.value.station)
    }

    @Test
    fun `date selection is restored and never silently changed to a covered date`() = runTest {
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        val future = date.plusYears(1)
        coEvery { repository.day(any(), any(), future) } returns TimetableDay(false, emptyList())
        viewModel.selectDate(future)
        assertEquals(future, viewModel.uiState.value.date)
        assertEquals(future.toEpochDay(), saved.get<Long>("timetable_date"))
        assertFalse(viewModel.uiState.value.day.published)
    }

    @Test
    fun `failed update keeps saved timetable visible with an error`() = runTest {
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        coEvery { repository.refresh(any(), any()) } returns Result.failure(IOException())
        viewModel.update()
        assertEquals(feed, viewModel.uiState.value.feed)
        assertTrue(viewModel.uiState.value.updateFailed)
        assertFalse(viewModel.uiState.value.updating)
    }

    @Test
    fun `opening without a download does not start a network transfer`() = runTest {
        coEvery { repository.feed(any()) } returns null
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.update(force = false)
        coVerify(exactly = 0) { repository.refresh(any(), any()) }
        viewModel.update()
        coVerify { repository.refresh(TransportType.LUAS, true) }
    }

    @Test
    fun `switching transport during a download cannot show its error on the other transport`() = runTest {
        val completion = CompletableDeferred<Result<Unit>>()
        coEvery { repository.refresh(TransportType.LUAS, true) } coAnswers { completion.await() }
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.update()
        assertTrue(viewModel.uiState.value.updating)
        transport.value = TransportType.DART
        assertFalse(viewModel.uiState.value.updating)
        completion.complete(Result.failure(IOException()))
        assertEquals(TransportType.DART, viewModel.uiState.value.feed?.transport)
        assertFalse(viewModel.uiState.value.updateFailed)
    }
}
