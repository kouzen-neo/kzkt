package com.kzkt.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kzkt_settings")

/**
 * Settings persistence via DataStore Preferences.
 * Ported from the original Python JSON settings approach
 */
class SettingsRepository(private val context: Context) {

    companion object {
        // Provider
        private val KEY_PROVIDER = stringPreferencesKey("llm_provider")
        private val KEY_LANGUAGE = stringPreferencesKey("target_language")
        private val KEY_CUSTOM_BASE_URL = stringPreferencesKey("custom_base_url")

        // API Keys
        private val KEY_GEMINI_KEY = stringPreferencesKey("gemini_api_key")
        private val KEY_OPENAI_KEY = stringPreferencesKey("openai_api_key")
        private val KEY_OPENROUTER_KEY = stringPreferencesKey("openrouter_api_key")
        private val KEY_ZEN_KEY = stringPreferencesKey("zen_api_key")
        private val KEY_OPENCODEGO_KEY = stringPreferencesKey("opencodego_api_key")
        private val KEY_CUSTOM_KEY = stringPreferencesKey("custom_api_key")

        // Models
        private val KEY_MODEL_GEMINI = stringPreferencesKey("model_gemini")
        private val KEY_MODEL_OPENAI = stringPreferencesKey("model_openai")
        private val KEY_MODEL_OPENROUTER = stringPreferencesKey("model_openrouter")
        private val KEY_MODEL_ZEN = stringPreferencesKey("model_zen")
        private val KEY_MODEL_OPENCODEGO = stringPreferencesKey("model_opencodego")
        private val KEY_MODEL_CUSTOM = stringPreferencesKey("model_custom")

        // Tweak params
        private val KEY_MAX_BUBBLES = intPreferencesKey("max_bubbles_per_request")
        private val KEY_REQUEST_DELAY = floatPreferencesKey("min_request_delay")
        private val KEY_SFX_MODE = stringPreferencesKey("filter_sfx_mode")
        private val KEY_PAD_X = floatPreferencesKey("pad_x_ratio")
        private val KEY_PAD_Y = floatPreferencesKey("pad_y_ratio")
        private val KEY_MIN_PAD = intPreferencesKey("min_pad")
        private val KEY_LAST_DIR = stringPreferencesKey("last_opened_dir")
        private val KEY_CUSTOM_FONT = stringPreferencesKey("custom_font_path")
        private val KEY_USE_INPAINTING = booleanPreferencesKey("use_inpainting")
    }

    data class Settings(
        val llmProvider: String = "gemini",
        val targetLanguage: String = "Indonesian",
        val customBaseUrl: String = "",
        val geminiApiKey: String = "",
        val openaiApiKey: String = "",
        val openrouterApiKey: String = "",
        val zenApiKey: String = "",
        val opencodegoApiKey: String = "",
        val customApiKey: String = "",
        val modelGemini: String = "gemini-3.1-flash-lite",
        val modelOpenai: String = "gpt-5.4-mini",
        val modelOpenrouter: String = "qwen/qwen2.5-vl-72b-instruct:free",
        val modelZen: String = "minimax-m3-free",
        val modelOpencodego: String = "mimo-v2.5",
        val modelCustom: String = "gpt-5.4-mini",
        val maxBubblesPerRequest: Int = 20,
        val minRequestDelay: Float = 2.0f,
        val filterSfxMode: String = "balanced",
        val padXRatio: Float = 0.40f,
        val padYRatio: Float = 0.25f,
        val minPad: Int = 35,
        val lastOpenedDir: String = "",
        val customFontPath: String = "",
        val useInpainting: Boolean = false,
    )

    private object Defaults {
        val settings = Settings()
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            llmProvider = prefs[KEY_PROVIDER] ?: Defaults.settings.llmProvider,
            targetLanguage = prefs[KEY_LANGUAGE] ?: Defaults.settings.targetLanguage,
            customBaseUrl = prefs[KEY_CUSTOM_BASE_URL] ?: Defaults.settings.customBaseUrl,
            geminiApiKey = prefs[KEY_GEMINI_KEY] ?: Defaults.settings.geminiApiKey,
            openaiApiKey = prefs[KEY_OPENAI_KEY] ?: Defaults.settings.openaiApiKey,
            openrouterApiKey = prefs[KEY_OPENROUTER_KEY] ?: Defaults.settings.openrouterApiKey,
            zenApiKey = prefs[KEY_ZEN_KEY] ?: Defaults.settings.zenApiKey,
            opencodegoApiKey = prefs[KEY_OPENCODEGO_KEY] ?: Defaults.settings.opencodegoApiKey,
            customApiKey = prefs[KEY_CUSTOM_KEY] ?: Defaults.settings.customApiKey,
            modelGemini = prefs[KEY_MODEL_GEMINI] ?: Defaults.settings.modelGemini,
            modelOpenai = prefs[KEY_MODEL_OPENAI] ?: Defaults.settings.modelOpenai,
            modelOpenrouter = prefs[KEY_MODEL_OPENROUTER] ?: Defaults.settings.modelOpenrouter,
            modelZen = prefs[KEY_MODEL_ZEN] ?: Defaults.settings.modelZen,
            modelOpencodego = prefs[KEY_MODEL_OPENCODEGO] ?: Defaults.settings.modelOpencodego,
            modelCustom = prefs[KEY_MODEL_CUSTOM] ?: Defaults.settings.modelCustom,
            maxBubblesPerRequest = prefs[KEY_MAX_BUBBLES] ?: Defaults.settings.maxBubblesPerRequest,
            minRequestDelay = prefs[KEY_REQUEST_DELAY] ?: Defaults.settings.minRequestDelay,
            filterSfxMode = prefs[KEY_SFX_MODE] ?: Defaults.settings.filterSfxMode,
            padXRatio = prefs[KEY_PAD_X] ?: Defaults.settings.padXRatio,
            padYRatio = prefs[KEY_PAD_Y] ?: Defaults.settings.padYRatio,
            minPad = prefs[KEY_MIN_PAD] ?: Defaults.settings.minPad,
            lastOpenedDir = prefs[KEY_LAST_DIR] ?: Defaults.settings.lastOpenedDir,
            customFontPath = prefs[KEY_CUSTOM_FONT] ?: Defaults.settings.customFontPath,
            useInpainting = prefs[KEY_USE_INPAINTING] ?: Defaults.settings.useInpainting,
        )
    }

    suspend fun saveProvider(provider: String) {
        context.dataStore.edit { it[KEY_PROVIDER] = provider }
    }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun saveApiKey(providerName: String, key: String) {
        context.dataStore.edit { prefs ->
            val keyPref = when (providerName) {
                "gemini" -> KEY_GEMINI_KEY
                "openai" -> KEY_OPENAI_KEY
                "openrouter" -> KEY_OPENROUTER_KEY
                "zen" -> KEY_ZEN_KEY
                "opencodego" -> KEY_OPENCODEGO_KEY
                "custom" -> KEY_CUSTOM_KEY
                else -> return@edit
            }
            prefs[keyPref] = key
        }
    }

    suspend fun saveModel(providerName: String, model: String) {
        context.dataStore.edit { prefs ->
            val key = when (providerName) {
                "gemini" -> KEY_MODEL_GEMINI
                "openai" -> KEY_MODEL_OPENAI
                "openrouter" -> KEY_MODEL_OPENROUTER
                "zen" -> KEY_MODEL_ZEN
                "opencodego" -> KEY_MODEL_OPENCODEGO
                "custom" -> KEY_MODEL_CUSTOM
                else -> return@edit
            }
            prefs[key] = model
        }
    }

    suspend fun saveCustomBaseUrl(url: String) {
        context.dataStore.edit { it[KEY_CUSTOM_BASE_URL] = url }
    }

    suspend fun saveCustomFontPath(path: String) {
        context.dataStore.edit { it[KEY_CUSTOM_FONT] = path }
    }

    suspend fun saveUseInpainting(enabled: Boolean) {
        context.dataStore.edit { it[KEY_USE_INPAINTING] = enabled }
    }

    suspend fun saveTweakParam(keyField: String, value: Any) {
        context.dataStore.edit { prefs ->
            when (keyField) {
                "max_bubbles" -> if (value is Int) prefs[KEY_MAX_BUBBLES] = value
                "request_delay" -> if (value is Float) prefs[KEY_REQUEST_DELAY] = value
                "sfx_mode" -> if (value is String) prefs[KEY_SFX_MODE] = value
                "pad_x" -> if (value is Float) prefs[KEY_PAD_X] = value
                "pad_y" -> if (value is Float) prefs[KEY_PAD_Y] = value
                "min_pad" -> if (value is Int) prefs[KEY_MIN_PAD] = value
                "use_inpainting" -> if (value is Boolean) prefs[KEY_USE_INPAINTING] = value
            }
        }
    }

    suspend fun saveLastDir(dir: String) {
        context.dataStore.edit { it[KEY_LAST_DIR] = dir }
    }
}
