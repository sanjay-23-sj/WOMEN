package com.sanx.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanx.app.data.local.entity.Contact
import com.sanx.app.data.model.EmergencySession
import com.sanx.app.data.model.MeshNode
import com.sanx.app.ui.theme.*
import com.sanx.app.ui.viewmodel.MainViewModel

/**
 * Home Dashboard Screen — the primary interface of SanX.
 *
 * Shows:
 * - Protection status toggle (large hero button)
 * - Danger score indicator (soft pulse ring)
 * - Status grid cards (triggers, mesh, battery, trusted circle)
 * - Emergency Level quick-launch row
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToEmergencyLive: () -> Unit,
    onMenuClick: () -> Unit
) {
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val session by viewModel.emergencySession.collectAsState()
    val dangerScore by viewModel.dangerScore.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val meshNodes by viewModel.nearbyMeshNodes.collectAsState()
    val triggerConfigs by viewModel.triggerConfigs.collectAsState()

    val scrollState = rememberScrollState()

    // Navigate to emergency live when emergency is active
    LaunchedEffect(session) {
        if (session != null) onNavigateToEmergencyLive()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanXBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        HomeHeader(onMenuClick = onMenuClick)

        // ── Protection Hero Button ─────────────────────────────────────────
        ProtectionToggleHero(
            isMonitoring = isMonitoring,
            dangerScore = dangerScore,
            onToggle = {
                if (isMonitoring) viewModel.stopProtection()
                else viewModel.startProtection()
            }
        )

        // ── Status Grid ────────────────────────────────────────────────────
        StatusGrid(
            contacts = contacts,
            meshNodes = meshNodes,
            isMonitoring = isMonitoring,
            activeTriggersCount = triggerConfigs.filter { it.enabled }.size
        )

        // ── Emergency Quick Launch ─────────────────────────────────────────
        EmergencyQuickLaunch(
            onLevel1 = { viewModel.triggerEmergency(com.sanx.app.data.model.Severity.LEVEL_1) },
            onLevel2 = { viewModel.triggerEmergency(com.sanx.app.data.model.Severity.LEVEL_2) },
            onLevel3 = { viewModel.triggerEmergency(com.sanx.app.data.model.Severity.LEVEL_3) }
        )
    }
}

@Composable
private fun HomeHeader(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SanXCard)
                    .border(1.dp, SanXBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = SanXTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "WOMEN",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = SanXTextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Invisible Protection. Visible Safety.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SanXTextSecondary
                )
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SanXCard)
                .border(1.dp, SanXBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Protection",
                tint = SanXSafe,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun ProtectionToggleHero(
    isMonitoring: Boolean,
    dangerScore: Float,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pinkPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val activePink = Color(0xFFFF2B69) // Hot brand pink accent glow
    val borderCol = if (isMonitoring) activePink.copy(alpha = 0.6f) else SanXBorder
    val glowColor = if (isMonitoring) activePink else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Soft pink radial neon glow pulse behind the card when ON
        if (isMonitoring) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .height(130.dp)
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = pulseAlpha), Color.Transparent),
                            radius = 350f
                        )
                    )
            )
        }

        // 2. Main Matte Card Switcher Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SanXCard)
                .border(
                    width = if (isMonitoring) 1.5.dp else 1.dp,
                    color = borderCol,
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(onClick = onToggle)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isMonitoring) SanXSafe else SanXTextDisabled)
                    )
                    Text(
                        text = if (isMonitoring) "Protection Mode ON" else "Protection Mode OFF",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SanXTextPrimary,
                        letterSpacing = (-0.3).sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = if (isMonitoring) 
                        "WOMEN background safety triggers, sensors, and mesh relay services are actively monitoring."
                    else 
                        "Decentralized relays, shake triggers, and triple tap sensors are currently offline.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isMonitoring) activePink else SanXTextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Switch Indicator / Shield Icon
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isMonitoring) activePink.copy(alpha = 0.12f) else SanXBlack)
                    .border(1.dp, if (isMonitoring) activePink.copy(alpha = 0.4f) else SanXBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMonitoring) Icons.Default.Security else Icons.Default.Shield,
                    contentDescription = "Toggle state",
                    tint = if (isMonitoring) activePink else SanXTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusGrid(
    contacts: List<Contact>,
    meshNodes: List<MeshNode>,
    isMonitoring: Boolean,
    activeTriggersCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.People,
            label = "Safety Contact",
            value = if (contacts.isNotEmpty()) contacts.first().name else "Not Set",
            accentColor = SanXInfo
        )
        StatusCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Bluetooth,
            label = "Mesh Network",
            value = if (meshNodes.isEmpty()) "Scanning…" else "${meshNodes.size} nearby",
            accentColor = SanXMesh
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Sensors,
            label = "Triggers",
            value = if (isMonitoring) "$activeTriggersCount ready" else "Inactive",
            accentColor = if (isMonitoring) SanXSafe else SanXTextDisabled
        )
        StatusCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.BatteryFull,
            label = "Battery",
            value = "Optimized",
            accentColor = SanXSafe
        )
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SanXCard)
            .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = SanXTextPrimary)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = SanXTextSecondary)
        }
    }
}

@Composable
private fun EmergencyQuickLaunch(
    onLevel1: () -> Unit,
    onLevel2: () -> Unit,
    onLevel3: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SanXCard)
            .border(1.dp, SanXBorder, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FlashOn, contentDescription = null,
                tint = SanXWarning, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Emergency Quick Launch",
                style = MaterialTheme.typography.titleMedium,
                color = SanXTextPrimary)
        }

        Text("Tap to activate silently. Hold for 3s to cancel.",
            style = MaterialTheme.typography.bodySmall,
            color = SanXTextSecondary)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EmergencyLevelButton(Modifier.weight(1f), "L1", "Silent", SanXInfo, onLevel1)
            EmergencyLevelButton(Modifier.weight(1f), "L2", "Alert", SanXWarning, onLevel2)
            EmergencyLevelButton(Modifier.weight(1f), "L3", "Critical", SanXEmergency, onLevel3)
        }
    }
}

@Composable
private fun EmergencyLevelButton(
    modifier: Modifier,
    level: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(level, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun DangerScoreCard(score: Float) {
    val scorePercent = (score * 100).toInt()
    val scoreColor = when {
        score > 0.7f -> SanXEmergency
        score > 0.4f -> SanXWarning
        else         -> SanXSafe
    }
    val label = when {
        score > 0.7f -> "ELEVATED DANGER"
        score > 0.4f -> "MONITOR"
        else         -> "ALL CLEAR"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SanXCard)
            .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("AI Danger Score", style = MaterialTheme.typography.labelMedium, color = SanXTextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = scoreColor)
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(scoreColor.copy(alpha = 0.12f))
                .border(2.dp, scoreColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$scorePercent%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
        }
    }
}
