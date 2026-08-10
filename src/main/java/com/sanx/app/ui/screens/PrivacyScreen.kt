package com.sanx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.sanx.app.ui.theme.*
import java.io.File

/**
 * Privacy & Security Settings Screen.
 * Controls encryption preferences, emergency data sharing scope,
 * AI analysis permissions, and local storage management.
 */
@Composable
fun PrivacyScreen(onMenuClick: () -> Unit) {
    var encryptionEnabled by remember { mutableStateOf(true) }
    var autoDeleteDays by remember { mutableIntStateOf(30) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().background(SanXBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp).padding(top = 56.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ScreenHeader("Privacy & Security", "Control your data and permissions", onMenuClick = onMenuClick)

        // Encryption
        SectionLabel("Data Encryption")
        PrivacyToggleCard(
            icon = Icons.Default.EnhancedEncryption,
            title = "AES-256 Local Encryption",
            description = "Encrypts all locally stored emergency recordings, logs, and evidence files.",
            checked = encryptionEnabled,
            onToggle = { encryptionEnabled = it },
            accentColor = SanXSafe,
            locked = true  // Cannot be disabled — safety requirement
        )

        val context = LocalContext.current
        val storagePath = remember { 
            File(context.getExternalFilesDir(null) ?: context.filesDir, "evidence").absolutePath 
        }

        // Storage Path Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SanXCard)
                .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = SanXCard)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SanXInfo.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Storage Path",
                        tint = SanXInfo,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Local Storage Directory",
                        style = MaterialTheme.typography.titleMedium,
                        color = SanXTextPrimary
                    )
                    Text(
                        text = storagePath,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        color = SanXSafe
                    )
                    Text(
                        text = "All recorded voice files and logs are stored privately in this device directory. Safe from external access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SanXTextSecondary
                    )
                }
            }
        }
        // Auto-delete
        SectionLabel("Data Retention")
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SanXCard)
                .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-Delete Logs After", style = MaterialTheme.typography.titleMedium)
                    Text("$autoDeleteDays days",
                        style = MaterialTheme.typography.bodySmall, color = SanXInfo)
                }
                Icon(Icons.Default.DeleteSweep, contentDescription = null,
                    tint = SanXTextSecondary, modifier = Modifier.size(20.dp))
            }
            Slider(
                value = autoDeleteDays.toFloat(),
                onValueChange = { autoDeleteDays = it.toInt() },
                valueRange = 7f..90f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = SanXInfo,
                    activeTrackColor = SanXInfo,
                    inactiveTrackColor = SanXBorder
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("7 days", style = MaterialTheme.typography.labelSmall, color = SanXTextDisabled)
                Text("90 days", style = MaterialTheme.typography.labelSmall, color = SanXTextDisabled)
            }
        }

        // App info
        InfoBox(
            icon = Icons.Default.VerifiedUser,
            text = "SanX is fully open, free, and privacy-first. No ads, no subscriptions, no data selling. Ever.",
            color = SanXSafe
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = SanXTextDisabled,
        letterSpacing = 1.sp
    )
}

@Composable
private fun PrivacyToggleCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color,
    locked: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SanXCard)
            .border(1.dp, if (checked) accentColor.copy(alpha = 0.25f) else SanXBorder,
                RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (checked) accentColor.copy(alpha = 0.12f) else SanXBorder.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title,
                tint = if (checked) accentColor else SanXTextDisabled,
                modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = if (checked) SanXTextPrimary else SanXTextSecondary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = SanXTextSecondary)
            if (locked) {
                Text("Always enabled for security", style = MaterialTheme.typography.labelSmall,
                    color = accentColor)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = if (locked) null else onToggle,
            enabled = !locked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SanXBlack, checkedTrackColor = accentColor,
                uncheckedThumbColor = SanXTextDisabled, uncheckedTrackColor = SanXCard,
                disabledCheckedThumbColor = SanXBlack, disabledCheckedTrackColor = accentColor.copy(alpha = 0.5f)
            )
        )
    }
}

private val Int.sp: androidx.compose.ui.unit.TextUnit
    get() = this.toFloat().sp

private val Float.sp: androidx.compose.ui.unit.TextUnit
    get() = androidx.compose.ui.unit.TextUnit(this, androidx.compose.ui.unit.TextUnitType.Sp)
