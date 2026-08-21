package com.dejan.blinkdot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    private val cornerIds = intArrayOf(R.id.cornerTL, R.id.cornerTR, R.id.cornerBL, R.id.cornerBR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        val master = findViewById<SwitchCompat>(R.id.swMaster)
        master.isChecked = prefs.enabled
        master.setOnCheckedChangeListener { _, checked -> prefs.enabled = checked }

        findViewById<MaterialButton>(R.id.btnApps).setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnTest).setOnClickListener {
            val first = prefs.enabledApps().firstOrNull()
            Blink.preview(this, if (first != null) prefs.colorFor(first) else 0xFF5B8CFF.toInt())
        }

        val corner = findViewById<MaterialButtonToggleGroup>(R.id.toggleCorner)
        corner.check(cornerIds[prefs.position.coerceIn(0, 3)])
        corner.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val index = cornerIds.indexOf(checkedId)
            if (index >= 0) prefs.position = index
        }

        slider(R.id.sliderSize, getString(R.string.dot_size), 6, 44, 1, prefs.dotSizeDp,
            { it.toString() + " dp" }) { prefs.dotSizeDp = it }

        slider(R.id.sliderMargin, getString(R.string.margin), 4, 90, 2, prefs.marginDp,
            { it.toString() + " dp" }) { prefs.marginDp = it }

        slider(R.id.sliderOn, getString(R.string.on_ms), 100, 2000, 50, prefs.onMs,
            { it.toString() + " ms" }) { prefs.onMs = it }

        slider(R.id.sliderOff, getString(R.string.off_ms), 200, 6000, 100, prefs.offMs,
            { it.toString() + " ms" }) { prefs.offMs = it }

        slider(R.id.sliderBright, getString(R.string.brightness), 1, 100, 1, prefs.brightnessPct,
            { it.toString() + " %" }) { prefs.brightnessPct = it }

        slider(R.id.sliderTimeout, getString(R.string.timeout), 0, 60, 5, prefs.timeoutMin,
            { if (it == 0) "until unlocked" else it.toString() + " min" }) { prefs.timeoutMin = it }

        // The give-up timer is meaningless while "blink until read" is on, so
        // it only appears when that switch is off.
        val timeoutRow = findViewById<View>(R.id.sliderTimeout)
        timeoutRow.visibility = if (prefs.blinkUntilRead) View.GONE else View.VISIBLE

        switchRow(R.id.switchUntilRead, getString(R.string.until_read),
            getString(R.string.until_read_sub), prefs.blinkUntilRead) { on ->
            prefs.blinkUntilRead = on
            timeoutRow.visibility = if (on) View.GONE else View.VISIBLE
        }

        switchRow(R.id.switchSmooth, getString(R.string.smooth), getString(R.string.smooth_sub),
            prefs.smooth) { prefs.smooth = it }

        switchRow(R.id.switchGlow, getString(R.string.glow), getString(R.string.glow_sub),
            prefs.glow) { prefs.glow = it }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        refreshAppCount()
    }

    private fun refreshAppCount() {
        val count = prefs.enabledApps().size
        findViewById<TextView>(R.id.tvAppCount).text =
            if (count == 0) getString(R.string.no_apps_selected)
            else getString(R.string.apps_selected, count)
    }

    private fun refreshPermissions() {
        permissionRow(
            R.id.rowNotif,
            getString(R.string.notif_access),
            getString(R.string.notif_access_why),
            NotifListener.hasAccess(this)
        ) {
            open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        permissionRow(
            R.id.rowOverlay,
            getString(R.string.overlay),
            getString(R.string.overlay_why),
            Settings.canDrawOverlays(this)
        ) {
            open(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
            )
        }

        permissionRow(
            R.id.rowBattery,
            getString(R.string.battery),
            getString(R.string.battery_why),
            batteryUnrestricted()
        ) {
            open(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + packageName)
                )
            )
        }

        permissionRow(
            R.id.rowPost,
            getString(R.string.post_notif),
            getString(R.string.post_notif_why),
            canPostNotifications()
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    77
                )
            }
        }
    }

    private fun batteryUnrestricted(): Boolean {
        val power = getSystemService(PowerManager::class.java) ?: return false
        return power.isIgnoringBatteryOptimizations(packageName)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (ignored: Throwable) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun permissionRow(
        rowId: Int,
        title: String,
        why: String,
        granted: Boolean,
        action: () -> Unit
    ) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.permTitle).text = title
        row.findViewById<TextView>(R.id.permWhy).text = why
        val button = row.findViewById<MaterialButton>(R.id.permBtn)
        button.text = getString(if (granted) R.string.granted else R.string.grant)
        button.isEnabled = !granted
        button.alpha = if (granted) 0.45f else 1f
        button.setOnClickListener { action() }
    }

    private fun switchRow(
        rowId: Int,
        title: String,
        sub: String,
        value: Boolean,
        save: (Boolean) -> Unit
    ) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.switchTitle).text = title
        row.findViewById<TextView>(R.id.switchSub).text = sub
        val box = row.findViewById<SwitchCompat>(R.id.switchBox)
        box.isChecked = value
        box.setOnCheckedChangeListener { _, checked -> save(checked) }
    }

    private fun slider(
        rowId: Int,
        title: String,
        min: Int,
        max: Int,
        step: Int,
        value: Int,
        format: (Int) -> String,
        save: (Int) -> Unit
    ) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.sliderTitle).text = title
        val label = row.findViewById<TextView>(R.id.sliderValue)
        val seek = row.findViewById<SeekBar>(R.id.sliderSeek)

        val steps = (max - min) / step
        seek.max = steps
        seek.progress = ((value.coerceIn(min, max) - min) / step).coerceIn(0, steps)
        label.text = format(value)

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actual = min + progress * step
                label.text = format(actual)
                save(actual)
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })
    }
}
