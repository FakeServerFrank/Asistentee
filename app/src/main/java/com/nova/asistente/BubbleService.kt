package com.nova.asistente

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class BubbleService : Service() {

    companion object {
        @Volatile
        var running = false
    }

    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (!Settings.canDrawOverlays(this)) {
            running = false
            stopSelf()
            return START_NOT_STICKY
        }
        if (bubble == null) {
            try {
                addBubble()
            } catch (e: Throwable) {
                Toast.makeText(this, "No pude mostrar la bolita: " + e.message, Toast.LENGTH_LONG).show()
                running = false
                stopSelf()
                return START_NOT_STICKY
            }
        }
        running = true
        return START_STICKY
    }

    override fun onDestroy() {
        removeSafe(panel)
        removeSafe(bubble)
        panel = null
        bubble = null
        running = false
        super.onDestroy()
    }

    private fun startAsForeground() {
        val channelId = "bubble"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Bolita", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }
        val notification = builder
            .setContentTitle("Asistente activo")
            .setContentText("Toca para abrir la app")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun addBubble() {
        val size = dp(56)
        val view = View(this)
        view.background = orbDrawable()
        view.alpha = 0.95f

        bubbleParams = WindowManager.LayoutParams(
            size, size,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        bubbleParams.gravity = Gravity.TOP or Gravity.START
        bubbleParams.x = 0
        bubbleParams.y = dp(200)

        view.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f
            var moved = false

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = bubbleParams.x
                        startY = bubbleParams.y
                        touchX = e.rawX
                        touchY = e.rawY
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.rawX - touchX
                        val dy = e.rawY - touchY
                        if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                        if (moved) {
                            bubbleParams.x = (startX + dx).toInt()
                            bubbleParams.y = (startY + dy).toInt()
                            wm.updateViewLayout(v, bubbleParams)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (moved) snapToEdge() else togglePanel()
                        return true
                    }
                }
                return false
            }
        })

        wm.addView(view, bubbleParams)
        bubble = view
    }

    private fun snapToEdge() {
        val v = bubble ?: return
        val w = resources.displayMetrics.widthPixels
        val h = resources.displayMetrics.heightPixels
        val size = dp(56)
        val target = if (bubbleParams.x + size / 2 < w / 2) 0 else w - size
        bubbleParams.y = bubbleParams.y.coerceIn(0, h - size - dp(80))
        val anim = ValueAnimator.ofInt(bubbleParams.x, target)
        anim.duration = 180
        anim.addUpdateListener {
            bubbleParams.x = it.animatedValue as Int
            if (v.isAttachedToWindow) wm.updateViewLayout(v, bubbleParams)
        }
        anim.start()
    }

    private fun togglePanel() {
        if (panel != null) hidePanel() else showPanel()
    }

    private fun showPanel() {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(20), dp(20), dp(20), dp(20))
        val bg = GradientDrawable()
        bg.cornerRadius = dp(24).toFloat()
        bg.setColor(Color.parseColor("#F20F1220"))
        card.background = bg

        val title = TextView(this)
        title.text = "Asistente"
        title.textSize = 20f
        title.setTextColor(Color.WHITE)
        card.addView(title)

        val info = TextView(this)
        info.text = "Aquí va a ir la caja para escribirle comandos (parte 3)."
        info.textSize = 14f
        info.setTextColor(Color.parseColor("#AEB4CC"))
        info.setPadding(0, dp(8), 0, dp(8))
        card.addView(info)

        val close = makeButton("Cerrar panel", "#374151") { hidePanel() }
        val closeParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
        closeParams.topMargin = dp(10)
        card.addView(close, closeParams)

        val quit = makeButton("Quitar bolita", "#B91C1C") {
            running = false
            stopSelf()
        }
        val quitParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
        quitParams.topMargin = dp(10)
        card.addView(quit, quitParams)

        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.86f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        wm.addView(card, params)
        panel = card
    }

    private fun hidePanel() {
        removeSafe(panel)
        panel = null
    }

    private fun removeSafe(v: View?) {
        if (v == null) return
        try {
            wm.removeView(v)
        } catch (e: Exception) {
            // ya estaba quitada
        }
    }
}
