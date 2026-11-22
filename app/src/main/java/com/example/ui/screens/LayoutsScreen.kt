package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControllerSettings
import com.example.model.KeySpec
import com.example.model.KeyboardLayoutProfile
import com.example.ui.ControllerViewModel
import com.example.ui.components.AddKeyModalDialog
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
fun LayoutsScreen(
    viewModel: ControllerViewModel,
    settings: ControllerSettings,
    allLayouts: List<KeyboardLayoutProfile>,
    activeLayout: KeyboardLayoutProfile,
    modifier: Modifier = Modifier
) {
    val rgbState = rememberRgbVisualState(settings.rgbEffect, Color(settings.rgbStaticColorArgb))
    var selectedKeyId by remember(activeLayout.id) { mutableStateOf<String?>(activeLayout.keys.firstOrNull()?.id) }
    val selectedKey = activeLayout.keys.firstOrNull { it.id == selectedKeyId }

    var showAddKeyDialog by remember { mutableStateOf(false) }
    var showSaveProfileDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Saved Game Profiles Row
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GAME KEYBOARD PROFILES",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${activeLayout.keys.size} keys",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allLayouts.forEach { profile ->
                        val isCurrent = profile.id == activeLayout.id
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) NeonCyan.copy(alpha = 0.2f) else ObsidianElevated)
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) NeonCyan else ObsidianBlack,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.selectLayout(profile.id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("layout_profile_${profile.id}")
                        ) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isCurrent) NeonCyan else TextPrimary
                            )
                            Text(
                                text = "${profile.gameTitle} (${profile.keys.size} keys)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddKeyDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("layouts_add_key_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+ ADD KEY", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = { showSaveProfileDialog = true },
                        border = BorderStroke(1.dp, NeonViolet),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_new_layout_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Layout", color = NeonViolet, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Interactive Layout Editor Grid (Tap any key to inspect/edit/reposition/resize)
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
            border = BorderStroke(1.5.dp, rgbState.borderBrush),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(activeLayout.keySpacingDp.dp)
            ) {
                Text(
                    text = "TAP A KEY TO EDIT LABEL, POSITION, OR SIZE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                val rows = activeLayout.keys.groupBy { it.row }.toSortedMap().values.map { it.sortedBy { k -> k.column } }
                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(activeLayout.keySpacingDp.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val isSelected = key.id == selectedKeyId
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(key.widthWeight.coerceAtLeast(0.5f))
                                    .height((38f * activeLayout.keyScale * key.heightScale).coerceIn(28f, 68f).dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> NeonCyan.copy(alpha = 0.35f)
                                            key.isCustomAdded -> NeonViolet.copy(alpha = 0.25f)
                                            else -> ObsidianElevated
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) NeonCyan else if (key.isCustomAdded) NeonViolet else ObsidianCard,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedKeyId = key.id }
                                    .testTag("editor_key_${key.id}")
                            ) {
                                Text(
                                    text = key.label,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Key Inspector
        if (selectedKey != null) {
            KeyInspectorCard(
                keySpec = selectedKey,
                onUpdate = { viewModel.updateKeySpec(it) },
                onMove = { dx, dy -> viewModel.moveKeyPosition(selectedKey.id, dx, dy) },
                onDelete = {
                    viewModel.removeCustomKey(selectedKey.id)
                    selectedKeyId = null
                }
            )
        }

        // Spacing, Scale & Restore Default 35-Key Layout
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("LAYOUT GEOMETRY & SPACING", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Key Spacing: ${activeLayout.keySpacingDp.toInt()} dp",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Slider(
                    value = activeLayout.keySpacingDp,
                    onValueChange = { viewModel.updateLayoutSpacingAndScale(it, activeLayout.keyScale) },
                    valueRange = 1f..12f
                )
                Text(
                    "Key Height Scale: ${"%.2f".format(activeLayout.keyScale)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Slider(
                    value = activeLayout.keyScale,
                    onValueChange = { viewModel.updateLayoutSpacingAndScale(activeLayout.keySpacingDp, it) },
                    valueRange = 0.75f..1.45f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRestoreConfirm = true },
                        border = BorderStroke(1.dp, NeonAmber),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_35_key_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restore Default 35-Key", color = NeonAmber, style = MaterialTheme.typography.labelLarge)
                    }
                    if (!activeLayout.isDefault) {
                        OutlinedButton(
                            onClick = { viewModel.deleteCustomLayout(activeLayout.id) },
                            border = BorderStroke(1.dp, NeonCrimson),
                            modifier = Modifier.testTag("delete_layout_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Layout", tint = NeonCrimson)
                        }
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

    if (showSaveProfileDialog) {
        SaveGameLayoutDialog(
            onDismiss = { showSaveProfileDialog = false },
            onSave = { name, game -> viewModel.saveAsNewGameLayout(name, game) }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            containerColor = ObsidianCard,
            title = { Text("Restore Default 35-Key Layout?", color = NeonAmber) },
            text = {
                Text(
                    "This restores the exact Sounce One-Handed RGB Gaming Keyboard (35 Keys) default layout without changing your general controller settings.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreDefault35KeyLayout()
                        showRestoreConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = ObsidianBlack)
                ) {
                    Text("Restore 35 Keys")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun KeyInspectorCard(
    keySpec: KeySpec,
    onUpdate: (KeySpec) -> Unit,
    onMove: (deltaCol: Int, deltaRow: Int) -> Unit,
    onDelete: () -> Unit
) {
    var labelDraft by remember(keySpec.id) { mutableStateOf(keySpec.label) }
    var widthDraft by remember(keySpec.id) { mutableFloatStateOf(keySpec.widthWeight) }
    var heightDraft by remember(keySpec.id) { mutableFloatStateOf(keySpec.heightScale) }
    var tapX by remember(keySpec.id) { mutableFloatStateOf(keySpec.localTapXPercent) }
    var tapY by remember(keySpec.id) { mutableFloatStateOf(keySpec.localTapYPercent) }

    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EDIT KEY: ${keySpec.label} (HID 0x${keySpec.hidUsageCode.toString(16)})",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
                if (keySpec.isCustomAdded) {
                    OutlinedButton(
                        onClick = onDelete,
                        border = BorderStroke(1.dp, NeonCrimson),
                        modifier = Modifier.testTag("remove_custom_key_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove Key", tint = NeonCrimson, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove", color = NeonCrimson, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = labelDraft,
                    onValueChange = { labelDraft = it },
                    label = { Text("Key Label") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onUpdate(
                            keySpec.copy(
                                label = labelDraft.trim().ifBlank { keySpec.label },
                                widthWeight = widthDraft,
                                heightScale = heightDraft,
                                localTapXPercent = tapX,
                                localTapYPercent = tapY
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = ObsidianBlack),
                    modifier = Modifier.testTag("apply_key_edit_btn")
                ) {
                    Text("Apply")
                }
            }

            // Reposition Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Move Position:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onMove(-1, 0) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left", tint = TextPrimary)
                    }
                    IconButton(onClick = { onMove(1, 0) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right", tint = TextPrimary)
                    }
                    IconButton(onClick = { onMove(0, -1) }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up Row", tint = TextPrimary)
                    }
                    IconButton(onClick = { onMove(0, 1) }) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down Row", tint = TextPrimary)
                    }
                }
            }

            Text("Key Width Weight: ${"%.2f".format(widthDraft)}x", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Slider(value = widthDraft, onValueChange = { widthDraft = it }, valueRange = 0.6f..3.5f)

            Text("Key Height Scale: ${"%.2f".format(heightDraft)}x", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Slider(value = heightDraft, onValueChange = { heightDraft = it }, valueRange = 0.75f..1.8f)

            Text(
                "Local Mode Touch Target: X=${(tapX * 100).toInt()}%, Y=${(tapY * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Slider(value = tapX, onValueChange = { tapX = it }, valueRange = 0.05f..0.95f)
            Slider(value = tapY, onValueChange = { tapY = it }, valueRange = 0.05f..0.95f)
        }
    }
}

@Composable
private fun SaveGameLayoutDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, gameTitle: String) -> Unit
) {
    var layoutName by remember { mutableStateOf("") }
    var gameTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = { Text("Save Game Layout Profile", color = NeonCyan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = layoutName,
                    onValueChange = { layoutName = it },
                    label = { Text("Layout Name (e.g. FPS Custom 38)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gameTitle,
                    onValueChange = { gameTitle = it },
                    label = { Text("Game Name (e.g. BGMI / CODM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = layoutName.isNotBlank(),
                onClick = {
                    onSave(layoutName, gameTitle)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack)
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
