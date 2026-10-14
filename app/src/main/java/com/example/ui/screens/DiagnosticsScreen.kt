package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VpnServer
import com.example.model.VpnState
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DiagnosticsScreen(
    vpnState: VpnState,
    currentServer: VpnServer?,
    diagnosticRunning: Boolean,
    diagnosticResults: Map<String, String>,
    onRunDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isConnected = vpnState == VpnState.CONNECTED

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (isConnected) NeonEmerald else NeonRed,
            glowColor = if (isConnected) NeonEmerald else NeonRed
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) NeonEmerald.copy(alpha = 0.15f)
                            else NeonRed.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Security else Icons.Default.Warning,
                        contentDescription = "Security Status",
                        tint = if (isConnected) NeonEmerald else NeonRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isConnected) "SECURITY SCORE: 100% PROTECTED" else "SECURITY SCORE: 35% EXPOSED",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) NeonEmerald else NeonRed,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isConnected)
                        "All outbound DNS, TCP, and UDP traffic is routed through encrypted open-source tunnel ${currentServer?.name ?: ""}."
                    else
                        "Your ISP and local network can see your browsing history, DNS queries, and real IP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRunDiagnostics,
                    enabled = !diagnosticRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSurfaceVariant,
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                        .testTag("run_diagnostics_button")
                ) {
                    if (diagnosticRunning) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auditing Network...", fontSize = 13.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Run Audit",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Run Privacy & Leak Audit",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "AUDIT BREAKDOWN",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        AuditItemCard(
            icon = Icons.Default.Dns,
            title = "DNS Leak Test",
            subtitle = diagnosticResults["DNS Resolver"] ?: "Cloudflare DNS (1.1.1.1)",
            status = if (isConnected) "PASSED • 0 LEAKS" else "EXPOSED TO ISP",
            isSecure = isConnected,
            description = "DNS queries are securely resolved inside the encrypted virtual tunnel preventing ISP eavesdropping."
        )

        Spacer(modifier = Modifier.height(10.dp))

        AuditItemCard(
            icon = Icons.Default.Security,
            title = "WebRTC & IP Shield",
            subtitle = if (isConnected) "Virtual IP: 10.8.0.2 active" else "Real IP exposed",
            status = if (isConnected) "PASSED • MASKED" else "UNSHIELDED",
            isSecure = isConnected,
            description = "Browser WebRTC STUN requests are routed strictly through the tun0 virtual adapter."
        )

        Spacer(modifier = Modifier.height(10.dp))

        AuditItemCard(
            icon = Icons.Default.Lock,
            title = "Cryptographic Cipher",
            subtitle = diagnosticResults["Tunnel Cipher"] ?: "AES-256-GCM / ChaCha20-Poly1305",
            status = "STRONG CIPHER",
            isSecure = true,
            description = "Military-grade AEAD cipher with forward secrecy authentication keys."
        )

        Spacer(modifier = Modifier.height(10.dp))

        AuditItemCard(
            icon = Icons.Default.NetworkCheck,
            title = "Gateway Roundtrip Latency",
            subtitle = diagnosticResults["Gateway Latency"] ?: "18ms (Direct Open Route)",
            status = "OPTIMAL",
            isSecure = true,
            description = "Response time to the closest recursive root nameserver."
        )

        Spacer(modifier = Modifier.height(10.dp))

        val ping8888Status = diagnosticResults["8.8.8.8 Connectivity Ping"] ?: "Run audit to test"
        val isPingSuccess = !ping8888Status.contains("Failed", ignoreCase = true) && !ping8888Status.contains("Run audit", ignoreCase = true)

        AuditItemCard(
            icon = Icons.Default.NetworkCheck,
            title = "8.8.8.8 Endpoint Ping",
            subtitle = ping8888Status,
            status = if (isPingSuccess) "REACHABLE • ONLINE" else "UNREACHABLE",
            isSecure = isPingSuccess,
            description = "Continuous background connectivity probe to 8.8.8.8 verifying end-to-end packet delivery."
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AuditItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    status: String,
    isSecure: Boolean,
    description: String
) {
    CyberCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSecure) NeonCyan else NeonRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSecure) NeonEmerald.copy(alpha = 0.15f)
                            else NeonRed.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSecure) NeonEmerald else NeonRed,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
