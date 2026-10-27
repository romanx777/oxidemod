package com.hx.oxidemod

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
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

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun showButton() {
        if (buttonView != null) return
        val tv = TextView(this).apply {
            text = "≡"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E1E2E"))
                cornerRadius = dp(50).toFloat()
                setStroke(dp(2), Color.parseColor("#00E5FF"))
            }
        }
        btnParams = WindowManager.LayoutParams(
            dp(56), dp(56),
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = dp(16); y = dp(100) }
        
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
        val ctx = this
        val rootLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E1E2E"))
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.parseColor("#333333"))
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // Header (Drag Handle)
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        header.addView(TextView(ctx).apply {
            text = "OXIDE MOD"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        
        header.addView(TextView(ctx).apply {
            text = "✕"
            setTextColor(Color.LTGRAY)
            textSize = 18f
            setPadding(dp(16), 0, dp(8), 0)
            setOnClickListener { removePanel() }
        })
        rootLayout.addView(header)

        // Status
        infoText = TextView(ctx).apply {
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 10f
            setPadding(0, 0, 0, dp(16))
        }
        rootLayout.addView(infoText)

        fun addFeature(title: String, init: Boolean, onChange: (Boolean) -> Unit) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            row.addView(TextView(ctx).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 14f
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            
            val sw = Switch(ctx).apply {
                isChecked = init
                setOnCheckedChangeListener { _, v -> onChange(v) }
            }
            row.addView(sw)
            rootLayout.addView(row)
        }

        addFeature("God Mode / Health", engine.enabledHealth) { engine.enabledHealth = it }
        addFeature("Infinite Ammo", engine.enabledAmmo) { engine.enabledAmmo = it }
        addFeature("Speed Hack", engine.enabledSpeed) { engine.enabledSpeed = it }

        // Kill Button
        val killBtn = TextView(ctx).apply {
            text = "KILL GAME PROCESS"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF3B30"))
                cornerRadius = dp(8).toFloat()
            }
            setOnClickListener {
                val pid = engine.pidNow()
                if (pid > 0) {
                    try { Runtime.getRuntime().exec("su -c kill -9 $pid") } catch (_: Exception) {}
                    Toast.makeText(ctx, "Process Killed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "Process not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val killParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(16)
        }
        rootLayout.addView(killBtn, killParams)

        // Make header draggable
        header.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX; downY = ev.rawY
                    initX = panelParams.x; initY = panelParams.y; true
                }
                MotionEvent.ACTION_MOVE -> {
                    panelParams.x = initX + (ev.rawX - downX).toInt()
                    panelParams.y = initY + (ev.rawY - downY).toInt()
                    wm.updateViewLayout(rootLayout, panelParams); true
                }
                else -> false
            }
        }

        panelParams = WindowManager.LayoutParams(
            dp(260), WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply { 
                gravity = Gravity.TOP or Gravity.START
                x = dp(80); y = dp(200) 
            }
        
        panelView = rootLayout
        wm.addView(rootLayout, panelParams)
        
        // Info updater
        val handler = android.os.Handler(mainLooper)
        handler.post(object : Runnable {
            override fun run() {
                if (panelView == null) return
                val pid = engine.pidNow(); val base = engine.baseNow()
                infoText?.text = when {
                    pid > 0 && base != 0L -> "STATUS: ATTACHED\nPID: $pid\nBASE: 0x${java.lang.Long.toHexString(base)}"
                    pid > 0                 -> "STATUS: WAITING FOR MODULE\nPID: $pid"
                    else                    -> "STATUS: GAME NOT RUNNING\nLook for ${Config.PACKAGE}"
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
