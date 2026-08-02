package com.kzkt.app.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import com.kzkt.app.core.Config.TweakParams
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

/**
 * Image processing: box filtering, crop/mask, SFX detection, image I/O.
 * Ported from the original Python image service
 */
object ImageProcessor {

    init {
        try {
            System.loadLibrary("opencv_java4")
        } catch (_: Throwable) {}
        try {
            OpenCVLoader.initLocal()
        } catch (_: Throwable) {}
    }


    // ── Image I/O ──────────────────────────────────────────────────

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun bitmapToBase64DataUri(bitmap: Bitmap): String {
        return "data:image/png;base64,${bitmapToBase64(bitmap)}"
    }

    fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    fun loadBitmap(path: String): Bitmap? {
        return BitmapFactory.decodeFile(path)
    }

    // ── Box Geometry ───────────────────────────────────────────────

    private fun areaBox(box: IntArray): Int {
        return maxOf(0, box[2] - box[0]) * maxOf(0, box[3] - box[1])
    }

    private fun intersectionArea(a: IntArray, b: IntArray): Int {
        val ix1 = maxOf(a[0], b[0])
        val iy1 = maxOf(a[1], b[1])
        val ix2 = minOf(a[2], b[2])
        val iy2 = minOf(a[3], b[3])
        return maxOf(0, ix2 - ix1) * maxOf(0, iy2 - iy1)
    }

    private fun shouldMerge(a: IntArray, b: IntArray): Boolean {
        val areaA = areaBox(a)
        val areaB = areaBox(b)
        if (areaA == 0 || areaB == 0) return false
        val inter = intersectionArea(a, b)
        if (inter == 0) return false
        val iou = inter.toDouble() / (areaA + areaB - inter)
        val coverSmall = inter.toDouble() / minOf(areaA, areaB)
        return iou >= 0.28 || coverSmall >= 0.82
    }

    private fun overlap1D(a1: Int, a2: Int, b1: Int, b2: Int): Int {
        return maxOf(0, minOf(a2, b2) - maxOf(a1, b1))
    }

    // ── Box Filtering ──────────────────────────────────────────────

    /**
     * Remove false giant boxes: if a big box overlaps a small box by >= 80% of the small one's area
     * and is > 2.5x the area, keep the small one.
     * buang_kotak_raksasa_palsu()
     */
    fun removeFalseGiants(boxes: List<IntArray>): List<IntArray> {
        if (boxes.isEmpty()) return boxes

        val withArea = boxes.sortedByDescending { areaBox(it) }
        val keep = BooleanArray(withArea.size) { true }

        for (i in withArea.indices) {
            if (!keep[i]) continue
            val (boxI, areaI) = withArea[i] to areaBox(withArea[i])
            for (j in i + 1 until withArea.size) {
                if (!keep[j]) continue
                val (boxJ, areaJ) = withArea[j] to areaBox(withArea[j])
                if (areaI > 2.5 * areaJ) {
                    val inter = intersectionArea(boxI, boxJ)
                    if (inter >= 0.8 * areaJ) {
                        keep[i] = false
                        break
                    }
                }
            }
        }
        return withArea.filterIndexed { i, _ -> keep[i] }
    }

    /**
     * Merge overlapping boxes iteratively.
     * gabung_kotak_tumpang_tindih()
     */
    fun mergeOverlapping(boxes: List<IntArray>): List<IntArray> {
        if (boxes.isEmpty()) return boxes
        var result = boxes.sortedBy { it[0] }

        var changed = true
        while (changed) {
            changed = false
            val newBoxes = mutableListOf<IntArray>()
            val used = BooleanArray(result.size) { false }

            for (i in result.indices) {
                if (used[i]) continue
                var (x1, y1, x2, y2) = result[i]

                for (j in i + 1 until result.size) {
                    if (used[j]) continue
                    if (result[j][0] > x2) break
                    if (shouldMerge(intArrayOf(x1, y1, x2, y2), result[j])) {
                        x1 = minOf(x1, result[j][0])
                        y1 = minOf(y1, result[j][1])
                        x2 = maxOf(x2, result[j][2])
                        y2 = maxOf(y2, result[j][3])
                        used[j] = true
                        changed = true
                    }
                }
                newBoxes.add(intArrayOf(x1, y1, x2, y2))
                used[i] = true
            }
            result = newBoxes
        }

        return result.sortedBy { it[1] * 10000 + it[0] }
    }

    /**
     * Remove boxes that are too wide, flat, or thin.
     * buang_kotak_ngawur()
     */
    fun removeNonsense(boxes: List<IntArray>, imgWidth: Int, imgHeight: Int): List<IntArray> {
        val totalArea = maxOf(1, imgWidth * imgHeight)
        return boxes.filter { box ->
            val (x1, y1, x2, y2) = box
            val w = maxOf(1, x2 - x1)
            val h = maxOf(1, y2 - y1)
            val ratio = w.toDouble() / h
            val areaRatio = (w * h).toDouble() / totalArea

            val tooWide = ratio >= 3.2 && w >= imgWidth * 0.35
            val tooFlat = w >= imgWidth * 0.50 && h <= imgHeight * 0.16
            val tooThin = areaRatio >= 0.035 && ratio >= 2.8

            !(tooWide || tooFlat || tooThin)
        }
    }

    /**
     * SFX / noise removal filter based on pixel analysis.
     * buang_kotak_sfx_dan_gambar()
     */
    fun removeSfxAndImages(
        mat: Mat,
        boxes: List<IntArray>,
        params: TweakParams,
    ): List<IntArray> {
        if (!params.filterSfxAktif) return boxes

        val imgHeight = mat.rows()
        val imgWidth = mat.cols()
        val totalArea = maxOf(1, imgHeight * imgWidth)

        val (blackThr, edgeThr, whiteSafe) = when (params.filterSfxMode.lowercase()) {
            "relaxed", "longgar" -> Triple(0.20, 0.14, 0.58)
            "strict", "ketat" -> Triple(0.13, 0.09, 0.68)
            else -> Triple(0.16, 0.11, 0.62) // balanced
        }

        return boxes.filter { box ->
            val (x1, y1, x2, y2) = box
            val w = maxOf(1, x2 - x1)
            val h = maxOf(1, y2 - y1)
            val areaRatio = (w * h).toDouble() / totalArea
            val ratio = w.toDouble() / h

            // Small boxes: always keep
            val isSmall = w < imgWidth * 0.18 && h < imgHeight * 0.18 && areaRatio < 0.020
            if (isSmall) return@filter true

            val crop = mat.submat(Rect(x1, y1, w, h))
            if (crop.empty()) {
                crop.release()
                return@filter false
            }

            try {
                val gray = Mat()
                val blackMask = Mat()
                val whiteMask = Mat()
                val edges = Mat()
                try {
                    Imgproc.cvtColor(crop, gray, Imgproc.COLOR_RGBA2GRAY)

                    // Black threshold
                    Imgproc.threshold(gray, blackMask, 79.0, 255.0, Imgproc.THRESH_BINARY_INV)
                    val blackRatio = Core.countNonZero(blackMask).toDouble() / gray.total()

                    // White threshold
                    Imgproc.threshold(gray, whiteMask, 220.0, 255.0, Imgproc.THRESH_BINARY)
                    val whiteRatio = Core.countNonZero(whiteMask).toDouble() / gray.total()

                    // Edge detection
                    Imgproc.Canny(gray, edges, 80.0, 160.0)
                    val edgeRatio = Core.countNonZero(edges).toDouble() / gray.total()

                    // Safe: mostly white = text bubble
                    if (whiteRatio >= whiteSafe) return@filter true

                    val isSfxOrImage = areaRatio > 0.018 && blackRatio > blackThr && edgeRatio > edgeThr
                    val isFlatSuspicious = ratio > 2.2 && w > imgWidth * 0.30 &&
                        edgeRatio > maxOf(0.07, edgeThr - 0.03) && whiteRatio < whiteSafe
                    val isLargeSuspicious = areaRatio > 0.045 && whiteRatio < 0.55 && edgeRatio > 0.075

                    !(isSfxOrImage || isFlatSuspicious || isLargeSuspicious)
                } finally {
                    edges.release()
                    whiteMask.release()
                    blackMask.release()
                    gray.release()
                }
            } finally {
                crop.release()
            }
        }
    }

    /**
     * Detect bubble background color (light/white vs dark/black).
     * Returns android.graphics.Color (Color.WHITE or Color.BLACK).
     */
    fun detectBubbleBackgroundColor(mat: Mat, box: IntArray): Int {
        val (x1, y1, x2, y2) = box
        val w = maxOf(1, x2 - x1)
        val h = maxOf(1, y2 - y1)

        val crop = mat.submat(Rect(x1, y1, w, h))
        if (crop.empty()) {
            crop.release()
            return android.graphics.Color.WHITE
        }

        val gray = Mat()
        try {
            Imgproc.cvtColor(crop, gray, Imgproc.COLOR_RGBA2GRAY)
            val meanVal = Core.mean(gray).`val`[0]
            return if (meanVal < 128.0) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
        } catch (_: Exception) {
            return android.graphics.Color.WHITE
        } finally {
            gray.release()
            crop.release()
        }
    }

    // ── Crop & Mask ────────────────────────────────────────────────

    /**
     * Expand crop region with padding but avoid overlapping other boxes.
     * buat_crop_lega_tapi_tidak_nyamber()
     */
    fun smartCropBounds(
        box: IntArray,
        allBoxes: List<IntArray>,
        imgWidth: Int,
        imgHeight: Int,
        padX: Int,
        padY: Int,
        params: TweakParams,
    ): IntArray {
        val (x1, y1, x2, y2) = box
        var cropX1 = maxOf(0, x1 - padX)
        var cropY1 = maxOf(0, y1 - padY)
        var cropX2 = minOf(imgWidth, x2 + padX)
        var cropY2 = minOf(imgHeight, y2 + padY)

        val boxW = maxOf(1, x2 - x1)
        val boxH = maxOf(1, y2 - y1)

        for (other in allBoxes) {
            if (other.contentEquals(box)) continue
            val (ox1, oy1, ox2, oy2) = other
            val otherW = maxOf(1, ox2 - ox1)
            val otherH = maxOf(1, oy2 - oy1)

            val overlapX = overlap1D(x1, x2, ox1, ox2).toDouble() / minOf(boxW, otherW)
            val overlapY = overlap1D(y1, y2, oy1, oy2).toDouble() / minOf(boxH, otherH)

            if (overlapX >= params.overlapBatasCrop) {
                if (oy1 >= y2) { // other is below
                    val batas = (y2 + oy1) / 2
                    cropY2 = minOf(cropY2, maxOf(y2, batas))
                } else if (oy2 <= y1) { // other is above
                    val batas = (oy2 + y1) / 2
                    cropY1 = maxOf(cropY1, minOf(y1, batas))
                }
            }

            if (overlapY >= params.overlapBatasCrop) {
                if (ox1 >= x2) { // other is to the right
                    val batas = (x2 + ox1) / 2
                    cropX2 = minOf(cropX2, maxOf(x2, batas))
                } else if (ox2 <= x1) { // other is to the left
                    val batas = (ox2 + x1) / 2
                    cropX1 = maxOf(cropX1, minOf(x1, batas))
                }
            }
        }

        return intArrayOf(cropX1, cropY1, cropX2, cropY2)
    }

    /**
     * Mask outside the bubble area with white.
     * mask_luar_box_utama()
     */
    fun maskOutsideBubble(
        crop: Mat,
        cropX1: Int, cropY1: Int,
        x1: Int, y1: Int, x2: Int, y2: Int,
        params: TweakParams,
    ): Mat {
        if (!params.maskAreaLuarBox) return crop

        val localX1 = x1 - cropX1
        val localY1 = y1 - cropY1
        val localX2 = x2 - cropX1
        val localY2 = y2 - cropY1

        val maskX1 = maxOf(0, localX1 - params.maskMargin)
        val maskY1 = maxOf(0, localY1 - params.maskMargin)
        val maskX2 = minOf(crop.cols(), localX2 + params.maskMargin)
        val maskY2 = minOf(crop.rows(), localY2 + params.maskMargin)

        val result = Mat.ones(crop.size(), crop.type())
        val region = result.submat(Rect(maskX1, maskY1, maskX2 - maskX1, maskY2 - maskY1))
        val srcRegion = crop.submat(Rect(maskX1, maskY1, maskX2 - maskX1, maskY2 - maskY1))
        try {
            Core.multiply(result, Scalar.all(255.0), result)
            srcRegion.copyTo(region)
        } finally {
            srcRegion.release()
            region.release()
        }
        return result
    }

    /**
     * Inpaint original text inside a speech bubble using OpenCV Photo.inpaint.
     * Erases dark text strokes seamlessly matching background screentone/texture.
     */
    fun inpaintBubbleText(mat: Mat, box: IntArray): Mat {
        if (mat.empty() || box.size < 4) return mat
        val cols = mat.cols()
        val rows = mat.rows()

        val x1 = box[0].coerceIn(0, maxOf(0, cols - 1))
        val y1 = box[1].coerceIn(0, maxOf(0, rows - 1))
        val w = (box[2] - x1).coerceIn(1, maxOf(1, cols - x1))
        val h = (box[3] - y1).coerceIn(1, maxOf(1, rows - y1))

        val rect = Rect(x1, y1, w, h)
        val crop = mat.submat(rect)
        if (crop.empty()) {
            crop.release()
            return mat
        }

        val gray = Mat()
        val textMask = Mat()
        val inpainted = Mat()
        try {
            Imgproc.cvtColor(crop, gray, Imgproc.COLOR_RGBA2GRAY)
            val meanBrightness = Core.mean(gray).`val`[0]
            if (meanBrightness > 128) {
                // White background: threshold dark text strokes
                Imgproc.threshold(gray, textMask, 150.0, 255.0, Imgproc.THRESH_BINARY_INV)
            } else {
                // Dark background: threshold light text strokes
                Imgproc.threshold(gray, textMask, 110.0, 255.0, Imgproc.THRESH_BINARY)
            }

            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.dilate(textMask, textMask, kernel)
            kernel.release()

            val bgrCrop = Mat()
            Imgproc.cvtColor(crop, bgrCrop, Imgproc.COLOR_RGBA2BGR)
            org.opencv.photo.Photo.inpaint(bgrCrop, textMask, inpainted, 3.0, org.opencv.photo.Photo.INPAINT_TELEA)
            bgrCrop.release()

            val rgbaResult = Mat()
            Imgproc.cvtColor(inpainted, rgbaResult, Imgproc.COLOR_BGR2RGBA)
            val targetSubmat = mat.submat(rect)
            rgbaResult.copyTo(targetSubmat)
            targetSubmat.release()
            rgbaResult.release()
        } catch (e: Exception) {
            Log.w("KZKT", "Inpainting failed: ${e.message}")
        } finally {
            inpainted.release()
            textMask.release()
            gray.release()
            crop.release()
        }
        return mat
    }

    /**
     * Auto-detect if page/manga is Webtoon format (tall vertical aspect ratio > 2.2).
     */
    fun isWebtoonLayout(bitmap: Bitmap): Boolean {
        val ratio = bitmap.height.toDouble() / maxOf(1, bitmap.width)
        return ratio > 2.2
    }

    // ── Landscape Auto-Split ───────────────────────────────────────

    data class SplitResult(val partPath: String, val resultPath: String?)

    /**
     * Auto-split wide images (ratio > 1.2) into pages, processing right-to-left.
     * Returns number of splits.
     */
    fun shouldAutoSplit(bitmap: Bitmap): Int {
        val ratio = bitmap.width.toDouble() / bitmap.height
        if (ratio <= 1.2) return 1
        return maxOf(2, (ratio / 0.71).toInt())
    }
}
