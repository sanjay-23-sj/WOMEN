package com.sanx.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanx.app.ui.theme.*

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanXBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SanXCard)
                    .border(1.dp, SanXBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SanXTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SanXTextPrimary
                )
                Text(
                    text = "How we protect and manage your data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SanXTextSecondary
                )
            }
        }

        // Introduction Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SanXCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SanXBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Privacy First Architecture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF2B69)
                )
                Text(
                    text = "WOMEN app does not have trackers, advertisements, or analytics engines. We believe safety application developers have a responsibility to keep user data isolated and highly guarded. Your locations, audio transcripts, and emergency logs remain encrypted and stay entirely on-device until a critical alert is triggered.",
                    fontSize = 13.sp,
                    color = SanXTextPrimary,
                    lineHeight = 20.sp
                )
            }
        }

        // Guarantees row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SanXCard)
                    .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("NO ADS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF2B69))
                    Text("100% Ad-Free", fontSize = 11.sp, color = SanXTextSecondary)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SanXCard)
                    .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("NO SELLING", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF2B69))
                    Text("Zero Data Selling", fontSize = 11.sp, color = SanXTextSecondary)
                }
            }
        }

        // Section: Why Permissions Are Required
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Why Permissions Are Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SanXTextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            PermissionUsageCard(
                icon = Icons.Default.Mic,
                title = "Microphone Permission",
                usage = "Used to record local security evidence when an emergency activates. It allows designated trusted contacts to hear live streaming audio silently through encrypted links during active alerts."
            )

            PermissionUsageCard(
                icon = Icons.Default.Bluetooth,
                title = "Bluetooth Permission",
                usage = "Used to broadcast localized, offline distress signals via Bluetooth Low Energy mesh and relay alerts A → B → C → D → E to other nearby WOMEN users when internet is offline."
            )

            PermissionUsageCard(
                icon = Icons.Default.LocationOn,
                title = "Location Permission",
                usage = "Used to determine physical location to embed accurate rescue coordinates inside the emergency SMS alerts sent to your trusted circle. Strangers on the mesh network cannot see your location."
            )
        }

        // Section: Emergency Data Protection
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Emergency Data Boundaries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SanXTextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            BoundaryItem(
                icon = Icons.Default.Security,
                title = "AES-256 On-Device Encryption",
                text = "All security audio recordings and logs are stored locally within protected storage directories using strong military-grade AES-256 encryption. They are completely inaccessible to third-party apps."
            )

            BoundaryItem(
                icon = Icons.Default.Warning,
                title = "Mesh Network Anonymity",
                text = "Nearby users relaying distress signals act as blind, anonymous network extenders. They do NOT have access to live audio streams, your location coordinates, user profile name, or safety contact details."
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SanXCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SanXBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔒 Data Retention & Deletion Policy",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SanXSafe
                )
                Text(
                    text = "• All recorded audio files and evidence logs are strictly saved in the app's secure internal sandbox directory.\n" +
                           "• Evidence remains encrypted using a transient session-specific AES key and is never backed up to Google Cloud or external databases.\n" +
                           "• You can permanently wipe all emergency records, local evidence, and safety logs at any time from the app's database with a single click inside the Privacy tab.",
                    fontSize = 13.sp,
                    color = SanXTextPrimary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionUsageCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    usage: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SanXCard)
            .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SanXSafeDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SanXSafe,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SanXTextPrimary
            )
            Text(
                text = usage,
                fontSize = 12.sp,
                color = SanXTextPrimary,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun BoundaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFF2B69),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SanXTextPrimary
            )
            Text(
                text = text,
                fontSize = 12.sp,
                color = SanXTextPrimary,
                lineHeight = 17.sp
            )
        }
    }
}
