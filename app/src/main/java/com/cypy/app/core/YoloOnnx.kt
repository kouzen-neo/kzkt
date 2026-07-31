package com.cypy.app.core

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import java.util.UUID

/**
 * YOLOv8 ONNX inference via ONNX Runtime Android.
 */
class YoloOnnx(
    private val context: Context,
    private val modelFilename: String = "eyecypy.dat",
    private val confThreshold: Double = 0.25,
    private val iouThreshold: Double = 0.45,
) {
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var modelSize: Int = 0

    data class Detection(val x1: Int, val y1: Int, val x2: Int, val y2: Int)

    fun initialize(): Boolean {
        try {
            Log.d("CYPY/YOLO", "=== INIT START ===")
            Log.d("CYPY/YOLO", "confThreshold=$confThreshold iouThreshold=$iouThreshold")

            val onnxFile = decryptModel()
            if (onnxFile == null || !onnxFile.exists()) {
                Log.e("CYPY/YOLO", "decryptModel returned null or file not found")
                return false
            }
            Log.d("CYPY/YOLO", "ONNX file ready: ${onnxFile.absolutePath} (${onnxFile.length()} bytes)")

            Log.d("CYPY/YOLO", "Getting OrtEnvironment...")
            ortEnv = OrtEnvironment.getEnvironment()
            Log.d("CYPY/YOLO", "OrtEnvironment OK")

            Log.d("CYPY/YOLO", "Creating SessionOptions...")
            val opts = OrtSession.SessionOptions()
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            Log.d("CYPY/YOLO", "Creating session from ${onnxFile.absolutePath}...")
            ortSession = ortEnv!!.createSession(onnxFile.absolutePath, opts)
            Log.d("CYPY/YOLO", "Session created OK")

            // Log input/output info
            val inputInfo = ortSession!!.inputNames
            val outputInfo = ortSession!!.outputNames
            Log.d("CYPY/YOLO", "Inputs: $inputInfo")
            Log.d("CYPY/YOLO", "Outputs: $outputInfo")

            Log.d("CYPY/YOLO", "=== INIT SUCCESS ===")
            return true
        } catch (e: UnsatisfiedLinkError) {
            Log.e("CYPY/YOLO", "Native library not loaded: ${e.message}")
            e.printStackTrace()
            return false
        } catch (e: ai.onnxruntime.OrtException) {
            Log.e("CYPY/YOLO", "ONNX Runtime error: ${e.message}")
            e.printStackTrace()
            return false
        } catch (e: Exception) {
            Log.e("CYPY/YOLO", "Failed: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun decryptModel(): File? {
        val onnxDirect = File(context.cacheDir, "eyecypy.onnx")

        // Check cached file validity — delete if corrupt (wrong key = wrong header)
        if (onnxDirect.exists()) {
            val cachedBytes = onnxDirect.readBytes()
            val header = cachedBytes.take(4).map { it.toInt().toChar() }.joinToString("")
            if (header == "ONNX") {
                Log.d("CYPY/YOLO", "Using cached ONNX: ${onnxDirect.length()} bytes (header OK)")
                return onnxDirect
            } else {
                Log.w("CYPY/YOLO", "Cached file header is \"$header\", expected ONNX — deleting corrupt cache")
                onnxDirect.delete()
            }
        }

        return try {
            Log.d("CYPY/YOLO", "Reading assets/models/$modelFilename ...")
            val inputStream = context.assets.open("models/$modelFilename")
            val encrypted = inputStream.readBytes()
            inputStream.close()
            Log.d("CYPY/YOLO", "Read ${encrypted.size} bytes from asset")

            val key = Constants.MODEL_DECRYPT_KEY
            Log.d("CYPY/YOLO", "XOR decrypting with key=$key ...")
            val decrypted = ByteArray(encrypted.size)
            for (i in encrypted.indices) {
                decrypted[i] = (encrypted[i].toInt() xor key).toByte()
            }
            Log.d("CYPY/YOLO", "Decrypted ${decrypted.size} bytes")

            // Verify it looks like an ONNX file (starts with "ONNX")
            val header = decrypted.take(4).map { it.toInt().toChar() }.joinToString("")
            Log.d("CYPY/YOLO", "File header: \"$header\" (expected \"ONNX\")")
            if (header != "ONNX") {
                Log.w("CYPY/YOLO", "Header mismatch after XOR — key may be wrong")
            }

            val tag = UUID.randomUUID().toString().take(8)
            val modelFile = File(context.cacheDir, "eyecypy_$tag.onnx")
            modelFile.writeBytes(decrypted)
            Log.d("CYPY/YOLO", "Written to ${modelFile.absolutePath}")

            if (modelFile.exists()) {
                modelSize = modelFile.length().toInt()
                modelFile.copyTo(onnxDirect, overwrite = true)
            } else {
                return null
            }

            // Clean up stale temp files from interrupted previous runs (keep the cached one)
            try {
                context.cacheDir.listFiles()?.forEach { f ->
                    val name = f.name
                    if (name.startsWith("eyecypy_") && name.endsWith(".onnx") && name != modelFile.name) {
                        f.delete()
                    }
                }
            } catch (_: Exception) {}

            modelFile
        } catch (e: Exception) {
            Log.e("CYPY/YOLO", "Decryption failed: ${e.message}", e)
            null
        }
    }

    private fun prepareInput(bitmap: Bitmap): Triple<FloatArray, DoubleArray, DoubleArray> {
        val h = bitmap.height.toDouble()
        val w = bitmap.width.toDouble()
        val targetSize = Constants.YOLO_INPUT_SIZE.toDouble()

        val scale = minOf(targetSize / h, targetSize / w)
        val newW = (w * scale).toInt()
        val newH = (h * scale).toInt()

        val dw = ((targetSize - newW) / 2.0).toInt()
        val dh = ((targetSize - newH) / 2.0).toInt()

        Log.d("CYPY/YOLO", "prepareInput: img=${w.toInt()}x${h} scale=%.3f new=${newW}x${newH} pad=${dw}x${dh}".format(scale))

        val resized = Bitmap.createScaledBitmap(bitmap, newW, newH, true)

        val padded = Bitmap.createBitmap(Constants.YOLO_INPUT_SIZE, Constants.YOLO_INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(padded)
        canvas.drawColor(android.graphics.Color.rgb(114, 114, 114))
        canvas.drawBitmap(resized, dw.toFloat(), dh.toFloat(), null)

        val pixels = IntArray(Constants.YOLO_INPUT_SIZE * Constants.YOLO_INPUT_SIZE)
        padded.getPixels(pixels, 0, Constants.YOLO_INPUT_SIZE, 0, 0, Constants.YOLO_INPUT_SIZE, Constants.YOLO_INPUT_SIZE)
        padded.recycle()
        resized.recycle()

        val inputData = FloatArray(3 * Constants.YOLO_INPUT_SIZE * Constants.YOLO_INPUT_SIZE)
        val area = Constants.YOLO_INPUT_SIZE * Constants.YOLO_INPUT_SIZE
        for (i in pixels.indices) {
            val pixel = pixels[i]
            inputData[i] = ((pixel shr 16) and 0xFF) / 255.0f
            inputData[area + i] = ((pixel shr 8) and 0xFF) / 255.0f
            inputData[2 * area + i] = (pixel and 0xFF) / 255.0f
        }

        return Triple(inputData, doubleArrayOf(scale, scale), doubleArrayOf(dw.toDouble(), dh.toDouble()))
    }

    fun predict(bitmap: Bitmap, confThreshold: Double = this.confThreshold, iouThreshold: Double = this.iouThreshold): List<Detection> {
        val env = ortEnv ?: throw IllegalStateException("ONNX Runtime not initialized")
        val session = ortSession ?: throw IllegalStateException("Model not loaded")

        val (inputData, ratios, paddings) = prepareInput(bitmap)
        val (dw, dh) = paddings[0] to paddings[1]
        val (ratioW, ratioH) = ratios[0] to ratios[1]

        val inputShape = longArrayOf(1, 3, Constants.YOLO_INPUT_SIZE.toLong(), Constants.YOLO_INPUT_SIZE.toLong())
        val inputName = session.inputNames.iterator().next()

        Log.d("CYPY/YOLO", "Running inference on ${bitmap.width}x${bitmap.height}...")
        // inputTensor & result hold native C++ handles — must be closed to avoid leaking memory on every page.
        // The output tensor's floatBuffer is a DIRECT buffer into native memory that becomes invalid once the
        // result is closed, so copy it to a heap array while still inside the use{} block.
        val outputData: FloatArray
        OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), inputShape).use { inputTensor ->
            session.run(mapOf(inputName to inputTensor)).use { result ->
                val onnxVal = result.get(0) as OnnxTensor
                val fb = onnxVal.floatBuffer
                outputData = FloatArray(fb.capacity())
                fb.get(outputData)
            }
        }
        val bufSize = outputData.size
        Log.d("CYPY/YOLO", "Output buffer size: $bufSize floats")

        // Determine grid dimensions from buffer size
        // Expected: 84 * 8400 = 705600. Could also be 8400 * 5 = 42000 (old model) or 8400 * 6 = 50400
        val grid: Int
        val channels: Int
        when {
            bufSize % 8400 == 0 -> { grid = 8400; channels = bufSize / 8400 }
            bufSize % 840 == 0 -> { grid = 840; channels = bufSize / 840 }
            else -> {
                Log.e("CYPY/YOLO", "Unknown output shape - buffer size $bufSize not divisible by 8400 or 840")
                return emptyList()
            }
        }
        Log.d("CYPY/YOLO", "Output shape: $channels x $grid (expected 84 x 8400)")

        // If it's transposed (grid, channels), transpose index access
        // Normal ONNX output is (1, 84, 8400) = data[c * grid + g]
        // Transposed would be (1, 8400, 84) = data[g * channels + c]
        // Detect by looking at conf value at position [4 * grid + 0] vs [0 * channels + 4]
        val sampleConfDirect = if (bufSize > 4 * grid) outputData[4 * grid] else -1f
        val sampleConfTransposed = if (bufSize > 4) outputData[4] else -1f
        val confIsValid = sampleConfDirect in 0.01f..1.0f
        val transposedConfIsValid = sampleConfTransposed in 0.01f..1.0f

        val transposed: Boolean
        if (confIsValid && !transposedConfIsValid) {
            transposed = false
        } else if (!confIsValid && transposedConfIsValid) {
            transposed = true
        } else if (confIsValid && transposedConfIsValid) {
            // Both look plausible — check if channel 5+ has reasonable values when accessed transposed
            transposed = outputData[5] in 0.0f..1.0f && outputData[4 * grid + 5] !in 0.0f..1.0f
        } else {
            transposed = false
        }

        if (transposed) {
            Log.d("CYPY/YOLO", "Detected transposed output (grid x channels). Adapting lookup.")
        }

        val detections = mutableListOf<Detection>()
        val boxes = mutableListOf<IntArray>()
        val confidences = mutableListOf<Float>()

        val outH = bitmap.height.toDouble()
        val outW = bitmap.width.toDouble()

        for (g in 0 until grid) {
            val conf: Float
            val xc: Double
            val yc: Double
            val boxW: Double
            val boxH: Double

            if (transposed) {
                conf = outputData[g * channels + 4]
                xc = outputData[g * channels + 0].toDouble()
                yc = outputData[g * channels + 1].toDouble()
                boxW = outputData[g * channels + 2].toDouble()
                boxH = outputData[g * channels + 3].toDouble()
            } else {
                conf = outputData[4 * grid + g]
                xc = outputData[0 * grid + g].toDouble()
                yc = outputData[1 * grid + g].toDouble()
                boxW = outputData[2 * grid + g].toDouble()
                boxH = outputData[3 * grid + g].toDouble()
            }

            if (conf < confThreshold) continue

            var x1 = (xc - boxW / 2.0 - dw) / ratioW
            var y1 = (yc - boxH / 2.0 - dh) / ratioH
            var x2 = (xc + boxW / 2.0 - dw) / ratioW
            var y2 = (yc + boxH / 2.0 - dh) / ratioH

            x1 = x1.coerceIn(0.0, outW)
            y1 = y1.coerceIn(0.0, outH)
            x2 = x2.coerceIn(0.0, outW)
            y2 = y2.coerceIn(0.0, outH)

            val bw = (x2 - x1).toInt()
            val bh = (y2 - y1).toInt()
            if (bw <= 0 || bh <= 0) continue

            boxes.add(intArrayOf(x1.toInt(), y1.toInt(), bw, bh))
            confidences.add(conf)
        }

        Log.d("CYPY/YOLO", "Pre-NMS: ${boxes.size} boxes, top conf=${confidences.maxOrNull()}")

        if (boxes.isNotEmpty()) {
            val order = confidences.indices.sortedByDescending { confidences[it] }
            val keep = mutableListOf<Int>()

            for (i in order) {
                val keepFlag = keep.all { k ->
                    val a = boxes[i]; val b = boxes[k]
                    val ix1 = maxOf(a[0], b[0]); val iy1 = maxOf(a[1], b[1])
                    val ix2 = minOf(a[0] + a[2], b[0] + b[2])
                    val iy2 = minOf(a[1] + a[3], b[1] + b[3])
                    val inter = maxOf(0, ix2 - ix1) * maxOf(0, iy2 - iy1)
                    val iou = inter.toDouble() / (a[2] * a[3] + b[2] * b[3] - inter)
                    iou <= iouThreshold
                }
                if (keepFlag) keep.add(i)
            }

            for (i in keep) {
                val b = boxes[i]
                detections.add(Detection(b[0], b[1],
                    (b[0] + b[2]).coerceAtMost(bitmap.width),
                    (b[1] + b[3]).coerceAtMost(bitmap.height)))
            }
        }

        Log.d("CYPY/YOLO", "Post-NMS: ${detections.size} detections")
        return detections
    }

    fun close() {
        Log.d("CYPY/YOLO", "Closing...")
        try { ortSession?.close() } catch (_: Exception) {}
        ortEnv?.close()
    }
}
