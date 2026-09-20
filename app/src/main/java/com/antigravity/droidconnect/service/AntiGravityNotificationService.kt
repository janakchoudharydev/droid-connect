package com.antigravity.droidconnect.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.antigravity.droidconnect.AntiGravityApp
import com.antigravity.droidconnect.model.NotificationPayload
import com.antigravity.droidconnect.network.WebhookClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AntiGravityNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        try {
            val prefs = AntiGravityApp.instance.preferences

            // 1. Check master toggle
            if (!prefs.isForwardingEnabled) {
                return
            }

            // 2. Smart filtering: Ignore ongoing notifications (media controls, foreground service alerts, persistent status)
            if (sbn.isOngoing) {
                return
            }

            val packageName = sbn.packageName ?: return

            // 3. Ignore self package
            if (packageName == applicationContext.packageName) {
                return
            }

            // 4. Check user filtering engine (blacklist/whitelist & system noise)
            if (!prefs.isAppAllowed(packageName)) {
                return
            }

            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            // 5. Null-safe extra extraction with fallbacks for OEM quirks
            val title = extractTitle(extras, packageName)
            val message = extractMessage(extras)

            // Skip if there is no useful content
            if (title.isBlank() && message.isBlank()) {
                return
            }

            val appName = getAppLabel(packageName)

            val payload = NotificationPayload(
                appName = appName,
                packageName = packageName,
                title = title.ifBlank { appName },
                message = message.ifBlank { "Notification from $appName" },
                postTime = sbn.postTime,
                priority = 3,
                tags = listOf(sanitizeTag(appName))
            )

            // 6. Asynchronous dispatch via WebhookClient
            serviceScope.launch {
                WebhookClient.sendNotification(
                    endpointUrl = prefs.webhookUrl,
                    payload = payload,
                    encryptionKey = prefs.encryptionKey
                )
            }
        } catch (e: Exception) {
            // Ensure any unexpected OEM notification format fails silently without crashing the background service
            Log.e(TAG, "Error handling notification: ${e.message}", e)
        }
    }

    private fun extractTitle(extras: android.os.Bundle, packageName: String): String {
        val titleCandidate = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()

        return titleCandidate?.trim() ?: ""
    }

    private fun extractMessage(extras: android.os.Bundle): String {
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!bigText.isNullOrBlank()) {
            return bigText.trim()
        }

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (!text.isNullOrBlank()) {
            return text.trim()
        }

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        if (!subText.isNullOrBlank()) {
            return subText.trim()
        }

        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        return infoText?.trim() ?: ""
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        } catch (e: Exception) {
            packageName
        }
    }

    private fun sanitizeTag(raw: String): String {
        return raw.lowercase().replace("[^a-z0-9_]".toRegex(), "_").take(16)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "AntiGravityService"
    }
}
