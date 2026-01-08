package com.verity.core.ui.chrome

/**
 * WorkspaceChromeSpec
 *
 * PURPOSE
 * -------
 * Canonical semantic contract describing how application chrome
 * (Top App Bar) should appear when the Invoice Workspace is active.
 *
 * This is a *description*, not a renderer.
 *
 * OWNERSHIP
 * ---------
 * - Produced by: Workspace domain (screen or ViewModel)
 * - Consumed by: Root UI frame (MainActivity / NavHost)
 * - Rendered by: VerityTopAppBar
 *
 * DESIGN INTENT
 * -------------
 * - Keeps Invoice Workspace as the semantic anchor of the app
 * - Ensures a single, global chrome instance (no duplication)
 * - Decouples chrome *meaning* from chrome *rendering*
 *
 * NON‑RESPONSIBILITIES
 * --------------------
 * - No navigation execution
 * - No state transitions
 * - No Compose UI code
 * - No lifecycle or ownership logic
 *
 * EVOLUTION
 * ---------
 * This spec is intentionally minimal.
 * If chrome transitions become complex in the future, this type
 * may be wrapped by a higher‑level ChromeState without requiring
 * changes to rendering code.
 */

/**
 * @property title
 * Primary chrome title (e.g. "Invoice", "Search", "Customers").
 *
 * @property subtitle
 * Optional secondary context (e.g. draft status, document state).
 *
 * @property navigationIcon
 * Leading navigation affordance for the workspace (none / back).
 *
 * @property actions
 * Trailing chrome actions relevant to the current workspace mode.
 *
 * @property chromeMode
 * Visual chrome mode controlling layout and density.
 * Defaults to Workspace mode.
 */
data class WorkspaceChromeSpec(
    val title: String,
    val subtitle: String? = null,
    val navigationIcon: com.verity.core.ui.molecules.VerityNavIcon,
    val actions: List<com.verity.core.ui.molecules.VerityTopBarAction>,
    val chromeMode: com.verity.core.ui.molecules.VerityChromeMode =
        com.verity.core.ui.molecules.VerityChromeMode.Workspace
)