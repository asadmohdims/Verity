package com.verity.core.ui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme

/**
 * VerityBadge
 *
 * A small static label chip — a document-type or status indicator, never a control (no click
 * slot; see VeritySegmentedControl for the selectable equivalent). Raw Material3 Text, not
 * VerityText: needs a caller-supplied content color VerityText deliberately doesn't expose (it
 * enforces one fixed color per VerityTextStyle) — same rationale as VeritySegmentedControl's own
 * label Text.
 */
@Composable
fun VerityBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = VerityTheme.typography.caption,
        color = contentColor,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
