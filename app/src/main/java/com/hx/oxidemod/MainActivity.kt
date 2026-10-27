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
            tv.text = "ЗАПУСК ОВЕРЛЕЮ...\n(Підтверди RooT в Magisk)"

            // Просто запускаємо сервіс. Він сам викличе Magisk, коли почне шукати гру
            startForegroundService(Intent(this, OverlayService::class.java))

            // Закриваємо додаток через 2 секунди, щоб він не висів
            android.os.Handler(mainLooper).postDelayed({
                finish()
            }, 2000)
        }

        btn.setOnClickListener { checkAndStart() }
        checkAndStart()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            recreate()
        }
    }
}
