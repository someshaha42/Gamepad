# Sounce Gaming Controller

Native Android Kotlin + Jetpack Compose gaming controller application supporting Bluetooth HID peripheral emulation, USB Host/Gadget compatibility inspection, and Local Floating Overlay touch gesture injection.

## Controller Modes

1. **Mode A: Gamepad**
   - Dual analog joysticks (L-Stick / R-Stick) with deadzone and adjustable sensitivity
   - 8-way tactile D-Pad, A/B/X/Y face buttons, L1/L2/R1/R2 shoulders & analog triggers, Start/Select, L3/R3
   - Custom button mapping (HID button bit index & Local Mode screen tap targets)
2. **Mode B: Keyboard + Mouse (Split-Screen)**
   - Left Keyboard + Right Mouse trackpad operating simultaneously
   - Draggable split-screen divider, independent keyboard and mouse opacity sliders
3. **Mode C: Keyboard Only**
   - **Gaming Keyboard (Default)**: Exact **Sounce One-Handed RGB Gaming Keyboard (35 Keys)** (`Esc..F5`, `~..6^` including `4₹`, `Tab..T`, `Caps Lock..G`, `Shift..B`, `Ctrl, FN, Alt, Spacebar`) + `+ ADD KEY` customization, key resizing, repositioning, spacing, and per-game saved layouts
   - **Normal Keyboard**: Complete QWERTY virtual keyboard with toggleable `F1–F12` row, symbols, modifiers, and arrows
4. **Mode D: Mouse Only**
   - Precision multi-touch trackpad, dedicated scroll strip, Left/Middle/Right click buttons, adjustable sensitivity/speed/size, and swappable L/R click

## Compatibility & Platform Limitations

- **Bluetooth HID (`BluetoothHidDevice`)**: Supported on Android 9 (API 28+) when device hardware and OEM Bluetooth stack allow `BluetoothProfile.HID_DEVICE` registration. Target phone/PC requires no companion app.
- **USB HID**: Standard unrooted Android exposes USB Host API (`UsbManager`) for reading attached USB peripherals, not USB HID Gadget peripheral mode (`/dev/hidg*`). If `/dev/hidg*` is writable (custom kernel/ROM), USB HID reports are dispatched; otherwise an accurate compatibility report is displayed and Bluetooth/Local modes stay available.
- **Local Controller Mode**: Uses `SYSTEM_ALERT_WINDOW` floating overlay + `AccessibilityService.dispatchGesture()` to inject mapped taps and drag gestures onto on-screen game buttons on the same phone. Standard Android apps cannot inject raw hardware `KeyEvent` or native gamepad `MotionEvent` into third-party games without system/ADB privileges.

## Build & GitHub Actions Debug APK

- **Automatic GitHub Build**: Pushing to `main`, opening a PR to `main`, or clicking **Run workflow** (`workflow_dispatch`) triggers `.github/workflows/android-debug.yml`, which runs `./gradlew assembleDebug` and publishes `Sounce-Gaming-Controller-Debug-APK` (`app/build/outputs/apk/debug/app-debug.apk`) in GitHub Actions Artifacts.
- **Local Android Studio**: Open the project in Android Studio and run `Build > Build Bundle(s) / APK(s) > Build APK(s)` or run `gradle assembleDebug`.
