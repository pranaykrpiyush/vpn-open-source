package com.example.vpn

import com.example.model.ConnectionTelemetry
import com.example.model.InternetStatus
import com.example.model.LogEntry
import com.example.model.SpeedPoint
import com.example.model.VpnServer
import com.example.model.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale

object VpnStateHolder {
    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _currentServer = MutableStateFlow<VpnServer?>(null)
    val currentServer: StateFlow<VpnServer?> = _currentServer.asStateFlow()

    private val _telemetry = MutableStateFlow(ConnectionTelemetry())
    val telemetry: StateFlow<ConnectionTelemetry> = _telemetry.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<SpeedPoint>>(emptyList())
    val speedHistory: StateFlow<List<SpeedPoint>> = _speedHistory.asStateFlow()

    private val _internetStatus = MutableStateFlow(InternetStatus.CHECKING)
    val internetStatus: StateFlow<InternetStatus> = _internetStatus.asStateFlow()

    private val _lastPingLatencyMs = MutableStateFlow<Long?>(null)
    val lastPingLatencyMs: StateFlow<Long?> = _lastPingLatencyMs.asStateFlow()

    const val pingTargetEndpoint: String = "8.8.8.8"

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(
                level = "SYS",
                message = "OpenShield Core initialized. Ready to secure network."
            )
        )
    )
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun updateState(state: VpnState) {
        _vpnState.value = state
        addLog("STATE", "VPN state transition -> ${state.displayName}")
        if (state == VpnState.DISCONNECTED) {
            _internetStatus.value = InternetStatus.CHECKING
            _lastPingLatencyMs.value = null
        }
    }

    fun updateInternetStatus(status: InternetStatus, latencyMs: Long? = null) {
        _internetStatus.value = status
        _lastPingLatencyMs.value = latencyMs
    }

    fun setCurrentServer(server: VpnServer?) {
        _currentServer.value = server
    }

    fun updateTelemetry(update: (ConnectionTelemetry) -> ConnectionTelemetry) {
        _telemetry.value = update(_telemetry.value)
    }

    fun recordSpeed(uploadBps: Long, downloadBps: Long) {
        val now = System.currentTimeMillis()
        val upKbps = (uploadBps * 8f) / 1000f
        val downKbps = (downloadBps * 8f) / 1000f
        val currentList = _speedHistory.value.toMutableList()
        currentList.add(SpeedPoint(now, upKbps, downKbps))
        if (currentList.size > 30) {
            currentList.removeAt(0)
        }
        _speedHistory.value = currentList
    }

    fun addLog(level: String, message: String) {
        val entry = LogEntry(level = level, message = message)
        val current = _logs.value.toMutableList()
        current.add(entry)
        if (current.size > 200) {
            current.removeAt(0)
        }
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun resetTelemetry() {
        _telemetry.value = ConnectionTelemetry()
        _speedHistory.value = emptyList()
        _internetStatus.value = InternetStatus.CHECKING
        _lastPingLatencyMs.value = null
    }
}
