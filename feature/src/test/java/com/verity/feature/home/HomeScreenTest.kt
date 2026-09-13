package com.verity.feature.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.verity.core.document.model.DocumentType
import com.verity.core.formatting.money.Money
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Robolectric Compose tests for the Home dashboard — see feature/invoice's
 * InvoiceWorkspaceScreenUndoTest for the methodology this follows (real semantics tree on the
 * JVM, no emulator).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fixedClock = Clock.fixed(
        Instant.parse("2026-09-15T00:00:00Z"),
        ZoneOffset.UTC
    )

    private class FakeHomeDataSource(
        private val documents: List<DocumentSummary>
    ) : HomeDataSource {
        override suspend fun loadAllDocuments(): List<DocumentSummary> = documents
    }

    private fun documentSummary(id: String) = DocumentSummary(
        documentId = id,
        documentNumber = "INV-00000$id",
        customerName = "Bhargava Industries",
        documentType = DocumentType.INVOICE,
        issueDate = java.time.LocalDate.of(2026, 9, 11),
        grandTotal = Money.ofRupees(7021),
        finalizedAtEpochMillis = id.toLong()
    )

    @Test
    fun `empty dashboard shows the empty state with a create button`() {
        val viewModel = HomeViewModel(
            homeDataSource = FakeHomeDataSource(emptyList()),
            clock = fixedClock
        )
        var createClicked = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                HomeRoute(
                    viewModel = viewModel,
                    onCreateNew = { createClicked = true },
                    onSeeAllDocuments = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No documents yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create your first invoice").performClick()

        assertTrue("Expected onCreateNew to be invoked", createClicked)
    }

    @Test
    fun `dashboard with documents shows this month total and recent documents`() {
        val viewModel = HomeViewModel(
            homeDataSource = FakeHomeDataSource(listOf(documentSummary("1"), documentSummary("2"))),
            clock = fixedClock
        )

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                HomeRoute(
                    viewModel = viewModel,
                    onCreateNew = {},
                    onSeeAllDocuments = {}
                )
            }
        }

        composeTestRule.onNodeWithText("₹14,042").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 documents invoiced · September 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("INV-000001 · Bhargava Industries").assertIsDisplayed()
        // Both fixture documents share "Bhargava Industries", so both rows get a "BI" avatar.
        composeTestRule.onAllNodesWithText("BI").assertCountEquals(2)
    }

    @Test
    fun `avatar initials use the first two words of a multi-word customer name`() {
        val longNameDocument = documentSummary("1").copy(
            customerName = "Aristocraft Papers Private Limited"
        )
        val viewModel = HomeViewModel(
            homeDataSource = FakeHomeDataSource(listOf(longNameDocument)),
            clock = fixedClock
        )

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                HomeRoute(viewModel = viewModel, onCreateNew = {}, onSeeAllDocuments = {})
            }
        }

        composeTestRule.onNodeWithText("AP").assertIsDisplayed()
    }

    @Test
    fun `See all is only shown once there are recent documents, and invokes the callback`() {
        val viewModel = HomeViewModel(
            homeDataSource = FakeHomeDataSource(listOf(documentSummary("1"))),
            clock = fixedClock
        )
        var sawAllClicked = false

        composeTestRule.setContent {
            VerityTheme(darkTheme = false, typography = VerityBaseTypography) {
                HomeRoute(
                    viewModel = viewModel,
                    onCreateNew = {},
                    onSeeAllDocuments = { sawAllClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("See all").performClick()
        assertTrue("Expected onSeeAllDocuments to be invoked", sawAllClicked)
    }
}
