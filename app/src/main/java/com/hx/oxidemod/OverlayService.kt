package com.hx.oxidemod

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var islandView: View? = null
    private var panelView: View? = null
    private var espView: EspView? = null
    private lateinit var islandParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams
    private lateinit var espParams: WindowManager.LayoutParams
    private var selectedCatIndex = 0
    private var selectedSubIndex = 0
    private var islandText: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        PatchEngine.start()
        showIsland()
        showEsp()
    }

    override fun onDestroy() { PatchEngine.stop(); removeViews(); super.onDestroy() }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= 26) (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel("hx", "mod", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, "hx").setContentTitle("CHEAT MENU").setSmallIcon(android.R.drawable.ic_menu_manage).build()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    inner class EspView(context: Context) : View(context) {
        private val fovPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat(); isAntiAlias = true }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (PatchEngine.visuals.fovEnabled) canvas.drawCircle(width / 2f, height / 2f, PatchEngine.visuals.fovRadius, fovPaint)
        }
    }

    private fun showEsp() {
        if (espView != null) return
        espView = EspView(this)
        espParams = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT)
        wm.addView(espView, espParams)
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable { override fun run() { espView?.invalidate(); handler.postDelayed(this, 30) } })
    }

    private fun showIsland() {
        if (islandView != null) return
        val ctx = this
        val island = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(10), dp(20), dp(10))
            background = GradientDrawable().apply { setColor(Color.parseColor("#000000")); cornerRadius = dp(30).toFloat(); setStroke(dp(1), Color.parseColor("#333333")) }
        }
        islandText = TextView(ctx).apply { text = "0 ГРАВЦІВ"; setTextColor(Color.WHITE); textSize = 12f; setTypeface(null, Typeface.BOLD) }
        island.addView(islandText)
        islandParams = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(50) }
        island.setOnClickListener { togglePanel() }
        islandView = island
        wm.addView(island, islandParams)
        
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable { 
            override fun run() { 
                islandText?.text = "${PatchEngine.playersNearby} ГР. | ${PatchEngine.closestPlayerDist}м"
                handler.postDelayed(this, 1000) 
            } 
        })
    }

    private fun togglePanel() { if (panelView != null) removePanel() else showPanel() }

    private fun showPanel() {
        val ctx = this
        val mainLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; background = GradientDrawable().apply { setColor(Color.parseColor("#0F0F0F")); cornerRadius = dp(16).toFloat(); setStroke(dp(1), Color.parseColor("#2C2C2C")) }
        }
        val header = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        header.addView(TextView(ctx).apply { text = "CHEAT MENU"; setTextColor(Color.parseColor("#007AFF")); textSize = 16f; setTypeface(null, Typeface.BOLD) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextView(ctx).apply { text = "X"; setTextColor(Color.GRAY); textSize = 18f; setTypeface(null, Typeface.BOLD); setPadding(dp(16), 0, 0, 0); setOnClickListener { removePanel() } })
        mainLayout.addView(header)

        val body = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(12), 0, dp(12), dp(12)) }
        val sidebar = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        body.addView(sidebar, LinearLayout.LayoutParams(dp(80), LinearLayout.LayoutParams.MATCH_PARENT))
        val contentWrapper = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, 0, 0) }
        body.addView(contentWrapper, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        val subTabsScroll = HorizontalScrollView(ctx)
        val subTabsLayout = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, dp(8)) }
        subTabsScroll.addView(subTabsLayout); contentWrapper.addView(subTabsScroll)
        val contentContainer = FrameLayout(ctx); contentWrapper.addView(contentContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        fun buildContent() {
            subTabsLayout.removeAllViews()
            val cat = Features.menuData[selectedCatIndex]
            for ((i, sub) in cat.subs.withIndex()) {
                val chip = TextView(ctx).apply {
                    text = sub.name.uppercase(); setPadding(dp(12), dp(6), dp(12), dp(6)); textSize = 11f; setTypeface(null, Typeface.BOLD)
                    setTextColor(if (i == selectedSubIndex) Color.WHITE else Color.GRAY)
                    background = GradientDrawable().apply { setColor(if (i == selectedSubIndex) Color.parseColor("#1A1A1A") else Color.TRANSPARENT); cornerRadius = dp(8).toFloat() }
                    setOnClickListener { selectedSubIndex = i; buildContent() }
                }
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, 0, dp(8), 0)
                subTabsLayout.addView(chip, lp)
            }
            contentContainer.removeAllViews()
            val scroll = ScrollView(ctx); val list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }; scroll.addView(list); contentContainer.addView(scroll)
            val sub = cat.subs[selectedSubIndex]
            
            for (el in sub.elements) {
                val card = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12))
                    background = GradientDrawable().apply { setColor(Color.parseColor("#1A1A1A")); cornerRadius = dp(8).toFloat() }
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, 0, 0, dp(8)); layoutParams = lp
                }
                when (el) {
                    is Features.MenuElement.SwitchItem -> {
                        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                        row.addView(TextView(ctx).apply { text = el.name; setTextColor(Color.LTGRAY); textSize = 13f }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                        row.addView(Switch(ctx).apply { isChecked = el.isChecked; setOnCheckedChangeListener { _, v -> el.isChecked = v; el.action(v) } })
                        card.addView(row)
                    }
                    is Features.MenuElement.SliderItem -> {
                        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0,0,0,dp(4)) }
                        row.addView(TextView(ctx).apply { text = el.name; setTextColor(Color.LTGRAY); textSize = 13f }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                        val valText = TextView(ctx).apply { text = String.format("%.1f", el.value); setTextColor(Color.parseColor("#007AFF")); textSize = 13f; setTypeface(null, Typeface.BOLD) }
                        row.addView(valText); card.addView(row)
                        card.addView(SeekBar(ctx).apply {
                            max = 100; progress = (((el.value - el.min) / (el.max - el.min)) * 100).toInt()
                            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                                override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                                    val v = el.min + (el.max - el.min) * (p / 100f); el.value = v; valText.text = String.format("%.1f", v); el.action(v)
                                }
                                override fun onStartTrackingTouch(sb: SeekBar?) {}
                                override fun onStopTrackingTouch(sb: SeekBar?) {}
                            })
                        })
                    }
                }
                list.addView(card)
            }
        }

        for ((i, cat) in Features.menuData.withIndex()) {
            val tab = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(8), dp(12), dp(8), dp(12))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, 0, 0, dp(4)); layoutParams = lp
                if (i == selectedCatIndex) background = GradientDrawable().apply { setColor(Color.parseColor("#1A1A1A")); cornerRadius = dp(8).toFloat() }
            }
            tab.addView(TextView(ctx).apply { text = cat.tag; setTextColor(if (i == selectedCatIndex) Color.parseColor("#007AFF") else Color.GRAY); textSize = 14f; setTypeface(null, Typeface.BOLD) })
            tab.addView(TextView(ctx).apply { text = cat.name; setTextColor(if (i == selectedCatIndex) Color.WHITE else Color.GRAY); textSize = 10f; setPadding(0, dp(4), 0, 0) })
            tab.setOnClickListener {
                selectedCatIndex = i; selectedSubIndex = 0
                for (j in 0 until sidebar.childCount) {
                    val v = sidebar.getChildAt(j) as LinearLayout; v.setBackgroundColor(Color.TRANSPARENT)
                    (v.getChildAt(0) as TextView).setTextColor(Color.GRAY); (v.getChildAt(1) as TextView).setTextColor(Color.GRAY)
                }
                tab.background = GradientDrawable().apply { setColor(Color.parseColor("#1A1A1A")); cornerRadius = dp(8).toFloat() }
                (tab.getChildAt(0) as TextView).setTextColor(Color.parseColor("#007AFF")); (tab.getChildAt(1) as TextView).setTextColor(Color.WHITE)
                buildContent()
            }
            sidebar.addView(tab)
        }

        mainLayout.addView(body)
        buildContent()

        header.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f; var downY = 0f; var initX = 0; var initY = 0
            override fun onTouch(v: View?, ev: MotionEvent): Boolean {
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> { downX = ev.rawX; downY = ev.rawY; initX = panelParams.x; initY = panelParams.y; return true }
                    MotionEvent.ACTION_MOVE -> { panelParams.x = initX + (ev.rawX - downX).toInt(); panelParams.y = initY + (ev.rawY - downY).toInt(); wm.updateViewLayout(mainLayout, panelParams); return true }
                }
                return false
            }
        })

        panelParams = WindowManager.LayoutParams(dp(340), dp(450),
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(120) }
        panelView = mainLayout
        wm.addView(mainLayout, panelParams)
    }

    private fun removePanel() { panelView?.let { try { wm.removeView(it) } catch (_: Throwable) {} }; panelView = null }
    private fun removeViews() { removePanel(); islandView?.let { try { wm.removeView(it) } catch (_: Throwable) {} }; islandView = null; espView?.let { try { wm.removeView(it) } catch (_: Throwable) {} }; espView = null }
}
