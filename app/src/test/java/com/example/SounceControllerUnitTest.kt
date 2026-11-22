package com.example

import com.example.data.DefaultSounceLayouts
import com.example.hid.HidInputState
import com.example.hid.HidReportDescriptors
import com.example.model.GamepadButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SounceControllerUnitTest {

    @Test
    fun defaultSounce35KeyLayoutMatchesExactSpecification() {
        val keys = DefaultSounceLayouts.createSounce35Keys()
        assertEquals(35, keys.size)

        val rows = keys.groupBy { it.row }.toSortedMap()
        assertEquals(6, rows.size)

        assertEquals(listOf("Esc", "F1", "F2", "F3", "F4", "F5"), rows[0]!!.map { it.label })
        assertEquals(listOf("~", "1!", "2@", "3#", "4₹", "5%", "6^"), rows[1]!!.map { it.label })
        assertEquals(listOf("Tab", "Q", "W", "E", "R", "T"), rows[2]!!.map { it.label })
        assertEquals(listOf("Caps Lock", "A", "S", "D", "F", "G"), rows[3]!!.map { it.label })
        assertEquals(listOf("Shift", "Z", "X", "C", "V", "B"), rows[4]!!.map { it.label })
        assertEquals(listOf("Ctrl", "FN", "Alt", "Spacebar"), rows[5]!!.map { it.label })
    }

    @Test
    fun hidDescriptorContainsAllThreeReportIds() {
        val desc = HidReportDescriptors.COMPOSITE_CONTROLLER_DESCRIPTOR
        assertTrue(desc.isNotEmpty())
        val asList = desc.toList()
        // Report ID tag is 0x85 followed by 1, 2, or 3
        val reportIds = mutableSetOf<Int>()
        for (i in 0 until asList.size - 1) {
            if (asList[i] == 0x85.toByte()) {
                reportIds.add(asList[i + 1].toInt())
            }
        }
        assertEquals(setOf(1, 2, 3), reportIds)
    }

    @Test
    fun hidInputStateBuildsValidKeyboardMouseGamepadReports() {
        val state = HidInputState()

        // Press Left Shift + W (0x1A)
        state.setKeyboardKey(0xE1, isModifier = true, modBit = DefaultSounceLayouts.MOD_LEFT_SHIFT, pressed = true)
        val kbReport = state.setKeyboardKey(0x1A, isModifier = false, modBit = 0, pressed = true)
        assertEquals(8, kbReport.size)
        assertEquals(0x02.toByte(), kbReport[0])
        assertEquals(0x1A.toByte(), kbReport[2])

        // Mouse click + move
        state.setMouseButton(1, true)
        val mouseReport = state.buildMouseMoveReport(15, -10, 1)
        assertEquals(4, mouseReport.size)
        assertEquals(1.toByte(), mouseReport[0])
        assertEquals(15.toByte(), mouseReport[1])
        assertEquals((-10).toByte(), mouseReport[2])
        assertEquals(1.toByte(), mouseReport[3])

        // Gamepad A button + Left stick full right
        state.setGamepadButton(GamepadButton.A.bitIndex, true)
        val padReport = state.setLeftStick(1f, 0f)
        assertEquals(9, padReport.size)
        assertEquals(1.toByte(), padReport[0])
        assertEquals(127.toByte(), padReport[3])
    }
}
