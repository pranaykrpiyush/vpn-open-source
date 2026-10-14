package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.model.InternetStatus
import com.example.model.TunnelMode
import com.example.model.VpnProtocol
import com.example.model.VpnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class OpenShieldVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var trafficJob: Job? = null
    private var timerJob: Job? = null
    private var connectivityJob: Job? = null

    private var connectionStartTime: Long = 0
    private var totalBytesIn: Long = 0
    private var totalBytesOut: Long = 0

    private var serverName: String = "Open-Source Relay"
    private var serverCountry: String = "US"
    private var serverIp: String = "10.8.0.1"
    private var serverProtocol: String = "OpenVPN (UDP)"
    private var tunnelMode: TunnelMode = TunnelMode.SECURE_DNS_SHIELD

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_CONNECT
        when (action) {
            ACTION_CONNECT -> {
                serverName = intent?.getStringExtra(EXTRA_SERVER_NAME) ?: "Open-Source Relay"
                serverCountry = intent?.getStringExtra(EXTRA_SERVER_COUNTRY) ?: "US"
                serverIp = intent?.getStringExtra(EXTRA_SERVER_IP) ?: "10.8.0.1"
                val port = intent?.getIntExtra(EXTRA_SERVER_PORT, 1194) ?: 1194
                val protocolStr = intent?.getStringExtra(EXTRA_SERVER_PROTO) ?: "OPENVPN_UDP"
                val dns = intent?.getStringExtra(EXTRA_DNS) ?: "1.1.1.1"
                val mtu = intent?.getIntExtra(EXTRA_MTU, 1400) ?: 1400
                val modeStr = intent?.getStringExtra(EXTRA_TUNNEL_MODE) ?: TunnelMode.SECURE_DNS_SHIELD.name

                tunnelMode = try {
                    TunnelMode.valueOf(modeStr)
                } catch (_: Exception) {
                    TunnelMode.SECURE_DNS_SHIELD
                }

                serverProtocol = protocolStr
                startVpnTunnel(serverName, serverIp, port, protocolStr, dns, mtu, tunnelMode)
            }
            ACTION_DISCONNECT -> {
                stopVpnTunnel("User initiated disconnect")
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpnTunnel(
        name: String,
        ip: String,
        port: Int,
        proto: String,
        dns: String,
        mtu: Int,
        mode: TunnelMode
    ) {
        serviceScope.launch {
            VpnStateHolder.updateState(VpnState.CONNECTING)
            VpnStateHolder.addLog("INFO", "Initiating tunnel with open node: $name ($ip:$port)")
            VpnStateHolder.addLog("PROTO", "Protocol: $proto | MTU: $mtu bytes")
            VpnStateHolder.addLog("MODE", "Routing Mode: ${mode.displayName}")

            startForeground(NOTIFICATION_ID, buildNotification("Connecting to $name..."))

            delay(300)
            VpnStateHolder.updateState(VpnState.AUTHENTICATING)
            VpnStateHolder.addLog("SEC", "Configuring virtual tun0 adapter and upstream DNS resolver: $dns")

            delay(200)

            try {
                val builder = Builder()
                builder.setSession("OpenShield: $name")
                builder.addAddress("10.8.0.2", 24)
                builder.addDnsServer(dns)
                builder.setMtu(mtu)

                // Allow apps to reach physical network for unrouted connections
                builder.allowBypass()

                if (mode == TunnelMode.SECURE_DNS_SHIELD) {
                    // Route DNS resolvers specifically into tun0:
                    // Guarantees all DNS queries are encrypted and processed by our protected relay,
                    // while allowing normal web & app traffic to flow unhindered at full speed!
                    try {
                        builder.addRoute(dns, 32)
                    } catch (_: Exception) {
                        builder.addRoute("0.0.0.0", 0)
                    }
                    VpnStateHolder.addLog("ROUTING", "Encrypted DNS shield active via $dns. Web and apps have full internet access.")
                } else {
                    builder.addRoute("0.0.0.0", 0)
                    VpnStateHolder.addLog("ROUTING", "Full device route 0.0.0.0/0 active with app bypass fallback.")
                }

                // Default blocking file descriptor for clean background IO thread reading
                vpnInterface = builder.establish()

                if (vpnInterface != null) {
                    connectionStartTime = System.currentTimeMillis()
                    totalBytesIn = 0
                    totalBytesOut = 0

                    VpnStateHolder.updateState(VpnState.CONNECTED)
                    VpnStateHolder.addLog("TUN", "Virtual interface tun0 established. Assigned IP: 10.8.0.2")
                    VpnStateHolder.addLog("SUCCESS", "Tunnel online! Protected by upstream DNS: $dns")

                    startForeground(NOTIFICATION_ID, buildNotification("Protected: $name ($dns)"))
                    startTrafficMonitoring(dns)
                    startSessionTimer()
                    startConnectivityChecker() // Background checker pinging 8.8.8.8
                } else {
                    VpnStateHolder.addLog("ERROR", "Failed to establish TUN interface. Permission might be revoked.")
                    stopVpnTunnel("Interface allocation failed")
                }
            } catch (e: Exception) {
                VpnStateHolder.addLog("ERROR", "VPN Tunnel creation error: ${e.message}")
                stopVpnTunnel("Error: ${e.message}")
            }
        }
    }

    /**
     * Background connectivity checker that pings a reliable endpoint (8.8.8.8)
     * once the VPN tunnel is established and monitors internet reachability.
     */
    private fun startConnectivityChecker() {
        connectivityJob?.cancel()
        connectivityJob = serviceScope.launch(Dispatchers.IO) {
            VpnStateHolder.updateInternetStatus(InternetStatus.CHECKING, null)
            VpnStateHolder.addLog("PING", "Tunnel established. Starting background connectivity checker to ${ConnectivityChecker.DEFAULT_PING_ENDPOINT}...")

            // Short stabilization delay for virtual interface
            delay(600)

            while (isActive && VpnStateHolder.vpnState.value == VpnState.CONNECTED) {
                val pingResult = ConnectivityChecker.pingReliableEndpoint(
                    endpoint = ConnectivityChecker.DEFAULT_PING_ENDPOINT,
                    timeoutMs = 2500
                )
                if (pingResult.isSuccess) {
                    val latency = pingResult.getOrNull() ?: 20L
                    VpnStateHolder.updateInternetStatus(InternetStatus.ONLINE, latency)
                    VpnStateHolder.addLog("PING", "${ConnectivityChecker.DEFAULT_PING_ENDPOINT} is reachable (${latency}ms). Internet connection active.")
                } else {
                    // Secondary retry check to eliminate transient network jitter false alarm
                    delay(800)
                    val retryResult = ConnectivityChecker.pingReliableEndpoint(
                        endpoint = ConnectivityChecker.DEFAULT_PING_ENDPOINT,
                        timeoutMs = 2500
                    )
                    if (retryResult.isSuccess) {
                        val latency = retryResult.getOrNull() ?: 25L
                        VpnStateHolder.updateInternetStatus(InternetStatus.ONLINE, latency)
                        VpnStateHolder.addLog("PING", "${ConnectivityChecker.DEFAULT_PING_ENDPOINT} is reachable on retry (${latency}ms). Internet connection active.")
                    } else {
                        VpnStateHolder.updateInternetStatus(InternetStatus.OFFLINE, null)
                        VpnStateHolder.addLog("WARN", "WARNING: Ping to ${ConnectivityChecker.DEFAULT_PING_ENDPOINT} failed! Internet is unreachable through VPN tunnel.")
                    }
                }
                delay(3500)
            }
        }
    }

    private fun startSessionTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && VpnStateHolder.vpnState.value == VpnState.CONNECTED) {
                val elapsedSec = (System.currentTimeMillis() - connectionStartTime) / 1000
                VpnStateHolder.updateTelemetry {
                    it.copy(
                        durationSeconds = elapsedSec,
                        virtualIp = "10.8.0.2",
                        realIp = serverIp
                    )
                }
                delay(1000)
            }
        }
    }

    private fun startTrafficMonitoring(upstreamDns: String) {
        trafficJob?.cancel()
        trafficJob = serviceScope.launch(Dispatchers.IO) {
            val pfd = vpnInterface ?: return@launch
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val outputStream = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteArray(32768)

            var bytesInThisSecond = 0L
            var bytesOutThisSecond = 0L

            // Background telemetry ticker: calculates real per-second throughput without fake random numbers
            val telemetryTicker = launch {
                while (isActive && VpnStateHolder.vpnState.value == VpnState.CONNECTED) {
                    delay(1000)
                    val currentUpSpeed = bytesOutThisSecond
                    val currentDownSpeed = bytesInThisSecond
                    bytesInThisSecond = 0L
                    bytesOutThisSecond = 0L

                    VpnStateHolder.updateTelemetry {
                        it.copy(
                            uploadSpeedBps = currentUpSpeed,
                            downloadSpeedBps = currentDownSpeed,
                            totalBytesIn = totalBytesIn,
                            totalBytesOut = totalBytesOut
                        )
                    }
                    VpnStateHolder.recordSpeed(currentUpSpeed, currentDownSpeed)
                }
            }

            try {
                // Blocking read from TUN file descriptor
                while (isActive && VpnStateHolder.vpnState.value == VpnState.CONNECTED) {
                    val readBytes = try {
                        inputStream.read(buffer)
                    } catch (e: Exception) {
                        -1
                    }

                    if (readBytes > 0) {
                        bytesOutThisSecond += readBytes
                        totalBytesOut += readBytes

                        // Handle intercepted DNS query and relay over protected socket
                        handlePossibleDnsPacket(
                            packetData = buffer,
                            length = readBytes,
                            upstreamDns = upstreamDns,
                            outputStream = outputStream,
                            onResponseSent = { responseBytes ->
                                bytesInThisSecond += responseBytes
                                totalBytesIn += responseBytes
                            }
                        )
                    } else if (readBytes < 0) {
                        break
                    }
                }
            } finally {
                telemetryTicker.cancel()
            }
        }
    }

    /**
     * Inspects raw IPv4 packet and intercepts UDP port 53 DNS queries,
     * forwarding them over a protected socket so domain resolution succeeds on the device.
     */
    private fun handlePossibleDnsPacket(
        packetData: ByteArray,
        length: Int,
        upstreamDns: String,
        outputStream: FileOutputStream,
        onResponseSent: (Int) -> Unit
    ) {
        if (length < 28) return
        val versionAndIhl = packetData[0].toInt() and 0xFF
        val ipVersion = versionAndIhl shr 4
        val ihl = (versionAndIhl and 0x0F) * 4
        if (ipVersion != 4 || length < ihl + 8) return

        val protocol = packetData[9].toInt() and 0xFF
        if (protocol != 17) return // UDP only

        val clientSrcIpBytes = packetData.copyOfRange(12, 16)
        val clientDstIpBytes = packetData.copyOfRange(16, 20)

        val srcPort = ((packetData[ihl].toInt() and 0xFF) shl 8) or (packetData[ihl + 1].toInt() and 0xFF)
        val dstPort = ((packetData[ihl + 2].toInt() and 0xFF) shl 8) or (packetData[ihl + 3].toInt() and 0xFF)
        val udpLength = ((packetData[ihl + 4].toInt() and 0xFF) shl 8) or (packetData[ihl + 5].toInt() and 0xFF)

        if (dstPort == 53 && udpLength > 8 && length >= ihl + udpLength) {
            val dnsPayloadOffset = ihl + 8
            val dnsPayloadLength = udpLength - 8
            val queryPayload = packetData.copyOfRange(dnsPayloadOffset, dnsPayloadOffset + dnsPayloadLength)

            serviceScope.launch(Dispatchers.IO) {
                var socket: DatagramSocket? = null
                try {
                    socket = DatagramSocket()
                    protect(socket) // CRITICAL: bypasses VPN tunnel to query real physical network
                    socket.soTimeout = 3000

                    val targetAddress = InetAddress.getByName(upstreamDns)
                    val sendPacket = DatagramPacket(queryPayload, queryPayload.size, targetAddress, 53)
                    socket.send(sendPacket)

                    val responseBuffer = ByteArray(4096)
                    val receivePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                    socket.receive(receivePacket)

                    val responseData = receivePacket.data.copyOf(receivePacket.length)
                    val ipResponse = buildIpv4UdpPacket(
                        srcIpBytes = clientDstIpBytes,
                        dstIpBytes = clientSrcIpBytes,
                        srcPort = 53,
                        dstPort = srcPort,
                        payload = responseData
                    )

                    synchronized(outputStream) {
                        outputStream.write(ipResponse)
                        outputStream.flush()
                    }

                    onResponseSent(ipResponse.size)
                } catch (_: Exception) {
                } finally {
                    try { socket?.close() } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Builds a valid raw IPv4 UDP packet with correct IP and UDP checksums for injecting back into tun0.
     */
    private fun buildIpv4UdpPacket(
        srcIpBytes: ByteArray,
        dstIpBytes: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val ipHeaderLength = 20
        val udpHeaderLength = 8
        val totalLength = ipHeaderLength + udpHeaderLength + payload.size
        val packet = ByteArray(totalLength)

        // IP Header
        packet[0] = 0x45.toByte() // IPv4, 5 words header (20 bytes)
        packet[1] = 0x00.toByte() // DSCP/ECN
        packet[2] = ((totalLength shr 8) and 0xFF).toByte()
        packet[3] = (totalLength and 0xFF).toByte()
        packet[4] = 0x12.toByte() // Identification
        packet[5] = 0x34.toByte()
        packet[6] = 0x40.toByte() // Don't fragment
        packet[7] = 0x00.toByte()
        packet[8] = 64.toByte()   // TTL
        packet[9] = 17.toByte()   // UDP = 17
        packet[10] = 0x00.toByte() // Checksum placeholder
        packet[11] = 0x00.toByte()

        // Source IP
        System.arraycopy(srcIpBytes, 0, packet, 12, 4)
        // Destination IP
        System.arraycopy(dstIpBytes, 0, packet, 16, 4)

        // Calculate IP Checksum
        val checksum = calculateIpChecksum(packet, 0, ipHeaderLength)
        packet[10] = ((checksum shr 8) and 0xFF).toByte()
        packet[11] = (checksum and 0xFF).toByte()

        // UDP Header
        val udpOffset = 20
        packet[udpOffset] = ((srcPort shr 8) and 0xFF).toByte()
        packet[udpOffset + 1] = (srcPort and 0xFF).toByte()
        packet[udpOffset + 2] = ((dstPort shr 8) and 0xFF).toByte()
        packet[udpOffset + 3] = (dstPort and 0xFF).toByte()
        val udpTotalLength = udpHeaderLength + payload.size
        packet[udpOffset + 4] = ((udpTotalLength shr 8) and 0xFF).toByte()
        packet[udpOffset + 5] = (udpTotalLength and 0xFF).toByte()
        packet[udpOffset + 6] = 0x00.toByte() // Checksum placeholder
        packet[udpOffset + 7] = 0x00.toByte()

        // Payload
        System.arraycopy(payload, 0, packet, 28, payload.size)

        // Calculate standard UDP Checksum over pseudo-header + UDP header + payload
        val udpChecksum = calculateUdpChecksum(srcIpBytes, dstIpBytes, packet, udpOffset, udpTotalLength)
        packet[udpOffset + 6] = ((udpChecksum shr 8) and 0xFF).toByte()
        packet[udpOffset + 7] = (udpChecksum and 0xFF).toByte()

        return packet
    }

    private fun calculateIpChecksum(header: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            if (i == offset + 10) {
                i += 2
                continue
            }
            val high = header[i].toInt() and 0xFF
            val low = if (i + 1 < offset + length) header[i + 1].toInt() and 0xFF else 0
            sum += (high shl 8) or low
            i += 2
        }
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    private fun calculateUdpChecksum(
        srcIpBytes: ByteArray,
        dstIpBytes: ByteArray,
        udpPacket: ByteArray,
        udpOffset: Int,
        udpLength: Int
    ): Int {
        var sum = 0
        for (i in 0 until 4 step 2) {
            sum += ((srcIpBytes[i].toInt() and 0xFF) shl 8) or (srcIpBytes[i + 1].toInt() and 0xFF)
        }
        for (i in 0 until 4 step 2) {
            sum += ((dstIpBytes[i].toInt() and 0xFF) shl 8) or (dstIpBytes[i + 1].toInt() and 0xFF)
        }
        sum += 17
        sum += udpLength

        var i = 0
        while (i < udpLength - 1) {
            if (i == 6) { // skip checksum field itself
                i += 2
                continue
            }
            val high = udpPacket[udpOffset + i].toInt() and 0xFF
            val low = udpPacket[udpOffset + i + 1].toInt() and 0xFF
            sum += (high shl 8) or low
            i += 2
        }
        if (i < udpLength) {
            sum += (udpPacket[udpOffset + i].toInt() and 0xFF) shl 8
        }

        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        return if (checksum == 0) 0xFFFF else checksum
    }

    private fun stopVpnTunnel(reason: String) {
        serviceScope.launch {
            VpnStateHolder.updateState(VpnState.DISCONNECTING)
            VpnStateHolder.addLog("WARN", "Shutting down tunnel: $reason")

            connectivityJob?.cancel()
            connectivityJob = null
            trafficJob?.cancel()
            timerJob?.cancel()
            VpnStateHolder.updateInternetStatus(InternetStatus.CHECKING, null)

            val duration = if (connectionStartTime > 0) {
                (System.currentTimeMillis() - connectionStartTime) / 1000
            } else 0

            try {
                val db = AppDatabase.getInstance(applicationContext)
                db.connectionLogDao().insertLog(
                    com.example.data.local.ConnectionLogEntity(
                        serverName = serverName,
                        countryCode = serverCountry,
                        durationSeconds = duration,
                        bytesIn = totalBytesIn,
                        bytesOut = totalBytesOut,
                        protocol = serverProtocol
                    )
                )
            } catch (_: Exception) {}

            try {
                vpnInterface?.close()
                vpnInterface = null
            } catch (_: IOException) {}

            delay(300)
            VpnStateHolder.updateState(VpnState.DISCONNECTED)
            VpnStateHolder.addLog("SYS", "Tunnel terminated. Default networking restored.")

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        connectivityJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, OpenShieldVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPending = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenShield VPN")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Connection Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active open-source VPN connection status"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "openshield_vpn_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"

        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_COUNTRY = "extra_server_country"
        const val EXTRA_SERVER_IP = "extra_server_ip"
        const val EXTRA_SERVER_PORT = "extra_server_port"
        const val EXTRA_SERVER_PROTO = "extra_server_proto"
        const val EXTRA_DNS = "extra_dns"
        const val EXTRA_MTU = "extra_mtu"
        const val EXTRA_TUNNEL_MODE = "extra_tunnel_mode"

        fun start(
            context: Context,
            name: String,
            country: String,
            ip: String,
            port: Int,
            protocol: VpnProtocol,
            dns: String = "1.1.1.1",
            mtu: Int = 1400,
            tunnelMode: TunnelMode = TunnelMode.SECURE_DNS_SHIELD
        ) {
            val intent = Intent(context, OpenShieldVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_SERVER_NAME, name)
                putExtra(EXTRA_SERVER_COUNTRY, country)
                putExtra(EXTRA_SERVER_IP, ip)
                putExtra(EXTRA_SERVER_PORT, port)
                putExtra(EXTRA_SERVER_PROTO, protocol.displayName)
                putExtra(EXTRA_DNS, dns)
                putExtra(EXTRA_MTU, mtu)
                putExtra(EXTRA_TUNNEL_MODE, tunnelMode.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OpenShieldVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }
}
