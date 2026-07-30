package com.cypy.app.core

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.ceil

/**
 * Rate limiter with retry and exponential backoff.
 * Ported from cypy/core/services/rate_limiter.py
 */
class RateLimiter(
    private val minRequestDelayMs: Long = 2000L,
) {
    private var lastCallTimeMs: Long = 0L

    private val lock = Any()

    suspend fun waitForSlot() {
        if (minRequestDelayMs <= 0) return
        val sleepTime = synchronized(lock) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastCallTimeMs
            if (elapsed < minRequestDelayMs) {
                minRequestDelayMs - elapsed
            } else 0L
        }
        if (sleepTime > 0) {
            delay(sleepTime)  // cancellable — STOP works instantly
        }
        synchronized(lock) {
            lastCallTimeMs = System.currentTimeMillis()
        }
    }

    suspend fun <T> executeWithRetry(
        apiCall: suspend () -> T,
        providerName: String = "Provider",
        maxRetries: Int = 3,
        isCancelled: () -> Boolean = { false },
        onWait: ((String) -> Unit)? = null,
    ): T? {
        waitForSlot()

        for (attempt in 0 until maxRetries) {
            if (isCancelled()) return null

            try {
                synchronized(lock) { lastCallTimeMs = System.currentTimeMillis() }
                return apiCall()
            } catch (ex: Exception) {
                val errStr = ex.message?.lowercase() ?: ""

                // ── 1. Rate Limit (HTTP 429) ──
                if (errStr.contains("429") ||
                    errStr.contains("too many requests") ||
                    errStr.contains("rate limit") ||
                    errStr.contains("quota exceeded")
                ) {
                    var waitSeconds = 5.0 * (2.0.pow(attempt))
                    // Try to parse retry-after from message
                    val match = Regex("retry in (\\d+(?:\\.\\d+)?)s", RegexOption.IGNORE_CASE)
                        .find(errStr)
                    if (match != null) {
                        val parsed = match.groupValues[1].toDoubleOrNull()
                        if (parsed != null) waitSeconds = parsed + 1.5
                    }

                    if (attempt == maxRetries - 1) throw ex

                    val totalSecs = ceil(waitSeconds).toInt()
                    val msg = "[Rate Limit] Hit rate limit for ${providerName}. Retrying in ${totalSecs}s..."
                    Log.w("CYPY", msg)
                    onWait?.invoke(msg)

                    for (s in totalSecs downTo 1) {
                        if (isCancelled()) return null
                        delay(1000L)
                    }
                    continue
                }

                // ── 2. Transient network / timeout ──
                val isTimeout = errStr.contains("timeout") ||
                    errStr.contains("timed out") ||
                    errStr.contains("connection") ||
                    errStr.contains("socket") ||
                    errStr.contains("reset")

                if (isTimeout) {
                    if (attempt == maxRetries - 1) throw ex
                    val msg = "[Network Warning] Attempt ${attempt + 1}/${maxRetries} for $providerName timed out. Retrying in 2s..."
                    Log.w("CYPY", msg)
                    onWait?.invoke(msg)
                    delay(2000L)
                    continue
                }

                // Non-retryable or last attempt
                if (attempt == maxRetries - 1) throw ex
                delay(3000L)
            }
        }
        return null
    }

    private fun Double.pow(exp: Int): Double {
        var result = 1.0
        repeat(exp) { result *= this }
        return result
    }
}
