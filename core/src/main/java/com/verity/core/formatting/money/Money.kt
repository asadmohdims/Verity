package com.verity.core.formatting.money

/**
 * Money
 *
 * PURPOSE
 * -------
 * Canonical representation of monetary values in Verity.
 *
 * This type is:
 * - Immutable
 * - UI-agnostic
 * - Currency-aware (INR only, v1)
 *
 * DESIGN PRINCIPLES
 * -----------------
 * - Money is factual, not emotional
 * - Formatting is centralized and consistent
 * - UI must NEVER format raw numbers
 * - Optimized for rupee-denominated invoices (paise supported but secondary)
 *
 * IMPORTANT
 * ---------
 * - Internally backed by Long (paise)
 * - Rupee-first API for invoice-centric usage
 * - May migrate to BigDecimal internally without changing callers
 */
@JvmInline
value class Money private constructor(
    val raw: Long
) {

    init {
        require(raw >= 0L) {
            "Money amount cannot be negative in v1 (raw=$raw)"
        }
    }

    /**
     * Formats money with currency symbol.
     *
     * Examples:
     * - ₹1452
     * - ₹3200
     */
    fun format(): String =
        "₹${formatPaise(raw)}"

    /**
     * Formats money without currency symbol.
     *
     * Used in tight layouts or internal summaries.
     *
     * Examples:
     * - 1452
     * - 3200
     *
     * @param alwaysTwoDecimals GST/printed-document convention always states two decimal places
     *   (e.g. "25,200.00"), unlike the default UI convention of omitting ".00" when exact.
     */
    fun formatPlain(alwaysTwoDecimals: Boolean = false): String =
        formatPaise(raw, alwaysTwoDecimals)

    override fun toString(): String =
        format()

    companion object {

        /** Primary factory for invoices (₹ as whole rupees). */
        fun ofRupees(rupees: Long): Money =
            Money(rupees * 100)


        /** Secondary factory for precision cases (stored as paise). */
        fun ofPaise(paise: Long): Money =
            Money(paise)

        private fun formatRupeesWithGrouping(value: Long): String {
            val str = value.toString()
            if (str.length <= 3) return str

            val lastThree = str.takeLast(3)
            val remaining = str.dropLast(3)

            val groupedRemaining = remaining
                .reversed()
                .chunked(2)
                .joinToString(",")
                .reversed()

            return "$groupedRemaining,$lastThree"
        }

        private fun formatPaise(paise: Long, alwaysTwoDecimals: Boolean = false): String {
            val rupees = paise / 100
            val remainder = paise % 100

            val groupedRupees = formatRupeesWithGrouping(rupees)

            return if (remainder == 0L && !alwaysTwoDecimals) {
                groupedRupees
            } else {
                "$groupedRupees.${remainder.toString().padStart(2, '0')}"
            }
        }
    }
}