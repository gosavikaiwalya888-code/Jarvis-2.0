
package com.jarvis.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class FloatingNotchService : Service() {

    private lateinit var windowManager: WindowManager
    private var notchView: View? = null
    private var expanded = false

    companion object {
        private const val CHANNEL_ID = "jarvis_notch"
        private const val NOTIFICATION_ID = 10
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        if (notchView == null) {
            showNotch()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Floating Interface",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Keeps the JARVIS floating interface active"

            val manager =
                getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS is active")
            .setContentText("Floating interface is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun makeBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), Color.rgb(0, 190, 220))
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }
    }

    private fun showNotch() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = makeBackground(Color.rgb(8, 15, 25), 22f)
            setPadding(dp(16), dp(9), dp(16), dp(9))
        }

        val title = makeText(
            "✦  J.A.R.V.I.S.  ✦",
            14f,
            Color.rgb(0, 220, 255)
        )

        container.addView(
            title,
            LinearLayout.LayoutParams(
                dp(155),
                dp(35)
            )
        )

        title.setOnClickListener {
            toggleNotch(container)
        }

        notchView = container

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(35)
        }

        windowManager.addView(container, params)
    }

    private fun toggleNotch(container: LinearLayout) {
        if (expanded) {
            container.removeAllViews()

            val title = makeText(
                "✦  J.A.R.V.I.S.  ✦",
                14f,
                Color.rgb(0, 220, 255)
            )

            container.addView(title, LinearLayout.LayoutParams(dp(155), dp(35)))
            title.setOnClickListener { toggleNotch(container) }
            expanded = false
            return
        }

        container.removeAllViews()
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(dp(18), dp(14), dp(18), dp(14))

        val heading = makeText(
            "J.A.R.V.I.S. SYSTEM",
            16f,
            Color.rgb(0, 220, 255)
        )

        val status = makeText(
            "SYSTEM ONLINE",
            12f,
            Color.GREEN
        )

        val info = makeText(
            "Notification panel ready",
            12f,
            Color.LTGRAY
        )

        val open = makeText(
            "OPEN JARVIS",
            13f,
            Color.WHITE
        ).apply {
            background = makeBackground(Color.rgb(0, 90, 120), 12f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
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

        val close = makeText(
            "COLLAPSE",
            12f,
            Color.rgb(0, 220, 255)
        ).apply {
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener { toggleNotch(container) }
        }

        container.addView(heading)
        container.addView(status)
        container.addView(info)
        container.addView(open)
        container.addView(close)

        expanded = true
    }

    override fun onDestroy() {
        super.onDestroy()

        notchView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        notchView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
