package com.antigravity.droidconnect.model

import android.graphics.drawable.Drawable

data class NotificationPayload(
    val appName: String,
    val packageName: String,
    val title: String,
    val message: String,
    val postTime: Long = System.currentTimeMillis(),
    val priority: Int = 3,
    val tags: List<String> = emptyList()
)

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    var isEnabled: Boolean
)
