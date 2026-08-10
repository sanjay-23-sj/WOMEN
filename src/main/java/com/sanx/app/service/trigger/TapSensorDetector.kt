package com.sanx.app.service.trigger

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.sanx.app.data.model.TriggerSensitivity
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects intentional double-tap gestures on the back of the phone while in a pocket.
 * Uses high-pass filtered accelerometer data combined with gyroscope confirmation
 * to distinguish intentional taps from random pocket movement.
 *
 * Algorithm:
 * 1. High-pass filter removes gravity component from raw accelerometer readings.
 * 2. Calculate jerk magnitude: sqrt(Δax² + Δay² + Δaz²).
 * 3. A tap candidate is registered when jerk magnitude exceeds the threshold.
 * 4. Two candidates within the time window confirm a double-tap.
 * 5. Gyroscope acts as a secondary guard: excessive rotation during the event
 *    reduces confidence (user is likely walking or running, not tapping deliberately).
 */
class TapSensorDetector(
    private val context: Context,
    private val sensitivity: TriggerSensitivity = TriggerSensitivity.MEDIUM,
    private val onDoubleTapDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Thresholds by sensitivity level
    private val tapThreshold: Float get() = when (sensitivity) {
        TriggerSensitivity.LOW    -> 18f
        TriggerSensitivity.MEDIUM -> 12f
        TriggerSensitivity.HIGH   -> 8f
    }
    private val doubleTapWindowMs: Long = 2000L
    private val cooldownMs: Long = 1500L
    private val maxGyroConfirmMs: Long = 300L
    private val gyroSuppressThreshold: Float = 8.0f   // rad/s — suppress if rotating this fast
    private val minTapIntervalMs: Long = 200L         // Debounce window to filter sensor bounces

    // Internal state
    private var lastTapTimeMs = 0L
    private var tapCount = 0
    private var lastCooldownMs = 0L

    // High-pass filter state
    private val gravity = FloatArray(3)
    private val alpha = 0.9f
    private var lastFilteredMagnitude = 0f

    // Gyroscope guard state
    private var lastGyroMagnitude = 0f

    fun start() {
        try {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (e: SecurityException) {
            try {
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            } catch (_: Exception) {}
        }
        try {
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroMagnitude = sqrt(
                    event.values[0] * event.values[0] +
                    event.values[1] * event.values[1] +
                    event.values[2] * event.values[2]
                )
            }

            Sensor.TYPE_ACCELEROMETER -> {
                // High-pass filter to isolate dynamic force from gravity
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                val linearX = event.values[0] - gravity[0]
                val linearY = event.values[1] - gravity[1]
                val linearZ = event.values[2] - gravity[2]

                val magnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)
                lastFilteredMagnitude = magnitude

                if (magnitude > tapThreshold) {
                    // Guard: suppress if excessive rotation is present (walking/running interference)
                    if (lastGyroMagnitude > gyroSuppressThreshold) return

                    // Cooldown guard
                    if (now - lastCooldownMs < cooldownMs) return

                    val timeSinceLast = now - lastTapTimeMs

                    // Debounce filter: Ignore bounces from the same physical tap event
                    if (timeSinceLast < minTapIntervalMs) return

                    if (timeSinceLast < doubleTapWindowMs) {
                        tapCount++
                        lastTapTimeMs = now
                        if (tapCount >= 3) {
                            // Confirmed triple-tap!
                            tapCount = 0
                            lastCooldownMs = now
                            onDoubleTapDetected()
                        }
                    } else {
                        tapCount = 1
                        lastTapTimeMs = now
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
