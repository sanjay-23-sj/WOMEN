package com.sanx.app.service.trigger

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.sanx.app.data.model.TriggerSensitivity
import kotlin.math.sqrt

/**
 * Detects aggressive panic shake gestures by analyzing
 * rolling standard deviation of 3-axis accelerometer data.
 * A sustained high-magnitude shake pattern over multiple samples triggers the emergency.
 */
class ShakeDetector(
    private val context: Context,
    private val sensitivity: TriggerSensitivity = TriggerSensitivity.MEDIUM,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Thresholds (in m/s²)
    private val shakeThreshold: Float get() = when (sensitivity) {
        TriggerSensitivity.LOW    -> 35f
        TriggerSensitivity.MEDIUM -> 26f
        TriggerSensitivity.HIGH   -> 18f
    }
    private val requiredConsecutiveHits = 4   // Must exceed threshold N consecutive samples
    private val cooldownMs = 2000L

    private var consecutiveHits = 0
    private var lastTriggerTime = 0L

    fun start() {
        try {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Remove gravity baseline (≈ 9.8 m/s²) from z-axis
        val magnitude = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (Math.abs(magnitude) > shakeThreshold) {
            consecutiveHits++
            if (consecutiveHits >= requiredConsecutiveHits) {
                val now = System.currentTimeMillis()
                if (now - lastTriggerTime > cooldownMs) {
                    lastTriggerTime = now
                    consecutiveHits = 0
                    onShakeDetected()
                }
            }
        } else {
            consecutiveHits = maxOf(0, consecutiveHits - 1)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
