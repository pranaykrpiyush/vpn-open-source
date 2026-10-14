package com.example.ui

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.InternetStatus
import com.example.model.TunnelMode
import com.example.model.VpnState
import com.example.vpn.OpenShieldVpnService
import com.example.ui.components.AddServerDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.ServersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: VpnViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
    val currentServer by viewModel.currentServer.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val speedHistory by viewModel.speedHistory.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val connectionHistory by viewModel.connectionHistory.collectAsStateWithLifecycle()
    val filteredServers by viewModel.filteredServers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountryFilter.collectAsStateWithLifecycle()
    val isFetchingLive by viewModel.isFetchingLive.collectAsStateWithLifecycle()
    val selectedProtocol by viewModel.selectedProtocol.collectAsStateWithLifecycle()
    val selectedDns by viewModel.selectedDns.collectAsStateWithLifecycle()
    val killSwitchEnabled by viewModel.killSwitchEnabled.collectAsStateWithLifecycle()
    val mtuSize by viewModel.mtuSize.collectAsStateWithLifecycle()
    val selectedTunnelMode by viewModel.selectedTunnelMode.collectAsStateWithLifecycle()
    val internetStatus by viewModel.internetStatus.collectAsStateWithLifecycle()
    val lastPingLatencyMs by viewModel.lastPingLatencyMs.collectAsStateWithLifecycle()
    val diagnosticRunning by viewModel.diagnosticRunning.collectAsStateWithLifecycle()
    val diagnosticResults by viewModel.diagnosticResults.collectAsStateWithLifecycle()

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startTunnelService(context)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("VPN permission was denied by system.")
            }
        }
    }

    val navItems = listOf(
        NavigationItem("Shield", Icons.Default.Shield, "nav_shield"),
        NavigationItem("Servers", Icons.Default.Public, "nav_servers"),
        NavigationItem("Diagnostics", Icons.Default.NetworkCheck, "nav_diagnostics"),
        NavigationItem("Logs", Icons.Default.Terminal, "nav_logs"),
        NavigationItem("Settings", Icons.Default.Settings, "nav_settings")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OPENSHIELD",
                        style = MaterialTheme.typography.titleMedium.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = NeonCyan
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = when (vpnState) {
                                VpnState.CONNECTED -> "PROTECTED"
                                VpnState.CONNECTING, VpnState.AUTHENTICATING -> "SYNCING"
                                else -> "OFFLINE"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = when (vpnState) {
                                VpnState.CONNECTED -> NeonEmerald
                                VpnState.CONNECTING, VpnState.AUTHENTICATING -> NeonCyan
                                else -> TextMuted
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberBackground,
                    titleContentColor = NeonCyan
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedNavIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedNavIndex = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = NeonCyan.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CyberBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Global Persistent Warning Banner when Internet is unreachable through VPN
            AnimatedVisibility(
                visible = vpnState == VpnState.CONNECTED && internetStatus == InternetStatus.OFFLINE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF330B10))
                        .border(1.dp, NeonRed.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = NeonRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⚠️ Internet unreachable: 8.8.8.8 ping failed",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFB4AB),
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.quickFixRestoreInternet(context)
                            scope.launch {
                                snackbarHostState.showSnackbar("Safe Mode Activated: Secure DNS Shield restored internet!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonEmerald,
                            contentColor = Color(0xFF002213)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("global_fix_internet_button")
                    ) {
                        Text(
                            text = "Fix Internet",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedNavIndex) {
                    0 -> DashboardScreen(
                        vpnState = vpnState,
                        currentServer = currentServer,
                        telemetry = telemetry,
                        speedHistory = speedHistory,
                        killSwitchEnabled = killSwitchEnabled,
                        dnsName = selectedDns.name,
                        tunnelMode = selectedTunnelMode,
                        internetStatus = internetStatus,
                        lastPingLatencyMs = lastPingLatencyMs,
                        onConnectToggle = {
                            viewModel.connectOrDisconnect(context) {
                                val intent = VpnService.prepare(context)
                                if (intent != null) {
                                    vpnPrepareLauncher.launch(intent)
                                }
                            }
                        },
                        onNavigateToServers = { selectedNavIndex = 1 },
                        onQuickFixInternet = {
                            viewModel.quickFixRestoreInternet(context)
                            scope.launch {
                                snackbarHostState.showSnackbar("Safe Mode Activated: Secure DNS Shield restored internet access!")
                            }
                        },
                        onToggleTunnelMode = {
                            val nextMode = if (selectedTunnelMode == TunnelMode.SECURE_DNS_SHIELD) {
                                TunnelMode.FULL_TUNNEL
                            } else {
                                TunnelMode.SECURE_DNS_SHIELD
                            }
                            viewModel.setTunnelMode(nextMode)
                            scope.launch {
                                snackbarHostState.showSnackbar("Switched to ${nextMode.displayName}")
                            }
                        },
                        onRetryPing = {
                            viewModel.retryPingConnectivity()
                            scope.launch {
                                snackbarHostState.showSnackbar("Checking connection to 8.8.8.8...")
                            }
                        },
                        onDisconnect = {
                            OpenShieldVpnService.stop(context)
                            scope.launch {
                                snackbarHostState.showSnackbar("VPN disconnected.")
                            }
                        }
                    )
                1 -> ServersScreen(
                    servers = filteredServers,
                    currentServer = currentServer,
                    searchQuery = searchQuery,
                    selectedCountry = selectedCountry,
                    isFetchingLive = isFetchingLive,
                    onSearchChanged = { viewModel.setSearchQuery(it) },
                    onCountryChanged = { viewModel.setCountryFilter(it) },
                    onServerSelected = { server ->
                        viewModel.selectServer(server)
                        selectedNavIndex = 0
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDeleteServer = { viewModel.deleteServer(it) },
                    onFetchLiveRelays = {
                        viewModel.fetchLiveServers { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    },
                    onOpenAddCustomDialog = { showAddDialog = true }
                )
                2 -> DiagnosticsScreen(
                    vpnState = vpnState,
                    currentServer = currentServer,
                    diagnosticRunning = diagnosticRunning,
                    diagnosticResults = diagnosticResults,
                    onRunDiagnostics = { viewModel.runDiagnostics() }
                )
                3 -> LogsScreen(
                    logs = logs,
                    history = connectionHistory,
                    onClearLogs = { viewModel.clearLogs() },
                    onClearHistory = { viewModel.clearHistory() }
                )
                4 -> SettingsScreen(
                    selectedTunnelMode = selectedTunnelMode,
                    selectedProtocol = selectedProtocol,
                    selectedDns = selectedDns,
                    dnsOptions = viewModel.dnsOptions,
                    killSwitchEnabled = killSwitchEnabled,
                    mtuSize = mtuSize,
                    onTunnelModeChanged = { viewModel.setTunnelMode(it) },
                    onProtocolChanged = { viewModel.setProtocol(it) },
                    onDnsChanged = { viewModel.setDns(it) },
                    onKillSwitchChanged = { viewModel.setKillSwitch(it) },
                    onMtuChanged = { viewModel.setMtu(it) }
                )
            }
        }
    }
}

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, ip, port, countryCode, countryName, protocol, config ->
                viewModel.addCustomServer(name, ip, port, countryCode, countryName, protocol, config)
                scope.launch {
                    snackbarHostState.showSnackbar("Profile '$name' imported successfully!")
                }
            }
        )
    }
}

private data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
)
