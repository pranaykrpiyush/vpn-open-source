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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.model.DnsOption
import com.example.model.TunnelMode
import com.example.model.VpnProtocol
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    selectedTunnelMode: TunnelMode,
    selectedProtocol: VpnProtocol,
    selectedDns: DnsOption,
    dnsOptions: List<DnsOption>,
    killSwitchEnabled: Boolean,
    mtuSize: Int,
    onTunnelModeChanged: (TunnelMode) -> Unit,
    onProtocolChanged: (VpnProtocol) -> Unit,
    onDnsChanged: (DnsOption) -> Unit,
    onKillSwitchChanged: (Boolean) -> Unit,
    onMtuChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Tunnel Routing Mode (Controls internet access)
        Text(
            text = "TUNNEL ROUTING MODE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = NeonCyan.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                TunnelMode.values().forEachIndexed { index, mode ->
                    val isSelected = selectedTunnelMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onTunnelModeChanged(mode) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NeonCyan else TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (mode == TunnelMode.SECURE_DNS_SHIELD) NeonEmerald.copy(alpha = 0.15f)
                                            else CyberSurfaceVariant
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = mode.badge,
                                        fontSize = 9.sp,
                                        color = if (mode == TunnelMode.SECURE_DNS_SHIELD) NeonEmerald else TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onTunnelModeChanged(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = NeonCyan,
                                unselectedColor = TextMuted
                            )
                        )
                    }

                    if (index < TunnelMode.values().size - 1) {
                        HorizontalDivider(
                            color = CyberBorder.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "TUNNEL PROTOCOL",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                VpnProtocol.values().forEachIndexed { index, protocol ->
                    val isSelected = selectedProtocol == protocol
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onProtocolChanged(protocol) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = protocol.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) NeonCyan else TextPrimary
                            )
                            Text(
                                text = when (protocol) {
                                    VpnProtocol.OPENVPN_UDP -> "High speed, ideal for gaming & video streaming"
                                    VpnProtocol.OPENVPN_TCP -> "Robust against restrictive network filtering"
                                    VpnProtocol.WIREGUARD -> "Modern cryptographic protocol with minimal overhead"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onProtocolChanged(protocol) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = NeonCyan,
                                unselectedColor = TextMuted
                            )
                        )
                    }

                    if (index < VpnProtocol.values().size - 1) {
                        HorizontalDivider(
                            color = CyberBorder.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "DNS LEAK PROTECTION",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                dnsOptions.forEachIndexed { index, dns ->
                    val isSelected = selectedDns.id == dns.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onDnsChanged(dns) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dns.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) NeonCyan else TextPrimary
                            )
                            Text(
                                text = "${dns.primaryIp} • ${dns.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onDnsChanged(dns) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = NeonCyan,
                                unselectedColor = TextMuted
                            )
                        )
                    }

                    if (index < dnsOptions.size - 1) {
                        HorizontalDivider(
                            color = CyberBorder.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SECURITY FIREWALL",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kill Switch",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Strictly prevents any unencrypted fallback traffic if the VPN drops unexpectedly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = killSwitchEnabled,
                        onCheckedChange = onKillSwitchChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = CyberSurfaceVariant,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberBackground
                        ),
                        modifier = Modifier.testTag("kill_switch_toggle")
                    )
                }

                HorizontalDivider(
                    color = CyberBorder.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Maximum Transmission Unit (MTU)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Current: $mtuSize bytes (Standard OpenVPN packet size)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1280, 1400, 1500).forEach { size ->
                            val isSelected = mtuSize == size
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else CyberSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else CyberBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onMtuChanged(size) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$size",
                                    fontSize = 11.sp,
                                    color = if (isSelected) NeonCyan else TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = NeonCyan.copy(alpha = 0.4f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Open Source",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% OPEN SOURCE & AUDITED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "OpenShield is built completely on open-source standards:\n" +
                            "• VPNGate Academic Volunteer Network (University of Tsukuba)\n" +
                            "• OpenVPN Protocol (GPLv2)\n" +
                            "• WireGuard Cryptographic Protocol (GPLv2)\n" +
                            "• Zero centralized tracking, zero analytics, zero data monetization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
