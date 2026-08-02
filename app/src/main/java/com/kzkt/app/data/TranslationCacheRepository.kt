package com.kzkt.app.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * Translation Memory: persistent local cache for crop translations based on MD5 crop hashes.
 * Allows 100% free & instant re-translation of identical speech bubbles.
 */
class TranslationCacheRepository(private val context: Context) {

    private val cacheFile: File
        get() = File(context.filesDir, "translation_cache.json")

    private val memoryCache = mutableMapOf<String, String>()

    init {
        loadCache()
    }

    @Synchronized
    private fun loadCache() {
        if (!cacheFile.exists()) return
        try {
            val jsonStr = cacheFile.readText()
            val json = JSONObject(jsonStr)
            for (key in json.keys()) {
                memoryCache[key] = json.getString(key)
            }
        } catch (e: Exception) {
            Log.w("KZKT", "Failed to load translation cache: ${e.message}")
        }
    }

    @Synchronized
    private fun saveCache() {
        try {
            val json = JSONObject()
            for ((k, v) in memoryCache) {
                json.put(k, v)
            }
            cacheFile.writeText(json.toString())
        } catch (e: Exception) {
            Log.w("KZKT", "Failed to save translation cache: ${e.message}")
        }
    }

    fun computeHash(bitmap: Bitmap): String {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(bytes)
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    @Synchronized
    fun getTranslation(cropBitmap: Bitmap, targetLanguage: String): String? {
        val hash = computeHash(cropBitmap)
        if (hash.isBlank()) return null
        val key = "${hash}_${targetLanguage.lowercase()}"
        return memoryCache[key]
    }

    @Synchronized
    fun saveTranslation(cropBitmap: Bitmap, targetLanguage: String, translatedText: String) {
        if (translatedText.isBlank() || translatedText.uppercase() == "SKIP") return
        val hash = computeHash(cropBitmap)
        if (hash.isBlank()) return
        val key = "${hash}_${targetLanguage.lowercase()}"
        memoryCache[key] = translatedText
        saveCache()
    }
}
