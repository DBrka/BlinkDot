package com.dejan.blinkdot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

/** Black canvas carrying one blinking dot in a corner. */
class DotView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var colors: List<Int> = listOf(Color.WHITE)
    private var sizePx = 0f
    private var marginPx = 0f
    private var position = 0
    private var onMs = 400
    private var offMs = 1600
    private var smooth = true
    private var glow = true

    private var startAt = SystemClock.elapsedRealtime()
    private var level = 0f
    private var colorIndex = 0

    private val ticker = Runnable { tick() }

    fun configure(p: Prefs) {
        val d = resources.displayMetrics.density
        sizePx = p.dotSizeDp * d
        marginPx = p.marginDp * d
        position = p.position
        onMs = p.onMs.coerceAtLeast(60)
        offMs = p.offMs.coerceAtLeast(0)
        smooth = p.smooth
        glow = p.glow
        invalidate()
    }

    fun setColors(c: List<Int>) {
        colors = if (c.isEmpty()) listOf(Color.WHITE) else c
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAt = SystemClock.elapsedRealtime()
        post(ticker)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    private fun tick() {
        val cycle = (onMs + offMs).coerceAtLeast(80)
        val elapsed = SystemClock.elapsedRealtime() - startAt
        colorIndex = ((elapsed / cycle) % colors.size.toLong()).toInt()
        val t = (elapsed % cycle).toInt()

        val next: Long
        if (smooth) {
            level = if (t < onMs) sin(PI * t / onMs).toFloat().coerceIn(0f, 1f) else 0f
            next = 16L
        } else {
            if (t < onMs) {
                level = 1f
                next = (onMs - t).toLong().coerceAtLeast(16L)
            } else {
                level = 0f
                next = (cycle - t).toLong().coerceAtLeast(16L)
            }
        }
        invalidate()
        postDelayed(ticker, next)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        if (level <= 0.01f || sizePx <= 0f) return

        val r = sizePx / 2f
        val cx = if (position == 0 || position == 2) marginPx + r else width - marginPx - r
        val cy = if (position == 0 || position == 1) marginPx + r else height - marginPx - r
        val col = colors[colorIndex.coerceIn(0, colors.size - 1)]

        if (glow) {
            paint.color = col
            paint.alpha = (level * 45f).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, r * 2.3f, paint)
            paint.color = col
            paint.alpha = (level * 80f).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, r * 1.5f, paint)
        }
        paint.color = col
        paint.alpha = (level * 255f).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, r, paint)
    }
}
