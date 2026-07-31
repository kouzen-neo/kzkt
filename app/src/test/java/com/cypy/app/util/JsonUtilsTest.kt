package com.cypy.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonUtilsTest {

    @Test
    fun `duplicate keys fall back to tolerant parse and keep first value`() {
        val json = """{"5_1":"satu","5_1":"dua","6_2":"tiga"}"""
        val result = JsonUtils.parseTranslationMap(json)
        assertEquals(2, result.size)
        assertEquals("satu", result["5_1"])   // first occurrence wins
        assertEquals("tiga", result["6_2"])
    }

    @Test
    fun `normal json parses strictly`() {
        val json = """{"1":"hai","2_1":"selamat pagi","3":"tes"}"""
        val result = JsonUtils.parseTranslationMap(json)
        assertEquals(3, result.size)
        assertEquals("hai", result["1"])
        assertEquals("selamat pagi", result["2_1"])
    }

    @Test
    fun `escaped quotes inside values survive the tolerant scan`() {
        val json = """{"1":"he said \"hi\" to me","2":"ok"}"""
        val result = JsonUtils.parseTranslationMap(json)
        assertEquals("he said \"hi\" to me", result["1"])
        assertEquals("ok", result["2"])
    }

    @Test
    fun `duplicate keys nested inside other objects are ignored`() {
        // The duplicate is deep inside a nested object (not our map) — must not be returned.
        val json = """{"1":"a","meta":{"1":"nested"},"2":"b"}"""
        val result = JsonUtils.parseTranslationMap(json)
        assertEquals(2, result.size)
        assertEquals("a", result["1"])
        assertEquals("b", result["2"])
        assertNull(result["meta"])
    }

    @Test
    fun `sanitize strips markdown fences and junk`() {
        val raw = """
            ```json
            {"1":"a"}
            ```
        """.trimIndent()
        val result = JsonUtils.parseTranslationMap(JsonUtils.sanitizeJson(raw))
        assertEquals(1, result.size)
        assertEquals("a", result["1"])
    }

    @Test
    fun `empty or garbage input yields empty map, not crash`() {
        assertTrue(JsonUtils.parseTranslationMap("").isEmpty())
        assertTrue(JsonUtils.parseTranslationMap("not json at all").isEmpty())
        assertTrue(JsonUtils.parseTranslationMap("null").isEmpty())
    }
}
