package com.sanx.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanx.app.R
import com.sanx.app.ui.theme.*

@Composable
fun AboutAppScreen(onBack: () -> Unit) {
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
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                    text = "About App",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SanXTextPrimary
                )
                Text(
                    text = "Our mission and security principles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SanXTextSecondary
                )
            }
        }

        // App Branding Soft Display
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600)) + expandVertically(animationSpec = tween(600))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, SanXBorder.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sanx_logo),
                        contentDescription = "WOMEN Shield Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Text(
                    text = "WOMEN App",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SanXTextPrimary
                )
                Text(
                    text = "Version 2.0.1",
                    fontSize = 12.sp,
                    color = SanXTextSecondary
                )
            }
        }

        // About Description Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SanXCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SanXBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Empowering Women's Safety Offline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF2B69)
                )
                Text(
                    text = "WOMEN (Wireless Offline Monitoring & Emergency Network) is built from the ground up to address critical scenarios where internet access is unavailable, cellular networks are congested, or active calls can endanger a victim.\n\nOur focus is complete, silent, and automated protection—reassuring you when you walk alone and keeping you safely backed up by technology and your trusted contacts.",
                    fontSize = 14.sp,
                    color = SanXTextPrimary,
                    lineHeight = 22.sp
                )
            }
        }

        // Core Concepts Columns
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConceptCard(
                icon = Icons.Default.Favorite,
                title = "Women Safety Focus",
                description = "Customized to work entirely silently. There are no sudden alarm sounds, flashing screens, or visible overlays that could trigger aggression from an attacker. Help flows background-first."
            )

            ConceptCard(
                icon = Icons.Default.NetworkWifi,
                title = "Bluetooth Low Energy Mesh",
                description = "A localized controlled mesh system that relays distress signals (A → B → C → D → E) across nearby phones without relying on cellular towers or mobile internet networks."
            )

            ConceptCard(
                icon = Icons.Default.Lock,
                title = "Trusted Circle live audio",
                description = "Ensures background recorded live audio can only be heard by designated contacts through cryptographically secure links, preventing nearby relay nodes from accessing private details."
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SanXSafeDim.copy(alpha = 0.05f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, SanXSafe.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "✨ Our Safety Manifesto",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SanXSafe
                )
                Text(
                    text = "WOMEN was designed with a single uncompromising belief: personal safety tools must be lightweight, silent, and entirely autonomous. We promise that the app will never track you, never display ads, and always protect your privacy with end-to-end cryptographic encryption. Your safety circle is your own.",
                    fontSize = 13.sp,
                    color = SanXTextPrimary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safety footnote
        Text(
            text = "Protection starts here. You are never alone.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = SanXTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConceptCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
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
                text = description,
                fontSize = 12.sp,
                color = SanXTextPrimary,
                lineHeight = 17.sp
            )
        }
    }
}
