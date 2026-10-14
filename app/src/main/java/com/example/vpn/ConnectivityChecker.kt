package com.example.vpn

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

object ConnectivityChecker {

    const val DEFAULT_PING_ENDPOINT = "8.8.8.8"

    /**
     * Pings a reliable endpoint (defaults to Google Public DNS 8.8.8.8)
     * using a resilient multi-stage probe:
     * 1. Direct ICMP Ping via Linux ping utility
     * 2. UDP DNS Query to endpoint:53
     * 3. TCP Socket Connection to endpoint:53
     * 4. TCP Socket Connection to endpoint:443
     * 5. HTTP captive portal 204 connectivity probe
     *
     * Returns Result.success(latencyMs) if reachable, or Result.failure on timeout/unreachable.
     */
    fun pingReliableEndpoint(endpoint: String = DEFAULT_PING_ENDPOINT, timeoutMs: Int = 2500): Result<Long> {
        val start = System.currentTimeMillis()

        // 1. ICMP Ping via Android OS ping utility
        try {
            val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "-w", "2", endpoint))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var pingOutputTime: Long? = null
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val str = line ?: continue
                if (str.contains("time=")) {
                    val msStr = str.substringAfter("time=").substringBefore(" ms").trim()
                    pingOutputTime = msStr.toDoubleOrNull()?.toLong()?.coerceAtLeast(1L)
                }
            }
            reader.close()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                val latency = pingOutputTime ?: (System.currentTimeMillis() - start).coerceAtLeast(1L)
                return Result.success(latency)
            }
        } catch (_: Exception) {
            // ICMP execution may be restricted on some sandboxes; fall through to socket probes
        }

        // 2. UDP DNS query to endpoint:53 (Google DNS)
        try {
            val datagramSocket = DatagramSocket()
            datagramSocket.soTimeout = timeoutMs
            // Minimal valid 17-byte DNS query payload for root domain "."
            val dnsQuery = byteArrayOf(
                0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01
            )
            val sendPacket = DatagramPacket(dnsQuery, dnsQuery.size, InetAddress.getByName(endpoint), 53)
            datagramSocket.send(sendPacket)
            val receiveBuf = ByteArray(512)
            val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
            datagramSocket.receive(receivePacket)
            datagramSocket.close()
            val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            return Result.success(latency)
        } catch (_: Exception) {
        }

        // 3. TCP Socket connection to port 53 (DNS-over-TCP)
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(endpoint, 53), timeoutMs)
            val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            socket.close()
            return Result.success(latency)
        } catch (_: Exception) {
        }

        // 4. TCP Socket connection to port 443
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(endpoint, 443), timeoutMs)
            val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            socket.close()
            return Result.success(latency)
        } catch (_: Exception) {
        }

        // 5. HTTP captive portal 204 connectivity probe
        try {
            val connection = (URL("http://connectivitycheck.gstatic.com/generate_204").openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = false
            }
            val code = connection.responseCode
            connection.disconnect()
            if (code == 204 || code in 200..299) {
                val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                return Result.success(latency)
            }
        } catch (_: Exception) {
        }

        return Result.failure(IOException("Ping to $endpoint timed out across all protocols. Internet unreachable."))
    }
}
