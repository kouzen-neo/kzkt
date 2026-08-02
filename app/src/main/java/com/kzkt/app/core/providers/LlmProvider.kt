package com.kzkt.app.core.providers

import android.graphics.Bitmap

/**
 * Abstract interface for all LLM providers.
 * Ported from the original Python base provider
 */
interface LlmProvider {
    val providerName: String
    val apiKey: String
    val modelName: String

    /**
     * Send a translated mosaic image + prompt to the LLM and return raw response text.
     * Should return a JSON string.
     */
    suspend fun translateImage(image: Bitmap, prompt: String): String?

    fun validateApiKey(): Boolean = apiKey.isNotBlank()
}
