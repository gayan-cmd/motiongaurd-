<div align="center">

# 🛡️ MotionGuard

### Motion Sickness Relief for Android

**MotionGuard** is an intelligent Android application that uses your device's built-in sensors to detect vehicle turns and curves in real time, then silently activates directional edge dimming and proportional haptic feedback — reducing the disorientation that causes motion sickness while you use your phone in a moving vehicle.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-orange?style=for-the-badge)](https://developer.android.com/about/versions/oreo)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

</div>

---

## 📖 Table of Contents

- [The Problem](#-the-problem)
- [The Solution](#-the-solution)
- [How It Works](#-how-it-works)
  - [Sensor Pipeline](#sensor-pipeline)
  - [Turn Detection Logic](#turn-detection-logic)
  - [Edge Dimming Overlay](#edge-dimming-overlay)
  - [Haptic Feedback](#haptic-feedback)
- [Features](#-features)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [Core Components](#-core-components)
- [Tech Stack](#-tech-stack)
- [Permissions](#-permissions-explained)
- [Requirements](#-requirements)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Clone & Build](#clone--build)
  - [First Launch Setup](#first-launch-setup)
- [Usage](#-usage)
- [Development Roadmap](#-development-roadmap)
- [Build Configuration](#-build-configuration)
- [Contributing](#-contributing)

---

## 🤢 The Problem

Motion sickness while traveling is caused by a **sensory conflict** — your inner ear detects movement and acceleration, but your eyes are focused on a stationary screen. This mismatch between what your body feels and what your eyes see triggers nausea, dizziness, and discomfort.

The worst moments are during **turns and curves**, where lateral G-forces are strongest. Standard solutions (close your eyes, look out the window) make your phone unusable. Medication has side effects. Nothing currently bridges the gap between "staying comfortable" and "staying connected."

---

## 💡 The Solution

MotionGuard intervenes **only when needed** and stays **completely invisible** otherwise.

When a turn is detected:

- 🟠 **The edge of the screen dims** on the side of the turn — reinforcing the visual cue that aligns with what your body feels, reducing sensory conflict.
- 📳 **The phone gently vibrates** with patterns scaled to turn severity — providing a tactile anchor that grounds your spatial awareness.

When the road straightens, both effects fade out immediately. The app never interrupts what you're doing — it runs silently in the background as a foreground service, whether you're on maps, music, or messages.

---

## ⚙️ How It Works

### Sensor Pipeline

```
┌──────────────────────────┐     ┌──────────────────────────┐
│  Gyroscope (Z-axis)      │     │  Accelerometer (X-axis)   │
│  Rotation rate (rad/s)   │     │  Lateral G-force (m/s²)   │
└────────────┬─────────────┘     └────────────┬─────────────┘
             │                                │
             └──────────────┬─────────────────┘
                            │
                    ┌───────▼────────┐
                    │  Exponential   │
                    │   Smoothing    │   factor = 0.15
                    │  (noise filter)│
                    └───────┬────────┘
                            │
                    ┌───────▼────────┐
                    │ Turn Classifier │
                    │  NONE / GENTLE  │
                    │  NORMAL / SHARP │
                    └───────┬────────┘
                            │
             ┌──────────────┼──────────────┐
             │                             │
    ┌────────▼─────────┐       ┌───────────▼──────────┐
    │  HapticManager   │       │    OverlayManager     │
    │ VibrationEffect  │       │  EdgeDimmingOverlay   │
    │ (pulse patterns) │       │  (canvas gradient)   │
    └──────────────────┘       └──────────────────────┘
```

Sensors run at `SENSOR_DELAY_GAME` (~60 Hz). Both sensor streams are independently smoothed using an **exponential moving average** to suppress road vibration noise before classification.

---

### Turn Detection Logic

Turn classification uses threshold-based logic on the smoothed sensor values:

| State | Gyro Z (abs) | Accel X (abs) | Description |
|---|---|---|---|
| **STRAIGHT** | < 0.8 rad/s | < 1.5 m/s² | No turn — effects inactive |
| **GENTLE** | < 1.2 rad/s | < 2.5 m/s² | Soft curve or gentle bend |
| **NORMAL** | < 2.0 rad/s | < 4.0 m/s² | Regular road turn |
| **SHARP** | ≥ 2.0 rad/s | ≥ 4.0 m/s² | Tight turn or sharp corner |

**Direction** is determined from the sign of the lateral accelerometer value:
- `accelX > 0` → **Left turn** (left edge dims)
- `accelX < 0` → **Right turn** (right edge dims)

---

### Edge Dimming Overlay

A transparent, full-screen `View` is injected directly into the `WindowManager` with `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE` — making all touches pass through to whatever app is running underneath.

On each sensor update, a `LinearGradient` is drawn on an Android `Canvas`:

```
Left turn:                          Right turn:
┌─────────────────────┐             ┌─────────────────────┐
│▓▓▒▒▒░░░░░           │             │           ░░░░░▒▒▒▓▓│
│▓▓▒▒▒░░░░░  [App]   │             │   [App]   ░░░░░▒▒▒▓▓│
│▓▓▒▒▒░░░░░           │             │           ░░░░░▒▒▒▓▓│
└─────────────────────┘             └─────────────────────┘
  ← 35% screen width →                ← 35% screen width →
  Orange rim → Black → Transparent    Transparent → Black → Orange rim
```

The intensity is **linearly interpolated** (8% per frame) toward the target value — producing a smooth fade-in and fade-out rather than an abrupt switch.

| Turn Intensity | Overlay Opacity (max alpha 85%) |
|---|---|
| GENTLE | 30% → effective ~25.5% opacity |
| NORMAL | 60% → effective ~51% opacity |
| SHARP | 100% → effective ~85% opacity |

---

### Haptic Feedback

The `HapticManager` only triggers vibration when the intensity **level changes**, preventing constant hammering of the vibrator motor on every 60Hz sensor tick.

| Intensity | Pattern | Amplitude |
|---|---|---|
| **NONE** | Cancel any ongoing vibration | — |
| **GENTLE** | Single 80ms pulse, looping every 480ms | 120 / 255 |
| **NORMAL** | Double 100ms pulses, looping every 680ms | 180 / 255 |
| **SHARP** | Triple 120ms pulses, looping every 660ms | 255 / 255 |

Uses `VibrationEffect.createWaveform()` with looping support (API 26+). Automatically resolves `VibratorManager` on Android 12+ vs legacy `VIBRATOR_SERVICE` on older devices.

---

## ✨ Features

- **Real-time turn detection** using gyroscope + accelerometer sensor fusion
- **Directional edge dimming** — dims the correct side of the screen for the direction of the turn
- **Proportional haptic feedback** — vibration intensity scales with turn severity
- **Smooth animations** — overlay fades in and out with linear interpolation, never snaps
- **Background operation** — works as a Foreground Service while you use any other app
- **Live notification** — persistent notification updates with real-time road status (Straight / Gentle curve / Turn / Sharp turn)
- **Stop from notification** — one-tap "Stop" action button directly in the notification
- **Battery protection warnings** — detects if battery optimization could kill the background sensor
- **Overlay permission flow** — guides user through `SYSTEM_ALERT_WINDOW` permission setup
- **WakeLock** — holds a partial CPU wake lock to prevent sensor suspension in background
- **`START_STICKY`** — Android will automatically restart the service if it's killed

---

## 🏗️ Architecture

MotionGuard follows a **layered, single-module architecture** appropriate for a focused utility application:

```
┌─────────────────────────────────────────────┐
│              UI Layer (Compose)              │
│         MainActivity + ControlPanel          │
│  Permission checks · Start/Stop control      │
└──────────────────────┬──────────────────────┘
                       │ startForegroundService()
┌──────────────────────▼──────────────────────┐
│          Service Layer (Background)          │
│            MotionGuardService                │
│  SensorEventListener · Turn detection        │
│  WakeLock · Notification management          │
└───────┬──────────────────────────┬───────────┘
        │                          │
┌───────▼───────┐          ┌───────▼───────────┐
│ HapticManager │          │   OverlayManager   │
│               │          │                    │
│ Vibrator API  │          │  EdgeDimmingOverlay │
│ Waveform      │          │  WindowManager      │
│ patterns      │          │  Canvas gradient    │
└───────────────┘          └────────────────────┘
```

| Layer | Responsibility |
|---|---|
| **UI (Compose)** | Permission management, start/stop control, user-facing status |
| **Service** | Sensor lifecycle, sensor fusion, turn classification, coordinator |
| **HapticManager** | Vibrator abstraction, waveform patterns, intensity gating |
| **OverlayManager** | WindowManager lifecycle, overlay visibility |
| **EdgeDimmingOverlay** | Canvas rendering, gradient drawing, intensity lerp |
| **NotificationHelper** | Foreground notification creation, channel setup, live updates |

---

## 📁 Project Structure

```
MotionGaurd/
│
├── app/
│   ├── build.gradle.kts                        # App-level Gradle config
│   ├── proguard-rules.pro                      # ProGuard config (release builds)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml             # Permissions, activity & service declarations
│       │   ├── java/com/gayan/motiongaurd/
│       │   │   ├── MainActivity.kt             # Entry point, permission flow, Compose UI
│       │   │   ├── MotionGuardService.kt       # Core foreground service + sensor loop
│       │   │   ├── HapticManager.kt            # Vibration patterns & intensity control
│       │   │   ├── EdgeDimmingOverlay.kt       # Custom Canvas view with gradient overlay
│       │   │   ├── OverlayManager.kt           # WindowManager add/remove lifecycle
│       │   │   ├── NotificationHelper.kt       # Foreground notification builder & channel
│       │   │   └── ui/theme/
│       │   │       ├── Color.kt                # Material 3 color tokens
│       │   │       ├── Theme.kt                # App theme with light/dark support
│       │   │       └── Type.kt                 # Typography scale
│       │   └── res/
│       │       ├── drawable/                   # Launcher icons (vector)
│       │       ├── mipmap-*/                   # Adaptive launcher icons (all densities)
│       │       ├── values/
│       │       │   ├── colors.xml
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       │           ├── backup_rules.xml
│       │           └── data_extraction_rules.xml
│       ├── test/                               # Unit tests (JUnit 4)
│       └── androidTest/                        # Instrumented UI tests (Espresso)
│
├── gradle/
│   ├── libs.versions.toml                      # Centralized version catalog (all deps)
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── build.gradle.kts                            # Root-level Gradle config
├── settings.gradle.kts                         # Project settings, repository config
├── gradle.properties                           # JVM args, AndroidX, Kotlin code style
├── gradlew / gradlew.bat                       # Gradle wrapper scripts
├── PROJECT_OVERVIEW.md                         # Detailed technical reference
├── project_shedule.md                          # Development phases & build plan
└── README.md                                   # This file
```

---

## 🔩 Core Components

### `MainActivity.kt`
The app's single activity. Built entirely with Jetpack Compose.

**Responsibilities:**
- Checks and tracks two critical permissions on `onCreate` and `onResume`:
  - **Overlay permission** (`SYSTEM_ALERT_WINDOW`) — required for the edge dimming to appear over other apps
  - **Battery optimization exemption** — prevents Android from killing the background sensor service
- Displays the `ControlPanel` composable which shows:
  - Warning card if overlay permission is missing (with a direct "Grant Permission" button)
  - Warning card if battery optimization is still active (with a "Disable Optimization" button)
  - A large **Start / Stop Protection** toggle button (green when stopped, red when running)
  - A live status indicator ("✓ Running in background" / "Not active")
  - A reminder: "You can close this app — protection continues"

---

### `MotionGuardService.kt`
The heart of MotionGuard. A Foreground Service that implements `SensorEventListener`.

**Lifecycle:**
1. `onCreate()` — creates notification channel, calls `startForeground()`, acquires `PARTIAL_WAKE_LOCK`, initializes `HapticManager` and `OverlayManager`, registers sensor listeners.
2. `onSensorChanged()` — applies exponential smoothing to gyro Z and accel X, then calls `detectTurn()`.
3. `detectTurn()` — classifies direction + intensity, dispatches to both managers, updates notification text.
4. `onDestroy()` — unregisters sensors, stops haptics, hides overlay, releases wake lock.

Returns `START_STICKY` — Android will restart the service automatically if it is killed by the system.

---

### `HapticManager.kt`
Abstracts the vibrator API across Android versions.

- On **Android 12+ (API 31+)**: uses `VibratorManager.defaultVibrator`
- On **older devices**: falls back to `VIBRATOR_SERVICE`

Uses `VibrationEffect.createWaveform(timings, amplitudes, repeatIndex)` with amplitude control for precise haptic intensity. Only triggers when the intensity *class* changes — not on every 60Hz sensor tick — to avoid excessive motor wear and power draw.

---

### `EdgeDimmingOverlay.kt`
A custom `View` subclass that draws entirely with the `Canvas` API.

- Maintains an internal `intensity` float (0.0 – 1.0) that **lerps toward `targetIntensity`** at 8% per frame, producing smooth fade transitions.
- Draws a `LinearGradient` with three color stops: `Orange rim → Black shadow → Transparent`.
- Gradient covers 35% of screen width on the correct side.
- Peak opacity: 85% at SHARP intensity.
- Tracks `lastActiveDirection` to hold the correct gradient side during the fade-out phase.

---

### `OverlayManager.kt`
Manages the `WindowManager` lifecycle for `EdgeDimmingOverlay`.

```kotlin
WindowManager.LayoutParams(
    MATCH_PARENT, MATCH_PARENT,
    TYPE_APPLICATION_OVERLAY,            // API 26+ (uses TYPE_PHONE on older)
    FLAG_NOT_FOCUSABLE or
    FLAG_NOT_TOUCHABLE or
    FLAG_LAYOUT_IN_SCREEN,
    PixelFormat.TRANSLUCENT
)
```

`FLAG_NOT_TOUCHABLE` ensures all tap events pass through the overlay to the app underneath.

---

### `NotificationHelper.kt`
A singleton `object` responsible for the required foreground service notification.

- Creates a `NotificationChannel` with `IMPORTANCE_LOW` (silent, no badge, no sound)
- Builds a persistent notification (`setOngoing(true)`, `setSilent(true)`) that:
  - Shows real-time road status text (updated every sensor cycle)
  - Has a **Stop** action that sends `ACTION_STOP` intent directly to the service
  - Tapping the notification opens `MainActivity`

---

## 🔐 Permissions Explained

| Permission | Why It's Needed |
|---|---|
| `POST_NOTIFICATIONS` | Required on Android 13+ to show the persistent foreground notification |
| `VIBRATE` | Needed to trigger haptic feedback via `VibrationEffect` |
| `SYSTEM_ALERT_WINDOW` | Required to draw the edge dimming overlay on top of all other apps |
| `FOREGROUND_SERVICE` | Needed to run a foreground service |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required for a foreground service that monitors device sensors |
| `WAKE_LOCK` | Holds a partial CPU wake lock to keep sensors active when the screen is off |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Allows the app to request exclusion from battery optimization (prevents service kills) |

**Hardware features** declared as required (app will not install on devices without):
- `android.hardware.sensor.gyroscope`
- `android.hardware.sensor.accelerometer`

---

## 📋 Requirements

| Requirement | Minimum |
|---|---|
| Android OS | **8.0 Oreo (API 26)** |
| Target SDK | API 36 |
| Hardware | Gyroscope + Accelerometer (required) |
| Haptic feedback | VibrationEffect support (API 26+) |
| Overlay permission | Must be granted manually by user via Settings |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 11** (configured in Android Studio or `JAVA_HOME`)
- **Android SDK** with API 26 – 36 installed
- A physical Android device (strongly recommended for sensor testing) or an emulator with gyroscope simulation enabled

---

### Clone & Build

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/MotionGuard.git
cd MotionGuard

# 2. Build the debug APK
./gradlew assembleDebug

# 3. Install directly onto a connected device
./gradlew installDebug

# 4. Run unit tests
./gradlew test

# 5. Run instrumented UI tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

Or open the project in **Android Studio** and press **Run ▶** directly.

---

### First Launch Setup

The app requires two permissions that must be granted manually — Android does not allow these to be requested via a runtime dialog.

**Step 1 — Overlay Permission (`Draw over other apps`)**

> The app will show a warning card on the home screen if this is missing.

1. Tap **"Grant Permission"** on the warning card, or go to:
   `Settings → Apps → MotionGuard → Advanced → Draw over other apps`
2. Toggle it **On**

**Step 2 — Battery Optimization Exemption (Recommended)**

> Without this, Android may suspend the sensor service when the screen is off.

1. Tap **"Disable Optimization"** on the warning card, or go to:
   `Settings → Battery → Battery Optimization → MotionGuard → Don't optimize`

**Step 3 — Start Protection**

1. Tap the large **"Start Protection"** button
2. The notification "MotionGuard Active" will appear in your status bar
3. You can now close the app — protection continues in the background

---

## 📱 Usage

```
┌─────────────────────────────┐
│        MotionGuard          │
│  Motion sickness protection │
│                             │
│  ┌─────────────────────┐   │
│  │  ⚠ Overlay Required │   │   ← Only shown if permission missing
│  │  [Grant Permission] │   │
│  └─────────────────────┘   │
│                             │
│  ┌─────────────────────────┐│
│  │    Start Protection     ││   ← Tap to start
│  └─────────────────────────┘│
│                             │
│       Not active            │   ← Shows "✓ Running in background" when active
│                             │
│  You can close this app —   │
│  protection continues       │
└─────────────────────────────┘
```

Once running, **the notification bar shows real-time status:**

| Road Condition | Notification Text |
|---|---|
| Straight road | `Monitoring... road is straight` |
| Gentle curve | `Gentle curve detected` |
| Turn | `Turn detected — TURNING LEFT / RIGHT` |
| Sharp turn | `Sharp turn! — TURNING LEFT / RIGHT` |

**To stop:** Tap the **Stop** button in the notification, or open the app and tap **"Stop Protection"**.

---

## 🗺️ Development Roadmap

| Phase | Feature | Status |
|---|---|---|
| **Phase 1** | Sensor Foundation — live gyroscope + accelerometer reading, threshold-based turn detection | ✅ **Complete** |
| **Phase 2** | Haptic Feedback — `VibrationEffect` waveforms scaled to turn severity | ✅ **Complete** |
| **Phase 3** | Edge Dimming Overlay — full-screen `WindowManager` canvas overlay with directional gradient | ✅ **Complete** |
| **Phase 4** | Combined Effects — haptic + visual firing together, threshold calibration | ✅ **Complete** |
| **Phase 5** | Background Foreground Service — persistent operation, wake lock, live notification | ✅ **Complete** |
| **Phase 6** | Settings Screen — sensitivity slider, haptic intensity toggle, dimming level, boot-on-start | 🔲 **Planned** |
| **Phase 7** | Polish & Real-World Testing — winding road calibration, false-trigger tuning, battery profiling | 🔲 **Planned** |

### Planned Settings (Phase 6)

```
Settings Screen
├── Turn Sensitivity      [slider]   Low ──────●────── High
├── Haptic Feedback       [toggle]   ● On  /  ○ Off
├── Haptic Intensity      [select]   Low / Medium / High
├── Dimming Intensity     [slider]   Light ────●──── Dark
└── Start on Boot         [toggle]   ● On  /  ○ Off
```

---

## 🛠️ Build Configuration

### `app/build.gradle.kts`

```kotlin
android {
    namespace         = "com.gayan.motiongaurd"
    compileSdk        = 36
    minSdk            = 26
    targetSdk         = 36
    versionCode       = 1
    versionName       = "1.0"
    javaCompatibility = JavaVersion.VERSION_11
}
```

### `gradle/libs.versions.toml`

```toml
[versions]
agp            = "9.2.1"
kotlin         = "2.2.10"
composeBom     = "2026.02.01"
coreKtx        = "1.18.0"
lifecycleRtx   = "2.10.0"
activityCompose = "1.13.0"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose      = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/settings-screen`
3. **Commit** your changes: `git commit -m "feat: add sensitivity slider to settings"`
4. **Push** to the branch: `git push origin feature/settings-screen`
5. **Open** a Pull Request

### Commit Message Convention

```
feat:     New feature
fix:      Bug fix
refactor: Code restructuring without behavior change
docs:     Documentation only
test:     Test additions or changes
chore:    Build process or tooling changes
```

---

## 📄 License

```
MIT License

Copyright (c) 2026 Gayan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

---

<div align="center">

Built with ❤️ for every person who's ever felt sick scrolling through their phone on a winding road.

**[⭐ Star this repo if MotionGuard helped you!](https://github.com/YOUR_USERNAME/MotionGuard)**

</div>
