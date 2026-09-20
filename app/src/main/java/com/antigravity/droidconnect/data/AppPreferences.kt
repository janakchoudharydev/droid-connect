package com.antigravity.droidconnect.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isForwardingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FORWARDING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FORWARDING_ENABLED, value).apply()

    var webhookUrl: String
        get() = prefs.getString(KEY_WEBHOOK_URL, DEFAULT_WEBHOOK_URL) ?: DEFAULT_WEBHOOK_URL
        set(value) = prefs.edit().putString(KEY_WEBHOOK_URL, value.trim()).apply()

    var encryptionKey: String
        get() = prefs.getString(KEY_ENCRYPTION_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ENCRYPTION_KEY, value.trim()).apply()

    var filterMode: FilterMode
        get() {
            val modeName = prefs.getString(KEY_FILTER_MODE, FilterMode.BLACKLIST.name)
            return try {
                FilterMode.valueOf(modeName ?: FilterMode.BLACKLIST.name)
            } catch (e: IllegalArgumentException) {
                FilterMode.BLACKLIST
            }
        }
        set(value) = prefs.edit().putString(KEY_FILTER_MODE, value.name).apply()

    var selectedPackages: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED_PACKAGES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SELECTED_PACKAGES, HashSet(value)).apply()

    fun isAppAllowed(packageName: String): Boolean {
        // Always block system UI and self package
        if (SYSTEM_NOISE_PACKAGES.contains(packageName)) {
            return false
        }

        val packages = selectedPackages
        return when (filterMode) {
            FilterMode.BLACKLIST -> !packages.contains(packageName)
            FilterMode.WHITELIST -> packages.contains(packageName)
        }
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        val current = HashSet(selectedPackages)
        when (filterMode) {
            FilterMode.BLACKLIST -> {
                // In blacklist mode, enabled == NOT in blacklist
                if (enabled) {
                    current.remove(packageName)
                } else {
                    current.add(packageName)
                }
            }
            FilterMode.WHITELIST -> {
                // In whitelist mode, enabled == in whitelist
                if (enabled) {
                    current.add(packageName)
                } else {
                    current.remove(packageName)
                }
            }
        }
        selectedPackages = current
    }

    fun isAppSelected(packageName: String): Boolean {
        return when (filterMode) {
            FilterMode.BLACKLIST -> !selectedPackages.contains(packageName)
            FilterMode.WHITELIST -> selectedPackages.contains(packageName)
        }
    }

    companion object {
        private const val PREFS_NAME = "antigravity_prefs"
        private const val KEY_FORWARDING_ENABLED = "key_forwarding_enabled"
        private const val KEY_WEBHOOK_URL = "key_webhook_url"
        private const val KEY_ENCRYPTION_KEY = "key_encryption_key"
        private const val KEY_FILTER_MODE = "key_filter_mode"
        private const val KEY_SELECTED_PACKAGES = "key_selected_packages"

        const val DEFAULT_WEBHOOK_URL = "https://ntfy.sh/my-secret-device-sync"

        val SYSTEM_NOISE_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.gms",
            "com.android.providers.downloads"
        )
    }
}
