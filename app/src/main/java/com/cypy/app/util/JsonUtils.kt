package com.cypy.app.util

/**
 * JSON utilities — parsing and sanitizing LLM responses.
 * Ported from cypy/core/services/image_service.py bersihkan_json_dari_gemini()
 */
object JsonUtils {

    /**
     * Strip markdown code fences and extract the first JSON object from raw text.
     * Handles ```json ... ```, ``` ... ```, and plain JSON.
     */
    fun sanitizeJson(rawText: String): String {
        var text = rawText.trim()

        // Remove ```json prefix
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json").trim()
        }
        // Remove ``` prefix
        if (text.startsWith("```")) {
            text = text.removePrefix("```").trim()
        }
        // Remove ``` suffix
        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }

        // Extract first { ... } block
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            text = text.substring(start, end + 1)
        }

        return text.trim()
    }
}
