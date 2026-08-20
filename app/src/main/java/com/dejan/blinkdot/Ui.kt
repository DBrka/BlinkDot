package com.dejan.blinkdot

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue

object Ui {

    fun dp(ctx: Context, value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        ctx.resources.displayMetrics
    ).toInt()

    fun circle(ctx: Context, color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(dp(ctx, 1), 0x33FFFFFF)
    }
}
