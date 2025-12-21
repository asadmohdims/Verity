package com.verity.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Verity Light color palette.
 *
 * Philosophy:
 * - Cool-neutral, grayscale-anchored
 * - Micro-chromatic (graphite bias), not flat gray
 * - Designed for dense financial data and long sessions
 * - No semantic meaning encoded via color
 */
val VerityLightColors = VerityColors(

    background = VerityColors.Background(
        // App canvas — not pure white, slightly cool
        app = Color(0xFFF8F9FB),

        // Subtle background separation (sections, modes)
        subtle = Color(0xFFF1F3F6),

        // Used rarely (e.g. inverse surfaces, dialogs)
        inverse = Color(0xFF121417)
    ),

    surface = VerityColors.Surface(
        // Primary containers (lists, forms)
        // Neutral canvas — must visually disappear
        base = Color(0xFFFFFFFF),

        // Context / identity / focus zones
        // Noticeably lifted from base (≈ +10% luminance)
        raised = Color(0xFFF3F6FF),

        // Dense / summary / conclusion zones
        // Clearly recessed from base (≈ −12% luminance)
        sunken = Color(0xFFE3E7EE),

        // Inverse surface for dark-on-light contexts
        inverse = Color(0xFF1A1D21)
    ),

    text = VerityColors.Text(
        // Primary reading text — ink, not black
        primary = Color(0xFF111418),

        // Secondary information
        secondary = Color(0xFF3A3F45),

        // Metadata, hints, low-priority labels
        muted = Color(0xFF6B7280),

        // Text on inverse surfaces
        inverse = Color(0xFFF4F6F8),

        // Disabled but still readable
        disabled = Color(0xFF9AA1AA)
    ),

    action = VerityColors.Action(
        // Primary action — restrained cool accent
        primary = Color(0xFF2F5EFF),

        // Secondary actions / outlines
        secondary = Color(0xFF5A6B8A),

        // Destructive intent (clear, not alarming)
        destructive = Color(0xFFB4232A),

        // Disabled actions
        disabled = Color(0xFFB8C0CC)
    ),

    state = VerityColors.State(
        // System feedback only (never financial meaning)
        success = Color(0xFF1E7F5C),
        warning = Color(0xFF9A6A12),
        error   = Color(0xFFB4232A),
        info    = Color(0xFF2F5EFF)
    ),

    finance = VerityColors.Finance(
        // Money is factual, not emotional
        neutral = Color(0xFF111418),

        // Emphasis via hierarchy, not hue
        emphasis = Color(0xFF0B0E12)
    )
)