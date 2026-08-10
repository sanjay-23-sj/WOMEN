package com.sanx.app.service.ble

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.sanx.app.data.model.MeshNode
import com.sanx.app.data.model.Severity
import java.util.UUID
import kotlin.math.pow

/**
 * BLE Mesh Network Manager for offline emergency communication.
 *
 * PRIVACY DESIGN:
 * - No personal identifiers are ever broadcast over BLE.
 * - The node ID is a session-randomized UUID regenerated per emergency.
 * - Only severity level, an anonymized timestamp offset, and a session token are embedded.
 * - Distance is estimated from RSSI, never from GPS coordinates.
 *
 * PROTOCOL:
 * Manufacturer data payload (4 bytes):
 * Byte 0: Protocol version (0x01)
 * Byte 1: Severity level (0x01, 0x02, or 0x03)
 * Byte 2: Session token nibble (random, for deduplication only)
 * Byte 3: Relay flag (0x01 if this node has internet and can relay)
 */
class BleMeshManager(
    private val context: Context,
    private val onNodeDiscovered: (MeshNode) -> Unit
) {
    companion object {
        val SANX_SERVICE_UUID: UUID = UUID.fromString("0000FA11-0000-1000-8000-00805F9B34FB")
        private const val MANUFACTURER_ID = 0x59A1   // SanX manufacturer code
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var isAdvertising = false
    private var isScanning = false

    // ─── Advertising (Distress Broadcast) ────────────────────────────────────

    @Suppress("UNUSED_PARAMETER")
    fun startDistressBroadcast(severity: Severity, hasInternet: Boolean, sessionToken: Byte) {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled || isAdvertising) return

        advertiser = adapter.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)  // Indefinite
            .build()

        val emergencyIdBytes = byteArrayOf(
            sessionToken,
            (sessionToken.toInt() xor 0xAA.toInt()).toByte(),
            (sessionToken.toInt() xor 0x55.toInt()).toByte(),
            (sessionToken.toInt() xor 0xFF.toInt()).toByte()
        )
        val encryptedTokenBytes = byteArrayOf(
            (emergencyIdBytes[0].toInt() xor 0xFA.toInt()).toByte(),
            (emergencyIdBytes[1].toInt() xor 0x11.toInt()).toByte(),
            (emergencyIdBytes[2].toInt() xor 0xA1.toInt()).toByte(),
            (emergencyIdBytes[3].toInt() xor 0x59.toInt()).toByte()
        )

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(
                MANUFACTURER_ID,
                byteArrayOf(
                    0x02,                                    // Protocol version (Propagation Mode)
                    severity.level.toByte(),                 // Severity
                    0x00,                                    // Hop count (0 for victim)
                    emergencyIdBytes[0],                     // Emergency ID Byte 0
                    emergencyIdBytes[1],                     // Emergency ID Byte 1
                    emergencyIdBytes[2],                     // Emergency ID Byte 2
                    emergencyIdBytes[3],                     // Emergency ID Byte 3
                    0x00, 0x00,                              // Timestamp offset
                    0x3C,                                    // Expiry time
                    encryptedTokenBytes[0],                  // Encrypted token Byte 0
                    encryptedTokenBytes[1],                  // Encrypted token Byte 1
                    encryptedTokenBytes[2],                  // Encrypted token Byte 2
                    encryptedTokenBytes[3],                  // Encrypted token Byte 3
                    0x00                                     // Relay Active Flag (direct from victim)
                )
            )
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
            isAdvertising = true
        } catch (_: Exception) {
            isAdvertising = false
        }
    }

    fun startRelayBroadcast(node: MeshNode) {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled || isAdvertising) return

        advertiser = adapter.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(15000)  // Automatically expires after 15 seconds to save battery!
            .build()

        val rawToken = node.nodeId.replace("SX-", "").toIntOrNull(16) ?: 0
        val sessionToken = rawToken.toByte()

        val emergencyIdBytes = byteArrayOf(
            sessionToken,
            (sessionToken.toInt() xor 0xAA.toInt()).toByte(),
            (sessionToken.toInt() xor 0x55.toInt()).toByte(),
            (sessionToken.toInt() xor 0xFF.toInt()).toByte()
        )
        val encryptedTokenBytes = byteArrayOf(
            (emergencyIdBytes[0].toInt() xor 0xFA.toInt()).toByte(),
            (emergencyIdBytes[1].toInt() xor 0x11.toInt()).toByte(),
            (emergencyIdBytes[2].toInt() xor 0xA1.toInt()).toByte(),
            (emergencyIdBytes[3].toInt() xor 0x59.toInt()).toByte()
        )

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(
                MANUFACTURER_ID,
                byteArrayOf(
                    0x02,                                    // Protocol version (Propagation Mode)
                    node.severity.level.toByte(),            // Severity
                    (node.hopCount + 1).coerceAtMost(5).toByte(), // Hop count incremented!
                    emergencyIdBytes[0],                     // Emergency ID Byte 0
                    emergencyIdBytes[1],                     // Emergency ID Byte 1
                    emergencyIdBytes[2],                     // Emergency ID Byte 2
                    emergencyIdBytes[3],                     // Emergency ID Byte 3
                    0x00, 0x00,                              // Timestamp offset
                    0x3C,                                    // Expiry time
                    encryptedTokenBytes[0],                  // Encrypted token Byte 0
                    encryptedTokenBytes[1],                  // Encrypted token Byte 1
                    encryptedTokenBytes[2],                  // Encrypted token Byte 2
                    encryptedTokenBytes[3],                  // Encrypted token Byte 3
                    0x01                                     // Relay Active Flag (relayed)
                )
            )
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
            isAdvertising = true
        } catch (_: Exception) {
            isAdvertising = false
        }
    }

    fun stopDistressBroadcast() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (_: Exception) {}
        isAdvertising = false
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
        }
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
        }
    }

    // ─── Scanning (Mesh Listener) ─────────────────────────────────────────────

    fun startMeshScan() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled || isScanning) return

        scanner = adapter.bluetoothLeScanner ?: return

        // Screen-off compatible scan filter targeting our Manufacturer ID and version byte
        val scanFilter = ScanFilter.Builder()
            .setManufacturerData(MANUFACTURER_ID, byteArrayOf())
            .build()

        // Ultra-responsive continuous scanning at 100% duty cycle for safety-critical offline discovery
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            isScanning = true
        } catch (_: Exception) {
            isScanning = false
        }
    }

    fun stopMeshScan() {
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        isScanning = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            parseAndNotify(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { parseAndNotify(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
        }
    }

    private fun parseAndNotify(result: ScanResult) {
        val bytes = result.scanRecord?.bytes ?: return
        var index = 0
        while (index < bytes.size - 1) {
            val length = bytes[index].toInt() and 0xFF
            if (length == 0) break // End of AD structures
            if (index + 1 + length > bytes.size) break

            val type = bytes[index + 1].toInt() and 0xFF
            if (type == 0xFF) { // Manufacturer Specific Data structure
                if (length >= 7) {
                    val compId = (bytes[index + 2].toInt() and 0xFF) or ((bytes[index + 3].toInt() and 0xFF) shl 8)
                    if (compId == MANUFACTURER_ID) {
                        val version = bytes[index + 4]
                        if (version == 0x02.toByte() && length >= 17) {
                            // Upgraded mesh version 0x02 propagation packet
                            val severityLevel = bytes[index + 5].toInt() and 0xFF
                            val severity = Severity.fromLevel(severityLevel)
                            val hopCount = bytes[index + 6].toInt() and 0xFF
                            val sessionToken = bytes[index + 7] // Byte 0 of Emergency ID hash
                            
                            val isRelayed = bytes[index + 18] == 0x01.toByte()
                            val distance = rssiToDistance(result.rssi)
                            val nodeId = "SX-${sessionToken.toInt().and(0xFF).toString(16).uppercase()}"

                            val node = MeshNode(
                                nodeId = nodeId,
                                approximateDistanceM = distance,
                                rssi = result.rssi,
                                severity = severity,
                                isRelayCapable = true,
                                hopCount = hopCount,
                                isRelayed = isRelayed,
                                emergencyId = nodeId
                            )
                            onNodeDiscovered(node)
                            return
                        } else if (version == 0x01.toByte()) {
                            // Version 0x01 backward compatibility fallback
                            val severityLevel = bytes[index + 5].toInt() and 0xFF
                            val severity = Severity.fromLevel(severityLevel)
                            val sessionToken = bytes[index + 6]
                            val canRelay = bytes[index + 7] == 0x01.toByte()

                            val distance = rssiToDistance(result.rssi)
                            val nodeId = "SX-${sessionToken.toInt().and(0xFF).toString(16).uppercase()}"

                            val node = MeshNode(
                                nodeId = nodeId,
                                approximateDistanceM = distance,
                                rssi = result.rssi,
                                severity = severity,
                                isRelayCapable = canRelay,
                                hopCount = 0,
                                isRelayed = false,
                                emergencyId = nodeId
                            )
                            onNodeDiscovered(node)
                            return
                        }
                    }
                }
            }
            index += length + 1
        }
    }

    /**
     * Estimate distance in meters from RSSI using the log-distance path loss model.
     * RSSI(d) = RSSI(d0) - 10n * log10(d/d0)
     * Calibrated for typical indoor BLE transmission (Tx power: -59 dBm at 1m, n=2.0).
     */
    private fun rssiToDistance(rssi: Int): Float {
        val txPower = -59f   // RSSI at 1 meter (calibrated)
        val n = 2.0f         // Path loss exponent (2.0 = free space)
        return 10f.pow((txPower - rssi) / (10 * n))
    }
}
