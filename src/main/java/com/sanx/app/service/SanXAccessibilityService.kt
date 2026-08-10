package com.sanx.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.sanx.app.data.model.TriggerConfig
import com.sanx.app.data.model.TriggerType
import com.sanx.app.service.trigger.TriggerManager

/**
 * Accessibility service that intercepts hardware key events globally.
 * Monitors Volume Up/Down and Power button presses to detect secret trigger patterns
 * even when the device is locked or the app is in the background/not recently opened.
 *
 * Employs a dual TriggerManager architecture:
 * 1. Delegates to the active [EmergencyService] triggerManager if running.
 * 2. Falls back to a local TriggerManager if the service is inactive, waking the app immediately on trigger.
 */
class SanXAccessibilityService : AccessibilityService() {

    companion object {
        // Set by EmergencyService to enable key event routing
        var triggerManager: TriggerManager? = null
        var isServiceRunning = false
    }

    private var localTriggerManager: TriggerManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        initLocalTriggerManager()
    }

    private fun initLocalTriggerManager() {
        localTriggerManager = TriggerManager(applicationContext) { _ ->
            // Wake up and trigger emergency directly from accessibility context (exempt from background FGS limits)
            val intent = Intent(applicationContext, EmergencyService::class.java).apply {
                action = EmergencyService.ACTION_TRIGGER_EMERGENCY
                putExtra(EmergencyService.EXTRA_SEVERITY, 1)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (_: Exception) {}
        }

        // Always enable continuous motion trigger configurations in local background listener
        val backgroundConfigs = listOf(
            TriggerConfig(TriggerType.DOUBLE_TAP_BACK, enabled = true),
            TriggerConfig(TriggerType.SHAKE_PANIC, enabled = true)
        )
        localTriggerManager?.start(backgroundConfigs)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        localTriggerManager?.stopAll()
        localTriggerManager = null
        triggerManager = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && action == KeyEvent.ACTION_DOWN) {
            val handled = EmergencyService.handleGlobalVolumeDownKeyPress()
            if (handled) return true // Consume key press (hide volume slider) during pending state
        }
        return super.onKeyEvent(event)
    }
}
