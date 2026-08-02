package com.kzkt.app.core

/**
 * Configuration constants and provider registry.
 * Ported from the original Python config module
 */
object Config {
    // ── Provider Metadata ──────────────────────────────────────────
    data class ProviderMeta(
        val key: String,
        val displayName: String,
        val envKey: String,
        val modelEnvKey: String,
        val defaultModel: String,
        val url: String = "",
        val description: String = "",
        val requiresKey: Boolean = true,
    )

    val PROVIDER_REGISTRY: Map<String, ProviderMeta> = mapOf(
        "gemini" to ProviderMeta(
            key = "gemini", displayName = "Google Gemini",
            envKey = "GEMINI_API_KEY", modelEnvKey = "MODEL_GEMINI",
            defaultModel = "gemini-3.1-flash-lite",
            url = "https://aistudio.google.com/",
            description = "Free tier available",
        ),
        "openai" to ProviderMeta(
            key = "openai", displayName = "OpenAI",
            envKey = "OPENAI_API_KEY", modelEnvKey = "MODEL_OPENAI",
            defaultModel = "gpt-5.4-mini",
            url = "https://platform.openai.com/api-keys",
            description = "GPT-5.4, GPT-5.4-mini",
        ),
        "zen" to ProviderMeta(
            key = "zen", displayName = "Zen (opencode.ai)",
            envKey = "ZEN_API_KEY", modelEnvKey = "MODEL_ZEN",
            defaultModel = "minimax-m3-free",
            url = "https://opencode.ai/auth",
            description = "Free models, optional API key for more quota",
            requiresKey = false,
        ),
        "opencodego" to ProviderMeta(
            key = "opencodego", displayName = "OpenCode Go",
            envKey = "OPENCODEGO_API_KEY", modelEnvKey = "MODEL_OPENCODEGO",
            defaultModel = "mimo-v2.5",
            url = "https://opencode.ai/auth",
            description = "API key required, high-performance models",
        ),
        "openrouter" to ProviderMeta(
            key = "openrouter", displayName = "OpenRouter",
            envKey = "OPENROUTER_API_KEY", modelEnvKey = "MODEL_OPENROUTER",
            defaultModel = "qwen/qwen2.5-vl-72b-instruct:free",
            url = "https://openrouter.ai/keys",
            description = "Access 100+ models (Claude, Llama, Mistral, etc.)",
        ),
        "custom" to ProviderMeta(
            key = "custom", displayName = "Custom",
            envKey = "CUSTOM_API_KEY", modelEnvKey = "MODEL_CUSTOM",
            defaultModel = "gpt-5.4-mini",
            url = "",
            description = "OpenAI-compatible API, custom base URL",
            requiresKey = false,
        ),
    )

    // ── Language ───────────────────────────────────────────────────
    val LANG_CODES: Map<String, String> = mapOf(
        "english" to "en", "indonesian" to "id", "spanish" to "es",
        "portuguese" to "pt", "javanese" to "jv", "japanese" to "jp",
        "jepang" to "jp", "korean" to "kr", "korea" to "kr",
        "chinese" to "cn", "chinese (simplified)" to "cn",
        "chinese (traditional)" to "tw", "mandarin" to "cn",
        "thai" to "th", "vietnamese" to "vi", "russian" to "ru",
        "arabic" to "ar", "hindi" to "hi", "malay" to "ms", "tagalog" to "tl",
    )

    val LANGUAGE_CHOICES: List<String> = listOf(
        "English", "Indonesian", "Japanese", "Mandarin", "Spanish",
        "French", "German", "Vietnamese", "Portuguese", "Javanese",
        "Korean", "Russian", "Arabic", "Thai",
    )

    // ── Tweakable Parameters ───────────────────────────────────────
    data class TweakParams(
        var maxBubblesPerRequest: Int = 20,
        var minRequestDelay: Double = 2.0,
        var filterSfxMode: String = "balanced",  // balanced | relaxed | strict
        var padXRatio: Double = 0.40,
        var padYRatio: Double = 0.25,
        var minPad: Int = 35,
        var skalaPotonganMosaik: Double = 2.0,
        var maxTinggiMosaik: Int = 6000,
        var overlapBatasCrop: Double = 0.35,
        var maskAreaLuarBox: Boolean = true,
        var maskMargin: Int = 18,
        var maskMarginRatio: Double = 0.12,
        var marginKiriNomor: Int = 55,
        var marginKanan: Int = 10,
        var jarakAntarPotongan: Int = 10,
        var lebarMosaikMin: Int = 360,
        var filterSfxAktif: Boolean = true,
        var simpanDebugFilterSfx: Boolean = true,
        var pakaiPatchUntukBoxGepeng: Boolean = true,
        var rasioBoxGepeng: Double = 2.4,
        var lebarBoxGepengRatio: Double = 0.45,
        var tinggiBoxGepengRatio: Double = 0.22,
        var useInpainting: Boolean = false,
        var customFontPath: String = "",
    )

    fun getLangCode(targetLanguage: String): String {
        val lang = targetLanguage.lowercase().trim()
        return LANG_CODES[lang] ?: lang.take(2)
    }

    // ── Preset Models per Provider ──────────────────────────────────
    val PRESET_MODELS: Map<String, List<String>> = mapOf(
        "gemini" to listOf("gemini-3.1-flash-lite", "gemini-2.5-flash", "gemini-2.5-pro"),
        "openai" to listOf("gpt-5.4-mini", "gpt-5.4", "gpt-4o-mini", "gpt-4o"),
        "openrouter" to listOf(
            "qwen/qwen2.5-vl-72b-instruct:free",
            "google/gemini-2.5-flash:free",
            "meta-llama/llama-3.2-11b-vision-instruct:free",
            "anthropic/claude-3.5-sonnet",
        ),
        "zen" to listOf("minimax-m3-free", "glm-4v-free", "qwen2.5-vl-free"),
        "opencodego" to listOf("mimo-v2.5", "claude-3-5-sonnet"),
    )

    val SUPPORTED_IMAGE_EXTENSIONS: Set<String> = setOf(".png", ".jpg", ".jpeg", ".webp")
    val REQUEST_TIMEOUT_MS: Long = 120_000L
}
