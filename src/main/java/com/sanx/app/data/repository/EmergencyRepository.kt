package com.sanx.app.data.repository

import android.content.Context
import android.telephony.SmsManager
import com.sanx.app.SanXApplication
import com.sanx.app.data.local.entity.Contact
import com.sanx.app.data.local.entity.EmergencyLog
import com.sanx.app.data.model.EmergencySession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Central repository coordinating emergency data operations.
 * Acts as the single source of truth between the database, services, and ViewModels.
 */
class EmergencyRepository(private val context: Context) {

    private val db = SanXApplication.database
    private val logDao = db.logDao()
    private val contactDao = db.contactDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    // ─── Contacts ─────────────────────────────────────────────────────────────

    fun getAllContacts(): Flow<List<Contact>> = contactDao.getAllContacts()

    suspend fun addContact(contact: Contact): Long {
        if (contact.priority == 1) {
            demoteOtherPrimaries()
        }
        return contactDao.insert(contact)
    }

    suspend fun updateContact(contact: Contact) {
        if (contact.priority == 1) {
            demoteOtherPrimaries()
        }
        contactDao.update(contact)
    }

    suspend fun removeContact(contact: Contact) = contactDao.delete(contact)

    private suspend fun demoteOtherPrimaries() {
        try {
            val list = contactDao.getAllContacts().first()
            list.filter { it.priority == 1 }.forEach { existingPrimary ->
                contactDao.update(existingPrimary.copy(priority = 2))
            }
        } catch (_: Exception) {}
    }

    // ─── Logs ─────────────────────────────────────────────────────────────────

    fun getSessionLogs(sessionId: String): Flow<List<EmergencyLog>> =
        logDao.getLogsForSession(sessionId)

    fun getRecentLogs(): Flow<List<EmergencyLog>> = logDao.getRecentLogs()

    suspend fun logEvent(sessionId: String, eventType: String, detail: String,
                         lat: Double = 0.0, lon: Double = 0.0, severity: Int = 1) {
        logDao.insert(
            EmergencyLog(
                sessionId = sessionId,
                eventType = eventType,
                detail = detail,
                latitude = lat,
                longitude = lon,
                severityLevel = severity
            )
        )
    }

    // ─── SMS Fallback ─────────────────────────────────────────────────────────

    /**
     * Sends emergency SMS to all trusted contacts with SMS enabled.
     * Runs silently on a background thread without triggering any UI.
     */
    fun sendEmergencySms(session: EmergencySession) {
        scope.launch {
            try {
                val prefs = context.getSharedPreferences("sanx_prefs", Context.MODE_PRIVATE)
                val subId = prefs.getInt("user_sub_id", -1)

                val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                val batteryPct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)

                val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                
                // Fetch the last known location immediately
                fusedClient.lastLocation.addOnCompleteListener { task ->
                    val location = if (task.isSuccessful) task.result else null
                    val lat = location?.latitude ?: com.sanx.app.service.EmergencyService.currentLocation.value?.latitude ?: 0.0
                    val lon = location?.longitude ?: com.sanx.app.service.EmergencyService.currentLocation.value?.longitude ?: 0.0
                    
                    val updatedSession = session.copy(
                        latitude = lat,
                        longitude = lon,
                        batteryPercent = batteryPct
                    )
                    
                    scope.launch {
                        val contacts = db.contactDao().getAllContacts()
                        try {
                            val list = contacts.first()
                            list.filter { it.notifyViaSms }.forEach { contact ->
                                sendSms(contact.phoneNumber, buildSmsMessage(updatedSession), subId)
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                try {
                    val prefs = context.getSharedPreferences("sanx_prefs", Context.MODE_PRIVATE)
                    val subId = prefs.getInt("user_sub_id", -1)

                    val contacts = db.contactDao().getAllContacts()
                    val list = contacts.first()
                    list.filter { it.notifyViaSms }.forEach { contact ->
                        sendSms(contact.phoneNumber, buildSmsMessage(session), subId)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun buildSmsMessage(session: EmergencySession): String {
        val mapsLink = if (session.latitude != 0.0)
            "https://maps.google.com/?q=${session.latitude},${session.longitude}"
        else "Location details pending"
        
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val dateStr = dateFormat.format(java.util.Date(session.startedAt))
        val timeStr = timeFormat.format(java.util.Date(session.startedAt))

        return "WOMEN Emergency Activated\n\n" +
                "Date: $dateStr\n" +
                "Time: $timeStr\n" +
                "Battery: ${session.batteryPercent}%\n\n" +
                "Location:\n$mapsLink\n\n" +
                "Go and hear live audio.\n\n" +
                "Emergency Audio Access Code:\n${session.audioAccessCode}"
    }

    private fun sendSms(number: String, message: String, subId: Int) {
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val systemSms = context.getSystemService(android.telephony.SmsManager::class.java)
                if (subId != -1) systemSms.createForSubscriptionId(subId) else systemSms
            } else {
                @Suppress("DEPRECATION")
                if (subId != -1) android.telephony.SmsManager.getSmsManagerForSubscriptionId(subId)
                else android.telephony.SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(number, null, parts, null, null)
        } catch (_: Exception) { }
    }

    // ─── Session ID Generator ─────────────────────────────────────────────────

    fun generateSessionId(): String = UUID.randomUUID().toString().take(12).uppercase()
}
