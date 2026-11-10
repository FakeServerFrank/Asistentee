package com.nova.asistente

import android.app.Application
import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter

// Guarda el error si la app se cierra sola, para mostrarlo la próxima vez que la abras.
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                getSharedPreferences("crash", Context.MODE_PRIVATE)
                    .edit().putString("last", sw.toString()).commit()
            } catch (e: Throwable) {
                // no hacer nada
            }
            previous?.uncaughtException(thread, error)
        }
    }
}
