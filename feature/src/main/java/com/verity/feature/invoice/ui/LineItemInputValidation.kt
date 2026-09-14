package com.verity.feature.invoice.ui

import com.verity.core.formatting.money.parseRupeesInputToPaise

/**
 * Validity of the line-item Add/Edit form's raw text input, before it's parsed into a
 * [com.verity.feature.invoice.draft.DraftLineItem].
 *
 * Quantity is optional — a blank field is valid on its own (e.g. a job-work line with nothing to
 * count) and commits as a genuinely absent quantity, not a fabricated value. It only becomes
 * invalid once the user types something that isn't a positive whole number.
 *
 * [showQuantityError] / [showRateError] are deliberately false while the corresponding field is
 * still blank (a fresh, untouched field shouldn't show a red error), even though [canSubmit] is
 * also false in that state for a blank Rate (Rate, unlike Quantity, is always required).
 */
data class LineItemInputValidation(
    val isDescriptionValid: Boolean,
    val isQuantityValid: Boolean,
    val isRateValid: Boolean,
    val showQuantityError: Boolean,
    val showRateError: Boolean
) {
    val canSubmit: Boolean
        get() = isDescriptionValid && isQuantityValid && isRateValid
}

fun validateLineItemInput(
    description: String,
    quantityInput: String,
    rateInput: String
): LineItemInputValidation {
    val isDescriptionValid = description.isNotBlank()

    val quantity = quantityInput.toLongOrNull()
    val isQuantityValid = quantityInput.isBlank() || (quantity != null && quantity > 0)

    val ratePaise = parseRupeesInputToPaise(rateInput)
    val isRateValid = ratePaise > 0

    return LineItemInputValidation(
        isDescriptionValid = isDescriptionValid,
        isQuantityValid = isQuantityValid,
        isRateValid = isRateValid,
        showQuantityError = quantityInput.isNotBlank() && !isQuantityValid,
        showRateError = rateInput.isNotBlank() && !isRateValid
    )
}
