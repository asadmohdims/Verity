package com.verity.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
            colorScheme = colors.toMaterialColorScheme(darkTheme),
            typography = MaterialTypography(),
            content = content
        )
    }
}

/**
 * Maps Verity's semantic tokens onto a real M3 ColorScheme.
 *
 * Verity's own components (VerityButton, VerityText, VerityDivider, VerityTopAppBar, ...) never
 * read MaterialTheme.colorScheme — they read VerityTheme.colors directly. This mapping exists
 * for the handful of raw Material3 components Verity doesn't wrap (NavigationBar,
 * FloatingActionButton, Divider, DropdownMenu, Scaffold's background) so they render with
 * Verity's brand instead of stock Material3's default baseline palette.
 */
private fun VerityColors.toMaterialColorScheme(darkTheme: Boolean): ColorScheme {
    val builder = if (darkTheme) ::darkColorSchemeFrom else ::lightColorSchemeFrom
    return builder(this)
}

private fun darkColorSchemeFrom(colors: VerityColors): ColorScheme = darkColorScheme(
    primary = colors.primary,
    onPrimary = colors.text.inverse,
    primaryContainer = colors.surface.assistInteractive,
    onPrimaryContainer = colors.text.primary,
    secondary = colors.accent,
    onSecondary = colors.text.inverse,
    background = colors.background.app,
    onBackground = colors.text.primary,
    surface = colors.surface.base,
    onSurface = colors.text.primary,
    surfaceVariant = colors.surface.raised,
    onSurfaceVariant = colors.text.secondary,
    outline = colors.borders.subtle,
    outlineVariant = colors.borders.divider,
    error = colors.cta.destructive,
    onError = colors.text.inverse
)

private fun lightColorSchemeFrom(colors: VerityColors): ColorScheme = lightColorScheme(
    primary = colors.primary,
    onPrimary = colors.text.inverse,
    primaryContainer = colors.surface.assistInteractive,
    onPrimaryContainer = colors.text.primary,
    secondary = colors.accent,
    onSecondary = colors.text.inverse,
    background = colors.background.app,
    onBackground = colors.text.primary,
    surface = colors.surface.base,
    onSurface = colors.text.primary,
    surfaceVariant = colors.surface.raised,
    onSurfaceVariant = colors.text.secondary,
    outline = colors.borders.subtle,
    outlineVariant = colors.borders.divider,
    error = colors.cta.destructive,
    onError = colors.text.inverse
)