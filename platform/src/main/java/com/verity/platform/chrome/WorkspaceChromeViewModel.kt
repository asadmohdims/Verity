package com.verity.platform.chrome

import com.verity.core.ui.chrome.WorkspaceChromeSpec
import com.verity.core.ui.molecules.VerityNavIcon
import com.verity.core.ui.molecules.VerityTopBarAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WorkspaceChromeViewModel
 *
 * PURPOSE
 * -------
 * Platform-level orchestrator for workspace application chrome.
 *
 * This ViewModel owns the *semantic intent* of the Top App Bar when
 * a workspace (e.g. Invoice Workspace) is active.
 *
 * OWNERSHIP & FLOW
 * ----------------
 * - Produced here (platform layer)
 * - Consumed by root UI frame (MainActivity / NavHost)
 * - Rendered by VerityTopAppBar
 *
 * DESIGN LAWS
 * -----------
 * - No UI rendering
 * - No Compose dependencies
 * - No feature module dependencies
 * - No navigation execution
 *
 * EVOLUTION
 * ---------
 * Workspace features emit semantic events.
 * This ViewModel derives the correct WorkspaceChromeSpec.
 */
class WorkspaceChromeViewModel {

    // ============================================================
    // Chrome State (Semantic Only)
    // ============================================================

    private val _chromeSpec = MutableStateFlow(
        WorkspaceChromeSpec(
            title = "Invoice",
            subtitle = "Draft",
            navigationIcon = VerityNavIcon.Back(
                onClick = { /* handled at root */ },
                contentDescription = "Back"
            ),
            actions = emptyList()
        )
    )

    val chromeSpec: StateFlow<WorkspaceChromeSpec> =
        _chromeSpec.asStateFlow()
}