package com.kzkt.app.core.providers

import android.graphics.Bitmap
import com.kzkt.app.core.ImageProcessor
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.StringReader
import java.util.concurrent.TimeUnit

/**
 * Custom OpenAI-compatible provider with configurable base URL.
 * Ported from the original Python custom provider
 */
class CustomProvider(
    override val apiKey: String,
    override val modelName: String,
    var baseUrl: String = "",
) : LlmProvider {

    override val providerName: String = "Custom"

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override fun validateApiKey(): Boolean = true  // optional key

    override suspend fun translateImage(image: Bitmap, prompt: String): String? {
        if (baseUrl.isBlank()) {
            throw RuntimeException("Custom provider base URL is not configured.")
        }

        val dataUri = ImageProcessor.bitmapToBase64DataUri(image)
        // Normalize base URL: remove trailing /chat/completions, /v1, or /
        var normalized = baseUrl.trimEnd('/')
        if (normalized.endsWith("/chat/completions")) normalized = normalized.removeSuffix("/chat/completions")
        val endpoint = if (normalized.endsWith("/v1")) "$normalized/chat/completions"
            else "$normalized/v1/chat/completions"

        val headers = mutableMapOf("Content-Type" to "application/json")
        if (apiKey.isNotBlank()) headers["Authorization"] = "Bearer $apiKey"

        val payload = mapOf(
            "model" to modelName,
            "temperature" to 0,
            "top_p" to 0.1,
            "stream" to false,
            "messages" to listOf(mapOf(
                "role" to "user",
                "content" to listOf(
                    mapOf("type" to "image_url", "image_url" to mapOf("url" to dataUri)),
                    mapOf("type" to "text", "text" to prompt)
                )
            ))
        )

        val request = Request.Builder()
            .url(endpoint)
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.code in listOf(401, 402)) throw ValueError("API_KEY_ERROR")
                if (!response.isSuccessful) {
                    throw RuntimeException("Custom API error ${response.code}: ${body.take(200)}")
                }

                val lenientReader = JsonReader(StringReader(body)).apply { isLenient = true }
                val json = JsonParser.parseReader(lenientReader)

                if (!json.isJsonObject) {
                    return@withContext body
                }
                val jsonObj = json.asJsonObject

                // 1. Standard OpenAI format: choices[0].message.content
                if (jsonObj.has("choices") && jsonObj.get("choices").isJsonArray) {
                    val choices = jsonObj.getAsJsonArray("choices")
                    if (choices.size() > 0 && choices[0].isJsonObject) {
                        val choiceObj = choices[0].asJsonObject
                        if (choiceObj.has("message") && choiceObj.get("message").isJsonObject) {
                            val msgObj = choiceObj.getAsJsonObject("message")
                            if (msgObj.has("content") && !msgObj.get("content").isJsonNull) {
                                val contentElem = msgObj.get("content")
                                if (contentElem.isJsonPrimitive) return@withContext contentElem.asString
                                if (contentElem.isJsonArray) {
                                    return@withContext contentElem.asJsonArray
                                        .filter { it.isJsonObject && it.asJsonObject.has("text") }
                                        .joinToString("\n") { it.asJsonObject.get("text").asString }
                                }
                            }
                        } else if (choiceObj.has("text") && !choiceObj.get("text").isJsonNull) {
                            return@withContext choiceObj.get("text").asString
                        }
                    }
                }

                // 2. Ollama / Direct response format
                if (jsonObj.has("response") && !jsonObj.get("response").isJsonNull) {
                    return@withContext jsonObj.get("response").asString
                }
                if (jsonObj.has("message") && jsonObj.get("message").isJsonObject) {
                    val msg = jsonObj.getAsJsonObject("message")
                    if (msg.has("content") && !msg.get("content").isJsonNull) {
                        return@withContext msg.get("content").asString
                    }
                }

                body
            } catch (e: java.io.IOException) {
                throw RuntimeException("Custom network error: ${e.message}")
            }
        }
    }


    class ValueError(message: String) : Exception(message)
}
