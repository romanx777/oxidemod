package com.hx.oxidemod

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val tv = TextView(this).apply {
            text = "Ініціалізація..."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#111111"))
            addView(tv)
            gravity = Gravity.CENTER
        }
        setContentView(layout)

        if (!Settings.canDrawOverlays(this)) {
            tv.text = "ПОТРІБЕН ДОЗВІЛ:\nНатисни сюди, щоб дозволити 'Поверх інших додатків'"
            layout.setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
            return
        }

        tv.text = "Розпакування та запуск..."
        
        Thread {
            try {
                val tmp = File(cacheDir, "hx_helper")
                assets.open("helper").use { it.copyTo(tmp.outputStream()) }
                tmp.setExecutable(true) 
                
                runOnUiThread { tv.text = "Запит Root... (якщо Magisk запитає - дозволь)" }
                
                val started = RootHelper.instance.start(tmp.absolutePath)
                
                runOnUiThread {
                    if (started) {
                        tv.text = "УСПІХ!\nОверлей запущено.\nШукай кнопку ≡ на екрані.\nМожеш згорнути цей додаток."
                        startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                    } else {
                        tv.text = "ПОМИЛКА ROOT.\nПеревір Magisk, чи дозволено для oxide mod."
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { tv.text = "Помилка: ${e.message}" }
            }
        }.start()
    }
}
