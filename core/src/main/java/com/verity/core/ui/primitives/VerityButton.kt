package com.verity.core.ui.primitives

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.foundation.LocalIndication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme

enum class VerityButtonRole {
    Primary,
    Secondary,
    Destructive
}

enum class VerityButtonState {
    Enabled,
    Disabled
}

@Composable
fun VerityButton(
    label: String,
    onClick: () -> Unit,
    role: VerityButtonRole = VerityButtonRole.Primary,
    state: VerityButtonState = VerityButtonState.Enabled,
    modifier: Modifier = Modifier
) {
    val enabled = state == VerityButtonState.Enabled

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value

    val backgroundColor = when (role) {
        VerityButtonRole.Primary ->
            when {
                !enabled ->
                    VerityTheme.colors.cta.primaryDisabled

                isPressed ->
                    VerityTheme.colors.cta.primary.copy(
                        red = 0.82f * VerityTheme.colors.cta.primary.red,
                        green = 0.82f * VerityTheme.colors.cta.primary.green,
                        blue = 0.82f * VerityTheme.colors.cta.primary.blue
                    )

                else ->
                    VerityTheme.colors.cta.primary
            }

        VerityButtonRole.Secondary,
        VerityButtonRole.Destructive ->
            when {
                !enabled ->
                    Color.Transparent

                isPressed ->
                    VerityTheme.colors.surface.raised.copy(alpha = 0.12f)

                else ->
                    Color.Transparent
            }
    }

    val borderModifier =
        if (role == VerityButtonRole.Secondary || role == VerityButtonRole.Destructive) {
            val borderColor = when {
                isPressed ->
                    VerityTheme.colors.borders.strong

                role == VerityButtonRole.Destructive && enabled ->
                    VerityTheme.colors.cta.destructive

                else ->
                    VerityTheme.colors.cta.secondaryBorder
            }

            Modifier.border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
        } else {
            Modifier
        }

    val textColor = when (role) {
        VerityButtonRole.Primary ->
            if (enabled)
                VerityTheme.colors.text.inverse
            else
                VerityTheme.colors.text.inverse.copy(alpha = 0.6f)

        VerityButtonRole.Secondary ->
            if (enabled)
                VerityTheme.colors.cta.secondaryText
            else
                VerityTheme.colors.cta.secondaryTextDisabled

        VerityButtonRole.Destructive ->
            if (enabled)
                VerityTheme.colors.cta.destructive
            else
                VerityTheme.colors.cta.destructiveDisabled
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(borderModifier)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            style = VerityTheme.typography.label
        )
    }
}