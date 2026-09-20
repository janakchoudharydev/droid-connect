package com.antigravity.droidconnect.deliverer

/**
 * Pluggable delivery interface for forwarding captured notifications.
 * Allows switching delivery mechanisms (Local Wi-Fi, Bark, ntfy, etc.)
 * without modifying the core NotificationListenerService.
 */
interface NotificationDeliverer {
    suspend fun deliver(
        packageName: String,
        appName: String,
        title: String,
        text: String
    ): Result<Unit>
}
