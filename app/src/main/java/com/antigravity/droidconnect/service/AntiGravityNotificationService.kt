package com.antigravity.droidconnect.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.antigravity.droidconnect.deliverer.LocalHttpDeliverer
import com.antigravity.droidconnect.deliverer.NotificationDeliverer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AntiGravityNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var deliverer: NotificationDeliverer

    override fun onCreate() {
        super.onCreate()
        deliverer = LocalHttpDeliverer(applicationContext)
        Log.i(TAG, "AntiGravityNotificationService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, ">>> NotificationListener connected to Android OS successfully!")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, ">>> NotificationListener disconnected! Requesting rebind...")
        try {
            requestRebind(ComponentName(this, AntiGravityNotificationService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to requestRebind on disconnect: ${e.message}", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        try {
            val packageName = sbn.packageName ?: return
            if (packageName == applicationContext.packageName) return

            // Prevent system and media player notification flood
            if (sbn.isOngoing) {
                Log.d(TAG, "Ignoring ongoing notification from $packageName")
                return
            }

            val prefs = getSharedPreferences(LocalHttpDeliverer.PREFS_NAME, Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean(LocalHttpDeliverer.KEY_FORWARDING_ACTIVE, true)
            if (!isEnabled) {
                Log.d(TAG, "Forwarding is disabled in settings, skipping notification from $packageName")
                return
            }

            val notification = sbn.notification ?: return
            val extras = notification.extras ?: Bundle()

            val appName = resolveAppName(packageName)
            val title = extractTitle(extras, sbn, appName)
            var text = extractText(extras, sbn)

            // If text is still blank, provide a fallback so it doesn't fail downstream
            if (text.isBlank()) {
                val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                text = if (isGroupSummary) "New message summary" else "New notification"
            }

            Log.d(TAG, "Dispatching: [$appName ($packageName)] $title -> $text")

            // Asynchronous non-blocking dispatch
            serviceScope.launch {
                deliverer.deliver(
                    packageName = packageName,
                    appName = appName,
                    title = title,
                    text = text
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process notification: ${e.message}", e)
        }
    }

    private fun extractTitle(extras: Bundle, sbn: StatusBarNotification, defaultAppName: String): String {
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()

        if (!title.isNullOrBlank()) return title.trim()

        val ticker = sbn.notification?.tickerText?.toString()
        if (!ticker.isNullOrBlank()) return ticker.trim()

        return defaultAppName
    }

    private fun extractText(extras: Bundle, sbn: StatusBarNotification): String {
        // 1. Check standard expanded text
        var text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!text.isNullOrBlank()) return text.trim()

        // 2. Check standard content text
        text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (!text.isNullOrBlank()) return text.trim()

        // 3. Check InboxStyle text lines (e.g. WhatsApp, Gmail)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!textLines.isNullOrEmpty()) {
            val latestLine = textLines.lastOrNull { !it.isNullOrBlank() }?.toString()
            if (!latestLine.isNullOrBlank()) {
                return latestLine.trim()
            }
        }

        // 4. Check MessagingStyle messages
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (!messages.isNullOrEmpty()) {
            val lastMsg = messages.lastOrNull() as? Bundle
            val msgText = lastMsg?.getCharSequence("text")?.toString()
            val sender = lastMsg?.getCharSequence("sender")?.toString()
            if (!msgText.isNullOrBlank()) {
                return if (!sender.isNullOrBlank()) "$sender: $msgText" else msgText.trim()
            }
        }

        // 5. Check subtext / info text / summary text
        text = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        if (!text.isNullOrBlank()) return text.trim()

        text = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        if (!text.isNullOrBlank()) return text.trim()

        text = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        if (!text.isNullOrBlank()) return text.trim()

        // 6. Fallback to tickerText
        val ticker = sbn.notification?.tickerText?.toString()
        if (!ticker.isNullOrBlank()) return ticker.trim()

        return ""
    }

    private fun resolveAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        } catch (e: Exception) {
            packageName
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "AntiGravityService"
    }
}
