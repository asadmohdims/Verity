package com.unitech.verity.core.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.unitech.verity.core.ui.primitives.VeritySpacer
import com.unitech.verity.core.ui.primitives.VeritySpace
import com.unitech.verity.core.ui.primitives.VerityText
import com.unitech.verity.core.ui.primitives.VerityTextStyle

/**
 * VerityListItem is a structural row molecule used for lists.
 *
 * It provides a consistent layout for:
 * - directories
 * - search results
 * - ledger rows
 *
 * Styling is entirely driven by primitives.
 */
@Composable
fun VerityListItem(
    leading: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {

        if (leading != null) {
            leading()
            VeritySpacer(size = VeritySpace.Medium, horizontal = true)
        }

        Column(
            modifier = Modifier
        ) {
            VerityText(
                text = title,
                style = VerityTextStyle.Body
            )

            if (subtitle != null) {
                VeritySpacer(size = VeritySpace.ExtraSmall)
                VerityText(
                    text = subtitle,
                    style = VerityTextStyle.Caption
                )
            }
        }

        if (trailing != null) {
            VeritySpacer(size = VeritySpace.Medium, horizontal = true)
            trailing()
        }
    }
}