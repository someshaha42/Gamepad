package com.example.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AccessibilityService enabling Local Controller mode to dispatch touch taps and drag gestures
 * onto on-screen game controls via AccessibilityService.dispatchGesture().
 *
 * Android platform limitation: A non-system Android app cannot inject raw hardware KeyEvents or
 * native Gamepad MotionEvents into other apps. Touch gesture injection targets mapped screen positions.
 */
class SounceAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: SounceAccessibilityService? = null

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _lastGestureStatus = MutableStateFlow("Accessibility Bridge Standby")
        val lastGestureStatus: StateFlow<String> = _lastGestureStatus.asStateFlow()

        fun getInstance(): SounceAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        _lastGestureStatus.value = "Sounce Touch Bridge Connected"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally minimal: we do not scrape screen content or read user text.
    }

    override fun onInterrupt() {
        _lastGestureStatus.value = "Accessibility Bridge Interrupted"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        _isConnected.value = false
        _lastGestureStatus.value = "Accessibility Bridge Stopped"
    }

    fun dispatchTapPercent(xPercent: Float, yPercent: Float, durationMs: Long = 55L): Boolean {
        val metrics = resources.displayMetrics
        val x = (xPercent.coerceIn(0.02f, 0.98f) * metrics.widthPixels)
        val y = (yPercent.coerceIn(0.02f, 0.98f) * metrics.heightPixels)
        return dispatchTapPixels(x, y, durationMs)
    }

    fun dispatchTapPixels(x: Float, y: Float, durationMs: Long = 55L): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(20L, 450L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    _lastGestureStatus.value = "Tap injected @ (${x.toInt()}, ${y.toInt()})"
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    _lastGestureStatus.value = "Tap cancelled by system"
                }
            },
            null
        )
    }

    fun dispatchSwipePercent(
        startXPercent: Float,
        startYPercent: Float,
        endXPercent: Float,
        endYPercent: Float,
        durationMs: Long = 110L
    ): Boolean {
        val metrics = resources.displayMetrics
        val sx = startXPercent.coerceIn(0.02f, 0.98f) * metrics.widthPixels
        val sy = startYPercent.coerceIn(0.02f, 0.98f) * metrics.heightPixels
        val ex = endXPercent.coerceIn(0.02f, 0.98f) * metrics.widthPixels
        val ey = endYPercent.coerceIn(0.02f, 0.98f) * metrics.heightPixels

        val path = Path().apply {
            moveTo(sx, sy)
            lineTo(ex, ey)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(30L, 350L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun maxSimultaneousStrokes(): Int {
        return GestureDescription.getMaxStrokeCount()
    }

    fun supportsContinuousStrokes(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}
