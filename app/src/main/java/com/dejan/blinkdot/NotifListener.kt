package com.dejan.blinkdot

import android.app.KeyguardManager
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import android.service.notification.StatusBarNotification

/**
 * Watches every notification the system posts. WhatsApp, Viber, Messenger and
 * SMS all arrive here, so no per-app integration is needed.
 */
class NotifListener : NotificationListenerService() {

    override fun onListenerConnected() {
        connected = true
        Log.d(TAG, "listener connected")
    }

    override fun onListenerDisconnected() {
        connected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return

        val prefs = Prefs(this)
        if (!prefs.enabled) return
        if (!prefs.isAppEnabled(pkg)) return

        // Skip persistent clutter: media controls, sync bars, group headers.
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
            Log.d(TAG, "skip " + pkg + ": ongoing")
            return
        }
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            Log.d(TAG, "skip " + pkg + ": group summary")
            return
        }
        if (!sbn.isClearable) {
            Log.d(TAG, "skip " + pkg + ": not clearable")
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
        if (!stillWaiting) BlinkState.remove(pkg)
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
