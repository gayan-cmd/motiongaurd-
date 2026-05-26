package com.gayan.motiongaurd

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.graphics.Color


class EdgeDimmingOverlay(context: Context) : View(context) {

    // Direction: -1 = left, 0 = none, 1 = right
    private var turnDirection = 0
    private var lastActiveDirection = 0
    private var intensity = 0f  // 0.0 to 1.0
    private var targetIntensity = 0f
    private val paint = Paint()

    // How wide the dimming gradient is
    // 0.35 = covers 35% of screen width on each side
    private val edgeWidthFraction = 0.35f

    // How dark the edge gets at maximum intensity
    // 0.85f = 85% opacity black at the edge
    private val maxAlpha = 0.85f

    fun updateTurn(direction: String, turnIntensity: String) {
        turnDirection = when (direction) {
            "TURNING LEFT"  -> -1
            "TURNING RIGHT" ->  1
            else            ->  0
        }
        
        if (turnDirection != 0) {
            lastActiveDirection = turnDirection
        }

        targetIntensity = when (turnIntensity) {
            "GENTLE" -> 0.3f
            "NORMAL" -> 0.6f
            "SHARP"  -> 1.0f
            else     -> 0f
        }

        // Smooth fade using simple lerp (linear interpolation)
        // This runs every sensor update (~60 times/sec)
        val lerpSpeed = 0.08f
        intensity += (targetIntensity - intensity) * lerpSpeed

        invalidate() // triggers redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (intensity < 0.01f) return // nothing to draw

        val w = width.toFloat()
        val h = height.toFloat()
        val edgeWidth = w * edgeWidthFraction
        val alpha = (intensity * maxAlpha * 255).toInt().coerceIn(0, 255)

        val drawDirection = if (turnDirection != 0) turnDirection else lastActiveDirection

        when (drawDirection) {

            // Left turn — dim left edge
            -1 -> {
                val rimColor = Color.argb(alpha, 255, 165, 0)
                val shadowColor = Color.argb(alpha, 0, 0, 0)
                val transparent = Color.TRANSPARENT

                val gradient = LinearGradient(
                    0f, 0f, edgeWidth, 0f,
                    intArrayOf(rimColor, shadowColor, transparent),
                    floatArrayOf(0f, 0.1f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, edgeWidth, h, paint)
            }

            // Right turn — dim right edge
            1 -> {
                val rimColor = Color.argb(alpha, 255, 165, 0)
                val shadowColor = Color.argb(alpha, 0, 0, 0)
                val transparent = Color.TRANSPARENT

                val gradient = LinearGradient(
                    w, 0f, w - edgeWidth, 0f,
                    intArrayOf(rimColor, shadowColor, transparent),
                    floatArrayOf(0f, 0.1f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(w - edgeWidth, 0f, w, h, paint)
            }
        }
    }
}