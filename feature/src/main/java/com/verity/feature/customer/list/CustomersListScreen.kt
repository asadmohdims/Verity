package com.verity.feature.customer.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.molecules.VerityCustomerAvatar
import com.verity.core.ui.molecules.VerityListItem
import com.verity.core.ui.primitives.VerityDivider
import com.verity.core.ui.primitives.VerityDividerStrength
import com.verity.core.ui.primitives.VeritySpace
import com.verity.core.ui.primitives.VeritySpacer
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.core.ui.primitives.VerityText
import com.verity.core.ui.primitives.VerityTextStyle
import com.verity.core.ui.primitives.dp

/**
 * CustomersListRoute
 *
 * Re-triggers a refresh each time this destination becomes current, same rationale as
 * DocumentsListRoute — a customer added/edited from Add/Edit Customer must show up here on
 * return without a fresh process/ViewModel.
 */
@Composable
fun CustomersListRoute(
    viewModel: CustomersListViewModel,
    onCustomerClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    CustomersListScreen(
        state = state,
        onCustomerClick = onCustomerClick
    )
}

/**
 * CustomersListScreen
 *
 * Pure renderer — never obtains a ViewModel, never collects a Flow directly (see CLAUDE.md's
 * Route/Screen/ViewModel ownership rule).
 */
@Composable
fun CustomersListScreen(
    state: CustomersListUiState,
    onCustomerClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    VeritySurface(
        type = VeritySurfaceType.Base,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(VeritySpace.Medium.dp)
        ) {
            when {
                state.customers.isEmpty() && !state.isLoading -> EmptyCustomers()

                state.customers.isNotEmpty() -> {
                    VeritySurface(type = VeritySurfaceType.Card) {
                        Column {
                            state.customers.forEachIndexed { index, customer ->
                                CustomerRow(
                                    customer = customer,
                                    onClick = { onCustomerClick(customer.customerId) }
                                )
                                if (index != state.customers.lastIndex) {
                                    VerityDivider(strength = VerityDividerStrength.Divider)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerRow(customer: CustomerListItem, onClick: () -> Unit) {
    VerityListItem(
        leading = { VerityCustomerAvatar(customerName = customer.customerName) },
        title = customer.customerName,
        titleMaxLines = 1,
        titleOverflow = TextOverflow.Ellipsis,
        subtitle = "${customer.city}, ${customer.state}",
        trailing = { BalanceChip(customer = customer) },
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    )
}

/**
 * DUE/SETTLED micro-chip matching the mockup's `.microchip` — no VerityChip primitive exists yet
 * for this pass, so this stays a local composable rather than becoming a premature abstraction
 * for one screen's use.
 */
@Composable
private fun BalanceChip(customer: CustomerListItem) {
    val (background, textColor, label) = if (customer.isSettled) {
        Triple(
            VerityTheme.colors.text.muted.copy(alpha = 0.12f),
            VerityTheme.colors.text.secondary,
            "SETTLED"
        )
    } else {
        Triple(
            VerityTheme.colors.state.error.copy(alpha = 0.14f),
            VerityTheme.colors.state.error,
            "${customer.balanceDue.format()} DUE"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color = background)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = VerityTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
private fun EmptyCustomers() {
    VeritySurface(type = VeritySurfaceType.Card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VeritySpace.Large.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerityText(text = "No customers yet", style = VerityTextStyle.Title)
            VeritySpacer(size = VeritySpace.Small)
            VerityText(
                text = "Customers you add will show up here.",
                style = VerityTextStyle.Caption
            )
        }
    }
}
