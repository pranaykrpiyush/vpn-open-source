package com.example

import com.example.model.InternetStatus
import com.example.model.VpnState
import com.example.vpn.ConnectivityChecker
import com.example.vpn.VpnStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun defaultPingEndpoint_isGoogleDns() {
        assertEquals("8.8.8.8", ConnectivityChecker.DEFAULT_PING_ENDPOINT)
    }

    @Test
    fun vpnStateHolder_tracksInternetStatusAndLatency() {
        VpnStateHolder.updateInternetStatus(InternetStatus.ONLINE, 18L)
        assertEquals(InternetStatus.ONLINE, VpnStateHolder.internetStatus.value)
        assertEquals(18L, VpnStateHolder.lastPingLatencyMs.value)

        VpnStateHolder.updateInternetStatus(InternetStatus.OFFLINE, null)
        assertEquals(InternetStatus.OFFLINE, VpnStateHolder.internetStatus.value)
        assertEquals(null, VpnStateHolder.lastPingLatencyMs.value)
    }

    @Test
    fun vpnStateHolder_resetsInternetOnDisconnect() {
        VpnStateHolder.updateInternetStatus(InternetStatus.ONLINE, 25L)
        VpnStateHolder.updateState(VpnState.DISCONNECTED)
        assertEquals(InternetStatus.CHECKING, VpnStateHolder.internetStatus.value)
        assertEquals(null, VpnStateHolder.lastPingLatencyMs.value)
    }
}

