package com.cypy.app.core.providers

import android.graphics.Bitmap
import com.cypy.app.core.ImageProcessor
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Zen provider (opencode.ai) — OpenAI-compatible, no key required.
 * Ported from cypy/core/providers/zen.py
 */
class ZenProvider(
    override val apiKey: String,
    override val modelName: String,
) : LlmProvider {

    override val providerName: String = "Zen (opencode.ai)"
    private val baseUrl = "https://opencode.ai/zen/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override fun validateApiKey(): Boolean = true  // Zen works without API key

    override suspend fun translateImage(image: Bitmap, prompt: String): String? {
        val dataUri = ImageProcessor.bitmapToBase64DataUri(image)

        val headers = mutableMapOf("Content-Type" to "application/json")
        if (apiKey.isNotBlank()) headers["Authorization"] = "Bearer $apiKey"

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
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
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
                    throw RuntimeException("Zen API error ${response.code}: $detail")
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
                throw RuntimeException("Zen network error: ${e.message}")
            }
        }
    }

    class ValueError(message: String) : Exception(message)
}
