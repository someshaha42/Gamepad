package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ConnectionEnvironment
import com.example.model.ControllerMode
import com.example.model.ControllerSettings
import com.example.model.GamepadButton
import com.example.model.GamepadButtonMapping
import com.example.model.KeyboardStyle
import com.example.model.MouseButtonConfig
import com.example.model.RgbEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.controllerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sounce_controller_prefs"
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val SELECTED_MODE = stringPreferencesKey("selected_mode")
        val KEYBOARD_STYLE = stringPreferencesKey("keyboard_style")
        val PREFERRED_CONNECTION = stringPreferencesKey("preferred_connection")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val ACTIVE_LAYOUT_ID = longPreferencesKey("active_layout_id")

        val CONTROLLER_OPACITY = floatPreferencesKey("controller_opacity")
        val KEYBOARD_OPACITY = floatPreferencesKey("keyboard_opacity")
        val MOUSE_OPACITY = floatPreferencesKey("mouse_opacity")
        val GAMEPAD_OPACITY = floatPreferencesKey("gamepad_opacity")
        val CONTROLLER_VISIBLE = booleanPreferencesKey("controller_visible")

        val MOUSE_SENSITIVITY = floatPreferencesKey("mouse_sensitivity")
        val MOUSE_SPEED = floatPreferencesKey("mouse_speed")
        val JOYSTICK_SENSITIVITY = floatPreferencesKey("joystick_sensitivity")
        val TRACKPAD_SCALE = floatPreferencesKey("trackpad_scale")
        val SPLIT_RATIO = floatPreferencesKey("split_ratio")

        val RGB_EFFECT = stringPreferencesKey("rgb_effect")
        val RGB_STATIC_COLOR = longPreferencesKey("rgb_static_color")

        val OVERLAY_X = intPreferencesKey("overlay_x")
        val OVERLAY_Y = intPreferencesKey("overlay_y")
        val OVERLAY_W = intPreferencesKey("overlay_w")
        val OVERLAY_H = intPreferencesKey("overlay_h")

        val COMPACT_HALF_WIDTH = booleanPreferencesKey("compact_half_width")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val MOUSE_SWAP_LR = booleanPreferencesKey("mouse_swap_lr")
        val MOUSE_LEFT_LABEL = stringPreferencesKey("mouse_left_label")
        val MOUSE_MIDDLE_LABEL = stringPreferencesKey("mouse_middle_label")
        val MOUSE_RIGHT_LABEL = stringPreferencesKey("mouse_right_label")
        val GAMEPAD_MAPPINGS_JSON = stringPreferencesKey("gamepad_mappings_json")
        val LAST_BT_DEVICE_ADDR = stringPreferencesKey("last_bt_device_addr")
    }

    val settingsFlow: Flow<ControllerSettings> = context.controllerDataStore.data.map { prefs ->
        val default = ControllerSettings()
        ControllerSettings(
            selectedMode = prefs[Keys.SELECTED_MODE]?.let { runCatching { ControllerMode.valueOf(it) }.getOrNull() }
                ?: default.selectedMode,
            selectedKeyboardStyle = prefs[Keys.KEYBOARD_STYLE]?.let { runCatching { KeyboardStyle.valueOf(it) }.getOrNull() }
                ?: default.selectedKeyboardStyle,
            preferredConnection = prefs[Keys.PREFERRED_CONNECTION]?.let { runCatching { ConnectionEnvironment.valueOf(it) }.getOrNull() }
                ?: default.preferredConnection,
            autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: default.autoReconnect,
            activeLayoutId = prefs[Keys.ACTIVE_LAYOUT_ID] ?: default.activeLayoutId,
            controllerOpacity = (prefs[Keys.CONTROLLER_OPACITY] ?: default.controllerOpacity).coerceIn(0.08f, 1f),
            keyboardOpacity = (prefs[Keys.KEYBOARD_OPACITY] ?: default.keyboardOpacity).coerceIn(0.08f, 1f),
            mouseOpacity = (prefs[Keys.MOUSE_OPACITY] ?: default.mouseOpacity).coerceIn(0.08f, 1f),
            gamepadOpacity = (prefs[Keys.GAMEPAD_OPACITY] ?: default.gamepadOpacity).coerceIn(0.08f, 1f),
            controllerVisible = prefs[Keys.CONTROLLER_VISIBLE] ?: default.controllerVisible,
            mouseSensitivity = (prefs[Keys.MOUSE_SENSITIVITY] ?: default.mouseSensitivity).coerceIn(0.2f, 4.0f),
            mouseSpeed = (prefs[Keys.MOUSE_SPEED] ?: default.mouseSpeed).coerceIn(0.2f, 4.0f),
            joystickSensitivity = (prefs[Keys.JOYSTICK_SENSITIVITY] ?: default.joystickSensitivity).coerceIn(0.3f, 2.5f),
            trackpadSizeScale = (prefs[Keys.TRACKPAD_SCALE] ?: default.trackpadSizeScale).coerceIn(0.6f, 1.4f),
            splitRatio = (prefs[Keys.SPLIT_RATIO] ?: default.splitRatio).coerceIn(0.35f, 0.72f),
            rgbEffect = prefs[Keys.RGB_EFFECT]?.let { runCatching { RgbEffect.valueOf(it) }.getOrNull() }
                ?: default.rgbEffect,
            rgbStaticColorArgb = prefs[Keys.RGB_STATIC_COLOR] ?: default.rgbStaticColorArgb,
            overlayX = prefs[Keys.OVERLAY_X] ?: default.overlayX,
            overlayY = prefs[Keys.OVERLAY_Y] ?: default.overlayY,
            overlayWidthDp = (prefs[Keys.OVERLAY_W] ?: default.overlayWidthDp).coerceIn(220, 720),
            overlayHeightDp = (prefs[Keys.OVERLAY_H] ?: default.overlayHeightDp).coerceIn(180, 560),
            compactHalfWidthGamingKb = prefs[Keys.COMPACT_HALF_WIDTH] ?: default.compactHalfWidthGamingKb,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: default.hapticFeedbackEnabled,
            mouseButtonConfig = MouseButtonConfig(
                leftLabel = prefs[Keys.MOUSE_LEFT_LABEL] ?: "LMB",
                middleLabel = prefs[Keys.MOUSE_MIDDLE_LABEL] ?: "MMB",
                rightLabel = prefs[Keys.MOUSE_RIGHT_LABEL] ?: "RMB",
                swapLeftRight = prefs[Keys.MOUSE_SWAP_LR] ?: false
            ),
            gamepadMappings = decodeGamepadMappings(prefs[Keys.GAMEPAD_MAPPINGS_JSON], default.gamepadMappings)
        )
    }

    val lastBluetoothAddressFlow: Flow<String?> = context.controllerDataStore.data.map { it[Keys.LAST_BT_DEVICE_ADDR] }

    suspend fun updateMode(mode: ControllerMode) {
        context.controllerDataStore.edit { it[Keys.SELECTED_MODE] = mode.name }
    }

    suspend fun updateKeyboardStyle(style: KeyboardStyle) {
        context.controllerDataStore.edit { it[Keys.KEYBOARD_STYLE] = style.name }
    }

    suspend fun updatePreferredConnection(env: ConnectionEnvironment) {
        context.controllerDataStore.edit { it[Keys.PREFERRED_CONNECTION] = env.name }
    }

    suspend fun updateAutoReconnect(enabled: Boolean) {
        context.controllerDataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
    }

    suspend fun updateActiveLayoutId(id: Long) {
        context.controllerDataStore.edit { it[Keys.ACTIVE_LAYOUT_ID] = id }
    }

    suspend fun updateOpacities(
        controller: Float? = null,
        keyboard: Float? = null,
        mouse: Float? = null,
        gamepad: Float? = null,
        visible: Boolean? = null
    ) {
        context.controllerDataStore.edit { prefs ->
            controller?.let { prefs[Keys.CONTROLLER_OPACITY] = it.coerceIn(0.08f, 1f) }
            keyboard?.let { prefs[Keys.KEYBOARD_OPACITY] = it.coerceIn(0.08f, 1f) }
            mouse?.let { prefs[Keys.MOUSE_OPACITY] = it.coerceIn(0.08f, 1f) }
            gamepad?.let { prefs[Keys.GAMEPAD_OPACITY] = it.coerceIn(0.08f, 1f) }
            visible?.let { prefs[Keys.CONTROLLER_VISIBLE] = it }
        }
    }

    suspend fun updateSensitivities(
        mouseSensitivity: Float? = null,
        mouseSpeed: Float? = null,
        joystickSensitivity: Float? = null,
        trackpadScale: Float? = null,
        splitRatio: Float? = null
    ) {
        context.controllerDataStore.edit { prefs ->
            mouseSensitivity?.let { prefs[Keys.MOUSE_SENSITIVITY] = it.coerceIn(0.2f, 4f) }
            mouseSpeed?.let { prefs[Keys.MOUSE_SPEED] = it.coerceIn(0.2f, 4f) }
            joystickSensitivity?.let { prefs[Keys.JOYSTICK_SENSITIVITY] = it.coerceIn(0.3f, 2.5f) }
            trackpadScale?.let { prefs[Keys.TRACKPAD_SCALE] = it.coerceIn(0.6f, 1.4f) }
            splitRatio?.let { prefs[Keys.SPLIT_RATIO] = it.coerceIn(0.35f, 0.72f) }
        }
    }

    suspend fun updateRgb(effect: RgbEffect? = null, staticColorArgb: Long? = null) {
        context.controllerDataStore.edit { prefs ->
            effect?.let { prefs[Keys.RGB_EFFECT] = it.name }
            staticColorArgb?.let { prefs[Keys.RGB_STATIC_COLOR] = it }
        }
    }

    suspend fun updateOverlayBounds(x: Int? = null, y: Int? = null, widthDp: Int? = null, heightDp: Int? = null) {
        context.controllerDataStore.edit { prefs ->
            x?.let { prefs[Keys.OVERLAY_X] = it }
            y?.let { prefs[Keys.OVERLAY_Y] = it }
            widthDp?.let { prefs[Keys.OVERLAY_W] = it.coerceIn(220, 720) }
            heightDp?.let { prefs[Keys.OVERLAY_H] = it.coerceIn(180, 560) }
        }
    }

    suspend fun updateCompactHalfWidth(compact: Boolean) {
        context.controllerDataStore.edit { it[Keys.COMPACT_HALF_WIDTH] = compact }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.controllerDataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun updateMouseConfig(config: MouseButtonConfig) {
        context.controllerDataStore.edit { prefs ->
            prefs[Keys.MOUSE_LEFT_LABEL] = config.leftLabel
            prefs[Keys.MOUSE_MIDDLE_LABEL] = config.middleLabel
            prefs[Keys.MOUSE_RIGHT_LABEL] = config.rightLabel
            prefs[Keys.MOUSE_SWAP_LR] = config.swapLeftRight
        }
    }

    suspend fun updateGamepadMapping(mapping: GamepadButtonMapping) {
        context.controllerDataStore.edit { prefs ->
            val current = decodeGamepadMappings(prefs[Keys.GAMEPAD_MAPPINGS_JSON], ControllerSettings().gamepadMappings)
                .toMutableMap()
            current[mapping.physicalButton] = mapping
            prefs[Keys.GAMEPAD_MAPPINGS_JSON] = encodeGamepadMappings(current)
        }
    }

    suspend fun saveLastBluetoothAddress(address: String?) {
        context.controllerDataStore.edit { prefs ->
            if (address == null) prefs.remove(Keys.LAST_BT_DEVICE_ADDR)
            else prefs[Keys.LAST_BT_DEVICE_ADDR] = address
        }
    }

    /**
     * Resets general controller preferences ONLY.
     * Deliberately preserves activeLayoutId and custom keyboard layouts stored in Room.
     */
    suspend fun resetGeneralControllerSettings() {
        context.controllerDataStore.edit { prefs ->
            val preservedLayoutId = prefs[Keys.ACTIVE_LAYOUT_ID] ?: 1L
            val preservedBtAddr = prefs[Keys.LAST_BT_DEVICE_ADDR]
            prefs.clear()
            prefs[Keys.ACTIVE_LAYOUT_ID] = preservedLayoutId
            if (preservedBtAddr != null) {
                prefs[Keys.LAST_BT_DEVICE_ADDR] = preservedBtAddr
            }
        }
    }

    private fun encodeGamepadMappings(map: Map<GamepadButton, GamepadButtonMapping>): String {
        val root = JSONObject()
        map.forEach { (button, mapping) ->
            val item = JSONObject().apply {
                put("targetBit", mapping.targetHidBit)
                put("label", mapping.customLabel)
                put("tapX", mapping.localTapXPercent.toDouble())
                put("tapY", mapping.localTapYPercent.toDouble())
            }
            root.put(button.name, item)
        }
        return root.toString()
    }

    private fun decodeGamepadMappings(
        json: String?,
        defaults: Map<GamepadButton, GamepadButtonMapping>
    ): Map<GamepadButton, GamepadButtonMapping> {
        if (json.isNullOrBlank()) return defaults
        return runCatching {
            val root = JSONObject(json)
            val result = defaults.toMutableMap()
            GamepadButton.entries.forEach { btn ->
                val obj = root.optJSONObject(btn.name)
                if (obj != null) {
                    result[btn] = GamepadButtonMapping(
                        physicalButton = btn,
                        targetHidBit = obj.optInt("targetBit", btn.bitIndex),
                        customLabel = obj.optString("label", btn.label),
                        localTapXPercent = obj.optDouble("tapX", 0.5).toFloat(),
                        localTapYPercent = obj.optDouble("tapY", 0.5).toFloat()
                    )
                }
            }
            result.toMap()
        }.getOrDefault(defaults)
    }
}
