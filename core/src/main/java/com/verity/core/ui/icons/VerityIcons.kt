
package com.verity.core.ui.icons
import com.verity.core.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource

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
 * - Icons are sourced from Material Icons (Filled) by default; the four bottom-nav destinations
 *   and the FAB's "create" glyph are hand-authored vectors instead (VerityNavIconVectors.kt),
 *   transcribed 1:1 from the approved Design canvas mockup — approved as a deliberate departure
 *   from Material Icons on 2026-09-14, since none of Home/Documents/Customers/Settings has a
 *   Material Icons equivalent that matches the mockup's outline style
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
        VerityIcon.Material(VerityAddOutlineIcon)

    val Edit: VerityIcon =
        VerityIcon.Material(Icons.Filled.Edit)

    /**
     * Bottom navigation destinations (R-13). Hand-authored outline vectors, not Material Icons —
     * see the class doc comment above.
     */
    val Home: VerityIcon =
        VerityIcon.Material(VerityHomeOutlineIcon)

    val Documents: VerityIcon =
        VerityIcon.Material(VerityDocumentsOutlineIcon)

    val Customers: VerityIcon =
        VerityIcon.Material(VerityCustomersOutlineIcon)

    val Settings: VerityIcon =
        VerityIcon.Material(VeritySettingsOutlineIcon)
}

/**
 * Renders a VerityIcon regardless of its underlying source (Material vector vs. drawable
 * resource) — the one place that when(icon) branch should be written, so call sites never have
 * to re-derive it.
 */
@Composable
fun VerityIconGlyph(
    icon: VerityIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    when (icon) {
        is VerityIcon.Material -> Icon(
            imageVector = icon.imageVector,
            contentDescription = contentDescription,
            modifier = modifier
        )
        is VerityIcon.VectorRes -> Icon(
            painter = painterResource(icon.resId),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}