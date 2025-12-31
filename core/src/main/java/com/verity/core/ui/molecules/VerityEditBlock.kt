package com.verity.core.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.verity.core.ui.primitives.VerityButton
import com.verity.core.ui.primitives.VerityButtonRole
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.dp

/**
 * VerityEditBlock
 *
 * Molecule responsible for:
 * - Providing a consistent editable block shell
 * - Surface, padding, and vertical rhythm
 * - Expand / collapse handling
 * - Standard action row (Add / Save / Cancel)
 *
 * This molecule:
 * - Owns layout only
 * - Does NOT own field state, validation, or domain logic
 * - Must be used for all multi-field edit blocks in Invoice Workspace
 */
@Composable
fun VerityEditBlock(
    title: String? = null,
    mode: VerityEditMode,
    expanded: Boolean,
    collapsedActionLabel: String? = null,
    onCollapsedAction: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {

        // Optional header
        if (title != null) {
            VerityText(
                text = title,
                style = VerityTextStyle.Title
            )
            VeritySpacer(size = VeritySpace.Small)
        }

        if (!expanded) {
            if (collapsedActionLabel != null && onCollapsedAction != null) {
                VeritySurface(
                    type = VeritySurfaceType.AssistInteractive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCollapsedAction)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = VeritySpace.Small.dp,
                                vertical = VeritySpace.Medium.dp
                            )
                    ) {
                        VerityText(
                            text = "+ $collapsedActionLabel",
                            style = VerityTextStyle.Body
                        )
                    }
                }

                VeritySpacer(size = VeritySpace.Small)
            }
            return@Column
        }

        VeritySurface(
            type = VeritySurfaceType.AssistInteractive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = VeritySpace.Small.dp,
                        vertical = VeritySpace.Medium.dp
                    )
            ) {

                // Injected editable content
                content()

                // Action row (if applicable)
                when (mode) {
                    VerityEditMode.Add -> {
                        renderActionRow(
                            primaryLabel = "Add",
                            secondaryLabel = "Cancel",
                            onPrimary = onAdd,
                            onSecondary = onCancel
                        )
                    }

                    VerityEditMode.Edit -> {
                        VeritySpacer(size = VeritySpace.Small)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Destructive delete on far left (optional)
                            if (onDelete != null) {
                                VerityButton(
                                    label = "Delete",
                                    role = VerityButtonRole.Destructive,
                                    onClick = onDelete
                                )
                            }
                            if (onDelete != null) {
                                VeritySpacer(
                                    size = VeritySpace.Medium,
                                    horizontal = true
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            if (onCancel != null) {
                                VerityButton(
                                    label = "Cancel",
                                    role = VerityButtonRole.Secondary,
                                    onClick = onCancel
                                )
                            }

                            if (onCancel != null && onSave != null) {
                                VeritySpacer(
                                    size = VeritySpace.Medium,
                                    horizontal = true
                                )
                            }

                            if (onSave != null) {
                                VerityButton(
                                    label = "Save",
                                    role = VerityButtonRole.Primary,
                                    onClick = onSave
                                )
                            }
                        }
                    }

                    VerityEditMode.View -> {
                        // No actions in view mode
                    }
                }
            }
        }
    }
}

@Composable
private fun renderActionRow(
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: (() -> Unit)?,
    onSecondary: (() -> Unit)?
) {
    if (onPrimary == null && onSecondary == null) return

    VeritySpacer(size = VeritySpace.Small)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Push actions to the right
        Spacer(
            modifier = Modifier.weight(1f)
        )

        if (onSecondary != null) {
            VerityButton(
                label = secondaryLabel,
                role = VerityButtonRole.Secondary,
                onClick = onSecondary
            )
        }

        if (onSecondary != null && onPrimary != null) {
            VeritySpacer(
                size = VeritySpace.Medium,
                horizontal = true
            )
        }

        if (onPrimary != null) {
            VerityButton(
                label = primaryLabel,
                role = VerityButtonRole.Primary,
                onClick = onPrimary
            )
        }
    }
}

/**
 * Edit lifecycle for VerityEditBlock.
 */
enum class VerityEditMode {
    Add,
    Edit,
    View
}
