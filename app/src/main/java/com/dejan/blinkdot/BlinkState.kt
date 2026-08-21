package com.dejan.blinkdot

/**
 * Packages that currently have an unread notification, with the colour each
 * should blink in. The blink screen cycles through these, so two waiting
 * messages alternate colours instead of one hiding the other.
 *
 * Also holds the two flags that make "blink until I open the message" work
 * without the app fighting itself:
 *
 * - [showing] tells the notification listener that the blink screen is already
 *   up, so a screen-off event means "the user dismissed it", not "bring it back".
 * - [armed] is cleared when the user dismisses the dot and set again when they
 *   unlock without reading. That is what stops a dismiss from immediately
 *   re-triggering, while still bringing the dot back the next time the phone
 *   locks with something still unread.
 */
object BlinkState {

    private val pending = LinkedHashMap<String, Int>()

    @Volatile
    private var listener: (() -> Unit)? = null

    /** True while the blink screen is alive. */
    @Volatile
    var showing = false

    /** True when the dot is allowed to appear on the next screen-off. */
    @Volatile
    var armed = false
        private set

    fun setListener(l: (() -> Unit)?) {
        listener = l
    }

    fun add(pkg: String, color: Int) {
        synchronized(pending) {
            pending.remove(pkg)
            pending[pkg] = color
        }
        armed = true
        listener?.invoke()
    }

    fun remove(pkg: String) {
        val changed = synchronized(pending) { pending.remove(pkg) != null }
        if (changed) listener?.invoke()
    }

    /** The user dismissed the dot: stay quiet until they unlock without reading. */
    fun disarm() {
        armed = false
    }

    /** Unlocked with something still unread, so blink again on the next lock. */
    fun rearmIfPending() {
        if (!isEmpty()) armed = true
    }

    fun clear() {
        synchronized(pending) { pending.clear() }
        armed = false
    }

    fun colors(): List<Int> = synchronized(pending) { pending.values.toList() }

    fun isEmpty(): Boolean = synchronized(pending) { pending.isEmpty() }
}
