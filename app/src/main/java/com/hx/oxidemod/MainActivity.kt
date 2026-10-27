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
            text = "Перевірка дозволів..."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            addView(tv)
            gravity = Gravity.CENTER
        }
        setContentView(layout)

        if (!Settings.canDrawOverlays(this)) {
            tv.text = "Потрібен дозвіл 'Поверх інших додатків'. Тапни сюди, щоб відкрити налаштування."
            layout.setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
            return
        }

        tv.text = "Запит Root... (якщо висить довго - перевір Magisk)"
        
        Thread {
            val deploy = deployHelper()
            val start = RootHelper.instance.start("/data/local/tmp/hx_helper")
            
            runOnUiThread {
                if (!deploy) {
                    tv.text = "Помилка: не вдалося скопіювати helper.\nПеревір, чи дав ти дозвіл Root.\nТапни, щоб спробувати ще."
                    layout.setOnClickListener { recreate() }
                } else if (!start) {
                    tv.text = "Помилка: не вдалося запустити helper.\nПеревір Root.\nТапни, щоб спробувати ще."
                    layout.setOnClickListener { recreate() }
                } else {
                    tv.text = "Успіх! Оверлей запущено.\nШукай кнопку ≡ на екрані."
                    startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                }
            }
        }.start()
    }

    private fun deployHelper(): Boolean = try {
        val tmp = File(cacheDir, "hx_helper")
        assets.open("helper").use { it.copyTo(tmp.outputStream()) }
        val p = ProcessBuilder("su", "-c",
            "cp ${tmp.absolutePath} /data/local/tmp/hx_helper && " +
            "chmod 755 /data/local/tmp/hx_helper && echo OK")
            .redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out.contains("OK")
    } catch (e: Throwable) { 
        e.printStackTrace()
        false 
    }
}
