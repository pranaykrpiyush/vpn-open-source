package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedPoint
import com.example.model.VpnState
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
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CyberBorder,
    glowColor: Color? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (glowColor != null) {
                        listOf(glowColor.copy(alpha = 0.6f), borderColor, borderColor)
                    } else {
                        listOf(borderColor, borderColor.copy(alpha = 0.5f))
                    }
                ),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CyberSurface
        )
    ) {
        content()
    }
}

@Composable
fun CyberPulseButton(
    vpnState: VpnState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = vpnState == VpnState.CONNECTED
    val isConnecting = vpnState == VpnState.CONNECTING || vpnState == VpnState.AUTHENTICATING

    val stateColor = when {
        isConnected -> NeonEmerald
        isConnecting -> NeonAmber
        else -> NeonCyan
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnecting || isConnected) 1.22f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 800 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isConnected || isConnecting) 0.45f else 0.2f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 800 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(220.dp)
            .testTag("vpn_power_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring 1
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(stateColor.copy(alpha = pulseAlpha))
        )

        // Outer pulsing ring 2
        Box(
            modifier = Modifier
                .size(175.dp)
                .scale(pulseScale * 0.95f)
                .clip(CircleShape)
                .border(1.5.dp, stateColor.copy(alpha = pulseAlpha * 2f), CircleShape)
        )

        // Core Shield Orb Button
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            stateColor.copy(alpha = 0.25f),
                            CyberSurfaceVariant,
                            CyberSurface
                        )
                    )
                )
                .border(2.5.dp, stateColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 75.dp),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        isConnecting -> Icons.Default.Refresh
                        isConnected -> Icons.Default.Shield
                        else -> Icons.Default.LockOpen
                    },
                    contentDescription = "VPN Shield Status",
                    tint = stateColor,
                    modifier = Modifier.size(46.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (vpnState) {
                        VpnState.CONNECTED -> "DISCONNECT"
                        VpnState.CONNECTING -> "CONNECTING"
                        VpnState.AUTHENTICATING -> "VERIFYING"
                        VpnState.DISCONNECTING -> "STOPPING"
                        VpnState.DISCONNECTED -> "CONNECT"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = stateColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ThroughputGraph(
    speedHistory: List<SpeedPoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CyberBackground.copy(alpha = 0.6f))
            .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (speedHistory.size < 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Awaiting network packets...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val maxSpeed = (speedHistory.maxOfOrNull { maxOf(it.downloadKbps, it.uploadKbps) } ?: 100f).coerceAtLeast(50f)

                val downloadPoints = speedHistory.mapIndexed { index, point ->
                    val x = (index.toFloat() / (speedHistory.size - 1)) * width
                    val y = height - (point.downloadKbps / maxSpeed) * (height - 12f) - 6f
                    Offset(x, y.coerceIn(4f, height - 4f))
                }

                val uploadPoints = speedHistory.mapIndexed { index, point ->
                    val x = (index.toFloat() / (speedHistory.size - 1)) * width
                    val y = height - (point.uploadKbps / maxSpeed) * (height - 12f) - 6f
                    Offset(x, y.coerceIn(4f, height - 4f))
                }

                if (downloadPoints.size > 1) {
                    val path = Path()
                    val fillPath = Path()
                    path.moveTo(downloadPoints[0].x, downloadPoints[0].y)
                    fillPath.moveTo(downloadPoints[0].x, height)
                    fillPath.lineTo(downloadPoints[0].x, downloadPoints[0].y)

                    for (i in 1 until downloadPoints.size) {
                        val prev = downloadPoints[i - 1]
                        val curr = downloadPoints[i]
                        val cX = (prev.x + curr.x) / 2
                        path.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                        fillPath.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                    }
                    fillPath.lineTo(downloadPoints.last().x, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = path,
                        color = NeonCyan,
                        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                    )
                }

                if (uploadPoints.size > 1) {
                    val upPath = Path()
                    upPath.moveTo(uploadPoints[0].x, uploadPoints[0].y)
                    for (i in 1 until uploadPoints.size) {
                        val prev = uploadPoints[i - 1]
                        val curr = uploadPoints[i]
                        val cX = (prev.x + curr.x) / 2
                        upPath.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                    }
                    drawPath(
                        path = upPath,
                        color = NeonAmber.copy(alpha = 0.85f),
                        style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonCyan))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("DOWN", fontSize = 9.sp, color = NeonCyan, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonAmber))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("UP", fontSize = 9.sp, color = NeonAmber, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    return try {
        val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    } catch (_: Exception) {
        "🌐"
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}

fun formatSpeed(bytesPerSec: Long): String {
    val kbps = (bytesPerSec * 8) / 1000.0
    if (kbps < 1000) return "%.0f Kbps".format(kbps)
    val mbps = kbps / 1000.0
    return "%.1f Mbps".format(mbps)
}

fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        "%02d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }
}
