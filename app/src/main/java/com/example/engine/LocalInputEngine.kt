package com.example.engine

import android.content.Context
import android.provider.Settings
import com.example.model.GamepadButton
import kotlin.math.abs

data class LocalControllerCapabilities(
    val hasOverlayPermission: Boolean = false,
    val hasAccessibilityEnabled: Boolean = false,
    val maxSimultaneousStrokes: Int = 10,
    val lastStatus: String = "Local Controller Standby"
)

/**
 * Local Controller engine that translates controller button presses, analog stick vectors,
 * and trackpad drags into AccessibilityService touch gestures on the same phone.
 */
class LocalInputEngine(private val context: Context) : InputEngine {

    private var lastMouseGestureTime = 0L
    private var lastStickGestureTime = 0L

    fun getCapabilities(): LocalControllerCapabilities {
        val overlayOk = Settings.canDrawOverlays(context)
        val service = SounceAccessibilityService.getInstance()
        val a11yOk = service != null || isAccessibilityEnabledInSettings()
        return LocalControllerCapabilities(
            hasOverlayPermission = overlayOk,
            hasAccessibilityEnabled = a11yOk,
            maxSimultaneousStrokes = service?.maxSimultaneousStrokes() ?: 10,
            lastStatus = SounceAccessibilityService.lastGestureStatus.value
        )
    }

    fun isAccessibilityEnabledInSettings(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(context.packageName, ignoreCase = true) &&
            enabled.contains("SounceAccessibilityService", ignoreCase = true)
    }

    override fun sendKey(
        hidUsageCode: Int,
        isModifier: Boolean,
        modifierMask: Int,
        pressed: Boolean,
        localTapX: Float,
        localTapY: Float
    ): DispatchOutcome {
        if (!pressed) return DispatchOutcome(true, "Local Key UP")
        val service = SounceAccessibilityService.getInstance()
            ?: return DispatchOutcome(false, "Enable Sounce Local Touch Bridge in Accessibility Settings")
        val ok = service.dispatchTapPercent(localTapX, localTapY, durationMs = 50L)
        return DispatchOutcome(
            ok,
            if (ok) "Local Tap @ (${(localTapX * 100).toInt()}%, ${(localTapY * 100).toInt()}%)"
            else "Gesture dispatch rejected"
        )
    }

    override fun sendMouseMove(dx: Int, dy: Int, wheel: Int): DispatchOutcome {
        val service = SounceAccessibilityService.getInstance()
            ?: return DispatchOutcome(false, "Accessibility Bridge inactive")
        val now = System.currentTimeMillis()
        if (now - lastMouseGestureTime < 90L) {
            return DispatchOutcome(false, "Throttled touch swipe")
        }
        lastMouseGestureTime = now
        if (wheel != 0) {
            val deltaY = if (wheel > 0) -0.14f else 0.14f
            val ok = service.dispatchSwipePercent(0.5f, 0.5f, 0.5f, 0.5f + deltaY, 120L)
            return DispatchOutcome(ok, "Local Scroll ${if (wheel > 0) "UP" else "DOWN"}")
        }
        val normX = (dx / 350f).coerceIn(-0.18f, 0.18f)
        val normY = (dy / 350f).coerceIn(-0.18f, 0.18f)
        if (abs(normX) < 0.01f && abs(normY) < 0.01f) return DispatchOutcome(false, "Micro delta")
        val ok = service.dispatchSwipePercent(0.65f, 0.45f, 0.65f + normX, 0.45f + normY, 75L)
        return DispatchOutcome(ok, "Local Look Swipe (${dx}, ${dy})")
    }

    override fun sendMouseButton(buttonBit: Int, pressed: Boolean): DispatchOutcome {
        if (!pressed) return DispatchOutcome(true, "Local Mouse UP")
        val service = SounceAccessibilityService.getInstance()
            ?: return DispatchOutcome(false, "Accessibility Bridge inactive")
        val tapX = when (buttonBit) {
            1 -> 0.84f // Primary fire button zone
            2 -> 0.72f // Secondary aim/scope zone
            else -> 0.50f
        }
        val ok = service.dispatchTapPercent(tapX, 0.65f, 55L)
        return DispatchOutcome(ok, "Local Mouse Tap #$buttonBit")
    }

    override fun sendGamepadButton(
        button: GamepadButton,
        hidBitIndex: Int,
        pressed: Boolean,
        localTapX: Float,
        localTapY: Float
    ): DispatchOutcome {
        if (!pressed) return DispatchOutcome(true, "Local ${button.label} UP")
        val service = SounceAccessibilityService.getInstance()
            ?: return DispatchOutcome(false, "Accessibility Bridge inactive")
        val ok = service.dispatchTapPercent(localTapX, localTapY, 55L)
        return DispatchOutcome(ok, "Local ${button.label} -> (${(localTapX * 100).toInt()}%, ${(localTapY * 100).toInt()}%)")
    }

    override fun sendGamepadDpad(up: Boolean, down: Boolean, left: Boolean, right: Boolean): DispatchOutcome {
        val x = (if (right) 0.08f else 0f) - (if (left) 0.08f else 0f)
        val y = (if (down) 0.08f else 0f) - (if (up) 0.08f else 0f)
        if (x == 0f && y == 0f) return DispatchOutcome(true, "Local D-Pad Center")
        val service = SounceAccessibilityService.getInstance()
            ?: return DispatchOutcome(false, "Accessibility Bridge inactive")
        val ok = service.dispatchSwipePercent(0.20f, 0.70f, 0.20f + x, 0.70f + y, 90L)
        return DispatchOutcome(ok, "Local D-Pad Swipe")
    }

    override fun sendGamepadLeftStick(x: Float, y: Float): DispatchOutcome {
        val service = SounceAccessibilityService.getInstance()
            ?: return DispatchOutcome(false, "Accessibility Bridge inactive")
        val now = System.currentTimeMillis()
        if (now - lastStickGestureTime < 110L) return DispatchOutcome(false, "Throttled stick")
        if (abs(x) < 0.15f && abs(y) < 0.15f) return DispatchOutcome(true, "LStick Center")
        lastStickGestureTime = now
        val ok = service.dispatchSwipePercent(0.20f, 0.72f, 0.20f + x * 0.11f, 0.72f + y * 0.11f, 95L)
        return DispatchOutcome(ok, "Local LStick Swipe")
    }

    override fun sendGamepadRightStick(x: Float, y: Float): DispatchOutcome {
        val service = SounceAccessibilityService.getInstance()
            ?: return DispatchOutcome(false, "Accessibility Bridge inactive")
        val now = System.currentTimeMillis()
        if (now - lastStickGestureTime < 110L) return DispatchOutcome(false, "Throttled stick")
        if (abs(x) < 0.15f && abs(y) < 0.15f) return DispatchOutcome(true, "RStick Center")
        lastStickGestureTime = now
        val ok = service.dispatchSwipePercent(0.70f, 0.48f, 0.70f + x * 0.12f, 0.48f + y * 0.12f, 85L)
        return DispatchOutcome(ok, "Local Aim Swipe")
    }

    override fun sendGamepadTriggers(l2: Float, r2: Float): DispatchOutcome {
        val service = SounceAccessibilityService.getInstance()
            ?: return DispatchOutcome(false, "Accessibility Bridge inactive")
        var ok = true
        if (r2 > 0.6f) ok = service.dispatchTapPercent(0.86f, 0.62f, 55L)
        else if (l2 > 0.6f) ok = service.dispatchTapPercent(0.75f, 0.56f, 55L)
        return DispatchOutcome(ok, "Local Triggers")
    }

    override fun releaseAll() {
        // Discrete gesture strokes finish automatically.
    }
}
