package com.verity.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Verity Dark color palette.
 *
 * Philosophy:
 * - Designed dark mode, not inverted light
 * - Cool-neutral, graphite-anchored
 * - Preserves contrast for dense financial data
 * - Avoids muddy grays and low-contrast text
 */
val VerityDarkColors = VerityColors(

    background = VerityColors.Background(
        // App canvas — deep graphite, not pure black
        app = Color(0xFF0E1116),

        // Subtle background separation
        subtle = Color(0xFF141821),

        // Inverse background for rare light-on-dark needs
        inverse = Color(0xFFF8F9FB)
    ),

    surface = VerityColors.Surface(
        // Primary containers
        // Neutral canvas — must visually disappear
        base = Color(0xFF161B24),

        // Context / identity / focus zones
        // Clearly lifted from base (≈ +10–12% luminance)
        raised = Color(0xFF232A3A),

        // Dense / summary / conclusion zones
        // Clearly recessed from base (≈ −12–15% luminance)
        assist = Color(0xFF0C1018),

        // Inverse surface (dialogs, rare light contexts)
        inverse = Color(0xFFECEFF3)
    ),

    borders = VerityColors.Borders(
        // Subtle outlines for inputs, cards, and assist surfaces
        subtle = Color(0xFF2A3142),

        // Hairline dividers between list items and sections
        divider = Color(0xFF323A4E)
    ),

    text = VerityColors.Text(
        // Primary reading text — bright but not white
        primary = Color(0xFFE6EAF0),

        // Secondary information
        secondary = Color(0xFFB4BBC7),

        // Metadata, hints
        muted = Color(0xFF8A93A3),

        // Text on inverse (light) surfaces
        inverse = Color(0xFF111418),

        // Disabled but still legible
        disabled = Color(0xFF6E7686)
    ),

    action = VerityColors.Action(
        // Primary action — same hue family as light mode
        primary = Color(0xFF4C6EF5),

        // Secondary actions
        secondary = Color(0xFF8FA3C8),

        // Destructive intent
        destructive = Color(0xFFEF6A6A),

        // Disabled actions
        disabled = Color(0xFF4C5566)
    ),

    state = VerityColors.State(
        // Feedback only, never semantic meaning
        success = Color(0xFF4FB286),
        warning = Color(0xFFE0A74F),
        error   = Color(0xFFEF6A6A),
        info    = Color(0xFF6B8CFF)
    ),

    finance = VerityColors.Finance(
        // Same philosophy as light mode
        neutral = Color(0xFFE6EAF0),

        // Emphasis via luminance, not hue
        emphasis = Color(0xFFF4F6FA)
    )
)