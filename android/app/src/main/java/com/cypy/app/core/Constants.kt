package com.cypy.app.core

/**
 * Translation prompt template and constants.
 * Ported from cypy/core/translator.py translate_mosaic() prompt.
 */
object Constants {
    // ── Example translations per language ──────────────────────────
    val TRANSLATION_EXAMPLES: Map<String, Pair<String, String>> = mapOf(
        "english" to Pair("Hello!", "Mother... wait..."),
        "indonesian" to Pair("Cepat bangun!", "Ibu... tunggu..."),
        "japanese" to Pair("早く起きて！", "お母さん…待って…"),
        "mandarin" to Pair("快点起床！", "妈妈……等等……"),
        "spanish" to Pair("¡Despierta rápido!", "Madre... espera..."),
        "portuguese" to Pair("Acorde rápido!", "Mãe... espere..."),
        "javanese" to Pair("Ndang tangi!", "Ibu... enteni..."),
    )

    // ── Model decryption ───────────────────────────────────────────
    const val MODEL_DECRYPT_KEY_STR = "indravoyager"
    val MODEL_DECRYPT_KEY: Int get() = MODEL_DECRYPT_KEY_STR.length * 7 + 6  // = 90

    // ── YOLO settings ──────────────────────────────────────────────
    const val YOLO_INPUT_SIZE: Int = 640
    val YOLO_PREDICTION_STAGES: List<Pair<Double, Double>> = listOf(
        Pair(0.28, 0.45),
        Pair(0.18, 0.55),
        Pair(0.10, 0.65),
    )

    // ── Prompt template (populated at runtime) ─────────────────────
    fun buildPrompt(targetLanguage: String): String {
        val langKey = targetLanguage.lowercase().trim()
        val examples = TRANSLATION_EXAMPLES[langKey]
            ?: TRANSLATION_EXAMPLES["english"]!!
        val (exampleVal1, exampleVal3) = examples

        return buildString {
            appendLine("You are an accurate, literal manga translator from its original language to $targetLanguage. ")
            appendLine("The image contains several speech bubbles arranged vertically. ")
            appendLine("Each bubble is prefixed with a LARGE RED NUMBER on its left as its ID.")
            appendLine()
            appendLine("MAIN TASK:")
            appendLine("Read the text in each bubble, then translate it into $targetLanguage, faithfully preserving the original meaning.")
            appendLine()
            appendLine("VERTICAL READING RULES:")
            appendLine("1. Read vertical text from top to bottom.")
            appendLine("2. If there are multiple vertical columns, read the rightmost column first, then move left.")
            appendLine("3. Do not reverse column orders.")
            appendLine("4. Do not mix text between bubbles.")
            appendLine()
            appendLine("TRANSLATION RULES:")
            appendLine("1. Translate literally and accurately. Do not make it overly polite, do not summarize, and do not invent content.")
            appendLine("2. Do not add subjects or objects not present in the original text.")
            appendLine("3. Do not alter the relationships between characters.")
            appendLine("4. If the text is rude, explicit, teasing, degrading, bashful, or begging, maintain that exact tone.")
            appendLine("5. If the text contains a question, the $targetLanguage output must also be a question.")
            appendLine("6. Do not create new sentences that sound unnatural if they are not in the original text.")
            appendLine("7. For long sentences, keep all parts of the meaning. Do not truncate.")
            appendLine("8. If unsure about some text, use [?] for that part.")
            appendLine("9. If the bubble only contains SFX, scribbles, is empty, or is background art and not a meaningful dialogue, reply with 'SKIP'.")
            appendLine()
            appendLine("HONORIFICS RULE:")
            appendLine("1. If the original text contains Japanese honorifics (san, kun, chan, sama, senpai, sensei, etc.), keep them as-is in the translation. Do NOT translate honorifics.")
            appendLine("2. Examples: -san stays as -san, -kun stays as -kun, -chan stays as -chan.")
            appendLine("3. This applies even when translating to non-Japanese languages.")
            appendLine()
            appendLine("SFX RULE:")
            appendLine("1. If a bubble contains ONLY sound effects (SFX) with no dialogue, reply with 'SKIP'.")
            appendLine("2. SFX examples: ドドド, ゴゴゴ, バキ, ギュウ, キラキラ, etc.")
            appendLine("3. If a bubble has BOTH dialogue and SFX, translate only the dialogue part.")
            appendLine()
            appendLine("RETURN ALL IDs RULE:")
            appendLine("1. You MUST return a JSON entry for EVERY red ID number visible in the image.")
            appendLine("2. Do NOT skip any ID numbers. If ID 1, 2, 3, 4, 5 are visible, your JSON must contain all 5 keys.")
            appendLine("3. For IDs you cannot read or translate, use 'SKIP' as the value.")
            appendLine("4. This is critical - missing IDs will cause errors.")
            appendLine()
            appendLine("OUTPUT FORMAT:")
            appendLine("Provide the response ONLY in valid JSON without markdown formatting.")
            appendLine("Keys must be the red ID numbers as strings.")
            appendLine("Values must be the $targetLanguage translation or 'SKIP'.")
            append("Example output: {\"1\": \"$exampleVal1\", \"2\": \"SKIP\", \"3\": \"$exampleVal3\", \"4\": \"SKIP\", \"5\": \"$exampleVal1\"}")
        }
    }
}
