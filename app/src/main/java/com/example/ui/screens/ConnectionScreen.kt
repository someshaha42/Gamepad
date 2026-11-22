package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.BtConnectionPhase
import com.example.engine.ConnectionSnapshot
import com.example.engine.UsbCompatibilityStatus
import com.example.model.ConnectionEnvironment
import com.example.model.ControllerSettings
import com.example.ui.ControllerViewModel
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConnectionScreen(
    viewModel: ControllerViewModel,
    settings: ControllerSettings,
    snapshot: ConnectionSnapshot,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshConnections()
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshConnections()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Automatic ConnectionManager Override & Status
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(
                1.5.dp,
                if (snapshot.isReadyToTransmit) StatusConnected else NeonCyan.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = snapshot.statusHeadline,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (snapshot.isReadyToTransmit) StatusConnected else NeonCyan
                        )
                        Text(
                            text = snapshot.statusSubtext,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.refreshConnections() },
                        border = BorderStroke(1.dp, NeonCyan),
                        modifier = Modifier.testTag("refresh_connections_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan)
                    }
                }

                Text(
                    text = "Engine Selection Mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ConnectionEnvironment.entries.forEach { env ->
                        val selected = settings.preferredConnection == env
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) NeonCyan else ObsidianElevated)
                                .clickable { viewModel.setPreferredConnection(env) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("conn_env_${env.name.lowercase()}")
                        ) {
                            Text(
                                text = env.displayName.split(" ").first(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) ObsidianBlack else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 2. Local Controller Mode Setup Wizard
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = NeonEmerald)
                    Spacer(Modifier.width(8.dp))
                    Text("LOCAL CONTROLLER (SAME PHONE)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Text(
                    text = "Controls games running on this same phone via floating overlay + Android Accessibility touch gesture dispatch. No Bluetooth pairing or USB cable required.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                PermissionCheckRow(
                    label = "1. Floating Overlay Permission (SYSTEM_ALERT_WINDOW)",
                    granted = snapshot.localCaps.hasOverlayPermission,
                    actionLabel = "Grant Overlay",
                    onAction = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                )
                PermissionCheckRow(
                    label = "2. Accessibility Touch Bridge (dispatchGesture)",
                    granted = snapshot.localCaps.hasAccessibilityEnabled,
                    actionLabel = "Open Accessibility",
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                Text(
                    text = "Android Note: Standard Android apps cannot inject raw native hardware KeyEvents/Gamepad MotionEvents into third-party games. Local mode dispatches mapped touch taps and joystick swipes (up to ${snapshot.localCaps.maxSimultaneousStrokes} strokes) onto game HUD buttons.",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusWarning
                )

                Button(
                    onClick = {
                        if (snapshot.isOverlayServiceRunning) {
                            viewModel.stopFloatingOverlay()
                        } else if (!snapshot.localCaps.hasOverlayPermission) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        } else {
                            viewModel.startFloatingOverlay(localMode = true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (snapshot.isOverlayServiceRunning) NeonCrimson else NeonEmerald,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("conn_start_local_btn")
                ) {
                    Icon(Icons.Default.AccessibilityNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (snapshot.isOverlayServiceRunning) "STOP LOCAL CONTROLLER" else "START LOCAL CONTROLLER",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // 3. Bluetooth HID Engine Panel
        val bt = snapshot.btState
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = NeonCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("BLUETOOTH HID PERIPHERAL", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Text(
                        text = if (bt.isAppRegistered) "SDP ACTIVE" else bt.phase.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bt.phase == BtConnectionPhase.CONNECTED) StatusConnected else NeonCyan
                    )
                }
                Text(text = bt.statusMessage, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                when (bt.phase) {
                    BtConnectionPhase.PERMISSION_REQUIRED -> {
                        Button(
                            onClick = {
                                btPermissionLauncher.launch(viewModel.connectionManager.bluetoothEngine.requiredPermissions())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                            modifier = Modifier.fillMaxWidth().testTag("grant_bt_perms_btn")
                        ) {
                            Text("Grant Bluetooth HID Permissions")
                        }
                    }
                    BtConnectionPhase.BLUETOOTH_DISABLED -> {
                        Button(
                            onClick = {
                                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                            modifier = Modifier.fillMaxWidth().testTag("enable_bt_btn")
                        ) {
                            Text("Turn On Bluetooth")
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (bt.isScanning) viewModel.stopBtDiscovery() else viewModel.startBtDiscovery()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet, contentColor = TextPrimary),
                                modifier = Modifier.weight(1f).testTag("scan_bt_btn")
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (bt.isScanning) "Stop Scan" else "Discover Devices")
                            }
                            if (bt.phase == BtConnectionPhase.CONNECTED) {
                                OutlinedButton(
                                    onClick = { viewModel.disconnectBtDevice() },
                                    border = BorderStroke(1.dp, NeonCrimson),
                                    modifier = Modifier.testTag("disconnect_bt_btn")
                                ) {
                                    Text("Disconnect", color = NeonCrimson)
                                }
                            }
                        }

                        if (bt.pairedDevices.isNotEmpty()) {
                            Text("Paired Devices (Tap to Connect)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            bt.pairedDevices.forEach { dev ->
                                val isCurrentHost = bt.connectedHostAddress == dev.address
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrentHost) StatusConnected.copy(alpha = 0.16f) else ObsidianElevated)
                                        .clickable {
                                            if (isCurrentHost) viewModel.disconnectBtDevice()
                                            else viewModel.connectBtDevice(dev.device)
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(dev.name, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                        Text(dev.address, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                    Text(
                                        text = if (isCurrentHost) "CONNECTED" else "CONNECT",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isCurrentHost) StatusConnected else NeonCyan
                                    )
                                }
                            }
                        }

                        if (bt.discoveredDevices.isNotEmpty()) {
                            Text("Nearby Discovered Devices", style = MaterialTheme.typography.labelLarge, color = NeonViolet)
                            bt.discoveredDevices.forEach { dev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ObsidianElevated)
                                        .clickable { viewModel.connectBtDevice(dev.device) }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(dev.name, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                        Text(dev.address, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                    Text("PAIR & CONNECT", style = MaterialTheme.typography.labelSmall, color = NeonViolet)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. USB HID Compatibility & Host Inspector
        val usb = snapshot.usbState
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(
                1.dp,
                if (usb.compatibility == UsbCompatibilityStatus.GADGET_HID_READY) StatusConnected else NeonAmber.copy(alpha = 0.55f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Usb, contentDescription = null, tint = NeonAmber)
                    Spacer(Modifier.width(8.dp))
                    Text("USB HID ENGINE & COMPATIBILITY", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Text(text = usb.statusTitle, style = MaterialTheme.typography.labelLarge, color = NeonAmber)
                Text(text = usb.statusDetail, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    text = "USB Host Feature: ${if (usb.hasUsbHostFeature) "Supported" else "Unavailable"} • Cable Connected: ${if (usb.isUsbCableConnected) "Yes" else "No"} • Gadget Node: ${usb.gadgetNodePath ?: "None writable"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                if (usb.attachedDevices.isNotEmpty()) {
                    Text("Detected USB Host Peripherals:", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    usb.attachedDevices.forEach { dev ->
                        Text(
                            text = "• ${dev.productName} (VID:${dev.vendorId} PID:${dev.productId}, ${dev.interfaceCount} ifaces${if (dev.hasHidInterface) ", HID" else ""})",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCheckRow(
    label: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) StatusConnected else StatusWarning,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
        if (!granted) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = NeonCyan, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Text("READY", style = MaterialTheme.typography.labelSmall, color = StatusConnected)
        }
    }
}
