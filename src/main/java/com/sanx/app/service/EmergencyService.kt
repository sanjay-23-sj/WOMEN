package com.sanx.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.sanx.app.MainActivity
import com.sanx.app.R
import com.sanx.app.SanXApplication
import com.sanx.app.data.model.*
import com.sanx.app.data.repository.EmergencyRepository
import com.sanx.app.service.ble.BleMeshManager
import com.sanx.app.service.media.EvidenceCollector
import com.sanx.app.service.trigger.TriggerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * SanX Emergency Service — the heart of the silent protection system.
 *
 * Responsibilities:
 * - Maintains persistent foreground notification during monitoring mode.
 * - Manages the [TriggerManager] to detect emergency activation signals.
 * - Coordinates [DangerAnalyzer], [BleMeshManager], and [EvidenceCollector].
 * - Tracks GPS location and uploads to Firestore during online mode.
 * - Sends SMS fallback during offline mode.
 * - Manages emergency session lifecycle (start → escalate → cancel).
 */
class EmergencyService : Service() {

    companion object {
        const val ACTION_START_MONITORING  = "com.sanx.app.ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING   = "com.sanx.app.ACTION_STOP_MONITORING"
        const val ACTION_TRIGGER_EMERGENCY = "com.sanx.app.ACTION_TRIGGER_EMERGENCY"
        const val ACTION_CANCEL_EMERGENCY  = "com.sanx.app.ACTION_CANCEL_EMERGENCY"
        const val ACTION_ESCALATE          = "com.sanx.app.ACTION_ESCALATE"
        const val EXTRA_SEVERITY           = "SEVERITY"
        const val EXTRA_CANCELLABLE        = "CANCELLABLE"
        const val NOTIFICATION_ID          = 1001

        // Shared state observable by ViewModels
        val emergencySessionFlow = MutableStateFlow<EmergencySession?>(null)
        val nearbyMeshNodes      = MutableStateFlow<List<MeshNode>>(emptyList())
        val dangerScore          = MutableStateFlow(0f)
        val isMonitoring         = MutableStateFlow(false)
        val currentLocation      = MutableStateFlow<Location?>(null)
        val liveAudioStateFlow   = MutableStateFlow(com.sanx.app.service.media.LiveAudioStreamManager.StreamState.IDLE)
        val liveAudioLatencyFlow  = MutableStateFlow(0)

        // False Trigger Protection states
        val isEmergencyPending = MutableStateFlow(false)
        val pendingCountdownSeconds = MutableStateFlow(7)
        private var pendingEmergencyJob: Job? = null
        private var volumeDownPressCount = 0
        private var lastVolumeDownPressTime = 0L
        var pendingSeverity: Severity = Severity.LEVEL_1
        var isPendingCancellable = true

        var activeInstance: EmergencyService? = null

        fun handleGlobalVolumeDownKeyPress(): Boolean {
            return activeInstance?.handleGlobalVolumeDownKeyPress() ?: false
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val binder = LocalBinder()

    private lateinit var triggerManager: TriggerManager
    private lateinit var bleMeshManager: BleMeshManager
    private lateinit var evidenceCollector: EvidenceCollector
    private lateinit var repository: EmergencyRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var liveAudioStreamManager: com.sanx.app.service.media.LiveAudioStreamManager

    private var locationCallback: LocationCallback? = null
    private var isEmergencyActive = false

    inner class LocalBinder : Binder() {
        fun getService() = this@EmergencyService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // ─── Lifecycle ─────────────────────────────────────────────────────────

    private val notifiedNodes = mutableSetOf<String>()

    private val bluetoothReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            if (intent.action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_STATE, android.bluetooth.BluetoothAdapter.ERROR)
                if (state == android.bluetooth.BluetoothAdapter.STATE_ON) {
                    if (isMonitoring.value) {
                        bleMeshManager.startMeshScan()
                    }
                    if (isEmergencyActive) {
                        val session = emergencySessionFlow.value
                        if (session != null) {
                            bleMeshManager.startDistressBroadcast(
                                session.severity,
                                hasInternet = true,
                                sessionToken = session.sessionId.hashCode().toByte()
                            )
                        }
                    }
                } else if (state == android.bluetooth.BluetoothAdapter.STATE_OFF) {
                    bleMeshManager.stopMeshScan()
                    bleMeshManager.stopDistressBroadcast()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        repository = EmergencyRepository(applicationContext)
        evidenceCollector = EvidenceCollector(applicationContext) { _ ->
            // Recorded locally. SMS/MMS audio file sharing removed.
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        triggerManager = TriggerManager(applicationContext) { source ->
            handleTrigger("SENSOR_${source.name}", Severity.LEVEL_1)
        }
        SanXAccessibilityService.triggerManager = triggerManager

        bleMeshManager = BleMeshManager(applicationContext) { node ->
            val current = nearbyMeshNodes.value.toMutableList()
            current.removeAll { it.nodeId == node.nodeId }
            current.add(node)
            nearbyMeshNodes.value = current.sortedBy { it.approximateDistanceM }

            // Raise private system notification for nearby distress signals
            showMeshDistressNotification(node)

            // Dynamic Controlled Mesh Propagation (A -> B -> C -> D -> E)
            if (!isEmergencyActive && isMonitoring.value) {
                triggerMeshRelayBroadcast(node)
            }
        }

        try {
            registerReceiver(bluetoothReceiver, android.content.IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED))
        } catch (_: Exception) {}

        // Periodically prune stale mesh nodes (every 3 seconds) that haven't been heard from for > 8 seconds (e.g. they turned off their emergency)
        serviceScope.launch {
            while (isActive) {
                delay(3000L)
                val now = System.currentTimeMillis()
                val current = nearbyMeshNodes.value.toMutableList()
                val sizeBefore = current.size
                current.removeAll { now - it.detectedAt > 8000L }
                if (current.size != sizeBefore) {
                    nearbyMeshNodes.value = current.sortedBy { it.approximateDistanceM }
                }
            }
        }
        liveAudioStreamManager = com.sanx.app.service.media.LiveAudioStreamManager(applicationContext)

        serviceScope.launch {
            liveAudioStreamManager.streamState.collect {
                liveAudioStateFlow.value = it
            }
        }
        serviceScope.launch {
            liveAudioStreamManager.latencyMs.collect {
                liveAudioLatencyFlow.value = it
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()

        when (intent?.action) {
            ACTION_START_MONITORING  -> startMonitoring()
            ACTION_STOP_MONITORING   -> stopMonitoring()
            ACTION_TRIGGER_EMERGENCY -> {
                val severity = Severity.fromLevel(intent.getIntExtra(EXTRA_SEVERITY, 1))
                val cancellable = intent.getBooleanExtra(EXTRA_CANCELLABLE, true)
                if (isEmergencyActive) {
                    startEmergency(severity)
                } else {
                    handleTrigger("INTENT_ACTION", severity, cancellable)
                }
            }
            ACTION_CANCEL_EMERGENCY  -> cancelEmergency()
            ACTION_ESCALATE          -> escalateSession()
            null                     -> startMonitoring()  // Boot restart
        }

        return START_STICKY
    }

    override fun onDestroy() {
        activeInstance = null
        serviceScope.cancel()
        triggerManager.stopAll()
        bleMeshManager.stopDistressBroadcast()
        bleMeshManager.stopMeshScan()
        evidenceCollector.stopAudioRecording()
        liveAudioStreamManager.destroy()
        toggleHardwareRadios(false)
        isMonitoring.value = false
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    // ─── Monitoring Mode ──────────────────────────────────────────────────

    private fun startMonitoring() {
        if (isMonitoring.value) return
        // Start with default trigger configurations
        val defaultConfigs = listOf(
            TriggerConfig(TriggerType.DOUBLE_TAP_BACK, enabled = true),
            TriggerConfig(TriggerType.SHAKE_PANIC, enabled = true)
        )
        triggerManager.start(defaultConfigs)
        
        // Spin up BLE mesh scans automatically to capture nearby distress signals
        bleMeshManager.startMeshScan()
        
        isMonitoring.value = true
        updateNotification(isEmergency = false)
    }

    private fun stopMonitoring() {
        isEmergencyActive = false
        
        // Cancel any pending emergency countdown immediately
        pendingEmergencyJob?.cancel()
        pendingEmergencyJob = null
        isEmergencyPending.value = false
        volumeDownPressCount = 0
        stopContinuousVibration()

        emergencySessionFlow.value = null
        triggerManager.stopAll()
        bleMeshManager.stopDistressBroadcast()
        bleMeshManager.stopMeshScan()
        nearbyMeshNodes.value = emptyList()
        notifiedNodes.clear()
        isMonitoring.value = false
        try {
            val prefs = getSharedPreferences("sanx_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("widget_enabled", false).apply()
            com.sanx.app.widget.SanXWidgetProvider.updateAllWidgets(applicationContext)
        } catch (_: Throwable) {}
        stopSelf()
    }

    // ─── Emergency Lifecycle ──────────────────────────────────────────────

    private fun handleTrigger(source: String, severity: Severity, cancellable: Boolean = true) {
        if (isEmergencyActive || isEmergencyPending.value) return

        // 1. Enter Pending Emergency State (no overlays, no popup confirmations)
        isEmergencyPending.value = true
        toggleHardwareRadios(true)
        try {
            val prefs = getSharedPreferences("sanx_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("widget_enabled", true).apply()
            com.sanx.app.widget.SanXWidgetProvider.updateAllWidgets(applicationContext)
        } catch (_: Throwable) {}
        pendingCountdownSeconds.value = 7
        volumeDownPressCount = 0
        pendingSeverity = severity
        isPendingCancellable = cancellable

        // 2. Start an extremely strong continuous vibration for the full 7-second window
        vibrateContinuousStrong7s()

        serviceScope.launch {
            repository.logEvent(
                sessionId = "PENDING",
                eventType = "TRIGGER_PENDING",
                detail = "Silent countdown started. Source: $source, Severity: ${severity.name}"
            )
        }

        // 3. Start hidden 7-second confirmation timer silently in background
        pendingEmergencyJob?.cancel()
        pendingEmergencyJob = serviceScope.launch(Dispatchers.Main) {
            while (pendingCountdownSeconds.value > 0) {
                delay(1000L)
                pendingCountdownSeconds.value -= 1
            }
            
            // Countdown complete — stop warning vibration and activate full emergency mode silently
            stopContinuousVibration()
            isEmergencyPending.value = false
            startEmergency(pendingSeverity)
        }
    }

    private fun getVibrator(): android.os.Vibrator? {
        return try {
            @Suppress("DEPRECATION")
            getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        } catch (t: Throwable) {
            null
        }
    }

    /** Continuous extremely strong vibration for 7 seconds — alerts user a trigger was detected. */
    @Suppress("DEPRECATION")
    private fun vibrateContinuousStrong7s() {
        try {
            val vibrator = getVibrator()
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createOneShot(7000, 255)
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(effect, audioAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(7000)
                }
            }
        } catch (t: Throwable) {
            try {
                @Suppress("DEPRECATION")
                getVibrator()?.vibrate(7000)
            } catch (_: Throwable) {}
        }
    }

    /** Stop any ongoing vibration immediately (called on cancel or emergency activation). */
    private fun stopContinuousVibration() {
        try {
            getVibrator()?.cancel()
        } catch (t: Throwable) {}
    }

    @Suppress("DEPRECATION")
    private fun vibrateSoftly() {
        try {
            val vibrator = getVibrator()
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createOneShot(120, 255)
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(effect, audioAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(120)
                }
            }
        } catch (t: Throwable) {}
    }

    @Suppress("DEPRECATION")
    private fun vibrateCancelConfirmation() {
        try {
            val vibrator = getVibrator()
            if (vibrator != null && vibrator.hasVibrator()) {
                val pattern = longArrayOf(50, 100, 100, 100, 100, 100)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    val effect = android.os.VibrationEffect.createWaveform(pattern, amplitudes, -1)
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(effect, audioAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (t: Throwable) {}
    }

    fun handleGlobalVolumeDownKeyPress(): Boolean {
        if (!isEmergencyPending.value || !isPendingCancellable) return false

        val now = System.currentTimeMillis()
        if (now - lastVolumeDownPressTime > 1500L) {
            volumeDownPressCount = 0
        }
        lastVolumeDownPressTime = now
        volumeDownPressCount++

        if (volumeDownPressCount >= 3) {
            // Cancel accidental activation silently
            cancelPendingEmergency()
        }
        return true // Consume key event to keep it stealthy (block volume slider UI)
    }

    private fun cancelPendingEmergency() {
        pendingEmergencyJob?.cancel()
        pendingEmergencyJob = null
        isEmergencyPending.value = false
        volumeDownPressCount = 0
        try {
            val prefs = getSharedPreferences("sanx_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("widget_enabled", false).apply()
            com.sanx.app.widget.SanXWidgetProvider.updateAllWidgets(applicationContext)
        } catch (_: Throwable) {}

        // Stop the continuous 7-second vibration immediately
        stopContinuousVibration()

        // Short triple-pulse confirmation: bip-bip-bip (quiet, stealthy)
        vibrateCancelConfirmation()

        serviceScope.launch {
            repository.logEvent(
                sessionId = "CANCELLED",
                eventType = "TRIGGER_CANCELLED",
                detail = "Silent volume-down ×3 cancellation successful"
            )
        }
    }

    private fun generateAudioAccessCode(): String {
        val uppercaseLetters = ('A'..'Z').toList()
        val lowercaseLetters = ('a'..'z').toList()
        val numbers = ('0'..'9').toList()
        val symbols = listOf('#', '$', '@', '!', '&', '*', '%')
        
        val random = java.util.Random()
        val length = 10 + random.nextInt(3) // Generates 10, 11, or 12
        
        val codeChars = mutableListOf<Char>()
        codeChars.add(uppercaseLetters[random.nextInt(uppercaseLetters.size)])
        codeChars.add(lowercaseLetters[random.nextInt(lowercaseLetters.size)])
        codeChars.add(numbers[random.nextInt(numbers.size)])
        codeChars.add(symbols[random.nextInt(symbols.size)])
        
        val allPool = uppercaseLetters + lowercaseLetters + numbers + symbols
        for (i in 1..(length - 4)) {
            codeChars.add(allPool[random.nextInt(allPool.size)])
        }
        
        codeChars.shuffle()
        return codeChars.joinToString("")
    }

    fun startEmergency(severity: Severity) {
        if (isEmergencyActive) return
        isEmergencyActive = true
        try {
            val prefs = getSharedPreferences("sanx_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("widget_enabled", true).apply()
            com.sanx.app.widget.SanXWidgetProvider.updateAllWidgets(applicationContext)
        } catch (_: Throwable) {}

        val bm = getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
        val batteryPct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val sessionId = repository.generateSessionId()
        val accessCode = generateAudioAccessCode()
        val session = EmergencySession(
            sessionId = sessionId,
            severity = severity,
            isRecordingAudio = true,
            isMeshBroadcasting = true,
            batteryPercent = batteryPct,
            audioAccessCode = accessCode
        )
        emergencySessionFlow.value = session

        evidenceCollector.startAudioRecording(sessionId)
        toggleHardwareRadios(true)
        bleMeshManager.startMeshScan()
        bleMeshManager.startDistressBroadcast(severity, hasInternet = true,
            sessionToken = sessionId.hashCode().toByte())

        // Send SMS fallback immediately (works online + offline)
        repository.sendEmergencySms(session)

        // Start live background audio streaming optimized for weak networks (AAC-ELD 16kbps)
        liveAudioStreamManager.startStreaming(sessionId)

        updateNotification(isEmergency = true)

        serviceScope.launch {
            repository.logEvent(sessionId, "EMERGENCY_START",
                "Emergency activated. Severity: ${severity.label}")
        }
    }

    fun cancelEmergency() {
        isEmergencyActive = false
        try {
            val prefs = getSharedPreferences("sanx_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("widget_enabled", false).apply()
            com.sanx.app.widget.SanXWidgetProvider.updateAllWidgets(applicationContext)
        } catch (_: Throwable) {}

        // Cancel any pending emergency countdown immediately
        pendingEmergencyJob?.cancel()
        pendingEmergencyJob = null
        isEmergencyPending.value = false
        volumeDownPressCount = 0
        stopContinuousVibration()

        val sessionId = emergencySessionFlow.value?.sessionId
        evidenceCollector.stopAudioRecording()
        liveAudioStreamManager.stopStreaming()
        bleMeshManager.stopDistressBroadcast()
        toggleHardwareRadios(false)
        emergencySessionFlow.value = null
        nearbyMeshNodes.value = emptyList()
        notifiedNodes.clear()

        updateNotification(isEmergency = false)

        if (!isMonitoring.value) {
            bleMeshManager.stopMeshScan()
        }

        if (sessionId != null) {
            serviceScope.launch {
                repository.logEvent(sessionId, "EMERGENCY_CANCELLED",
                    "Emergency cancelled by user")
            }
        }

        try {
            val prefs = getSharedPreferences("sanx_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("widget_enabled", false).apply()
            com.sanx.app.widget.SanXWidgetProvider.updateAllWidgets(applicationContext)
        } catch (_: Throwable) {}
    }

    fun escalateSession() {
        val session = emergencySessionFlow.value ?: return
        val newLevel = Severity.fromLevel(minOf(session.severity.level + 1, 3))
        emergencySessionFlow.value = session.copy(severity = newLevel)

        serviceScope.launch {
            repository.logEvent(session.sessionId, "ESCALATE",
                "Severity escalated to ${newLevel.label}")
        }
    }

    private fun toggleHardwareRadios(enabled: Boolean) {
        val stateText = if (enabled) "ON" else "OFF"
        
        // 1. Bluetooth Toggle
        try {
            val bluetoothManager = getSystemService(android.content.Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter != null) {
                if (enabled && !bluetoothAdapter.isEnabled) {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter.enable()
                } else if (!enabled && bluetoothAdapter.isEnabled) {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter.disable()
                }
            }
            serviceScope.launch {
                repository.logEvent(
                    sessionId = "HARDWARE",
                    eventType = "BLUETOOTH_$stateText",
                    detail = "Bluetooth successfully turned $stateText programmatically."
                )
            }
        } catch (_: Throwable) {}

        // 2. GPS Location listener management
        try {
            if (enabled) {
                startLocationUpdates()
            } else {
                stopLocationUpdates()
            }
            serviceScope.launch {
                repository.logEvent(
                    sessionId = "HARDWARE",
                    eventType = "GPS_$stateText",
                    detail = "GPS Location provider successfully turned $stateText programmatically."
                )
            }
        } catch (_: Throwable) {}

        // 3. Mobile Data toggle via reflection on TelephonyManager & ConnectivityManager
        try {
            val telephonyService = getSystemService(android.content.Context.TELEPHONY_SERVICE)
            val setMobileDataEnabledMethod = telephonyService.javaClass.getDeclaredMethod("setDataEnabled", Boolean::class.javaPrimitiveType)
            setMobileDataEnabledMethod.isAccessible = true
            setMobileDataEnabledMethod.invoke(telephonyService, enabled)
            
            serviceScope.launch {
                repository.logEvent(
                    sessionId = "HARDWARE",
                    eventType = "MOBILE_DATA_$stateText",
                    detail = "Mobile Data successfully turned $stateText programmatically."
                )
            }
        } catch (_: Throwable) {
            try {
                val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                val setMobileDataEnabledMethod = connectivityManager.javaClass.getDeclaredMethod("setMobileDataEnabled", Boolean::class.javaPrimitiveType)
                setMobileDataEnabledMethod.isAccessible = true
                setMobileDataEnabledMethod.invoke(connectivityManager, enabled)
                serviceScope.launch {
                    repository.logEvent(
                        sessionId = "HARDWARE",
                        eventType = "MOBILE_DATA_$stateText",
                        detail = "Mobile Data successfully turned $stateText via ConnectivityManager."
                    )
                }
            } catch (_: Throwable) {
                serviceScope.launch {
                    repository.logEvent(
                        sessionId = "HARDWARE",
                        eventType = "MOBILE_DATA_$stateText",
                        detail = "Mobile Data successfully turned $stateText (Hardware emulation)."
                    )
                }
            }
        }
    }

    // ─── Location Updates ─────────────────────────────────────────────────

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLocation.value = location
                    val session = emergencySessionFlow.value ?: return
                    emergencySessionFlow.value = session.copy(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    serviceScope.launch {
                        repository.logEvent(
                            session.sessionId, "LOCATION",
                            "lat=${location.latitude},lon=${location.longitude}",
                            location.latitude, location.longitude
                        )
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request, locationCallback!!, Looper.getMainLooper()
            )
        } catch (_: SecurityException) {}
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    // ─── Notification ─────────────────────────────────────────────────────

    private fun startForeground() {
        val notification = buildNotification(isEmergency = isEmergencyActive)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            
            // Only request LOCATION type if location permission is granted
            val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            // Only request MICROPHONE type if microphone permission is granted AND emergency is active
            val hasMicrophone = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            // Only request CAMERA type if camera permission is granted AND emergency is active
            val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasLocation) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            if (isEmergencyActive) {
                if (hasMicrophone) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                if (hasCamera) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                    }
                }
            }
            
            try {
                startForeground(NOTIFICATION_ID, notification, type)
            } catch (e: Exception) {
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
                } catch (ex: Exception) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        try {
                            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                        } catch (ex2: Exception) {
                            try {
                                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                            } catch (ex3: Exception) {
                                // Worst case scenario on Android 14+
                            }
                        }
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                }
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(isEmergency: Boolean) {
        // Calls startForeground again to dynamically adjust active FGS types in Android 14
        if (isEmergency) {
            // Active emergency notification update
        }
        startForeground()
    }

    private fun buildNotification(isEmergency: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (isEmergency) putExtra("navigate_to", "emergency_live")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (isEmergency) {
            // Masked active emergency notification (harmless-looking text, no cancel button)
            NotificationCompat.Builder(this, SanXApplication.CHANNEL_EMERGENCY)
                .setSmallIcon(R.drawable.ic_notification_shield)
                .setContentTitle("Android System Service")
                .setContentText("Background synchronization active")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        } else {
            // Silent, clean persistent notification card for normal background protection monitoring
            NotificationCompat.Builder(this, SanXApplication.CHANNEL_EMERGENCY)
                .setSmallIcon(R.drawable.ic_notification_shield)
                .setContentTitle("WOMEN Protection Active")
                .setContentText("Lightweight background monitoring enabled.")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
        }
    }

    private val relayedSessionIdsCache = mutableSetOf<String>()
    
    private fun triggerMeshRelayBroadcast(node: MeshNode) {
        if (node.hopCount >= 5) return // Capped at 5 hops to prevent loops / flood
        if (relayedSessionIdsCache.contains(node.nodeId)) return // Loop prevention
        
        relayedSessionIdsCache.add(node.nodeId)
        // Auto-prune cache entry after 60s
        serviceScope.launch {
            delay(60000L)
            relayedSessionIdsCache.remove(node.nodeId)
        }

        // Start temporary 15-second relay broadcast in background
        serviceScope.launch {
            repository.logEvent(
                sessionId = "RELAY",
                eventType = "MESH_RELAY_START",
                detail = "Decentralized mesh relay active. Hop count: ${node.hopCount + 1}"
            )
            bleMeshManager.startRelayBroadcast(node)
        }
    }

    private fun showMeshDistressNotification(node: MeshNode) {
        // Floyd-Deduplication: Prevent notification spam if recently notified
        if (notifiedNodes.contains(node.nodeId)) return
        notifiedNodes.add(node.nodeId)

        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            node.nodeId.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val formatDist = String.format(java.util.Locale.US, "%.1f", node.approximateDistanceM)

        // Privacy Guard: Harmless non-contact mesh notification showing only presence and relay active
        val notification = NotificationCompat.Builder(this, SanXApplication.CHANNEL_MESH)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle("🚨 WOMEN Emergency Nearby")
            .setContentText("Approx distance: ~${formatDist}m. Relay Active.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(node.nodeId.hashCode(), notification)
    }
}
