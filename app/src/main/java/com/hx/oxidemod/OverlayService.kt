package com.hx.oxidemod

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var buttonView: View? = null
    private var panelView: View? = null
    private lateinit var btnParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams
    private val engine = PatchEngine()
    private var infoText: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        engine.start()
        showButton()
    }

    override fun onDestroy() {
        engine.stop()
        removeViews()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel("hx", "mod", NotificationManager.IMPORTANCE_LOW))
        }
        return Notification.Builder(this, "hx")
            .setContentTitle("oxide mod").setContentText("running")
            .setSmallIcon(android.R.drawable.ic_menu_manage).build()
    }

    private fun showButton() {
        if (buttonView != null) return
        val tv = TextView(this).apply {
            text = "≡"; setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(180, 20, 20, 20))
        }
        btnParams = WindowManager.LayoutParams(
            120, 120,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 32; y = 200 }
        tv.setOnTouchListener(buttonTouch())
        buttonView = tv
        wm.addView(tv, btnParams)
    }

    private var downX = 0f; private var downY = 0f
    private var initX = 0; private var initY = 0; private var moved = false

    private fun buttonTouch() = View.OnTouchListener { _, ev ->
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX; downY = ev.rawY
                initX = btnParams.x; initY = btnParams.y; moved = false; true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - downX; val dy = ev.rawY - downY
                if (abs(dx) + abs(dy) > 16f) moved = true
                btnParams.x = initX + dx.toInt(); btnParams.y = initY + dy.toInt()
                wm.updateViewLayout(buttonView, btnParams); true
            }
            MotionEvent.ACTION_UP -> { if (!moved) togglePanel(); true }
            else -> false
        }
    }

    private fun togglePanel() { if (panelView != null) removePanel() else showPanel() }

    private fun showPanel() {
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.argb(230, 15, 15, 15))
        }
        ll.addView(TextView(this).apply { text = "oxide mod v2"; setTextColor(Color.WHITE); textSize = 16f; setPadding(0, 0, 0, 12) })
        infoText = TextView(this).apply { setTextColor(Color.LTGRAY); textSize = 11f; setPadding(0, 0, 0, 12) }
        ll.addView(infoText)
        
        fun cb(l: String, i: Boolean, f: (Boolean) -> Unit) {
            ll.addView(CheckBox(this).apply {
                text = l; setTextColor(Color.WHITE); isChecked = i
                setOnCheckedChangeListener { _, v -> f(v) }
            })
        }
        cb("freeze health", engine.enabledHealth) { engine.enabledHealth = it }
        cb("freeze ammo",   engine.enabledAmmo)   { engine.enabledAmmo = it }
        cb("speed",         engine.enabledSpeed)  { engine.enabledSpeed = it }
        
        ll.addView(TextView(this).apply {
            text = "❌ ВИМКНУТИ МОД"; setTextColor(Color.parseColor("#ff5555")); textSize = 14f; setPadding(0, 20, 0, 0)
            setOnClickListener { stopSelf() }
        })
        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 200; y = 300 }
        panelView = ll
        wm.addView(ll, panelParams)
        
        // Оновлення статусу
        val handler = android.os.Handler(mainLooper)
        handler.post(object : Runnable {
            override fun run() {
                if (panelView == null) return
                val pid = engine.pidNow(); val base = engine.baseNow()
                infoText?.text = when {
                    pid > 0 && base != 0L -> "pid $pid\nbase 0x${java.lang.Long.toHexString(base)}"
                    pid > 0                 -> "pid $pid\nwaiting for ${Config.MODULE}"
                    else                    -> "waiting for ${Config.PACKAGE}"
                }
                handler.postDelayed(this, 500)
            }
        })
    }

    private fun removePanel() {
        panelView?.let { try { wm.removeView(it) } catch (_: Throwable) {} }
        panelView = null; infoText = null
    }
    
    private fun removeViews() {
        removePanel()
        buttonView?.let { try { wm.removeView(it) } catch (_: Throwable) {} }
        buttonView = null
    }
}
