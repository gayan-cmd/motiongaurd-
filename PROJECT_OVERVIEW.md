# MotionGuard — Project Overview

> A motion sickness relief app for Android that uses device sensors to detect turns in real time and activates edge dimming and haptic feedback to reduce disorientation while traveling.

---

## Table of Contents

- [What It Does](#what-it-does)
- [How It Works](#how-it-works)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Core Components](#core-components)
- [Android Permissions](#android-permissions)
- [Build Configuration](#build-configuration)
- [Development Roadmap](#development-roadmap)
- [Current Status](#current-status)

---

## What It Does

MotionGuard is an Android application designed to reduce motion sickness for people who use their phone while riding in a vehicle. The core idea is simple:

- **Detects turns and curves** in real time using the phone's gyroscope and accelerometer.
- **Activates edge dimming** — a transparent overlay that darkens the edge of the screen on the side of the turn (left turn → left edge dims, right turn → right edge dims).
- **Triggers haptic feedback** — vibration pulses proportional to the severity of the turn (gentle / normal / sharp).
- **Runs in the background** as a persistent Foreground Service, so it stays active while you use maps, music, or any other app.
- **Stays completely invisible on straight roads** — no visual interference when it's not needed.

---

## How It Works

### Turn Detection Pipeline

```
Gyroscope (Z-axis rotation)  ──┐
                                ├──► Exponential Smoothing ──► Turn Classifier ──► Effects
Accelerometer (X-axis linear) ─┘
```

1. **Raw sensor data** comes in at `SENSOR_DELAY_GAME` rate (~60Hz).
2. **Exponential smoothing** (`factor = 0.15`) filters out jitter and noise.
3. **Turn classifier** applies threshold logic:

| Gyro (abs) | Accel (abs) | Intensity |
|---|---|---|
| < 0.8 rad/s | < 1.5 m/s² | NONE (straight) |
| < 1.2 rad/s | < 2.5 m/s² | GENTLE |
| < 2.0 rad/s | < 4.0 m/s² | NORMAL |
| ≥ 2.0 rad/s | ≥ 4.0 m/s² | SHARP |

4. **Turn direction** is determined from the sign of the lateral accelerometer value (positive X → left, negative X → right).
5. Both **haptic** and **edge dimming** effects are updated on every sensor cycle.

### Edge Dimming

The overlay uses a `LinearGradient` drawn on a full-screen transparent `Canvas` view injected via `WindowManager`. The gradient fades from an **orange rim** → black shadow → transparent, covering 35% of the screen width on the turn side. The intensity lerps smoothly (8% per frame) toward the target so it fades in and out gracefully rather than snapping.

| Turn Intensity | Overlay Alpha |
|---|---|
| GENTLE | 30% |
| NORMAL | 60% |
| SHARP | 85% (max) |

### Haptic Patterns

Haptic is only triggered when intensity **changes** (not every sensor update) to avoid hammering the vibrator.

| Intensity | Pattern |
|---|---|
| NONE | Cancel vibration |
| GENTLE | Single soft pulse (80ms @ amplitude 120), looping |
| NORMAL | Double medium pulses (100ms + 100ms @ amplitude 180), looping |
| SHARP | Triple strong pulses (120ms × 3 @ amplitude 255), looping |

---

## Tech Stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose (Material 3) |
| Build System | Gradle (Kotlin DSL) |
| Dependency Management | Version Catalog (`libs.versions.toml`) |
| Sensors | `SensorManager` — `TYPE_GYROSCOPE`, `TYPE_LINEAR_ACCELERATION` |
| Haptics | `VibrationEffect` API (Android 8.0 / API 26+) |
| Overlay | `WindowManager` + custom `Canvas` `View` |
| Background | Android Foreground Service |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 36 |
| AGP Version | 9.2.1 |
| Kotlin Version | 2.2.10 |
| Compose BOM | 2026.02.01 |

---

## Project Structure

```
MotionGaurd/
├── app/
│   ├── build.gradle.kts              # App-level Gradle config
│   └── src/main/
│       ├── AndroidManifest.xml       # Permissions, activity & service declarations
│       └── java/com/gayan/motiongaurd/
│           ├── MainActivity.kt       # Entry point, permission checks, Compose UI
│           ├── MotionGuardService.kt # Foreground service — sensor loop, detection logic
│           ├── HapticManager.kt      # Vibration patterns and intensity control
│           ├── EdgeDimmingOverlay.kt # Canvas-drawn overlay view
│           ├── OverlayManager.kt     # WindowManager lifecycle (add/remove overlay)
│           ├── NotificationHelper.kt # Foreground notification builder
│           └── ui/theme/             # Compose theme (colors, typography, shapes)
├── gradle/
│   └── libs.versions.toml            # Centralized dependency version catalog
├── build.gradle.kts                  # Root-level Gradle config
├── settings.gradle.kts               # Project settings
├── project_shedule.md                # Development phases and build plan
└── PROJECT_OVERVIEW.md               # This file
```

---

## Core Components

### `MainActivity.kt`
The entry point of the app. Manages:
- **Permission checks** on launch and resume: overlay (`SYSTEM_ALERT_WINDOW`) and battery optimization exemption.
- **Service control**: starts/stops `MotionGuardService` via the Compose `ControlPanel` UI.
- **UI state sync**: keeps the start/stop button in sync with the running state of the background service.

The `ControlPanel` composable shows:
- A warning card if overlay permission is missing.
- A warning card if battery optimization is still enabled (which can kill the background service).
- A large **Start / Stop Protection** toggle button.
- A status indicator ("✓ Running in background" vs "Not active").

---

### `MotionGuardService.kt`
The heart of the app — an Android Foreground Service that:
- Registers as a `SensorEventListener` for `TYPE_GYROSCOPE` and `TYPE_LINEAR_ACCELERATION`.
- Applies exponential smoothing to raw sensor values.
- Runs `detectTurn()` on every sensor update to classify direction and intensity.
- Dispatches updates to `HapticManager` and `OverlayManager`.
- Holds a `PARTIAL_WAKE_LOCK` to prevent the CPU from sleeping while monitoring.
- Updates the persistent notification with the current road status in real time.
- Uses `START_STICKY` so Android restarts it if killed.

---

### `HapticManager.kt`
Manages the vibrator hardware. Resolves the correct vibrator API (uses `VibratorManager` on Android 12+ and the deprecated `VIBRATOR_SERVICE` on older devices). Triggers looping `VibrationEffect.createWaveform` patterns scaled to turn severity, only firing when the intensity level *changes*.

---

### `EdgeDimmingOverlay.kt`
A custom `View` drawn entirely with Android Canvas. On each sensor update:
- Recomputes target overlay intensity from turn severity.
- Lerps the current intensity toward the target (smooth fade-in/out).
- Draws a `LinearGradient` on the left or right edge (orange rim → black shadow → transparent).
- Calls `invalidate()` to trigger a redraw.

The overlay covers 35% of screen width and peaks at 85% opacity on a SHARP turn.

---

### `OverlayManager.kt`
Manages the `WindowManager` lifecycle for the `EdgeDimmingOverlay`. Handles:
- Checking the `SYSTEM_ALERT_WINDOW` permission.
- Adding the full-screen translucent overlay view with `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE` so all touches pass through to the underlying app.
- Removing the view cleanly when the service stops.

---

### `NotificationHelper.kt`
A singleton object that builds the required foreground service notification:
- Creates the notification channel (`IMPORTANCE_LOW` — silent, no badge).
- Builds a persistent notification with a live status text (updated every turn detection cycle).
- Includes a **Stop** action button that sends `ACTION_STOP` to the service.
- Tapping the notification opens `MainActivity`.

---

## Android Permissions

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Required on Android 13+ to show the foreground notification |
| `VIBRATE` | Required for haptic feedback |
| `SYSTEM_ALERT_WINDOW` | Required to draw the edge dimming overlay over other apps |
| `FOREGROUND_SERVICE` | Required to start a foreground service |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required for sensor-monitoring foreground services |
| `WAKE_LOCK` | Keeps the CPU awake while sensors are active |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Requests the user to exempt the app from battery optimization |

Hardware features declared as **required**:
- `android.hardware.sensor.gyroscope`
- `android.hardware.sensor.accelerometer`

---

## Build Configuration

```kotlin
// app/build.gradle.kts
namespace         = "com.gayan.motiongaurd"
applicationId     = "com.gayan.motiongaurd"
compileSdk        = 36
minSdk            = 26
targetSdk         = 36
versionCode       = 1
versionName       = "1.0"
javaCompatibility = JavaVersion.VERSION_11
```

**Key dependencies** (via Compose BOM `2026.02.01`):
- `androidx.compose.material3`
- `androidx.compose.ui`
- `androidx.activity:activity-compose`
- `androidx.lifecycle:lifecycle-runtime-ktx`
- `androidx.core:core-ktx`

---

## Development Roadmap

The project is broken into 7 phases (from `project_shedule.md`):

| Phase | Description | Status |
|---|---|---|
| **1** | Sensor Foundation — gyroscope + accelerometer reading + turn detection | ✅ Complete |
| **2** | Haptic Feedback — `VibrationEffect` API wired to turn intensity | ✅ Complete |
| **3** | Edge Dimming Overlay — `WindowManager` canvas overlay, directional gradient | ✅ Complete |
| **4** | Combining Both — haptic + visual fire together, threshold calibration | ✅ Complete |
| **5** | Background Service — Foreground Service so app works while closed | ✅ Complete |
| **6** | Settings Screen — sensitivity slider, haptic toggle, dimming intensity, boot start | 🔲 Planned |
| **7** | Polish & Real World Testing — road testing, false trigger tuning, battery drain fixes | 🔲 Planned |

---

## Current Status

**Phases 1–5 are fully implemented.** The app can:
- Detect turns and curves in a moving vehicle using fused gyroscope + accelerometer data.
- Apply proportional haptic patterns (GENTLE / NORMAL / SHARP).
- Draw a smooth directional edge dimming overlay on top of any app.
- Run entirely in the background as a persistent foreground service.
- Survive app closure and update its notification with live road status.

**Remaining work** focuses on the user-facing settings screen (Phase 6) and real-world calibration testing on winding roads (Phase 7).
