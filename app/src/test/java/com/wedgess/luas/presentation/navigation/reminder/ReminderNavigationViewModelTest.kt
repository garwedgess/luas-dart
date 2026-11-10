package com.wedgess.luas.presentation.navigation.reminder

import androidx.lifecycle.SavedStateHandle
import com.wedgess.luas.domain.model.Alarm
import com.wedgess.luas.domain.model.AlarmKind
import com.wedgess.luas.domain.model.AlarmState
import com.wedgess.luas.domain.model.DartStationEntity
import com.wedgess.luas.domain.model.LuasLineEntity
import com.wedgess.luas.domain.model.LuasStopEntity
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.repository.AlarmConfigRepository
import com.wedgess.luas.domain.repository.DartRepository
import com.wedgess.luas.domain.repository.LuasRepository
import com.wedgess.luas.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderNavigationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val alarms = mockk<AlarmConfigRepository>()
    private val preferences = mockk<PreferencesRepository>()
    private val luas = mockk<LuasRepository>()
    private val dart = mockk<DartRepository>()
    private val now = Instant.parse("2026-11-10T09:00:00Z")
    private val dartAlarm = Alarm(
        id = "dart", requestCode = 1, kind = AlarmKind.SPECIFIC_DEPARTURE, state = AlarmState.FIRED,
        transport = TransportType.DART, stationCode = "PERSE", stationName = "Old station name",
        tripCode = "E807", notifyMinutesBefore = 3, reminderAt = now, createdAt = now, updatedAt = now
    )
    private fun model(saved: SavedStateHandle = SavedStateHandle()) =
        ReminderNavigationViewModel(alarms, preferences, luas, dart, saved)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { alarms.get(any()) } returns Result.success(dartAlarm)
        coEvery { preferences.updateSelectedTransportType(any()) } returns Result.success(Unit)
        coEvery { preferences.updateSelectedDartStation(any()) } returns Result.success(Unit)
        coEvery { preferences.updateSelectedRedLineStation(any()) } returns Result.success(Unit)
        coEvery { preferences.updateSelectedGreenLineStation(any()) } returns Result.success(Unit)
        every { dart.fetchStations() } returns flowOf(
            Result.success(listOf(DartStationEntity.initial().copy(name = "Dublin Pearse", code = "PERSE")))
        )
        every { luas.fetchAllStops() } returns flowOf(
            Result.success(
                listOf(
                    LuasStopEntity.initial().copy(
                        name = "Abbey Street",
                        abbreviation = "ABB",
                        line = LuasLineEntity.RED
                    )
                )
            )
        )
    }

    @After
    fun cleanup() = Dispatchers.resetMain()

    @Test
    fun `DART tap selects the stored alarm transport and current station name`() = runTest(dispatcher) {
        val vm = model()
        vm.open("dart")
        assertEquals("E807", vm.state.value.target?.alarm?.tripCode)
        assertEquals("Dublin Pearse", vm.state.value.target?.alarm?.stationName)
        coVerify { preferences.updateSelectedTransportType(TransportType.DART) }
        coVerify { preferences.updateSelectedDartStation("Dublin Pearse") }
    }

    @Test
    fun `Luas tap resolves its actual line instead of opening the default green tab`() = runTest(dispatcher) {
        coEvery { alarms.get("red") } returns Result.success(
            dartAlarm.copy(id = "red", transport = TransportType.LUAS, stationCode = "ABB", tripCode = null)
        )
        val vm = model()
        vm.open("red")
        assertEquals(LuasLineEntity.RED, vm.state.value.target?.line)
        coVerify { preferences.updateSelectedRedLineStation("Abbey Street") }
        coVerify(exactly = 0) { preferences.updateSelectedGreenLineStation(any()) }
    }

    @Test
    fun `new taps get new tokens and old completions cannot clear them`() = runTest(dispatcher) {
        val vm = model()
        vm.open("dart")
        val first = vm.state.value.target!!.token
        vm.open("dart")
        val second = vm.state.value.target!!.token
        assertNotEquals(first, second)
        vm.complete(first, false)
        assertEquals(second, vm.state.value.target?.token)
        vm.complete(second, false)
        assertNull(vm.state.value.target)
    }

    @Test
    fun `pending navigation restores but consumed focus does not replay`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val vm = model(saved)
        vm.open("dart")
        val restored = model(saved)
        assertEquals("PERSE", restored.state.value.target?.alarm?.stationCode)
        restored.complete(restored.state.value.target!!.token, false)
        assertNull(model(saved).state.value.target)
    }

    @Test
    fun `missing reminder reports a failure without guessing a station`() = runTest(dispatcher) {
        coEvery { alarms.get("gone") } returns Result.success(null)
        val vm = model()
        vm.open("gone")
        assertEquals(ReminderNavigationError.OPEN_FAILED, vm.state.value.error)
        assertNull(vm.state.value.target)
        coVerify(exactly = 0) { preferences.updateSelectedDartStation(any()) }
    }

    @Test
    fun `a removed station is never replaced with the catalogue first station`() = runTest(dispatcher) {
        every { dart.fetchStations() } returns flowOf(
            Result.success(listOf(DartStationEntity.initial().copy(name = "Other", code = "OTHER")))
        )
        val vm = model()
        vm.open("dart")
        assertEquals(ReminderNavigationError.OPEN_FAILED, vm.state.value.error)
        coVerify(exactly = 0) { preferences.updateSelectedDartStation(any()) }
    }

    @Test
    fun `empty offline catalogue has a bounded open wait`() = runTest(dispatcher) {
        every { dart.fetchStations() } returns MutableStateFlow(Result.success(emptyList()))
        val vm = model()
        vm.open("dart")
        assertEquals(true, vm.state.value.loading)
        advanceTimeBy(12_000)
        runCurrent()
        assertEquals(false, vm.state.value.loading)
        assertEquals(ReminderNavigationError.OPEN_FAILED, vm.state.value.error)
    }

    @Test
    fun `a newer tap cancels a delayed lookup from the previous notification`() = runTest(dispatcher) {
        val delayed = CompletableDeferred<Result<Alarm?>>()
        coEvery { alarms.get("slow") } coAnswers { delayed.await() }
        val vm = model()
        vm.open("slow")
        vm.open("dart")
        delayed.complete(Result.success(dartAlarm.copy(id = "slow")))
        runCurrent()
        assertEquals("dart", vm.state.value.target?.alarm?.id)
    }
}
