package com.example.ui.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ControllerSettings
import com.example.model.KeySpec
import com.example.model.KeyboardLayoutProfile
import com.example.model.KeyboardStyle
import com.example.ui.components.RgbVisualState
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextSecondary

@Composable
fun KeyboardMouseSplitPanel(
    settings: ControllerSettings,
    layout: KeyboardLayoutProfile,
    rgbState: RgbVisualState,
    onAddKeyClick: () -> Unit,
    onSplitRatioChange: (Float) -> Unit,
    onKeyEvent: (KeySpec, Boolean) -> Unit,
    onMouseMove: (Int, Int) -> Unit,
    onMouseScroll: (Int) -> Unit,
    onMouseButton: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val ratio = settings.splitRatio.coerceIn(0.38f, 0.68f)
        val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPLIT COMBO • Drag divider to resize",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "KB ${(settings.keyboardOpacity * 100).toInt()}% | Mouse ${(settings.mouseOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = rgbState.primaryAccent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // LEFT: Keyboard (operates simultaneously with Mouse on right)
                Box(modifier = Modifier.weight(ratio)) {
                    if (settings.selectedKeyboardStyle == KeyboardStyle.GAMING_35) {
                        GamingKeyboard35Panel(
                            layout = layout,
                            opacity = settings.keyboardOpacity * settings.controllerOpacity,
                            rgbState = rgbState,
                            compactHalfWidth = false,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                            showHeaderControls = false,
                            onAddKeyClick = onAddKeyClick,
                            onKeyEvent = onKeyEvent
                        )
                    } else {
                        NormalKeyboardPanel(
                            opacity = settings.keyboardOpacity * settings.controllerOpacity,
                            rgbState = rgbState,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                            onKeyEvent = onKeyEvent
                        )
                    }
                }

                // Draggable Divider Handle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(14.dp)
                        .height(210.dp)
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianElevated)
                        .border(1.dp, rgbState.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .testTag("split_divider")
                        .pointerInput(ratio, totalWidthPx) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaRatio = dragAmount / totalWidthPx
                                onSplitRatioChange((ratio + deltaRatio).coerceIn(0.38f, 0.68f))
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(36.dp)
                            .background(rgbState.primaryAccent, RoundedCornerShape(2.dp))
                    )
                }

                // RIGHT: Mouse
                Box(modifier = Modifier.weight(1f - ratio)) {
                    MouseTrackpadPanel(
                        opacity = settings.mouseOpacity * settings.controllerOpacity,
                        sensitivity = settings.mouseSensitivity,
                        speed = settings.mouseSpeed,
                        trackpadScale = settings.trackpadSizeScale,
                        buttonConfig = settings.mouseButtonConfig,
                        rgbState = rgbState,
                        hapticEnabled = settings.hapticFeedbackEnabled,
                        compactMode = true,
                        onMove = onMouseMove,
                        onScroll = onMouseScroll,
                        onButton = onMouseButton
                    )
                }
            }
        }
    }
}
