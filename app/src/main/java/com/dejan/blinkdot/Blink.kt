package com.dejan.blinkdot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Starting an activity from the background is restricted. Two routes are
 * allowed: holding the overlay permission, or a full-screen-intent
 * notification. Try the direct route first and fall back to the other.
 */
object Blink {

    private const val TAG = "BlinkDot"
    private const val CHANNEL = "blink_trigger"
    private const val NOTIF_ID = 9911

    fun show(ctx: Context) {
        val overlay = Settings.canDrawOverlays(ctx)
        Log.d(TAG, "show() canDrawOverlays=" + overlay)
        if (overlay) {
            try {
                ctx.startActivity(BlinkActivity.intent(ctx))
                Log.d(TAG, "startActivity issued")
                return
            } catch (t: Throwable) {
                Log.d(TAG, "startActivity failed: " + t)
            }
        }
        Log.d(TAG, "falling back to full-screen intent")
        fullScreenIntent(ctx)
    }

    fun preview(ctx: Context, color: Int) {
        try {
            ctx.startActivity(BlinkActivity.previewIntent(ctx, color))
        } catch (ignored: Throwable) {
        }
    }

    private fun fullScreenIntent(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return

        val channel = NotificationChannel(
            CHANNEL,
            ctx.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        nm.createNotificationChannel(channel)

        val pi = PendingIntent.getActivity(
            ctx,
            1,
            BlinkActivity.intent(ctx),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_dot)
            .setContentTitle(ctx.getString(R.string.app_name))
            .setContentText(ctx.getString(R.string.tagline))
            .setFullScreenIntent(pi, true)
            .setAutoCancel(true)
            .setTimeoutAfter(5_000L)
            .build()

        nm.notify(NOTIF_ID, n)
    }
}
