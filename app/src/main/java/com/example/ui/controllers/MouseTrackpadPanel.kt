package com.example.ui.controllers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MouseButtonConfig
import com.example.ui.components.RgbVisualState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun MouseTrackpadPanel(
    opacity: Float,
    sensitivity: Float,
    speed: Float,
    trackpadScale: Float,
    buttonConfig: MouseButtonConfig,
    rgbState: RgbVisualState,
    hapticEnabled: Boolean,
    compactMode: Boolean = false,
    onMove: (dx: Int, dy: Int) -> Unit,
    onScroll: (wheel: Int) -> Unit,
    onButton: (buttonBit: Int, pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var cursorTouchPos by remember { mutableStateOf<Offset?>(null) }
    var scrollAccum by remember { mutableStateOf(0f) }
    var leftDown by remember { mutableStateOf(false) }
    var middleDown by remember { mutableStateOf(false) }
    var rightDown by remember { mutableStateOf(false) }

    val basePadHeight = if (compactMode) 150.dp else (210f * trackpadScale).coerceIn(130f, 320f).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(opacity.coerceIn(0.08f, 1f))
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBlack.copy(alpha = 0.88f))
            .border(1.5.dp, rgbState.borderBrush, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!compactMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mouse,
                        contentDescription = "Mouse Trackpad",
                        tint = rgbState.primaryAccent,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "PRECISION TRACKPAD",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Sens ${"%.1f".format(sensitivity)}x • Spd ${"%.1f".format(speed)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(basePadHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Trackpad surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ObsidianCard, ObsidianElevated, ObsidianBlack)
                        )
                    )
                    .border(1.dp, rgbState.primaryAccent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .testTag("mouse_trackpad_surface")
                    .pointerInput(sensitivity, speed) {
                        detectDragGestures(
                            onDragStart = { offset -> cursorTouchPos = offset },
                            onDragEnd = { cursorTouchPos = null },
                            onDragCancel = { cursorTouchPos = null }
                        ) { change, dragAmount ->
                            change.consume()
                            cursorTouchPos = change.position
                            val gain = sensitivity * speed
                            val dx = (dragAmount.x * gain).roundToInt().coerceIn(-127, 127)
                            val dy = (dragAmount.y * gain).roundToInt().coerceIn(-127, 127)
                            if (dx != 0 || dy != 0) {
                                onMove(dx, dy)
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 32.dp.toPx()
                    var x = step
                    while (x < size.width) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = step
                    while (y < size.height) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                    cursorTouchPos?.let { pos ->
                        drawCircle(
                            color = rgbState.primaryAccent.copy(alpha = 0.25f),
                            radius = 28.dp.toPx(),
                            center = pos
                        )
                        drawCircle(
                            color = rgbState.primaryAccent,
                            radius = 6.dp.toPx(),
                            center = pos
                        )
                    }
                }
                Text(
                    text = if (cursorTouchPos == null) "DRAG TO MOVE CURSOR" else "TRACKING",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }

            // Dedicated Scroll Wheel strip
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianElevated)
                    .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .testTag("mouse_scroll_strip")
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            scrollAccum += dragAmount.y
                            if (scrollAccum >= 22f) {
                                scrollAccum = 0f
                                onScroll(-1)
                            } else if (scrollAccum <= -22f) {
                                scrollAccum = 0f
                                onScroll(1)
                            }
                        }
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Scroll Wheel",
                        tint = NeonViolet
                    )
                    Text(
                        text = "SCR",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Left / Middle / Right Click buttons
        val leftBit = if (buttonConfig.swapLeftRight) 2 else 1
        val rightBit = if (buttonConfig.swapLeftRight) 1 else 2

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MouseClickButton(
                label = buttonConfig.leftLabel,
                pressed = leftDown,
                accent = rgbState.primaryAccent,
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("mouse_left_click")
                    .pointerInput(leftBit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            leftDown = true
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onButton(leftBit, true)
                            waitForUpOrCancellation()
                            leftDown = false
                            onButton(leftBit, false)
                        }
                    }
            )
            if (!compactMode) {
                MouseClickButton(
                    label = buttonConfig.middleLabel,
                    pressed = middleDown,
                    accent = NeonViolet,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(48.dp)
                        .testTag("mouse_middle_click")
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                middleDown = true
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onButton(4, true)
                                waitForUpOrCancellation()
                                middleDown = false
                                onButton(4, false)
                            }
                        }
                )
            }
            MouseClickButton(
                label = buttonConfig.rightLabel,
                pressed = rightDown,
                accent = rgbState.secondaryAccent,
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("mouse_right_click")
                    .pointerInput(rightBit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            rightDown = true
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onButton(rightBit, true)
                            waitForUpOrCancellation()
                            rightDown = false
                            onButton(rightBit, false)
                        }
                    }
            )
        }
    }
}

@Composable
private fun MouseClickButton(
    label: String,
    pressed: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) accent.copy(alpha = 0.35f) else ObsidianElevated)
            .border(1.5.dp, if (pressed) NeonCyan else accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (pressed) Color.White else TextPrimary
        )
    }
}
