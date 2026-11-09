package com.wedgess.luas.domain

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
}
