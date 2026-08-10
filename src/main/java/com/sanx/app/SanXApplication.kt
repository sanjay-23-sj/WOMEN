package com.sanx.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.sanx.app.data.local.AppDatabase

/**
 * SanX Application class.
 * Initializes the Room database and notification channels on startup.
 * Kept minimal to ensure fast boot speed on low-end devices.
 */
class SanXApplication : Application() {

    companion object {
        const val CHANNEL_EMERGENCY = "sanx_emergency"
        const val CHANNEL_MESH = "sanx_mesh"
        const val CHANNEL_ALERTS = "sanx_alerts"

        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        initDatabase()
        createNotificationChannels()
    }

    private fun initDatabase() {
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "sanx_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Emergency protection — default priority, non-dismissible (elevated for background vibration privileges)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_EMERGENCY,
                    "Emergency Protection",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Persistent emergency protection indicator"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 100)
                }
            )

            // Mesh network relay alerts
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MESH,
                    "Mesh Relay",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Nearby emergency relay alerts"
                    enableLights(true)
                    lightColor = 0xFFFF4545.toInt()
                    enableVibration(true)
                }
            )

            // General safety alerts
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    "Safety Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "SanX system alerts and status updates"
                }
            )
        }
    }
}
