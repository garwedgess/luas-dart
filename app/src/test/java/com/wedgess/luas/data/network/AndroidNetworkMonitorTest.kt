package com.wedgess.luas.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidNetworkMonitorTest {
    private val context = mockk<Context>()
    private val connectivity = mockk<ConnectivityManager>(relaxed = true)
    private val network = mockk<Network>()
    private val capabilities = mockk<NetworkCapabilities>()
    private val callback = slot<ConnectivityManager.NetworkCallback>()
    private var current: Network? = network
    private var validated = true

    private fun monitor(): AndroidNetworkMonitor {
        every { context.getSystemService(ConnectivityManager::class.java) } returns connectivity
        every { connectivity.activeNetwork } answers { current }
        every { connectivity.getNetworkCapabilities(any()) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } answers { validated }
        every { connectivity.registerDefaultNetworkCallback(capture(callback)) } returns Unit
        return AndroidNetworkMonitor(context)
    }

    @Test
    fun `connection validation changes are observed and callback is unregistered on cancellation`() = runTest {
        monitor().isOnline.test {
            assertTrue(awaitItem())
            validated = false
            callback.captured.onCapabilitiesChanged(network, capabilities)
            assertFalse(awaitItem())
            validated = true
            callback.captured.onCapabilitiesChanged(network, capabilities)
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { connectivity.unregisterNetworkCallback(callback.captured) }
    }

    @Test
    fun `losing an old network does not report offline while its replacement is validated`() = runTest {
        monitor().isOnline.test {
            assertTrue(awaitItem())
            callback.captured.onLost(mockk())
            expectNoEvents()
            current = null
            every { connectivity.getNetworkCapabilities(null) } returns null
            callback.captured.onLost(network)
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
