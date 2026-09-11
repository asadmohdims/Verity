package com.verity.core.formatting.money

/**
 * Parses a decimal rupee amount typed by the user (e.g. "1234", "1234.5", "1234.50") into paise
 * using integer/string arithmetic only.
 *
 * This exists because `(input.toDouble() * 100).toLong()` is exposed to float rounding error —
 * e.g. 2.90 is not exactly representable as a Double, and `2.90 * 100` can land at
 * 289.99999999999994, which truncates to 289 paise instead of 290. CLAUDE.md's money-path rule
 * ("never Double, never floating point, anywhere in the money path") rules that out.
 *
 * Returns 0 for blank or malformed input, matching this project's existing UI fallback behavior
 * for an in-progress, not-yet-valid text field.
 */
fun parseRupeesInputToPaise(input: String): Long {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return 0L

    val parts = trimmed.split(".")
    if (parts.size > 2) return 0L

    val rupees = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return 0L
    if (rupees < 0L) return 0L

    val paiseFraction = if (parts.size == 2) {
        val fraction = parts[1]
        if (fraction.isEmpty() || !fraction.all { it.isDigit() }) return 0L
        fraction.padEnd(2, '0').take(2).toLong()
    } else {
        0L
    }

    return rupees * 100 + paiseFraction
}

/**
 * Formats paise as a plain decimal rupee string suitable for pre-filling an editable text field
 * (e.g. 290 -> "2.90", 500 -> "5"). This is an input-editing helper, not a display formatter —
 * use Money.format() / Money.formatPlain() to render an amount to the user.
 */
fun formatPaiseAsRupeesInput(paise: Long): String {
    val rupees = paise / 100
    val remainder = paise % 100
    return if (remainder == 0L) rupees.toString() else "$rupees.${remainder.toString().padStart(2, '0')}"
}
