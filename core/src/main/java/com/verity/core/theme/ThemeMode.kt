package com.verity.core.theme

/**
 * ThemeMode
 *
 * The user's theme preference — distinct from VerityTheme's own `darkTheme: Boolean`, which is
 * the *resolved* value SYSTEM ultimately collapses into. Lives in `core` (not `platform`, where
 * it's persisted, or `feature`, where it's edited) since both of those, plus the `app` composition
 * root, need to reference the same type.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}
