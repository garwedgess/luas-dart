package com.wedgess.luas.presentation.timetable

import androidx.lifecycle.SavedStateHandle
import com.wedgess.luas.domain.model.StationPreferences
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.PreferencesRepository
import com.wedgess.luas.domain.repository.StationPreferencesRepository
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
import kotlinx.coroutines.test.runCurrent
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
    private val preferences = mockk<PreferencesRepository>(relaxed = true)
    private val stationPreferences = mockk<StationPreferencesRepository>()
    private val green = MutableStateFlow("")
    private val red = MutableStateFlow("")
    private val dart = MutableStateFlow("")
    private val luasPicker = MutableStateFlow(StationPreferences())
    private val dartPicker = MutableStateFlow(StationPreferences())
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
        every { preferences.fetchSelectedGreenLineStation() } returns green
        every { preferences.fetchSelectedRedLineStation() } returns red
        every { preferences.fetchSelectedDartStation() } returns dart
        every { stationPreferences.observe(TransportType.LUAS) } returns luasPicker
        every { stationPreferences.observe(TransportType.DART) } returns dartPicker
        coEvery { stationPreferences.markUsed(any(), any()) } returns Result.success(Unit)
        coEvery { stationPreferences.setFavourite(any(), any(), any()) } returns Result.success(Unit)
        coEvery { repository.feed(any()) } answers { feed.copy(transport = firstArg()) }
        coEvery { repository.stations(any()) } returns listOf(first, second)
        coEvery { repository.day(any(), any(), any()) } returns TimetableDay(true, emptyList())
        coEvery { repository.refresh(any(), any()) } returns Result.success(Unit)
        saved = SavedStateHandle()
        viewModel = TimetableViewModel(
            repository,
            transportUseCase,
            Clock.fixed(now, TimetableZone),
            saved,
            preferences,
            stationPreferences
        )
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

    @Test
    fun `date queries keep the current coherent content until the next date is ready`() = runTest {
        val next = CompletableDeferred<TimetableDay>()
        coEvery { repository.day(any(), any(), date.plusDays(1)) } coAnswers { next.await() }
        val shown = mutableListOf<TimetableUiState>()
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect { shown.add(it) } }
        shown.clear()
        viewModel.selectDate(date.plusDays(1))
        runCurrent()
        assertEquals(date, viewModel.uiState.value.date)
        assertEquals(feed, viewModel.uiState.value.feed)
        assertTrue(shown.none { it.loading })
        next.complete(TimetableDay(true, emptyList()))
        runCurrent()
        assertEquals(date.plusDays(1), viewModel.uiState.value.date)
        assertTrue(shown.none { it.loading })
    }

    @Test
    fun `a late cancelled date query cannot replace a newer selected day`() = runTest {
        val slow = CompletableDeferred<TimetableDay>()
        coEvery { repository.day(any(), any(), date.plusDays(1)) } coAnswers { slow.await() }
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.selectDate(date.plusDays(1))
        viewModel.selectDate(date.plusDays(2))
        slow.complete(TimetableDay(false, emptyList()))
        runCurrent()
        assertEquals(date.plusDays(2), viewModel.uiState.value.date)
        assertTrue(viewModel.uiState.value.day.published)
    }

    @Test
    fun `timetables initially opens the saved Forecast DART station`() = runTest {
        transport.value = TransportType.DART
        dart.value = second.name
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        assertEquals(second, viewModel.uiState.value.station)
        coVerify { repository.day(TransportType.DART, second.id, date) }
    }

    @Test
    fun `Luas starts from the most recently chosen Forecast line station`() = runTest {
        green.value = first.name
        red.value = second.name
        luasPicker.value = StationPreferences(recents = listOf(second.name, first.name))
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        assertEquals(second, viewModel.uiState.value.station)
    }

    @Test
    fun `a timetable choice overrides Forecast and never changes the Forecast selection`() = runTest {
        transport.value = TransportType.DART
        dart.value = second.name
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.selectStation(first)
        dart.value = first.name
        dart.value = second.name
        assertEquals(first, viewModel.uiState.value.station)
        coVerify(exactly = 0) { preferences.updateSelectedDartStation(any()) }
        coVerify(exactly = 0) { preferences.updateSelectedRedLineStation(any()) }
        coVerify(exactly = 0) { preferences.updateSelectedGreenLineStation(any()) }
        coVerify { stationPreferences.markUsed(TransportType.DART, first.name) }
    }

    @Test
    fun `a restored timetable selection takes priority over the Forecast default`() = runTest {
        transport.value = TransportType.DART
        dart.value = first.name
        val restored = TimetableViewModel(
            repository,
            transportUseCase,
            Clock.fixed(now, TimetableZone),
            SavedStateHandle(mapOf("timetable_station_DART" to second.id)),
            preferences,
            stationPreferences
        )
        backgroundScope.launch(dispatcher) { restored.uiState.collect {} }
        assertEquals(second, restored.uiState.value.station)
    }

    @Test
    fun `a Forecast station missing from the timetable falls back to an available station`() = runTest {
        transport.value = TransportType.DART
        dart.value = "Unavailable station"
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        assertEquals(first, viewModel.uiState.value.station)
    }

    @Test
    fun `feed aliases and accents resolve the same Forecast station`() = runTest {
        transport.value = TransportType.DART
        dart.value = "Dún Laoghaire Mallin"
        val timetable = TimetableStation("dun laoghaire", "Dun Laoghaire")
        coEvery { repository.stations(TransportType.DART) } returns listOf(first, timetable)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        assertEquals(timetable, viewModel.uiState.value.station)
    }

    @Test
    fun `shared favourites update immediately without reloading the timetable`() = runTest {
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        luasPicker.value = StationPreferences(favourites = setOf(second.name))
        assertEquals(setOf(second.name), viewModel.uiState.value.stationPreferences.favourites)
        coVerify(exactly = 1) { repository.day(any(), any(), any()) }
        transport.value = TransportType.DART
        dartPicker.value = StationPreferences(favourites = setOf(first.name))
        assertEquals(setOf(first.name), viewModel.uiState.value.stationPreferences.favourites)
    }

    @Test
    fun `favouriting from timetable uses the same per-transport store as Forecast`() = runTest {
        transport.value = TransportType.DART
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.toggleFavourite(second.name, true)
        coVerify(exactly = 1) { stationPreferences.setFavourite(TransportType.DART, second.name, true) }
        coVerify(exactly = 0) { stationPreferences.setFavourite(TransportType.LUAS, any(), any()) }
    }
}
