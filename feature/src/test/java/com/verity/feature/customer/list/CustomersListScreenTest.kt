package com.verity.feature.customer.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.verity.core.formatting.money.Money
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class CustomersListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val customers = listOf(
        CustomerListItem(
            customerId = "cust-1",
            customerName = "Bhargava Industries",
            city = "Mumbai",
            state = "Maharashtra",
            balanceDue = Money.ofPaise(12_141_00)
        ),
        CustomerListItem(
            customerId = "cust-2",
            customerName = "Sharma Enterprises",
            city = "Pune",
            state = "Maharashtra",
            balanceDue = Money.ofPaise(0)
        )
    )

    @Test
    fun `shows a DUE chip for an outstanding balance and SETTLED for zero`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomersListScreen(
                    state = CustomersListUiState(isLoading = false, customers = customers),
                    onCustomerClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Bhargava Industries").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹12,141 DUE").assertIsDisplayed()
        composeTestRule.onNodeWithText("SETTLED").assertIsDisplayed()
    }

    @Test
    fun `tapping a customer row fires onCustomerClick with its customerId`() {
        var clicked: String? = null

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomersListScreen(
                    state = CustomersListUiState(isLoading = false, customers = customers),
                    onCustomerClick = { clicked = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Sharma Enterprises").performClick()

        assertEquals("cust-2", clicked)
    }

    @Test
    fun `zero customers shows the empty state, not a crash`() {
        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                CustomersListScreen(
                    state = CustomersListUiState(isLoading = false, customers = emptyList()),
                    onCustomerClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No customers yet").assertIsDisplayed()
    }
}
