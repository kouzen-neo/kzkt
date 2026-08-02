package com.kzkt.app.core

import android.content.Context
import android.graphics.*
import android.util.Log
import java.util.regex.Pattern

/**
 * Font management and text rendering.
 * Ported from the original Python font + text rendering services
 */
class TextRenderer(private val context: Context) {

    // Cache base Typeface per font file, not per (font, size) — the size is applied via Paint.textSize.
    // Keeps 2 entries (Komika Axis + KosugiMaru) instead of unbounded growth from dynamic font sizing.
    private val fontCache = mutableMapOf<String, Typeface>()

    // Font paths
    private val FONT_MANGA = "fonts/Komika Axis.ttf"
    private val FONT_UNIVERSAL = "fonts/KosugiMaru.ttf"

    // Non-Latin Unicode ranges: Hiragana, Katakana, CJK, Hangul, Thai, Cyrillic, Arabic
    private val NON_LATIN_PATTERN = Pattern.compile(
        "[\\u3040-\\u309f\\u30a0-\\u30ff\\u4e00-\\u9fff\\uac00-\\ud7af\\u0e00-\\u0e7f\\u0400-\\u04ff\\u0600-\\u06ff]"
    )

    fun hasNonLatin(text: String): Boolean {
        return NON_LATIN_PATTERN.matcher(text).find()
    }

    private fun getTypeface(text: String, size: Int, language: String? = null): Typeface {
        val isNonLatin = hasNonLatin(text)
        val fontPath = if (isNonLatin) FONT_UNIVERSAL else FONT_MANGA

        return fontCache.getOrPut(fontPath) {
            try {
                Typeface.createFromAsset(context.assets, fontPath)
            } catch (e: Exception) {
                Log.w("KZKT", "Font load failed for $fontPath: ${e.message}, using default")
                Typeface.DEFAULT
            }
        }
    }

    // ── Font Size Settings ─────────────────────────────────────────

    private data class TextSettings(
        val scaleW: Double,
        val scaleH: Double,
        val fontScale: Double,
        val spacingRatio: Double,
        val maxFont: Int,
        val minFont: Int,
    )

    private fun pickTextSettings(boxWidth: Int, boxHeight: Int, text: String): TextSettings {
        val cleanText = text.replace(" ", "").replace("\n", "")
        val charCount = cleanText.length
        val area = boxWidth * boxHeight

        val isLargeBubble = boxWidth >= 150 && boxHeight >= 130 && area >= 30000
        val isShortText = charCount <= 55
        val isVeryShortText = charCount <= 28

        return when {
            isLargeBubble && isVeryShortText -> TextSettings(0.85, 0.78, 0.95, 0.055, 86, 10)
            isLargeBubble && isShortText -> TextSettings(0.82, 0.78, 0.94, 0.060, 82, 10)
            else -> TextSettings(0.76, 0.76, 0.92, 0.075, 76, 8)
        }
    }

    // ── Word Wrapping ──────────────────────────────────────────────

    private fun wrapTextPerWord(paint: Paint, text: String, maxWidth: Float): String {
        if (text.isEmpty()) return ""

        val isCjk = hasNonLatin(text) && !text.contains(" ")
        val rawWords: List<String> = if (isCjk) {
            text.toList().map { it.toString() }
        } else {
            text.split(" ")
        }

        if (rawWords.isEmpty()) return ""

        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in rawWords) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            val measuredWidth = paint.measureText(candidate)

            if (measuredWidth <= maxWidth || currentLine.isEmpty()) {
                currentLine = if (currentLine.isEmpty()) StringBuilder(word)
                    else StringBuilder("$currentLine $word")
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }

        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines.joinToString("\n")
    }

    // ── Main Text Rendering ────────────────────────────────────────

    /**
     * Render translated text inside a speech bubble.
     * Ported from tulis_teks_di_balon()
     *
     * @param canvas target canvas (drawn on a Bitmap)
     * @param bubbleRect [x1, y1, x2, y2] bubble coordinates
     * @param text translated text
     * @param backgroundPatch draw white patch behind text
     * @param targetLanguage language code
     */
    fun renderTextInBubble(
        canvas: Canvas,
        bubbleRect: IntArray,
        text: String,
        backgroundPatch: Boolean = false,
        targetLanguage: String? = null,
    ) {
        val (x1, y1, x2, y2) = bubbleRect
        val boxWidth = maxOf(1, x2 - x1)
        val boxHeight = maxOf(1, y2 - y1)
        val settings = pickTextSettings(boxWidth, boxHeight, text)

        val langKey = targetLanguage?.lowercase() ?: ""
        val isJapanese = langKey == "japanese" || langKey == "jepang"

        if (isJapanese) {
            renderJapaneseVertical(canvas, text, x1, y1, x2, y2, settings, backgroundPatch)
            return
        }

        var displayText = text
        if (!hasNonLatin(text)) displayText = text.uppercase()

        val maxW = (boxWidth * settings.scaleW).toFloat()
        val maxH = (boxHeight * settings.scaleH).toFloat()
        val minFontSize = settings.minFont
        val maxFontSize = settings.maxFont

        var bestFontSize = minFontSize
        var bestWrappedText = displayText
        var bestSpacing = 1f
        var bestScore = -1f

        // Search for best font size
        for (fSize in maxFontSize downTo minFontSize) {
            val paint = Paint().apply {
                typeface = getTypeface(displayText, fSize)
                textSize = fSize.toFloat()
                isAntiAlias = true
            }

            val spacing = maxOf(1, (fSize * settings.spacingRatio).toInt()).toFloat()
            val wrapped = wrapTextPerWord(paint, displayText, maxW)
            val lines = wrapped.split("\n")
            var maxLineWidth = 0f
            var totalHeight = 0f
            for (line in lines) {
                val lineWidth = paint.measureText(line)
                if (lineWidth > maxLineWidth) maxLineWidth = lineWidth
                totalHeight += paint.textSize + spacing
            }
            totalHeight -= spacing // Remove spacing after last line

            if (maxLineWidth <= maxW && totalHeight <= maxH) {
                val fillW = maxLineWidth / maxOf(1f, maxW)
                val fillH = totalHeight / maxOf(1f, maxH)
                val score = fSize * 10f + fillW + fillH
                if (score > bestScore) {
                    bestScore = score
                    bestFontSize = fSize
                    bestWrappedText = wrapped
                    bestSpacing = spacing
                }
                break
            } else if (fSize > bestFontSize * 1.5) {
                val overflowRatio = maxOf(
                    maxLineWidth / maxOf(1f, maxW),
                    totalHeight / maxOf(1f, maxH)
                )
                if (overflowRatio <= 1.15f) {
                    val fillW = maxLineWidth / maxOf(1f, maxW)
                    val fillH = totalHeight / maxOf(1f, maxH)
                    val score = fSize * 10f + fillW + fillH - overflowRatio * 50f
                    if (score > bestScore) {
                        bestScore = score
                        bestFontSize = fSize
                        bestWrappedText = wrapped
                        bestSpacing = spacing
                    }
                }
            }
        }

        // Final render
        bestFontSize = maxOf(minFontSize, (bestFontSize * settings.fontScale).toInt())
        val finalPaint = Paint().apply {
            typeface = getTypeface(displayText, bestFontSize)
            textSize = bestFontSize.toFloat()
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }

        val finalWrapped = wrapTextPerWord(finalPaint, displayText, maxW)
        val finalLines = finalWrapped.split("\n")
        var textWidth = 0f
        var textHeight = 0f
        for (line in finalLines) {
            val lw = finalPaint.measureText(line)
            if (lw > textWidth) textWidth = lw
            textHeight += finalPaint.textSize + bestSpacing
        }
        textHeight -= bestSpacing

        val centerX = x1 + (boxWidth - textWidth) / 2f
        val centerY = y1 + (boxHeight - textHeight) / 2f

        val strokeW = maxOf(1f, bestFontSize / 11f)

        // Background patch
        if (backgroundPatch) {
            val pad = maxOf(6f, bestFontSize / 2f)
            val rect = RectF(
                centerX - pad,
                centerY - pad,
                centerX + textWidth + pad,
                centerY + textHeight + pad
            )
            val radius = maxOf(4f, bestFontSize / 2f).toFloat()
            try {
                canvas.drawRoundRect(rect, radius, radius, Paint().apply { color = Color.WHITE })
            } catch (e: Exception) {
                canvas.drawRect(rect, Paint().apply { color = Color.WHITE })
            }
        }

        // Draw each line with stroke
        var currentY = centerY
        for (line in finalLines) {
            val lineWidth = finalPaint.measureText(line)
            val lineX = centerX + (textWidth - lineWidth) / 2f

            // Stroke (white outline)
            val strokePaint = Paint(finalPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = Color.WHITE
            }
            canvas.drawText(line, lineX, currentY - finalPaint.fontMetrics.ascent, strokePaint)

            // Fill (black text)
            val fillPaint = Paint(finalPaint).apply {
                style = Paint.Style.FILL
                color = Color.BLACK
            }
            canvas.drawText(line, lineX, currentY - finalPaint.fontMetrics.ascent, fillPaint)

            currentY += finalPaint.textSize + bestSpacing
        }
    }

    // ── Japanese Vertical Text ─────────────────────────────────────

    private fun renderJapaneseVertical(
        canvas: Canvas,
        text: String,
        x1: Int, y1: Int,
        x2: Int, y2: Int,
        settings: TextSettings,
        backgroundPatch: Boolean,
    ) {
        val cleanText = text.replace(" ", "").replace("\n", "")
        val boxWidth = maxOf(1, x2 - x1)
        val boxHeight = maxOf(1, y2 - y1)

        val maxW = (boxWidth * settings.scaleW).toFloat()
        val maxH = (boxHeight * settings.scaleH).toFloat()
        val minFontSize = settings.minFont
        val maxFontSize = settings.maxFont

        var bestFontSize = minFontSize
        var bestColumns = listOf<String>()

        for (fSize in maxFontSize downTo minFontSize) {
            val charH = fSize
            val charW = fSize
            val charsPerCol = maxOf(1, (maxH / charH).toInt())
            val columns = cleanText.chunked(charsPerCol)
            val totalW = columns.size * charW

            if (totalW <= maxW) {
                bestFontSize = fSize
                bestColumns = columns
                break
            }
        }

        if (bestColumns.isEmpty()) {
            val charsPerCol = maxOf(1, (maxH / minFontSize).toInt())
            bestColumns = cleanText.chunked(charsPerCol)
        }

        bestFontSize = maxOf(minFontSize, (bestFontSize * settings.fontScale).toInt())
        val paint = Paint().apply {
            typeface = getTypeface(cleanText, bestFontSize)
            textSize = bestFontSize.toFloat()
            isAntiAlias = true
        }

        val fontMetrics = paint.fontMetrics
        val charH = paint.textSize
        val charW = paint.textSize

        val actualW = bestColumns.size * charW
        val actualH = (bestColumns.maxOfOrNull { it.length } ?: 0) * charH

        val startX = x1 + (boxWidth - actualW) / 2f + charW
        val startY = y1 + (boxHeight - actualH) / 2f

        val strokeW = maxOf(1f, bestFontSize / 11f)
        val strokePaint = Paint(paint).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }
        val fillPaint = Paint(paint).apply {
            style = Paint.Style.FILL
            color = Color.BLACK
        }

        // Background patch
        if (backgroundPatch) {
            val pad = maxOf(6f, bestFontSize / 2f)
            val rect = RectF(
                startX - actualW + charW - pad,
                startY - pad,
                startX + charW + pad,
                startY + actualH + pad
            )
            canvas.drawRect(rect, Paint().apply { color = Color.WHITE })
        }

        // Draw columns right-to-left
        var curX = startX
        for (col in bestColumns) {
            var curY = startY
            for (ch in col) {
                var offsetX = 0f
                var offsetY = 0f
                val displayChar = when (ch) {
                    '。', '、', '.' -> {
                        offsetX = charW * 0.6f
                        offsetY = -charW * 0.6f
                        ch
                    }
                    'ー' -> '︱'
                    else -> ch
                }

                val cx = curX + offsetX
                val cy = curY + offsetY

                // Stroke (white outline) — drawn as fill with stroke for outline effect
                strokePaint.style = Paint.Style.FILL_AND_STROKE
                strokePaint.color = Color.WHITE
                canvas.drawText(displayChar.toString(), cx, cy - fontMetrics.ascent, strokePaint)

                // Fill (black text on top)
                canvas.drawText(displayChar.toString(), cx, cy - fontMetrics.ascent, fillPaint)

                curY += charH
            }
            curX -= charW
        }
    }
}
