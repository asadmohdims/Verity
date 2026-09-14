package com.verity.core.formatting.money

/**
 * AmountInWords
 *
 * "Rupees ... Only" statement customary on Indian invoices/GST documents, stating the grand
 * total in words using the Indian numbering system (lakh/crore grouping), not the Western
 * thousand/million/billion grouping.
 */
object AmountInWords {

    private val ones = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    /** e.g. 2973600L (paise) -> "Rupees Twenty Nine Thousand Seven Hundred Thirty Six Only" */
    fun forPaise(paise: Long): String {
        require(paise >= 0) { "Amount in words is undefined for a negative amount" }

        val rupees = paise / 100
        val remainderPaise = paise % 100

        val rupeeWords = if (rupees == 0L) "Zero" else convertIndianGrouped(rupees)

        return if (remainderPaise == 0L) {
            "Rupees $rupeeWords Only"
        } else {
            val paiseWords = convertBelowThousand(remainderPaise.toInt())
            "Rupees $rupeeWords and $paiseWords Paise Only"
        }
    }

    /** Converts using Indian grouping: crore (1,00,00,000) / lakh (1,00,000) / thousand / below-1000. */
    private fun convertIndianGrouped(number: Long): String {
        var remaining = number
        val parts = mutableListOf<String>()

        val crore = remaining / 10_000_000
        remaining %= 10_000_000
        if (crore > 0) parts += "${convertIndianGrouped(crore)} Crore"

        val lakh = remaining / 100_000
        remaining %= 100_000
        if (lakh > 0) parts += "${convertBelowThousand(lakh.toInt())} Lakh"

        val thousand = remaining / 1_000
        remaining %= 1_000
        if (thousand > 0) parts += "${convertBelowThousand(thousand.toInt())} Thousand"

        if (remaining > 0) parts += convertBelowThousand(remaining.toInt())

        return parts.joinToString(" ")
    }

    private fun convertBelowThousand(number: Int): String {
        require(number in 0..999)

        val hundreds = number / 100
        val remainder = number % 100

        val parts = mutableListOf<String>()
        if (hundreds > 0) parts += "${ones[hundreds]} Hundred"

        if (remainder in 1..19) {
            parts += ones[remainder]
        } else if (remainder >= 20) {
            val tensPart = tens[remainder / 10]
            val onesPart = ones[remainder % 10]
            parts += if (onesPart.isEmpty()) tensPart else "$tensPart $onesPart"
        }

        return parts.joinToString(" ")
    }
}
