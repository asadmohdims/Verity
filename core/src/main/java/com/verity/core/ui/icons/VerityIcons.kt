
package com.verity.core.ui.icons
import com.verity.core.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface VerityIcon {

    data class Material(
        val imageVector: ImageVector
    ) : VerityIcon

    data class VectorRes(
        val resId: Int
    ) : VerityIcon
}

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
    val Back: VerityIcon =
        VerityIcon.Material(Icons.AutoMirrored.Filled.ArrowBack)

    /**
     * Chrome / Global actions
     */
    val Preview: VerityIcon =
        VerityIcon.VectorRes(R.drawable.preview)
    val Search: VerityIcon =
        VerityIcon.Material(Icons.Filled.Search)

    val Overflow: VerityIcon =
        VerityIcon.Material(Icons.Filled.MoreVert)

    /**
     * Explicit draft actions
     */
    val Add: VerityIcon =
        VerityIcon.Material(Icons.Filled.Add)

    val Edit: VerityIcon =
        VerityIcon.Material(Icons.Filled.Edit)
}