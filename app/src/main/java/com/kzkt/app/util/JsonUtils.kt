package com.kzkt.app.util

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/**
 * JSON utilities — parsing and sanitizing LLM responses.
 * Ported from the original Python image service bersihkan_json_dari_gemini()
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

    /**
     * Parse an LLM translation response into a map of bubble-id → text.
     *
     * Prefers a strict parse (Gson). If the AI emitted duplicate keys (e.g. the same
     * bubble id twice), strict Gson fails and the whole batch would be lost — so we
     * fall back to a tolerant token scan that keeps the FIRST value for each key and
     * drops the rest, so one bad key can't sink 50 bubbles.
     */
    fun parseTranslationMap(cleanedJson: String): Map<String, String> {
        try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            val strict = GsonBuilder().setLenient().create().fromJson<Map<String, String>>(cleanedJson, type)
            return strict ?: emptyMap()
        } catch (_: Exception) {
            // duplicate keys (or similar) → tolerant re-parse
        }
        return scanDuplicateTolerant(cleanedJson)
    }

    private fun scanDuplicateTolerant(json: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val reader = JsonReader(StringReader(json))
        reader.isLenient = true
        try {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) return result
            reader.beginObject()
            while (reader.hasNext()) {
                val key = reader.nextName()
                if (result.containsKey(key)) {
                    // Duplicate id — skip its value, keep the first occurrence.
                    reader.skipValue()
                    continue
                }
                when (reader.peek()) {
                    JsonToken.STRING -> result[key] = reader.nextString()
                    JsonToken.NUMBER -> result[key] = reader.nextString()
                    JsonToken.BOOLEAN -> result[key] = reader.nextBoolean().toString()
                    else -> reader.skipValue() // null, nested object/array — not a text we can render
                }
            }
            reader.endObject()
        } catch (_: Exception) {
            // Malformed LLM output: salvage whatever complete pairs we got before the failure.
        }
        return result
    }
}
