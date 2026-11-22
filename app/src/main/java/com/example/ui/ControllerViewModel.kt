package com.example.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultSounceLayouts
import com.example.engine.ConnectionManager
import com.example.engine.ConnectionSnapshot
import com.example.model.ConnectionEnvironment
import com.example.model.ControllerMode
import com.example.model.ControllerSettings
import com.example.model.GamepadButton
import com.example.model.GamepadButtonMapping
import com.example.model.KeySpec
import com.example.model.KeyboardLayoutProfile
import com.example.model.KeyboardStyle
import com.example.model.MouseButtonConfig
import com.example.model.RgbEffect
import com.example.overlay.FloatingOverlayService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ControllerViewModel(application: Application) : AndroidViewModel(application) {

    val connectionManager = ConnectionManager.getInstance(application)
    private val settingsStore = connectionManager.settingsDataStore
    private val layoutRepo = connectionManager.layoutRepository

    val settings: StateFlow<ControllerSettings> = settingsStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ControllerSettings()
        )

    val connectionSnapshot: StateFlow<ConnectionSnapshot> = connectionManager.snapshot
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = connectionManager.snapshot.value
        )

    val allLayouts: StateFlow<List<KeyboardLayoutProfile>> = layoutRepo.allLayouts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(DefaultSounceLayouts.createDefault35LayoutProfile())
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeLayout: StateFlow<KeyboardLayoutProfile> = settings
        .flatMapLatest { s -> layoutRepo.observeLayout(s.activeLayoutId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DefaultSounceLayouts.createDefault35LayoutProfile()
        )

    fun setControllerMode(mode: ControllerMode) {
        viewModelScope.launch { settingsStore.updateMode(mode) }
    }

    fun setKeyboardStyle(style: KeyboardStyle) {
        viewModelScope.launch { settingsStore.updateKeyboardStyle(style) }
    }

    fun setPreferredConnection(env: ConnectionEnvironment) {
        connectionManager.setPreferredEnvironment(env)
    }

    fun setAutoReconnect(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateAutoReconnect(enabled) }
    }

    fun updateOpacities(
        controller: Float? = null,
        keyboard: Float? = null,
        mouse: Float? = null,
        gamepad: Float? = null,
        visible: Boolean? = null
    ) {
        viewModelScope.launch {
            settingsStore.updateOpacities(controller, keyboard, mouse, gamepad, visible)
        }
    }

    fun updateSensitivities(
        mouseSensitivity: Float? = null,
        mouseSpeed: Float? = null,
        joystickSensitivity: Float? = null,
        trackpadScale: Float? = null,
        splitRatio: Float? = null
    ) {
        viewModelScope.launch {
            settingsStore.updateSensitivities(
                mouseSensitivity,
                mouseSpeed,
                joystickSensitivity,
                trackpadScale,
                splitRatio
            )
        }
    }

    fun updateRgb(effect: RgbEffect? = null, staticColorArgb: Long? = null) {
        viewModelScope.launch { settingsStore.updateRgb(effect, staticColorArgb) }
    }

    fun updateOverlaySize(widthDp: Int? = null, heightDp: Int? = null) {
        viewModelScope.launch { settingsStore.updateOverlayBounds(widthDp = widthDp, heightDp = heightDp) }
    }

    fun setCompactHalfWidth(compact: Boolean) {
        viewModelScope.launch { settingsStore.updateCompactHalfWidth(compact) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateHapticFeedback(enabled) }
    }

    fun updateMouseConfig(config: MouseButtonConfig) {
        viewModelScope.launch { settingsStore.updateMouseConfig(config) }
    }

    fun updateGamepadMapping(mapping: GamepadButtonMapping) {
        viewModelScope.launch { settingsStore.updateGamepadMapping(mapping) }
    }

    fun selectLayout(layoutId: Long) {
        viewModelScope.launch { settingsStore.updateActiveLayoutId(layoutId) }
    }

    fun addKeyToActiveLayout(spec: KeySpec) {
        viewModelScope.launch {
            layoutRepo.addKeyToLayout(settings.value.activeLayoutId, spec)
        }
    }

    fun removeCustomKey(keyId: String) {
        viewModelScope.launch {
            layoutRepo.removeCustomKey(settings.value.activeLayoutId, keyId)
        }
    }

    fun updateKeySpec(spec: KeySpec) {
        viewModelScope.launch {
            layoutRepo.updateKeySpec(settings.value.activeLayoutId, spec)
        }
    }

    fun moveKeyPosition(keyId: String, deltaColumn: Int, deltaRow: Int) {
        viewModelScope.launch {
            layoutRepo.swapOrMoveKeyPosition(settings.value.activeLayoutId, keyId, deltaColumn, deltaRow)
        }
    }

    fun updateLayoutSpacingAndScale(spacingDp: Float, scale: Float) {
        viewModelScope.launch {
            layoutRepo.updateLayoutSpacingAndScale(settings.value.activeLayoutId, spacingDp, scale)
        }
    }

    fun saveAsNewGameLayout(name: String, gameTitle: String) {
        viewModelScope.launch {
            val newId = layoutRepo.saveAsNewGameLayout(settings.value.activeLayoutId, name, gameTitle)
            settingsStore.updateActiveLayoutId(newId)
        }
    }

    fun deleteCustomLayout(layoutId: Long) {
        viewModelScope.launch {
            layoutRepo.deleteLayout(layoutId)
            if (settings.value.activeLayoutId == layoutId) {
                settingsStore.updateActiveLayoutId(1L)
            }
        }
    }

    fun restoreDefault35KeyLayout() {
        viewModelScope.launch {
            layoutRepo.restoreDefault35KeyLayout()
            settingsStore.updateActiveLayoutId(1L)
        }
    }

    fun resetGeneralControllerSettings() {
        viewModelScope.launch {
            settingsStore.resetGeneralControllerSettings()
        }
    }

    // Connection & Overlay actions
    fun refreshConnections() = connectionManager.refreshAllCapabilities()
    fun startBtDiscovery() = connectionManager.bluetoothEngine.startDiscovery()
    fun stopBtDiscovery() = connectionManager.bluetoothEngine.stopDiscovery()
    fun connectBtDevice(device: BluetoothDevice) = connectionManager.bluetoothEngine.connectToDevice(device)
    fun disconnectBtDevice() = connectionManager.bluetoothEngine.disconnect()

    fun startFloatingOverlay(localMode: Boolean = true) {
        FloatingOverlayService.startOverlay(getApplication(), localMode)
    }

    fun stopFloatingOverlay() {
        FloatingOverlayService.sendAction(getApplication(), FloatingOverlayService.ACTION_STOP_SERVICE)
    }

    fun toggleControllerVisibility(visible: Boolean) {
        updateOpacities(visible = visible)
    }
}
