package com.antigravity.droidconnect.deliverer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LocalHttpDeliverer(private val context: Context) : NotificationDeliverer {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    override suspend fun deliver(
        packageName: String,
        appName: String,
        title: String,
        text: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val serverEndpoint = prefs.getString(KEY_SERVER_ENDPOINT, DEFAULT_ENDPOINT)?.trim().orEmpty()

            if (serverEndpoint.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Server endpoint not configured"))
            }

            val formattedUrl = if (serverEndpoint.startsWith("http://") || serverEndpoint.startsWith("https://")) {
                serverEndpoint.removeSuffix("/")
            } else {
                "http://$serverEndpoint".removeSuffix("/")
            }

            val targetUrl = if (formattedUrl.endsWith("/notify")) formattedUrl else "$formattedUrl/notify"

            val json = JSONObject().apply {
                put("packageName", packageName)
                put("appName", appName)
                put("title", title)
                put("text", text)
                put("timestamp", System.currentTimeMillis())
            }

            val body = json.toString().toRequestBody(JSON_TYPE)
            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully delivered notification to Mac: HTTP ${response.code}")
                    Result.success(Unit)
                } else {
                    val error = "Mac receiver returned HTTP ${response.code}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deliver notification over local Wi-Fi: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "LocalHttpDeliverer"
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
        const val PREFS_NAME = "antigravity_config"
        const val KEY_SERVER_ENDPOINT = "key_mac_server_endpoint"
        const val KEY_FORWARDING_ACTIVE = "key_forwarding_active"
        const val DEFAULT_ENDPOINT = "192.168.0.106:3000"
    }
}
