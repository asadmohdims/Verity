package com.unitech.verity.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Verity base typography (v0).
 *
 * Editorial, calm, long-session safe.
 * System font only.
 *
 * This is a foundation, not a final brand lock.
 */
val VerityBaseTypography = VerityTypography(

    display = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold
    ),

    title = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Medium
    ),

    body = numericTextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ).copy(
        lineHeight = 24.sp
    ),

    label = numericTextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ).copy(
        lineHeight = 20.sp
    ),

    caption = numericTextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ).copy(
        lineHeight = 16.sp
    )
)