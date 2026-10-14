package com.example.data.repository

import android.util.Base64
import com.example.data.local.AppDatabase
import com.example.data.local.ConnectionLogEntity
import com.example.data.local.ServerEntity
import com.example.model.VpnProtocol
import com.example.model.VpnServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class VpnRepository(private val database: AppDatabase) {

    private val serverDao = database.serverDao()
    private val logDao = database.connectionLogDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val serversFlow: Flow<List<VpnServer>> = serverDao.getAllServers().map { entities ->
        entities.map { it.toVpnServer() }
    }

    val historyFlow: Flow<List<ConnectionLogEntity>> = logDao.getRecentHistory()

    suspend fun initializeDefaultServersIfEmpty() = withContext(Dispatchers.IO) {
        val count = serverDao.getServerCount()
        if (count == 0) {
            val defaults = getDefaultCuratedServers()
            serverDao.insertServers(defaults.map { ServerEntity.fromVpnServer(it) })
        }
    }

    suspend fun toggleFavorite(serverId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        serverDao.setFavorite(serverId, isFavorite)
    }

    suspend fun addCustomServer(server: VpnServer) = withContext(Dispatchers.IO) {
        serverDao.insertServer(ServerEntity.fromVpnServer(server))
    }

    suspend fun deleteServer(serverId: String) = withContext(Dispatchers.IO) {
        serverDao.deleteServer(serverId)
    }

    suspend fun logSession(
        serverName: String,
        countryCode: String,
        durationSeconds: Long,
        bytesIn: Long,
        bytesOut: Long,
        protocol: String
    ) = withContext(Dispatchers.IO) {
        logDao.insertLog(
            ConnectionLogEntity(
                serverName = serverName,
                countryCode = countryCode,
                durationSeconds = durationSeconds,
                bytesIn = bytesIn,
                bytesOut = bytesOut,
                protocol = protocol
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        logDao.clearHistory()
    }

    suspend fun fetchLiveVpnGateServers(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://www.vpngate.net/api/iphone/")
                .header("User-Agent", "OpenShield-VPN/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: ""
            val parsedServers = parseVpnGateCsv(body)
            if (parsedServers.isNotEmpty()) {
                serverDao.insertServers(parsedServers.map { ServerEntity.fromVpnServer(it) })
                Result.success(parsedServers.size)
            } else {
                Result.failure(Exception("No servers found in response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseVpnGateCsv(csvContent: String): List<VpnServer> {
        val results = mutableListOf<VpnServer>()
        val lines = csvContent.lines()
        var headerIndex = -1

        for ((index, line) in lines.withIndex()) {
            if (line.startsWith("#HostName") || line.startsWith("HostName")) {
                headerIndex = index
                break
            }
        }

        if (headerIndex == -1) return emptyList()

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("*")) continue

            val tokens = line.split(",")
            if (tokens.size >= 15) {
                try {
                    val hostName = tokens[0].trim()
                    val ip = tokens[1].trim()
                    val ping = tokens[3].trim().toIntOrNull() ?: 60
                    val speedBps = tokens[4].trim().toLongOrNull() ?: 50000000L
                    val speedMbps = (speedBps / 1_000_000.0).coerceAtLeast(1.0)
                    val countryLong = tokens[5].trim()
                    val countryShort = tokens[6].trim().uppercase()
                    val configBase64 = tokens[14].trim()

                    var decodedConfig = ""
                    if (configBase64.isNotEmpty()) {
                        try {
                            decodedConfig = String(Base64.decode(configBase64, Base64.DEFAULT))
                        } catch (_: Exception) {}
                    }

                    results.add(
                        VpnServer(
                            id = "vpngate_$ip",
                            name = "$countryLong Volunteer Relay",
                            countryCode = countryShort,
                            countryName = countryLong,
                            ip = ip,
                            port = 1194,
                            protocol = VpnProtocol.OPENVPN_UDP,
                            pingMs = ping,
                            speedMbps = speedMbps,
                            isFavorite = false,
                            isCustom = false,
                            configData = decodedConfig,
                            features = listOf("VPNGate Open Relay", "Academic Host", "No Registration")
                        )
                    )
                } catch (_: Exception) {}
            }
        }
        return results
    }

    private fun getDefaultCuratedServers(): List<VpnServer> {
        return listOf(
            VpnServer(
                id = "default_us_ny",
                name = "US East (New York) #1",
                countryCode = "US",
                countryName = "United States",
                ip = "198.51.100.42",
                port = 1194,
                protocol = VpnProtocol.OPENVPN_UDP,
                pingMs = 28,
                speedMbps = 145.0,
                isFavorite = true,
                features = listOf("Open-Source Relay", "P2P Allowed", "10 Gbps Uplink", "Zero Logs")
            ),
            VpnServer(
                id = "default_de_fra",
                name = "Germany (Frankfurt) OpenHub",
                countryCode = "DE",
                countryName = "Germany",
                ip = "198.51.100.58",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                pingMs = 34,
                speedMbps = 180.0,
                isFavorite = true,
                features = listOf("WireGuard High-Speed", "Privacy Shield", "Ad-Block DNS")
            ),
            VpnServer(
                id = "default_jp_tok",
                name = "Japan (Tokyo) Tsukuba Academic",
                countryCode = "JP",
                countryName = "Japan",
                ip = "203.0.113.19",
                port = 1194,
                protocol = VpnProtocol.OPENVPN_UDP,
                pingMs = 62,
                speedMbps = 110.0,
                isFavorite = false,
                features = listOf("Academic Volunteer", "Bypass Filters", "Open Source")
            ),
            VpnServer(
                id = "default_sg_sin",
                name = "Singapore Cloud Node #3",
                countryCode = "SG",
                countryName = "Singapore",
                ip = "203.0.113.88",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                pingMs = 45,
                speedMbps = 160.0,
                isFavorite = false,
                features = listOf("WireGuard", "Asia Backbone", "Low Latency")
            ),
            VpnServer(
                id = "default_ch_zur",
                name = "Switzerland (Zurich) Privacy Fortress",
                countryCode = "CH",
                countryName = "Switzerland",
                ip = "198.51.100.115",
                port = 443,
                protocol = VpnProtocol.OPENVPN_TCP,
                pingMs = 38,
                speedMbps = 130.0,
                isFavorite = true,
                features = listOf("Swiss Privacy Laws", "TCP Stealth", "Zero Logs")
            ),
            VpnServer(
                id = "default_gb_lon",
                name = "United Kingdom (London) Fast Relay",
                countryCode = "GB",
                countryName = "United Kingdom",
                ip = "198.51.100.77",
                port = 1194,
                protocol = VpnProtocol.OPENVPN_UDP,
                pingMs = 31,
                speedMbps = 155.0,
                isFavorite = false,
                features = listOf("OpenVPN UDP", "Streaming Optimized", "Anti-Throttling")
            ),
            VpnServer(
                id = "default_nl_ams",
                name = "Netherlands (Amsterdam) Libre Relay",
                countryCode = "NL",
                countryName = "Netherlands",
                ip = "198.51.100.92",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                pingMs = 30,
                speedMbps = 195.0,
                isFavorite = false,
                features = listOf("WireGuard", "Tor Friendly", "P2P Friendly")
            ),
            VpnServer(
                id = "default_ca_tor",
                name = "Canada (Toronto) Shield #2",
                countryCode = "CA",
                countryName = "Canada",
                ip = "198.51.100.33",
                port = 1194,
                protocol = VpnProtocol.OPENVPN_UDP,
                pingMs = 42,
                speedMbps = 125.0,
                isFavorite = false,
                features = listOf("North America Relay", "DNSSEC", "OpenVPN")
            ),
            VpnServer(
                id = "default_fr_par",
                name = "France (Paris) OpenNode",
                countryCode = "FR",
                countryName = "France",
                ip = "198.51.100.64",
                port = 1194,
                protocol = VpnProtocol.OPENVPN_UDP,
                pingMs = 36,
                speedMbps = 140.0,
                isFavorite = false,
                features = listOf("European Relay", "No Logs", "DDoS Shield")
            ),
            VpnServer(
                id = "default_se_sto",
                name = "Sweden (Stockholm) Nordic Haven",
                countryCode = "SE",
                countryName = "Sweden",
                ip = "198.51.100.12",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                pingMs = 48,
                speedMbps = 175.0,
                isFavorite = false,
                features = listOf("WireGuard", "Strong Encryption", "Privacy Focused")
            )
        )
    }
}
