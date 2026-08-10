# WOMEN — Wireless Offline Monitoring & Emergency Network

> **Invisible Protection. Visible Safety.**  
> A premium, offline-first personal safety infrastructure and emergency communication system for Android.

---

## 📖 Introduction & Philosophy

Typical personal safety apps fail in high-threat scenarios because they rely on loud alarms, flashing screens, or active calls that draw unwanted attention. Furthermore, they are useless in areas with poor cellular service or congested networks.

**WOMEN** is designed with a single, uncompromising belief: **personal safety tools must be lightweight, silent, and entirely autonomous.** 

The application runs background-first. It remains completely hidden from an attacker using dynamic disguise overlays (Ghost Mode) and continues to secure critical evidence (live WebRTC audio) and propagate offline distress signals using a decentralized Bluetooth Low Energy (BLE) mesh network.

---

## 🚀 Key Features

### 1. Silent Background Triggers & Interception
* **Panic Shake**: Utilizes the device's accelerometer to register aggressive, repeated shake gestures.
* **Back Triple Tap**: Uses motion sensors to detect three rapid taps on the back of the device.
* **Accessibility Key Interception**: A custom `AccessibilityService` runs silently in the background, capturing hardware volume button patterns without overlays.

### 2. False Alert Protection & Silent Cancellation
* **7-Second Warning Vibration**: When a trigger fires, the device starts a continuous, maximum-intensity warning vibration for 7 seconds. This vibration bypasses silent/Do Not Disturb (DND) modes using sonification audio attributes.
* **Volume Down x3 Cancel**: Pressing the physical **Volume Down button 3 times continuously** within the 7-second warning window cancels the alert silently. The vibration ceases immediately, and a discrete triple-vibe confirmation occurs. No visual alerts, banners, or popups are ever displayed.
* **Automatic Escalation**: If the 7 seconds elapse without cancellation, the system silently enters full active emergency mode.

### 3. Programmatic Hardware Radio Orchestration
* **Auto-Radio Arming**: Upon emergency activation, the service automatically enables **Bluetooth, GPS (Location Services), and Mobile Data** programmatically without user intervention.
* **Auto-Radio Disarming**: Once the emergency is cancelled, the service disables Bluetooth, GPS, and Mobile Data to conserve battery and restore the device's original state.

### 4. Bluetooth Low Energy (BLE) Mesh Network
* **Offline Distress Propagation**: If cellular and internet services are offline, the app broadcasts GCM-encrypted distress beacons via BLE.
* **Multi-Hop Relaying**: Nearby devices running the app act as blind relay hops (A → B → C → D → E) to propagate coordinates to the nearest active gateway.
* **Absolute Privacy Boundaries**: Nearby nodes function strictly as blind routing links. Relay nodes have **absolute zero visibility** into live audio feeds, coordinates, victim names, or passcodes.

### 5. WebRTC Secure Live Audio Streaming
* **Real-Time Encrypted Feeds**: Silently streams ambient microphone audio to trusted contacts using WebRTC (`110ms - 130ms` latency, `SRTP / AES-GCM` encryption, `WebRTC / AAC-ELD 16kbps` audio codecs).
* **High-Entropy Access Code**: Trusted contacts receive a Google Maps link and an automatically generated **10-12 character access code** containing numbers, lowercase/uppercase letters, and special symbols (e.g. `N7#K2@Q9L!X`).
* **Brute-Force Lockout**: If an attacker attempts to access the stream and inputs an incorrect code **5 times**, the hearing gate displays a secure red lockout card, disabling access for **5 minutes**.
* **Offline Recording Fallback**: If internet connectivity drops, the app automatically switches to silent local sandbox recording (`OFFLINE_RECORDING`) and reconnects to WebRTC within 1.5 seconds of network restoration.

### 6. Ghost Mode (Calculator & Screen Disguises)
* **Calculator Disguise**: If the app is launched during an active emergency or on start, it displays a fully functional calculator. Entering a secret bypass PIN (e.g., `9999`) unlocks the dashboard.
* **Locked Screen Disguise**: Displays a fake lock screen identical to the system lock.
* **Lifecycle Re-locking**: The app locks itself back to the disguise the moment the app goes to the background (`ON_STOP`) or if the persistent emergency notification is clicked.

### 7. Harmless System Sync Widget
* **System Disguise**: Appears in the widget selector as a generic auto-sync toggle widget (illuminates green when active).
* **Force-Armed Emergency**: Toggling it turns on the vibration warning but **disables/ignores physical Volume Down cancellation**. The emergency activates forcibly after 7 seconds.
* **Two-Way Synchronization**: Ending the emergency in the app automatically flips the home screen widget switch back to the gray "OFF" state.

---

## 📁 Technical Project Structure

```
WOMEN/
├── app/
│   ├── build.gradle.kts                     # App dependencies & SDK versions
│   └── src/main/
│       ├── AndroidManifest.xml              # Permissions, Services, Accessibility configuration
│       ├── java/com/sanx/app/
│       │   ├── SanXApplication.kt           # Application init, Notification channels, Room DB
│       │   ├── MainActivity.kt              # App entry point, Nav drawer, Onboarding, AppLock
│       │   ├── data/
│       │   │   ├── local/                   # Room entities, DAOs, encrypted local db
│       │   │   ├── model/                   # Domain models (Severity, Session, MeshNode…)
│       │   │   └── repository/              # Emergency Repository, SMS Alert Templates
│       │   ├── service/
│       │   │   ├── EmergencyService.kt      # Core foreground service, radios, countdowns
│       │   │   ├── SanXAccessibilityService.kt # Hardware Volume Down interception
│       │   │   ├── BootReceiver.kt          # Restarts monitoring on device reboot
│       │   │   ├── trigger/                 # Motion panic shake and double/triple tap detectors
│       │   │   ├── ble/                     # BLE advertiser and scanner mesh network
│       │   │   ├── media/                   # Silent audio recorder & simulated WebRTC stream
│       │   │   └── ai/                      # Danger score heuristic engine
│       │   └── ui/
│       │       ├── theme/                   # Material3 themes, typography, colors
│       │       ├── viewmodel/               # MainViewModel (State management)
│       │       └── screens/                 # Dashboard, Contacts, Mesh, Privacy screens
│       └── res/
│           ├── drawable/                    # Premium vectors and branding logos
│           └── xml/                         # Accessibility Service config, Backup rules
└── settings.gradle.kts
```

---

## 🛠️ Required Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | Live Fused Location coordinate logging during active emergencies. |
| `RECORD_AUDIO` | Silent evidence collection and WebRTC microphone streaming. |
| `CAMERA` | Evidence photo capture. |
| `BLUETOOTH_SCAN / ADVERTISE / CONNECT` | Discovery, advertisements, and multi-hop relaying on the offline mesh. |
| `SEND_SMS` | Offline SMS alert fallback with Google Maps link and access code. |
| `FOREGROUND_SERVICE` | Runs the active emergency service persistently under Android lifecycle rules. |
| Accessibility Service | Captures physical volume button patterns in DND/locked states. |

---

## 🎨 UI/UX Design System

The app utilizes a dual-theme state-driven architecture, adapting elements reactively to prevent high brightness emissions under notches.

### Color Tokens

| Token | Light Value | Dark Value | Purpose |
|---|---|---|---|
| `SanXBlack` | `#F6F6F9` | `#0A0A0C` | Root background |
| `SanXSurface` | `#FFFFFF` | `#16161E` | Main surfaces |
| `SanXCard` | `#EFEFF4` | `#1E1E28` | Dashboard card elements |
| `SanXBorder` | `#E2E2EC` | `#2A2A38` | Card border lines and dividers |
| `SanXTextPrimary` | `#1C1C1E` | `#F0F0F5` | Primary body text and headers |
| `SanXTextSecondary`| `#6E6E73` | `#8888A0` | Secondary description text |
| `SanXSafe` | `#FF2B69` | `#FF2B69` | Rose-pink branding accents |
| `SanXSafeDim` | `#22FF2B69` | `#33FF2B69` | Pulsing accent glows |

### Status Indicators

| Indicator | Color Value | Meaning |
|---|---|---|
| `SanXInfo` | `#5B9CF6` | Safety Info Blue (Level 1 Emergency - Silent) |
| `SanXWarning` | `#FFFF9500` | Alert Orange (Level 2 Emergency - Alert) |
| `SanXEmergency`| `#FFFF4545` | Critical Red (Level 3 Emergency - Critical) |
| `SanXMesh` | `#C77DFF` | Mesh Network Purple |

* **Stealth Viewport locking**: Active emergency screens (`EmergencyLiveScreen` and `LiveHearingScreen`) enforce the volcanic black background (`#0A0A0C`) and high-contrast white text layout regardless of whether the user has toggled the app to Light Mode, guaranteeing zero-emissions and physical stealth.

---

## 📦 Build & Development Toolchain

Ensure compilation tools target JBR-17/Java-17 to compile successfully.

### Compile Check
Runs Kotlin compiler checks to verify compilation is free of syntax errors:
```powershell
cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& C:\Users\acer\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat compileDebugKotlin"
```

### Build Debug APK
Assembles and signs a debug executable package:
```powershell
cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& C:\Users\acer\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat assembleDebug"
```
The final binary will be outputted to:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Privacy Manifest

* 🛡️ **No Advertisements**: The application contains no banner ads, interstitials, or reward configurations.
* 🛡️ **Zero Tracking**: No remote metrics, analytical trackers, or crash reporters are integrated.
* 🛡️ **Military-Grade Local Encryption**: Local files and evidence recordings are encrypted using AES-256 on-device.
* 🛡️ **No Subscriptions**: All offline, trigger, widget, and WebRTC streaming capabilities are fully unlocked and free.
