package com.hx.oxidemod

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
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
    
    private var selectedCategoryIndex = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        PatchEngine.start()
        showButton()
    }

    override fun onDestroy() {
        PatchEngine.stop()
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
                setColor(Color.parseColor("#1A1A1A"))
                cornerRadius = dp(50).toFloat()
                setStroke(dp(2), Color.parseColor("#007AFF"))
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
        val mainLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#141414"))
                cornerRadius = dp(12).toFloat()
            }
        }

        // --- Header (Avis style) ---
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0A0A0A"))
                cornerRadii = floatArrayOf(dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        
        val profileImg = TextView(ctx).apply {
            text = "👤"
            textSize = 18f
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply { setColor(Color.parseColor("#333333")); cornerRadius = dp(20).toFloat() }
        }
        header.addView(profileImg)
        
        header.addView(TextView(ctx).apply {
            text = "  OXIDE TOOLS"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        header.addView(TextView(ctx).apply {
            text = "✕"
            setTextColor(Color.LTGRAY)
            textSize = 18f
            setPadding(dp(16), 0, 0, 0)
            setOnClickListener { removePanel() }
        })
        mainLayout.addView(header)

        // --- Body (Sidebar + Content) ---
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val sidebar = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val sidebarParams = LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.MATCH_PARENT)
        body.addView(sidebar, sidebarParams)

        val contentContainer = FrameLayout(ctx).apply {
            setPadding(dp(8), dp(4), dp(4), dp(4))
        }
        body.addView(contentContainer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))

        fun buildContent() {
            contentContainer.removeAllViews()
            val scroll = ScrollView(ctx)
            val contentList = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            scroll.addView(contentList)
            contentContainer.addView(scroll)

            val cat = Features.menuData[selectedCategoryIndex]
            for (item in cat.items) {
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(12), dp(8), dp(12))
                    background = GradientDrawable().apply { setColor(Color.parseColor("#1A1A1A")); cornerRadius = dp(8).toFloat() }
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.setMargins(0, dp(4), 0, dp(4))
                    layoutParams = lp
                }
                row.addView(TextView(ctx).apply {
                    text = item.name
                    setTextColor(Color.WHITE)
                    textSize = 13f
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                val sw = Switch(ctx).apply {
                    isChecked = item.isEnabled
                    setOnCheckedChangeListener { _, v ->
                        item.isEnabled = v
                        item.action(v)
                    }
                }
                row.addView(sw)
                contentList.addView(row)
            }
        }

        for ((index, cat) in Features.menuData.withIndex()) {
            val tab = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(12), dp(8), dp(12))
                if (index == selectedCategoryIndex) {
                    background = GradientDrawable().apply { setColor(Color.parseColor("#1A1A1A")); cornerRadius = dp(8).toFloat() }
                }
            }
            tab.addView(TextView(ctx).apply { text = cat.icon; textSize = 16f; setPadding(0, 0, dp(8), 0) })
            tab.addView(TextView(ctx).apply { 
                text = cat.name; 
                setTextColor(if (index == selectedCategoryIndex) Color.parseColor("#007AFF") else Color.GRAY)
                textSize = 12f 
            })
            tab.setOnClickListener {
                selectedCategoryIndex = index
                for (i in 0 until sidebar.childCount) {
                    val v = sidebar.getChildAt(i) as LinearLayout
                    v.setBackgroundColor(Color.TRANSPARENT)
                    (v.getChildAt(1) as TextView).setTextColor(Color.GRAY)
                }
                tab.background = GradientDrawable().apply { setColor(Color.parseColor("#1A1A1A")); cornerRadius = dp(8).toFloat() }
                (tab.getChildAt(1) as TextView).setTextColor(Color.parseColor("#007AFF"))
                buildContent()
            }
            sidebar.addView(tab)
        }

        mainLayout.addView(body)
        buildContent()

        // Draggable Header
        header.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX; downY = ev.rawY
                    initX = panelParams.x; initY = panelParams.y; true
                }
                MotionEvent.ACTION_MOVE -> {
                    panelParams.x = initX + (ev.rawX - downX).toInt()
                    panelParams.y = initY + (ev.rawY - downY).toInt()
                    wm.updateViewLayout(mainLayout, panelParams); true
                }
                else -> false
            }
        }

        panelParams = WindowManager.LayoutParams(
            dp(320), dp(400),
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply { 
                gravity = Gravity.TOP or Gravity.START
                x = dp(40); y = dp(150) 
            }
        
        panelView = mainLayout
        wm.addView(mainLayout, panelParams)
    }

    private fun removePanel() {
        panelView?.let { try { wm.removeView(it) } catch (_: Throwable) {} }
        panelView = null
    }
    
    private fun removeViews() {
        removePanel()
        buttonView?.let { try { wm.removeView(it) } catch (_: Throwable) {} }
        buttonView = null
    }
}
