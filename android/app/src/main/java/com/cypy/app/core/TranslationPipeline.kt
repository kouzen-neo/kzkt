package com.cypy.app.core

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import com.cypy.app.core.Config.TweakParams
import com.cypy.app.core.providers.LlmProvider
import com.cypy.app.util.JsonUtils
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

/**
 * Main translation pipeline: detection → filter → mosaic → LLM → render → save.
 * Ported from cypy/core/translator.py
 */
class TranslationPipeline(
    private val yolo: YoloOnnx?,
    private val provider: LlmProvider,
    private val textRenderer: TextRenderer,
    private val params: TweakParams,
    private val rateLimiter: RateLimiter = RateLimiter((params.minRequestDelay * 1000).toLong()),
    private val targetLanguage: String = "Indonesian",
    private val onProgress: (String) -> Unit = {},
    private val isCancelled: () -> Boolean = { false },
) {
    data class PipelineResult(
        val outputPath: String?,
        val bubblesFound: Int = 0,
        val bubblesTranslated: Int = 0,
        val failed: Boolean = false,
        val alreadyDone: Boolean = false,
    )

    /**
     * Process a single manga page image.
     */
    suspend fun processSingleImage(inputPath: String, outputDir: String): PipelineResult {
        if (isCancelled()) return PipelineResult(null, failed = true)

        val imgFile = File(inputPath)
        if (!imgFile.exists()) return PipelineResult(null, failed = true)

        val bitmap = ImageProcessor.loadBitmap(inputPath) ?: return PipelineResult(null, failed = true)

        // Auto-split landscape
        val splitCount = ImageProcessor.shouldAutoSplit(bitmap)
        if (splitCount > 1) {
            return processLandscape(bitmap, inputPath, outputDir, splitCount)
        }

        return processBitmap(bitmap, inputPath, outputDir)
    }

    private suspend fun processLandscape(
        bitmap: Bitmap, inputPath: String, outputDir: String, splitCount: Int
    ): PipelineResult {
        onProgress("[Auto-Split] Wide image detected. Splitting into $splitCount parts...")
        val imgHeight = bitmap.height
        val splitWidth = bitmap.width / splitCount
        val results = mutableListOf<String>()

        for (i in 0 until splitCount) {
            if (isCancelled()) return PipelineResult(null, failed = true)

            val xEnd = bitmap.width - (i * splitWidth)
            val xStart = if (i == splitCount - 1) 0 else xEnd - splitWidth

            val partBitmap = Bitmap.createBitmap(bitmap, xStart, 0, xEnd - xStart, imgHeight)
            val partPath = File(
                outputDir,
                "${File(inputPath).nameWithoutExtension}_split${i + 1}.png"
            ).absolutePath
            saveBitmap(partBitmap, partPath)

            onProgress("  Translating Part ${i + 1}...")
            val result = processBitmap(partBitmap, partPath, outputDir)
            if (result.outputPath != null) results.add(result.outputPath)

            File(partPath).delete()
        }

        if (results.size == splitCount) {
            // Recombine right-to-left (manga order)
            val images = results.map { ImageProcessor.loadBitmap(it)!! }.reversed()
            val targetH = images.maxOf { it.height }
            val resized = images.map { bmp ->
                if (bmp.height != targetH) {
                    val scale = targetH.toDouble() / bmp.height
                    Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), targetH, true)
                } else bmp
            }

            val totalW = resized.sumOf { it.width }
            val combined = Bitmap.createBitmap(totalW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(combined)
            var x = 0
            for (img in resized) {
                canvas.drawBitmap(img, x.toFloat(), 0f, null)
                x += img.width
            }

            // Cleanup individual results
            results.forEach { File(it).delete() }

            val outputPath = MosaicBuilder.makeOutputPath(inputPath, targetLanguage, outputDir)
            saveBitmap(combined, outputPath)
            return PipelineResult(outputPath, bubblesFound = results.size)
        }

        return PipelineResult(null, failed = true)
    }

    private suspend fun processBitmap(
        bitmap: Bitmap, inputPath: String, outputDir: String
    ): PipelineResult {
        onProgress("Translating: ${File(inputPath).name}")

        val mat = ImageProcessor.bitmapToMat(bitmap)
        val imgHeight = mat.rows()
        val imgWidth = mat.cols()

        // ── YOLO Detection (3-stage cascade) ──
        val rawBoxes = mutableListOf<IntArray>()
        for ((conf, iou) in Constants.YOLO_PREDICTION_STAGES) {
            if (isCancelled()) return PipelineResult(null, failed = true)
            val detections = yolo?.predict(bitmap) ?: continue
            for (d in detections) {
                rawBoxes.add(intArrayOf(d.x1, d.y1, d.x2, d.y2))
            }
        }
        onProgress("  Found ${rawBoxes.size} raw detections...")

        // ── Filtering ──
        var filtered = ImageProcessor.removeFalseGiants(rawBoxes)
        filtered = ImageProcessor.mergeOverlapping(filtered)
        filtered = ImageProcessor.removeNonsense(filtered, imgWidth, imgHeight)
        filtered = ImageProcessor.removeSfxAndImages(mat, filtered, params)
        onProgress("  Filtered to ${filtered.size} speech bubbles...")

        if (filtered.isEmpty()) {
            onProgress("  No text bubbles found.")
            val outputPath = MosaicBuilder.makeOutputPath(inputPath, targetLanguage, outputDir)
            saveBitmap(bitmap, outputPath)
            return PipelineResult(outputPath)
        }

        // ── Crop extraction ──
        val cropItems = mutableListOf<MosaicBuilder.CropItem>()
        val coordinateMap = mutableMapOf<String, IntArray>()

        for ((order, box) in filtered.withIndex()) {
            val (x1, y1, x2, y2) = box
            val boxW = maxOf(1, x2 - x1)
            val boxH = maxOf(1, y2 - y1)

            val padX = maxOf(params.minPad, (boxW * params.padXRatio).toInt())
            val padY = maxOf(params.minPad, (boxH * params.padYRatio).toInt())

            val (cropX1, cropY1, cropX2, cropY2) = ImageProcessor.smartCropBounds(
                box, filtered, imgWidth, imgHeight, padX, padY, params
            )

            val cropMat = mat.submat(org.opencv.core.Rect(cropX1, cropY1, cropX2 - cropX1, cropY2 - cropY1))
            var maskedMat = ImageProcessor.maskOutsideBubble(cropMat, cropX1, cropY1, x1, y1, x2, y2, params)

            // Scale up
            val scale = params.skalaPotonganMosaik
            val cropBitmap = ImageProcessor.matToBitmap(maskedMat)
            val scaledBitmap = if (scale != 1.0) {
                Bitmap.createScaledBitmap(
                    cropBitmap,
                    maxOf(1, (cropBitmap.width * scale).toInt()),
                    maxOf(1, (cropBitmap.height * scale).toInt()),
                    true
                )
            } else cropBitmap

            cropItems.add(MosaicBuilder.CropItem((order + 1).toString(), scaledBitmap))
            coordinateMap[(order + 1).toString()] = box
        }

        // ── Mosaic → LLM Translate ──
        val maxPerBatch = params.maxBubblesPerRequest
        val chunks = MosaicBuilder.chunkCrops(cropItems, maxPerBatch)
        val allTranslations = mutableMapOf<String, String>()

        for ((chunkIdx, chunk) in chunks.withIndex()) {
            if (isCancelled()) return PipelineResult(null, failed = true)

            if (chunks.size > 1) {
                onProgress("  [Chunk ${chunkIdx + 1}/${chunks.size}] Processing bubbles ${chunk.first().id}..${chunk.last().id}")
            }

            val shrunk = MosaicBuilder.shrinkCropsIfTooTall(chunk, params.maxTinggiMosaik, params.jarakAntarPotongan)
            val mosaic = MosaicBuilder.buildMosaic(shrunk, params)

            onProgress("  Translating with ${provider.providerName}...")
            val prompt = Constants.buildPrompt(targetLanguage)

            try {
                val result = rateLimiter.executeWithRetry(
                    apiCall = { provider.translateImage(mosaic, prompt) },
                    providerName = provider.providerName,
                    isCancelled = isCancelled,
                    onWait = { msg -> onProgress(msg) }
                )

                if (result != null) {
                    val cleaned = JsonUtils.sanitizeJson(result)
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val parsed: Map<String, String> = GsonBuilder().setLenient().create().fromJson(cleaned, type)
                    allTranslations.putAll(parsed)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                if (msg == "API_KEY_ERROR") {
                    onProgress("[!] API key for ${provider.providerName} is expired or invalid.")
                } else {
                    onProgress("[!] ${provider.providerName} request failed: $msg")
                }
            }
        }

        if (allTranslations.isEmpty()) {
            onProgress("  [!] Translation failed.")
            return PipelineResult(null, failed = true)
        }

        // ── Render translations ──
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        var translatedCount = 0
        // Convert all JSON keys to id strings: LLM may return int keys "1" vs 1
        val normalizedTranslations = mutableMapOf<String, String>()
        for ((key, text) in allTranslations) {
            val id = if (key.startsWith("_")) key else {
                // Parse integer prefix from keys like "1" or "1_2"
                val parts = key.split("_")
                if (parts.size == 2) {
                    "${parts[0].toIntOrNull() ?: key}_${parts[1].toIntOrNull() ?: parts[1]}"
                } else if (parts.size == 1) {
                    parts[0].toIntOrNull()?.toString() ?: key
                } else key
            }
            normalizedTranslations[id] = text
        }

        for ((num, text) in normalizedTranslations) {
            if (num in coordinateMap && text.uppercase() != "SKIP" && text.isNotBlank()) {
                val (x1, y1, x2, y2) = coordinateMap[num]!!
                val w = maxOf(1, x2 - x1)
                val h = maxOf(1, y2 - y1)
                val ratio = w.toDouble() / h
                val areaRatio = (w * h).toDouble() / maxOf(1, imgWidth * imgHeight)

                // Skip suspicious boxes (same logic as Python)
                if (ratio >= 3.2 && w >= imgWidth * 0.35) continue
                if (areaRatio >= 0.035 && ratio >= 2.8) continue

                val suspiciousFlat = ratio >= params.rasioBoxGepeng &&
                    w >= imgWidth * params.lebarBoxGepengRatio &&
                    h <= imgHeight * params.tinggiBoxGepengRatio

                if (params.pakaiPatchUntukBoxGepeng && suspiciousFlat) {
                    textRenderer.renderTextInBubble(canvas, coordinateMap[num]!!, text,
                        backgroundPatch = true, targetLanguage = targetLanguage)
                } else {
                    // Background: blurred white patch
                    val marginX = (w * params.maskMarginRatio).toInt()
                    val marginY = (h * params.maskMarginRatio).toInt()
                    val cornerRadius = maxOf(6, minOf(w, h) / 3)

                    val overlay = Bitmap.createBitmap(resultBitmap.width, resultBitmap.height, Bitmap.Config.ARGB_8888)
                    overlay.eraseColor(Color.TRANSPARENT)
                    val overlayCanvas = Canvas(overlay)

                    val bgPaint = Paint().apply {
                        color = Color.WHITE
                        isAntiAlias = true
                    }
                    overlayCanvas.drawRoundRect(
                        RectF(
                            (x1 + marginX).toFloat(), (y1 + marginY).toFloat(),
                            (x2 - marginX).toFloat(), (y2 - marginY).toFloat()
                        ),
                        cornerRadius.toFloat(), cornerRadius.toFloat(), bgPaint
                    )

                    // Apply blur (simple Box blur since RenderScript is deprecated)
                    val blurPaint = Paint().apply {
                        maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.drawBitmap(overlay, 0f, 0f, blurPaint)

                    textRenderer.renderTextInBubble(canvas, coordinateMap[num]!!, text,
                        backgroundPatch = false, targetLanguage = targetLanguage)
                }
                translatedCount++
            }
        }

        val outputPath = MosaicBuilder.makeOutputPath(inputPath, targetLanguage, outputDir)
        saveBitmap(resultBitmap, outputPath)
        onProgress("  Done! ${translatedCount}/${cropItems.size} bubbles translated.")

        return PipelineResult(
            outputPath = outputPath,
            bubblesFound = cropItems.size,
            bubblesTranslated = translatedCount,
        )
    }

    /**
     * Batch process multiple images with multi-page batching.
     * Ported from process_image_batch() — stitches bubbles from multiple pages into combined requests.
     */
    suspend fun processImageBatch(
        imagePaths: List<String>,
        outputDir: String,
    ): List<PipelineResult> {
        if (imagePaths.isEmpty()) return emptyList()
        onProgress("[Multi-Page Batch] Processing ${imagePaths.size} pages...")

        // Phase 1: Process each page (detection + crop), skip already done
        data class PageData(
            val path: String,
            val pil: Bitmap,
            val draws: Canvas?,
            val imgWidth: Int,
            val imgHeight: Int,
            val crops: MutableList<Pair<String, Bitmap>>,
            val coordMap: MutableMap<String, IntArray>,
            val alreadyDone: Boolean = false,
            val failed: Boolean = false,
        )

        val pageDataList = mutableListOf<PageData>()

        for ((idx, imgPath) in imagePaths.withIndex()) {
            if (isCancelled()) return emptyList()

            val expectedOutput = MosaicBuilder.makeOutputPath(imgPath, targetLanguage, outputDir)
            if (File(expectedOutput).exists()) {
                onProgress("  [${idx + 1}/${imagePaths.size}] Skipping ${File(imgPath).name} (Already translated).")
                pageDataList.add(PageData(imgPath, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), null, 0, 0,
                    mutableListOf(), mutableMapOf(), alreadyDone = true))
                continue
            }

            val bitmap = ImageProcessor.loadBitmap(imgPath)
            if (bitmap == null) {
                pageDataList.add(PageData(imgPath, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), null, 0, 0,
                    mutableListOf(), mutableMapOf(), failed = true))
                continue
            }

            val mat = ImageProcessor.bitmapToMat(bitmap)
            val imgHeight = mat.rows()
            val imgWidth = mat.cols()

            // YOLO detection
            val rawBoxes = mutableListOf<IntArray>()
            for ((conf, iou) in Constants.YOLO_PREDICTION_STAGES) {
                val detections = yolo?.predict(bitmap) ?: continue
                for (d in detections) rawBoxes.add(intArrayOf(d.x1, d.y1, d.x2, d.y2))
            }
            var filtered = ImageProcessor.removeFalseGiants(rawBoxes)
            filtered = ImageProcessor.mergeOverlapping(filtered)
            filtered = ImageProcessor.removeNonsense(filtered, imgWidth, imgHeight)
            filtered = ImageProcessor.removeSfxAndImages(mat, filtered, params)

            val resultBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val crops = mutableListOf<Pair<String, Bitmap>>()
            val coordMap = mutableMapOf<String, IntArray>()

            for ((order, box) in filtered.withIndex()) {
                val (x1, y1, x2, y2) = box
                val boxW = maxOf(1, x2 - x1)
                val boxH = maxOf(1, y2 - y1)
                val padX = maxOf(params.minPad, (boxW * params.padXRatio).toInt())
                val padY = maxOf(params.minPad, (boxH * params.padYRatio).toInt())
                val id = "${idx + 1}_${order + 1}"

                val (cropX1, cropY1, cropX2, cropY2) = ImageProcessor.smartCropBounds(
                    box, filtered, imgWidth, imgHeight, padX, padY, params)
                val cropMat = mat.submat(org.opencv.core.Rect(cropX1, cropY1, cropX2 - cropX1, cropY2 - cropY1))
                var maskedMat = ImageProcessor.maskOutsideBubble(cropMat, cropX1, cropY1, x1, y1, x2, y2, params)

                val scale = params.skalaPotonganMosaik
                val cropBitmap = ImageProcessor.matToBitmap(maskedMat)
                val scaled = if (scale != 1.0) Bitmap.createScaledBitmap(cropBitmap,
                    maxOf(1, (cropBitmap.width * scale).toInt()),
                    maxOf(1, (cropBitmap.height * scale).toInt()), true)
                else cropBitmap

                crops.add(id to scaled)
                coordMap[id] = box
            }

            pageDataList.add(PageData(imgPath, resultBmp, Canvas(resultBmp), imgWidth, imgHeight,
                crops.toMutableList(), coordMap))
        }

        // Phase 2: Collect all crops across pages and batch
        val allCrops = pageDataList.filter { !it.alreadyDone && !it.failed }
            .flatMap { it.crops }

        if (allCrops.isEmpty()) {
            return pageDataList.map { PipelineResult(MosaicBuilder.makeOutputPath(it.path, targetLanguage, outputDir),
                alreadyDone = it.alreadyDone, failed = it.failed) }
        }

        onProgress("  Total bubbles across all pages: ${allCrops.size}")

        // Phase 3: Chunk → Mosaic → LLM
        val cropItems = allCrops.map { MosaicBuilder.CropItem(it.first, it.second) }
        val chunks = MosaicBuilder.chunkCrops(cropItems, params.maxBubblesPerRequest)
        val allTranslations = mutableMapOf<String, String>()

        for ((chunkIdx, chunk) in chunks.withIndex()) {
            if (isCancelled()) return emptyList()
            onProgress("  [Batch ${chunkIdx + 1}/${chunks.size}] ${chunk.size} bubbles...")

            val shrunk = MosaicBuilder.shrinkCropsIfTooTall(chunk, params.maxTinggiMosaik, params.jarakAntarPotongan)
            val mosaic = MosaicBuilder.buildMosaic(shrunk, params)
            val prompt = Constants.buildPrompt(targetLanguage)

            try {
                val result = rateLimiter.executeWithRetry(
                    apiCall = { provider.translateImage(mosaic, prompt) },
                    providerName = provider.providerName,
                    isCancelled = isCancelled,
                    onWait = { msg -> onProgress(msg) }
                )
                if (result != null) {
                    val cleaned = JsonUtils.sanitizeJson(result)
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    allTranslations.putAll(GsonBuilder().setLenient().create().fromJson(cleaned, type))
                }
            } catch (e: Exception) {
                onProgress("[!] ${provider.providerName} request failed: ${e.message}")
            }
        }

        // Phase 4: Render per-page
        val results = mutableListOf<PipelineResult>()
        for (page in pageDataList) {
            if (page.alreadyDone) {
                results.add(PipelineResult(MosaicBuilder.makeOutputPath(page.path, targetLanguage, outputDir), alreadyDone = true))
                continue
            }
            if (page.failed) {
                results.add(PipelineResult(null, failed = true))
                continue
            }

            var translatedCount = 0
            val canvas = Canvas(page.pil)

            for ((id, text) in allTranslations) {
                if (id in page.coordMap && text.uppercase() != "SKIP" && text.isNotBlank()) {
                    val (x1, y1, x2, y2) = page.coordMap[id]!!
                    textRenderer.renderTextInBubble(canvas, intArrayOf(x1, y1, x2, y2), text,
                        backgroundPatch = false, targetLanguage = targetLanguage)
                    translatedCount++
                }
            }

            val outputPath = MosaicBuilder.makeOutputPath(page.path, targetLanguage, outputDir)
            saveBitmap(page.pil, outputPath)
            results.add(PipelineResult(outputPath, bubblesFound = page.crops.size, bubblesTranslated = translatedCount))
        }

        return results
    }

    private fun saveBitmap(bitmap: Bitmap, path: String) {
        val file = File(path)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
