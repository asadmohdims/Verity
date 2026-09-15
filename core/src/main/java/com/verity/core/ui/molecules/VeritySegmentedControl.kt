package com.verity.core.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme

/**
 * VeritySegmentedControl
 *
 * A small, generic N-way picker (System/Light/Dark today; any small fixed option set tomorrow) —
 * matches the mockup's `.segrow`/`.seg` (a raised track with a lighter "thumb" behind the selected
 * segment). No animated thumb slide — a static background swap, since nothing in this app's
 * design system has established a motion spec yet (see CLAUDE.md's Testing/UX sections).
 */
@Composable
fun <T> VeritySegmentedControl(
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(VerityTheme.colors.surface.raised)
            .padding(3.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) VerityTheme.colors.surface.base else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 9.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Raw Material3 Text, not VerityText: needs the same type scale for both states,
                // differing only by color (text.primary selected / text.secondary unselected) —
                // no VerityTextStyle maps to that pairing, same rationale as HomeScreen's
                // SectionTitle/OverlineLabel.
                Text(
                    text = labelFor(option),
                    style = VerityTheme.typography.label,
                    color = if (isSelected) VerityTheme.colors.text.primary else VerityTheme.colors.text.secondary
                )
            }
        }
    }
}
