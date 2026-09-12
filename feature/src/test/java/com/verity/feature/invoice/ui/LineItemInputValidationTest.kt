package com.verity.feature.invoice.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineItemInputValidationTest {

    @Test
    fun `fully valid input can submit`() {
        val result = validateLineItemInput(
            description = "Cotton fabric",
            quantityInput = "10",
            rateInput = "125.50"
        )

        assertTrue(result.canSubmit)
        assertFalse(result.showQuantityError)
        assertFalse(result.showRateError)
    }

    @Test
    fun `completely empty input cannot submit but shows no errors yet`() {
        val result = validateLineItemInput(
            description = "",
            quantityInput = "",
            rateInput = ""
        )

        assertFalse(result.canSubmit)
        assertFalse(result.showQuantityError)
        assertFalse(result.showRateError)
    }

    @Test
    fun `blank description cannot submit`() {
        val result = validateLineItemInput(
            description = "   ",
            quantityInput = "10",
            rateInput = "100"
        )

        assertFalse(result.isDescriptionValid)
        assertFalse(result.canSubmit)
    }

    @Test
    fun `zero quantity cannot submit and shows an error`() {
        val result = validateLineItemInput(
            description = "Item",
            quantityInput = "0",
            rateInput = "100"
        )

        assertFalse(result.canSubmit)
        assertTrue(result.showQuantityError)
    }

    @Test
    fun `non numeric quantity cannot submit and shows an error`() {
        val result = validateLineItemInput(
            description = "Item",
            quantityInput = "abc",
            rateInput = "100"
        )

        assertFalse(result.canSubmit)
        assertTrue(result.showQuantityError)
    }

    @Test
    fun `zero rate cannot submit and shows an error`() {
        val result = validateLineItemInput(
            description = "Item",
            quantityInput = "10",
            rateInput = "0"
        )

        assertFalse(result.canSubmit)
        assertTrue(result.showRateError)
    }

    @Test
    fun `malformed rate cannot submit and shows an error`() {
        val result = validateLineItemInput(
            description = "Item",
            quantityInput = "10",
            rateInput = "12.34.56"
        )

        assertFalse(result.canSubmit)
        assertTrue(result.showRateError)
    }

    @Test
    fun `negative quantity cannot submit and shows an error`() {
        val result = validateLineItemInput(
            description = "Item",
            quantityInput = "-5",
            rateInput = "100"
        )

        assertFalse(result.canSubmit)
        assertTrue(result.showQuantityError)
    }
}
