package com.verity.core.document.model

/**
 * Seller identity printed on every finalized document.
 *
 * TEMPORARY (Milestone 1): there is no seller/organization profile screen yet, so this is a
 * hardcoded constant rather than user-editable data. A settings screen (Business Profile, Phase 3
 * of the UX roadmap) can replace this later without touching anything that consumes it.
 *
 * Most values below are the real business details for Unitech Machineries, sourced from the
 * manual invoice the user reviewed during the PDF design pass (2026-09-14) - not fabricated.
 * PINCODE is the one field that source didn't state and still needs a real value; everything
 * else here should be double-checked against the real business but is not a placeholder.
 */
val HARDCODED_SELLER = SellerDetails(
    name = "UNITECH MACHINERIES",
    gstin = "09AFFPA0657R2Z3",
    addressLine1 = "Zakir Colony, Sherpur Road",
    addressLine2 = null,
    city = "Muzaffarnagar",
    state = "Uttar Pradesh",
    stateCode = "09",
    pincode = "PLACEHOLDER_PINCODE",
    tagline = "Manufacturers of Paper Mill & Sugar Mill Machinery",
    email = "amil.unitech@gmail.com",
    phone = "+91 98370 06677, 97600066777, 89380 06677",
    bankName = "Punjab and Sind Bank (Roorkee Road) (MZN)",
    bankAccountNumber = "02561600000395",
    bankIfsc = "PSIB0000256",
    msmeOrUamNumber = "UP58A0008598",
    termsAndConditions = listOf(
        "All disputes are subject to Muzaffarnagar court jurisdiction.",
        "Goods once sold cannot be returned without prior confirmation.",
        "All goods shall be booked at purchaser's risk & in case of any damage, loss or " +
            "pilferage in transit, the customer shall have to claim directly to the carrier " +
            "concerned."
    )
)
