package com.dejan.blinkdot

import android.app.KeyguardManager
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Watches every notification the system posts. WhatsApp, Viber, Messenger and
 * SMS all arrive here, so no per-app integration is needed.
 */
class NotifListener : NotificationListenerService() {

    private var receiverRegistered = false

    /**
     * Brings the dot back when the phone locks again with something still
     * unread. Only acts when the blink screen is not already up — while it is,
     * a screen-off means the user dismissed it.
     */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (BlinkState.showing) return
                    val prefs = Prefs(this@NotifListener)
                    if (!prefs.enabled || !prefs.blinkUntilRead) return
                    if (!BlinkState.armed || BlinkState.isEmpty()) return
                    Log.d(TAG, "screen off with unread messages, blinking again")
                    Blink.show(this@NotifListener)
                }

                // Re-arm on both, because ACTION_USER_PRESENT never fires on a
                // phone with no lock screen set. Waking the screen and putting
                // it away again without reading counts as still unread.
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> BlinkState.rearmIfPending()
            }
        }
    }

    override fun onListenerConnected() {
        connected = true
        Log.d(TAG, "listener connected")
        rebuildPending()
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                screenReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_USER_PRESENT)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    override fun onListenerDisconnected() {
        connected = false
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(screenReceiver)
            } catch (ignored: IllegalArgumentException) {
            }
            receiverRegistered = false
        }
        super.onDestroy()
    }

    /** Rebuild the unread set after a restart, so a reboot does not lose it. */
    private fun rebuildPending() {
        val prefs = Prefs(this)
        if (!prefs.enabled) return
        try {
            BlinkState.clear()
            activeNotifications?.forEach { sbn ->
                val pkg = sbn.packageName ?: return@forEach
                if (pkg == packageName || !prefs.isAppEnabled(pkg)) return@forEach
                if (!worthBlinking(sbn)) return@forEach
                BlinkState.add(pkg, prefs.colorFor(pkg))
            }
        } catch (ignored: Throwable) {
        }
    }

    /** Skip persistent clutter: media controls, sync bars, group headers. */
    private fun worthBlinking(sbn: StatusBarNotification): Boolean {
        val flags = sbn.notification?.flags ?: return false
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        return sbn.isClearable
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.notification == null) return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return

        val prefs = Prefs(this)
        if (!prefs.enabled) return
        if (!prefs.isAppEnabled(pkg)) return
        if (!worthBlinking(sbn)) {
            Log.d(TAG, "skip " + pkg + ": ongoing, summary or not clearable")
            return
        }

        BlinkState.add(pkg, prefs.colorFor(pkg))
        val show = screenOffOrLocked()
        Log.d(TAG, "posted " + pkg + " screenOffOrLocked=" + show)
        if (show) Blink.show(this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        val stillWaiting = try {
            activeNotifications?.any { it.packageName == pkg && it.isClearable } ?: false
        } catch (ignored: Throwable) {
            false
        }
        if (!stillWaiting) {
            Log.d(TAG, "cleared " + pkg)
            BlinkState.remove(pkg)
        }
    }

    private fun screenOffOrLocked(): Boolean {
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        val interactive = power?.isInteractive ?: true
        val locked = keyguard?.isKeyguardLocked ?: false
        return !interactive || locked
    }

    companion object {

        private const val TAG = "BlinkDot"

        @Volatile
        var connected = false

        /** Whether the user has granted notification access in system settings. */
        fun hasAccess(ctx: Context): Boolean {
            val flat = Settings.Secure.getString(
                ctx.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return flat.split(":").any { entry ->
                ComponentName.unflattenFromString(entry)?.packageName == ctx.packageName
            }
        }
    }
}
