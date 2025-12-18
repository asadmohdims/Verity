package com.verity.core.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle

/**
 * VerityHeader is a screen-level heading molecule.
 *
 * It provides consistent hierarchy for screen titles
 * with optional subtitle and trailing actions.
 */
@Composable
fun VerityHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier
        ) {
            VerityText(
                text = title,
                style = VerityTextStyle.Display
            )

            if (subtitle != null) {
                VeritySpacer(size = VeritySpace.ExtraSmall)
                VerityText(
                    text = subtitle,
                    style = VerityTextStyle.Body
                )
            }
        }

        if (trailing != null) {
            VeritySpacer(size = VeritySpace.Medium, horizontal = true)
            trailing()
        }
    }
}