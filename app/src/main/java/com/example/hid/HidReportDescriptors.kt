package com.example.hid

/**
 * Standard USB/Bluetooth HID Report Descriptors.
 * Uses distinct Report IDs in a composite descriptor so Keyboard, Mouse,
 * Keyboard+Mouse combo, and Gamepad work seamlessly on host devices.
 */
object HidReportDescriptors {

    const val REPORT_ID_KEYBOARD: Int = 1
    const val REPORT_ID_MOUSE: Int = 2
    const val REPORT_ID_GAMEPAD: Int = 3

    val COMPOSITE_CONTROLLER_DESCRIPTOR: ByteArray = byteArrayOf(
        // ================= REPORT ID 1: KEYBOARD =================
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x01.toByte(), //   Report ID (1)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(), //   Usage Minimum (Left Control)
        0x29.toByte(), 0xE7.toByte(), //   Usage Maximum (Right GUI)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x08.toByte(), //   Report Count (8)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Var, Abs) - Modifier byte
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x81.toByte(), 0x01.toByte(), //   Input (Const, Array, Abs) - Reserved byte
        0x95.toByte(), 0x06.toByte(), //   Report Count (6)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(), //   Logical Maximum (101)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(), //   Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(), //   Usage Maximum (101)
        0x81.toByte(), 0x00.toByte(), //   Input (Data, Array, Abs) - 6 key array
        0xC0.toByte(),                // End Collection

        // ================= REPORT ID 2: MOUSE ====================
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x02.toByte(), //   Report ID (2)
        0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
        0x05.toByte(), 0x09.toByte(), //     Usage Page (Button)
        0x19.toByte(), 0x01.toByte(), //     Usage Minimum (Button 1)
        0x29.toByte(), 0x03.toByte(), //     Usage Maximum (Button 3)
        0x15.toByte(), 0x00.toByte(), //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //     Logical Maximum (1)
        0x95.toByte(), 0x03.toByte(), //     Report Count (3)
        0x75.toByte(), 0x01.toByte(), //     Report Size (1)
        0x81.toByte(), 0x02.toByte(), //     Input (Data, Var, Abs) - L/R/M buttons
        0x95.toByte(), 0x01.toByte(), //     Report Count (1)
        0x75.toByte(), 0x05.toByte(), //     Report Size (5)
        0x81.toByte(), 0x01.toByte(), //     Input (Const, Array, Abs) - Padding
        0x05.toByte(), 0x01.toByte(), //     Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //     Usage (X)
        0x09.toByte(), 0x31.toByte(), //     Usage (Y)
        0x09.toByte(), 0x38.toByte(), //     Usage (Wheel)
        0x15.toByte(), 0x81.toByte(), //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(), //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(), //     Report Size (8)
        0x95.toByte(), 0x03.toByte(), //     Report Count (3)
        0x81.toByte(), 0x06.toByte(), //     Input (Data, Var, Rel) - dX, dY, Wheel
        0xC0.toByte(),                //   End Collection (Physical)
        0xC0.toByte(),                // End Collection (Application)

        // ================= REPORT ID 3: GAMEPAD ==================
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05.toByte(), // Usage (Game Pad)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x03.toByte(), //   Report ID (3)
        0x05.toByte(), 0x09.toByte(), //   Usage Page (Button)
        0x19.toByte(), 0x01.toByte(), //   Usage Minimum (Button 1)
        0x29.toByte(), 0x10.toByte(), //   Usage Maximum (Button 16)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x10.toByte(), //   Report Count (16)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Var, Abs) - 2 bytes buttons
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x39.toByte(), //   Usage (Hat switch)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x07.toByte(), //   Logical Maximum (7)
        0x35.toByte(), 0x00.toByte(), //   Physical Minimum (0)
        0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), // Physical Maximum (315)
        0x65.toByte(), 0x14.toByte(), //   Unit (Degrees)
        0x75.toByte(), 0x04.toByte(), //   Report Size (4)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x42.toByte(), //   Input (Data, Var, Abs, Null) - Hat 4 bits
        0x65.toByte(), 0x00.toByte(), //   Unit (None)
        0x75.toByte(), 0x04.toByte(), //   Report Size (4)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x01.toByte(), //   Input (Const) - Padding 4 bits
        0x09.toByte(), 0x30.toByte(), //   Usage (X) - Left Stick X
        0x09.toByte(), 0x31.toByte(), //   Usage (Y) - Left Stick Y
        0x09.toByte(), 0x32.toByte(), //   Usage (Z) - Right Stick X
        0x09.toByte(), 0x35.toByte(), //   Usage (Rz) - Right Stick Y
        0x15.toByte(), 0x81.toByte(), //   Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(), //   Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x95.toByte(), 0x04.toByte(), //   Report Count (4)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Var, Abs) - 4 stick axes
        0x05.toByte(), 0x02.toByte(), //   Usage Page (Simulation Controls)
        0x09.toByte(), 0xC5.toByte(), //   Usage (Brake / L2)
        0x09.toByte(), 0xC4.toByte(), //   Usage (Accelerator / R2)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x95.toByte(), 0x02.toByte(), //   Report Count (2)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Var, Abs) - 2 analog triggers
        0xC0.toByte()                 // End Collection
    )
}
