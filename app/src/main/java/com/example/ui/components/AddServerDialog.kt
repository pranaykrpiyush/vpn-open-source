package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.VpnProtocol
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, ip: String, port: Int, countryCode: String, countryName: String, protocol: VpnProtocol, config: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("1194") }
    var countryCode by remember { mutableStateOf("US") }
    var countryName by remember { mutableStateOf("United States") }
    var selectedProtocol by remember { mutableStateOf(VpnProtocol.OPENVPN_UDP) }
    var configText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IMPORT OPEN CONFIG",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        name = "Free Open Relay - Tsukuba Node"
                        ip = "130.158.6.140"
                        portText = "1194"
                        countryCode = "JP"
                        countryName = "Japan"
                        selectedProtocol = VpnProtocol.OPENVPN_UDP
                        configText = "client\ndev tun\nproto udp\nremote 130.158.6.140 1194\nresolv-retry infinite\nnobind\npersist-key\npersist-tun\ncipher AES-256-GCM"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSurfaceVariant,
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Fill sample",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-fill Sample Open-Source Node", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                CustomTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Profile / Server Name",
                    placeholder = "e.g. Tokyo Academic Relay",
                    modifier = Modifier.testTag("custom_server_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CustomTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = "Host / IP Address",
                        placeholder = "198.51.100.25",
                        modifier = Modifier
                            .weight(2f)
                            .testTag("custom_server_ip_input")
                    )

                    CustomTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = "Port",
                        placeholder = "1194",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CustomTextField(
                        value = countryCode,
                        onValueChange = { countryCode = it.take(2).uppercase() },
                        label = "Code",
                        placeholder = "US",
                        modifier = Modifier.weight(1f)
                    )

                    CustomTextField(
                        value = countryName,
                        onValueChange = { countryName = it },
                        label = "Country Name",
                        placeholder = "United States",
                        modifier = Modifier.weight(2f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "PROTOCOL",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VpnProtocol.values().forEach { proto ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedProtocol = proto
                                    if (proto == VpnProtocol.WIREGUARD) portText = "51820"
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedProtocol == proto,
                                onClick = {
                                    selectedProtocol = proto
                                    if (proto == VpnProtocol.WIREGUARD) portText = "51820"
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Text(
                                text = proto.tag,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                CustomTextField(
                    value = configText,
                    onValueChange = { configText = it },
                    label = "Config Text (.ovpn / WireGuard payload)",
                    placeholder = "Paste raw open-source configuration directives...",
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            val port = portText.toIntOrNull() ?: 1194
                            onAdd(name, ip, port, countryCode, countryName, selectedProtocol, configText)
                            onDismiss()
                        },
                        enabled = name.isNotBlank() && ip.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = CyberBackground
                        ),
                        modifier = Modifier.testTag("save_custom_server_button")
                    ) {
                        Text("Save & Select", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, color = TextMuted) },
        placeholder = { Text(placeholder, fontSize = 12.sp, color = TextMuted) },
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = CyberBorder,
            focusedContainerColor = CyberBackground,
            unfocusedContainerColor = CyberBackground,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = modifier
    )
}
