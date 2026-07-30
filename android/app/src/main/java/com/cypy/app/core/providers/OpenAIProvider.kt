package com.cypy.app.core.providers

import android.graphics.Bitmap
import com.cypy.app.core.Config.REQUEST_TIMEOUT_MS
import com.cypy.app.core.ImageProcessor
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenAI provider — REST API.
 * Ported from cypy/core/providers/openai.py
 */
class OpenAIProvider(
    override val apiKey: String,
    override val modelName: String,
) : LlmProvider {

    override val providerName: String = "OpenAI"
    private val baseUrl = "https://api.openai.com/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    private val gson = Gson()

    override fun validateApiKey(): Boolean = apiKey.isNotBlank()

    override suspend fun translateImage(image: Bitmap, prompt: String): String? {
        val dataUri = ImageProcessor.bitmapToBase64DataUri(image)

        val payload = mapOf(
            "model" to modelName,
            "temperature" to 0,
            "top_p" to 0.1,
            "response_format" to mapOf("type" to "json_object"),
            "messages" to listOf(mapOf(
                "role" to "user",
                "content" to listOf(
                    mapOf("type" to "image_url", "image_url" to mapOf("url" to dataUri, "detail" to "high")),
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

        return executeRequest(request)
    }

    protected suspend fun executeRequest(request: Request): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.code == 401) throw ValueError("API_KEY_ERROR")
                if (!response.isSuccessful) {
                    throw RuntimeException("${providerName} API error ${response.code}: ${body.take(200)}")
                }

                val json = JsonParser.parseString(body).asJsonObject
                val choices = json.getAsJsonArray("choices")
                if (choices != null && choices.size() > 0) {
                    return@withContext choices[0].asJsonObject
                        .getAsJsonObject("message")
                        .get("content")?.asString
                }
                body
            } catch (e: IOException) {
                throw RuntimeException("${providerName} network error: ${e.message}")
            }
        }
    }

    class ValueError(message: String) : Exception(message)
}
