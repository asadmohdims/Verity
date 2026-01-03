package com.verity.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Verity Typography v1
 *
 * Financial, calm, high-density, audit-safe.
 * Optimized for invoices and long reading sessions.
 *
 * Principles:
 * - Weight-driven hierarchy (not size-heavy)
 * - Tabular numerals for numeric stability
 * - Tight but breathable vertical rhythm
 * - No decorative or context-specific styles
 *
 * This replaces the earlier v0 typography.
 */
val VerityBaseTypography = VerityTypography(

    // Large anchors only (screen headers, totals)
    display = TextStyle(
        fontFamily = VerityFontFamily,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.01).sp
    ),

    // Application chrome identity (Top App Bar, app-level anchors)
    chromeTitle = TextStyle(
        fontFamily = VerityFontFamily,
        fontSize = 26.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.005).sp
    ),

    // Section titles, important headings
    title = TextStyle(
        fontFamily = VerityFontFamily,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold
    ),

    // Primary reading text, descriptions
    body = numericTextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ).copy(
        lineHeight = 22.sp
    ),

    // Labels, secondary facts, micro-metrics
    label = numericTextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    ).copy(
        lineHeight = 18.sp
    ),

    // Metadata, hints, tertiary information
    caption = numericTextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    ).copy(
        lineHeight = 14.sp,
        letterSpacing = 0.02.sp
    )
)