package com.verity.core.ui.molecules

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.icons.VerityIconGlyph
import com.verity.core.ui.icons.VerityIcons
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * VerityDateField
 *
 * Bordered, tap-to-open date selector — matches the visual language of the existing "Document
 * Type" selector field in InvoiceWorkspaceScreen (label above, bordered box, chevron) rather than
 * VerityTextField: a calendar date has no ambiguous free-text format to fight with, so a picker
 * is the right primitive here, not a text field the user has to type a date into.
 *
 * Wraps Material3's DatePicker/DatePickerDialog behind an explicit, contained opt-in: those APIs
 * are still annotated @ExperimentalMaterial3Api in this project's pinned Compose BOM (2024.09.00)
 * despite being stable, widely-shipped in production — the opt-in stays in this one file rather
 * than leaking into feature code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerityDateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    formatter: (LocalDate) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "Select date",
    /** Latest date the picker will let the user land on, e.g. today for a "no post-dating" field. */
    maxDate: LocalDate? = null
) {
    var isPickerOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        VerityText(text = label, style = VerityTextStyle.Label)
        VeritySpacer(size = VeritySpace.ExtraSmall)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(
                    width = 1.dp,
                    color = VerityTheme.colors.borders.subtle,
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable { isPickerOpen = true }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VerityText(
                text = value?.let(formatter) ?: placeholder,
                // Placeholder borrows Caption's muted color; a real value reads at Body/primary —
                // VerityText deliberately has no arbitrary-color escape hatch (see its own doc
                // comment), so the style itself is the only lever available here.
                style = if (value != null) VerityTextStyle.Body else VerityTextStyle.Caption
            )
            VerityIconGlyph(
                icon = VerityIcons.ChevronDown,
                contentDescription = null
            )
        }
    }

    if (isPickerOpen) {
        val initialMillis = (value ?: LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val selectableDates = remember(maxDate) {
            if (maxDate == null) {
                DatePickerDefaults.AllDates
            } else {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        return !date.isAfter(maxDate)
                    }
                }
            }
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = selectableDates
        )

        DatePickerDialog(
            onDismissRequest = { isPickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(
                        pickerState.selectedDateMillis?.let { millis ->
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                    )
                    isPickerOpen = false
                }) {
                    VerityText(text = "OK", style = VerityTextStyle.Body)
                }
            },
            dismissButton = {
                TextButton(onClick = { isPickerOpen = false }) {
                    VerityText(text = "Cancel", style = VerityTextStyle.Caption)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = VerityTheme.colors.surface.raised
            )
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = VerityTheme.colors.surface.raised,
                    titleContentColor = VerityTheme.colors.text.secondary,
                    headlineContentColor = VerityTheme.colors.text.primary,
                    weekdayContentColor = VerityTheme.colors.text.muted,
                    subheadContentColor = VerityTheme.colors.text.secondary,
                    navigationContentColor = VerityTheme.colors.text.primary,
                    yearContentColor = VerityTheme.colors.text.primary,
                    currentYearContentColor = VerityTheme.colors.primary,
                    selectedYearContentColor = VerityTheme.colors.text.inverse,
                    selectedYearContainerColor = VerityTheme.colors.primary,
                    dayContentColor = VerityTheme.colors.text.primary,
                    disabledDayContentColor = VerityTheme.colors.text.disabled,
                    selectedDayContentColor = VerityTheme.colors.text.inverse,
                    selectedDayContainerColor = VerityTheme.colors.primary,
                    todayContentColor = VerityTheme.colors.primary,
                    todayDateBorderColor = VerityTheme.colors.primary
                )
            )
        }
    }
}
