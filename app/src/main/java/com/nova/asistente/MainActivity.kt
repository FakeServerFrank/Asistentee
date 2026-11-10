package com.nova.asistente

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var status: TextView? = null
    private var mainButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crash = getSharedPreferences("crash", Context.MODE_PRIVATE).getString("last", null)
        if (crash != null) {
            showText("La app se cerró antes por este error:", crash)
            return
        }

        try {
            buildUi()
        } catch (e: Throwable) {
            showText("No se pudo abrir la pantalla:", e.stackTraceToString())
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun buildUi() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setPadding(dp(28), dp(28), dp(28), dp(28))

        val orb = View(this)
        orb.background = orbDrawable()
        val orbParams = LinearLayout.LayoutParams(dp(72), dp(72))
        orbParams.bottomMargin = dp(16)
        root.addView(orb, orbParams)

        val title = TextView(this)
        title.text = "Asistente"
        title.textSize = 26f
        title.setTextColor(Color.WHITE)
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER
        root.addView(title)

        val st = TextView(this)
        st.textSize = 15f
        st.gravity = Gravity.CENTER
        st.setPadding(0, dp(12), 0, dp(16))
        status = st
        root.addView(st)

        val mb = makeButton("Activar permiso", "#7C5CFF") { onMainButton() }
        mainButton = mb
        root.addView(mb, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))

        val version = TextView(this)
        version.text = "versión 0.4"
        version.textSize = 12f
        version.setTextColor(Color.parseColor("#6B7280"))
        version.gravity = Gravity.CENTER
        version.setPadding(0, dp(20), 0, 0)
        root.addView(version)

        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.setBackgroundColor(Color.parseColor("#0F1220"))
        scroll.addView(
            root,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(scroll)
    }

    private fun refreshState() {
        val st = status ?: return
        val granted = Settings.canDrawOverlays(this)
        if (!granted) {
            st.text = "Primero activa el permiso para mostrarse sobre otras apps"
            st.setTextColor(Color.parseColor("#FBBF24"))
            mainButton?.text = "Activar permiso"
        } else if (BubbleService.running) {
            st.text = "Bolita activa ✓"
            st.setTextColor(Color.parseColor("#4ADE80"))
            mainButton?.text = "Ocultar bolita"
        } else {
            st.text = "Permiso activado ✓"
            st.setTextColor(Color.parseColor("#4ADE80"))
            mainButton?.text = "Mostrar bolita"
        }
    }

    private fun onMainButton() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings()
        } else {
            toggleBubble()
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

    private fun toggleBubble() {
        if (BubbleService.running) {
            stopService(Intent(this, BubbleService::class.java))
            BubbleService.running = false
        } else {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            val i = Intent(this, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            BubbleService.running = true
            Toast.makeText(this, "Bolita activada: búscala en el borde izquierdo", Toast.LENGTH_LONG).show()
        }
        refreshState()
    }

    // Pantalla de error: muestra el texto para que lo puedas copiar y mandármelo.
    private fun showText(title: String, body: String) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setBackgroundColor(Color.parseColor("#0F1220"))
        box.setPadding(dp(20), dp(40), dp(20), dp(20))

        val t = TextView(this)
        t.text = title
        t.textSize = 18f
        t.setTextColor(Color.WHITE)
        t.typeface = Typeface.DEFAULT_BOLD
        box.addView(t)

        val scroll = ScrollView(this)
        val tv = TextView(this)
        tv.text = body
        tv.textSize = 12f
        tv.setTextColor(Color.parseColor("#FBBF24"))
        tv.setTextIsSelectable(true)
        tv.setPadding(0, dp(12), 0, dp(12))
        scroll.addView(tv)
        box.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val copy = makeButton("Copiar error", "#7C5CFF") {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("error", body))
            Toast.makeText(this, "Copiado", Toast.LENGTH_SHORT).show()
        }
        val copyParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
        copyParams.topMargin = dp(10)
        box.addView(copy, copyParams)

        val go = makeButton("Continuar", "#2563EB") {
            getSharedPreferences("crash", Context.MODE_PRIVATE).edit().remove("last").apply()
            recreate()
        }
        val goParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
        goParams.topMargin = dp(10)
        box.addView(go, goParams)

        setContentView(box)
    }
}
