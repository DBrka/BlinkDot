package com.dejan.blinkdot

import kotlin.math.abs

object Palette {

    /** Colours offered in the picker. */
    val COLORS = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFF5B8CFF.toInt(), 0xFF3DDC84.toInt(), 0xFFFF3B30.toInt(),
        0xFFFFB020.toInt(), 0xFFB05BFF.toInt(), 0xFF00E5FF.toInt(), 0xFFFF2D95.toInt(),
        0xFF25D366.toInt(), 0xFF7360F2.toInt(), 0xFF0084FF.toInt(), 0xFF2AABEE.toInt(),
        0xFFE1306C.toInt(), 0xFFFF7A00.toInt(), 0xFFC0FF00.toInt(), 0xFF00FFA3.toInt(),
        0xFFFF5E5E.toInt(), 0xFF8AA0FF.toInt(), 0xFFFFD400.toInt(), 0xFF6EE7FF.toInt(),
        0xFFB9FF66.toInt(), 0xFFFF9DE0.toInt(), 0xFF9BA3AF.toInt(), 0xFF4B5563.toInt()
    )

    /** Brand-ish defaults so a freshly enabled app already looks right. */
    private val KNOWN = mapOf(
        "com.whatsapp" to 0xFF25D366.toInt(),
        "com.whatsapp.w4b" to 0xFF25D366.toInt(),
        "com.viber.voip" to 0xFF7360F2.toInt(),
        "com.facebook.orca" to 0xFF0084FF.toInt(),
        "com.facebook.katana" to 0xFF1877F2.toInt(),
        "org.telegram.messenger" to 0xFF2AABEE.toInt(),
        "org.thunderdog.challegram" to 0xFF2AABEE.toInt(),
        "com.instagram.android" to 0xFFE1306C.toInt(),
        "com.snapchat.android" to 0xFFFFD400.toInt(),
        "com.discord" to 0xFFB05BFF.toInt(),
        "com.slack" to 0xFF611F69.toInt(),
        "com.microsoft.teams" to 0xFF6264A7.toInt(),
        "com.skype.raider" to 0xFF00AFF0.toInt(),
        "org.signal" to 0xFF3A76F0.toInt(),
        "org.thoughtcrime.securesms" to 0xFF3A76F0.toInt(),
        "com.samsung.android.messaging" to 0xFF00A3FF.toInt(),
        "com.google.android.apps.messaging" to 0xFF1A73E8.toInt(),
        "com.samsung.android.email.provider" to 0xFFFF7A00.toInt(),
        "com.google.android.gm" to 0xFFEA4335.toInt(),
        "com.microsoft.office.outlook" to 0xFF0078D4.toInt(),
        "com.android.server.telecom" to 0xFF3DDC84.toInt(),
        "com.samsung.android.dialer" to 0xFF3DDC84.toInt(),
        "com.samsung.android.incallui" to 0xFF3DDC84.toInt()
    )

    fun suggestFor(pkg: String): Int =
        KNOWN[pkg] ?: COLORS[abs(pkg.hashCode()) % COLORS.size]
}
