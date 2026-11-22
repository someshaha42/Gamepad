package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.ConnectionSnapshot
import com.example.model.ControllerMode
import com.example.model.ControllerSettings
import com.example.model.KeyboardLayoutProfile
import com.example.model.KeyboardStyle
import com.example.ui.ControllerViewModel
import com.example.ui.components.AddKeyModalDialog
import com.example.ui.components.GamepadMappingDialog
import com.example.ui.components.rememberRgbVisualState
import com.example.ui.controllers.GamepadPanel
import com.example.ui.controllers.GamingKeyboard35Panel
import com.example.ui.controllers.KeyboardMouseSplitPanel
import com.example.ui.controllers.MouseTrackpadPanel
import com.example.ui.controllers.NormalKeyboardPanel
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ControllerScreen(
    viewModel: ControllerViewModel,
    settings: ControllerSettings,
    snapshot: ConnectionSnapshot,
    activeLayout: KeyboardLayoutProfile,
    onNavigateToLayouts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rgbState = rememberRgbVisualState(
        effect = settings.rgbEffect,
        staticColor = Color(settings.rgbStaticColorArgb)
    )
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var showGamepadMappingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Four Controller Modes Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControllerMode.entries.forEach { mode ->
                val selected = settings.selectedMode == mode
                val icon = when (mode) {
                    ControllerMode.GAMEPAD -> Icons.Default.Gamepad
                    ControllerMode.KEYBOARD_MOUSE -> Icons.Default.Layers
                    ControllerMode.KEYBOARD_ONLY -> Icons.Default.Keyboard
                    ControllerMode.MOUSE_ONLY -> Icons.Default.Mouse
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) rgbState.primaryAccent.copy(alpha = 0.22f) else ObsidianElevated)
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) rgbState.primaryAccent else ObsidianCard,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setControllerMode(mode) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("mode_${mode.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mode.displayName,
                        tint = if (selected) rgbState.primaryAccent else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) TextPrimary else TextSecondary
                    )
                }
            }
        }

        // 2. Keyboard Style Tabs (inside Keyboard Only or Keyboard+Mouse; Gaming Keyboard is default)
        if (settings.selectedMode == ControllerMode.KEYBOARD_ONLY ||
            settings.selectedMode == ControllerMode.KEYBOARD_MOUSE
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(KeyboardStyle.GAMING_35, KeyboardStyle.NORMAL_QWERTY).forEach { style ->
                    val selected = settings.selectedKeyboardStyle == style
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) rgbState.primaryAccent else Color.Transparent)
                            .clickable { viewModel.setKeyboardStyle(style) }
                            .padding(vertical = 9.dp)
                            .testTag("kb_tab_${style.name.lowercase()}")
                    ) {
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) ObsidianBlack else TextSecondary
                        )
                    }
                }
            }
        }

        // 3. Quick Local Floating Controller & Show/Hide Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (snapshot.isOverlayServiceRunning) {
                        viewModel.stopFloatingOverlay()
                    } else if (!Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } else {
                        viewModel.startFloatingOverlay(localMode = true)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (snapshot.isOverlayServiceRunning) NeonCrimson else NeonEmerald,
                    contentColor = ObsidianBlack
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("start_local_controller_btn")
            ) {
                Icon(
                    imageVector = if (snapshot.isOverlayServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (snapshot.isOverlayServiceRunning) "STOP LOCAL CONTROLLER" else "START LOCAL CONTROLLER",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = { viewModel.toggleControllerVisibility(!settings.controllerVisible) },
                border = BorderStroke(1.dp, rgbState.primaryAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("quick_show_hide_btn")
            ) {
                Icon(
                    imageVector = if (settings.controllerVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Visibility",
                    tint = rgbState.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (settings.controllerVisible) "HIDE" else "SHOW",
                    style = MaterialTheme.typography.labelLarge,
                    color = rgbState.primaryAccent
                )
            }
        }

        // 4. Active Interactive Controller Workspace
        if (!settings.controllerVisible) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, rgbState.primaryAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Controller Visual Hidden (Service Active)",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                    Text(
                        text = "Tap SHOW above or tap the floating SHOW PAD pill / notification action to restore the controller immediately.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            when (settings.selectedMode) {
                ControllerMode.GAMEPAD -> {
                    GamepadPanel(
                        opacity = settings.gamepadOpacity * settings.controllerOpacity,
                        joystickSensitivity = settings.joystickSensitivity,
                        mappings = settings.gamepadMappings,
                        rgbState = rgbState,
                        hapticEnabled = settings.hapticFeedbackEnabled,
                        onOpenMappingDialog = { showGamepadMappingDialog = true },
                        onButtonEvent = { btn, mapping, down ->
                            viewModel.connectionManager.dispatchGamepadButton(
                                btn,
                                mapping.targetHidBit,
                                down,
                                mapping.localTapXPercent,
                                mapping.localTapYPercent
                            )
                        },
                        onDpadEvent = { u, d, l, r -> viewModel.connectionManager.dispatchGamepadDpad(u, d, l, r) },
                        onLeftStick = { x, y -> viewModel.connectionManager.dispatchLeftStick(x, y) },
                        onRightStick = { x, y -> viewModel.connectionManager.dispatchRightStick(x, y) },
                        onTriggers = { l2, r2 -> viewModel.connectionManager.dispatchTriggers(l2, r2) }
                    )
                }
                ControllerMode.KEYBOARD_MOUSE -> {
                    KeyboardMouseSplitPanel(
                        settings = settings,
                        layout = activeLayout,
                        rgbState = rgbState,
                        onAddKeyClick = { showAddKeyDialog = true },
                        onSplitRatioChange = { r -> viewModel.updateSensitivities(splitRatio = r) },
                        onKeyEvent = { key, down ->
                            viewModel.connectionManager.dispatchKey(
                                key.hidUsageCode,
                                key.label,
                                key.isModifier,
                                key.modifierMask,
                                down,
                                key.localTapXPercent,
                                key.localTapYPercent
                            )
                        },
                        onMouseMove = { dx, dy -> viewModel.connectionManager.dispatchMouseMove(dx, dy) },
                        onMouseScroll = { w -> viewModel.connectionManager.dispatchMouseMove(0, 0, w) },
                        onMouseButton = { bit, down -> viewModel.connectionManager.dispatchMouseButton(bit, down) }
                    )
                }
                ControllerMode.KEYBOARD_ONLY -> {
                    if (settings.selectedKeyboardStyle == KeyboardStyle.GAMING_35) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            GamingKeyboard35Panel(
                                layout = activeLayout,
                                opacity = settings.keyboardOpacity * settings.controllerOpacity,
                                rgbState = rgbState,
                                compactHalfWidth = settings.compactHalfWidthGamingKb,
                                hapticEnabled = settings.hapticFeedbackEnabled,
                                onAddKeyClick = { showAddKeyDialog = true },
                                onKeyEvent = { key, down ->
                                    viewModel.connectionManager.dispatchKey(
                                        key.hidUsageCode,
                                        key.label,
                                        key.isModifier,
                                        key.modifierMask,
                                        down,
                                        key.localTapXPercent,
                                        key.localTapYPercent
                                    )
                                }
                            )
                        }
                    } else {
                        NormalKeyboardPanel(
                            opacity = settings.keyboardOpacity * settings.controllerOpacity,
                            rgbState = rgbState,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                            onKeyEvent = { key, down ->
                                viewModel.connectionManager.dispatchKey(
                                    key.hidUsageCode,
                                    key.label,
                                    key.isModifier,
                                    key.modifierMask,
                                    down,
                                    key.localTapXPercent,
                                    key.localTapYPercent
                                )
                            }
                        )
                    }
                }
                ControllerMode.MOUSE_ONLY -> {
                    MouseTrackpadPanel(
                        opacity = settings.mouseOpacity * settings.controllerOpacity,
                        sensitivity = settings.mouseSensitivity,
                        speed = settings.mouseSpeed,
                        trackpadScale = settings.trackpadSizeScale,
                        buttonConfig = settings.mouseButtonConfig,
                        rgbState = rgbState,
                        hapticEnabled = settings.hapticFeedbackEnabled,
                        onMove = { dx, dy -> viewModel.connectionManager.dispatchMouseMove(dx, dy) },
                        onScroll = { w -> viewModel.connectionManager.dispatchMouseMove(0, 0, w) },
                        onButton = { bit, down -> viewModel.connectionManager.dispatchMouseButton(bit, down) }
                    )
                }
            }
        }

        // 5. Quick Live Opacity + Layout Customize Strip
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Master Controller Opacity: ${(settings.controllerOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Customize Keys →",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonViolet,
                        modifier = Modifier
                            .clickable { onNavigateToLayouts() }
                            .testTag("go_to_layouts_link")
                    )
                }
                Slider(
                    value = settings.controllerOpacity,
                    onValueChange = { viewModel.updateOpacities(controller = it) },
                    valueRange = 0.1f..1f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                    modifier = Modifier.testTag("quick_master_opacity_slider")
                )
            }
        }

        // 6. Live HID / Gesture Packet Telemetry Monitor
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
            border = BorderStroke(1.dp, ObsidianElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "LIVE INPUT TELEMETRY (${snapshot.activeEngine.label})",
                    style = MaterialTheme.typography.labelSmall,
                    color = rgbState.primaryAccent
                )
                if (snapshot.recentPackets.isEmpty()) {
                    Text(
                        text = "Press any key, stick, or trackpad to inspect real HID / local gesture packets.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                } else {
                    snapshot.recentPackets.take(3).forEach { pkt ->
                        Text(
                            text = "• $pkt",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showAddKeyDialog) {
        AddKeyModalDialog(
            onDismiss = { showAddKeyDialog = false },
            onAddKey = { viewModel.addKeyToActiveLayout(it) }
        )
    }

    if (showGamepadMappingDialog) {
        GamepadMappingDialog(
            mappings = settings.gamepadMappings,
            onSaveMapping = { viewModel.updateGamepadMapping(it) },
            onDismiss = { showGamepadMappingDialog = false }
        )
    }
}
