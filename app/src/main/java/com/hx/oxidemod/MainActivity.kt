package com.hx.oxidemod

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F0F0F"))
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        val tv = TextView(this).apply {
            text = "CHEAT MENU"
            setTextColor(Color.parseColor("#007AFF"))
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        val btn = Button(this).apply {
            text = "УВІЙТИ"
            setBackgroundColor(Color.parseColor("#007AFF"))
            setTextColor(Color.WHITE)
            textSize = 16f
        }

        layout.addView(tv)
        layout.addView(btn)
        setContentView(layout)

        fun checkAndStart() {
            if (!Settings.canDrawOverlays(this)) {
                tv.text = "ПОТРІБЕН ДОЗВІЛ НА ОВЕРЛЕЙ"
                Toast.makeText(this, "Увімкни 'Поверх інших' і повернись", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                return
            }
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                return
            }

            btn.isEnabled = false
            btn.text = "ЗАПУСК..."
            tv.text = "ЗАПИТ ROOT (Дай дозвіл в Magisk)..."

            Thread {
                try {
                    // Прямий виклик su з таймаутом 10 секунд
                    val process = ProcessBuilder("su", "-c", "id").start()
                    val finished = process.waitFor(10, TimeUnit.SECONDS)
                    
                    if (!finished) {
                        process.destroy()
                        runOnUiThread {
                            btn.isEnabled = true
                            btn.text = "СПРОБУВАТИ ЩЕ"
                            tv.text = "ТАЙМ-АУТ.\nВідкрий Magisk вручну і дозволь root."
                        }
                        return@Thread
                    }

                    val out = process.inputStream.bufferedReader().readText()
                    if (out.contains("uid=0")) {
                        val tmp = File(cacheDir, "hx_helper")
                        assets.open("helper").use { it.copyTo(tmp.outputStream()) }
                        tmp.setExecutable(true)

                        // Копіюємо хелпер
                        val cp = ProcessBuilder("su", "-c", "cp ${tmp.absolutePath} /data/local/tmp/hx_helper && chmod 755 /data/local/tmp/hx_helper").start()
                        cp.waitFor(5, TimeUnit.SECONDS)

                        runOnUiThread {
                            tv.text = "УСПІХ!\nШукай острів зверху."
                            startForegroundService(Intent(this, OverlayService::class.java))
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            btn.isEnabled = true
                            btn.text = "СПРОБУВАТИ ЩЕ"
                            tv.text = "ПОМИЛКА: ROOT НЕ НАДАНО\n($out)"
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        btn.isEnabled = true
                        btn.text = "СПРОБУВАТИ ЩЕ"
                        tv.text = "ПОМИЛКА: ${e.message}"
                    }
                }
            }.start()
        }

        btn.setOnClickListener { checkAndStart() }
        checkAndStart()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) recreate()
    }
}
