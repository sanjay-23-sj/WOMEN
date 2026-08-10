package com.sanx.app.data.model

/** Represents the danger severity level of an emergency session. */
enum class Severity(val level: Int, val label: String) {
    LEVEL_1(1, "Silent Alert"),
    LEVEL_2(2, "Community Alert"),
    LEVEL_3(3, "Critical Broadcast");

    companion object {
        fun fromLevel(level: Int): Severity =
            entries.firstOrNull { it.level == level } ?: LEVEL_1
    }
}

/** Aggregated real-time data from device sensors. */
data class SensorSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val accX: Float = 0f,
    val accY: Float = 0f,
    val accZ: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val magnitude: Float = 0f,      // sqrt(x²+y²+z²)
    val dangerScore: Float = 0f     // 0.0 – 1.0
)

/** Live state of an active emergency session. */
data class EmergencySession(
    val sessionId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val severity: Severity = Severity.LEVEL_1,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isOnline: Boolean = true,
    val isRecordingAudio: Boolean = false,
    val isRecordingCamera: Boolean = false,
    val isMeshBroadcasting: Boolean = false,
    val batteryPercent: Int = 100,
    val dangerScore: Float = 0f,
    val relayCount: Int = 0,
    val audioAccessCode: String = ""
)

/** Represents a nearby BLE mesh signal detected from another SanX device. */
data class MeshNode(
    val nodeId: String,             // Randomized, never links to identity
    val approximateDistanceM: Float,
    val rssi: Int,
    val severity: Severity,
    val detectedAt: Long = System.currentTimeMillis(),
    val isRelayCapable: Boolean = false,
    val hopCount: Int = 0,
    val isRelayed: Boolean = false,
    val emergencyId: String = ""
)

/** Configuration settings for a single emergency trigger. */
data class TriggerConfig(
    val type: TriggerType,
    val enabled: Boolean = true,
    val sensitivity: TriggerSensitivity = TriggerSensitivity.MEDIUM,
    val customPhrase: String = ""
)

enum class TriggerType {
    DOUBLE_TAP_BACK,
    SHAKE_PANIC
}

enum class TriggerSensitivity {
    LOW, MEDIUM, HIGH
}
