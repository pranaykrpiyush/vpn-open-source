package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionTelemetry
import com.example.model.InternetStatus
import com.example.model.SpeedPoint
import com.example.model.TunnelMode
import com.example.model.VpnServer
import com.example.model.VpnState
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberPulseButton
import com.example.ui.components.ThroughputGraph
import com.example.ui.components.formatBytes
import com.example.ui.components.formatDuration
import com.example.ui.components.formatSpeed
import com.example.ui.components.getFlagEmoji
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    vpnState: VpnState,
    currentServer: VpnServer?,
    telemetry: ConnectionTelemetry,
    speedHistory: List<SpeedPoint>,
    killSwitchEnabled: Boolean,
    dnsName: String,
    tunnelMode: TunnelMode,
    internetStatus: InternetStatus,
    lastPingLatencyMs: Long? = null,
    onConnectToggle: () -> Unit,
    onNavigateToServers: () -> Unit,
    onQuickFixInternet: () -> Unit,
    onToggleTunnelMode: () -> Unit,
    onRetryPing: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isConnected = vpnState == VpnState.CONNECTED
    val isConnecting = vpnState == VpnState.CONNECTING || vpnState == VpnState.AUTHENTICATING

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Header Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(
                    when {
                        isConnected && internetStatus == InternetStatus.OFFLINE -> NeonRed.copy(alpha = 0.20f)
                        isConnected -> NeonEmerald.copy(alpha = 0.15f)
                        isConnecting -> NeonAmber.copy(alpha = 0.15f)
                        else -> CyberSurfaceVariant
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isConnected && internetStatus == InternetStatus.OFFLINE -> NeonRed.copy(alpha = 0.8f)
                        isConnected -> NeonEmerald.copy(alpha = 0.5f)
                        isConnecting -> NeonAmber.copy(alpha = 0.5f)
                        else -> CyberBorder
                    },
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isConnected && internetStatus == InternetStatus.OFFLINE -> NeonRed
                            isConnected -> NeonEmerald
                            isConnecting -> NeonAmber
                            else -> NeonRed
                        }
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    isConnected && internetStatus == InternetStatus.OFFLINE -> "⚠️ TUNNEL ACTIVE • NO INTERNET (PING 8.8.8.8 FAILED)"
                    isConnected && internetStatus == InternetStatus.ONLINE -> "SHIELD ACTIVE • 8.8.8.8 ONLINE (${lastPingLatencyMs ?: 20}ms)"
                    isConnected -> "SHIELD ACTIVE • PINGING 8.8.8.8..."
                    isConnecting -> "ESTABLISHING OPEN TUNNEL..."
                    else -> "UNPROTECTED • TAP TO SECURE"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = when {
                    isConnected && internetStatus == InternetStatus.OFFLINE -> NeonRed
                    isConnected -> NeonEmerald
                    isConnecting -> NeonAmber
                    else -> TextSecondary
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Internet Connectivity Status Banner
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when (internetStatus) {
                        InternetStatus.ONLINE -> NeonEmerald.copy(alpha = 0.10f)
                        InternetStatus.OFFLINE -> NeonRed.copy(alpha = 0.15f)
                        InternetStatus.CHECKING -> CyberSurfaceVariant
                    }
                )
                .border(
                    1.dp,
                    when (internetStatus) {
                        InternetStatus.ONLINE -> NeonEmerald.copy(alpha = 0.35f)
                        InternetStatus.OFFLINE -> NeonRed.copy(alpha = 0.5f)
                        InternetStatus.CHECKING -> CyberBorder
                    },
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (internetStatus) {
                    InternetStatus.ONLINE -> Icons.Default.CheckCircle
                    InternetStatus.OFFLINE -> Icons.Default.Warning
                    InternetStatus.CHECKING -> Icons.Default.NetworkCheck
                },
                contentDescription = "Internet Status",
                tint = when (internetStatus) {
                    InternetStatus.ONLINE -> NeonEmerald
                    InternetStatus.OFFLINE -> NeonRed
                    InternetStatus.CHECKING -> TextMuted
                },
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when (internetStatus) {
                    InternetStatus.ONLINE -> "8.8.8.8 Ping: ${lastPingLatencyMs ?: 20}ms • Reachable"
                    InternetStatus.OFFLINE -> "8.8.8.8 Ping: Unreachable • Packet Timeout"
                    InternetStatus.CHECKING -> "Pinging 8.8.8.8 (Checking Internet)..."
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = when (internetStatus) {
                    InternetStatus.ONLINE -> NeonEmerald
                    InternetStatus.OFFLINE -> NeonRed
                    InternetStatus.CHECKING -> TextMuted
                }
            )
        }

        if (internetStatus == InternetStatus.OFFLINE && isConnected) {
            Spacer(modifier = Modifier.height(12.dp))
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("internet_unreachable_warning_card"),
                borderColor = NeonRed,
                glowColor = NeonRed
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Internet Unreachable",
                                tint = NeonRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WARNING: INTERNET UNREACHABLE",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                ),
                                color = NeonRed
                            )
                            Text(
                                text = "Tunnel established • Ping to 8.8.8.8 timed out",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A0D11))
                            .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (tunnelMode == TunnelMode.FULL_TUNNEL) {
                                "Full Route (0.0.0.0/0) redirects all device traffic to tun0. Because open relays do not proxy raw TCP packets, web browsers and apps cannot access the internet."
                            } else {
                                "The VPN tunnel is established, but ping packets to endpoint 8.8.8.8 are timing out. Web pages and apps cannot load."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB4AB),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onQuickFixInternet,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald,
                                contentColor = Color(0xFF002213)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_internet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fix Now: Switch to Secure DNS Shield",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRetryPing,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("retry_ping_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Re-ping 8.8.8.8",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            }

                            OutlinedButton(
                                onClick = onDisconnect,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("disconnect_vpn_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Disconnect",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Cyber Pulse Connect Button
        CyberPulseButton(
            vpnState = vpnState,
            onClick = onConnectToggle
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Selected Server Card
        CyberCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToServers() }
                .testTag("selected_server_card"),
            glowColor = if (isConnected) NeonCyan else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = getFlagEmoji(currentServer?.countryCode ?: "US"),
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = currentServer?.name ?: "Select Open-Source Relay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentServer?.countryName ?: "United States",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${currentServer?.pingMs ?: 32} ms",
                                    fontSize = 10.sp,
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberSurfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentServer?.protocol?.tag ?: "UDP",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "CHANGE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Change Server",
                        tint = NeonCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tunnel Mode Toggle Card
        CyberCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleTunnelMode() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Tunnel Mode",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MODE: ${tunnelMode.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (tunnelMode == TunnelMode.SECURE_DNS_SHIELD)
                                "Guaranteed full-speed internet • Encrypted DNS"
                            else
                                "Full 0.0.0.0/0 device routing with fallback",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SWITCH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Speed & Waveform Telemetry Card
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Download",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DOWNLOAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isConnected) formatSpeed(telemetry.downloadSpeedBps) else "0.0 Mbps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Upload",
                                tint = NeonAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "UPLOAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isConnected) formatSpeed(telemetry.uploadSpeedBps) else "0.0 Mbps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                ThroughputGraph(speedHistory = speedHistory)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session Stats Matrix
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CyberCard(modifier = Modifier.weight(1f)) {
                MetricItem(
                    icon = Icons.Default.Timer,
                    label = "DURATION",
                    value = if (isConnected) formatDuration(telemetry.durationSeconds) else "--:--",
                    tint = NeonCyan
                )
            }
            CyberCard(modifier = Modifier.weight(1f)) {
                MetricItem(
                    icon = Icons.Default.Speed,
                    label = "DATA USAGE",
                    value = if (isConnected) formatBytes(telemetry.totalBytesIn + telemetry.totalBytesOut) else "0 MB",
                    tint = NeonEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CyberCard(modifier = Modifier.weight(1f)) {
                MetricItem(
                    icon = Icons.Default.Dns,
                    label = "VIRTUAL IP",
                    value = if (isConnected) telemetry.virtualIp else "None",
                    tint = TextPrimary
                )
            }
            CyberCard(modifier = Modifier.weight(1f)) {
                MetricItem(
                    icon = Icons.Default.Lock,
                    label = "CIPHER",
                    value = telemetry.cipher,
                    tint = NeonAmber
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CyberCard(modifier = Modifier.weight(1f)) {
                MetricItem(
                    icon = Icons.Default.NetworkCheck,
                    label = "8.8.8.8 PING",
                    value = when {
                        !isConnected -> "--"
                        internetStatus == InternetStatus.ONLINE -> "${lastPingLatencyMs ?: 20} ms"
                        internetStatus == InternetStatus.OFFLINE -> "Timeout"
                        else -> "Pinging..."
                    },
                    tint = when {
                        !isConnected -> TextMuted
                        internetStatus == InternetStatus.ONLINE -> NeonEmerald
                        internetStatus == InternetStatus.OFFLINE -> NeonRed
                        else -> NeonAmber
                    }
                )
            }
            CyberCard(modifier = Modifier.weight(1f)) {
                MetricItem(
                    icon = if (internetStatus == InternetStatus.ONLINE) Icons.Default.CheckCircle else Icons.Default.Warning,
                    label = "CONNECTIVITY",
                    value = when {
                        !isConnected -> "Standby"
                        internetStatus == InternetStatus.ONLINE -> "Online"
                        internetStatus == InternetStatus.OFFLINE -> "Unreachable"
                        else -> "Checking..."
                    },
                    tint = when {
                        !isConnected -> TextMuted
                        internetStatus == InternetStatus.ONLINE -> NeonEmerald
                        internetStatus == InternetStatus.OFFLINE -> NeonRed
                        else -> NeonAmber
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecurityFeatureChip(
                icon = Icons.Default.Shield,
                title = "Kill Switch",
                subtitle = if (killSwitchEnabled) "Active" else "Off",
                isActive = killSwitchEnabled,
                modifier = Modifier.weight(1f)
            )
            SecurityFeatureChip(
                icon = Icons.Default.Dns,
                title = "DNS Shield",
                subtitle = dnsName.substringBefore(" "),
                isActive = true,
                modifier = Modifier.weight(1f)
            )
            SecurityFeatureChip(
                icon = Icons.Default.VpnKey,
                title = "Open Source",
                subtitle = "GPL Relays",
                isActive = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SecurityFeatureChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, if (isActive) NeonCyan.copy(alpha = 0.3f) else CyberBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) NeonCyan else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (isActive) NeonEmerald else TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
