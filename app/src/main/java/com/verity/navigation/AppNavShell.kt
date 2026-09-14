package com.verity.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.verity.core.theme.VerityTheme
import com.verity.core.ui.chrome.WorkspaceChromeSpec
import com.verity.core.ui.icons.VerityIconGlyph
import com.verity.core.ui.icons.VerityIcons
import com.verity.core.ui.molecules.VerityBottomNav
import com.verity.core.ui.molecules.VerityBottomNavItem
import com.verity.core.ui.molecules.VerityChromeMode
import com.verity.core.ui.molecules.VerityNavIcon
import com.verity.core.ui.molecules.VerityTopAppBar
import com.verity.core.ui.molecules.VerityTopBarAction
import com.verity.core.ui.primitives.VeritySurface
import com.verity.core.ui.primitives.VeritySurfaceType
import com.verity.feature.home.HomeRoute
import com.verity.feature.home.HomeViewModel
import com.verity.feature.invoice.preview.InvoiceFinalizedScreen
import com.verity.feature.invoice.preview.InvoicePreviewScreen
import com.verity.feature.invoice.ui.InvoiceWorkspaceRoute
import com.verity.feature.invoice.ui.InvoiceWorkspaceViewModel

/**
 * Route constants for the whole app's single NavHost. The four tab roots (Home/Documents/
 * Customers/Settings) are Brand-mode, switched by the bottom nav, never pushed; Workspace/
 * Preview/Finalized are the existing invoice-creation flow, unchanged from before R-13, reached
 * by pushing on top of whichever tab was current when the FAB was tapped.
 *
 * FINALIZED is the confirmation screen (checkmark + "Invoice Finalized"); its "View Document"
 * button pushes FINALIZED_DOCUMENT, which reuses InvoicePreviewScreen as a read-only viewer — this
 * app has no separate Document Detail screen yet (Phase 2 per the UX roadmap).
 */
internal object AppRoutes {
    const val HOME = "home"
    const val DOCUMENTS = "documents"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"
    const val WORKSPACE = "workspace"
    const val PREVIEW = "preview"
    const val FINALIZED = "finalized"
    const val FINALIZED_DOCUMENT = "finalized_document"
}

private val bottomNavItems = listOf(
    VerityBottomNavItem(AppRoutes.HOME, "Home", VerityIcons.Home),
    VerityBottomNavItem(AppRoutes.DOCUMENTS, "Documents", VerityIcons.Documents),
    VerityBottomNavItem(AppRoutes.CUSTOMERS, "Customers", VerityIcons.Customers),
    VerityBottomNavItem(AppRoutes.SETTINGS, "Settings", VerityIcons.Settings)
)

/**
 * AppNavShell
 *
 * R-13: the navigation shell (bottom nav + FAB) wrapping the app's single NavHost. Previously
 * this NavHost, its chrome-switching logic, and the preview/finalize wiring all lived directly in
 * MainActivity; extracted here so MainActivity stays composition-root-only (object construction),
 * with the actual UI tree living in one place. Behavior for workspace/preview/finalized is
 * unchanged from before this refactor.
 */
@Composable
fun AppNavShell(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    invoiceWorkspaceViewModel: InvoiceWorkspaceViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTabRoute = bottomNavItems.any { it.route == currentRoute }

    fun goToTab(route: String) {
        navController.navigate(route) {
            popUpTo(AppRoutes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    fun goToWorkspace() {
        // The FAB/"Create" actions already ARE the user's "start a new invoice" intent — landing
        // on InvoiceWorkspaceRoute's own empty-state prompt ("Create Invoice — Start a new
        // invoice from scratch") after that would just be a redundant second tap. Only start a
        // fresh draft when none is active yet, so resuming an in-progress draft (e.g. the user
        // backed out mid-edit and tapped the FAB again) is left untouched.
        if (!invoiceWorkspaceViewModel.hasActiveDraft.value) {
            invoiceWorkspaceViewModel.onCreateInvoice()
        }
        navController.navigate(AppRoutes.WORKSPACE)
    }

    val workspaceChromeSpec by invoiceWorkspaceViewModel.chromeSpec.collectAsState()
    val previewDocument by invoiceWorkspaceViewModel.previewDocument.collectAsState()
    val finalizedDocument by invoiceWorkspaceViewModel.finalizedDocument.collectAsState()

    val chromeSpecWithNavigation = remember(workspaceChromeSpec, previewDocument) {
        val canPreview = previewDocument != null

        workspaceChromeSpec.copy(
            actions = workspaceChromeSpec.actions.map { action ->
                if (
                    action is VerityTopBarAction.Icon &&
                    action.contentDescription == "Preview invoice"
                ) {
                    // Disabled (not just a silent no-op) until Billed To is set — that's what
                    // previewDocument being null actually means (see DraftToInvoiceDocument,
                    // which needs the buyer's state to determine GST mode). Found by the user
                    // tapping Preview on a blank draft and seeing nothing happen at all.
                    action.copy(
                        enabled = canPreview,
                        onClick = {
                            if (canPreview) {
                                navController.navigate(AppRoutes.PREVIEW)
                            }
                        }
                    )
                } else {
                    action
                }
            }
        )
    }

    val effectiveChromeSpec = when (currentRoute) {
        AppRoutes.HOME -> brandChrome(title = "Verity")
        AppRoutes.DOCUMENTS -> brandChrome(title = "Documents")
        AppRoutes.CUSTOMERS -> brandChrome(title = "Customers")
        AppRoutes.SETTINGS -> brandChrome(title = "Settings")
        AppRoutes.PREVIEW -> supportChrome(title = "Invoice Preview") { navController.popBackStack() }
        AppRoutes.FINALIZED -> supportChrome(title = "Invoice Finalized") { navController.popBackStack() }
        AppRoutes.FINALIZED_DOCUMENT ->
            supportChrome(
                title = finalizedDocument?.identity?.documentNumber ?: "Invoice"
            ) { navController.popBackStack() }
        else -> chromeSpecWithNavigation
    }

    Scaffold(
        topBar = {
            VerityTopAppBar(
                title = effectiveChromeSpec.title,
                subtitle = effectiveChromeSpec.subtitle,
                navigationIcon = effectiveChromeSpec.navigationIcon,
                actions = effectiveChromeSpec.actions,
                chromeMode = effectiveChromeSpec.chromeMode
            )
        },
        bottomBar = {
            if (isTabRoute) {
                VerityBottomNav(
                    items = bottomNavItems,
                    selectedRoute = currentRoute,
                    onSelect = ::goToTab
                )
            }
        },
        floatingActionButton = {
            if (isTabRoute) {
                // Circular, matching the mockup's `.fab{border-radius:999px}` — M3's own default
                // FAB shape is a rounded square, not a circle.
                FloatingActionButton(
                    onClick = ::goToWorkspace,
                    shape = CircleShape,
                    containerColor = VerityTheme.colors.cta.primary,
                    contentColor = VerityTheme.colors.text.inverse
                ) {
                    VerityIconGlyph(icon = VerityIcons.Add, contentDescription = "Create")
                }
            }
        }
    ) { innerPadding ->
        VeritySurface(
            type = VeritySurfaceType.Base,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = AppRoutes.HOME
            ) {
                composable(AppRoutes.HOME) {
                    HomeRoute(
                        viewModel = homeViewModel,
                        onCreateNew = ::goToWorkspace,
                        onSeeAllDocuments = { goToTab(AppRoutes.DOCUMENTS) }
                    )
                }

                composable(AppRoutes.DOCUMENTS) {
                    PlaceholderScreen(
                        title = "Documents",
                        message = "Every invoice and challan you finalize will be listed and searchable here soon."
                    )
                }

                composable(AppRoutes.CUSTOMERS) {
                    PlaceholderScreen(
                        title = "Customers",
                        message = "Your customer list, balances, and document history will live here soon."
                    )
                }

                composable(AppRoutes.SETTINGS) {
                    PlaceholderScreen(
                        title = "Settings",
                        message = "Business profile and theme settings are coming soon."
                    )
                }

                composable(AppRoutes.WORKSPACE) {
                    InvoiceWorkspaceRoute(
                        viewModel = invoiceWorkspaceViewModel
                    )
                }

                composable(AppRoutes.PREVIEW) {
                    val previewDocument by invoiceWorkspaceViewModel
                        .previewDocument
                        .collectAsState()
                    val finalizedDocument by invoiceWorkspaceViewModel
                        .finalizedDocument
                        .collectAsState()
                    val isFinalizing by invoiceWorkspaceViewModel
                        .isFinalizing
                        .collectAsState()

                    // Finalize completes asynchronously in the ViewModel; once it publishes a
                    // result, move forward to the finalized screen and drop "preview" from the
                    // back stack (the draft it showed no longer exists — back must not be able
                    // to return to it).
                    LaunchedEffect(finalizedDocument) {
                        if (finalizedDocument != null) {
                            navController.navigate(AppRoutes.FINALIZED) {
                                popUpTo(AppRoutes.PREVIEW) { inclusive = true }
                            }
                        }
                    }

                    requireNotNull(previewDocument) {
                        "Preview route entered without an active draft"
                    }

                    InvoicePreviewScreen(
                        document = previewDocument!!,
                        onBack = { navController.popBackStack() },
                        onFinalize = { invoiceWorkspaceViewModel.onFinalizeInvoice() },
                        isFinalizing = isFinalizing
                    )
                }

                composable(AppRoutes.FINALIZED) {
                    val finalizedDocument by invoiceWorkspaceViewModel
                        .finalizedDocument
                        .collectAsState()

                    requireNotNull(finalizedDocument) {
                        "Finalized route entered without a finalized document"
                    }

                    InvoiceFinalizedScreen(
                        document = finalizedDocument!!,
                        onViewDocument = { navController.navigate(AppRoutes.FINALIZED_DOCUMENT) }
                    )
                }

                composable(AppRoutes.FINALIZED_DOCUMENT) {
                    val finalizedDocument by invoiceWorkspaceViewModel
                        .finalizedDocument
                        .collectAsState()

                    requireNotNull(finalizedDocument) {
                        "Finalized document route entered without a finalized document"
                    }

                    InvoicePreviewScreen(
                        document = finalizedDocument!!,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private fun brandChrome(title: String): WorkspaceChromeSpec = WorkspaceChromeSpec(
    title = title,
    navigationIcon = VerityNavIcon.None,
    actions = emptyList(),
    chromeMode = VerityChromeMode.Brand
)

private fun supportChrome(title: String, onBack: () -> Unit): WorkspaceChromeSpec = WorkspaceChromeSpec(
    title = title,
    navigationIcon = VerityNavIcon.Back(onClick = onBack, contentDescription = "Back"),
    actions = emptyList(),
    chromeMode = VerityChromeMode.Support
)
