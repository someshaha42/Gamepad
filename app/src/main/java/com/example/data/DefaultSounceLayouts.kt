package com.example.data

import com.example.model.KeySpec
import com.example.model.KeyboardLayoutProfile

/**
 * Exact default layouts for Sounce Gaming Controller:
 * 1) Sounce One-Handed RGB Gaming Keyboard (35 Keys)
 * 2) Complete QWERTY Normal Keyboard
 * 3) Extra Key Catalog for the "+ ADD KEY" customization panel
 */
object DefaultSounceLayouts {

    const val GAMING_35_KEYBOARD_NAME = "Sounce One-Handed RGB Gaming Keyboard (35 Keys)"

    // HID Modifier bits (Usage Page 0x07, byte 0)
    const val MOD_LEFT_CTRL = 0x01
    const val MOD_LEFT_SHIFT = 0x02
    const val MOD_LEFT_ALT = 0x04
    const val MOD_LEFT_GUI = 0x08

    /**
     * Exact 35-Key Default Structure:
     * Row 1 (6): Esc, F1, F2, F3, F4, F5
     * Row 2 (7): ~, 1!, 2@, 3#, 4₹, 5%, 6^
     * Row 3 (6): Tab, Q, W, E, R, T
     * Row 4 (6): Caps Lock, A, S, D, F, G
     * Row 5 (6): Shift, Z, X, C, V, B
     * Row 6 (4): Ctrl, FN, Alt, Spacebar
     * Total = 35 keys.
     */
    fun createSounce35Keys(): List<KeySpec> = listOf(
        // Row 1: Function Keys (6 Keys)
        KeySpec("g_esc", "Esc", 0x29, row = 0, column = 0, localTapXPercent = 0.08f, localTapYPercent = 0.15f),
        KeySpec("g_f1", "F1", 0x3A, row = 0, column = 1, localTapXPercent = 0.20f, localTapYPercent = 0.15f),
        KeySpec("g_f2", "F2", 0x3B, row = 0, column = 2, localTapXPercent = 0.32f, localTapYPercent = 0.15f),
        KeySpec("g_f3", "F3", 0x3C, row = 0, column = 3, localTapXPercent = 0.44f, localTapYPercent = 0.15f),
        KeySpec("g_f4", "F4", 0x3D, row = 0, column = 4, localTapXPercent = 0.56f, localTapYPercent = 0.15f),
        KeySpec("g_f5", "F5", 0x3E, row = 0, column = 5, localTapXPercent = 0.68f, localTapYPercent = 0.15f),

        // Row 2: Number Keys (7 Keys)
        KeySpec("g_tilde", "~", 0x35, row = 1, column = 0, localTapXPercent = 0.12f, localTapYPercent = 0.28f),
        KeySpec("g_1", "1!", 0x1E, row = 1, column = 1, localTapXPercent = 0.25f, localTapYPercent = 0.28f),
        KeySpec("g_2", "2@", 0x1F, row = 1, column = 2, localTapXPercent = 0.38f, localTapYPercent = 0.28f),
        KeySpec("g_3", "3#", 0x20, row = 1, column = 3, localTapXPercent = 0.51f, localTapYPercent = 0.28f),
        KeySpec("g_4", "4₹", 0x21, row = 1, column = 4, localTapXPercent = 0.64f, localTapYPercent = 0.28f),
        KeySpec("g_5", "5%", 0x22, row = 1, column = 5, localTapXPercent = 0.77f, localTapYPercent = 0.28f),
        KeySpec("g_6", "6^", 0x23, row = 1, column = 6, localTapXPercent = 0.89f, localTapYPercent = 0.28f),

        // Row 3: Letter Keys (6 Keys)
        KeySpec("g_tab", "Tab", 0x2B, row = 2, column = 0, widthWeight = 1.25f, localTapXPercent = 0.10f, localTapYPercent = 0.42f),
        KeySpec("g_q", "Q", 0x14, row = 2, column = 1, localTapXPercent = 0.20f, localTapYPercent = 0.42f),
        KeySpec("g_w", "W", 0x1A, row = 2, column = 2, localTapXPercent = 0.25f, localTapYPercent = 0.60f),
        KeySpec("g_e", "E", 0x08, row = 2, column = 3, localTapXPercent = 0.35f, localTapYPercent = 0.42f),
        KeySpec("g_r", "R", 0x15, row = 2, column = 4, localTapXPercent = 0.78f, localTapYPercent = 0.82f),
        KeySpec("g_t", "T", 0x17, row = 2, column = 5, localTapXPercent = 0.55f, localTapYPercent = 0.42f),

        // Row 4: Letter Keys (6 Keys)
        KeySpec("g_caps", "Caps Lock", 0x39, row = 3, column = 0, widthWeight = 1.35f, isCapsLock = true, localTapXPercent = 0.10f, localTapYPercent = 0.56f),
        KeySpec("g_a", "A", 0x04, row = 3, column = 1, localTapXPercent = 0.16f, localTapYPercent = 0.70f),
        KeySpec("g_s", "S", 0x16, row = 3, column = 2, localTapXPercent = 0.25f, localTapYPercent = 0.80f),
        KeySpec("g_d", "D", 0x07, row = 3, column = 3, localTapXPercent = 0.34f, localTapYPercent = 0.70f),
        KeySpec("g_f", "F", 0x09, row = 3, column = 4, localTapXPercent = 0.70f, localTapYPercent = 0.55f),
        KeySpec("g_g", "G", 0x0A, row = 3, column = 5, localTapXPercent = 0.78f, localTapYPercent = 0.45f),

        // Row 5: Letter Keys (6 Keys)
        KeySpec("g_shift", "Shift", 0xE1, row = 4, column = 0, widthWeight = 1.45f, isModifier = true, modifierMask = MOD_LEFT_SHIFT, localTapXPercent = 0.12f, localTapYPercent = 0.74f),
        KeySpec("g_z", "Z", 0x1D, row = 4, column = 1, localTapXPercent = 0.22f, localTapYPercent = 0.86f),
        KeySpec("g_x", "X", 0x1B, row = 4, column = 2, localTapXPercent = 0.34f, localTapYPercent = 0.86f),
        KeySpec("g_c", "C", 0x06, row = 4, column = 3, localTapXPercent = 0.84f, localTapYPercent = 0.86f),
        KeySpec("g_v", "V", 0x19, row = 4, column = 4, localTapXPercent = 0.60f, localTapYPercent = 0.86f),
        KeySpec("g_b", "B", 0x05, row = 4, column = 5, localTapXPercent = 0.50f, localTapYPercent = 0.86f),

        // Row 6: Control Keys (4 Keys)
        KeySpec("g_ctrl", "Ctrl", 0xE0, row = 5, column = 0, widthWeight = 1.25f, isModifier = true, modifierMask = MOD_LEFT_CTRL, localTapXPercent = 0.14f, localTapYPercent = 0.90f),
        KeySpec("g_fn", "FN", 0x00, row = 5, column = 1, widthWeight = 1.1f, isFnKey = true, localTapXPercent = 0.28f, localTapYPercent = 0.90f),
        KeySpec("g_alt", "Alt", 0xE2, row = 5, column = 2, widthWeight = 1.15f, isModifier = true, modifierMask = MOD_LEFT_ALT, localTapXPercent = 0.42f, localTapYPercent = 0.90f),
        KeySpec("g_space", "Spacebar", 0x2C, row = 5, column = 3, widthWeight = 2.8f, localTapXPercent = 0.88f, localTapYPercent = 0.74f)
    )

    fun createDefault35LayoutProfile(): KeyboardLayoutProfile = KeyboardLayoutProfile(
        id = 1L,
        name = GAMING_35_KEYBOARD_NAME,
        gameTitle = "Default 35-Key Sounce Preset",
        keys = createSounce35Keys(),
        keySpacingDp = 4f,
        keyScale = 1f,
        isDefault = true
    )

    /**
     * Complete Normal QWERTY Virtual Keyboard (with Function row option + full letters, numbers, symbols, modifiers).
     */
    fun createNormalQwertyRows(includeFunctionRow: Boolean): List<List<KeySpec>> {
        val fnRow = listOf(
            KeySpec("q_esc", "Esc", 0x29, 0, 0),
            KeySpec("q_f1", "F1", 0x3A, 0, 1),
            KeySpec("q_f2", "F2", 0x3B, 0, 2),
            KeySpec("q_f3", "F3", 0x3C, 0, 3),
            KeySpec("q_f4", "F4", 0x3D, 0, 4),
            KeySpec("q_f5", "F5", 0x3E, 0, 5),
            KeySpec("q_f6", "F6", 0x3F, 0, 6),
            KeySpec("q_f7", "F7", 0x40, 0, 7),
            KeySpec("q_f8", "F8", 0x41, 0, 8),
            KeySpec("q_f9", "F9", 0x42, 0, 9),
            KeySpec("q_f10", "F10", 0x43, 0, 10),
            KeySpec("q_f11", "F11", 0x44, 0, 11),
            KeySpec("q_f12", "F12", 0x45, 0, 12)
        )

        val numRow = listOf(
            KeySpec("q_tilde", "~", 0x35, 1, 0),
            KeySpec("q_1", "1!", 0x1E, 1, 1),
            KeySpec("q_2", "2@", 0x1F, 1, 2),
            KeySpec("q_3", "3#", 0x20, 1, 3),
            KeySpec("q_4", "4$", 0x21, 1, 4),
            KeySpec("q_5", "5%", 0x22, 1, 5),
            KeySpec("q_6", "6^", 0x23, 1, 6),
            KeySpec("q_7", "7&", 0x24, 1, 7),
            KeySpec("q_8", "8*", 0x25, 1, 8),
            KeySpec("q_9", "9(", 0x26, 1, 9),
            KeySpec("q_0", "0)", 0x27, 1, 10),
            KeySpec("q_minus", "-_", 0x2D, 1, 11),
            KeySpec("q_eq", "=+", 0x2E, 1, 12),
            KeySpec("q_bksp", "Bksp", 0x2A, 1, 13, widthWeight = 1.6f)
        )

        val topRow = listOf(
            KeySpec("q_tab", "Tab", 0x2B, 2, 0, widthWeight = 1.4f),
            KeySpec("q_q", "Q", 0x14, 2, 1),
            KeySpec("q_w", "W", 0x1A, 2, 2),
            KeySpec("q_e", "E", 0x08, 2, 3),
            KeySpec("q_r", "R", 0x15, 2, 4),
            KeySpec("q_t", "T", 0x17, 2, 5),
            KeySpec("q_y", "Y", 0x1C, 2, 6),
            KeySpec("q_u", "U", 0x18, 2, 7),
            KeySpec("q_i", "I", 0x0C, 2, 8),
            KeySpec("q_o", "O", 0x12, 2, 9),
            KeySpec("q_p", "P", 0x13, 2, 10),
            KeySpec("q_lb", "[{", 0x2F, 2, 11),
            KeySpec("q_rb", "]}", 0x30, 2, 12),
            KeySpec("q_bs", "\\|", 0x31, 2, 13)
        )

        val homeRow = listOf(
            KeySpec("q_caps", "Caps", 0x39, 3, 0, widthWeight = 1.55f, isCapsLock = true),
            KeySpec("q_a", "A", 0x04, 3, 1),
            KeySpec("q_s", "S", 0x16, 3, 2),
            KeySpec("q_d", "D", 0x07, 3, 3),
            KeySpec("q_f", "F", 0x09, 3, 4),
            KeySpec("q_g", "G", 0x0A, 3, 5),
            KeySpec("q_h", "H", 0x0B, 3, 6),
            KeySpec("q_j", "J", 0x0D, 3, 7),
            KeySpec("q_k", "K", 0x0E, 3, 8),
            KeySpec("q_l", "L", 0x0F, 3, 9),
            KeySpec("q_semi", ";:", 0x33, 3, 10),
            KeySpec("q_quot", "'\"", 0x34, 3, 11),
            KeySpec("q_enter", "Enter", 0x28, 3, 12, widthWeight = 1.85f)
        )

        val bottomRow = listOf(
            KeySpec("q_lshift", "Shift", 0xE1, 4, 0, widthWeight = 1.8f, isModifier = true, modifierMask = MOD_LEFT_SHIFT),
            KeySpec("q_z", "Z", 0x1D, 4, 1),
            KeySpec("q_x", "X", 0x1B, 4, 2),
            KeySpec("q_c", "C", 0x06, 4, 3),
            KeySpec("q_v", "V", 0x19, 4, 4),
            KeySpec("q_b", "B", 0x05, 4, 5),
            KeySpec("q_n", "N", 0x11, 4, 6),
            KeySpec("q_m", "M", 0x10, 4, 7),
            KeySpec("q_comma", ",<", 0x36, 4, 8),
            KeySpec("q_dot", ".>", 0x37, 4, 9),
            KeySpec("q_slash", "/?", 0x38, 4, 10),
            KeySpec("q_up", "▲", 0x52, 4, 11),
            KeySpec("q_rshift", "Shift", 0xE5, 4, 12, widthWeight = 1.4f, isModifier = true, modifierMask = 0x20)
        )

        val modRow = listOf(
            KeySpec("q_ctrl", "Ctrl", 0xE0, 5, 0, widthWeight = 1.3f, isModifier = true, modifierMask = MOD_LEFT_CTRL),
            KeySpec("q_win", "Win", 0xE3, 5, 1, widthWeight = 1.1f, isModifier = true, modifierMask = MOD_LEFT_GUI),
            KeySpec("q_alt", "Alt", 0xE2, 5, 2, widthWeight = 1.2f, isModifier = true, modifierMask = MOD_LEFT_ALT),
            KeySpec("q_space", "Spacebar", 0x2C, 5, 3, widthWeight = 4.5f),
            KeySpec("q_ralt", "Alt", 0xE6, 5, 4, widthWeight = 1.1f, isModifier = true, modifierMask = 0x40),
            KeySpec("q_left", "◀", 0x50, 5, 5),
            KeySpec("q_down", "▼", 0x51, 5, 6),
            KeySpec("q_right", "▶", 0x4F, 5, 7)
        )

        return if (includeFunctionRow) {
            listOf(fnRow, numRow, topRow, homeRow, bottomRow, modRow)
        } else {
            listOf(numRow, topRow, homeRow, bottomRow, modRow)
        }
    }

    /**
     * Catalog of additional keys users can add via "+ ADD KEY".
     */
    val extraKeyCatalog: List<KeySpec> = listOf(
        KeySpec("add_7", "7&", 0x24, 1, 7, isCustomAdded = true),
        KeySpec("add_8", "8*", 0x25, 1, 8, isCustomAdded = true),
        KeySpec("add_9", "9(", 0x26, 1, 9, isCustomAdded = true),
        KeySpec("add_0", "0)", 0x27, 1, 10, isCustomAdded = true),
        KeySpec("add_y", "Y", 0x1C, 2, 6, isCustomAdded = true),
        KeySpec("add_u", "U", 0x18, 2, 7, isCustomAdded = true),
        KeySpec("add_i", "I", 0x0C, 2, 8, isCustomAdded = true),
        KeySpec("add_o", "O", 0x12, 2, 9, isCustomAdded = true),
        KeySpec("add_p", "P", 0x13, 2, 10, isCustomAdded = true),
        KeySpec("add_h", "H", 0x0B, 3, 6, isCustomAdded = true),
        KeySpec("add_j", "J", 0x0D, 3, 7, isCustomAdded = true),
        KeySpec("add_k", "K", 0x0E, 3, 8, isCustomAdded = true),
        KeySpec("add_l", "L", 0x0F, 3, 9, isCustomAdded = true),
        KeySpec("add_n", "N", 0x11, 4, 6, isCustomAdded = true),
        KeySpec("add_m", "M", 0x10, 4, 7, isCustomAdded = true),
        KeySpec("add_enter", "Enter", 0x28, 3, 6, widthWeight = 1.5f, isCustomAdded = true),
        KeySpec("add_bksp", "Bksp", 0x2A, 1, 7, widthWeight = 1.4f, isCustomAdded = true),
        KeySpec("add_f6", "F6", 0x3F, 0, 6, isCustomAdded = true),
        KeySpec("add_f7", "F7", 0x40, 0, 7, isCustomAdded = true),
        KeySpec("add_f8", "F8", 0x41, 0, 8, isCustomAdded = true),
        KeySpec("add_f9", "F9", 0x42, 0, 9, isCustomAdded = true),
        KeySpec("add_f10", "F10", 0x43, 0, 10, isCustomAdded = true),
        KeySpec("add_f11", "F11", 0x44, 0, 11, isCustomAdded = true),
        KeySpec("add_f12", "F12", 0x45, 0, 12, isCustomAdded = true),
        KeySpec("add_up", "▲", 0x52, 4, 6, isCustomAdded = true),
        KeySpec("add_down", "▼", 0x51, 5, 5, isCustomAdded = true),
        KeySpec("add_left", "◀", 0x50, 5, 4, isCustomAdded = true),
        KeySpec("add_right", "▶", 0x4F, 5, 6, isCustomAdded = true),
        KeySpec("add_del", "Del", 0x4C, 0, 7, isCustomAdded = true),
        KeySpec("add_home", "Home", 0x4A, 0, 8, isCustomAdded = true),
        KeySpec("add_end", "End", 0x4D, 0, 9, isCustomAdded = true),
        KeySpec("add_win", "Win", 0xE3, 5, 4, isModifier = true, modifierMask = MOD_LEFT_GUI, isCustomAdded = true)
    )
}
