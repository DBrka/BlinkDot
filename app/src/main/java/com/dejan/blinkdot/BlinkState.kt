package com.dejan.blinkdot

/**
 * Packages that currently have an unread notification, with the colour each
 * should blink in. The blink screen cycles through these, so two waiting
 * messages alternate colours instead of one hiding the other.
 */
object BlinkState {

    private val pending = LinkedHashMap<String, Int>()

    @Volatile
    private var listener: (() -> Unit)? = null

    fun setListener(l: (() -> Unit)?) {
        listener = l
    }

    fun add(pkg: String, color: Int) {
        synchronized(pending) {
            pending.remove(pkg)
            pending[pkg] = color
        }
        listener?.invoke()
    }

    fun remove(pkg: String) {
        val changed = synchronized(pending) { pending.remove(pkg) != null }
        if (changed) listener?.invoke()
    }

    fun clear() {
        synchronized(pending) { pending.clear() }
    }

    fun colors(): List<Int> = synchronized(pending) { pending.values.toList() }

    fun isEmpty(): Boolean = synchronized(pending) { pending.isEmpty() }
}
