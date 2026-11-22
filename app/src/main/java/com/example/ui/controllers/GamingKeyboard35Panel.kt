package com.example.ui.controllers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeySpec
import com.example.model.KeyboardLayoutProfile
import com.example.ui.components.RgbVisualState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GamingKeyboard35Panel(
    layout: KeyboardLayoutProfile,
    opacity: Float,
    rgbState: RgbVisualState,
    compactHalfWidth: Boolean,
    hapticEnabled: Boolean,
    showHeaderControls: Boolean = true,
    onAddKeyClick: () -> Unit = {},
    onKeyEvent: (KeySpec, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val pressedKeys = remember { mutableStateMapOf<String, Boolean>() }
    var capsLockActive by remember { mutableStateOf(false) }
    var fnLayerActive by remember { mutableStateOf(false) }

    val groupedRows = remember(layout.keys) {
        layout.keys.groupBy { it.row }.toSortedMap().values.map { rowList ->
            rowList.sortedBy { it.column }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val useCompactWidth = compactHalfWidth && maxWidth >= 560.dp
        val targetFraction = if (useCompactWidth) 0.54f else 1f

        Column(
            modifier = Modifier
                .fillMaxWidth(targetFraction)
                .alpha(opacity.coerceIn(0.08f, 1f))
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianBlack.copy(alpha = 0.88f))
                .border(1.5.dp, rgbState.borderBrush, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            if (showHeaderControls) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Gaming Keyboard",
                            tint = rgbState.primaryAccent,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Column {
                            Text(
                                text = layout.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${layout.keys.size} keys • ${if (fnLayerActive) "FN LAYER ON" else "STANDARD LAYER"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (fnLayerActive) NeonEmerald else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = ObsidianElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, rgbState.primaryAccent),
                        modifier = Modifier
                            .testTag("add_key_button")
                            .clickable { onAddKeyClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Key",
                                tint = rgbState.primaryAccent,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "+ ADD KEY",
                                style = MaterialTheme.typography.labelLarge,
                                color = rgbState.primaryAccent
                            )
                        }
                    }
                }
            }

            val spacing = layout.keySpacingDp.coerceIn(1f, 12f).dp
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                groupedRows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        rowKeys.forEach { key ->
                            val isDown = pressedKeys[key.id] == true
                            val isLatched = (key.isCapsLock && capsLockActive) || (key.isFnKey && fnLayerActive)
                            val active = isDown || isLatched
                            val keyHeight = (38f * layout.keyScale * key.heightScale).coerceIn(28f, 72f).dp

                            GamingKeyCap(
                                keySpec = key,
                                isActive = active,
                                isLatched = isLatched,
                                rgbState = rgbState,
                                modifier = Modifier
                                    .weight(key.widthWeight.coerceAtLeast(0.5f))
                                    .height(keyHeight)
                                    .testTag("key_${key.id}")
                                    .pointerInput(key.id, fnLayerActive) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            pressedKeys[key.id] = true
                                            if (hapticEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            if (key.isFnKey) {
                                                fnLayerActive = !fnLayerActive
                                            } else {
                                                if (key.isCapsLock) capsLockActive = !capsLockActive
                                                val resolved = resolveFnLayerKey(key, fnLayerActive)
                                                onKeyEvent(resolved, true)
                                            }
                                            waitForUpOrCancellation()
                                            pressedKeys[key.id] = false
                                            if (!key.isFnKey) {
                                                val resolved = resolveFnLayerKey(key, fnLayerActive)
                                                onKeyEvent(resolved, false)
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveFnLayerKey(key: KeySpec, fnActive: Boolean): KeySpec {
    if (!fnActive) return key
    return when (key.id) {
        "g_f1" -> key.copy(hidUsageCode = 0x3F) // F6
        "g_f2" -> key.copy(hidUsageCode = 0x40) // F7
        "g_f3" -> key.copy(hidUsageCode = 0x41) // F8
        "g_f4" -> key.copy(hidUsageCode = 0x42) // F9
        "g_f5" -> key.copy(hidUsageCode = 0x43) // F10
        "g_esc" -> key.copy(hidUsageCode = 0x4A) // Home
        else -> key
    }
}

@Composable
fun GamingKeyCap(
    keySpec: KeySpec,
    isActive: Boolean,
    isLatched: Boolean,
    rgbState: RgbVisualState,
    modifier: Modifier = Modifier
) {
    val isWasd = keySpec.label in setOf("W", "A", "S", "D")
    val borderColor = when {
        isActive -> NeonCyan
        isWasd -> rgbState.primaryAccent
        keySpec.isCustomAdded -> NeonViolet
        else -> rgbState.primaryAccent.copy(alpha = 0.45f)
    }

    val bgBrush = if (isActive) {
        Brush.verticalGradient(listOf(rgbState.primaryAccent.copy(alpha = 0.45f), NeonViolet.copy(alpha = 0.35f)))
    } else if (isWasd) {
        Brush.verticalGradient(listOf(ObsidianCard, ObsidianElevated))
    } else {
        Brush.verticalGradient(listOf(ObsidianElevated, ObsidianBlack))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(
                width = if (isActive || isWasd) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 2.dp)
    ) {
        Text(
            text = keySpec.label,
            color = when {
                isLatched -> NeonEmerald
                isActive -> Color.White
                isWasd -> rgbState.primaryAccent
                else -> TextPrimary
            },
            fontSize = if (keySpec.label.length > 4) 10.sp else 12.sp,
            fontWeight = if (isWasd || isActive) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
