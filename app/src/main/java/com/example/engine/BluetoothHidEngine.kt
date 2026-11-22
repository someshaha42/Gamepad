package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.hid.HidInputState
import com.example.hid.HidReportDescriptors
import com.example.model.GamepadButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors

enum class BtConnectionPhase {
    UNSUPPORTED_API,
    NO_BLUETOOTH_HARDWARE,
    PERMISSION_REQUIRED,
    BLUETOOTH_DISABLED,
    REGISTERING_HID,
    READY_DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class BtDeviceItem(
    val name: String,
    val address: String,
    val isBonded: Boolean,
    val device: BluetoothDevice
)

data class BluetoothHidUiState(
    val phase: BtConnectionPhase = BtConnectionPhase.READY_DISCONNECTED,
    val isAppRegistered: Boolean = false,
    val connectedHostName: String? = null,
    val connectedHostAddress: String? = null,
    val pairedDevices: List<BtDeviceItem> = emptyList(),
    val discoveredDevices: List<BtDeviceItem> = emptyList(),
    val isScanning: Boolean = false,
    val statusMessage: String = "Initializing Bluetooth HID profile..."
)

class BluetoothHidEngine(
    private val context: Context,
    private val hidState: HidInputState,
    private val onLastConnectedAddress: (String) -> Unit
) : InputEngine {

    private val _state = MutableStateFlow(BluetoothHidUiState())
    val state: StateFlow<BluetoothHidUiState> = _state.asStateFlow()

    private val btManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val btAdapter: BluetoothAdapter? = btManager?.adapter

    private val executor = Executors.newSingleThreadExecutor()
    private var hidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null
    private var receiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    if (device != null && hasRequiredPermissions()) {
                        val name = runCatching { device.name }.getOrNull().orEmpty().ifBlank { "Bluetooth Device" }
                        val item = BtDeviceItem(
                            name = name,
                            address = device.address,
                            isBonded = device.bondState == BluetoothDevice.BOND_BONDED,
                            device = device
                        )
                        _state.update { current ->
                            val updated = (current.discoveredDevices.filterNot { it.address == item.address } + item)
                            current.copy(discoveredDevices = updated)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _state.update { it.copy(isScanning = false) }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshPairedDevices()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    evaluateAndInitialize()
                }
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                hidDevice = proxy as? BluetoothHidDevice
                registerHidApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                hostDevice = null
                _state.update {
                    it.copy(
                        isAppRegistered = false,
                        connectedHostName = null,
                        connectedHostAddress = null,
                        phase = BtConnectionPhase.READY_DISCONNECTED,
                        statusMessage = "Bluetooth HID service disconnected."
                    )
                }
            }
        }
    }

    private val hidCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                _state.update {
                    it.copy(
                        isAppRegistered = registered,
                        phase = if (registered) {
                            if (hostDevice != null) BtConnectionPhase.CONNECTED else BtConnectionPhase.READY_DISCONNECTED
                        } else {
                            BtConnectionPhase.ERROR
                        },
                        statusMessage = if (registered) {
                            "HID Peripheral registered. Pair or tap a device to connect."
                        } else {
                            "Bluetooth HID registration failed on this device stack."
                        }
                    )
                }
            }

            @SuppressLint("MissingPermission")
            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        hostDevice = device
                        val name = if (hasRequiredPermissions()) {
                            runCatching { device?.name }.getOrNull() ?: device?.address ?: "Host"
                        } else {
                            device?.address ?: "Host"
                        }
                        device?.address?.let { onLastConnectedAddress(it) }
                        _state.update {
                            it.copy(
                                phase = BtConnectionPhase.CONNECTED,
                                connectedHostName = name,
                                connectedHostAddress = device?.address,
                                statusMessage = "Connected as HID Controller to $name"
                            )
                        }
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        _state.update {
                            it.copy(
                                phase = BtConnectionPhase.CONNECTING,
                                statusMessage = "Connecting to ${device?.address ?: "host"}..."
                            )
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (hostDevice?.address == device?.address || hostDevice == null) {
                            hostDevice = null
                            _state.update {
                                it.copy(
                                    phase = BtConnectionPhase.READY_DISCONNECTED,
                                    connectedHostName = null,
                                    connectedHostAddress = null,
                                    statusMessage = "Disconnected from host. Ready to connect."
                                )
                            }
                        }
                    }
                }
            }
        }
    } else null

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun evaluateAndInitialize() {
        registerReceiverIfNeeded()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            _state.update {
                it.copy(
                    phase = BtConnectionPhase.UNSUPPORTED_API,
                    statusMessage = "Bluetooth HID Device requires Android 9 (API 28) or higher."
                )
            }
            return
        }
        val adapter = btAdapter
        if (adapter == null) {
            _state.update {
                it.copy(
                    phase = BtConnectionPhase.NO_BLUETOOTH_HARDWARE,
                    statusMessage = "No Bluetooth hardware detected on this device."
                )
            }
            return
        }
        if (!hasRequiredPermissions()) {
            _state.update {
                it.copy(
                    phase = BtConnectionPhase.PERMISSION_REQUIRED,
                    statusMessage = "Grant Bluetooth permissions to discover and emulate HID hardware."
                )
            }
            return
        }
        if (!adapter.isEnabled) {
            _state.update {
                it.copy(
                    phase = BtConnectionPhase.BLUETOOTH_DISABLED,
                    statusMessage = "Bluetooth is turned off. Enable Bluetooth to use HID Controller."
                )
            }
            return
        }

        refreshPairedDevices()

        if (hidDevice == null) {
            _state.update {
                it.copy(
                    phase = BtConnectionPhase.REGISTERING_HID,
                    statusMessage = "Binding Bluetooth HID Device profile..."
                )
            }
            adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        } else if (!_state.value.isAppRegistered) {
            registerHidApp()
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val hid = hidDevice ?: return
        val cb = hidCallback ?: return
        if (!hasRequiredPermissions()) return

        val sdp = BluetoothHidDeviceAppSdpSettings(
            "Sounce Gaming Controller",
            "Sounce RGB Keyboard, Mouse & Gamepad HID",
            "Sounce",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidReportDescriptors.COMPOSITE_CONTROLLER_DESCRIPTOR
        )
        val qosOut = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )
        val ok = runCatching {
            hid.registerApp(sdp, null, qosOut, executor, cb)
        }.getOrDefault(false)

        if (!ok) {
            _state.update {
                it.copy(
                    phase = BtConnectionPhase.ERROR,
                    statusMessage = "System rejected Bluetooth HID app registration. Some OEM stacks restrict HID profile."
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val adapter = btAdapter ?: return
        if (!hasRequiredPermissions() || !adapter.isEnabled) return
        val bonded = runCatching { adapter.bondedDevices }.getOrNull().orEmpty()
        val items = bonded.map { dev ->
            BtDeviceItem(
                name = runCatching { dev.name }.getOrNull().orEmpty().ifBlank { dev.address },
                address = dev.address,
                isBonded = true,
                device = dev
            )
        }.sortedBy { it.name.lowercase() }
        _state.update { it.copy(pairedDevices = items) }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val adapter = btAdapter ?: return
        if (!hasRequiredPermissions() || !adapter.isEnabled) return
        registerReceiverIfNeeded()
        runCatching {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            _state.update { it.copy(discoveredDevices = emptyList(), isScanning = true) }
            adapter.startDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val adapter = btAdapter ?: return
        if (!hasRequiredPermissions()) return
        runCatching {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
        }
        _state.update { it.copy(isScanning = false) }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        if (!hasRequiredPermissions()) return
        val hid = hidDevice ?: run {
            evaluateAndInitialize()
            return
        }
        stopDiscovery()
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            runCatching { device.createBond() }
            _state.update {
                it.copy(
                    phase = BtConnectionPhase.CONNECTING,
                    statusMessage = "Pairing with ${device.address}... Accept pairing prompt on target device."
                )
            }
            return
        }
        _state.update {
            it.copy(
                phase = BtConnectionPhase.CONNECTING,
                statusMessage = "Connecting HID to ${runCatching { device.name }.getOrNull() ?: device.address}..."
            )
        }
        runCatching { hid.connect(device) }
    }

    @SuppressLint("MissingPermission")
    fun reconnectLast(address: String) {
        val adapter = btAdapter ?: return
        if (!hasRequiredPermissions() || !adapter.isEnabled) return
        val dev = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return
        if (dev.bondState == BluetoothDevice.BOND_BONDED) {
            connectToDevice(dev)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val host = hostDevice ?: return
        if (!hasRequiredPermissions()) return
        runCatching { hidDevice?.disconnect(host) }
    }

    private fun sendHidReport(reportId: Int, payload: ByteArray, label: String): DispatchOutcome {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return DispatchOutcome(false, "BT HID unsupported < API 28")
        }
        val hid = hidDevice
        val host = hostDevice
        if (hid == null || host == null || !_state.value.isAppRegistered) {
            return DispatchOutcome(false, "BT HID not connected")
        }
        executor.execute {
            runCatching {
                if (hasRequiredPermissions()) {
                    hid.sendReport(host, reportId, payload)
                }
            }
        }
        return DispatchOutcome(true, "BT HID [ID=$reportId] $label")
    }

    override fun sendKey(
        hidUsageCode: Int,
        isModifier: Boolean,
        modifierMask: Int,
        pressed: Boolean,
        localTapX: Float,
        localTapY: Float
    ): DispatchOutcome {
        val report = hidState.setKeyboardKey(hidUsageCode, isModifier, modifierMask, pressed)
        return sendHidReport(
            HidReportDescriptors.REPORT_ID_KEYBOARD,
            report,
            "Key 0x${hidUsageCode.toString(16)} ${if (pressed) "DOWN" else "UP"}"
        )
    }

    override fun sendMouseMove(dx: Int, dy: Int, wheel: Int): DispatchOutcome {
        if (dx == 0 && dy == 0 && wheel == 0) return DispatchOutcome(false, "Zero delta")
        val report = hidState.buildMouseMoveReport(dx, dy, wheel)
        return sendHidReport(
            HidReportDescriptors.REPORT_ID_MOUSE,
            report,
            "Mouse dx=$dx dy=$dy w=$wheel"
        )
    }

    override fun sendMouseButton(buttonBit: Int, pressed: Boolean): DispatchOutcome {
        val report = hidState.setMouseButton(buttonBit, pressed)
        return sendHidReport(
            HidReportDescriptors.REPORT_ID_MOUSE,
            report,
            "MouseBtn $buttonBit ${if (pressed) "DOWN" else "UP"}"
        )
    }

    override fun sendGamepadButton(
        button: GamepadButton,
        hidBitIndex: Int,
        pressed: Boolean,
        localTapX: Float,
        localTapY: Float
    ): DispatchOutcome {
        val report = hidState.setGamepadButton(hidBitIndex, pressed)
        return sendHidReport(
            HidReportDescriptors.REPORT_ID_GAMEPAD,
            report,
            "Pad ${button.label} ${if (pressed) "DOWN" else "UP"}"
        )
    }

    override fun sendGamepadDpad(up: Boolean, down: Boolean, left: Boolean, right: Boolean): DispatchOutcome {
        val report = hidState.setGamepadDpad(up, down, left, right)
        return sendHidReport(HidReportDescriptors.REPORT_ID_GAMEPAD, report, "D-Pad")
    }

    override fun sendGamepadLeftStick(x: Float, y: Float): DispatchOutcome {
        val report = hidState.setLeftStick(x, y)
        return sendHidReport(HidReportDescriptors.REPORT_ID_GAMEPAD, report, "LStick (${"%.2f".format(x)}, ${"%.2f".format(y)})")
    }

    override fun sendGamepadRightStick(x: Float, y: Float): DispatchOutcome {
        val report = hidState.setRightStick(x, y)
        return sendHidReport(HidReportDescriptors.REPORT_ID_GAMEPAD, report, "RStick (${"%.2f".format(x)}, ${"%.2f".format(y)})")
    }

    override fun sendGamepadTriggers(l2: Float, r2: Float): DispatchOutcome {
        val report = hidState.setTriggers(l2, r2)
        return sendHidReport(HidReportDescriptors.REPORT_ID_GAMEPAD, report, "Triggers L2=${"%.2f".format(l2)} R2=${"%.2f".format(r2)}")
    }

    override fun releaseAll() {
        val (kb, mouse, pad) = hidState.resetAll()
        if (hostDevice != null && hidDevice != null) {
            sendHidReport(HidReportDescriptors.REPORT_ID_KEYBOARD, kb, "Release KB")
            sendHidReport(HidReportDescriptors.REPORT_ID_MOUSE, mouse, "Release Mouse")
            sendHidReport(HidReportDescriptors.REPORT_ID_GAMEPAD, pad, "Release Pad")
        }
    }

    @SuppressLint("MissingPermission")
    fun shutdown() {
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(discoveryReceiver) }
            receiverRegistered = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                if (hasRequiredPermissions()) {
                    hidDevice?.unregisterApp()
                    btAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
                }
            }
        }
        hidDevice = null
        hostDevice = null
        executor.shutdown()
    }

    private fun registerReceiverIfNeeded() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            discoveryReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }
}
