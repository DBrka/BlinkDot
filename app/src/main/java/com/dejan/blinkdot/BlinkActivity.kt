package com.dejan.blinkdot

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.content.ContextCompat

/**
 * The black lock-screen surface that carries the blinking dot. It shows over
 * the keyguard and turns the screen on itself, because nothing can paint on a
 * screen that is genuinely off.
 */
class BlinkActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var dot: DotView
    private val handler = Handler(Looper.getMainLooper())
    private var preview = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF, Intent.ACTION_USER_PRESENT -> dismiss()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        preview = intent?.getBooleanExtra(EXTRA_PREVIEW, false) == true
        if (!preview) BlinkState.showing = true

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val lp = window.attributes
        lp.screenBrightness = prefs.brightnessPct.coerceIn(1, 100) / 100f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.attributes = lp

        dot = DotView(this)
        dot.setBackgroundColor(Color.BLACK)
        setContentView(dot)

        // Must come after setContentView: the insets controller needs a decor view.
        hideSystemBars()

        dot.configure(prefs)
        refreshColors()

        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        BlinkState.setListener { handler.post { refreshColors() } }

        if (preview) {
            handler.postDelayed({ dismiss() }, PREVIEW_MS)
        } else if (!prefs.blinkUntilRead) {
            val minutes = prefs.timeoutMin
            if (minutes > 0) handler.postDelayed({ dismiss() }, minutes * 60_000L)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        preview = intent?.getBooleanExtra(EXTRA_PREVIEW, false) == true
        dot.configure(prefs)
        refreshColors()
    }

    private fun refreshColors() {
        if (preview) {
            dot.setColors(listOf(intent?.getIntExtra(EXTRA_COLOR, Color.WHITE) ?: Color.WHITE))
            return
        }
        val colors = BlinkState.colors()
        if (colors.isEmpty()) {
            dismiss()
            return
        }
        dot.setColors(colors)
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                // Transient behaviour re-hides the bars after the system briefly
                // shows them (e.g. a heads-up notification), keeping the screen
                // black again on its own.
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    /** Any touch hands the phone back to the normal lock screen. */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            dismiss()
            return true
        }
        return super.onTouchEvent(event)
    }

    /**
     * The user is putting the dot away. Disarm so screen-off does not bring it
     * straight back; unlocking without reading re-arms it.
     */
    private fun dismiss() {
        if (!preview) BlinkState.disarm()
        handler.removeCallbacksAndMessages(null)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        if (!preview) BlinkState.showing = false
        BlinkState.setListener(null)
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(receiver)
        } catch (ignored: IllegalArgumentException) {
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_PREVIEW = "preview"
        private const val EXTRA_COLOR = "color"
        private const val PREVIEW_MS = 12_000L

        fun intent(ctx: Context): Intent =
            Intent(ctx, BlinkActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }

        fun previewIntent(ctx: Context, color: Int): Intent =
            intent(ctx).putExtra(EXTRA_PREVIEW, true).putExtra(EXTRA_COLOR, color)
    }
}
