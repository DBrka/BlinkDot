package com.dejan.blinkdot

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Preset swatches plus RGB sliders, previewed on black like the real thing. */
object ColorPicker {

    fun show(ctx: Context, initial: Int, onPick: (Int) -> Unit) {
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_color, null)

        val preview = view.findViewById<View>(R.id.preview)
        val hex = view.findViewById<TextView>(R.id.hex)
        val grid = view.findViewById<GridLayout>(R.id.grid)
        val seekR = view.findViewById<SeekBar>(R.id.seekR)
        val seekG = view.findViewById<SeekBar>(R.id.seekG)
        val seekB = view.findViewById<SeekBar>(R.id.seekB)

        var current = initial

        fun render() {
            preview.background = Ui.circle(ctx, current)
            hex.text = String.format("#%06X", 0xFFFFFF and current)
        }

        fun applyToSeeks(color: Int) {
            seekR.progress = Color.red(color)
            seekG.progress = Color.green(color)
            seekB.progress = Color.blue(color)
        }

        val seekListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                current = Color.rgb(seekR.progress, seekG.progress, seekB.progress)
                render()
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        }
        seekR.setOnSeekBarChangeListener(seekListener)
        seekG.setOnSeekBarChangeListener(seekListener)
        seekB.setOnSeekBarChangeListener(seekListener)

        for (color in Palette.COLORS) {
            val swatch = View(ctx)
            val params = GridLayout.LayoutParams().apply {
                width = Ui.dp(ctx, 40)
                height = Ui.dp(ctx, 40)
                setMargins(Ui.dp(ctx, 5), Ui.dp(ctx, 5), Ui.dp(ctx, 5), Ui.dp(ctx, 5))
            }
            swatch.layoutParams = params
            swatch.background = Ui.circle(ctx, color)
            swatch.setOnClickListener {
                current = color
                applyToSeeks(color)
                render()
            }
            grid.addView(swatch)
        }

        applyToSeeks(current)
        render()

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.pick_colour)
            .setView(view)
            .setPositiveButton(R.string.use_colour) { _, _ -> onPick(current) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
