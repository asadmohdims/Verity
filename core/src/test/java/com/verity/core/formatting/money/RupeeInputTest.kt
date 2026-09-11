package com.verity.core.formatting.money

import org.junit.Assert.assertEquals
import org.junit.Test

class RupeeInputTest {

    @Test
    fun `whole rupee amount parses to paise`() {
        assertEquals(290000L, parseRupeesInputToPaise("2900"))
    }

    @Test
    fun `decimal rupee amount parses without float rounding error`() {
        // A naive (input.toDouble() * 100).toLong() truncates this to 289 paise on some inputs
        // because 2.90 is not exactly representable as a Double. This must land exactly on 290.
        assertEquals(290L, parseRupeesInputToPaise("2.90"))
    }

    @Test
    fun `single decimal digit is treated as tenths`() {
        assertEquals(250L, parseRupeesInputToPaise("2.5"))
    }

    @Test
    fun `blank input parses to zero`() {
        assertEquals(0L, parseRupeesInputToPaise(""))
        assertEquals(0L, parseRupeesInputToPaise("   "))
    }

    @Test
    fun `malformed input parses to zero`() {
        assertEquals(0L, parseRupeesInputToPaise("abc"))
        assertEquals(0L, parseRupeesInputToPaise("1.2.3"))
        assertEquals(0L, parseRupeesInputToPaise("1.ab"))
        assertEquals(0L, parseRupeesInputToPaise("-5"))
    }

    @Test
    fun `leading dot is treated as zero rupees`() {
        assertEquals(50L, parseRupeesInputToPaise(".5"))
    }

    @Test
    fun `formatting paise back to rupees input round trips whole amounts`() {
        assertEquals("29", formatPaiseAsRupeesInput(2900L))
    }

    @Test
    fun `formatting paise back to rupees input round trips fractional amounts`() {
        assertEquals("2.90", formatPaiseAsRupeesInput(290L))
    }

    @Test
    fun `formatting zero paise yields zero`() {
        assertEquals("0", formatPaiseAsRupeesInput(0L))
    }
}
