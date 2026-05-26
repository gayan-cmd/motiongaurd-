package com.gayan.motiongaurd

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticManager(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Turn intensity levels
    enum class TurnIntensity {
        NONE,
        GENTLE,   // soft curve
        NORMAL,   // regular turn
        SHARP     // sharp turn
    }

    private var currentIntensity = TurnIntensity.NONE

    fun update(gyroZ: Float, accelX: Float) {
        val intensity = classifyTurn(gyroZ, accelX)

        // Only trigger haptic if intensity changed
        // avoids hammering the vibrator every sensor update
        if (intensity != currentIntensity) {
            currentIntensity = intensity
            triggerHaptic(intensity)
        }
    }

    private fun classifyTurn(gyroZ: Float, accelX: Float): TurnIntensity {
        val gyroAbs = Math.abs(gyroZ)
        val accelAbs = Math.abs(accelX)

        return when {
            gyroAbs < 0.8f && accelAbs < 1.5f -> TurnIntensity.NONE
            gyroAbs < 1.2f && accelAbs < 2.5f -> TurnIntensity.GENTLE
            gyroAbs < 2.0f && accelAbs < 4.0f -> TurnIntensity.NORMAL
            else                               -> TurnIntensity.SHARP
        }
    }

    private fun triggerHaptic(intensity: TurnIntensity) {
        when (intensity) {

            TurnIntensity.NONE -> {
                // Stop any ongoing vibration
                vibrator.cancel()
            }

            TurnIntensity.GENTLE -> {
                // Repeating soft pulses
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 80, 400) // initial delay, vibrate, pause
                    val amplitudes = intArrayOf(0, 120, 0)
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(pattern, amplitudes, 1) // repeat from index 1 (looping)
                    )
                }
            }

            TurnIntensity.NORMAL -> {
                // Repeating double medium pulses
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 100, 80, 100, 400)
                    // delay, vibrate, pause, vibrate, long pause before repeat
                    val amplitudes = intArrayOf(0, 180, 0, 180, 0)
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(pattern, amplitudes, 1)
                        // 1 = repeat from index 1
                    )
                }
            }

            TurnIntensity.SHARP -> {
                // Repeating three strong pulses — clear warning
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 120, 60, 120, 60, 120, 300)
                    // delay, vibe, pause, vibe, pause, vibe, long pause before repeat
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0)
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(pattern, amplitudes, 1)
                        // 1 = repeat from index 1
                    )
                }
            }
        }
    }

    fun stop() {
        vibrator.cancel()
        currentIntensity = TurnIntensity.NONE
    }
}