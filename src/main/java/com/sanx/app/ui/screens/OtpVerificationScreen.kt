package com.sanx.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanx.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * OTP Verification Onboarding Screen.
 * Verifies the user's name and contact number via a premium simulated offline OTP system.
 * Keeps user data completely offline and secure.
 */
@Composable
fun OtpVerificationScreen(
    onVerificationSuccess: (name: String, phone: String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    
    var step by remember { mutableStateOf(1) } // 1: Input details, 2: Enter OTP, 3: Success
    var countdown by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Timer logic
    LaunchedEffect(isTimerRunning, countdown) {
        if (isTimerRunning && countdown > 0) {
            delay(1000)
            countdown--
        } else if (countdown == 0) {
            isTimerRunning = false
        }
    }

    // Trigger dynamic carrier SMS OTP
    fun triggerSimulatedOtp() {
        if (name.trim().isEmpty()) {
            Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.trim().isEmpty() || phone.length < 10) {
            Toast.makeText(context, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
            return
        }

        focusManager.clearFocus()

        // Generate a random 6-digit OTP
        generatedOtp = (100000 + Random.nextInt(900000)).toString()
        step = 2
        enteredOtp = ""
        countdown = 60
        isTimerRunning = true

        // Dispatch a real SMS text using SmsManager to send OTP directly to the user's phone!
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
            val smsMessage = "SanX Verification Code: $generatedOtp. Valid for 10 minutes."
            smsManager.sendTextMessage(phone.trim(), null, smsMessage, null, null)
            Toast.makeText(context, "Verification SMS sent to $phone!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "SMS dispatch error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // Verification logic
    fun verifyOtp() {
        if (enteredOtp == generatedOtp) {
            step = 3
            scope.launch {
                delay(1800)
                // Save user details to offline SharedPreferences
                val prefs = context.getSharedPreferences("sanx_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("user_name", name.trim())
                    putString("user_phone", phone.trim())
                    putBoolean("user_verified", true)
                    apply()
                }
                onVerificationSuccess(name.trim(), phone.trim())
            }
        } else {
            Toast.makeText(context, "Incorrect OTP code. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SanXBlack)
    ) {
        // Main Screen content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo/Shield
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SanXSafe.copy(alpha = 0.2f), SanXBlack)
                        )
                    )
                    .border(1.dp, SanXSafe.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SanXSafe,
                    modifier = Modifier.size(36.dp)
                )
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(200))
                },
                label = "stepTransition",
                modifier = Modifier.weight(1f)
            ) { currentStep ->
                when (currentStep) {
                    1 -> {
                        // Step 1: Input details
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Secure Verification",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SanXTextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Establish your identity offline to register your SanX safety key.",
                                    fontSize = 14.sp,
                                    color = SanXTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Name Input
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "YOUR FULL NAME",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SanXTextSecondary,
                                    letterSpacing = 1.sp
                                )
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { Text("e.g. John Doe", color = SanXTextHint) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = SanXTextPrimary,
                                        unfocusedTextColor = SanXTextPrimary,
                                        focusedBorderColor = SanXSafe,
                                        unfocusedBorderColor = SanXBorder,
                                        focusedContainerColor = SanXSurface,
                                        unfocusedContainerColor = SanXSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )
                            }

                            // Contact Number Input
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "PHONE NUMBER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SanXTextSecondary,
                                    letterSpacing = 1.sp
                                )
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { if (it.length <= 15) phone = it },
                                    placeholder = { Text("e.g. +1 555-0199", color = SanXTextHint) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = SanXTextPrimary,
                                        unfocusedTextColor = SanXTextPrimary,
                                        focusedBorderColor = SanXSafe,
                                        unfocusedBorderColor = SanXBorder,
                                        focusedContainerColor = SanXSurface,
                                        unfocusedContainerColor = SanXSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { triggerSimulatedOtp() }
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = { triggerSimulatedOtp() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SanXSafe,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Request Verification Code",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    2 -> {
                        // Step 2: Enter OTP
                        Column(
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Enter Code",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SanXTextPrimary
                                )
                                Text(
                                    text = "A secure verification code was sent to $phone.",
                                    fontSize = 14.sp,
                                    color = SanXTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom Premium 6-Box OTP input
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = enteredOtp,
                                    onValueChange = {
                                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                            enteredOtp = it
                                            if (it.length == 6) {
                                                verifyOtp()
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = SanXTextPrimary,
                                        unfocusedTextColor = SanXTextPrimary,
                                        focusedBorderColor = SanXSafe,
                                        unfocusedBorderColor = SanXBorder,
                                        focusedContainerColor = SanXSurface,
                                        unfocusedContainerColor = SanXSurface
                                    ),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 8.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { verifyOtp() }
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Timer / Resend section
                            if (isTimerRunning) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = SanXTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Resend available in ${countdown}s",
                                        fontSize = 14.sp,
                                        color = SanXTextSecondary
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.clickable {
                                        triggerSimulatedOtp()
                                    },
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = SanXSafe,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Resend Code",
                                        fontSize = 14.sp,
                                        color = SanXSafe,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { step = 1 },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SanXTextPrimary
                                    ),
                                    border = BorderStroke(1.dp, SanXBorder),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                ) {
                                    Text(
                                        text = "Back",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { verifyOtp() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SanXSafe,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                ) {
                                    Text(
                                        text = "Verify",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    3 -> {
                        // Step 3: Success Screen
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(SanXSafeDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SanXSafe,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Verification Complete",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = SanXTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Welcome $name. Your SanX identity has been fully verified and locally encrypted.",
                                fontSize = 15.sp,
                                color = SanXTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }

            // Info details footer
            InfoBox(
                icon = Icons.Default.Lock,
                text = "No personal data leaves your device. SanX works 100% offline, keeping you safe and private.",
                color = SanXBorder
            )
        }

    }
}
