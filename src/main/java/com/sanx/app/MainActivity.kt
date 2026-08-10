package com.sanx.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.sanx.app.service.EmergencyService
import com.sanx.app.ui.screens.*
import com.sanx.app.ui.theme.*
import com.sanx.app.ui.viewmodel.MainViewModel
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.sanx.app.ui.viewmodel.GhostDisguise
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale

/**
 * SanX Main Activity.
 * - Handles permission requests on first launch.
 * - Sets up the Compose UI with bottom navigation.
 * - Starts the EmergencyService in foreground mode.
 */
class MainActivity : ComponentActivity() {

    companion object {
        val triggerLockFlow = MutableStateFlow(0)
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        triggerLockFlow.value += 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load persisted theme
        val prefs = getSharedPreferences("sanx_prefs", Context.MODE_PRIVATE)
        AppTheme.isDark = prefs.getBoolean("app_theme_dark", true)

        // Hide Android System Status and Navigation Bars for an immersive full-screen safety HUD
        try {
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } catch (_: Exception) {}

        setContent {
            SanXTheme {
                SanXApp(
                    viewModel = viewModel,
                    onPermissionsGranted = { startEmergencyService() }
                )
            }
        }
    }

    private fun startEmergencyService() {
        val intent = Intent(this, EmergencyService::class.java).apply {
            action = EmergencyService.ACTION_START_MONITORING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        try {
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } catch (_: Exception) {}
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            val handled = EmergencyService.handleGlobalVolumeDownKeyPress()
            if (handled) return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

// ─── Routes ───────────────────────────────────────────────────────────────────

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    data object Home          : Screen("home",     Icons.Default.Home,             "Home")
    data object Triggers      : Screen("triggers", Icons.Default.Sensors,          "Triggers")
    data object Contacts      : Screen("contacts", Icons.Default.People,           "Circle")
    data object EmergencyLive : Screen("emergency",Icons.Default.Warning,          "Emergency")
    data object Mesh          : Screen("mesh",     Icons.Default.Bluetooth,        "Mesh")
    data object Ghost         : Screen("ghost",    Icons.Default.VisibilityOff,    "Ghost")
    data object Privacy       : Screen("privacy",  Icons.Default.Security,        "Privacy")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Triggers,
    Screen.Contacts,
    Screen.EmergencyLive,
    Screen.Mesh,
    Screen.Ghost,
    Screen.Privacy
)

// ─── Custom Drawer Components ──────────────────────────────────────────────────

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg: Color = if (selected) SanXSafeDim else Color.Transparent
    val tint: Color = if (selected) SanXSafe else SanXTextSecondary
    val borderCol: Color = if (selected) SanXSafe.copy(alpha = 0.3f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) SanXTextPrimary else SanXTextSecondary
        )
    }
}

@Composable
private fun ThemeToggleSwitch(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (isDark) 26.dp else 2.dp,
        animationSpec = tween(250, easing = EaseInOut),
        label = "thumbOffset"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF1E1E28) else Color(0xFFE2E2EC),
        animationSpec = tween(250),
        label = "bg"
    )
    
    val thumbColor by animateColorAsState(
        targetValue = Color(0xFFFF2B69),
        animationSpec = tween(250),
        label = "thumb"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SanXCard)
            .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
            .clickable { onToggle(!isDark) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = Color(0xFFFF2B69),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = if (isDark) "Dark Mode" else "Light Mode",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SanXTextPrimary
            )
        }

        // Custom Toggle Pill Track
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(backgroundColor)
                .padding(2.dp)
        ) {
            // Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            )
        }
    }
}

// ─── Root App Composable ──────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SanXApp(
    viewModel: MainViewModel,
    onPermissionsGranted: () -> Unit
) {
    var showIntro by remember { mutableStateOf(true) }

    if (showIntro) {
        IntroScreen(onFinished = { showIntro = false })
        return
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.VIBRATE)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(Manifest.permission.READ_PHONE_NUMBERS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    val allGranted = permissionsState.allPermissionsGranted
    var currentRoute by remember { mutableStateOf(Screen.Home.route) }
    val emergencySession by viewModel.emergencySession.collectAsState()
    var activeHearingSessionId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val activityIntent = (context as? android.app.Activity)?.intent
    val navigateTo = activityIntent?.getStringExtra("navigate_to")
    val intentSessionId = activityIntent?.getStringExtra("session_id")

    LaunchedEffect(navigateTo, intentSessionId) {
        if (navigateTo == "hear_live_audio" && !intentSessionId.isNullOrEmpty()) {
            activeHearingSessionId = intentSessionId
            currentRoute = "live_hearing"
            activityIntent.removeExtra("navigate_to")
            activityIntent.removeExtra("session_id")
        }
    }

    // Notify activity to start service once permissions are valid
    LaunchedEffect(allGranted) {
        if (allGranted) {
            onPermissionsGranted()
        }
    }

    // Auto-navigate to emergency live when session activates
    LaunchedEffect(emergencySession) {
        if (emergencySession != null) {
            currentRoute = Screen.EmergencyLive.route
        }
    }

    val ghostModeEnabled by viewModel.ghostModeEnabled.collectAsState()
    val appLockEnabled by viewModel.appLockEnabled.collectAsState()
    val ghostPin by viewModel.ghostPin.collectAsState()
    val ghostDisguise by viewModel.ghostDisguise.collectAsState()

    var isGhostUnlocked by remember { mutableStateOf(false) }
    var isAppUnlocked by remember { mutableStateOf(false) }

    // Unified Lifecycle re-locking when app is minimized or sent to background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                isGhostUnlocked = false
                isAppUnlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Direct notification click / new intent re-locking
    val triggerLock by MainActivity.triggerLockFlow.collectAsState()
    LaunchedEffect(triggerLock) {
        if (triggerLock > 0) {
            isGhostUnlocked = false
            isAppUnlocked = false
        }
    }

    LaunchedEffect(emergencySession) {
        if (emergencySession == null) {
            isGhostUnlocked = false
        }
    }

    val prefs = remember { context.getSharedPreferences("sanx_prefs", Context.MODE_PRIVATE) }
    var userPhone by remember { mutableStateOf(prefs.getString("user_phone", "") ?: "") }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }

    if (!allGranted) {
        PermissionSetupScreen(
            onRequest = { permissionsState.launchMultiplePermissionRequest() }
        )
        return
    }

    var showWelcome by remember { mutableStateOf(false) }

    // Two-Stage Personalized Onboarding on first launch
    if (userName.isEmpty() || userPhone.isEmpty()) {
        OnboardingFlowScreen(
            onComplete = { name, phone, subId ->
                prefs.edit().apply {
                    putString("user_name", name)
                    putString("user_phone", phone)
                    putInt("user_sub_id", subId)
                    putBoolean("user_verified", true)
                    apply()
                }
                userName = name
                userPhone = phone
                showWelcome = true
            }
        )
        return
    }

    if (showWelcome) {
        WelcomeScreen(
            username = userName,
            onFinished = { showWelcome = false }
        )
        return
    }

    // App Lock Lockout on launch if enabled under Ghost Mode (Calculator or Passcode screen depending on selection)
    if (ghostModeEnabled && appLockEnabled && !isAppUnlocked) {
        if (ghostDisguise == GhostDisguise.CALCULATOR) {
            CalculatorDisguiseScreen(
                correctPin = ghostPin,
                onUnlock = { isAppUnlocked = true }
            )
        } else {
            SecurePinLockScreen(
                correctPin = ghostPin,
                onUnlock = { isAppUnlocked = true }
            )
        }
        return
    }

    // Emergency Session HUD Lockout (Calculator or Passcode screen depending on selection)
    // AUTOMATIC GHOST MODE DURING ACTIVE EMERGENCY: Force disguise even if ghostModeEnabled is false in settings
    if (emergencySession != null && !isGhostUnlocked) {
        if (ghostDisguise == GhostDisguise.CALCULATOR) {
            CalculatorDisguiseScreen(
                correctPin = ghostPin,
                onUnlock = { isGhostUnlocked = true }
            )
        } else {
            SecurePinLockScreen(
                correctPin = ghostPin,
                onUnlock = { isGhostUnlocked = true }
            )
        }
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SanXSurface,
                drawerContentColor = SanXTextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Header with Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    currentRoute = Screen.Home.route
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, SanXBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.women_logo),
                                    contentDescription = "WOMEN Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column {
                                Text(
                                    text = "WOMEN",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SanXTextPrimary
                                )
                                Text(
                                    text = "Protection Starts Now",
                                    fontSize = 11.sp,
                                    color = SanXTextSecondary
                                )
                            }
                        }

                        HorizontalDivider(color = SanXBorder, thickness = 1.dp)

                        // Navigation Items
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DrawerItem(
                                icon = Icons.Default.Dashboard,
                                label = "Safety Dashboard",
                                selected = currentRoute == Screen.Home.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    currentRoute = Screen.Home.route
                                }
                            )

                            DrawerItem(
                                icon = Icons.Default.Info,
                                label = "How To Use",
                                selected = currentRoute == "how_to_use",
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    currentRoute = "how_to_use"
                                }
                            )

                            DrawerItem(
                                icon = Icons.Default.Favorite,
                                label = "About App",
                                selected = currentRoute == "about_app",
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    currentRoute = "about_app"
                                }
                            )

                            DrawerItem(
                                icon = Icons.Default.PrivacyTip,
                                label = "Privacy Policy",
                                selected = currentRoute == "privacy_policy",
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    currentRoute = "privacy_policy"
                                }
                            )
                        }
                    }

                    // Theme Toggle at the bottom
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ThemeToggleSwitch(
                            isDark = AppTheme.isDark,
                            onToggle = { dark ->
                                AppTheme.isDark = dark
                                prefs.edit().putBoolean("app_theme_dark", dark).apply()
                            }
                        )
                        
                        Text(
                            text = "WOMEN Safety Network © 2026",
                            fontSize = 10.sp,
                            color = SanXTextDisabled,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        gesturesEnabled = emergencySession == null // Disable gestures during emergencies
    ) {
        Scaffold(
            containerColor = SanXBlack,
            bottomBar = {
                // Hide the application bottom navigation bar during active emergencies or custom screens to keep it clean
                if (emergencySession == null && currentRoute != "how_to_use" && currentRoute != "about_app" && currentRoute != "privacy_policy") {
                    SanXBottomNav(
                        currentRoute = currentRoute,
                        hasActiveEmergency = false,
                        onNavigation = { currentRoute = it }
                    )
                }
            }
        ) { innerPadding ->
            val padding = if (emergencySession != null || currentRoute == Screen.EmergencyLive.route || currentRoute == "live_hearing") {
                PaddingValues(0.dp)
            } else {
                innerPadding
            }
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) togetherWith
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                    },
                    label = "screenTransition"
                ) { route ->
                    when (route) {
                        Screen.Home.route          -> HomeScreen(viewModel, onNavigateToEmergencyLive = { currentRoute = Screen.EmergencyLive.route }, onMenuClick = { scope.launch { drawerState.open() } })
                        Screen.Triggers.route      -> TriggersScreen(viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                        Screen.Contacts.route      -> ContactsScreen(viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                        Screen.EmergencyLive.route -> EmergencyLiveScreen(viewModel)
                        Screen.Mesh.route          -> MeshScreen(viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                        Screen.Ghost.route         -> GhostScreen(viewModel, onMenuClick = { scope.launch { drawerState.open() } })
                        Screen.Privacy.route       -> PrivacyScreen(onMenuClick = { scope.launch { drawerState.open() } })
                        "how_to_use"               -> HowToUseScreen(onBack = { currentRoute = Screen.Home.route })
                        "about_app"                -> AboutAppScreen(onBack = { currentRoute = Screen.Home.route })
                        "privacy_policy"           -> PrivacyPolicyScreen(onBack = { currentRoute = Screen.Home.route })
                        "live_hearing"             -> LiveHearingScreen(
                            sessionId = activeHearingSessionId ?: "UNKNOWN",
                            viewModel = viewModel,
                            onBack = { currentRoute = Screen.Home.route }
                        )
                        else                       -> HomeScreen(viewModel, onNavigateToEmergencyLive = {}, onMenuClick = { scope.launch { drawerState.open() } })
                    }
                }
            }
        }
    }
}

// ─── Bottom Navigation ────────────────────────────────────────────────────────

@Composable
private fun SanXBottomNav(
    currentRoute: String,
    hasActiveEmergency: Boolean,
    onNavigation: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SanXSurface)
            .border(
                width = 1.dp,
                color = if (hasActiveEmergency) SanXEmergency.copy(alpha = 0.3f) else SanXBorder,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route
                val isEmergencyTab = screen == Screen.EmergencyLive

                val iconColor = when {
                    isEmergencyTab && hasActiveEmergency -> SanXEmergency
                    selected && !isEmergencyTab          -> SanXSafe
                    else                                 -> SanXTextDisabled
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigation(screen.route) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.label,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                        // Emergency badge dot
                        if (isEmergencyTab && hasActiveEmergency) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SanXEmergency)
                                    .border(1.dp, SanXSurface, CircleShape)
                            )
                        }
                    }
                    Text(
                        text = screen.label,
                        fontSize = 9.sp,
                        color = iconColor,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

// ─── Permission Setup Screen ──────────────────────────────────────────────────

@Composable
private fun PermissionSetupScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanXBlack)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "WOMEN",
            tint = SanXSafe,
            modifier = Modifier.size(64.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("WOMEN needs permissions",
                style = MaterialTheme.typography.headlineMedium,
                color = SanXTextPrimary)
            Text(
                "To protect you silently, WOMEN requires location, microphone, camera, and Bluetooth access. All data stays private and encrypted on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = SanXTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SanXSafe,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Security, contentDescription = null,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Grant Permissions", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ─── Calculator Disguise Screen (Ghost Mode) ───────────────────────────────────

@Composable
fun CalculatorDisguiseScreen(correctPin: String, onUnlock: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var clearOnNextKey by remember { mutableStateOf(false) }

    fun evaluate(val1: Double, val2: Double, op: String): Double {
        return when (op) {
            "+" -> val1 + val2
            "-" -> val1 - val2
            "*" -> val1 * val2
            "/" -> if (val2 != 0.0) val1 / val2 else 0.0
            else -> val2
        }
    }

    fun onKeyPress(key: String) {
        when {
            key == "C" -> {
                display = "0"
                expression = ""
                clearOnNextKey = false
            }
            key in listOf("+", "-", "*", "/") -> {
                expression = "$display $key"
                clearOnNextKey = true
            }
            key == "=" -> {
                if (display == correctPin) {
                    onUnlock()
                    return
                }
                if (expression.isNotEmpty()) {
                    val parts = expression.split(" ")
                    if (parts.size >= 2) {
                        val val1 = parts[0].toDoubleOrNull() ?: 0.0
                        val op = parts[1]
                        val val2 = display.toDoubleOrNull() ?: 0.0
                        val res = evaluate(val1, val2, op)
                        display = if (res % 1 == 0.0) res.toInt().toString() else res.toString()
                    }
                    expression = ""
                    clearOnNextKey = true
                }
            }
            key == "." -> {
                if (!display.contains(".")) {
                    display += "."
                }
            }
            key.isNotEmpty() && key[0].isDigit() -> {
                if (display == "0" || clearOnNextKey) {
                    display = key
                    clearOnNextKey = false
                } else {
                    display += key
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanXBlack)
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Output display
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            if (expression.isNotEmpty()) {
                Text(
                    text = expression,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SanXTextDisabled,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = display,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                color = SanXTextPrimary,
                maxLines = 1,
                softWrap = false
            )
        }

        // Divider
        HorizontalDivider(color = SanXBorder, thickness = 1.dp)

        // Calculator Keys
        val rows = listOf(
            listOf("C", "(", ")", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=", "")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    ) {
                        if (key.isNotEmpty()) {
                            val isOperator = key in listOf("/", "*", "-", "+", "=")
                            val isSpecial = key in listOf("C", "(", ")")
                            
                            val containerColor = when {
                                key == "=" -> SanXSafe
                                isOperator -> SanXCard
                                isSpecial -> SanXBorder
                                else -> SanXSurface
                            }
                            
                            val contentColor = when {
                                key == "=" -> SanXBlack
                                isOperator -> SanXSafe
                                else -> SanXTextPrimary
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(containerColor)
                                    .clickable { onKeyPress(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── SIM Info Model ──────────────────────────────────────────────────────────
data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val number: String,
    val carrierName: String
)

// ─── Secure PIN Lock Screen (App Lock) ────────────────────────────────────────
@Composable
fun SecurePinLockScreen(correctPin: String, onUnlock: () -> Unit) {
    var enteredCode by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun onNumberPress(num: String) {
        if (enteredCode.length < 4) {
            enteredCode += num
            isError = false
            if (enteredCode.length == 4) {
                if (enteredCode == correctPin) {
                    onUnlock()
                } else {
                    isError = true
                    scope.launch {
                        delay(600)
                        enteredCode = ""
                        isError = false
                    }
                }
            }
        }
    }

    fun onDeletePress() {
        if (enteredCode.isNotEmpty()) {
            enteredCode = enteredCode.dropLast(1)
            isError = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanXBlack)
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isError) SanXEmergency.copy(alpha = 0.15f) else SanXSafe.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isError) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isError) SanXEmergency else SanXSafe,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = if (isError) "Incorrect Passcode" else "Enter Passcode",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isError) SanXEmergency else SanXTextPrimary
            )
            Text(
                text = "Securely unlock your SanX vault",
                style = MaterialTheme.typography.bodyMedium,
                color = SanXTextSecondary
            )
        }

        // Passcode DOTS indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val active = i < enteredCode.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isError) SanXEmergency else if (active) SanXSafe else SanXBorder)
                        .border(
                            width = 1.dp,
                            color = if (isError) SanXEmergency else if (active) SanXSafe else SanXTextDisabled,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Premium Custom Numeric Pad grid
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "DEL")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (key.isNotEmpty()) {
                                val isAction = key == "C" || key == "DEL"
                                val containerColor = if (isAction) Transparent else SanXSurface
                                val contentColor = if (isError && !isAction) SanXEmergency else if (isAction) SanXTextSecondary else SanXTextPrimary

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(containerColor)
                                        .border(
                                            width = if (isAction) 0.dp else 1.dp,
                                            color = if (isError && !isAction) SanXEmergency.copy(alpha = 0.3f) else SanXBorder,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            when (key) {
                                                "C" -> {
                                                    enteredCode = ""
                                                    isError = false
                                                }
                                                "DEL" -> onDeletePress()
                                                else -> onNumberPress(key)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") {
                                        @Suppress("DEPRECATION")
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Delete",
                                            tint = contentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

// ─── Two-Stage Personalized Onboarding ────────────────────────────────────────
@Composable
fun OnboardingFlowScreen(
    onComplete: (name: String, phone: String, subId: Int) -> Unit
) {
    var stage by remember { mutableIntStateOf(1) } // 1: Name entry, 2: Welcome & SIM select
    var name by remember { mutableStateOf("") }
    var selectedSimSlot by remember { mutableStateOf<Int?>(null) }
    var selectedSubId by remember { mutableIntStateOf(-1) }
    var phoneNumber by remember { mutableStateOf("") }

    val context = LocalContext.current
    val subscriptionManager = remember { context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager }
    
    var hasPhonePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_NUMBERS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { map ->
            hasPhonePermission = map.values.all { it }
        }
    )

    // Request permissions automatically when stage 2 starts
    LaunchedEffect(stage) {
        if (stage == 2 && !hasPhonePermission) {
            permissionLauncher.launch(
                buildList {
                    add(android.Manifest.permission.READ_PHONE_STATE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        add(android.Manifest.permission.READ_PHONE_NUMBERS)
                    }
                }.toTypedArray()
            )
        }
    }

    val activeSims = remember(hasPhonePermission) {
        val list = mutableListOf<SimInfo>()
        try {
            if (hasPhonePermission && subscriptionManager != null) {
                val subList = subscriptionManager.activeSubscriptionInfoList
                if (subList != null) {
                    for (info in subList) {
                        var num = ""
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            num = subscriptionManager.getPhoneNumber(info.subscriptionId)
                        }
                        if (num.isEmpty()) {
                            @Suppress("DEPRECATION")
                            num = info.number ?: ""
                        }
                        list.add(
                            SimInfo(
                                subscriptionId = info.subscriptionId,
                                slotIndex = info.simSlotIndex,
                                number = num,
                                carrierName = info.carrierName?.toString() ?: "Carrier"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        // Fallback only if no active SIMs are detected (so the list is not completely blank in sandbox/emulators)
        if (list.isEmpty()) {
            list.add(SimInfo(-1, 0, "", "SIM Slot 1"))
            list.add(SimInfo(-1, 1, "", "SIM Slot 2"))
        }
        list.sortedBy { it.slotIndex }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SanXBlack)
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Shield Header Icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SanXSafe.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = SanXSafe,
                modifier = Modifier.size(32.dp)
            )
        }

        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                fadeOut(animationSpec = tween(200))
            },
            label = "onboardingTransition",
            modifier = Modifier.wrapContentHeight()
        ) { currentStage ->
            when (currentStage) {
                1 -> {
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
                                text = "Setup Your Profile",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = SanXTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Welcome to WOMEN. Enter your name to customize your safety vault details.",
                                fontSize = 14.sp,
                                color = SanXTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SanXTextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = SanXTextPrimary,
                                    unfocusedTextColor = SanXTextPrimary,
                                    focusedBorderColor = SanXSafe,
                                    unfocusedBorderColor = SanXBorder,
                                    focusedContainerColor = SanXSurface,
                                    unfocusedContainerColor = SanXSurface
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { if (name.trim().isNotEmpty()) stage = 2 },
                            enabled = name.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SanXSafe,
                                contentColor = Color.White,
                                disabledContainerColor = SanXBorder,
                                disabledContentColor = SanXTextDisabled
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            @Suppress("DEPRECATION")
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                2 -> {
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
                                text = "Welcome, ${name.trim()}!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = SanXTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Select your active SIM card phone number below to finish your profile setup.",
                                fontSize = 14.sp,
                                color = SanXTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // SIM selection grid (Only active SIMs are displayed)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            activeSims.forEach { sim ->
                                val isSelected = selectedSimSlot == sim.slotIndex
                                val borderStroke = if (isSelected) BorderStroke(1.5.dp, SanXSafe) else BorderStroke(1.dp, SanXBorder)
                                val bg = if (isSelected) SanXSafe.copy(alpha = 0.08f) else SanXSurface

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(bg)
                                        .border(borderStroke, RoundedCornerShape(16.dp))
                                        .clickable {
                                            selectedSimSlot = sim.slotIndex
                                            selectedSubId = sim.subscriptionId
                                            phoneNumber = sim.number
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) SanXSafe.copy(alpha = 0.15f) else SanXSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SimCard,
                                            contentDescription = null,
                                            tint = if (isSelected) SanXSafe else SanXTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "SIM Slot ${sim.slotIndex + 1} (${sim.carrierName})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SanXTextPrimary
                                        )
                                        Text(
                                            text = if (sim.number.isNotEmpty()) sim.number else "Number not auto-detected",
                                            fontSize = 13.sp,
                                            color = if (sim.number.isNotEmpty()) SanXTextSecondary else SanXTextDisabled
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedSimSlot = sim.slotIndex
                                            selectedSubId = sim.subscriptionId
                                            phoneNumber = sim.number
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = SanXSafe,
                                            unselectedColor = SanXBorder
                                        )
                                    )
                                }
                            }
                        }

                        // Fallback manual input if a slot is selected
                        if (selectedSimSlot != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "PHONE NUMBER (EDIT IF NEEDED)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SanXTextSecondary,
                                    letterSpacing = 1.sp
                                )
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    placeholder = { Text("e.g. +91 98765 43210", color = SanXTextHint) },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SanXTextSecondary) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = SanXTextPrimary,
                                        unfocusedTextColor = SanXTextPrimary,
                                        focusedBorderColor = SanXSafe,
                                        unfocusedBorderColor = SanXBorder,
                                        focusedContainerColor = SanXSurface,
                                        unfocusedContainerColor = SanXSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = { stage = 1 },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SanXTextPrimary),
                                border = BorderStroke(1.dp, SanXBorder),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            ) {
                                Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (phoneNumber.trim().isNotEmpty()) {
                                        onComplete(name.trim(), phoneNumber.trim(), selectedSubId)
                                    }
                                },
                                enabled = selectedSimSlot != null && phoneNumber.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SanXSafe,
                                    contentColor = Color.White,
                                    disabledContainerColor = SanXBorder,
                                    disabledContentColor = SanXTextDisabled
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            ) {
                                Text("Confirm", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Minimal & Eye-catching Intro/Splash Screen ──────────────────────────────
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    var shieldVisible by remember { mutableStateOf(false) }
    var glowVisible by remember { mutableStateOf(false) }

    val shieldAlpha by animateFloatAsState(
        targetValue = if (shieldVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseInOut),
        label = "shieldAlpha"
    )
    val shieldScale by animateFloatAsState(
        targetValue = if (shieldVisible) 1f else 0.9f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutBack),
        label = "shieldScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (glowVisible) 0.35f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = EaseInOut),
        label = "glowAlpha"
    )
    val glowScale by animateFloatAsState(
        targetValue = if (glowVisible) 1.35f else 1.0f,
        animationSpec = tween(durationMillis = 1200, easing = EaseOutBack),
        label = "glowScale"
    )

    LaunchedEffect(Unit) {
        delay(200L)
        shieldVisible = true
        delay(150L)
        // Trigger gentle soft glow pulse once!
        glowVisible = true
        delay(1100L)
        // Fade the glow out softly
        glowVisible = false
        delay(1350L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SanXBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // 1. High-premium rounded image containing the uploaded WOMEN logo shield with a hardware radial glow halo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(230.dp)
            ) {
                // Background soft glow pulse circle
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(glowScale)
                        .alpha(glowAlpha)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(Color(0xFFFF2B69), Color.Transparent),
                                radius = 270f
                            )
                        )
                )

                // The Logo Shield Card
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(shieldScale)
                        .alpha(shieldAlpha)
                        .clip(RoundedCornerShape(44.dp))
                        .border(1.dp, SanXBorder.copy(alpha = 0.5f), RoundedCornerShape(44.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.women_logo),
                        contentDescription = "WOMEN Shield Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 2. Logo shield stands alone centered on matte background (text removed)
        }
    }
}

// ─── Emotionally Warm & Safe Welcome Screen ───────────────────────────────────
@Composable
fun WelcomeScreen(username: String, onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseInOut),
        label = "welcomeAlpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2200L) // Show for 2.2 seconds
        visible = false
        delay(600L)  // Fade out for 600ms
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SanXBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .alpha(alpha)
        ) {
            // 1. Official WOMEN Logo Shield Card softly rendered at the center
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, SanXBorder.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.women_logo),
                    contentDescription = "WOMEN Shield Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Warm greeting typography
            Text(
                text = "Welcome, $username",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SanXTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "You became a member in WOMEN.",
                fontSize = 15.sp,
                color = Color(0xFFFF2B69), // Brand pink accent
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Subtle sub-line
            Text(
                text = "Protection starts now.",
                fontSize = 13.sp,
                color = SanXTextSecondary, // Soft grey secondary text
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
        }
    }
}


