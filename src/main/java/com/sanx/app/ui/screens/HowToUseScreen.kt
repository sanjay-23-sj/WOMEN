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

@Composable
fun HowToUseScreen(onBack: () -> Unit) {
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    text = "How To Use",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SanXTextPrimary
                )
                Text(
                    text = "A beginner-friendly guide to your safety vault",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SanXTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Guide Cards with dynamic transition delay
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                animationSpec = tween(500),
                initialOffsetY = { 40 }
            )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GuideCard(
                    step = "1",
                    icon = Icons.Default.Shield,
                    title = "Enable Protection Mode",
                    description = "Activate the main shield switch on the dashboard. This keeps the triple tap, shake detection, and mesh relays active silently in the background, even when the phone is locked or the app is closed."
                )

                GuideCard(
                    step = "2",
                    icon = Icons.Default.Sensors,
                    title = "Hidden Emergency Triggers",
                    description = "Choose your preferred discrete activation gestures from the Settings tab: triple-tapping the back of your phone, or shaking the device. All triggers operate entirely in the background."
                )

                GuideCard(
                    step = "3",
                    icon = Icons.Default.Warning,
                    title = "False Trigger Protection",
                    description = "If an emergency trigger fires accidentally, your phone will vibrate continuously at medium strength for 7 seconds as a silent warning. To cancel before the alert activates, press the physical Volume Down button 3 times consecutively within those 7 seconds. This is the ONLY cancellation method — no overlays or visible confirmations appear."
                )

                GuideCard(
                    step = "4",
                    icon = Icons.Default.Mic,
                    title = "Secure Background Live Audio",
                    description = "Once emergency mode fully activates, the app silently starts a background recording session and begins streaming surrounding audio. You don't need to tap the screen, speak, or open the app."
                )

                GuideCard(
                    step = "5",
                    icon = Icons.Default.Bluetooth,
                    title = "Offline Bluetooth Mesh Relay",
                    description = "When in an emergency without internet, your device broadcasts encrypted BLE packets. Nearby WOMEN app users automatically relay your distress signal (A → B → C → D → E), extending rescue coverage without cellular networks."
                )

                GuideCard(
                    step = "6",
                    icon = Icons.Default.People,
                    title = "Designated Trusted Circle",
                    description = "Designate trusted contacts who will receive emergency SMS alerts with your real-time location. Through the app, they can listen to your background live audio streaming instantly to verify your situation."
                )

                GuideCard(
                    step = "7",
                    icon = Icons.Default.VisibilityOff,
                    title = "Automated Ghost Mode",
                    description = "If an attacker opens the WOMEN app during an active emergency, they will immediately be met with a fully functional Calculator or PIN passcode screen. Active status notifications remain completely disguised."
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SanXSafeDim.copy(alpha = 0.05f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, SanXSafe.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🛡️ Emergency Preparedness Checklist",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SanXSafe
                )
                Text(
                    text = "• Verify that at least one Trusted Contact is added under the Circle tab.\n" +
                           "• Test the motion triggers (double tap / shake) to familiarize yourself with the 7-second vibration.\n" +
                           "• Re-enable the Accessibility Service in Settings every time the app updates.\n" +
                           "• Keep Bluetooth and Location permissions granted to ensure silent background rescue works.",
                    fontSize = 13.sp,
                    color = SanXTextPrimary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun GuideCard(
    step: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SanXCard)
            .border(1.dp, SanXBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SanXSafeDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SanXSafe,
                modifier = Modifier.size(22.dp)
            )
            // Tiny Step badge
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF2B69))
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SanXTextPrimary
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = SanXTextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}
