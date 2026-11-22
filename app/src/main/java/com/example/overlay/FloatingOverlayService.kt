package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.DefaultSounceLayouts
import com.example.engine.ConnectionManager
import com.example.model.ControllerMode
import com.example.model.ControllerSettings
import com.example.model.KeyboardStyle
import com.example.ui.components.rememberRgbVisualState
import com.example.ui.controllers.GamepadPanel
import com.example.ui.controllers.GamingKeyboard35Panel
import com.example.ui.controllers.KeyboardMouseSplitPanel
import com.example.ui.controllers.MouseTrackpadPanel
import com.example.ui.controllers.NormalKeyboardPanel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SounceControllerTheme
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FloatingOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_START_LOCAL = "com.example.action.START_LOCAL"
        const val ACTION_SHOW_CONTROLLER = "com.example.action.SHOW_CONTROLLER"
        const val ACTION_HIDE_CONTROLLER = "com.example.action.HIDE_CONTROLLER"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "sounce_overlay_channel"
        private const val NOTIFICATION_ID = 4041

        fun startOverlay(context: Context, localMode: Boolean = true) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = if (localMode) ACTION_START_LOCAL else ACTION_SHOW_CONTROLLER
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                this.action = action
            }
            context.startService(intent)
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var connectionManager: ConnectionManager
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var latestSettings = ControllerSettings()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        connectionManager = ConnectionManager.getInstance(applicationContext)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        attachOverlayWindow()
        connectionManager.setOverlayServiceRunning(running = true, localExplicit = true)

        serviceScope.launch {
            connectionManager.settingsDataStore.settingsFlow.collect { settings ->
                latestSettings = settings
                updateWindowSizeForVisibility(settings)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                connectionManager.releaseAllInputs()
                connectionManager.setOverlayServiceRunning(running = false, localExplicit = false)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_HIDE_CONTROLLER -> {
                serviceScope.launch {
                    connectionManager.settingsDataStore.updateOpacities(visible = false)
                }
            }
            ACTION_SHOW_CONTROLLER -> {
                serviceScope.launch {
                    connectionManager.settingsDataStore.updateOpacities(visible = true)
                }
            }
            ACTION_START_LOCAL -> {
                connectionManager.setOverlayServiceRunning(running = true, localExplicit = true)
                serviceScope.launch {
                    connectionManager.settingsDataStore.updateOpacities(visible = true)
                }
            }
        }
        return START_STICKY
    }

    private fun attachOverlayWindow() {
        if (composeView != null) return

        val density = resources.displayMetrics.density
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            (latestSettings.overlayWidthDp * density).roundToInt(),
            (latestSettings.overlayHeightDp * density).roundToInt(),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = latestSettings.overlayX
            y = latestSettings.overlayY
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
            setContent {
                SounceControllerTheme {
                    OverlayRootContent()
                }
            }
        }

        layoutParams = params
        composeView = view
        windowManager.addView(view, params)
    }

    private fun updateWindowSizeForVisibility(settings: ControllerSettings) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val density = resources.displayMetrics.density
        if (settings.controllerVisible) {
            params.width = (settings.overlayWidthDp * density).roundToInt()
            params.height = (settings.overlayHeightDp * density).roundToInt()
        } else {
            params.width = (148 * density).roundToInt()
            params.height = (52 * density).roundToInt()
        }
        params.x = settings.overlayX
        params.y = settings.overlayY
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun moveOverlayBy(dxPx: Float, dyPx: Float) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        params.x = (params.x + dxPx.roundToInt()).coerceAtLeast(0)
        params.y = (params.y + dyPx.roundToInt()).coerceAtLeast(0)
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun saveCurrentPosition() {
        val params = layoutParams ?: return
        serviceScope.launch {
            connectionManager.settingsDataStore.updateOverlayBounds(x = params.x, y = params.y)
        }
    }

    private fun resizeOverlayBy(dxDp: Float, dyDp: Float) {
        val nextW = (latestSettings.overlayWidthDp + dxDp.roundToInt()).coerceIn(240, 680)
        val nextH = (latestSettings.overlayHeightDp + dyDp.roundToInt()).coerceIn(190, 520)
        latestSettings = latestSettings.copy(overlayWidthDp = nextW, overlayHeightDp = nextH)
        updateWindowSizeForVisibility(latestSettings)
    }

    private fun saveCurrentSize() {
        serviceScope.launch {
            connectionManager.settingsDataStore.updateOverlayBounds(
                widthDp = latestSettings.overlayWidthDp,
                heightDp = latestSettings.overlayHeightDp
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Composable
    private fun OverlayRootContent() {
        val settings by connectionManager.settingsDataStore.settingsFlow.collectAsState(initial = latestSettings)
        val snapshot by connectionManager.snapshot.collectAsState()
        val activeLayout by connectionManager.settingsDataStore.settingsFlow
            .flatMapLatest { s -> connectionManager.layoutRepository.observeLayout(s.activeLayoutId) }
            .collectAsState(initial = DefaultSounceLayouts.createDefault35LayoutProfile())

        val rgbState = rememberRgbVisualState(
            effect = settings.rgbEffect,
            staticColor = Color(settings.rgbStaticColorArgb)
        )
        val density = LocalDensity.current.density

        if (!settings.controllerVisible) {
            // Minimized restore pill so game screen touches pass everywhere else
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(ObsidianBlack.copy(alpha = 0.88f))
                    .border(1.5.dp, rgbState.primaryAccent, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(onDragEnd = { saveCurrentPosition() }) { change, drag ->
                            change.consume()
                            moveOverlayBy(drag.x, drag.y)
                        }
                    }
                    .clickable {
                        serviceScope.launch {
                            connectionManager.settingsDataStore.updateOpacities(visible = true)
                        }
                    }
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Show Controller",
                    tint = rgbState.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "SHOW PAD",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianBlack.copy(alpha = (0.65f * settings.controllerOpacity).coerceIn(0.05f, 0.92f)))
                .border(1.5.dp, rgbState.borderBrush, RoundedCornerShape(16.dp))
                .padding(6.dp)
        ) {
            // Draggable Overlay Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianElevated.copy(alpha = 0.88f))
                    .pointerInput(Unit) {
                        detectDragGestures(onDragEnd = { saveCurrentPosition() }) { change, drag ->
                            change.consume()
                            moveOverlayBy(drag.x, drag.y)
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag Overlay",
                        tint = rgbState.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = "Mode",
                        tint = if (snapshot.isReadyToTransmit) StatusConnected else StatusWarning,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${settings.selectedMode.shortLabel} • ${snapshot.activeEngine.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable {
                                serviceScope.launch {
                                    connectionManager.settingsDataStore.updateOpacities(visible = false)
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Hide Controller",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable {
                                connectionManager.releaseAllInputs()
                                connectionManager.setOverlayServiceRunning(false, false)
                                stopSelf()
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Overlay",
                            tint = Color(0xFFFF4D6D),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Active Controller Surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (settings.selectedMode) {
                    ControllerMode.GAMEPAD -> {
                        GamepadPanel(
                            opacity = settings.gamepadOpacity * settings.controllerOpacity,
                            joystickSensitivity = settings.joystickSensitivity,
                            mappings = settings.gamepadMappings,
                            rgbState = rgbState,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                            compactMode = true,
                            onButtonEvent = { btn, mapping, pressed ->
                                connectionManager.dispatchGamepadButton(
                                    btn,
                                    mapping.targetHidBit,
                                    pressed,
                                    mapping.localTapXPercent,
                                    mapping.localTapYPercent
                                )
                            },
                            onDpadEvent = { u, d, l, r -> connectionManager.dispatchGamepadDpad(u, d, l, r) },
                            onLeftStick = { x, y -> connectionManager.dispatchLeftStick(x, y) },
                            onRightStick = { x, y -> connectionManager.dispatchRightStick(x, y) },
                            onTriggers = { l2, r2 -> connectionManager.dispatchTriggers(l2, r2) }
                        )
                    }
                    ControllerMode.KEYBOARD_MOUSE -> {
                        KeyboardMouseSplitPanel(
                            settings = settings,
                            layout = activeLayout,
                            rgbState = rgbState,
                            onAddKeyClick = {},
                            onSplitRatioChange = { r ->
                                serviceScope.launch { connectionManager.settingsDataStore.updateSensitivities(splitRatio = r) }
                            },
                            onKeyEvent = { key, down ->
                                connectionManager.dispatchKey(
                                    key.hidUsageCode,
                                    key.label,
                                    key.isModifier,
                                    key.modifierMask,
                                    down,
                                    key.localTapXPercent,
                                    key.localTapYPercent
                                )
                            },
                            onMouseMove = { dx, dy -> connectionManager.dispatchMouseMove(dx, dy) },
                            onMouseScroll = { w -> connectionManager.dispatchMouseMove(0, 0, w) },
                            onMouseButton = { bit, down -> connectionManager.dispatchMouseButton(bit, down) }
                        )
                    }
                    ControllerMode.KEYBOARD_ONLY -> {
                        if (settings.selectedKeyboardStyle == KeyboardStyle.GAMING_35) {
                            GamingKeyboard35Panel(
                                layout = activeLayout,
                                opacity = settings.keyboardOpacity * settings.controllerOpacity,
                                rgbState = rgbState,
                                compactHalfWidth = false,
                                hapticEnabled = settings.hapticFeedbackEnabled,
                                showHeaderControls = false,
                                onKeyEvent = { key, down ->
                                    connectionManager.dispatchKey(
                                        key.hidUsageCode,
                                        key.label,
                                        key.isModifier,
                                        key.modifierMask,
                                        down,
                                        key.localTapXPercent,
                                        key.localTapYPercent
                                    )
                                }
                            )
                        } else {
                            NormalKeyboardPanel(
                                opacity = settings.keyboardOpacity * settings.controllerOpacity,
                                rgbState = rgbState,
                                hapticEnabled = settings.hapticFeedbackEnabled,
                                onKeyEvent = { key, down ->
                                    connectionManager.dispatchKey(
                                        key.hidUsageCode,
                                        key.label,
                                        key.isModifier,
                                        key.modifierMask,
                                        down,
                                        key.localTapXPercent,
                                        key.localTapYPercent
                                    )
                                }
                            )
                        }
                    }
                    ControllerMode.MOUSE_ONLY -> {
                        MouseTrackpadPanel(
                            opacity = settings.mouseOpacity * settings.controllerOpacity,
                            sensitivity = settings.mouseSensitivity,
                            speed = settings.mouseSpeed,
                            trackpadScale = settings.trackpadSizeScale,
                            buttonConfig = settings.mouseButtonConfig,
                            rgbState = rgbState,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                            compactMode = true,
                            onMove = { dx, dy -> connectionManager.dispatchMouseMove(dx, dy) },
                            onScroll = { w -> connectionManager.dispatchMouseMove(0, 0, w) },
                            onButton = { bit, down -> connectionManager.dispatchMouseButton(bit, down) }
                        )
                    }
                }
            }

            // Bottom-Right Resize Handle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianElevated.copy(alpha = 0.85f))
                        .pointerInput(density) {
                            detectDragGestures(onDragEnd = { saveCurrentSize() }) { change, drag ->
                                change.consume()
                                resizeOverlayBy(drag.x / density, drag.y / density)
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Resize Overlay",
                        tint = rgbState.primaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingOverlayService::class.java).apply { action = ACTION_SHOW_CONTROLLER },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FloatingOverlayService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.action_show_controller), showIntent)
            .addAction(0, getString(R.string.action_stop_overlay), stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        composeView?.let { runCatching { windowManager.removeView(it) } }
        composeView = null
        connectionManager.setOverlayServiceRunning(false, false)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
