package com.antigravity.droidconnect.network

import android.util.Log
import com.antigravity.droidconnect.crypto.CryptoManager
import com.antigravity.droidconnect.model.NotificationPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WebhookClient {

    private const val TAG = "WebhookClient"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Sends a notification payload to the configured webhook endpoint asynchronously.
     */
    suspend fun sendNotification(
        endpointUrl: String,
        payload: NotificationPayload,
        encryptionKey: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var url = endpointUrl.trim()
            if (url.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Webhook URL is empty"))
            }

            if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                url = "https://$url"
            }

            val isBark = url.contains("api.day.app", ignoreCase = true)
            val (targetUrl, jsonBody) = if (isBark) {
                Pair(url, buildBarkJson(payload, encryptionKey))
            } else {
                try {
                    val uri = java.net.URI(url)
                    val path = uri.path?.trim('/') ?: ""
                    if (path.isNotEmpty() && !path.contains("/")) {
                        val serverRoot = "${uri.scheme}://${uri.authority}"
                        Pair(serverRoot, buildNtfyJson(payload, encryptionKey, topic = path))
                    } else {
                        Pair(url, buildNtfyJson(payload, encryptionKey, topic = ""))
                    }
                } catch (e: Exception) {
                    Pair(url, buildNtfyJson(payload, encryptionKey, topic = ""))
                }
            }

            val requestBody = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .post(requestBody)

            if (encryptionKey.isNotBlank()) {
                requestBuilder.header("X-Encrypted", "AES-256-GCM")
            }

            val request = requestBuilder.build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Notification forwarded successfully: HTTP ${response.code}")
                    Result.success(Unit)
                } else {
                    val errorMsg = "HTTP error ${response.code}: ${response.message}"
                    Log.e(TAG, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send webhook: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Formats JSON payload adhering to ntfy.sh standard.
     */
    private fun buildNtfyJson(
        payload: NotificationPayload,
        encryptionKey: String,
        topic: String = ""
    ): JSONObject {
        var displayTitle = "[${payload.appName}] ${payload.title}".trim()
        var displayMessage = payload.message

        if (encryptionKey.isNotBlank()) {
            displayTitle = CryptoManager.encrypt(displayTitle, encryptionKey)
            displayMessage = CryptoManager.encrypt(displayMessage, encryptionKey)
        }

        val json = JSONObject()
        if (topic.isNotBlank()) {
            json.put("topic", topic)
        }
        json.put("title", displayTitle)
        json.put("message", displayMessage)
        json.put("priority", payload.priority)

        // Attach custom app icon URL if available
        if (!payload.iconUrl.isNullOrBlank()) {
            json.put("icon", payload.iconUrl)
        }

        val tagsArray = JSONArray()
        if (payload.tags.isEmpty()) {
            tagsArray.put("bell")
        } else {
            payload.tags.forEach { tagsArray.put(it) }
        }
        json.put("tags", tagsArray)

        return json
    }

    /**
     * Formats JSON payload for Bark server standard.
     */
    private fun buildBarkJson(payload: NotificationPayload, encryptionKey: String): JSONObject {
        var displayTitle = "[${payload.appName}] ${payload.title}".trim()
        var displayBody = payload.message

        if (encryptionKey.isNotBlank()) {
            displayTitle = CryptoManager.encrypt(displayTitle, encryptionKey)
            displayBody = CryptoManager.encrypt(displayBody, encryptionKey)
        }

        val json = JSONObject()
        json.put("title", displayTitle)
        json.put("body", displayBody)
        json.put("group", payload.appName)
        if (!payload.iconUrl.isNullOrBlank()) {
            json.put("icon", payload.iconUrl)
        }
        return json
    }
}
