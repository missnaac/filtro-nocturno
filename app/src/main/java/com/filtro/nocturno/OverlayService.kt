package com.filtro.nocturno

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.View
import android.view.WindowManager

class OverlayService : Service() {
    private var overlay: View? = null
    private lateinit var wm: WindowManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (intent?.action == "STOP") {
            removeOverlay(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            return START_NOT_STICKY
        }
        startForeground(1, notification())
        applyFilter()
        return START_STICKY
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("filtro", "Filtro nocturno", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "filtro")
            .setContentTitle("Filtro nocturno activo")
            .setContentText("Toca para ajustar")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(open).setOngoing(true).build()
    }

    private fun applyFilter() {
        val p = getSharedPreferences("fn", MODE_PRIVATE)
        val inten = p.getInt("inten", 55) / 100f
        val temp = p.getInt("temp", 50) / 100f
        // El límite de 0.75 evita que Android bloquee toques con overlays muy opacos
        val a = (inten * 0.75f * 255).toInt()
        val g = (150 + (40 - 150) * temp).toInt()
        val b = (60 + (10 - 60) * temp).toInt()
        val color = Color.argb(a, 255, g, b)
        if (overlay == null) {
            overlay = View(this)
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT)
            wm.addView(overlay, lp)
        }
        overlay?.setBackgroundColor(color)
    }

    private fun removeOverlay() {
        overlay?.let { wm.removeView(it) }
        overlay = null
    }

    override fun onDestroy() { removeOverlay(); super.onDestroy() }
}
