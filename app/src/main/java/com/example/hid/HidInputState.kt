package com.example.hid

import kotlin.math.roundToInt

/**
 * Thread-safe HID report builder for Keyboard (8 bytes), Mouse (4 bytes), and Gamepad (9 bytes).
 */
class HidInputState {
    private val lock = Any()

    // Keyboard state
    private var modifierMask: Int = 0
    private val activeKeys = LinkedHashSet<Int>(6)

    // Mouse button state
    private var mouseButtonsMask: Int = 0

    // Gamepad state
    private var gamepadButtonsMask: Int = 0
    private var dpadUp = false
    private var dpadDown = false
    private var dpadLeft = false
    private var dpadRight = false
    private var leftX = 0f
    private var leftY = 0f
    private var rightX = 0f
    private var rightY = 0f
    private var leftTrigger = 0f
    private var rightTrigger = 0f

    fun setKeyboardKey(hidUsageCode: Int, isModifier: Boolean, modBit: Int, pressed: Boolean): ByteArray = synchronized(lock) {
        if (isModifier) {
            modifierMask = if (pressed) modifierMask or modBit else modifierMask and modBit.inv()
        } else if (hidUsageCode > 0) {
            if (pressed) {
                if (activeKeys.size >= 6) {
                    val first = activeKeys.firstOrNull()
                    if (first != null) activeKeys.remove(first)
                }
                activeKeys.add(hidUsageCode)
            } else {
                activeKeys.remove(hidUsageCode)
            }
        }
        buildKeyboardReportLocked()
    }

    fun buildKeyboardReportLocked(): ByteArray {
        val report = ByteArray(8)
        report[0] = (modifierMask and 0xFF).toByte()
        report[1] = 0x00
        var index = 2
        for (usage in activeKeys) {
            if (index < 8) {
                report[index++] = (usage and 0xFF).toByte()
            }
        }
        return report
    }

    fun setMouseButton(buttonBit: Int, pressed: Boolean): ByteArray = synchronized(lock) {
        mouseButtonsMask = if (pressed) {
            mouseButtonsMask or buttonBit
        } else {
            mouseButtonsMask and buttonBit.inv()
        }
        byteArrayOf((mouseButtonsMask and 0x07).toByte(), 0, 0, 0)
    }

    fun buildMouseMoveReport(dx: Int, dy: Int, wheel: Int = 0): ByteArray = synchronized(lock) {
        byteArrayOf(
            (mouseButtonsMask and 0x07).toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte()
        )
    }

    fun setGamepadButton(bitIndex: Int, pressed: Boolean): ByteArray = synchronized(lock) {
        val mask = 1 shl bitIndex
        gamepadButtonsMask = if (pressed) gamepadButtonsMask or mask else gamepadButtonsMask and mask.inv()
        buildGamepadReportLocked()
    }

    fun setGamepadDpad(up: Boolean, down: Boolean, left: Boolean, right: Boolean): ByteArray = synchronized(lock) {
        dpadUp = up
        dpadDown = down
        dpadLeft = left
        dpadRight = right
        buildGamepadReportLocked()
    }

    fun setLeftStick(x: Float, y: Float): ByteArray = synchronized(lock) {
        leftX = x.coerceIn(-1f, 1f)
        leftY = y.coerceIn(-1f, 1f)
        buildGamepadReportLocked()
    }

    fun setRightStick(x: Float, y: Float): ByteArray = synchronized(lock) {
        rightX = x.coerceIn(-1f, 1f)
        rightY = y.coerceIn(-1f, 1f)
        buildGamepadReportLocked()
    }

    fun setTriggers(l2: Float, r2: Float): ByteArray = synchronized(lock) {
        leftTrigger = l2.coerceIn(0f, 1f)
        rightTrigger = r2.coerceIn(0f, 1f)
        buildGamepadReportLocked()
    }

    fun resetAll(): Triple<ByteArray, ByteArray, ByteArray> = synchronized(lock) {
        modifierMask = 0
        activeKeys.clear()
        mouseButtonsMask = 0
        gamepadButtonsMask = 0
        dpadUp = false
        dpadDown = false
        dpadLeft = false
        dpadRight = false
        leftX = 0f
        leftY = 0f
        rightX = 0f
        rightY = 0f
        leftTrigger = 0f
        rightTrigger = 0f
        Triple(buildKeyboardReportLocked(), byteArrayOf(0, 0, 0, 0), buildGamepadReportLocked())
    }

    private fun computeHatValue(): Int {
        return when {
            dpadUp && dpadRight -> 1
            dpadRight && dpadDown -> 3
            dpadDown && dpadLeft -> 5
            dpadLeft && dpadUp -> 7
            dpadUp -> 0
            dpadRight -> 2
            dpadDown -> 4
            dpadLeft -> 6
            else -> 8 // Null / neutral
        }
    }

    fun buildGamepadReportLocked(): ByteArray {
        val hat = computeHatValue() and 0x0F
        return byteArrayOf(
            (gamepadButtonsMask and 0xFF).toByte(),
            ((gamepadButtonsMask ushr 8) and 0xFF).toByte(),
            hat.toByte(),
            (leftX * 127f).roundToInt().coerceIn(-127, 127).toByte(),
            (leftY * 127f).roundToInt().coerceIn(-127, 127).toByte(),
            (rightX * 127f).roundToInt().coerceIn(-127, 127).toByte(),
            (rightY * 127f).roundToInt().coerceIn(-127, 127).toByte(),
            (leftTrigger * 255f).roundToInt().coerceIn(0, 255).toByte(),
            (rightTrigger * 255f).roundToInt().coerceIn(0, 255).toByte()
        )
    }
}
