package com.sanx.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanx.app.data.local.entity.Contact
import com.sanx.app.data.model.*
import com.sanx.app.data.repository.EmergencyRepository
import com.sanx.app.service.EmergencyService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Main ViewModel shared across all non-emergency screens.
 * Observes EmergencyService state flows and exposes them to the UI.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EmergencyRepository(application)
    private val context get() = getApplication<Application>()

    // ─── Service-backed state ──────────────────────────────────────────────────
    val isMonitoring: StateFlow<Boolean>          = EmergencyService.isMonitoring
    val emergencySession: StateFlow<EmergencySession?> = EmergencyService.emergencySessionFlow
    val nearbyMeshNodes: StateFlow<List<MeshNode>> = EmergencyService.nearbyMeshNodes
    val dangerScore: StateFlow<Float>             = EmergencyService.dangerScore

    // ─── Database-backed state ─────────────────────────────────────────────────
    val contacts: StateFlow<List<Contact>> = repository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs = repository.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Trigger configuration (DataStore-backed in production) ──────────────
    private val _triggerConfigs = MutableStateFlow(defaultTriggerConfigs())
    val triggerConfigs: StateFlow<List<TriggerConfig>> = _triggerConfigs.asStateFlow()

    // ─── Ghost Mode ───────────────────────────────────────────────────────────
    private val _ghostModeEnabled = MutableStateFlow(false)
    val ghostModeEnabled: StateFlow<Boolean> = _ghostModeEnabled.asStateFlow()

    private val _ghostDisguise = MutableStateFlow(GhostDisguise.CALCULATOR)
    val ghostDisguise: StateFlow<GhostDisguise> = _ghostDisguise.asStateFlow()

    private val _ghostPin = MutableStateFlow("9999")
    val ghostPin: StateFlow<String> = _ghostPin.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(false)
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    // ─── Service control ──────────────────────────────────────────────────────

    fun startProtection() {
        sendServiceAction(EmergencyService.ACTION_START_MONITORING)
    }

    fun stopProtection() {
        sendServiceAction(EmergencyService.ACTION_STOP_MONITORING)
    }

    fun triggerEmergency(severity: Severity = Severity.LEVEL_1) {
        sendServiceAction(EmergencyService.ACTION_TRIGGER_EMERGENCY,
            EmergencyService.EXTRA_SEVERITY to severity.level)
    }

    fun cancelEmergency() {
        sendServiceAction(EmergencyService.ACTION_CANCEL_EMERGENCY)
    }

    fun escalateEmergency() {
        sendServiceAction(EmergencyService.ACTION_ESCALATE)
    }

    // ─── Contacts CRUD ────────────────────────────────────────────────────────

    fun addContact(contact: Contact) {
        viewModelScope.launch { repository.addContact(contact) }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch { repository.updateContact(contact) }
    }

    fun removeContact(contact: Contact) {
        viewModelScope.launch { repository.removeContact(contact) }
    }

    // ─── Trigger config ───────────────────────────────────────────────────────

    fun updateTrigger(type: TriggerType, enabled: Boolean, sensitivity: TriggerSensitivity) {
        _triggerConfigs.value = _triggerConfigs.value.map {
            if (it.type == type) it.copy(enabled = enabled, sensitivity = sensitivity) else it
        }
    }

    fun setGhostMode(enabled: Boolean) { _ghostModeEnabled.value = enabled }
    fun setGhostDisguise(disguise: GhostDisguise) { _ghostDisguise.value = disguise }
    fun setGhostPin(pin: String) { _ghostPin.value = pin }
    fun setAppLockEnabled(enabled: Boolean) { _appLockEnabled.value = enabled }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun sendServiceAction(action: String, vararg extras: Pair<String, Int>) {
        val intent = Intent(context, EmergencyService::class.java).apply {
            this.action = action
            extras.forEach { (key, value) -> putExtra(key, value) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun defaultTriggerConfigs() = listOf(
        TriggerConfig(TriggerType.DOUBLE_TAP_BACK, enabled = true, sensitivity = TriggerSensitivity.MEDIUM),
        TriggerConfig(TriggerType.SHAKE_PANIC, enabled = true, sensitivity = TriggerSensitivity.MEDIUM)
    )
}

enum class GhostDisguise { CALCULATOR, LOCK_SCREEN }
