package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.DefaultSounceLayouts
import com.example.model.GamepadButton
import com.example.model.GamepadButtonMapping
import com.example.model.KeySpec
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddKeyModalDialog(
    onDismiss: () -> Unit,
    onAddKey: (KeySpec) -> Unit
) {
    var customLabel by remember { mutableStateOf("") }
    var customUsageHex by remember { mutableStateOf("0x28") }
    var targetRow by remember { mutableIntStateOf(2) }
    var widthWeight by remember { mutableFloatStateOf(1f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Text(
                text = "+ ADD KEY TO GAMING LAYOUT",
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tap any preset key below to add it to row ${targetRow + 1}, or define a custom key label & HID usage code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Text(
                    text = "Target Row: ${targetRow + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..5).forEach { r ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (targetRow == r) NeonCyan else ObsidianElevated)
                                .clickable { targetRow = r }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "R${r + 1}",
                                color = if (targetRow == r) ObsidianBlack else TextPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text(
                    text = "Preset Keys Catalog",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DefaultSounceLayouts.extraKeyCatalog.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianElevated)
                                .border(1.dp, NeonViolet.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable {
                                    onAddKey(preset.copy(row = targetRow, widthWeight = widthWeight))
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("catalog_${preset.id}")
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Or Create Custom Key",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Custom Key Label (e.g. JUMP, M4)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customUsageHex,
                    onValueChange = { customUsageHex = it },
                    label = { Text("USB HID Usage Hex (0x04..0x65)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Key Width Scale: ${"%.1f".format(widthWeight)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Slider(
                    value = widthWeight,
                    onValueChange = { widthWeight = it },
                    valueRange = 0.7f..3.0f
                )
            }
        },
        confirmButton = {
            Button(
                enabled = customLabel.isNotBlank(),
                onClick = {
                    val cleanHex = customUsageHex.trim().removePrefix("0x").removePrefix("0X")
                    val usage = cleanHex.toIntOrNull(16)?.coerceIn(0, 0x65) ?: 0x28
                    onAddKey(
                        KeySpec(
                            id = "custom_${System.currentTimeMillis()}",
                            label = customLabel.trim(),
                            hidUsageCode = usage,
                            row = targetRow,
                            column = 99,
                            widthWeight = widthWeight,
                            isCustomAdded = true
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack)
            ) {
                Text("Add Custom")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun GamepadMappingDialog(
    mappings: Map<GamepadButton, GamepadButtonMapping>,
    onSaveMapping: (GamepadButtonMapping) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBtn by remember { mutableStateOf(GamepadButton.A) }
    val current = mappings[selectedBtn] ?: GamepadButtonMapping(selectedBtn)
    var customLabel by remember(selectedBtn) { mutableStateOf(current.customLabel) }
    var targetBit by remember(selectedBtn) { mutableIntStateOf(current.targetHidBit) }
    var localX by remember(selectedBtn) { mutableFloatStateOf(current.localTapXPercent) }
    var localY by remember(selectedBtn) { mutableFloatStateOf(current.localTapYPercent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Text("Custom Gamepad Mapping", color = NeonCyan, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select button to remap:", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(GamepadButton.A, GamepadButton.B, GamepadButton.X, GamepadButton.Y, GamepadButton.L1, GamepadButton.R1).forEach { btn ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedBtn == btn) NeonCyan else ObsidianElevated)
                                .clickable { selectedBtn = btn }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = btn.label,
                                color = if (selectedBtn == btn) ObsidianBlack else TextPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(GamepadButton.L2, GamepadButton.R2, GamepadButton.START, GamepadButton.SELECT, GamepadButton.L3, GamepadButton.R3).forEach { btn ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedBtn == btn) NeonCyan else ObsidianElevated)
                                .clickable { selectedBtn = btn }
                                .padding(horizontal = 7.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = btn.label,
                                color = if (selectedBtn == btn) ObsidianBlack else TextPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Button Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("HID Button Bit Index: ${targetBit + 1}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Slider(
                    value = targetBit.toFloat(),
                    onValueChange = { targetBit = it.toInt().coerceIn(0, 15) },
                    valueRange = 0f..15f,
                    steps = 14
                )
                Text(
                    "Local Mode Tap Target: X=${(localX * 100).toInt()}%, Y=${(localY * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Slider(value = localX, onValueChange = { localX = it }, valueRange = 0.05f..0.95f)
                Slider(value = localY, onValueChange = { localY = it }, valueRange = 0.05f..0.95f)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveMapping(
                        GamepadButtonMapping(
                            physicalButton = selectedBtn,
                            targetHidBit = targetBit,
                            customLabel = customLabel.ifBlank { selectedBtn.label },
                            localTapXPercent = localX,
                            localTapYPercent = localY
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack)
            ) {
                Text("Save Mapping")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
