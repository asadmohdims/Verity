package com.verity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.verity.app.BuildConfig
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.feature.document.DocumentsListViewModel
import com.verity.feature.document.search.DocumentSearchViewModel
import com.verity.feature.home.HomeViewModel
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.ui.InvoiceWorkspaceViewModel
import com.verity.navigation.AppNavShell
import com.verity.platform.autocomplete.DefaultCustomerAutocompleteDataSource
import com.verity.platform.database.PlatformDatabaseFactory
import com.verity.platform.customer.DefaultCustomerRollupDataSource
import com.verity.platform.database.seed.CustomerSeedLoader
import com.verity.platform.database.seed.toEntity
import com.verity.platform.document.DefaultDocumentDetailDataSource
import com.verity.platform.document.DefaultDocumentSearchDataSource
import com.verity.platform.finalize.DEFAULT_ORG_ID
import com.verity.platform.finalize.DefaultInvoiceFinalizer
import com.verity.platform.home.DefaultHomeDataSource
import com.verity.platform.pdf.DefaultInvoicePdfRenderer
import com.verity.platform.sync.DefaultFirebaseRestoreClient
import com.verity.platform.sync.DefaultFirebaseSyncClient
import com.verity.platform.sync.DefaultInvoiceNumberAllocator
import com.verity.platform.sync.FirebaseAuthGate
import com.verity.platform.sync.FirestoreOnlineCounterSource
import com.verity.platform.sync.FirestoreRestoreSource
import com.verity.platform.sync.SyncStatusStore
import java.time.Clock

/**
 * MainActivity
 *
 * Composition root. Constructs the object graph (database, ViewModels) and hands off to
 * AppNavShell for the actual UI tree (chrome, navigation, screens) — no business logic, no
 * navigation logic lives here.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkTheme = false
            val navController = rememberNavController()
            val view = LocalView.current
            SideEffect {
                val controller = WindowInsetsControllerCompat(window, view)
                controller.isAppearanceLightStatusBars = !isDarkTheme
                controller.isAppearanceLightNavigationBars = !isDarkTheme
            }

            val context = LocalContext.current
            val database = remember { PlatformDatabaseFactory.create(context) }

            // Cloud sync wiring (see the cloud-sync plan) — constructed once at the composition
            // root, same manual-DI pattern as everything else here (no Hilt, see CLAUDE.md).
            val syncStatusStore = remember { SyncStatusStore(context = context) }
            val firebaseAuthGate = remember { FirebaseAuthGate(auth = FirebaseAuth.getInstance()) }
            val firebaseSyncClient = remember {
                DefaultFirebaseSyncClient(
                    firestore = FirebaseFirestore.getInstance(),
                    storage = FirebaseStorage.getInstance(),
                    documentDao = database.documentDao(),
                    ledgerEntryDao = database.ledgerEntryDao(),
                    syncStatusStore = syncStatusStore
                )
            }
            val invoiceNumberAllocator = remember {
                DefaultInvoiceNumberAllocator(
                    onlineCounterSource = FirestoreOnlineCounterSource(FirebaseFirestore.getInstance()),
                    documentDao = database.documentDao()
                )
            }
            val firebaseRestoreClient = remember {
                DefaultFirebaseRestoreClient(
                    remoteSource = FirestoreRestoreSource(FirebaseFirestore.getInstance()),
                    documentDao = database.documentDao(),
                    ledgerEntryDao = database.ledgerEntryDao()
                )
            }

            // One-time seed bootstrap: populate customers from the fixture asset on first run.
            LaunchedEffect(Unit) {
                if (database.customerDao().count() == 0) {
                    val seedCustomers = CustomerSeedLoader.load(context).map { it.toEntity() }
                    database.customerDao().upsertAll(seedCustomers)
                }
            }

            // Sign in with the fixed business account, then - only on a device with no local
            // documents yet, and only once ever - bulk-restore from the cloud (see
            // FirebaseRestoreClient's doc comment). A no-op on the existing primary device: its
            // Room already has data, so restoreAll() is never even attempted there.
            //
            // Bug fixed 2026-09-15: this used to check only the isInitialRestoreCompleted() flag,
            // not actual local document count - so on ANY device's first launch with this feature
            // (including an existing device with years of real local data), the flag started
            // false and restore ran anyway, merging in whatever happened to be in the cloud.
            // Caught by testing on a real device that already had local test data: a document
            // that only existed on a different device (the emulator) showed up here too. Restore
            // itself worked exactly as designed - only the trigger condition was wrong.
            LaunchedEffect(Unit) {
                val signedIn = firebaseAuthGate.ensureSignedIn(
                    email = BuildConfig.FIREBASE_AUTH_EMAIL,
                    password = BuildConfig.FIREBASE_AUTH_PASSWORD
                )
                val isNewDevice = database.documentDao().count() == 0
                if (signedIn && isNewDevice && !syncStatusStore.isInitialRestoreCompleted()) {
                    runCatching { firebaseRestoreClient.restoreAll(DEFAULT_ORG_ID) }
                        .onSuccess { syncStatusStore.markInitialRestoreCompleted() }
                }
            }

            // Shared across InvoiceWorkspaceViewModel and DocumentDetailViewModel — stateless,
            // so one Context-bound instance is enough for both.
            val invoicePdfRenderer = remember {
                DefaultInvoicePdfRenderer(
                    context = context,
                    syncClient = firebaseSyncClient,
                    orgId = DEFAULT_ORG_ID
                )
            }

            val invoiceWorkspaceViewModel = remember {
                InvoiceWorkspaceViewModel(
                    draftStore = InvoiceDraftStore(
                        initialDraft = InvoiceDraftUiState()
                    ),
                    customerAutocompleteDataSource = DefaultCustomerAutocompleteDataSource(
                        customerDao = database.customerDao()
                    ),
                    invoiceFinalizer = DefaultInvoiceFinalizer(
                        database = database,
                        clock = Clock.systemDefaultZone(),
                        numberAllocator = invoiceNumberAllocator,
                        syncClient = firebaseSyncClient
                    ),
                    invoicePdfRenderer = invoicePdfRenderer
                )
            }

            val homeDataSource = remember {
                DefaultHomeDataSource(database = database, syncStatusStore = syncStatusStore)
            }

            val homeViewModel = remember {
                HomeViewModel(
                    homeDataSource = homeDataSource,
                    clock = Clock.systemDefaultZone()
                )
            }

            val documentsListViewModel = remember {
                DocumentsListViewModel(homeDataSource = homeDataSource)
            }

            val documentDetailDataSource = remember {
                DefaultDocumentDetailDataSource(database = database)
            }

            val documentSearchViewModel = remember {
                DocumentSearchViewModel(
                    homeDataSource = homeDataSource,
                    searchDataSource = DefaultDocumentSearchDataSource(database = database)
                )
            }

            val customerRollupDataSource = remember {
                DefaultCustomerRollupDataSource(database = database)
            }

            VerityTheme(
                darkTheme = isDarkTheme,
                typography = VerityBaseTypography
            ) {
                AppNavShell(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    invoiceWorkspaceViewModel = invoiceWorkspaceViewModel,
                    documentsListViewModel = documentsListViewModel,
                    documentDetailDataSource = documentDetailDataSource,
                    documentSearchViewModel = documentSearchViewModel,
                    customerRollupDataSource = customerRollupDataSource,
                    invoicePdfRenderer = invoicePdfRenderer
                )
            }
        }
    }
}
