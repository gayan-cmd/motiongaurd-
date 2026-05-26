The App — What We're Building
A motion sickness relief app that uses the gyroscope and accelerometer to detect turns and curves in real time, then activates edge dimming and haptic feedback only when needed — staying completely invisible on straight roads.

Project Phases
Phase 1 — Sensor Foundation
The core of the whole app. Get the gyroscope and accelerometer reading live data, write the turn detection logic (threshold-based — when lateral G-force or rotation rate crosses a value, a turn is detected), and test it by just printing sensor values to the screen while sitting in a moving car. Nothing visual yet, just proving the detection works.

Phase 2 — Haptic Feedback
Wire the turn detection into Android's VibrationEffect API. When a turn is detected, pulse the haptic motor with an intensity proportional to the severity of the turn — gentle curve gets a soft pulse, sharp turn gets a stronger one. Test this in isolation before combining with visuals.

Phase 3 — Edge Dimming Overlay
Create a transparent overlay that sits on top of the screen. When a turn is detected, darken the edge on the turn side — left turn dims the left edge, right turn dims the right edge. The dimming fades in smoothly when the turn starts and fades out when the road straightens. This is a Canvas-drawn overlay using WindowManager.

Phase 4 — Combining Both
Combine Phase 2 and 3 so they fire together. Tune the thresholds so the combo feels natural and not intrusive. This phase is mostly about calibration through real-world testing in a vehicle.

Phase 5 — Background Service
Right now the app only works when it's open on screen. This phase converts it into a Foreground Service so it runs in the background while you use other apps — maps, music, anything. This is what makes it actually useful in real life. A persistent notification will show "Motion Guard is active" (Android requires this for foreground services).

Phase 6 — Settings Screen
A simple UI with controls for:

Sensitivity slider (how sharp a turn triggers the effect)
Haptic intensity toggle (on/off or low/medium/high)
Edge dimming intensity slider
Start on boot toggle

Phase 7 — Polish & Real World Testing
Ride testing on winding roads specifically. Fine-tune the detection threshold so it doesn't false-trigger on lane changes but catches proper curves. Smooth out the fade animations. Fix any battery drain issues from the sensor running continuously.

Tech Stack

Language — Kotlin
Sensors — SensorManager with TYPE_GYROSCOPE and TYPE_LINEAR_ACCELERATION
Haptics — VibrationEffect API (API 26+)
Overlay — WindowManager + custom Canvas View
Background — Android Foreground Service
UI — Jetpack Compose for the settings screen
Min SDK — API 26 (Android 8.0)


Build Order Logic
The phases are sequenced deliberately — each one builds on the last and is independently testable. You never combine two unproven things. Phase 1 is the most critical because if turn detection isn't accurate, nothing else works well. Phases 2 and 3 can technically be developed in parallel once Phase 1 is solid, but doing them sequentially is safer for a solo build.

