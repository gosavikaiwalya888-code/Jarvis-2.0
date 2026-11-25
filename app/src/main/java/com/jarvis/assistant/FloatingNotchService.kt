
package com.jarvis.assistant

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView

class FloatingNotchService : Service() {

    private lateinit var windowManager: WindowManager
    private var notch: TextView? = null

    override fun onCreate() {
        super.onCreate()

        val channelId = "jarvis_notch"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "JARVIS Floating Notch",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("JARVIS is active")
            .setContentText("Floating assistant is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(10, notification)

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val view = TextView(this).apply {
            text = "✦ JARVIS"
            textSize = 16f
            setTextColor(Color.CYAN)
            setTypeface(null, Typeface.BOLD)
            setPadding(28, 14, 28, 14)
            setBackgroundColor(Color.rgb(10, 25, 45))
            gravity = Gravity.CENTER
            setOnClickListener {
                val intent = Intent(
                    this@FloatingNotchService,
                    MainActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 45
        }

        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            false
        }

        notch = view
        windowManager.addView(view, params)
    }

    override fun onDestroy() {
        notch?.let {
            if (::windowManager.isInitialized) {
                windowManager.removeView(it)
            }
        }
        notch = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
