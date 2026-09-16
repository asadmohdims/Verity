package com.verity.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.verity.core.document.model.displayLabel
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
import com.verity.feature.customer.detail.CustomerDetail
import com.verity.feature.customer.detail.CustomerDetailDataSource
import com.verity.feature.customer.detail.CustomerDetailRoute
import com.verity.feature.customer.detail.CustomerDetailViewModel
import com.verity.feature.invoice.draft.DraftAddress
import com.verity.feature.customer.edit.AddEditCustomerRoute
import com.verity.feature.customer.edit.AddEditCustomerViewModel
import com.verity.feature.customer.edit.CustomerEditDataSource
import com.verity.feature.customer.list.CustomersListRoute
import com.verity.feature.customer.list.CustomersListViewModel
import com.verity.feature.document.DocumentDetailDataSource
import com.verity.feature.document.DocumentDetailViewModel
import com.verity.feature.document.DocumentsListRoute
import com.verity.feature.document.DocumentsListViewModel
import com.verity.feature.document.search.DocumentSearchRoute
import com.verity.feature.document.search.DocumentSearchViewModel
import com.verity.feature.home.HomeRoute
import com.verity.feature.home.HomeViewModel
import com.verity.feature.invoice.pdf.InvoicePdfRenderer
import com.verity.feature.invoice.pdf.PdfViewerScreen
import com.verity.feature.invoice.pdf.printPdf
import com.verity.feature.invoice.pdf.sharePdf
import com.verity.feature.invoice.ui.LineItemEntryRoute
import com.verity.feature.invoice.preview.InvoiceFinalizedScreen
import com.verity.feature.invoice.preview.InvoicePreviewScreen
import com.verity.feature.invoice.ui.InvoiceWorkspaceRoute
import com.verity.feature.invoice.ui.InvoiceWorkspaceViewModel
import com.verity.feature.referencelist.ManageReferenceListRoute
import com.verity.feature.referencelist.ManageReferenceListViewModel
import com.verity.feature.referencelist.ReferenceListDataSource
import com.verity.feature.referencelist.ReferenceListKind
import com.verity.feature.settings.SettingsRoute
import com.verity.feature.settings.SettingsViewModel
import java.io.File
import kotlinx.coroutines.launch

/**
 * Route constants for the whole app's single NavHost. The four tab roots (Home/Documents/
 * Customers/Settings) are Brand-mode, switched by the bottom nav, never pushed; Workspace/
 * Preview/Finalized are the existing invoice-creation flow, unchanged from before R-13, reached
 * by pushing on top of whichever tab was current when the FAB was tapped.
 *
 * FINALIZED is the confirmation screen (checkmark + "Invoice Finalized"); its "View Document"
 * button pushes FINALIZED_DOCUMENT, which reuses InvoicePreviewScreen as a read-only viewer — this
 * app has no separate Document Detail screen yet (Phase 2 per the UX roadmap). Its "View PDF"
 * button pushes PDF_VIEWER, which renders the actual generated PDF file via PdfViewerScreen —
 * distinct from FINALIZED_DOCUMENT's in-app Compose preview of the same document.
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
    const val PDF_VIEWER = "pdf_viewer"
    const val DOCUMENT_DETAIL = "document/{documentId}"
    const val DOCUMENT_PDF = "document/{documentId}/pdf"
    const val DOCUMENT_SEARCH = "documents/search"
    const val CUSTOMER_DETAIL = "customer/{customerId}"
    const val CUSTOMER_ADD = "customer/add"
    const val CUSTOMER_EDIT = "customer/{customerId}/edit"
    const val REFERENCE_LIST = "settings/reference-list/{kind}"
    const val LINE_ITEM_ADD = "workspace/line-item/add"
    const val LINE_ITEM_EDIT = "workspace/line-item/{index}/edit"

    fun documentDetail(documentId: String) = "document/$documentId"
    fun documentPdf(documentId: String) = "document/$documentId/pdf"
    fun customerDetail(customerId: String) = "customer/$customerId"
    fun customerEdit(customerId: String) = "customer/$customerId/edit"
    fun referenceList(kind: ReferenceListKind) = "settings/reference-list/${kind.name}"
    fun lineItemEdit(index: Int) = "workspace/line-item/$index/edit"
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
    invoiceWorkspaceViewModel: InvoiceWorkspaceViewModel,
    documentsListViewModel: DocumentsListViewModel,
    documentDetailDataSource: DocumentDetailDataSource,
    documentSearchViewModel: DocumentSearchViewModel,
    customerDetailDataSource: CustomerDetailDataSource,
    customerEditDataSource: CustomerEditDataSource,
    customersListViewModel: CustomersListViewModel,
    settingsViewModel: SettingsViewModel,
    invoicePdfRenderer: InvoicePdfRenderer,
    referenceListDataSource: ReferenceListDataSource
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTabRoute = bottomNavItems.any { it.route == currentRoute }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun goToTab(route: String) {
        navController.navigate(route) {
            popUpTo(AppRoutes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    fun goToWorkspace() {
        // Only start a fresh draft when none is active yet, so resuming an in-progress draft
        // (e.g. the user backed out mid-edit and tapped the FAB again) is left untouched. This
        // also guarantees WORKSPACE is never composed without an active draft — see
        // InvoiceWorkspaceRoute.
        if (!invoiceWorkspaceViewModel.hasActiveDraft.value) {
            invoiceWorkspaceViewModel.onCreateInvoice()
        } else {
            // onCreateInvoice() already refreshes reference-list suggestions for a genuinely new
            // draft; resuming an existing one skips that call entirely, so do it here instead —
            // covers switching to Settings to add an HSN/Unit/Transporter value mid-draft, then
            // returning via the FAB (see refreshReferenceListSuggestions()'s own doc comment).
            invoiceWorkspaceViewModel.refreshReferenceListSuggestions()
        }
        navController.navigate(AppRoutes.WORKSPACE)
    }

    /**
     * Share/Print top-bar actions for a PDF-viewing route. [resolveFile] resolves (and, if
     * needed, generates — see ensurePdf()/ensureFinalizedPdf()'s own doc comments) the exact File
     * to hand off, run in its own coroutine per tap rather than relying on a screen-local
     * produceState — this lives in the shared chrome computed above every route's own
     * composable() body, so it has no access to that local state.
     */
    fun pdfShareAndPrintActions(resolveFile: suspend () -> File): List<VerityTopBarAction> = listOf(
        VerityTopBarAction.Icon(
            icon = VerityIcons.Share,
            contentDescription = "Share PDF",
            onClick = { coroutineScope.launch { sharePdf(context, resolveFile()) } }
        ),
        VerityTopBarAction.Icon(
            icon = VerityIcons.Print,
            contentDescription = "Print PDF",
            onClick = { coroutineScope.launch { printPdf(context, resolveFile()) } }
        )
    )

    fun goToWorkspaceForCustomer(customer: CustomerDetail) {
        // Same "don't clobber an in-progress draft" rule as goToWorkspace() above — a customer
        // arrived here already known, so billed-to (and, by the existing default, shipped-to) is
        // prefilled the same way a manual autocomplete pick would set it.
        if (!invoiceWorkspaceViewModel.hasActiveDraft.value) {
            invoiceWorkspaceViewModel.onCreateInvoice(
                prefillBilledTo = DraftAddress(
                    name = customer.customerName,
                    gstin = customer.gstin,
                    addressLine1 = customer.addressLine1,
                    city = customer.city,
                    state = customer.state,
                    stateCode = customer.stateCode,
                    pincode = customer.pincode,
                    customerId = customer.customerId
                )
            )
        } else {
            // Same reasoning as goToWorkspace() above — resuming an existing draft otherwise
            // skips onCreateInvoice()'s refresh entirely.
            invoiceWorkspaceViewModel.refreshReferenceListSuggestions()
        }
        navController.navigate(AppRoutes.WORKSPACE)
    }

    val workspaceChromeSpec by invoiceWorkspaceViewModel.chromeSpec.collectAsState()
    val previewDocument by invoiceWorkspaceViewModel.previewDocument.collectAsState()
    val finalizedDocument by invoiceWorkspaceViewModel.finalizedDocument.collectAsState()

    val chromeSpecWithNavigation = remember(workspaceChromeSpec, previewDocument) {
        val canPreview = previewDocument != null

        workspaceChromeSpec.copy(
            navigationIcon = when (val icon = workspaceChromeSpec.navigationIcon) {
                // The ViewModel builds this with a placeholder onClick ("handled at root") since
                // it doesn't own navigation — root must actually wire it, same as the Preview
                // action below. Previously left unwired, so the Workspace screen's back arrow was
                // a silent no-op and never returned to Home.
                is VerityNavIcon.Back -> icon.copy(onClick = { navController.popBackStack() })
                VerityNavIcon.None -> icon
            },
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
        AppRoutes.HOME -> brandChrome(title = "Verity", isEntrySurface = true)
        AppRoutes.DOCUMENTS -> brandChrome(
            title = "Documents",
            actions = listOf(
                VerityTopBarAction.Icon(
                    icon = VerityIcons.Search,
                    contentDescription = "Search documents",
                    onClick = { navController.navigate(AppRoutes.DOCUMENT_SEARCH) }
                )
            )
        )
        AppRoutes.CUSTOMERS -> brandChrome(
            title = "Customers",
            actions = listOf(
                VerityTopBarAction.Icon(
                    icon = VerityIcons.Add,
                    contentDescription = "Add customer",
                    onClick = { navController.navigate(AppRoutes.CUSTOMER_ADD) }
                )
            )
        )
        AppRoutes.SETTINGS -> brandChrome(title = "Settings")
        AppRoutes.REFERENCE_LIST -> {
            val kind = navBackStackEntry?.arguments?.getString("kind")
                ?.let { runCatching { ReferenceListKind.valueOf(it) }.getOrNull() }
            supportChrome(
                title = when (kind) {
                    ReferenceListKind.TRANSPORTER_NAME -> "Transporter Names"
                    ReferenceListKind.HSN_CODE -> "HSN Codes"
                    ReferenceListKind.UNIT -> "Units"
                    null -> "Reference List"
                }
            ) { navController.popBackStack() }
        }
        AppRoutes.PREVIEW -> supportChrome(
            title = "${previewDocument?.identity?.documentType?.displayLabel ?: "Invoice"} Preview"
        ) { navController.popBackStack() }
        AppRoutes.FINALIZED -> supportChrome(
            title = "${finalizedDocument?.identity?.documentType?.displayLabel ?: "Invoice"} Finalized"
        ) { navController.popBackStack() }
        AppRoutes.FINALIZED_DOCUMENT ->
            supportChrome(
                title = finalizedDocument?.identity?.documentNumber ?: "Invoice"
            ) { navController.popBackStack() }
        AppRoutes.PDF_VIEWER ->
            supportChrome(
                title = finalizedDocument?.identity?.documentNumber ?: "Invoice",
                // ensureFinalizedPdf() is idempotent (see its own doc comment) — safe to call
                // again here even though PdfViewerScreen's own produceState already resolved it
                // once; this action lives in the shared top-bar chrome, one level above that
                // per-route composable, so it can't just read that local state.
                actions = pdfShareAndPrintActions { invoiceWorkspaceViewModel.ensureFinalizedPdf() }
            ) { navController.popBackStack() }
        AppRoutes.DOCUMENT_DETAIL ->
            supportChrome(title = "Document") { navController.popBackStack() }
        AppRoutes.DOCUMENT_PDF -> {
            val documentId = navBackStackEntry?.arguments?.getString("documentId")
            supportChrome(
                title = "Document",
                actions = if (documentId != null) {
                    // Same idempotent-reuse reasoning as PDF_VIEWER above, but there's no shared,
                    // root-level ViewModel to reuse here — DocumentDetailViewModel is scoped per
                    // nav back-stack entry, created inside DOCUMENT_PDF's own composable() block
                    // below, not reachable from this shared chrome code. documentDetailDataSource/
                    // invoicePdfRenderer are the same plain dependencies that ViewModel is built
                    // from, so this is a small, obviously-correct re-derivation, not a second
                    // source of truth.
                    pdfShareAndPrintActions {
                        val document = requireNotNull(documentDetailDataSource.loadDocument(documentId)) {
                            "No document found for id $documentId"
                        }
                        invoicePdfRenderer.ensurePdf(document)
                    }
                } else {
                    emptyList()
                }
            ) { navController.popBackStack() }
        }
        AppRoutes.DOCUMENT_SEARCH ->
            supportChrome(title = "Search") { navController.popBackStack() }
        AppRoutes.CUSTOMER_DETAIL -> {
            val customerId = navBackStackEntry?.arguments?.getString("customerId")
            supportChrome(
                title = "Customer",
                onBack = { navController.popBackStack() },
                actions = if (customerId != null) {
                    listOf(
                        VerityTopBarAction.Icon(
                            icon = VerityIcons.Edit,
                            contentDescription = "Edit customer",
                            onClick = { navController.navigate(AppRoutes.customerEdit(customerId)) }
                        )
                    )
                } else {
                    emptyList()
                }
            )
        }
        AppRoutes.CUSTOMER_ADD ->
            supportChrome(title = "Add Customer") { navController.popBackStack() }
        AppRoutes.CUSTOMER_EDIT ->
            supportChrome(title = "Edit Customer") { navController.popBackStack() }
        AppRoutes.LINE_ITEM_ADD ->
            supportChrome(title = "Add Line Item") { navController.popBackStack() }
        AppRoutes.LINE_ITEM_EDIT ->
            supportChrome(title = "Edit Line Item") { navController.popBackStack() }
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
                        onSeeAllDocuments = { goToTab(AppRoutes.DOCUMENTS) },
                        onDocumentClick = { documentId ->
                            navController.navigate(AppRoutes.documentDetail(documentId))
                        }
                    )
                }

                composable(AppRoutes.DOCUMENTS) {
                    DocumentsListRoute(
                        viewModel = documentsListViewModel,
                        onDocumentClick = { documentId ->
                            navController.navigate(AppRoutes.documentDetail(documentId))
                        }
                    )
                }

                composable(AppRoutes.DOCUMENT_SEARCH) {
                    DocumentSearchRoute(
                        viewModel = documentSearchViewModel,
                        onDocumentClick = { documentId ->
                            navController.navigate(AppRoutes.documentDetail(documentId))
                        },
                        onCustomerClick = { customerId ->
                            navController.navigate(AppRoutes.customerDetail(customerId))
                        }
                    )
                }

                composable(
                    route = AppRoutes.CUSTOMER_DETAIL,
                    arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val customerId = requireNotNull(backStackEntry.arguments?.getString("customerId"))

                    val customerDetailViewModel: CustomerDetailViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                CustomerDetailViewModel(
                                    customerId = customerId,
                                    dataSource = customerDetailDataSource
                                )
                            }
                        }
                    )

                    CustomerDetailRoute(
                        viewModel = customerDetailViewModel,
                        onDocumentClick = { documentId ->
                            navController.navigate(AppRoutes.documentDetail(documentId))
                        },
                        onNewInvoice = ::goToWorkspaceForCustomer
                    )
                }

                composable(AppRoutes.CUSTOMER_ADD) {
                    val addCustomerViewModel: AddEditCustomerViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                AddEditCustomerViewModel(
                                    customerId = null,
                                    dataSource = customerEditDataSource
                                )
                            }
                        }
                    )

                    AddEditCustomerRoute(
                        viewModel = addCustomerViewModel,
                        onDone = { navController.popBackStack() }
                    )
                }

                composable(
                    route = AppRoutes.CUSTOMER_EDIT,
                    arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val customerId = requireNotNull(backStackEntry.arguments?.getString("customerId"))

                    val editCustomerViewModel: AddEditCustomerViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                AddEditCustomerViewModel(
                                    customerId = customerId,
                                    dataSource = customerEditDataSource
                                )
                            }
                        }
                    )

                    AddEditCustomerRoute(
                        viewModel = editCustomerViewModel,
                        onDone = { navController.popBackStack() }
                    )
                }

                composable(AppRoutes.CUSTOMERS) {
                    CustomersListRoute(
                        viewModel = customersListViewModel,
                        onCustomerClick = { customerId ->
                            navController.navigate(AppRoutes.customerDetail(customerId))
                        }
                    )
                }

                composable(AppRoutes.SETTINGS) {
                    SettingsRoute(
                        viewModel = settingsViewModel,
                        onManageTransporterNames = {
                            navController.navigate(AppRoutes.referenceList(ReferenceListKind.TRANSPORTER_NAME))
                        },
                        onManageHsnCodes = {
                            navController.navigate(AppRoutes.referenceList(ReferenceListKind.HSN_CODE))
                        },
                        onManageUnits = {
                            navController.navigate(AppRoutes.referenceList(ReferenceListKind.UNIT))
                        }
                    )
                }

                composable(
                    route = AppRoutes.REFERENCE_LIST,
                    arguments = listOf(navArgument("kind") { type = NavType.StringType })
                ) { backStackEntry ->
                    val kind = ReferenceListKind.valueOf(
                        requireNotNull(backStackEntry.arguments?.getString("kind"))
                    )

                    val referenceListViewModel: ManageReferenceListViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                ManageReferenceListViewModel(
                                    kind = kind,
                                    dataSource = referenceListDataSource
                                )
                            }
                        }
                    )

                    ManageReferenceListRoute(viewModel = referenceListViewModel)
                }

                composable(AppRoutes.WORKSPACE) {
                    InvoiceWorkspaceRoute(
                        viewModel = invoiceWorkspaceViewModel,
                        onAddLineItem = { navController.navigate(AppRoutes.LINE_ITEM_ADD) },
                        onEditLineItem = { index ->
                            navController.navigate(AppRoutes.lineItemEdit(index))
                        }
                    )
                }

                composable(AppRoutes.LINE_ITEM_ADD) {
                    LineItemEntryRoute(
                        viewModel = invoiceWorkspaceViewModel,
                        editingIndex = null,
                        onDone = { navController.popBackStack() }
                    )
                }

                composable(
                    route = AppRoutes.LINE_ITEM_EDIT,
                    arguments = listOf(navArgument("index") { type = NavType.IntType })
                ) { backStackEntry ->
                    val index = backStackEntry.arguments?.getInt("index") ?: 0
                    LineItemEntryRoute(
                        viewModel = invoiceWorkspaceViewModel,
                        editingIndex = index,
                        onDone = { navController.popBackStack() }
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

                    // Snapshot of finalizedDocument as it stood when this Preview was entered.
                    // Needed because the job-work "Continue to Invoice" flow deliberately leaves
                    // finalizedDocument holding the just-finalized Challan rather than nulling it
                    // eagerly (see onContinueToJobWorkInvoice()'s comment - nulling it there raced
                    // the still-composed "finalized" route's requireNotNull and crashed). That
                    // means finalizedDocument can already be non-null the moment this Preview is
                    // entered for the follow-up Invoice draft - a naive "!= null" check below
                    // would immediately bounce straight back to that stale Challan's Finalized
                    // screen before this Invoice is ever finalized, without ever showing Preview.
                    // Only a document that's actually different from what was here on entry means
                    // *this* draft's finalize genuinely just completed.
                    val finalizedDocumentOnEntry = remember { finalizedDocument }

                    // Finalize completes asynchronously in the ViewModel; once it publishes a new
                    // result, move forward to the finalized screen and drop both "preview" AND
                    // "workspace" from the back stack — the draft they showed no longer exists
                    // (onFinalizeInvoice() clears hasActiveDraft), so back from "finalized" must
                    // land on the tab underneath, not on a stale workspace entry.
                    LaunchedEffect(finalizedDocument) {
                        if (finalizedDocument != null && finalizedDocument != finalizedDocumentOnEntry) {
                            navController.navigate(AppRoutes.FINALIZED) {
                                popUpTo(AppRoutes.WORKSPACE) { inclusive = true }
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
                        onViewDocument = { navController.navigate(AppRoutes.FINALIZED_DOCUMENT) },
                        onViewPdf = { navController.navigate(AppRoutes.PDF_VIEWER) },
                        onContinueToJobWorkInvoice = {
                            invoiceWorkspaceViewModel.onContinueToJobWorkInvoice()
                            navController.navigate(AppRoutes.WORKSPACE) {
                                popUpTo(AppRoutes.FINALIZED) { inclusive = true }
                            }
                        }
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
                        onBack = { navController.popBackStack() },
                        // Only ever resolvable here for the Invoice side of a job-work pair —
                        // its jobWorkLink.linkedDocumentId is written at that Invoice's own
                        // finalize time (see DefaultInvoiceFinalizer). The Challan side's link
                        // never resolves this way (that id doesn't exist at Challan-finalize
                        // time) — see DocumentDetailViewModel.linkedDocumentId for the reverse
                        // lookup used once a Challan is reopened from Documents/Home instead.
                        onViewLinkedDocument = finalizedDocument!!.jobWorkLink?.linkedDocumentId?.let { linkedId ->
                            { navController.navigate(AppRoutes.documentDetail(linkedId)) }
                        }
                    )
                }

                composable(AppRoutes.PDF_VIEWER) {
                    val finalizedDocument by invoiceWorkspaceViewModel
                        .finalizedDocument
                        .collectAsState()

                    requireNotNull(finalizedDocument) {
                        "PDF viewer route entered without a finalized document"
                    }

                    // ensureFinalizedPdf() is idempotent - safe to call every time this route is
                    // entered, whether or not the eager attempt in onFinalizeInvoice() succeeded.
                    val pdfFile by produceState<File?>(initialValue = null, finalizedDocument) {
                        value = invoiceWorkspaceViewModel.ensureFinalizedPdf()
                    }

                    PdfViewerScreen(file = pdfFile)
                }

                composable(
                    route = AppRoutes.DOCUMENT_DETAIL,
                    arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val documentId = requireNotNull(backStackEntry.arguments?.getString("documentId"))

                    val documentDetailViewModel: DocumentDetailViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                DocumentDetailViewModel(
                                    documentId = documentId,
                                    dataSource = documentDetailDataSource,
                                    invoicePdfRenderer = invoicePdfRenderer
                                )
                            }
                        }
                    )
                    val document by documentDetailViewModel.document.collectAsState()
                    val linkedDocumentId by documentDetailViewModel.linkedDocumentId.collectAsState()

                    val loadedDocument = document
                    if (loadedDocument == null) {
                        DocumentLoadingIndicator()
                    } else {
                        InvoicePreviewScreen(
                            document = loadedDocument,
                            onBack = { navController.popBackStack() },
                            onViewPdf = {
                                navController.navigate(AppRoutes.documentPdf(documentId))
                            },
                            onViewLinkedDocument = linkedDocumentId?.let { linkedId ->
                                { navController.navigate(AppRoutes.documentDetail(linkedId)) }
                            }
                        )
                    }
                }

                composable(
                    route = AppRoutes.DOCUMENT_PDF,
                    arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val documentId = requireNotNull(backStackEntry.arguments?.getString("documentId"))

                    val documentDetailViewModel: DocumentDetailViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                DocumentDetailViewModel(
                                    documentId = documentId,
                                    dataSource = documentDetailDataSource,
                                    invoicePdfRenderer = invoicePdfRenderer
                                )
                            }
                        }
                    )
                    val document by documentDetailViewModel.document.collectAsState()

                    // ensurePdf() is idempotent - safe to call every time this route is entered.
                    val pdfFile by produceState<File?>(initialValue = null, document) {
                        value = if (document != null) documentDetailViewModel.ensurePdf() else null
                    }

                    PdfViewerScreen(file = pdfFile)
                }
            }
        }
    }
}

@Composable
private fun DocumentLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = VerityTheme.colors.primary)
    }
}

private fun brandChrome(
    title: String,
    actions: List<VerityTopBarAction> = emptyList(),
    isEntrySurface: Boolean = false
): WorkspaceChromeSpec = WorkspaceChromeSpec(
    title = title,
    navigationIcon = VerityNavIcon.None,
    actions = actions,
    chromeMode = VerityChromeMode.Brand(isEntrySurface = isEntrySurface)
)

private fun supportChrome(
    title: String,
    actions: List<VerityTopBarAction> = emptyList(),
    onBack: () -> Unit
): WorkspaceChromeSpec = WorkspaceChromeSpec(
    title = title,
    navigationIcon = VerityNavIcon.Back(onClick = onBack, contentDescription = "Back"),
    actions = actions,
    chromeMode = VerityChromeMode.Support
)
