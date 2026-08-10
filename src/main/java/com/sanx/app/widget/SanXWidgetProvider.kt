package com.sanx.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.sanx.app.R
import com.sanx.app.service.EmergencyService

class SanXWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE) {
            val prefs = context.getSharedPreferences("sanx_prefs", Context.MODE_PRIVATE)
            val currentState = prefs.getBoolean("widget_enabled", false)
            val newState = !currentState

            prefs.edit().putBoolean("widget_enabled", newState).apply()

            if (newState) {
                // Trigger the alert (non-cancellable, 7 seconds vibration first!)
                val serviceIntent = Intent(context, EmergencyService::class.java).apply {
                    action = EmergencyService.ACTION_TRIGGER_EMERGENCY
                    putExtra(EmergencyService.EXTRA_SEVERITY, 1)
                    putExtra(EmergencyService.EXTRA_CANCELLABLE, false)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (_: Exception) {}
            } else {
                // If they toggle it off manually before alert completes, let's reset it or cancel it
                val serviceIntent = Intent(context, EmergencyService::class.java).apply {
                    action = EmergencyService.ACTION_CANCEL_EMERGENCY
                }
                try {
                    context.startService(serviceIntent)
                } catch (_: Exception) {}
            }

            // Update all widgets
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.sanx.app.ACTION_WIDGET_TOGGLE"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            val prefs = context.getSharedPreferences("sanx_prefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("widget_enabled", false)

            // Update Toggle Switch drawable
            val toggleIcon = if (isEnabled) R.drawable.ic_toggle_on else R.drawable.ic_toggle_off
            views.setImageViewResource(R.id.widget_toggle, toggleIcon)

            // Update Status text for realism
            views.setTextViewText(R.id.widget_status, if (isEnabled) "Active" else "Optimized")

            // Bind click action
            val intent = Intent(context, SanXWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_toggle, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SanXWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
