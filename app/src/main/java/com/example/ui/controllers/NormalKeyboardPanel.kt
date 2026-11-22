package com.example.ui.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.DefaultSounceLayouts
import com.example.model.KeySpec
import com.example.ui.components.RgbVisualState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextSecondary

@Composable
fun NormalKeyboardPanel(
    opacity: Float,
    rgbState: RgbVisualState,
    hapticEnabled: Boolean,
    onKeyEvent: (KeySpec, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showFunctionRow by remember { mutableStateOf(true) }
    var capsLockActive by remember { mutableStateOf(false) }
    val pressedKeys = remember { mutableStateMapOf<String, Boolean>() }

    val rows = remember(showFunctionRow) {
        DefaultSounceLayouts.createNormalQwertyRows(includeFunctionRow = showFunctionRow)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(opacity.coerceIn(0.08f, 1f))
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBlack.copy(alpha = 0.90f))
            .border(1.5.dp, rgbState.borderBrush, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FULL QWERTY HID KEYBOARD",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianElevated)
                    .border(1.dp, rgbState.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { showFunctionRow = !showFunctionRow }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("toggle_fn_row")
            ) {
                Text(
                    text = if (showFunctionRow) "Hide F1-F12" else "Show F1-F12",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
            }
        }

        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                rowKeys.forEach { key ->
                    val isDown = pressedKeys[key.id] == true
                    val isLatched = key.isCapsLock && capsLockActive
                    GamingKeyCap(
                        keySpec = key,
                        isActive = isDown || isLatched,
                        isLatched = isLatched,
                        rgbState = rgbState,
                        modifier = Modifier
                            .weight(key.widthWeight)
                            .height(38.dp)
                            .testTag("qwerty_${key.id}")
                            .pointerInput(key.id) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    pressedKeys[key.id] = true
                                    if (hapticEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    if (key.isCapsLock) capsLockActive = !capsLockActive
                                    onKeyEvent(key, true)
                                    waitForUpOrCancellation()
                                    pressedKeys[key.id] = false
                                    onKeyEvent(key, false)
                                }
                            }
                    )
                }
            }
        }
    }
}
