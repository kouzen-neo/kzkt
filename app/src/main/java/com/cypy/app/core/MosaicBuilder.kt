package com.cypy.app.core

import android.graphics.*
import com.cypy.app.CypyApplication
import com.cypy.app.core.Config.TweakParams

/**
 * Build mosaic images from bubble crops for batch LLM translation.
 * Ported from cypy/core/translator.py mosaic assembly sections.
 */
object MosaicBuilder {

    data class CropItem(val id: String, val bitmap: Bitmap)

    /**
     * Partition crops into chunks of maxBubblesPerChunk.
     */
    fun chunkCrops(crops: List<CropItem>, maxPerChunk: Int = 20): List<List<CropItem>> {
        if (crops.isEmpty()) return emptyList()
        val chunkSize = if (maxPerChunk <= 0) 20 else maxPerChunk
        return crops.chunked(chunkSize)
    }

    /**
     * Shrink crops if mosaic would exceed max height.
     */
    fun shrinkCropsIfTooTall(
        crops: List<CropItem>,
        maxHeight: Int = 6000,
        spacing: Int = 10,
    ): List<CropItem> {
        val totalImageHeight = crops.sumOf { it.bitmap.height }
        val totalSpace = crops.size * spacing + 20
        val initialHeight = totalImageHeight + totalSpace

        if (initialHeight <= maxHeight || crops.isEmpty()) return crops

        val targetImageHeight = maxOf(1, maxHeight - totalSpace)
        val ratio = targetImageHeight.toDouble() / totalImageHeight

        return crops.map { (id, bmp) ->
            val newW = maxOf(1, (bmp.width * ratio).toInt())
            val newH = maxOf(1, (bmp.height * ratio).toInt())
            val scaled = Bitmap.createScaledBitmap(bmp, newW, newH, true)
            CropItem(id, scaled)
        }
    }

    /**
     * Build a mosaic image: white canvas with numbered crops arranged vertically.
     * Red ID numbers on the left, crops on the right.
     */
    fun buildMosaic(
        crops: List<CropItem>,
        params: TweakParams,
    ): Bitmap {
        if (crops.isEmpty()) {
            return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }

        val mosaicWidth = maxOf(
            params.lebarMosaikMin,
            (crops.maxOf { it.bitmap.width } + params.marginKiriNomor + params.marginKanan)
        )

        val mosaicHeight = crops.sumOf { it.bitmap.height } +
            (crops.size * params.jarakAntarPotongan) + 20

        val mosaic = Bitmap.createBitmap(mosaicWidth, mosaicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mosaic)
        canvas.drawColor(Color.WHITE)

        // Number font
        val numberPaint = Paint().apply {
            color = Color.RED
            textSize = 40f
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            typeface = Typeface.DEFAULT_BOLD
        }

        var yOffset = 10
        for ((id, crop) in crops) {
            // Draw red ID number
            val numY = yOffset + (crop.height / 2f) - (numberPaint.textSize / 2f) + 20f
            canvas.drawText(id, 5f, numY, numberPaint)

            // Paste crop
            canvas.drawBitmap(crop, params.marginKiriNomor.toFloat(), yOffset.toFloat(), null)
            yOffset += crop.height + params.jarakAntarPotongan
        }

        return mosaic
    }

    /**
     * Generate output filename with language code folder.
     * Falls back to fallbackDir (or parent dir of input) when input path is unwritable
     * (e.g. PhotoPicker synthetic paths).
     */
    fun makeOutputPath(inputPath: String, targetLanguage: String, fallbackDir: String = ""): String {
        val langCode = Config.getLangCode(targetLanguage).uppercase()
        val file = java.io.File(inputPath)
        val pathStr = file.absolutePath

        val isSynthetic = pathStr.contains(".transforms") ||
                pathStr.contains("picker_get_content") ||
                pathStr.contains("photopicker")

        val base = if (!isSynthetic && fallbackDir.isBlank() && file.parentFile != null && file.parentFile!!.canWrite()) {
            file.parentFile!!
        } else if (fallbackDir.isNotBlank()) {
            java.io.File(fallbackDir)
        } else {
            java.io.File(CypyApplication.instance.getExternalFilesDir(null), "translated")
        }

        val outputDir = java.io.File(base, langCode)
        try { outputDir.mkdirs() } catch (_: Exception) {}
        return java.io.File(outputDir, file.name).absolutePath
    }

}
