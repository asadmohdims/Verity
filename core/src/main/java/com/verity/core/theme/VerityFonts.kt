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
 *
 * This enforces hierarchy discipline and avoids visual noise.
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
    )
)