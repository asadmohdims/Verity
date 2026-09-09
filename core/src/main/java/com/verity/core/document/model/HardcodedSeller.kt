package com.verity.core.document.model

/**
 * Seller identity printed on every finalized document.
 *
 * TEMPORARY (Milestone 1): there is no seller/organization profile screen yet, so this is a
 * hardcoded constant rather than user-editable data. Replace the placeholder values below with
 * your real business details — GSTIN and pincode in particular are non-optional on a real GST
 * invoice. A settings screen can replace this later without touching anything that consumes it.
 */
val HARDCODED_SELLER = SellerDetails(
    name = "PLACEHOLDER BUSINESS NAME",
    gstin = "27AAAAA0000A1Z5",
    addressLine1 = "PLACEHOLDER ADDRESS LINE 1",
    addressLine2 = null,
    city = "PLACEHOLDER CITY",
    state = "PLACEHOLDER STATE",
    stateCode = "27",
    pincode = "000000"
)
