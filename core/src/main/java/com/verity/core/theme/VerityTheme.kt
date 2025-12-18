package com.verity.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal holding Verity semantic colors.
 */
val LocalVerityColors = staticCompositionLocalOf<VerityColors> {
    error("VerityColors not provided. Did you forget to wrap your UI in VerityTheme?")
}

/**
 * CompositionLocal holding Verity semantic typography.
 */
val LocalVerityTypography = staticCompositionLocalOf<VerityTypography> {
    error("VerityTypography not provided. Did you forget to wrap your UI in VerityTheme?")
}

/**
 * Access point for Verity theme tokens.
 *
 * UI code should consume:
 * - VerityTheme.colors
 * - VerityTheme.typography
 *
 * and must not read MaterialTheme directly.
 */
object VerityTheme {

    val colors: VerityColors
        @Composable
        get() = LocalVerityColors.current

    val typography: VerityTypography
        @Composable
        get() = LocalVerityTypography.current
}

/**
 * Root theme wrapper for the Verity app.
 *
 * This function must be applied exactly once at the app root.
 * No feature or screen should apply MaterialTheme directly.
 */
@Composable
fun VerityTheme(
    /**
     * Single switch point for palette selection: true for dark theme, false for light theme.
     */
    darkTheme: Boolean,
    typography: VerityTypography,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        VerityDarkColors
    } else {
        VerityLightColors
    }
    CompositionLocalProvider(
        LocalVerityColors provides colors,
        LocalVerityTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = MaterialTypography(),
            content = content
        )
    }
}