package com.dejan.blinkdot

import android.content.Context

class Prefs(ctx: Context) {

    private val sp = ctx.applicationContext
        .getSharedPreferences("blinkdot", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    /** 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right */
    var position: Int
        get() = sp.getInt("position", 0)
        set(v) = sp.edit().putInt("position", v).apply()

    var dotSizeDp: Int
        get() = sp.getInt("dot_size", 14)
        set(v) = sp.edit().putInt("dot_size", v).apply()

    var marginDp: Int
        get() = sp.getInt("margin", 26)
        set(v) = sp.edit().putInt("margin", v).apply()

    var onMs: Int
        get() = sp.getInt("on_ms", 400)
        set(v) = sp.edit().putInt("on_ms", v).apply()

    var offMs: Int
        get() = sp.getInt("off_ms", 1600)
        set(v) = sp.edit().putInt("off_ms", v).apply()

    var smooth: Boolean
        get() = sp.getBoolean("smooth", true)
        set(v) = sp.edit().putBoolean("smooth", v).apply()

    var glow: Boolean
        get() = sp.getBoolean("glow", true)
        set(v) = sp.edit().putBoolean("glow", v).apply()

    /** Screen brightness while the dot is showing, 1..100 percent. */
    var brightnessPct: Int
        get() = sp.getInt("brightness", 4)
        set(v) = sp.edit().putInt("brightness", v).apply()

    /** Minutes before the dot gives up. 0 means keep going until unlocked. */
    var timeoutMin: Int
        get() = sp.getInt("timeout_min", 10)
        set(v) = sp.edit().putInt("timeout_min", v).apply()

    /**
     * Keep blinking until the message is actually opened, rather than giving
     * up after a timeout. The dot comes back every time the phone locks again
     * while something is still unread.
     */
    var blinkUntilRead: Boolean
        get() = sp.getBoolean("until_read", true)
        set(v) = sp.edit().putBoolean("until_read", v).apply()

    private fun appSet(): Set<String> = sp.getStringSet("apps", emptySet()) ?: emptySet()

    fun enabledApps(): Set<String> = appSet()

    fun isAppEnabled(pkg: String): Boolean = appSet().contains(pkg)

    fun setAppEnabled(pkg: String, on: Boolean) {
        val s = HashSet(appSet())
        if (on) s.add(pkg) else s.remove(pkg)
        sp.edit().putStringSet("apps", s).apply()
        if (on && !sp.contains("color_" + pkg)) setColor(pkg, Palette.suggestFor(pkg))
    }

    fun colorFor(pkg: String): Int = sp.getInt("color_" + pkg, Palette.suggestFor(pkg))

    fun setColor(pkg: String, color: Int) = sp.edit().putInt("color_" + pkg, color).apply()
}
