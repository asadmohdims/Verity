package com.verity.core.ui.primitives
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme

/**
 * VerityTextField
 *
 * SelectionSearch role ONLY.
 * - Material 3 aligned (OutlinedTextField)
 * - Explicit edit mode
 * - Stable focus & keyboard
 * - Anchored dropdown
 *
 * Other roles will be added later.
 */
enum class VerityTextFieldRole {
    Basic,
    SelectionSearch
}

data class VeritySuggestion(
    val id: String,
    val primary: String,
    val secondary: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerityTextField(
    role: VerityTextFieldRole,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    editing: Boolean,
    onEnterEdit: (() -> Unit)?,
    onExitEdit: (() -> Unit)?,
    suggestions: List<VeritySuggestion>,
    onSelectSuggestion: ((VeritySuggestion) -> Unit)?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorText: String? = null,
    /**
     * SelectionSearch only. When true, the full (unfiltered) suggestion list opens as soon as
     * the field gains focus, instead of waiting for the first keystroke — a "dropdown" feel for
     * a short, closed-ish list (HSN Code, Unit) without introducing an actual popup menu, which
     * CLAUDE.md's UI conventions rule out ("suggestions expand inline below the input field —
     * never a popup/dropdown menu"). Free text past the list is still accepted either way.
     */
    expandSuggestionsOnFocus: Boolean = false
) {
    when (role) {
        VerityTextFieldRole.Basic -> BasicTextField(
            label = label,
            placeholder = placeholder,
            value = value,
            onValueChange = onValueChange,
            errorText = errorText,
            modifier = modifier
        )

        VerityTextFieldRole.SelectionSearch -> SelectionSearchField(
            label = label,
            placeholder = placeholder,
            value = value,
            onValueChange = onValueChange,
            suggestions = suggestions,
            onSelectSuggestion = onSelectSuggestion,
            expandSuggestionsOnFocus = expandSuggestionsOnFocus,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionSearchField(
    label: String,
    placeholder: String?,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<VeritySuggestion>,
    onSelectSuggestion: ((VeritySuggestion) -> Unit)?,
    expandSuggestionsOnFocus: Boolean,
    modifier: Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {

        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            placeholder = {
                if (placeholder != null) {
                    Text(placeholder)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (expandSuggestionsOnFocus && focusState.isFocused) {
                        expanded = true
                    }
                },
            singleLine = true,
            textStyle = VerityTheme.typography.body.copy(
                color = VerityTheme.colors.text.primary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = VerityTheme.colors.text.primary,
                unfocusedTextColor = VerityTheme.colors.text.primary,
                disabledTextColor = VerityTheme.colors.text.disabled,

                focusedBorderColor = VerityTheme.colors.primary,
                unfocusedBorderColor = VerityTheme.colors.borders.subtle,

                focusedLabelColor = VerityTheme.colors.primary,
                unfocusedLabelColor = VerityTheme.colors.text.muted,

                focusedPlaceholderColor = VerityTheme.colors.text.muted,
                unfocusedPlaceholderColor = VerityTheme.colors.text.muted,

                cursorColor = VerityTheme.colors.primary
            )
        )

        AnimatedVisibility(
            visible = expanded && suggestions.isNotEmpty() && (expandSuggestionsOnFocus || value.isNotBlank()),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = VerityTheme.colors.surface.base,
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .border(
                        width = 1.dp,
                        color = VerityTheme.colors.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Column {
                    Box(
                        modifier = Modifier.heightIn(max = 190.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = suggestions,
                                key = { it.id }
                            ) { suggestion ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectSuggestion?.invoke(suggestion)
                                            expanded = false
                                            keyboardController?.hide()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 18.dp)
                                ) {
                                    Text(
                                        text = suggestion.primary,
                                        color = VerityTheme.colors.text.primary,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                                    )
                                    if (suggestion.secondary != null) {
                                        Text(
                                            text = suggestion.secondary,
                                            color = VerityTheme.colors.text.secondary,
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                if (suggestion != suggestions.last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = VerityTheme.colors.borders.divider
                                    )
                                }
                            }
                        }

                        // Top edge fade
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            VerityTheme.colors.surface.base,
                                            VerityTheme.colors.surface.base.copy(alpha = 0f)
                                        )
                                    )
                                )
                        )

                        // Bottom edge fade
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            VerityTheme.colors.surface.base.copy(alpha = 0f),
                                            VerityTheme.colors.surface.base
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicTextField(
    label: String,
    placeholder: String?,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    errorText: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = {
                if (placeholder != null) {
                    Text(placeholder)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorText != null,
            textStyle = VerityTheme.typography.body.copy(
                color = VerityTheme.colors.text.primary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = VerityTheme.colors.text.primary,
                unfocusedTextColor = VerityTheme.colors.text.primary,
                disabledTextColor = VerityTheme.colors.text.disabled,

                focusedBorderColor = VerityTheme.colors.primary,
                unfocusedBorderColor = VerityTheme.colors.borders.subtle,

                errorBorderColor = VerityTheme.colors.state.error,
                errorLabelColor = VerityTheme.colors.state.error,
                errorCursorColor = VerityTheme.colors.state.error,

                focusedLabelColor = VerityTheme.colors.primary,
                unfocusedLabelColor = VerityTheme.colors.text.muted,

                focusedPlaceholderColor = VerityTheme.colors.text.muted,
                unfocusedPlaceholderColor = VerityTheme.colors.text.muted,

                cursorColor = VerityTheme.colors.primary
            )
        )

        if (errorText != null) {
            Text(
                text = errorText,
                color = VerityTheme.colors.state.error,
                style = VerityTheme.typography.caption,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
