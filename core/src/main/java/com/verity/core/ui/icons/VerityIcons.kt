package com.verity.core.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * VerityIcons
 *
 * Canonical icon registry for Verity.
 *
 * Design rules:
 * - Icons represent actions or navigation only
 * - Icons never represent state or business meaning
 * - Icons are sourced exclusively from Material Icons (Filled)
 * - AutoMirrored variants are used where navigation direction matters
 *
 * Usage rules:
 * - UI code must reference icons only via VerityIcons
 * - Direct usage of Icons.* outside this file is prohibited
 * - New icons require explicit design approval
 */
object VerityIcons {

    /**
     * Navigation
     */
    val Back: ImageVector =
        Icons.AutoMirrored.Filled.ArrowBack

    /**
     * Chrome / Global actions
     */
    val Search: ImageVector =
        Icons.Filled.Search

    val Overflow: ImageVector =
        Icons.Filled.MoreVert

    /**
     * Explicit draft actions
     */
    val Add: ImageVector =
        Icons.Filled.Add

    val Edit: ImageVector =
        Icons.Filled.Edit
}