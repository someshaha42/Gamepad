package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.RgbEffect
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianOutline
import kotlin.math.PI
import kotlin.math.sin

data class RgbVisualState(
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val borderBrush: Brush,
    val glowAlpha: Float
)

@Composable
fun rememberRgbVisualState(
    effect: RgbEffect,
    staticColor: Color = NeonCyan
): RgbVisualState {
    if (effect == RgbEffect.OFF) {
        return RgbVisualState(
            primaryAccent = ObsidianOutline,
            secondaryAccent = ObsidianOutline,
            borderBrush = Brush.linearGradient(listOf(ObsidianOutline, ObsidianOutline)),
            glowAlpha = 0f
        )
    }

    if (effect == RgbEffect.STATIC) {
        return RgbVisualState(
            primaryAccent = staticColor,
            secondaryAccent = NeonViolet,
            borderBrush = Brush.linearGradient(listOf(staticColor.copy(alpha = 0.85f), NeonViolet.copy(alpha = 0.75f))),
            glowAlpha = 0.75f
        )
    }

    val transition = rememberInfiniteTransition(label = "rgb_lighting")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (effect == RgbEffect.BREATHING) 3200 else 4800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rgb_phase"
    )

    return when (effect) {
        RgbEffect.BREATHING -> {
            val wave = (0.35f + 0.65f * ((sin(phase * 2f * PI.toFloat()) + 1f) * 0.5f))
            val c1 = staticColor.copy(alpha = wave)
            val c2 = NeonViolet.copy(alpha = wave * 0.85f)
            RgbVisualState(
                primaryAccent = c1,
                secondaryAccent = c2,
                borderBrush = Brush.linearGradient(listOf(c1, c2, c1)),
                glowAlpha = wave
            )
        }
        RgbEffect.RAINBOW -> {
            val palette = listOf(NeonCyan, NeonBlue, NeonViolet, NeonCrimson, NeonEmerald, NeonCyan)
            val idx = (phase * (palette.size - 1)).toInt().coerceIn(0, palette.size - 2)
            val frac = (phase * (palette.size - 1)) - idx
            val c1 = lerpColor(palette[idx], palette[idx + 1], frac)
            val nextIdx = (idx + 2) % (palette.size - 1)
            val c2 = lerpColor(palette[nextIdx], palette[nextIdx + 1], frac)
            RgbVisualState(
                primaryAccent = c1,
                secondaryAccent = c2,
                borderBrush = Brush.linearGradient(listOf(c1, c2, NeonViolet, c1)),
                glowAlpha = 0.9f
            )
        }
        else -> RgbVisualState(
            primaryAccent = staticColor,
            secondaryAccent = NeonViolet,
            borderBrush = Brush.linearGradient(listOf(staticColor, NeonViolet)),
            glowAlpha = 0.75f
        )
    }
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * f,
        green = start.green + (stop.green - start.green) * f,
        blue = start.blue + (stop.blue - start.blue) * f,
        alpha = start.alpha + (stop.alpha - start.alpha) * f
    )
}
