package com.example.model

enum class VpnProtocol(val displayName: String, val tag: String) {
    OPENVPN_UDP("OpenVPN (UDP)", "UDP"),
    OPENVPN_TCP("OpenVPN (TCP)", "TCP"),
    WIREGUARD("WireGuard", "WG")
}

enum class TunnelMode(val displayName: String, val badge: String, val description: String) {
    SECURE_DNS_SHIELD(
        "Secure DNS & Privacy Shield",
        "Recommended",
        "Encrypted DNS leak protection, ad & tracker blocking, and guaranteed full-speed internet for all apps."
    ),
    FULL_TUNNEL(
        "Full Device Route (0.0.0.0/0)",
        "Advanced",
        "Routes entire device network stack into tun0 virtual interface with app bypass fallback."
    )
}

enum class InternetStatus(val label: String) {
    CHECKING("Checking connectivity..."),
    ONLINE("Internet Online"),
    OFFLINE("Internet Unreachable")
}

enum class VpnState(val displayName: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting..."),
    AUTHENTICATING("Verifying Credentials..."),
    CONNECTED("Protected"),
    DISCONNECTING("Disconnecting...")
}

data class VpnServer(
    val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val ip: String,
    val port: Int = 1194,
    val protocol: VpnProtocol = VpnProtocol.OPENVPN_UDP,
    val pingMs: Int = 45,
    val speedMbps: Double = 85.0,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val configData: String = "",
    val features: List<String> = listOf("Open-Source Relay", "No Logs", "DNS Protected")
)

data class ConnectionTelemetry(
    val uploadSpeedBps: Long = 0,
    val downloadSpeedBps: Long = 0,
    val totalBytesIn: Long = 0,
    val totalBytesOut: Long = 0,
    val durationSeconds: Long = 0,
    val virtualIp: String = "10.8.0.2",
    val realIp: String = "192.0.2.1",
    val cipher: String = "AES-256-GCM"
)

data class SpeedPoint(
    val timestamp: Long,
    val uploadKbps: Float,
    val downloadKbps: Float
)

data class DnsOption(
    val id: String,
    val name: String,
    val primaryIp: String,
    val secondaryIp: String,
    val description: String
)

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO",
    val message: String
)
