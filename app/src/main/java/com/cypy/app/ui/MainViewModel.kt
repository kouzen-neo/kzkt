package com.cypy.app.ui

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cypy.app.core.Config
import com.cypy.app.core.ImageProcessor
import com.cypy.app.core.RateLimiter
import com.cypy.app.core.TextRenderer
import com.cypy.app.core.TranslationPipeline
import com.cypy.app.core.YoloOnnx
import com.cypy.app.core.providers.*
import com.cypy.app.data.HistoryEntry
import com.cypy.app.data.HistoryRepository
import com.cypy.app.data.SettingsRepository
import com.cypy.app.util.PdfExporter
import com.cypy.app.util.PdfImporter
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepo = SettingsRepository(application)
    val historyRepo = HistoryRepository(application)

    // Observable state — all writes happen on the Main thread (see [post]).
    val settings = mutableStateOf(SettingsRepository.Settings())

    // File processing
    val selectedFiles = mutableStateListOf<String>()
    val translationLog = mutableStateListOf<String>()
    val translationActive = mutableStateOf(false)
    val translationProgress = mutableStateOf(0f)
    val translationTotal = mutableStateOf(0)
    val translationDone = mutableStateOf(0)

    // Result
    val resultPaths = mutableStateListOf<String>()
    val currentPreviewPath = mutableStateOf<String?>(null)

    // YOLO model state
    val yoloReady = mutableStateOf(false)
    val yoloError = mutableStateOf<String?>(null)

    // Custom provider model list (from /v1/models)
    val customModels = mutableStateListOf<String>()
    val customModelsLoading = mutableStateOf(false)

    // Cancel flag
    private var _cancelled = false

    private var yolo: YoloOnnx? = null
    private var textRenderer: TextRenderer? = null

    private var pipelineLaunched = false

    /**
     * Compose snapshot state is not thread-safe: it must only be written on the
     * Main thread. [post] marshals every state write from a background thread
     * onto the Main dispatcher (this was a source of UI jank — F1).
     */
    private fun post(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main.immediate) { block() }
    }

    init {
        // Load settings
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { s ->
                settings.value = s
            }
        }
    }

    fun initialize(context: android.content.Context) {
        if (pipelineLaunched) return
        pipelineLaunched = true

        viewModelScope.launch(Dispatchers.IO) {
            yolo = YoloOnnx(context)
            val ok = yolo!!.initialize()
            post {
                if (ok) {
                    yoloReady.value = true
                    translationLog.add("YOLO model loaded successfully")
                } else {
                    yoloError.value = "Failed to load YOLO model"
                    translationLog.add("[!] Failed to load YOLO model")
                }
            }
            textRenderer = TextRenderer(context)
        }
    }

    fun createProvider(): LlmProvider? {
        val s = settings.value
        val meta = Config.PROVIDER_REGISTRY[s.llmProvider] ?: return null

        val apiKey = when (s.llmProvider) {
            "gemini" -> s.geminiApiKey
            "openai" -> s.openaiApiKey
            "openrouter" -> s.openrouterApiKey
            "zen" -> s.zenApiKey
            "opencodego" -> s.opencodegoApiKey
            "custom" -> s.customApiKey
            else -> ""
        }
        val modelName = when (s.llmProvider) {
            "gemini" -> s.modelGemini
            "openai" -> s.modelOpenai
            "openrouter" -> s.modelOpenrouter
            "zen" -> s.modelZen
            "opencodego" -> s.modelOpencodego
            "custom" -> s.modelCustom
            else -> meta.defaultModel
        }

        return when (s.llmProvider) {
            "gemini" -> GeminiProvider(apiKey, modelName)
            "openai" -> OpenAIProvider(apiKey, modelName)
            "openrouter" -> OpenRouterProvider(apiKey, modelName)
            "zen" -> ZenProvider(apiKey, modelName)
            "opencodego" -> OpenCodeGoProvider(apiKey, modelName)
            "custom" -> CustomProvider(apiKey, modelName, s.customBaseUrl)
            else -> null
        }
    }

    private var translationJob: kotlinx.coroutines.Job? = null

    fun startTranslation() {
        if (translationActive.value) return
        val yolo = yolo ?: run {
            translationLog.add("[!] YOLO model is not ready.")
            return
        }
        val provider = createProvider() ?: run {
            translationLog.add("[!] Provider configuration is incomplete.")
            return
        }

        _cancelled = false
        translationActive.value = true
        translationLog.clear()
        resultPaths.clear()
        translationProgress.value = 0f
        translationTotal.value = selectedFiles.size
        translationDone.value = 0

        val filesToProcess = selectedFiles.toList()
        val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val cypyFolder = File(downloadFolder, "CYPY")
        cypyFolder.mkdirs()
        val outputDir = cypyFolder.absolutePath

        translationJob?.cancel()
        translationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val params = Config.TweakParams(
                    maxBubblesPerRequest = settings.value.maxBubblesPerRequest,
                    minRequestDelay = settings.value.minRequestDelay.toDouble(),
                    filterSfxMode = settings.value.filterSfxMode,
                    padXRatio = settings.value.padXRatio.toDouble(),
                    padYRatio = settings.value.padYRatio.toDouble(),
                    minPad = settings.value.minPad,
                )

                val pipeline = TranslationPipeline(
                    yolo = yolo,
                    provider = provider,
                    textRenderer = textRenderer!!,
                    params = params,
                    targetLanguage = settings.value.targetLanguage,
                    onProgress = { msg ->
                        post {
                            translationLog.add(msg)
                            if (msg.contains("Done!")) {
                                translationDone.value = translationDone.value + 1
                                translationProgress.value = translationDone.value.toFloat() / translationTotal.value
                            }
                        }
                    },
                    isCancelled = { _cancelled }
                )

                var completed = 0
                // extract + translate + reassemble for PDFs, 1 step for images
                val totalSteps = filesToProcess.fold(0) { acc, f ->
                    acc + if (f.endsWith(".pdf", ignoreCase = true)) 3 else 1
                }
                val tempDir = File(getApplication<Application>().cacheDir, "pdf_input")
                val translatedPages = mutableListOf<String>()

                for ((idx, path) in filesToProcess.withIndex()) {
                    if (_cancelled) {
                        post { translationLog.add("[Cancelled] Translation stopped by user.") }
                        break
                    }

                    if (path.endsWith(".pdf", ignoreCase = true)) {
                        // ── PDF: extract pages → batch translate → reassemble ──
                        val fileName = File(path).name
                        post { translationLog.add("[${idx + 1}/${filesToProcess.size}] Opening PDF $fileName...") }
                        val pdfFile = File(path)
                        val pages = PdfImporter.extractPdfToImages(pdfFile, tempDir)
                        if (pages.isEmpty()) {
                            post {
                                translationLog.add("[!] Could not read PDF: $fileName")
                                translationDone.value = ++completed
                                translationProgress.value = completed.toFloat() / totalSteps
                            }
                            continue
                        }
                        post { translationLog.add("  Extracted ${pages.size} pages from PDF.") }

                        val results = pipeline.processImageBatch(pages, outputDir)
                        val translated = results.mapNotNull { it.outputPath }
                        post { translationLog.add("  Translated ${translated.size}/${pages.size} pages.") }

                        val outputPdf = File(cypyFolder, "${pdfFile.nameWithoutExtension}.pdf")
                        PdfExporter.createPdfFromImages(translated, outputPdf)
                        if (outputPdf.exists()) {
                            post {
                                translationLog.add("  PDF saved: ${outputPdf.absolutePath}")
                                resultPaths.add(outputPdf.absolutePath)
                                currentPreviewPath.value = outputPdf.absolutePath
                            }
                            recordHistory(fileName, outputPdf.absolutePath, pages.size)
                        } else {
                            post { translationLog.add("[!] Failed to assemble PDF.") }
                        }

                        // Cleanup temporary page images
                        pages.forEach { File(it).delete() }
                        post {
                            translationDone.value = ++completed
                            translationProgress.value = completed.toFloat() / totalSteps
                        }
                    } else {
                        // ── Single image ──
                        val fileName = File(path).name
                        post { translationLog.add("[${idx + 1}/${filesToProcess.size}] Processing $fileName...") }
                        val result = pipeline.processSingleImage(path, outputDir)
                        if (result.outputPath != null) {
                            post {
                                resultPaths.add(result.outputPath)
                                currentPreviewPath.value = result.outputPath
                            }
                            recordHistory(fileName, result.outputPath, 1)
                        }
                        post {
                            translationDone.value = ++completed
                            translationProgress.value = completed.toFloat() / totalSteps
                        }
                    }
                }

                if (!_cancelled) {
                    post { translationLog.add("Translation complete.") }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                post { translationLog.add("[Cancelled] Translation stopped by user.") }
            } finally {
                post { translationActive.value = false }
            }
        }
    }

    fun cancelTranslation() {
        _cancelled = true
        translationJob?.cancel()
        translationJob = null
        translationActive.value = false
        if (!translationLog.lastOrNull().orEmpty().contains("[Cancelled]")) {
            translationLog.add("[Cancelled] Translation stopped by user.")
        }
    }

    fun addFiles(paths: List<String>) {
        selectedFiles.clear()
        selectedFiles.addAll(paths)
    }

    fun addLog(msg: String) {
        translationLog.add(msg)
    }

    /** Remove one entry from the Riwayat tab. */
    fun deleteHistoryEntry(timestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) { historyRepo.delete(timestamp) }
    }

    /** Persist one finished file (image or assembled PDF) into the Riwayat tab. */
    private fun recordHistory(fileName: String, outputPath: String, pageCount: Int) {
        val s = settings.value
        val entry = HistoryEntry(
            timestamp = System.currentTimeMillis(),
            fileName = fileName,
            outputPath = outputPath,
            pageCount = pageCount,
            provider = s.llmProvider,
            targetLanguage = s.targetLanguage,
        )
        viewModelScope.launch(Dispatchers.IO) { historyRepo.record(entry) }
    }

    override fun onCleared() {
        super.onCleared()
        yolo?.close()
    }

    // ── Custom provider model auto-detect ──

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun fetchCustomModels(baseUrl: String, apiKey: String) {
        if (baseUrl.isBlank()) return
        customModelsLoading.value = true
        customModels.clear()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Normalize: strip /chat/completions and /v1, then add /v1/models
                var normalized = baseUrl.trimEnd('/')
                if (normalized.endsWith("/chat/completions")) normalized = normalized.removeSuffix("/chat/completions")
                if (normalized.endsWith("/v1")) normalized = normalized.removeSuffix("/v1")
                val endpoint = "$normalized/v1/models"
                val request = Request.Builder().url(endpoint)
                if (apiKey.isNotBlank()) request.header("Authorization", "Bearer $apiKey")
                val response = httpClient.newCall(request.build()).execute()
                val body = response.body?.string() ?: ""

                val json = JsonParser.parseString(body).asJsonObject
                val data = json.getAsJsonArray("data")
                val models = mutableListOf<String>()
                if (data != null) {
                    for (elem in data) {
                        val id = elem.asJsonObject.get("id")?.asString
                        if (id != null) models.add(id)
                    }
                }
                // Fallback: some providers use "models" key
                if (models.isEmpty()) {
                    val modelsArr = json.getAsJsonArray("models")
                    if (modelsArr != null) {
                        for (elem in modelsArr) {
                            val id = elem.asJsonObject.get("id")?.asString
                            if (id != null) models.add(id)
                        }
                    }
                }

                post {
                    if (models.isNotEmpty()) {
                        customModels.addAll(models.sorted())
                        translationLog.add("Found ${models.size} models from custom provider")
                    } else {
                        translationLog.add("[!] No models found at $baseUrl/v1/models")
                    }
                    customModelsLoading.value = false
                }
            } catch (e: Exception) {
                post {
                    translationLog.add("[!] Failed to fetch models: ${e.message}")
                    customModelsLoading.value = false
                }
            }
        }
    }
}
