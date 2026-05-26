package com.gayan.motiongaurd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "motionguard_channel"
    private const val CHANNEL_NAME = "MotionGuard Service"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW // LOW = no sound, just shows in bar
        ).apply {
            description = "MotionGuard is actively protecting you"
            setShowBadge(false)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun buildNotification(context: Context, status: String): Notification {
        // Tapping the notification opens the app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Stop service action button in notification
        val stopIntent = Intent(context, MotionGuardService::class.java).apply {
            action = MotionGuardService.ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            context, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("MotionGuard Active")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPending)
            .setOngoing(true) // can't be swiped away
            .setSilent(true)
            .build()
    }
}