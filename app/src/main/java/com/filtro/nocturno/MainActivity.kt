package com.filtro.nocturno

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("fn", MODE_PRIVATE) }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)

        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad)
        }
        fun text(t: String, size: Float = 16f) = TextView(this).apply {
            this.text = t; textSize = size; setPadding(0, pad / 2, 0, 0)
        }
        root.addView(text("Filtro nocturno", 26f))

        val sw = Switch(this).apply {
            text = "Activar filtro"; textSize = 18f
            isChecked = prefs.getBoolean("on", false)
            setPadding(0, pad, 0, pad)
        }
        root.addView(sw)

        fun slider(label: String, key: String, def: Int): SeekBar {
            root.addView(text(label))
            return SeekBar(this).apply {
                max = 100; progress = prefs.getInt(key, def)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(b: SeekBar?, v: Int, u: Boolean) {
                        prefs.edit().putInt(key, v).apply(); refresh()
                    }
                    override fun onStartTrackingTouch(b: SeekBar?) {}
                    override fun onStopTrackingTouch(b: SeekBar?) {}
                })
                root.addView(this)
            }
        }
        slider("Intensidad", "inten", 55)
        slider("Color: ámbar → rojo", "temp", 50)

        sw.setOnCheckedChangeListener { _, on ->
            if (on && !Settings.canDrawOverlays(this)) {
                sw.isChecked = false
                Toast.makeText(this, "Permite «Mostrar sobre otras apps» y vuelve", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } else {
                prefs.edit().putBoolean("on", on).apply(); refresh()
            }
        }
        root.addView(text("Puedes cerrar la app: el filtro sigue activo mientras la notificación esté visible.", 13f))
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun refresh() {
        val i = Intent(this, OverlayService::class.java)
        if (prefs.getBoolean("on", false)) startForegroundService(i)
        else startService(i.setAction("STOP"))
    }
}
