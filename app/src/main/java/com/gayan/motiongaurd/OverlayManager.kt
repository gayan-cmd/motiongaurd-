package com.gayan.motiongaurd

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager

class OverlayManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: EdgeDimmingOverlay? = null
    private var isShowing = false

    fun hasPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun showOverlay() {
        if (isShowing || !hasPermission()) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE)
                as WindowManager

        overlayView = EdgeDimmingOverlay(context)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Required for Android 12+: Window alpha must be <= 0.8 for touches to pass through
            alpha = 0.8f
        }

        windowManager?.addView(overlayView, params)
        isShowing = true
    }

    fun updateOverlay(direction: String, intensity: String) {
        overlayView?.updateTurn(direction, intensity)
    }

    fun hideOverlay() {
        if (!isShowing) return
        windowManager?.removeView(overlayView)
        overlayView = null
        isShowing = false
    }

    fun isShowing() = isShowing
}