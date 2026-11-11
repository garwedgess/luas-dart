package com.wedgess.luas.domain

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class ConnectivityTest {

    @Test
    fun `an IOException is offline, directly or as the cause`() {
        assertTrue(IOException("down").isOffline())
        assertTrue(SocketTimeoutException().isOffline())
        assertTrue(Exception("Network error occurred", IOException("no route")).isOffline())
    }

    @Test
    fun `other failures are not offline`() {
        assertFalse(Exception("Serialization error").isOffline())
        assertFalse(IllegalStateException("bad", IllegalArgumentException()).isOffline())
    }

    @Test
    fun `station catalogue is retried on reconnection but not on disconnection`() = runTest {
        val online = MutableStateFlow(false)
        var attempts = 0
        flow { emit(++attempts) }.restartOnReconnect(online).test {
            assertEquals(1, awaitItem())
            online.value = true
            assertEquals(2, awaitItem())
            online.value = false
            expectNoEvents()
            online.value = true
            assertEquals(3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
