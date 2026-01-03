package com.verity.core.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.verity.core.R

/**
 * VerityFontFamily
 *
 * Primary typeface for Verity.
 *
 * Typeface: Inter (static)
 *
 * Rationale:
 * - Designed for UI and dense information
 * - Excellent numeric clarity
 * - Neutral, professional fintech tone
 *
 * We intentionally restrict weights to:
 * - Regular (400)
 * - Medium (500)
 * - SemiBold (600)
 * - ExtraBold (800) — identity-only (application chrome)
 *
 * ExtraBold is reserved exclusively for application identity
 * (e.g. Top App Bar title) and must never be used in
 * document, form, or financial content.
 *
 * This enforces hierarchy discipline while allowing
 * a strong, recognisable Verity identity.
 */
val VerityFontFamily = FontFamily(
    Font(
        resId = R.font.inter_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.inter_medium,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.inter_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.inter_extrabold,
        weight = FontWeight.ExtraBold
    )
)