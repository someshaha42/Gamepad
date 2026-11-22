package com.example.engine

import com.example.model.GamepadButton

data class DispatchOutcome(
    val dispatched: Boolean,
    val summary: String
)

interface InputEngine {
    fun sendKey(hidUsageCode: Int, isModifier: Boolean, modifierMask: Int, pressed: Boolean, localTapX: Float = 0.5f, localTapY: Float = 0.5f): DispatchOutcome
    fun sendMouseMove(dx: Int, dy: Int, wheel: Int = 0): DispatchOutcome
    fun sendMouseButton(buttonBit: Int, pressed: Boolean): DispatchOutcome
    fun sendGamepadButton(button: GamepadButton, hidBitIndex: Int, pressed: Boolean, localTapX: Float = 0.5f, localTapY: Float = 0.5f): DispatchOutcome
    fun sendGamepadDpad(up: Boolean, down: Boolean, left: Boolean, right: Boolean): DispatchOutcome
    fun sendGamepadLeftStick(x: Float, y: Float): DispatchOutcome
    fun sendGamepadRightStick(x: Float, y: Float): DispatchOutcome
    fun sendGamepadTriggers(l2: Float, r2: Float): DispatchOutcome
    fun releaseAll()
}
