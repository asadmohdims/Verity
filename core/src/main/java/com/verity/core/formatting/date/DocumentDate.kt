package com.verity.core.formatting.date

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * DocumentDate
 *
 * Shared display formatting for dates on invoice/challan documents and their PDFs — e.g.
 * "14 Sep 2026". Screens previously each defined their own inline DateTimeFormatter for this
 * same "dd MMM yyyy" pattern; centralized here so the PDF renderer and every screen stay
 * consistent by construction rather than by convention.
 */
object DocumentDate {

    private val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun format(date: LocalDate): String = date.format(displayFormatter)
}
