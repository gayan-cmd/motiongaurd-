package com.gayan.motiongaurd

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.app.NotificationManager
import android.os.IBinder

class MotionGuardService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var hapticManager: HapticManager
    private lateinit var overlayManager: OverlayManager

    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null

    // Smoothed values
    private var smoothedGyroZ = 0f
    private var smoothedAccelX = 0f
    private val smoothingFactor = 0.15f

    // Current state
    private var turnDirection = "STRAIGHT"
    private var turnIntensity = "NONE"

    companion object {
        const val ACTION_STOP = "ACTION_STOP"
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, MotionGuardService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MotionGuardService::class.java)
            context.stopService(intent)
        }
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        // Set up notification
        NotificationHelper.createChannel(this)
        
        val notification = NotificationHelper.buildNotification(this, "Monitoring for turns...")
        if (android.os.Build.VERSION.SDK_INT >= 34) { // Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            startForeground(
                NotificationHelper.NOTIFICATION_ID, 
                notification, 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }

        // Acquire partial wakelock to prevent CPU from sleeping
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MotionGuard::SensorWakeLock")
        wakeLock?.acquire()

        // Initialize managers
        hapticManager = HapticManager(this)
        overlayManager = OverlayManager(this)

        // Set up sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        // Start listening
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Show overlay
        if (overlayManager.hasPermission()) {
            overlayManager.showOverlay()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle stop action from notification button
        if (intent?.action == ACTION_STOP) {
            isRunning = false
            @Suppress("DEPRECATION")
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        // START_STICKY = if Android kills the service, restart it automatically
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRunning) return

        when (event.sensor.type) {

            Sensor.TYPE_GYROSCOPE -> {
                smoothedGyroZ = smoothedGyroZ + smoothingFactor *
                        (event.values[2] - smoothedGyroZ)
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                smoothedAccelX = smoothedAccelX + smoothingFactor *
                        (event.values[0] - smoothedAccelX)
            }
        }

        detectTurn()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun detectTurn() {
        val gyroAbs = Math.abs(smoothedGyroZ)
        val accelAbs = Math.abs(smoothedAccelX)

        val isTurning = gyroAbs > 0.8f || accelAbs > 1.5f

        turnDirection = when {
            !isTurning -> "STRAIGHT"
            smoothedAccelX > 0 -> "TURNING LEFT"
            smoothedAccelX < 0 -> "TURNING RIGHT"
            else -> "TURNING"
        }

        turnIntensity = when {
            gyroAbs < 0.8f && accelAbs < 1.5f -> "NONE"
            gyroAbs < 1.2f && accelAbs < 2.5f -> "GENTLE"
            gyroAbs < 2.0f && accelAbs < 4.0f -> "NORMAL"
            else -> "SHARP"
        }

        // Update both effects
        hapticManager.update(smoothedGyroZ, smoothedAccelX)
        overlayManager.updateOverlay(turnDirection, turnIntensity)

        // Update notification text with current status
        updateNotification()
    }

    private fun updateNotification() {
        if (!isRunning) return
        
        val status = when (turnIntensity) {
            "NONE"   -> "Monitoring... road is straight"
            "GENTLE" -> "Gentle curve detected"
            "NORMAL" -> "Turn detected — $turnDirection"
            "SHARP"  -> "Sharp turn! — $turnDirection"
            else     -> "Monitoring..."
        }

        val notification = NotificationHelper.buildNotification(this, status)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        
        @Suppress("DEPRECATION")
        stopForeground(true)
        
        sensorManager.unregisterListener(this)
        hapticManager.stop()
        overlayManager.hideOverlay()
        
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}