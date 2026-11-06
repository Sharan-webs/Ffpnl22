package com.ff.panel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var panelVisible = false

    companion object {
        const val CHANNEL_ID = "ff_panel_channel"
        const val NOTIF_ID = 1
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        getSharedPreferences("ff_panel", MODE_PRIVATE)
            .edit().putBoolean("running", true).apply()

        createNotificationChannel()

        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }

        setupOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FF Panel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "FF Headshot Panel overlay"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FF Panel Running")
            .setContentText("Open Free Fire — tap icon to toggle panel")
            .setSmallIcon(R.drawable.ic_notif)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 250
        }

        val tvIcon         = floatingView.findViewById<TextView>(R.id.tvIcon)
        val panelContainer = floatingView.findViewById<LinearLayout>(R.id.panelContainer)
        val tvStatus       = floatingView.findViewById<TextView>(R.id.tvStatus)
        val switchHS       = floatingView.findViewById<SwitchCompat>(R.id.switchHeadshot)
        val tvClose        = floatingView.findViewById<TextView>(R.id.tvClose)

        var initX = 0; var initY = 0
        var touchX = 0f; var touchY = 0f
        var moved = false

        tvIcon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    params.x = initX + dx
                    params.y = initY + dy
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        panelVisible = !panelVisible
                        panelContainer.visibility =
                            if (panelVisible) View.VISIBLE else View.GONE
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }
                else -> false
            }
        }

        switchHS.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                tvStatus.text = "HEADSHOT: ON"
                tvStatus.setTextColor(0xFF00FF88.toInt())
                tvIcon.setBackgroundResource(R.drawable.icon_bg_active)
            } else {
                tvStatus.text = "HEADSHOT: OFF"
                tvStatus.setTextColor(0xFFFF4444.toInt())
                tvIcon.setBackgroundResource(R.drawable.icon_bg)
            }
        }

        tvClose.setOnClickListener { stopSelf() }

        windowManager.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        getSharedPreferences("ff_panel", MODE_PRIVATE)
            .edit().putBoolean("running", false).apply()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
