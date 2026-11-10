package com.nova.asistente

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F1220"))
            setPadding(dp(32), dp(32), dp(32), dp(32))
        }

        val orb = TextView(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#7C5CFF"), Color.parseColor("#27D3FF"))
                gradientType = GradientDrawable.LINEAR_GRADIENT
            }
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96)).apply {
                bottomMargin = dp(28)
            }
        }

        val title = TextView(this).apply {
            text = "Asistente"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Una bolita flotante que te ayuda sin salir de lo que estás haciendo."
            textSize = 15f
            setTextColor(Color.parseColor("#AEB4CC"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(32))
        }

        status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(20))
        }

        button = Button(this).apply {
            setTextColor(Color.WHITE)
            isAllCaps = false
            textSize = 16f
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(Color.parseColor("#7C5CFF"))
            }
            setOnClickListener { openOverlaySettings() }
        }

        root.addView(orb)
        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        root.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        )
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        if (Settings.canDrawOverlays(this)) {
            status.text = "Permiso activado ✓"
            status.setTextColor(Color.parseColor("#4ADE80"))
            button.text = "Revisar permiso"
        } else {
            status.text = "Falta el permiso para mostrarse sobre otras apps"
            status.setTextColor(Color.parseColor("#FBBF24"))
            button.text = "Activar permiso"
        }
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
