package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ConnectionEnvironment
import com.example.model.ControllerMode
import com.example.model.ControllerSettings
import com.example.model.KeyboardStyle
import com.example.model.RgbEffect
import com.example.ui.ControllerViewModel
import com.example.ui.components.rememberRgbVisualState
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: ControllerViewModel,
    settings: ControllerSettings,
    onNavigateToLayouts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rgbState = rememberRgbVisualState(settings.rgbEffect, Color(settings.rgbStaticColorArgb))
    var confirmResetGeneral by remember { mutableStateOf(false) }
    var confirmRestore35 by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Controller Mode & Keyboard Layout
        SettingsSectionCard(title = "CONTROLLER MODE & KEYBOARD LAYOUT") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ControllerMode.entries.forEach { mode ->
                    val selected = settings.selectedMode == mode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) NeonCyan else ObsidianElevated)
                            .clickable { viewModel.setControllerMode(mode) }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = mode.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) ObsidianBlack else TextPrimary
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyboardStyle.entries.forEach { style ->
                    val selected = settings.selectedKeyboardStyle == style
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) NeonViolet else ObsidianElevated)
                            .clickable { viewModel.setKeyboardStyle(style) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = onNavigateToLayouts,
                border = BorderStroke(1.dp, NeonCyan),
                modifier = Modifier.fillMaxWidth().testTag("settings_customize_keys_btn")
            ) {
                Text("Customize Keys & Save Game Layouts →", color = NeonCyan)
            }
        }

        // 2. DEDICATED TRANSPARENCY CONTROL SECTION (Major Feature)
        SettingsSectionCard(title = "TRANSPARENCY CONTROL & LIVE PREVIEW") {
            // Live Transparency Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianBlack)
                    .border(1.dp, ObsidianElevated, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "BACKGROUND GAME LAYER PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center)
                )
                if (settings.controllerVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(settings.controllerOpacity),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PreviewBadge("KB ${(settings.keyboardOpacity * 100).toInt()}%", settings.keyboardOpacity, NeonCyan, Modifier.weight(1f))
                        PreviewBadge("MOUSE ${(settings.mouseOpacity * 100).toInt()}%", settings.mouseOpacity, NeonViolet, Modifier.weight(1f))
                        PreviewBadge("PAD ${(settings.gamepadOpacity * 100).toInt()}%", settings.gamepadOpacity, NeonEmerald, Modifier.weight(1f))
                    }
                } else {
                    Text(
                        text = "CONTROLLER VISUAL HIDDEN (Service Active)",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonAmber,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            LabeledSlider(
                label = "Master Controller Opacity",
                value = settings.controllerOpacity,
                range = 0.08f..1f,
                valueLabel = "${(settings.controllerOpacity * 100).toInt()}%",
                testTag = "slider_controller_opacity",
                onChange = { viewModel.updateOpacities(controller = it) }
            )
            LabeledSlider(
                label = "Keyboard Opacity",
                value = settings.keyboardOpacity,
                range = 0.08f..1f,
                valueLabel = "${(settings.keyboardOpacity * 100).toInt()}%",
                testTag = "slider_keyboard_opacity",
                onChange = { viewModel.updateOpacities(keyboard = it) }
            )
            LabeledSlider(
                label = "Mouse Opacity",
                value = settings.mouseOpacity,
                range = 0.08f..1f,
                valueLabel = "${(settings.mouseOpacity * 100).toInt()}%",
                testTag = "slider_mouse_opacity",
                onChange = { viewModel.updateOpacities(mouse = it) }
            )
            LabeledSlider(
                label = "Gamepad Opacity",
                value = settings.gamepadOpacity,
                range = 0.08f..1f,
                valueLabel = "${(settings.gamepadOpacity * 100).toInt()}%",
                testTag = "slider_gamepad_opacity",
                onChange = { viewModel.updateOpacities(gamepad = it) }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.toggleControllerVisibility(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (settings.controllerVisible) NeonEmerald else ObsidianElevated,
                        contentColor = if (settings.controllerVisible) ObsidianBlack else TextPrimary
                    ),
                    modifier = Modifier.weight(1f).testTag("show_controller_btn")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SHOW CONTROLLER", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = { viewModel.toggleControllerVisibility(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!settings.controllerVisible) NeonAmber else ObsidianElevated,
                        contentColor = if (!settings.controllerVisible) ObsidianBlack else TextPrimary
                    ),
                    modifier = Modifier.weight(1f).testTag("hide_controller_btn")
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("HIDE CONTROLLER", style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                text = "Note: Hiding the visual controller keeps the background service alive and restores via the floating pill or notification. On Android, visible overlay zones capture their own touches; invisible overlay regions do not reliably pass touches through to third-party games.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        // 3. Mouse & Joystick Sensitivity
        SettingsSectionCard(title = "MOUSE & JOYSTICK SENSITIVITY") {
            LabeledSlider(
                label = "Mouse Sensitivity",
                value = settings.mouseSensitivity,
                range = 0.3f..3.5f,
                valueLabel = "${"%.2f".format(settings.mouseSensitivity)}x",
                onChange = { viewModel.updateSensitivities(mouseSensitivity = it) }
            )
            LabeledSlider(
                label = "Mouse Acceleration / Speed",
                value = settings.mouseSpeed,
                range = 0.3f..3.5f,
                valueLabel = "${"%.2f".format(settings.mouseSpeed)}x",
                onChange = { viewModel.updateSensitivities(mouseSpeed = it) }
            )
            LabeledSlider(
                label = "Trackpad Size Scale",
                value = settings.trackpadSizeScale,
                range = 0.65f..1.35f,
                valueLabel = "${"%.2f".format(settings.trackpadSizeScale)}x",
                onChange = { viewModel.updateSensitivities(trackpadScale = it) }
            )
            LabeledSlider(
                label = "Joystick Sensitivity",
                value = settings.joystickSensitivity,
                range = 0.4f..2.2f,
                valueLabel = "${"%.2f".format(settings.joystickSensitivity)}x",
                onChange = { viewModel.updateSensitivities(joystickSensitivity = it) }
            )
            SettingsSwitchRow(
                label = "Swap Left / Right Mouse Click",
                checked = settings.mouseButtonConfig.swapLeftRight,
                onCheckedChange = {
                    viewModel.updateMouseConfig(settings.mouseButtonConfig.copy(swapLeftRight = it))
                }
            )
        }

        // 4. RGB Lighting Effects
        SettingsSectionCard(title = "RGB LIGHTING EFFECTS") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RgbEffect.entries.forEach { effect ->
                    val selected = settings.rgbEffect == effect
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) rgbState.primaryAccent else ObsidianElevated)
                            .clickable { viewModel.updateRgb(effect = effect) }
                            .padding(vertical = 8.dp)
                            .testTag("rgb_effect_${effect.name.lowercase()}")
                    ) {
                        Text(
                            text = effect.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) ObsidianBlack else TextPrimary
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Accent Color:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                listOf(
                    0xFF00F0FFL,
                    0xFF9D4EDDL,
                    0xFFFF2A6DL,
                    0xFF00FF9DL,
                    0xFFFFB800L
                ).forEach { hex ->
                    val selected = settings.rgbStaticColorArgb == hex
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) Color.White else ObsidianBlack,
                                shape = CircleShape
                            )
                            .clickable { viewModel.updateRgb(staticColorArgb = hex) }
                    )
                }
            }
        }

        // 5. Overlay & Connection Settings
        SettingsSectionCard(title = "OVERLAY & CONNECTION PREFERENCES") {
            LabeledSlider(
                label = "Overlay Width",
                value = settings.overlayWidthDp.toFloat(),
                range = 240f..680f,
                valueLabel = "${settings.overlayWidthDp} dp",
                onChange = { viewModel.updateOverlaySize(widthDp = it.toInt()) }
            )
            LabeledSlider(
                label = "Overlay Height",
                value = settings.overlayHeightDp.toFloat(),
                range = 190f..520f,
                valueLabel = "${settings.overlayHeightDp} dp",
                onChange = { viewModel.updateOverlaySize(heightDp = it.toInt()) }
            )
            SettingsSwitchRow(
                label = "Compact ~Half-Width Gaming Keyboard (Tablets/Landscape)",
                checked = settings.compactHalfWidthGamingKb,
                onCheckedChange = { viewModel.setCompactHalfWidth(it) }
            )
            SettingsSwitchRow(
                label = "Tactile Haptic Feedback",
                checked = settings.hapticFeedbackEnabled,
                onCheckedChange = { viewModel.setHapticFeedback(it) }
            )
            SettingsSwitchRow(
                label = "Auto Reconnect Last Bluetooth HID Host",
                checked = settings.autoReconnect,
                onCheckedChange = { viewModel.setAutoReconnect(it) }
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ConnectionEnvironment.entries.forEach { env ->
                    val selected = settings.preferredConnection == env
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) NeonCyan else ObsidianElevated)
                            .clickable { viewModel.setPreferredConnection(env) }
                            .padding(vertical = 7.dp)
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

        // 6. SEPARATE RESET CONTROLS
        SettingsSectionCard(title = "RESET CONTROLS (ISOLATED)") {
            Text(
                text = "General controller settings and the 35-key keyboard layout reset independently so resetting one never erases the other.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            OutlinedButton(
                onClick = { confirmResetGeneral = true },
                border = BorderStroke(1.dp, NeonCrimson),
                modifier = Modifier.fillMaxWidth().testTag("reset_general_settings_btn")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = NeonCrimson)
                Spacer(Modifier.width(6.dp))
                Text("Reset Controller Settings", color = NeonCrimson)
            }
            OutlinedButton(
                onClick = { confirmRestore35 = true },
                border = BorderStroke(1.dp, NeonAmber),
                modifier = Modifier.fillMaxWidth().testTag("settings_restore_35_btn")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = NeonAmber)
                Spacer(Modifier.width(6.dp))
                Text("Restore Default 35-Key Layout", color = NeonAmber)
            }
        }
    }

    if (confirmResetGeneral) {
        AlertDialog(
            onDismissRequest = { confirmResetGeneral = false },
            containerColor = ObsidianCard,
            title = { Text("Reset General Controller Settings?", color = NeonCrimson) },
            text = {
                Text(
                    "Resets sensitivities, transparency sliders, RGB effect, and overlay dimensions to defaults. Your saved keyboard layouts and custom keys remain untouched.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetGeneralControllerSettings()
                        confirmResetGeneral = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson, contentColor = TextPrimary)
                ) { Text("Reset Settings") }
            },
            dismissButton = { TextButton(onClick = { confirmResetGeneral = false }) { Text("Cancel") } }
        )
    }

    if (confirmRestore35) {
        AlertDialog(
            onDismissRequest = { confirmRestore35 = false },
            containerColor = ObsidianCard,
            title = { Text("Restore Default 35-Key Layout?", color = NeonAmber) },
            text = {
                Text(
                    "Restores the Sounce One-Handed RGB Gaming Keyboard (35 Keys) default layout without changing your general controller settings.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreDefault35KeyLayout()
                        confirmRestore35 = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = ObsidianBlack)
                ) { Text("Restore 35 Keys") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore35 = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianElevated),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
            content()
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    testTag: String? = null,
    onChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PreviewBadge(
    label: String,
    alphaValue: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .alpha(alphaValue.coerceIn(0.08f, 1f))
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.25f))
            .border(1.5.dp, color, RoundedCornerShape(8.dp))
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
    }
}
