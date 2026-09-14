package com.verity.core.formatting.money

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountInWordsTest {

    @Test
    fun `matches the approved invoice design's own worked example`() {
        // 29,736.00 -> from the finalized PDF design artifact (Main.dc.html)
        assertEquals(
            "Rupees Twenty Nine Thousand Seven Hundred Thirty Six Only",
            AmountInWords.forPaise(2_973_600L)
        )
    }

    @Test
    fun `matches the design's stress-test worked example, including lakh grouping`() {
        // 4,57,604.00 -> from the finalized PDF design's 10-line-item stress test artboard
        assertEquals(
            "Rupees Four Lakh Fifty Seven Thousand Six Hundred Four Only",
            AmountInWords.forPaise(45_760_400L)
        )
    }

    @Test
    fun `zero rupees`() {
        assertEquals("Rupees Zero Only", AmountInWords.forPaise(0L))
    }

    @Test
    fun `non-zero paise remainder is stated separately`() {
        assertEquals(
            "Rupees One Hundred and Fifty Paise Only",
            AmountInWords.forPaise(10_050L)
        )
    }

    @Test
    fun `crore grouping`() {
        // 1,23,45,678 - one crore twenty three lakh forty five thousand six hundred seventy eight
        assertEquals(
            "Rupees One Crore Twenty Three Lakh Forty Five Thousand Six Hundred Seventy Eight Only",
            AmountInWords.forPaise(12_345_67800L)
        )
    }

    @Test
    fun `teens are not treated as tens-plus-ones`() {
        assertEquals("Rupees Seventeen Only", AmountInWords.forPaise(1_700L))
    }

    @Test
    fun `negative amount is rejected`() {
        try {
            AmountInWords.forPaise(-1L)
            error("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
