package com.example.ui.controllers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GamepadButton
import com.example.model.GamepadButtonMapping
import com.example.ui.components.RgbVisualState
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.hypot

@Composable
fun GamepadPanel(
    opacity: Float,
    joystickSensitivity: Float,
    mappings: Map<GamepadButton, GamepadButtonMapping>,
    rgbState: RgbVisualState,
    hapticEnabled: Boolean,
    compactMode: Boolean = false,
    onOpenMappingDialog: () -> Unit = {},
    onButtonEvent: (GamepadButton, GamepadButtonMapping, Boolean) -> Unit,
    onDpadEvent: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit,
    onLeftStick: (x: Float, y: Float) -> Unit,
    onRightStick: (x: Float, y: Float) -> Unit,
    onTriggers: (l2: Float, r2: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val pressed = remember { mutableStateMapOf<GamepadButton, Boolean>() }

    fun mappingFor(btn: GamepadButton): GamepadButtonMapping =
        mappings[btn] ?: GamepadButtonMapping(btn)

    fun triggerDpadUpdate() {
        onDpadEvent(
            pressed[GamepadButton.DPAD_UP] == true,
            pressed[GamepadButton.DPAD_DOWN] == true,
            pressed[GamepadButton.DPAD_LEFT] == true,
            pressed[GamepadButton.DPAD_RIGHT] == true
        )
    }

    fun triggerL2R2Update() {
        val l2 = if (pressed[GamepadButton.L2] == true) 1f else 0f
        val r2 = if (pressed[GamepadButton.R2] == true) 1f else 0f
        onTriggers(l2, r2)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(opacity.coerceIn(0.08f, 1f))
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianBlack.copy(alpha = 0.88f))
            .border(1.5.dp, rgbState.borderBrush, RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Shoulder & Trigger Row: L2, L1, SELECT, [Map], START, R1, R2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShoulderButton(
                mapping = mappingFor(GamepadButton.L2),
                isPressed = pressed[GamepadButton.L2] == true,
                accent = NeonViolet,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("pad_l2")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pressed[GamepadButton.L2] = true
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onButtonEvent(GamepadButton.L2, mappingFor(GamepadButton.L2), true)
                            triggerL2R2Update()
                            waitForUpOrCancellation()
                            pressed[GamepadButton.L2] = false
                            onButtonEvent(GamepadButton.L2, mappingFor(GamepadButton.L2), false)
                            triggerL2R2Update()
                        }
                    }
            )
            ShoulderButton(
                mapping = mappingFor(GamepadButton.L1),
                isPressed = pressed[GamepadButton.L1] == true,
                accent = NeonCyan,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("pad_l1")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pressed[GamepadButton.L1] = true
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onButtonEvent(GamepadButton.L1, mappingFor(GamepadButton.L1), true)
                            waitForUpOrCancellation()
                            pressed[GamepadButton.L1] = false
                            onButtonEvent(GamepadButton.L1, mappingFor(GamepadButton.L1), false)
                        }
                    }
            )
            if (!compactMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianElevated)
                        .border(1.dp, rgbState.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onOpenMappingDialog() }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .testTag("remap_gamepad_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Custom Button Mapping",
                        tint = rgbState.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            ShoulderButton(
                mapping = mappingFor(GamepadButton.R1),
                isPressed = pressed[GamepadButton.R1] == true,
                accent = NeonCyan,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("pad_r1")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pressed[GamepadButton.R1] = true
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onButtonEvent(GamepadButton.R1, mappingFor(GamepadButton.R1), true)
                            waitForUpOrCancellation()
                            pressed[GamepadButton.R1] = false
                            onButtonEvent(GamepadButton.R1, mappingFor(GamepadButton.R1), false)
                        }
                    }
            )
            ShoulderButton(
                mapping = mappingFor(GamepadButton.R2),
                isPressed = pressed[GamepadButton.R2] == true,
                accent = NeonCrimson,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("pad_r2")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pressed[GamepadButton.R2] = true
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onButtonEvent(GamepadButton.R2, mappingFor(GamepadButton.R2), true)
                            triggerL2R2Update()
                            waitForUpOrCancellation()
                            pressed[GamepadButton.R2] = false
                            onButtonEvent(GamepadButton.R2, mappingFor(GamepadButton.R2), false)
                            triggerL2R2Update()
                        }
                    }
            )
        }

        // Middle Row: D-Pad + Center Select/Start + A/B/X/Y Diamond
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // D-Pad Cluster
            val dpadSize = if (compactMode) 40.dp else 46.dp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DpadButton(
                    label = "▲",
                    isPressed = pressed[GamepadButton.DPAD_UP] == true,
                    accent = rgbState.primaryAccent,
                    modifier = Modifier
                        .size(dpadSize)
                        .testTag("pad_dpad_up")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                pressed[GamepadButton.DPAD_UP] = true
                                triggerDpadUpdate()
                                waitForUpOrCancellation()
                                pressed[GamepadButton.DPAD_UP] = false
                                triggerDpadUpdate()
                            }
                        }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(dpadSize * 0.55f)) {
                    DpadButton(
                        label = "◀",
                        isPressed = pressed[GamepadButton.DPAD_LEFT] == true,
                        accent = rgbState.primaryAccent,
                        modifier = Modifier
                            .size(dpadSize)
                            .testTag("pad_dpad_left")
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    pressed[GamepadButton.DPAD_LEFT] = true
                                    triggerDpadUpdate()
                                    waitForUpOrCancellation()
                                    pressed[GamepadButton.DPAD_LEFT] = false
                                    triggerDpadUpdate()
                                }
                            }
                    )
                    DpadButton(
                        label = "▶",
                        isPressed = pressed[GamepadButton.DPAD_RIGHT] == true,
                        accent = rgbState.primaryAccent,
                        modifier = Modifier
                            .size(dpadSize)
                            .testTag("pad_dpad_right")
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    pressed[GamepadButton.DPAD_RIGHT] = true
                                    triggerDpadUpdate()
                                    waitForUpOrCancellation()
                                    pressed[GamepadButton.DPAD_RIGHT] = false
                                    triggerDpadUpdate()
                                }
                            }
                    )
                }
                DpadButton(
                    label = "▼",
                    isPressed = pressed[GamepadButton.DPAD_DOWN] == true,
                    accent = rgbState.primaryAccent,
                    modifier = Modifier
                        .size(dpadSize)
                        .testTag("pad_dpad_down")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                pressed[GamepadButton.DPAD_DOWN] = true
                                triggerDpadUpdate()
                                waitForUpOrCancellation()
                                pressed[GamepadButton.DPAD_DOWN] = false
                                triggerDpadUpdate()
                            }
                        }
                )
            }

            // Center Select & Start
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShoulderButton(
                    mapping = mappingFor(GamepadButton.SELECT),
                    isPressed = pressed[GamepadButton.SELECT] == true,
                    accent = TextSecondary,
                    modifier = Modifier
                        .width(68.dp)
                        .height(34.dp)
                        .testTag("pad_select")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                pressed[GamepadButton.SELECT] = true
                                onButtonEvent(GamepadButton.SELECT, mappingFor(GamepadButton.SELECT), true)
                                waitForUpOrCancellation()
                                pressed[GamepadButton.SELECT] = false
                                onButtonEvent(GamepadButton.SELECT, mappingFor(GamepadButton.SELECT), false)
                            }
                        }
                )
                ShoulderButton(
                    mapping = mappingFor(GamepadButton.START),
                    isPressed = pressed[GamepadButton.START] == true,
                    accent = NeonEmerald,
                    modifier = Modifier
                        .width(68.dp)
                        .height(34.dp)
                        .testTag("pad_start")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                pressed[GamepadButton.START] = true
                                onButtonEvent(GamepadButton.START, mappingFor(GamepadButton.START), true)
                                waitForUpOrCancellation()
                                pressed[GamepadButton.START] = false
                                onButtonEvent(GamepadButton.START, mappingFor(GamepadButton.START), false)
                            }
                        }
                )
            }

            // A/B/X/Y Diamond
            val abxySize = if (compactMode) 42.dp else 48.dp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FaceButton(
                    mapping = mappingFor(GamepadButton.Y),
                    isPressed = pressed[GamepadButton.Y] == true,
                    color = NeonAmber,
                    modifier = Modifier
                        .size(abxySize)
                        .testTag("pad_y")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                pressed[GamepadButton.Y] = true
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onButtonEvent(GamepadButton.Y, mappingFor(GamepadButton.Y), true)
                                waitForUpOrCancellation()
                                pressed[GamepadButton.Y] = false
                                onButtonEvent(GamepadButton.Y, mappingFor(GamepadButton.Y), false)
                            }
                        }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(abxySize * 0.6f)) {
                    FaceButton(
                        mapping = mappingFor(GamepadButton.X),
                        isPressed = pressed[GamepadButton.X] == true,
                        color = NeonBlue,
                        modifier = Modifier
                            .size(abxySize)
                            .testTag("pad_x")
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    pressed[GamepadButton.X] = true
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onButtonEvent(GamepadButton.X, mappingFor(GamepadButton.X), true)
                                    waitForUpOrCancellation()
                                    pressed[GamepadButton.X] = false
                                    onButtonEvent(GamepadButton.X, mappingFor(GamepadButton.X), false)
                                }
                            }
                    )
                    FaceButton(
                        mapping = mappingFor(GamepadButton.B),
                        isPressed = pressed[GamepadButton.B] == true,
                        color = NeonCrimson,
                        modifier = Modifier
                            .size(abxySize)
                            .testTag("pad_b")
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    pressed[GamepadButton.B] = true
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onButtonEvent(GamepadButton.B, mappingFor(GamepadButton.B), true)
                                    waitForUpOrCancellation()
                                    pressed[GamepadButton.B] = false
                                    onButtonEvent(GamepadButton.B, mappingFor(GamepadButton.B), false)
                                }
                            }
                    )
                }
                FaceButton(
                    mapping = mappingFor(GamepadButton.A),
                    isPressed = pressed[GamepadButton.A] == true,
                    color = NeonEmerald,
                    modifier = Modifier
                        .size(abxySize)
                        .testTag("pad_a")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                pressed[GamepadButton.A] = true
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onButtonEvent(GamepadButton.A, mappingFor(GamepadButton.A), true)
                                waitForUpOrCancellation()
                                pressed[GamepadButton.A] = false
                                onButtonEvent(GamepadButton.A, mappingFor(GamepadButton.A), false)
                            }
                        }
                )
            }
        }

        // Bottom Row: Left Analog Joystick + L3/R3 + Right Analog Joystick
        val stickSize = if (compactMode) 104.dp else 128.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnalogJoystick(
                label = "L-STICK",
                sensitivity = joystickSensitivity,
                accentColor = rgbState.primaryAccent,
                modifier = Modifier
                    .size(stickSize)
                    .testTag("pad_left_stick"),
                onStickMoved = onLeftStick
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShoulderButton(
                    mapping = mappingFor(GamepadButton.L3),
                    isPressed = pressed[GamepadButton.L3] == true,
                    accent = rgbState.primaryAccent,
                    modifier = Modifier
                        .width(48.dp)
                        .height(34.dp)
                        .testTag("pad_l3")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                pressed[GamepadButton.L3] = true
                                onButtonEvent(GamepadButton.L3, mappingFor(GamepadButton.L3), true)
                                waitForUpOrCancellation()
                                pressed[GamepadButton.L3] = false
                                onButtonEvent(GamepadButton.L3, mappingFor(GamepadButton.L3), false)
                            }
                        }
                )
                ShoulderButton(
                    mapping = mappingFor(GamepadButton.R3),
                    isPressed = pressed[GamepadButton.R3] == true,
                    accent = rgbState.secondaryAccent,
                    modifier = Modifier
                        .width(48.dp)
                        .height(34.dp)
                        .testTag("pad_r3")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                pressed[GamepadButton.R3] = true
                                onButtonEvent(GamepadButton.R3, mappingFor(GamepadButton.R3), true)
                                waitForUpOrCancellation()
                                pressed[GamepadButton.R3] = false
                                onButtonEvent(GamepadButton.R3, mappingFor(GamepadButton.R3), false)
                            }
                        }
                )
            }
            AnalogJoystick(
                label = "R-STICK",
                sensitivity = joystickSensitivity,
                accentColor = rgbState.secondaryAccent,
                modifier = Modifier
                    .size(stickSize)
                    .testTag("pad_right_stick"),
                onStickMoved = onRightStick
            )
        }
    }
}

@Composable
private fun ShoulderButton(
    mapping: GamepadButtonMapping,
    isPressed: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPressed) accent.copy(alpha = 0.35f) else ObsidianElevated)
            .border(1.2.dp, if (isPressed) Color.White else accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
    ) {
        Text(
            text = mapping.customLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) Color.White else TextPrimary
        )
    }
}

@Composable
private fun DpadButton(
    label: String,
    isPressed: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPressed) accent.copy(alpha = 0.4f) else ObsidianCard)
            .border(1.2.dp, if (isPressed) NeonCyan else accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
    ) {
        Text(text = label, color = if (isPressed) Color.White else TextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun FaceButton(
    mapping: GamepadButtonMapping,
    isPressed: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isPressed) color.copy(alpha = 0.45f)
                else ObsidianCard
            )
            .border(1.8.dp, if (isPressed) Color.White else color, CircleShape)
    ) {
        Text(
            text = mapping.customLabel,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (isPressed) Color.White else color
        )
    }
}

@Composable
fun AnalogJoystick(
    label: String,
    sensitivity: Float,
    accentColor: Color,
    onStickMoved: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pointerInput(sensitivity) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onStickMoved(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onStickMoved(0f, 0f)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val maxRadius = (size.width.coerceAtMost(size.height) * 0.34f).coerceAtLeast(1f)
                    val next = knobOffset + dragAmount
                    val dist = hypot(next.x, next.y)
                    val clamped = if (dist > maxRadius) {
                        Offset(next.x / dist * maxRadius, next.y / dist * maxRadius)
                    } else {
                        next
                    }
                    knobOffset = clamped
                    val nx = ((clamped.x / maxRadius) * sensitivity).coerceIn(-1f, 1f)
                    val ny = ((clamped.y / maxRadius) * sensitivity).coerceIn(-1f, 1f)
                    onStickMoved(nx, ny)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerR = size.minDimension * 0.46f
            val innerR = size.minDimension * 0.21f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ObsidianCard, ObsidianBlack),
                    center = center,
                    radius = outerR
                ),
                radius = outerR,
                center = center
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.55f),
                radius = outerR,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.16f),
                radius = outerR * 0.55f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            val thumbCenter = center + knobOffset
            drawCircle(
                color = accentColor.copy(alpha = 0.32f),
                radius = innerR * 1.25f,
                center = thumbCenter
            )
            drawCircle(
                color = ObsidianElevated,
                radius = innerR,
                center = thumbCenter
            )
            drawCircle(
                color = accentColor,
                radius = innerR,
                center = thumbCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        if (knobOffset == Offset.Zero) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }
    }
}
