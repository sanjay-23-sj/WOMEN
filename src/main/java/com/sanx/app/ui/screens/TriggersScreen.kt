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
import com.sanx.app.data.model.TriggerConfig
import com.sanx.app.data.model.TriggerSensitivity
import com.sanx.app.data.model.TriggerType
import com.sanx.app.ui.theme.*
import com.sanx.app.ui.viewmodel.MainViewModel

/**
 * Trigger Settings Screen.
 * Displays all available emergency trigger types with enable toggles
 * and sensitivity sliders. Includes a test mode per trigger.
 */
@Composable
fun TriggersScreen(viewModel: MainViewModel, onMenuClick: () -> Unit) {
    val configs by viewModel.triggerConfigs.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanXBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader("Trigger Settings", "Configure how emergencies activate silently", onMenuClick = onMenuClick)

        configs.forEach { config ->
            TriggerCard(
                config = config,
                onToggle = { enabled ->
                    viewModel.updateTrigger(config.type, enabled, config.sensitivity)
                },
                onSensitivityChange = { sensitivity ->
                    viewModel.updateTrigger(config.type, config.enabled, sensitivity)
                }
            )
        }

        // Info card
        InfoBox(
            icon = Icons.Default.Info,
            text = "All triggers operate silently. The phone screen will not change when an emergency is activated.",
            color = SanXInfo
        )
    }
}

@Composable
private fun TriggerCard(
    config: TriggerConfig,
    onToggle: (Boolean) -> Unit,
    onSensitivityChange: (TriggerSensitivity) -> Unit
) {
    val (icon, title, description) = triggerMeta(config.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SanXCard)
            .border(
                width = if (config.enabled) 1.dp else 1.dp,
                color = if (config.enabled) SanXSafe.copy(alpha = 0.35f) else SanXBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (config.enabled) SanXSafe.copy(alpha = 0.12f) else SanXBorder.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (config.enabled) SanXSafe else SanXTextDisabled,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        color = if (config.enabled) SanXTextPrimary else SanXTextDisabled)
                    Text(description, style = MaterialTheme.typography.bodySmall,
                        color = SanXTextSecondary)
                }
            }
            Switch(
                checked = config.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SanXBlack,
                    checkedTrackColor = SanXSafe,
                    uncheckedThumbColor = SanXTextDisabled,
                    uncheckedTrackColor = SanXCard
                )
            )
        }

        // Sensitivity selector (only if enabled)
        if (config.enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sensitivity", style = MaterialTheme.typography.labelMedium,
                    color = SanXTextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TriggerSensitivity.entries.forEach { s ->
                        SensitivityChip(
                            label = s.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = config.sensitivity == s,
                            onClick = { onSensitivityChange(s) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SensitivityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SanXSafe.copy(alpha = 0.15f) else SanXBlack)
            .border(
                width = 1.dp,
                color = if (selected) SanXSafe else SanXBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) SanXSafe else SanXTextSecondary
        )
    }
}

@Composable
fun InfoBox(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = color,
            modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.9f),
            lineHeight = 18.sp)
    }
}

private fun triggerMeta(type: TriggerType): Triple<ImageVector, String, String> = when (type) {
    TriggerType.DOUBLE_TAP_BACK ->
        Triple(Icons.Default.TouchApp, "Back Triple Tap", "Tap back of phone three times rapidly")
    TriggerType.SHAKE_PANIC ->
        Triple(Icons.Default.Vibration, "Panic Shake", "Aggressive repeated shake gesture")
}
