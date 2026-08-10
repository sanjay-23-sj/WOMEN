package com.sanx.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanx.app.data.model.MeshNode
import com.sanx.app.data.model.Severity
import com.sanx.app.ui.theme.*
import com.sanx.app.ui.viewmodel.MainViewModel

/**
 * Mesh Network Screen.
 * Displays nearby SanX devices detected via BLE mesh scanning.
 * Shows approximate distance, danger level, and relay capability per node.
 * All displayed information is anonymized — no identity data is shown.
 */
@Composable
fun MeshScreen(viewModel: MainViewModel, onMenuClick: () -> Unit) {
    val nodes by viewModel.nearbyMeshNodes.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SanXBlack)
            .padding(horizontal = 20.dp).padding(top = 56.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader("Mesh Rescue Network",
                "Anonymous nearby emergency relay nodes",
                onMenuClick = onMenuClick)
        }

        item { MeshStatusHeader(nodeCount = nodes.size) }

        item {
            InfoBox(
                icon = Icons.Default.Lock,
                text = "No identity, phone number, or personal data is visible. Only presence, distance, severity, and relay capability are shown.",
                color = SanXMesh
            )
        }

        if (nodes.isEmpty()) {
            item { MeshEmptyState() }
        }

        items(nodes) { node ->
            MeshNodeCard(node = node)
        }
    }
}

@Composable
private fun MeshStatusHeader(nodeCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SanXMeshDim)
            .border(1.dp, SanXMesh.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("BLE Scan Active", style = MaterialTheme.typography.titleMedium, color = SanXMesh)
            Text("Scanning for SanX distress signals…",
                style = MaterialTheme.typography.bodySmall, color = SanXMesh.copy(alpha = 0.7f))
        }
        Box(
            modifier = Modifier
                .size(48.dp).clip(CircleShape)
                .background(SanXMesh.copy(alpha = 0.15f))
                .border(1.5.dp, SanXMesh.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = nodeCount.toString(),
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SanXMesh
            )
        }
    }
}

@Composable
private fun MeshNodeCard(node: MeshNode) {
    val severityColor = when (node.severity) {
        Severity.LEVEL_1 -> SanXInfo
        Severity.LEVEL_2 -> SanXWarning
        Severity.LEVEL_3 -> SanXEmergency
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SanXCard)
            .border(1.dp, severityColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Left: Signal strength visual icon
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(severityColor.copy(alpha = 0.1f))
                .border(1.5.dp, severityColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (node.approximateDistanceM < 30) Icons.Default.Bluetooth else Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = "Signal",
                tint = severityColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // Center: Node ID, Severity level, and Relay capability
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Node ${node.nodeId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SanXTextPrimary)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(node.severity.label, fontSize = 10.sp,
                        color = severityColor, fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                text = "RSSI: ${node.rssi} dBm",
                style = MaterialTheme.typography.bodySmall,
                color = SanXTextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Visual proximity indicator progress bar (closer = fuller bar)
            val proximity = (1f - (node.approximateDistanceM / 60f)).coerceIn(0.05f, 1.0f)
            val animatedProximity by animateFloatAsState(
                targetValue = proximity,
                animationSpec = tween(durationMillis = 800, easing = EaseOutBack),
                label = "proximityAnim"
            )
            LinearProgressIndicator(
                progress = { animatedProximity },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFFF2B69), // Soft pink brand emergency accent!
                trackColor = SanXBorder
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (node.isRelayCapable) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null,
                        tint = SanXSafe, modifier = Modifier.size(11.dp))
                    Text("Relay active", fontSize = 11.sp, color = SanXSafe)
                }
            }
        }

        // Right: Prominent, highly visual, easily understandable distance metric!
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "~${"%.1f".format(node.approximateDistanceM)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = severityColor
            )
            Text(
                text = "meters",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SanXTextSecondary
            )
        }
    }
}

@Composable
private fun MeshEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null,
            tint = SanXMesh.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
        Text("No nearby nodes detected", style = MaterialTheme.typography.titleMedium,
            color = SanXTextSecondary)
        Text("Other WOMEN devices in emergency mode will appear here.",
            style = MaterialTheme.typography.bodySmall, color = SanXTextDisabled,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
