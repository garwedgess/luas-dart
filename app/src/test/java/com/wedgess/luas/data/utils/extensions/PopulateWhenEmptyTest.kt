package com.wedgess.luas.data.utils.extensions

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PopulateWhenEmptyTest {
    @Test
    fun `empty catalogue retries and stops requesting once the database is populated`() = runTest {
        val database = MutableStateFlow(emptyList<String>())
        var attempts = 0
        val shown = mutableListOf<List<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            database.populateWhenEmpty {
                attempts++
                if (attempts == 2) database.value = listOf("Station")
            }.collect { shown.add(it) }
        }
        runCurrent()
        assertEquals(1, attempts)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(listOf("Station"), shown.last())
        advanceTimeBy(120_000)
        assertEquals(2, attempts)
    }

    @Test
    fun `leaving the screen stops empty catalogue retries`() = runTest {
        var attempts = 0
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            MutableStateFlow(emptyList<String>()).populateWhenEmpty { attempts++ }.collect {}
        }
        runCurrent()
        job.cancel()
        advanceTimeBy(120_000)
        assertEquals(1, attempts)
    }

    @Test
    fun `a cached catalogue needs no network request`() = runTest {
        var attempts = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            MutableStateFlow(listOf("Station")).populateWhenEmpty { attempts++ }.collect {}
        }
        advanceTimeBy(120_000)
        assertEquals(0, attempts)
    }
}
