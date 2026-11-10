package com.nova.asistente

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun orbDrawable(): GradientDrawable {
    val d = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.parseColor("#7C5CFF"), Color.parseColor("#27D3FF"))
    )
    d.shape = GradientDrawable.OVAL
    return d
}

fun Context.makeButton(label: String, colorHex: String, onClick: () -> Unit): Button {
    val b = Button(this)
    b.text = label
    b.setTextColor(Color.WHITE)
    b.isAllCaps = false
    b.textSize = 16f
    val bg = GradientDrawable()
    bg.cornerRadius = dp(28).toFloat()
    bg.setColor(Color.parseColor(colorHex))
    b.background = bg
    b.setOnClickListener { onClick() }
    return b
}
