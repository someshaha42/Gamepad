package com.example.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.hid.HidInputState
import com.example.hid.HidReportDescriptors
import com.example.model.GamepadButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

enum class UsbCompatibilityStatus {
    GADGET_HID_READY,
    USB_CONNECTED_NO_GADGET_ACCESS,
    USB_HOST_PERIPHERALS_DETECTED,
    UNSUPPORTED_STANDARD_ANDROID
}

data class UsbAttachedDeviceInfo(
    val deviceName: String,
    val productName: String,
    val vendorId: Int,
    val productId: Int,
    val interfaceCount: Int,
    val hasHidInterface: Boolean
)

data class UsbHidUiState(
    val compatibility: UsbCompatibilityStatus = UsbCompatibilityStatus.UNSUPPORTED_STANDARD_ANDROID,
    val isUsbCableConnected: Boolean = false,
    val isUsbHostModeActive: Boolean = false,
    val hasUsbHostFeature: Boolean = false,
    val gadgetNodePath: String? = null,
    val attachedDevices: List<UsbAttachedDeviceInfo> = emptyList(),
    val statusTitle: String = "USB HID Compatibility Check",
    val statusDetail: String = "Inspecting USB Host and USB Gadget HID capabilities..."
)

/**
 * Inspects Android USB Host API (UsbManager, UsbDevice, UsbInterface, UsbEndpoint)
 * and USB Gadget HID peripheral capability (/dev/hidg*).
 * Never fakes a USB HID Gadget connection when the device kernel/Android build does not allow it.
 */
class UsbHidEngine(
    private val context: Context,
    private val hidState: HidInputState
) : InputEngine {

    private val usbManager: UsbManager? =
        context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val _state = MutableStateFlow(UsbHidUiState())
    val state: StateFlow<UsbHidUiState> = _state.asStateFlow()

    private val executor = Executors.newSingleThreadExecutor()
    private var gadgetStream: FileOutputStream? = null
    private var receiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED ||
                action == UsbManager.ACTION_USB_DEVICE_DETACHED ||
                action == "android.hardware.usb.action.USB_STATE"
            ) {
                val cableConnected = if (action == "android.hardware.usb.action.USB_STATE") {
                    intent.getBooleanExtra("connected", false)
                } else {
                    _state.value.isUsbCableConnected
                }
                refreshUsbCapabilities(cableConnected)
            }
        }
    }

    fun initialize() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction("android.hardware.usb.action.USB_STATE")
            }
            ContextCompat.registerReceiver(
                context,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            receiverRegistered = true
        }
        refreshUsbCapabilities(_state.value.isUsbCableConnected)
    }

    fun refreshUsbCapabilities(cableConnectedOverride: Boolean? = null) {
        val hasHostFeature = context.packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_USB_HOST
        )
        val devices = usbManager?.deviceList?.values?.map { dev ->
            var hasHid = false
            for (i in 0 until dev.interfaceCount) {
                if (dev.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_HID) {
                    hasHid = true
                    break
                }
            }
            UsbAttachedDeviceInfo(
                deviceName = dev.deviceName,
                productName = dev.productName ?: "USB Device (${dev.vendorId}:${dev.productId})",
                vendorId = dev.vendorId,
                productId = dev.productId,
                interfaceCount = dev.interfaceCount,
                hasHidInterface = hasHid
            )
        }.orEmpty()

        val stickyUsbIntent = context.registerReceiver(
            null,
            IntentFilter("android.hardware.usb.action.USB_STATE")
        )
        val cableConnected = cableConnectedOverride
            ?: stickyUsbIntent?.getBooleanExtra("connected", false)
            ?: false

        // Probe Linux ConfigFS / Gadget HID device nodes if kernel exposes them
        val candidateNodes = listOf("/dev/hidg0", "/dev/hidg1", "/dev/hidg2")
        val writableNode = candidateNodes.firstOrNull { path ->
            val f = File(path)
            f.exists() && f.canWrite()
        }

        val status: UsbCompatibilityStatus
        val title: String
        val detail: String

        if (writableNode != null) {
            status = UsbCompatibilityStatus.GADGET_HID_READY
            title = "USB HID Gadget Ready ($writableNode)"
            detail = "Kernel USB Gadget HID node $writableNode is writable. Composite HID reports can be transmitted over USB."
            openGadgetStream(writableNode)
        } else {
            closeGadgetStream()
            when {
                devices.isNotEmpty() -> {
                    status = UsbCompatibilityStatus.USB_HOST_PERIPHERALS_DETECTED
                    title = "USB Host Active (${devices.size} device detected)"
                    detail = "USB Host API detected attached peripherals, which reads external devices. Phone-to-phone USB HID output requires a writable kernel USB Gadget (/dev/hidg*), which standard Android restricts."
                }
                cableConnected -> {
                    status = UsbCompatibilityStatus.USB_CONNECTED_NO_GADGET_ACCESS
                    title = "USB Cable Connected (Gadget Restricted)"
                    detail = "USB cable detected, but standard unrooted Android does not expose /dev/hidg* USB HID Gadget write access to apps. Use Bluetooth HID or Local Controller mode."
                }
                else -> {
                    status = UsbCompatibilityStatus.UNSUPPORTED_STANDARD_ANDROID
                    title = "USB HID Peripheral Unavailable"
                    detail = "Standard Android USB Host API cannot emulate a USB HID controller to another phone without kernel USB Gadget (/dev/hidg*) support. Bluetooth HID and Local modes remain fully active."
                }
            }
        }

        _state.update {
            it.copy(
                compatibility = status,
                isUsbCableConnected = cableConnected,
                isUsbHostModeActive = devices.isNotEmpty(),
                hasUsbHostFeature = hasHostFeature,
                gadgetNodePath = writableNode,
                attachedDevices = devices,
                statusTitle = title,
                statusDetail = detail
            )
        }
    }

    private fun openGadgetStream(path: String) {
        if (gadgetStream != null) return
        runCatching {
            gadgetStream = FileOutputStream(File(path))
        }.onFailure {
            gadgetStream = null
        }
    }

    private fun closeGadgetStream() {
        runCatching { gadgetStream?.close() }
        gadgetStream = null
    }

    fun isReadyToSend(): Boolean {
        return _state.value.compatibility == UsbCompatibilityStatus.GADGET_HID_READY && gadgetStream != null
    }

    private fun writeGadgetReport(reportId: Int, payload: ByteArray, label: String): DispatchOutcome {
        val stream = gadgetStream
        if (stream == null || _state.value.compatibility != UsbCompatibilityStatus.GADGET_HID_READY) {
            return DispatchOutcome(false, "USB HID Gadget not available on this build")
        }
        val frame = ByteArray(payload.size + 1)
        frame[0] = reportId.toByte()
        System.arraycopy(payload, 0, frame, 1, payload.size)
        executor.execute {
            runCatching {
                stream.write(frame)
                stream.flush()
            }
        }
        return DispatchOutcome(true, "USB HID [ID=$reportId] $label")
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
        return writeGadgetReport(HidReportDescriptors.REPORT_ID_KEYBOARD, report, "Key 0x${hidUsageCode.toString(16)}")
    }

    override fun sendMouseMove(dx: Int, dy: Int, wheel: Int): DispatchOutcome {
        val report = hidState.buildMouseMoveReport(dx, dy, wheel)
        return writeGadgetReport(HidReportDescriptors.REPORT_ID_MOUSE, report, "Mouse dx=$dx dy=$dy")
    }

    override fun sendMouseButton(buttonBit: Int, pressed: Boolean): DispatchOutcome {
        val report = hidState.setMouseButton(buttonBit, pressed)
        return writeGadgetReport(HidReportDescriptors.REPORT_ID_MOUSE, report, "MouseBtn $buttonBit")
    }

    override fun sendGamepadButton(
        button: GamepadButton,
        hidBitIndex: Int,
        pressed: Boolean,
        localTapX: Float,
        localTapY: Float
    ): DispatchOutcome {
        val report = hidState.setGamepadButton(hidBitIndex, pressed)
        return writeGadgetReport(HidReportDescriptors.REPORT_ID_GAMEPAD, report, "Pad ${button.label}")
    }

    override fun sendGamepadDpad(up: Boolean, down: Boolean, left: Boolean, right: Boolean): DispatchOutcome {
        val report = hidState.setGamepadDpad(up, down, left, right)
        return writeGadgetReport(HidReportDescriptors.REPORT_ID_GAMEPAD, report, "D-Pad")
    }

    override fun sendGamepadLeftStick(x: Float, y: Float): DispatchOutcome {
        val report = hidState.setLeftStick(x, y)
        return writeGadgetReport(HidReportDescriptors.REPORT_ID_GAMEPAD, report, "LStick")
    }

    override fun sendGamepadRightStick(x: Float, y: Float): DispatchOutcome {
        val report = hidState.setRightStick(x, y)
        return writeGadgetReport(HidReportDescriptors.REPORT_ID_GAMEPAD, report, "RStick")
    }

    override fun sendGamepadTriggers(l2: Float, r2: Float): DispatchOutcome {
        val report = hidState.setTriggers(l2, r2)
        return writeGadgetReport(HidReportDescriptors.REPORT_ID_GAMEPAD, report, "Triggers")
    }

    override fun releaseAll() {
        val (kb, mouse, pad) = hidState.resetAll()
        if (isReadyToSend()) {
            writeGadgetReport(HidReportDescriptors.REPORT_ID_KEYBOARD, kb, "Release KB")
            writeGadgetReport(HidReportDescriptors.REPORT_ID_MOUSE, mouse, "Release Mouse")
            writeGadgetReport(HidReportDescriptors.REPORT_ID_GAMEPAD, pad, "Release Pad")
        }
    }

    fun shutdown() {
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(usbReceiver) }
            receiverRegistered = false
        }
        closeGadgetStream()
        executor.shutdown()
    }
}
