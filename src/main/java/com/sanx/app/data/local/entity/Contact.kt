package com.sanx.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Trusted contact stored locally in Room.
 * Contacts are notified during active emergency sessions.
 */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val priority: Int = 1,              // 1 = Primary, 2 = Secondary, 3 = Tertiary
    val notifyViaSms: Boolean = true,
    val notifyViaPush: Boolean = true,
    val shareLocation: Boolean = true,
    val shareAudio: Boolean = false,    // Requires explicit opt-in
    val shareCamera: Boolean = false,   // Requires explicit opt-in
    val addedAt: Long = System.currentTimeMillis()
)
