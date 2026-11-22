package com.example.engine

import android.content.Context
import com.example.data.LayoutRepository
import com.example.data.SettingsDataStore
import com.example.data.SounceDatabase
import com.example.hid.HidInputState
import com.example.model.ActiveEngineType
import com.example.model.ConnectionEnvironment
import com.example.model.GamepadButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectionSnapshot(
    val activeEngine: ActiveEngineType = ActiveEngineType.NONE,
    val preferredEnvironment: ConnectionEnvironment = ConnectionEnvironment.AUTO,
    val isReadyToTransmit: Boolean = false,
    val statusHeadline: String = "Standby",
    val statusSubtext: String = "Select Bluetooth HID, USB check, or Start Local Controller",
    val btState: BluetoothHidUiState = BluetoothHidUiState(),
    val usbState: UsbHidUiState = UsbHidUiState(),
    val localCaps: LocalControllerCapabilities = LocalControllerCapabilities(),
    val isOverlayServiceRunning: Boolean = false,
    val recentPackets: List<String> = emptyList()
)

/**
 * Automatic ConnectionManager coordinating Bluetooth HID, USB HID compatibility,
 * and Local Floating Overlay engines without conflicting active inputs.
 */
class ConnectionManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settingsDataStore = SettingsDataStore(appContext)
    private val database = SounceDatabase.getInstance(appContext)
    val layoutRepository = LayoutRepository(database.layoutDao())

    private val hidState = HidInputState()

    val bluetoothEngine = BluetoothHidEngine(appContext, hidState) { connectedAddr ->
        scope.launch { settingsDataStore.saveLastBluetoothAddress(connectedAddr) }
    }
    val usbEngine = UsbHidEngine(appContext, hidState)
    val localEngine = LocalInputEngine(appContext)

    private val _preferredEnv = MutableStateFlow(ConnectionEnvironment.AUTO)
    private val _overlayRunning = MutableStateFlow(false)
    private val _localModeExplicit = MutableStateFlow(false)
    private val _recentPackets = MutableStateFlow<List<String>>(emptyList())

    private val _snapshot = MutableStateFlow(ConnectionSnapshot())
    val snapshot: StateFlow<ConnectionSnapshot> = _snapshot.asStateFlow()

    private var autoReconnectAttempted = false

    init {
        scope.launch {
            layoutRepository.ensureDefaultLayoutSeeded()
        }
        scope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                _preferredEnv.value = settings.preferredConnection
                if (settings.autoReconnect && !autoReconnectAttempted &&
                    settings.preferredConnection != ConnectionEnvironment.LOCAL_OVERLAY
                ) {
                    autoReconnectAttempted = true
                    val lastAddr = settingsDataStore.lastBluetoothAddressFlow.first()
                    if (!lastAddr.isNullOrBlank()) {
                        bluetoothEngine.reconnectLast(lastAddr)
                    }
                }
            }
        }
        bluetoothEngine.evaluateAndInitialize()
        usbEngine.initialize()

        scope.launch {
            combine(
                _preferredEnv,
                bluetoothEngine.state,
                usbEngine.state,
                SounceAccessibilityService.isConnected,
                _overlayRunning,
                _localModeExplicit,
                _recentPackets
            ) { values ->
                val pref = values[0] as ConnectionEnvironment
                val bt = values[1] as BluetoothHidUiState
                val usb = values[2] as UsbHidUiState
                val a11yConnected = values[3] as Boolean
                val overlayRunning = values[4] as Boolean
                val localExplicit = values[5] as Boolean
                @Suppress("UNCHECKED_CAST")
                val packets = values[6] as List<String>

                val localCaps = localEngine.getCapabilities().copy(hasAccessibilityEnabled = a11yConnected)

                val resolvedEngine = resolveEngine(pref, bt, usb, overlayRunning, localExplicit, a11yConnected)
                val (ready, headline, subtext) = describeState(resolvedEngine, bt, usb, localCaps, overlayRunning)

                ConnectionSnapshot(
                    activeEngine = resolvedEngine,
                    preferredEnvironment = pref,
                    isReadyToTransmit = ready,
                    statusHeadline = headline,
                    statusSubtext = subtext,
                    btState = bt,
                    usbState = usb,
                    localCaps = localCaps,
                    isOverlayServiceRunning = overlayRunning,
                    recentPackets = packets
                )
            }.collect { _snapshot.value = it }
        }
    }

    private fun resolveEngine(
        pref: ConnectionEnvironment,
        bt: BluetoothHidUiState,
        usb: UsbHidUiState,
        overlayRunning: Boolean,
        localExplicit: Boolean,
        a11yConnected: Boolean
    ): ActiveEngineType {
        return when (pref) {
            ConnectionEnvironment.BLUETOOTH_HID -> ActiveEngineType.BLUETOOTH_HID
            ConnectionEnvironment.USB_HID -> ActiveEngineType.USB_HID_GADGET
            ConnectionEnvironment.LOCAL_OVERLAY -> ActiveEngineType.LOCAL_ACCESSIBILITY
            ConnectionEnvironment.AUTO -> {
                // Respect active remote links first unless user explicitly started Local Controller
                when {
                    localExplicit && overlayRunning -> ActiveEngineType.LOCAL_ACCESSIBILITY
                    usb.compatibility == UsbCompatibilityStatus.GADGET_HID_READY -> ActiveEngineType.USB_HID_GADGET
                    bt.phase == BtConnectionPhase.CONNECTED -> ActiveEngineType.BLUETOOTH_HID
                    overlayRunning && a11yConnected -> ActiveEngineType.LOCAL_ACCESSIBILITY
                    bt.isAppRegistered -> ActiveEngineType.BLUETOOTH_HID
                    else -> ActiveEngineType.NONE
                }
            }
        }
    }

    private fun describeState(
        engine: ActiveEngineType,
        bt: BluetoothHidUiState,
        usb: UsbHidUiState,
        localCaps: LocalControllerCapabilities,
        overlayRunning: Boolean
    ): Triple<Boolean, String, String> {
        return when (engine) {
            ActiveEngineType.BLUETOOTH_HID -> {
                val ready = bt.phase == BtConnectionPhase.CONNECTED
                Triple(
                    ready,
                    if (ready) "BT HID: ${bt.connectedHostName ?: "Connected"}" else "BT HID: ${bt.phase.name.replace('_', ' ')}",
                    bt.statusMessage
                )
            }
            ActiveEngineType.USB_HID_GADGET -> {
                val ready = usb.compatibility == UsbCompatibilityStatus.GADGET_HID_READY
                Triple(ready, usb.statusTitle, usb.statusDetail)
            }
            ActiveEngineType.LOCAL_ACCESSIBILITY -> {
                val ready = overlayRunning && localCaps.hasAccessibilityEnabled
                val title = when {
                    ready -> "Local Controller Active"
                    overlayRunning -> "Overlay Running (Enable Touch Bridge)"
                    else -> "Local Controller Standby"
                }
                val detail = when {
                    ready -> "Floating overlay dispatching touch gestures via AccessibilityService."
                    overlayRunning -> "Overlay visible. Enable Sounce Local Touch Bridge in Accessibility Settings to inject taps."
                    else -> "Tap START LOCAL CONTROLLER to launch the floating game overlay."
                }
                Triple(ready, title, detail)
            }
            ActiveEngineType.NONE -> Triple(
                false,
                "Controller Standby",
                "Pair via Bluetooth HID, check USB compatibility, or Start Local Controller."
            )
        }
    }

    fun setPreferredEnvironment(env: ConnectionEnvironment) {
        scope.launch {
            activeInputEngine()?.releaseAll()
            _localModeExplicit.value = (env == ConnectionEnvironment.LOCAL_OVERLAY)
            settingsDataStore.updatePreferredConnection(env)
        }
    }

    fun setOverlayServiceRunning(running: Boolean, localExplicit: Boolean = running) {
        _overlayRunning.value = running
        _localModeExplicit.value = localExplicit
    }

    fun refreshAllCapabilities() {
        bluetoothEngine.evaluateAndInitialize()
        usbEngine.refreshUsbCapabilities()
    }

    private fun activeInputEngine(): InputEngine? {
        return when (_snapshot.value.activeEngine) {
            ActiveEngineType.BLUETOOTH_HID -> bluetoothEngine
            ActiveEngineType.USB_HID_GADGET -> usbEngine
            ActiveEngineType.LOCAL_ACCESSIBILITY -> localEngine
            ActiveEngineType.NONE -> null
        }
    }

    private fun recordOutcome(outcome: DispatchOutcome, fallbackLabel: String) {
        val line = if (outcome.dispatched) outcome.summary else "[Preview] $fallbackLabel (${outcome.summary})"
        _recentPackets.update { current ->
            (listOf(line) + current).take(8)
        }
    }

    fun dispatchKey(
        hidUsageCode: Int,
        label: String,
        isModifier: Boolean,
        modifierMask: Int,
        pressed: Boolean,
        localTapX: Float = 0.5f,
        localTapY: Float = 0.5f
    ) {
        val engine = activeInputEngine()
        if (engine == null) {
            if (pressed) recordOutcome(DispatchOutcome(false, "Standby"), "Key $label")
            return
        }
        val res = engine.sendKey(hidUsageCode, isModifier, modifierMask, pressed, localTapX, localTapY)
        if (pressed || res.dispatched) recordOutcome(res, "Key $label")
    }

    fun dispatchMouseMove(dx: Int, dy: Int, wheel: Int = 0) {
        val engine = activeInputEngine() ?: return
        val res = engine.sendMouseMove(dx, dy, wheel)
        if (res.dispatched && wheel != 0) recordOutcome(res, "Scroll")
    }

    fun dispatchMouseButton(buttonBit: Int, pressed: Boolean) {
        val engine = activeInputEngine()
        if (engine == null) {
            if (pressed) recordOutcome(DispatchOutcome(false, "Standby"), "Mouse #$buttonBit")
            return
        }
        val res = engine.sendMouseButton(buttonBit, pressed)
        if (pressed) recordOutcome(res, "Mouse #$buttonBit")
    }

    fun dispatchGamepadButton(
        button: GamepadButton,
        hidBitIndex: Int,
        pressed: Boolean,
        localTapX: Float = 0.5f,
        localTapY: Float = 0.5f
    ) {
        val engine = activeInputEngine()
        if (engine == null) {
            if (pressed) recordOutcome(DispatchOutcome(false, "Standby"), button.label)
            return
        }
        val res = engine.sendGamepadButton(button, hidBitIndex, pressed, localTapX, localTapY)
        if (pressed) recordOutcome(res, button.label)
    }

    fun dispatchGamepadDpad(up: Boolean, down: Boolean, left: Boolean, right: Boolean) {
        val engine = activeInputEngine() ?: return
        engine.sendGamepadDpad(up, down, left, right)
    }

    fun dispatchLeftStick(x: Float, y: Float) {
        activeInputEngine()?.sendGamepadLeftStick(x, y)
    }

    fun dispatchRightStick(x: Float, y: Float) {
        activeInputEngine()?.sendGamepadRightStick(x, y)
    }

    fun dispatchTriggers(l2: Float, r2: Float) {
        activeInputEngine()?.sendGamepadTriggers(l2, r2)
    }

    fun releaseAllInputs() {
        bluetoothEngine.releaseAll()
        usbEngine.releaseAll()
        localEngine.releaseAll()
    }

    companion object {
        @Volatile
        private var INSTANCE: ConnectionManager? = null

        fun getInstance(context: Context): ConnectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
