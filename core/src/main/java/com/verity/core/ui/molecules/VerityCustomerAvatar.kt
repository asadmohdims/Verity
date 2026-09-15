package com.verity.core.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verity.core.theme.VerityTheme
import java.util.Locale

/**
 * VerityCustomerAvatar
 *
 * Small circular initials avatar, matching the approved Design Blueprint's mockups. Promoted out
 * of DocumentSummaryRow (its original, private home) once the Customers List needed the exact
 * same avatar — two real call sites earns this its own file, per CLAUDE.md's "earn your
 * abstractions" principle.
 *
 * Uses raw Material3 Text rather than VerityText: this is the one place a two-letter initial
 * needs to sit on a brand-tinted circle in VerityTheme.colors.primary specifically, at the
 * mockup's 13sp/Bold weight, which none of VerityTextStyle's fixed style→color mappings produce.
 */
@Composable
fun VerityCustomerAvatar(customerName: String, modifier: Modifier = Modifier) {
    val initials = remember(customerName) { initialsFor(customerName) }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(VerityTheme.colors.surface.assistInteractive),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = VerityTheme.typography.label.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = VerityTheme.colors.primary
        )
    }
}

private fun initialsFor(customerName: String): String {
    val words = customerName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(2).uppercase(Locale.ENGLISH)
        else -> (words[0].take(1) + words[1].take(1)).uppercase(Locale.ENGLISH)
    }
}
