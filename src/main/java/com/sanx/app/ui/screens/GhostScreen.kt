package com.sanx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanx.app.ui.theme.*
import com.sanx.app.ui.viewmodel.GhostDisguise
import com.sanx.app.ui.viewmodel.MainViewModel

/**
 * Ghost Mode Settings Screen.
 * Configures the stealth disguise that masks emergency activity from an attacker.
 * Calculator disguise is the primary/recommended mode.
 */
@Composable
fun GhostScreen(viewModel: MainViewModel, onMenuClick: () -> Unit) {
    val ghostEnabled by viewModel.ghostModeEnabled.collectAsState()
    val ghostDisguise by viewModel.ghostDisguise.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().background(SanXBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp).padding(top = 56.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ScreenHeader("Ghost Mode", "Stealth emergency operation", onMenuClick = onMenuClick)

        // Master toggle card
        GhostMasterToggle(enabled = ghostEnabled, onToggle = viewModel::setGhostMode)

        // Disguise selector
        if (ghostEnabled) {
            Text("Select Disguise", style = MaterialTheme.typography.titleMedium,
                color = SanXTextSecondary)

            DisguiseOption(
                title = "Calculator",
                description = "Shows a fully functional calculator. Emergency runs behind it silently.",
                icon = Icons.Default.Calculate,
                recommended = true,
                selected = ghostDisguise == GhostDisguise.CALCULATOR,
                onClick = { viewModel.setGhostDisguise(GhostDisguise.CALCULATOR) }
            )
            DisguiseOption(
                title = "Locked Screen",
                description = "Displays a fake lock screen that looks identical to your system lock.",
                icon = Icons.Default.Lock,
                recommended = false,
                selected = ghostDisguise == GhostDisguise.LOCK_SCREEN,
                onClick = { viewModel.setGhostDisguise(GhostDisguise.LOCK_SCREEN) }
            )


            Spacer(modifier = Modifier.height(6.dp))

            // App Lock Toggle Card
            val appLockEnabled by viewModel.appLockEnabled.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SanXCard)
                    .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = SanXSafe, modifier = Modifier.size(24.dp))
                    Column {
                        Text("App Lock Disguise", style = MaterialTheme.typography.titleMedium)
                        Text("Prompt disguise lock screen on app start", style = MaterialTheme.typography.bodySmall, color = SanXTextSecondary)
                    }
                }
                Switch(
                    checked = appLockEnabled, onCheckedChange = viewModel::setAppLockEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SanXBlack, checkedTrackColor = SanXSafe,
                        uncheckedThumbColor = SanXTextDisabled, uncheckedTrackColor = SanXCard
                    )
                )
            }

            // Set Custom PIN Card
            val ghostPin by viewModel.ghostPin.collectAsState()
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SanXCard)
                    .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Password, contentDescription = null, tint = SanXWarning, modifier = Modifier.size(24.dp))
                    Text("Secure PIN Code", style = MaterialTheme.typography.titleMedium)
                }
                
                OutlinedTextField(
                    value = ghostPin,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 8) {
                            viewModel.setGhostPin(newValue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom Bypass PIN") },
                    placeholder = { Text("e.g. 9999") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SanXTextPrimary,
                        unfocusedTextColor = SanXTextPrimary,
                        focusedBorderColor = SanXSafe,
                        unfocusedBorderColor = SanXBorder,
                        focusedLabelColor = SanXSafe,
                        unfocusedLabelColor = SanXTextSecondary
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )
                )
            }
        }

        // How it works
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SanXCard)
                .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null,
                    tint = SanXInfo, modifier = Modifier.size(18.dp))
                Text("How Ghost Mode Works", style = MaterialTheme.typography.titleMedium)
            }
            HowItWorksStep("1", "When emergency activates, Ghost Mode immediately shows the disguise screen.")
            HowItWorksStep("2", "All emergency operations — recording, GPS, BLE mesh — continue silently.")
            HowItWorksStep("3", "The attacker sees only the innocent disguise, not the emergency UI.")
            HowItWorksStep("4", "Deactivate by entering a secret PIN within the disguise overlay.")
        }

        InfoBox(
            icon = Icons.Default.PrivacyTip,
            text = "Ghost Mode is designed to protect your safety in high-threat situations. No disguise is foolproof.",
            color = SanXWarning
        )
    }
}

@Composable
private fun GhostMasterToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) SanXMeshDim else SanXCard)
            .border(
                1.dp,
                if (enabled) SanXMesh.copy(alpha = 0.5f) else SanXBorder,
                RoundedCornerShape(18.dp)
            )
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VisibilityOff,
                contentDescription = "Ghost Mode",
                tint = if (enabled) SanXMesh else SanXTextDisabled,
                modifier = Modifier.size(24.dp))
            Column {
                Text("Ghost Mode", style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) SanXTextPrimary else SanXTextSecondary)
                Text(if (enabled) "Active — stealth engaged" else "Inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) SanXMesh else SanXTextDisabled)
            }
        }
        Switch(
            checked = enabled, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SanXBlack, checkedTrackColor = SanXMesh,
                uncheckedThumbColor = SanXTextDisabled, uncheckedTrackColor = SanXCard
            )
        )
    }
}

@Composable
private fun DisguiseOption(
    title: String,
    description: String,
    icon: ImageVector,
    recommended: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) SanXMesh.copy(alpha = 0.08f) else SanXCard)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) SanXMesh else SanXBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title,
            tint = if (selected) SanXMesh else SanXTextSecondary,
            modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = if (selected) SanXMesh else SanXTextPrimary)
                if (recommended) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SanXSafe.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("Recommended", fontSize = 9.sp, color = SanXSafe,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    }
                }
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = SanXTextSecondary)
        }
        RadioButton(
            selected = selected, onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = SanXMesh, unselectedColor = SanXTextDisabled
            )
        )
    }
}

@Composable
private fun HowItWorksStep(step: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                .background(SanXBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(step, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SanXTextSecondary)
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = SanXTextSecondary,
            modifier = Modifier.weight(1f))
    }
}
