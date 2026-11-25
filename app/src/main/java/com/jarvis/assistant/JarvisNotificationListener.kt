
package com.jarvis.assistant

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent

class JarvisNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(
        sbn: StatusBarNotification?
    ) {
        if (sbn == null) return

        val extras = sbn.notification.extras

        val appName = sbn.packageName
        val title = extras.getCharSequence("android.title")?.toString()
            ?: "Notification"

        val message = extras.getCharSequence("android.text")?.toString()
            ?: ""

        val update = Intent("com.jarvis.assistant.NOTIFICATION").apply {
            setPackage(packageName)
            putExtra("app", appName)
            putExtra("title", title)
            putExtra("message", message)
        }

        sendBroadcast(update)
    }
}
