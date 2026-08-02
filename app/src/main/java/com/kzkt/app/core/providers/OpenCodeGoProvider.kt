package com.kzkt.app.core.providers

import android.graphics.Bitmap
import com.kzkt.app.core.ImageProcessor
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenCode Go provider (opencode.ai) — OpenAI-compatible with API key.
 * Ported from the original Python OpenCodeGo provider
 */
class OpenCodeGoProvider(
    override val apiKey: String,
    override val modelName: String,
) : LlmProvider {

    override val providerName: String = "OpenCode Go"
    private val baseUrl = "https://opencode.ai/zen/go/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override suspend fun translateImage(image: Bitmap, prompt: String): String? {
        val dataUri = ImageProcessor.bitmapToBase64DataUri(image)

        val payload = mapOf(
            "model" to modelName,
            "temperature" to 0,
            "top_p" to 0.1,
            "messages" to listOf(mapOf(
                "role" to "user",
                "content" to listOf(
                    mapOf("type" to "image_url", "image_url" to mapOf("url" to dataUri)),
                    mapOf("type" to "text", "text" to prompt)
                )
            ))
        )

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.code == 401) throw ValueError("API_KEY_ERROR")
                if (!response.isSuccessful) {
                    val detail = try {
                        JsonParser.parseString(body).asJsonObject
                            .getAsJsonObject("error")
                            .get("message")?.asString ?: body.take(200)
                    } catch (_: Exception) { body.take(200) }
                    throw RuntimeException("OpenCode Go API error ${response.code}: $detail")
                }

                val json = JsonParser.parseString(body).asJsonObject
                val choices = json.getAsJsonArray("choices")
                if (choices != null && choices.size() > 0) {
                    return@withContext choices[0].asJsonObject
                        .getAsJsonObject("message")
                        .get("content")?.asString
                }
                body
            } catch (e: java.io.IOException) {
                throw RuntimeException("OpenCode Go network error: ${e.message}")
            }
        }
    }

    class ValueError(message: String) : Exception(message)
}
