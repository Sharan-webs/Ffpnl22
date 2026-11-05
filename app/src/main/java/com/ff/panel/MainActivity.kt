package com.ff.panel

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val hasOverlay = Settings.canDrawOverlays(this)

        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        } else {
            true
        }

        val prefs = getSharedPreferences("ff_panel", MODE_PRIVATE)
        val serviceRunning = prefs.getBoolean("running", false)

        // ——— Overlay Permission Card ———
        val tvOverlayStatus = findViewById<TextView>(R.id.tvOverlayStatus)
        val btnGrantOverlay = findViewById<Button>(R.id.btnGrantOverlay)

        if (hasOverlay) {
            tvOverlayStatus.text = "✓  GRANTED"
            tvOverlayStatus.setTextColor(0xFF00FF88.toInt())
            btnGrantOverlay.visibility = View.GONE
        } else {
            tvOverlayStatus.text = "✗  REQUIRED"
            tvOverlayStatus.setTextColor(0xFFFF4444.toInt())
            btnGrantOverlay.visibility = View.VISIBLE
            btnGrantOverlay.setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        // ——— Notification Permission Card ———
        val cardNotif = findViewById<LinearLayout>(R.id.cardNotif)
        val tvNotifStatus = findViewById<TextView>(R.id.tvNotifStatus)
        val btnGrantNotif = findViewById<Button>(R.id.btnGrantNotif)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cardNotif.visibility = View.VISIBLE
            if (hasNotif) {
                tvNotifStatus.text = "✓  GRANTED"
                tvNotifStatus.setTextColor(0xFF00FF88.toInt())
                btnGrantNotif.visibility = View.GONE
            } else {
                tvNotifStatus.text = "✗  REQUIRED"
                tvNotifStatus.setTextColor(0xFFFF4444.toInt())
                btnGrantNotif.visibility = View.VISIBLE
                btnGrantNotif.setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissions(
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            101
                        )
                    }
                }
            }
        } else {
            cardNotif.visibility = View.GONE
        }

        // ——— Start / Stop Button ———
        val btnStartStop = findViewById<Button>(R.id.btnStartStop)

        if (serviceRunning) {
            btnStartStop.text = "■  STOP OVERLAY"
            btnStartStop.setBackgroundColor(0xFF1A1A1A.toInt())
            btnStartStop.setTextColor(0xFFFF4444.toInt())
            btnStartStop.isEnabled = true
            btnStartStop.setOnClickListener {
                stopService(Intent(this, FloatingService::class.java))
                btnStartStop.postDelayed({ updateUI() }, 400)
            }
        } else {
            btnStartStop.text = "▶  START OVERLAY"
            if (hasOverlay) {
                btnStartStop.setBackgroundColor(0xFFFF4444.toInt())
                btnStartStop.setTextColor(0xFFFFFFFF.toInt())
                btnStartStop.isEnabled = true
                btnStartStop.setOnClickListener {
                    val intent = Intent(this, FloatingService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    btnStartStop.postDelayed({ updateUI() }, 400)
                }
            } else {
                btnStartStop.setBackgroundColor(0xFF1A1A1A.toInt())
                btnStartStop.setTextColor(0xFF555555.toInt())
                btnStartStop.isEnabled = false
            }
        }
    }
}
