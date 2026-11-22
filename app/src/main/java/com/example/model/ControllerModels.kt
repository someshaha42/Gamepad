package com.example.model

enum class ControllerMode(val displayName: String, val shortLabel: String) {
    GAMEPAD("Gamepad", "Gamepad"),
    KEYBOARD_MOUSE("Keyboard + Mouse", "KB + Mouse"),
    KEYBOARD_ONLY("Keyboard Only", "Keyboard"),
    MOUSE_ONLY("Mouse Only", "Mouse")
}

enum class KeyboardStyle(val displayName: String) {
    GAMING_35("Gaming Keyboard"),
    NORMAL_QWERTY("Normal Keyboard")
}

enum class ConnectionEnvironment(val displayName: String) {
    AUTO("Automatic"),
    BLUETOOTH_HID("Bluetooth HID"),
    USB_HID("USB HID"),
    LOCAL_OVERLAY("Local Controller")
}

enum class ActiveEngineType(val label: String) {
    NONE("Standby"),
    BLUETOOTH_HID("Bluetooth HID Peripheral"),
    USB_HID_GADGET("USB HID Gadget"),
    LOCAL_ACCESSIBILITY("Local Touch Overlay")
}

enum class RgbEffect(val displayName: String) {
    STATIC("Static"),
    BREATHING("Breathing"),
    RAINBOW("Rainbow"),
    OFF("Off")
}

enum class GamepadButton(val label: String, val bitIndex: Int) {
    A("A", 0),
    B("B", 1),
    X("X", 2),
    Y("Y", 3),
    L1("L1", 4),
    R1("R1", 5),
    L2("L2", 6),
    R2("R2", 7),
    SELECT("SELECT", 8),
    START("START", 9),
    L3("L3", 10),
    R3("R3", 11),
    DPAD_UP("UP", 12),
    DPAD_DOWN("DOWN", 13),
    DPAD_LEFT("LEFT", 14),
    DPAD_RIGHT("RIGHT", 15)
}

data class KeySpec(
    val id: String,
    val label: String,
    val hidUsageCode: Int,
    val row: Int,
    val column: Int,
    val widthWeight: Float = 1f,
    val heightScale: Float = 1f,
    val isModifier: Boolean = false,
    val modifierMask: Int = 0,
    val isFnKey: Boolean = false,
    val isCapsLock: Boolean = false,
    val isCustomAdded: Boolean = false,
    val localTapXPercent: Float = 0.5f,
    val localTapYPercent: Float = 0.5f
)

data class KeyboardLayoutProfile(
    val id: Long = 1L,
    val name: String = "Sounce 35-Key Default",
    val gameTitle: String = "Universal FPS / MOBA",
    val keys: List<KeySpec>,
    val keySpacingDp: Float = 4f,
    val keyScale: Float = 1f,
    val isDefault: Boolean = true
)

data class GamepadButtonMapping(
    val physicalButton: GamepadButton,
    val targetHidBit: Int = physicalButton.bitIndex,
    val customLabel: String = physicalButton.label,
    val localTapXPercent: Float = 0.5f,
    val localTapYPercent: Float = 0.5f
)

data class MouseButtonConfig(
    val leftLabel: String = "LMB",
    val middleLabel: String = "MMB",
    val rightLabel: String = "RMB",
    val swapLeftRight: Boolean = false
)

data class ControllerSettings(
    val selectedMode: ControllerMode = ControllerMode.KEYBOARD_ONLY,
    val selectedKeyboardStyle: KeyboardStyle = KeyboardStyle.GAMING_35,
    val preferredConnection: ConnectionEnvironment = ConnectionEnvironment.AUTO,
    val autoReconnect: Boolean = true,
    val activeLayoutId: Long = 1L,
    // Transparency controls (0.08f .. 1.0f)
    val controllerOpacity: Float = 0.92f,
    val keyboardOpacity: Float = 0.94f,
    val mouseOpacity: Float = 0.94f,
    val gamepadOpacity: Float = 0.94f,
    val controllerVisible: Boolean = true,
    // Sensitivities
    val mouseSensitivity: Float = 1.4f,
    val mouseSpeed: Float = 1.2f,
    val joystickSensitivity: Float = 1.15f,
    val trackpadSizeScale: Float = 1.0f,
    val splitRatio: Float = 0.52f,
    // RGB Lighting
    val rgbEffect: RgbEffect = RgbEffect.RAINBOW,
    val rgbStaticColorArgb: Long = 0xFF00F0FF,
    // Floating Overlay Geometry
    val overlayX: Int = 36,
    val overlayY: Int = 140,
    val overlayWidthDp: Int = 360,
    val overlayHeightDp: Int = 270,
    val compactHalfWidthGamingKb: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val mouseButtonConfig: MouseButtonConfig = MouseButtonConfig(),
    val gamepadMappings: Map<GamepadButton, GamepadButtonMapping> = GamepadButton.entries.associateWith {
        GamepadButtonMapping(physicalButton = it)
    }
)
