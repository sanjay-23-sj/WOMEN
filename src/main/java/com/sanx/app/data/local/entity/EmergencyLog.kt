package com.sanx.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Emergency log entry persisted locally in Room.
 * Stores each event during an active emergency session.
 */
@Entity(tableName = "emergency_logs")
data class EmergencyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,      // "TRIGGER", "LOCATION", "RELAY", "AUDIO", "DANGER_SCORE"
    val detail: String,         // JSON-encoded detail payload
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val severityLevel: Int = 1  // 1 = Silent, 2 = Community, 3 = Critical
)
