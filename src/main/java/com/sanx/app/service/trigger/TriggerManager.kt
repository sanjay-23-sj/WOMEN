package com.sanx.app.service.trigger

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.sanx.app.data.model.TriggerConfig
import com.sanx.app.data.model.TriggerSensitivity
import com.sanx.app.data.model.TriggerType

/**
 * Central coordinator for all SanX emergency trigger detection systems.
 * Manages lifecycle of all sensor listeners and hardware key state machines.
 * Calls [onEmergencyTriggered] exactly once per trigger event (debounced).
 */
class TriggerManager(
    private val context: Context,
    private val onEmergencyTriggered: (source: TriggerType) -> Unit
) {

    private var tapDetector: TapSensorDetector? = null
    private var shakeDetector: ShakeDetector? = null
    private val handler = Handler(Looper.getMainLooper())

    // Debounce guard: prevents multiple triggers within 3 seconds
    private var lastTriggerTime = 0L
    private val debounceMs = 3000L



    /**
     * Starts all configured triggers.
     * Call from EmergencyService.onStartCommand.
     */
    fun start(configs: List<TriggerConfig>) {
        configs.forEach { config ->
            if (!config.enabled) return@forEach
            when (config.type) {
                TriggerType.DOUBLE_TAP_BACK -> startDoubleTap(config.sensitivity)
                TriggerType.SHAKE_PANIC     -> startShake(config.sensitivity)
            }
        }
    }

    fun stopAll() {
        tapDetector?.stop()
        tapDetector = null
        shakeDetector?.stop()
        shakeDetector = null
        handler.removeCallbacksAndMessages(null)
    }

    // ─── Internal starts ─────────────────────────────────────────────────────

    private fun startDoubleTap(sensitivity: TriggerSensitivity) {
        tapDetector?.stop()
        tapDetector = TapSensorDetector(context, sensitivity) {
            fire(TriggerType.DOUBLE_TAP_BACK)
        }.also { it.start() }
    }

    private fun startShake(sensitivity: TriggerSensitivity) {
        shakeDetector?.stop()
        shakeDetector = ShakeDetector(context, sensitivity) {
            fire(TriggerType.SHAKE_PANIC)
        }.also { it.start() }
    }



    // ─── Fire with debounce ───────────────────────────────────────────────────

    private fun fire(source: TriggerType) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < debounceMs) return
        lastTriggerTime = now
        handler.post { onEmergencyTriggered(source) }
    }
}
