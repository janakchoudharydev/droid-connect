package com.antigravity.droidconnect.network

object AppIconHelper {

    data class AppVisuals(
        val iconUrl: String? = null,
        val emojiTag: String = "bell"
    )

    fun resolveAppVisuals(packageName: String, appName: String): AppVisuals {
        val lowerPkg = packageName.lowercase()
        val lowerName = appName.lowercase()

        // 1. Check exact / known package matches
        val curated = CURATED_APPS[lowerPkg]
        if (curated != null) {
            return curated
        }

        // 2. Fuzzy heuristic matching by package or app name
        val emoji = when {
            lowerPkg.contains("whatsapp") || lowerName.contains("whatsapp") -> "speech_balloon"
            lowerPkg.contains("telegram") || lowerName.contains("telegram") -> "airplane"
            lowerPkg.contains("signal") || lowerName.contains("signal") -> "speech_balloon"
            lowerPkg.contains("discord") || lowerName.contains("discord") -> "speech_balloon"
            lowerPkg.contains("slack") || lowerName.contains("slack") -> "speech_balloon"
            lowerPkg.contains("message") || lowerPkg.contains("mms") || lowerPkg.contains("sms") || lowerName.contains("message") -> "speech_balloon"
            lowerPkg.contains("mail") || lowerPkg.contains("email") || lowerName.contains("mail") -> "envelope"
            lowerPkg.contains("dialer") || lowerPkg.contains("phone") || lowerPkg.contains("call") || lowerName.contains("phone") -> "phone"
            lowerPkg.contains("instagram") || lowerPkg.contains("camera") || lowerName.contains("photo") -> "camera"
            lowerPkg.contains("music") || lowerPkg.contains("spotify") || lowerPkg.contains("audio") || lowerPkg.contains("sound") -> "musical_note"
            lowerPkg.contains("youtube") || lowerPkg.contains("video") || lowerPkg.contains("netflix") -> "arrow_forward"
            lowerPkg.contains("bank") || lowerPkg.contains("pay") || lowerPkg.contains("paisa") || lowerPkg.contains("wallet") -> "credit_card"
            lowerPkg.contains("calendar") || lowerName.contains("calendar") -> "calendar"
            lowerPkg.contains("map") || lowerPkg.contains("navigation") || lowerPkg.contains("gps") -> "round_pushpin"
            lowerPkg.contains("browser") || lowerPkg.contains("chrome") || lowerPkg.contains("firefox") -> "globe_with_meridians"
            lowerPkg.contains("shop") || lowerPkg.contains("store") || lowerPkg.contains("cart") || lowerPkg.contains("food") -> "package"
            else -> "bell"
        }

        return AppVisuals(iconUrl = null, emojiTag = emoji)
    }

    private val CURATED_APPS = mapOf(
        // WhatsApp
        "com.whatsapp" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/whatsapp.png",
            emojiTag = "speech_balloon"
        ),
        "com.whatsapp.w4b" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/whatsapp.png",
            emojiTag = "speech_balloon"
        ),

        // Telegram
        "org.telegram.messenger" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/telegram.png",
            emojiTag = "airplane"
        ),
        "org.thunderdog.chatterino" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/telegram.png",
            emojiTag = "airplane"
        ),

        // Google Apps
        "com.google.android.googlequicksearchbox" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/google.png",
            emojiTag = "globe_with_meridians"
        ),
        "com.google.android.gm" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/gmail.png",
            emojiTag = "envelope"
        ),
        "com.google.android.apps.messaging" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/google-messages.png",
            emojiTag = "speech_balloon"
        ),
        "com.google.android.dialer" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/phone.png",
            emojiTag = "phone"
        ),
        "com.google.android.apps.maps" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/google-maps.png",
            emojiTag = "round_pushpin"
        ),
        "com.google.android.youtube" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/youtube.png",
            emojiTag = "arrow_forward"
        ),
        "com.google.android.apps.photos" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/google-photos.png",
            emojiTag = "camera"
        ),
        "com.google.android.apps.docs" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/google-drive.png",
            emojiTag = "page_facing_up"
        ),

        // Social / Collaboration
        "com.instagram.android" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/instagram.png",
            emojiTag = "camera"
        ),
        "com.twitter.android" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/twitter.png",
            emojiTag = "bird"
        ),
        "com.slack" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/slack.png",
            emojiTag = "speech_balloon"
        ),
        "com.discord" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/discord.png",
            emojiTag = "speech_balloon"
        ),
        "com.linkedin.android" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/linkedin.png",
            emojiTag = "briefcase"
        ),
        "com.spotify.music" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/spotify.png",
            emojiTag = "musical_note"
        ),

        // Microsoft
        "com.microsoft.office.outlook" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/outlook.png",
            emojiTag = "envelope"
        ),
        "com.microsoft.teams" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/microsoft-teams.png",
            emojiTag = "speech_balloon"
        ),

        // Payment / Delivery
        "com.google.android.apps.nbu.paisa.user" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/google-pay.png",
            emojiTag = "credit_card"
        ),
        "net.one97.paytm" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/paytm.png",
            emojiTag = "credit_card"
        ),
        "com.ubercab" to AppVisuals(
            iconUrl = "https://cdn.jsdelivr.net/gh/walkxcode/dashboard-icons/png/uber.png",
            emojiTag = "taxi"
        )
    )
}
