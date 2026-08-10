package com.sanx.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanx.app.data.model.EmergencySession
import com.sanx.app.data.model.Severity
import com.sanx.app.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.sanx.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Emergency Live HUD — displayed during an active emergency session.
 * Shows real-time GPS status, danger score, evidence recording indicators,
 * relay activity, and escalation/cancel controls.
 *
 * Designed to be instantly readable and panic-proof — large text, minimal elements.
 */
@Composable
fun EmergencyLiveScreen(viewModel: MainViewModel) {
    val session by viewModel.emergencySession.collectAsState()
    val meshNodes by viewModel.nearbyMeshNodes.collectAsState()
    val logs by viewModel.recentLogs.collectAsState()

    // If no active session, show idle state
    if (session == null) {
        EmergencyIdleOverlay()
        return
    }

    val activeSession = session!!

    // Pulse animation for emergency header
    val infiniteTransition = rememberInfiniteTransition(label = "emergencyPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = EaseInOut), RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val severityColor = when (activeSession.severity) {
        Severity.LEVEL_1 -> SanXInfo
        Severity.LEVEL_2 -> SanXWarning
        Severity.LEVEL_3 -> SanXEmergency
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0C))
            .padding(horizontal = 20.dp).padding(top = 64.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Emergency Header ─────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulse)
                    .clip(RoundedCornerShape(20.dp))
                    .background(severityColor.copy(alpha = 0.12f))
                    .border(1.5.dp, severityColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null,
                        tint = severityColor, modifier = Modifier.size(32.dp))
                    Text("EMERGENCY ACTIVE", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = severityColor, letterSpacing = 1.sp)
                    Text(activeSession.severity.label, style = MaterialTheme.typography.bodyMedium,
                        color = severityColor.copy(alpha = 0.8f))
                    Text(
                        text = "Session ${activeSession.sessionId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // ── Live Status Grid ─────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiveStatusTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocationOn,
                    label = "GPS",
                    value = if (activeSession.latitude != 0.0)
                        "${"%.4f".format(activeSession.latitude)},\n${"%.4f".format(activeSession.longitude)}"
                    else "Acquiring…",
                    color = SanXSafe
                )
                LiveStatusTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Mic,
                    label = "Recording",
                    value = if (activeSession.isRecordingAudio) "LIVE" else "Off",
                    color = if (activeSession.isRecordingAudio) SanXEmergency else SanXTextDisabled
                )
                LiveStatusTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Bluetooth,
                    label = "Mesh Relay",
                    value = "${meshNodes.size} nodes",
                    color = SanXMesh
                )
            }
        }

        // ── Control buttons ───────────────────────────────────────────────────
        item {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Cancel button
                OutlinedButton(
                    onClick = { viewModel.cancelEmergency() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SanXBorder)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SanXTextSecondary)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel")
                }

                // Escalate button (disabled at max level)
                val canEscalate = activeSession.severity.level < 3
                Button(
                    onClick = { if (canEscalate) viewModel.escalateEmergency() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canEscalate) SanXEmergency else SanXCard,
                        contentColor = if (canEscalate) SanXBlack else SanXTextDisabled
                    )
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Escalate", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Activity log ─────────────────────────────────────────────────────
        item {
            Text("Activity Log", style = MaterialTheme.typography.titleMedium,
                color = SanXTextSecondary)
        }

        items(logs.take(10)) { log ->
            val logColor = when (log.eventType) {
                "TRIGGER"           -> SanXEmergency
                "LOCATION"          -> SanXSafe
                "EMERGENCY_START"   -> SanXWarning
                "ESCALATE"          -> SanXEmergency
                else                -> SanXTextDisabled
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SanXCard)
                    .border(1.dp, SanXBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(logColor).padding(top = 5.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(log.detail, style = MaterialTheme.typography.bodySmall,
                        color = SanXTextPrimary)
                    Text(
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = SanXTextDisabled
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStatusTile(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SanXCard)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SanXTextSecondary)
    }
}

@Composable
private fun DangerScoreBar(score: Float) {
    val color = when {
        score > 0.7f -> SanXEmergency
        score > 0.4f -> SanXWarning
        else         -> SanXSafe
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SanXCard)
            .border(1.dp, SanXBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("AI Danger Score", style = MaterialTheme.typography.labelMedium,
                color = SanXTextSecondary)
            Text("${(score * 100).toInt()}%", fontWeight = FontWeight.Bold,
                color = color, fontSize = 14.sp)
        }
        LinearProgressIndicator(
            progress = { score },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = SanXBorder
        )
    }
}

@Composable
private fun EmergencyIdleOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(SanXBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.CheckCircleOutline, contentDescription = null,
                tint = SanXSafe, modifier = Modifier.size(64.dp))
            Text("No Active Emergency", style = MaterialTheme.typography.headlineSmall,
                color = SanXTextPrimary)
            Text("System is monitoring in the background.",
                style = MaterialTheme.typography.bodyMedium, color = SanXTextSecondary)
        }
    }
}
