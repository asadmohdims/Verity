package com.verity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.verity.app.BuildConfig
import com.verity.core.theme.ThemeMode
import com.verity.core.theme.VerityBaseTypography
import com.verity.core.theme.VerityTheme
import com.verity.feature.customer.list.CustomersListViewModel
import com.verity.feature.document.DocumentsListViewModel
import com.verity.feature.document.search.DocumentSearchViewModel
import com.verity.feature.home.HomeViewModel
import com.verity.feature.invoice.draft.InvoiceDraftStore
import com.verity.feature.invoice.draft.InvoiceDraftUiState
import com.verity.feature.invoice.ui.InvoiceWorkspaceViewModel
import com.verity.feature.settings.SettingsViewModel
import com.verity.navigation.AppNavShell
import com.verity.platform.autocomplete.DefaultCustomerAutocompleteDataSource
import com.verity.platform.customer.DefaultCustomerDetailDataSource
import com.verity.platform.customer.DefaultCustomerEditDataSource
import com.verity.platform.customer.DefaultCustomerListDataSource
import com.verity.platform.database.PlatformDatabaseFactory
import com.verity.platform.database.seed.CustomerSeedLoader
import com.verity.platform.database.seed.toEntity
import com.verity.platform.document.DefaultDocumentDetailDataSource
import com.verity.platform.document.DefaultDocumentSearchDataSource
import com.verity.platform.finalize.DEFAULT_ORG_ID
import com.verity.platform.finalize.DefaultDocumentNumberPreviewDataSource
import com.verity.platform.finalize.DefaultInvoiceFinalizer
import com.verity.platform.home.DefaultHomeDataSource
import com.verity.platform.pdf.DefaultInvoicePdfRenderer
import com.verity.platform.referencelist.DefaultReferenceListDataSource
import com.verity.platform.settings.ThemePreferenceStore
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
            val context = LocalContext.current
            val themePreferenceStore = remember { ThemePreferenceStore(context = context) }
            val themeMode by themePreferenceStore.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val navController = rememberNavController()
            val view = LocalView.current
            SideEffect {
                val controller = WindowInsetsControllerCompat(window, view)
                controller.isAppearanceLightStatusBars = !isDarkTheme
                controller.isAppearanceLightNavigationBars = !isDarkTheme
            }

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
                    customerDao = database.customerDao(),
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
                    ledgerEntryDao = database.ledgerEntryDao(),
                    customerDao = database.customerDao(),
                    watermarkStore = syncStatusStore
                )
            }

            // Sign in, pull anything new since this device last synced (documents, ledger
            // entries, and — as of the customerId-divergence fix — customers too), THEN seed
            // fixture customers only if the table is still empty, THEN push anything local that's
            // never been pushed. Order matters: this used to be two independent LaunchedEffects
            // racing each other, and the local seed (fast: JSON parse + Room insert) always won
            // against restore (two network round trips) — every fresh device generated its own
            // random customer ids before any cloud data could possibly arrive, which is exactly
            // the bug this whole sequence exists to prevent (see CLAUDE.md's Data & sync
            // architecture). sync() itself is safe to call on every launch regardless of existing
            // local data (see FirebaseRestoreClient's own doc comment) — filtered on a
            // Firestore-assigned server timestamp, not a client clock, so an established device
            // only ever re-fetches what's genuinely new since it last checked.
            LaunchedEffect(Unit) {
                val signedIn = firebaseAuthGate.ensureSignedIn(
                    email = BuildConfig.FIREBASE_AUTH_EMAIL,
                    password = BuildConfig.FIREBASE_AUTH_PASSWORD
                )
                if (signedIn) {
                    runCatching { firebaseRestoreClient.sync(DEFAULT_ORG_ID) }
                }

                // Only a genuinely first-ever launch for this org (nothing local, nothing in the
                // cloud to restore) — or this exact launch was offline — reaches here with zero
                // customers.
                if (database.customerDao().count() == 0) {
                    val seedCustomers = CustomerSeedLoader.load(context).map { it.toEntity() }
                    database.customerDao().upsertAll(seedCustomers)
                    seedCustomers.forEach { firebaseSyncClient.pushCustomer(it) }
                }

                // Cheap, idempotent no-op once everything is synced — but on the first launch
                // after this feature shipped, this is what pushes a device's pre-existing local
                // customers (every row defaults to syncedToCloud=false after Migration5To6) to
                // the cloud for the first time, establishing them as the canonical set future
                // devices adopt instead of reseeding their own.
                database.customerDao().getUnsyncedCustomers().forEach { firebaseSyncClient.pushCustomer(it) }
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

            val referenceListDataSource = remember {
                DefaultReferenceListDataSource(referenceListDao = database.referenceListDao())
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
                    referenceListDataSource = referenceListDataSource,
                    invoicePdfRenderer = invoicePdfRenderer,
                    documentNumberPreviewDataSource = DefaultDocumentNumberPreviewDataSource(
                        documentDao = database.documentDao()
                    )
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
                DefaultDocumentDetailDataSource(database = database, syncClient = firebaseSyncClient)
            }

            val documentSearchViewModel = remember {
                DocumentSearchViewModel(
                    homeDataSource = homeDataSource,
                    searchDataSource = DefaultDocumentSearchDataSource(database = database)
                )
            }

            val customerDetailDataSource = remember {
                DefaultCustomerDetailDataSource(database = database)
            }

            val customerEditDataSource = remember {
                DefaultCustomerEditDataSource(database = database, syncClient = firebaseSyncClient)
            }

            val customersListViewModel = remember {
                CustomersListViewModel(
                    dataSource = DefaultCustomerListDataSource(database = database)
                )
            }

            val settingsViewModel = remember {
                SettingsViewModel(
                    themeSettingsDataSource = themePreferenceStore,
                    homeDataSource = homeDataSource,
                    appVersionLabel = BuildConfig.VERSION_NAME
                )
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
                    customerDetailDataSource = customerDetailDataSource,
                    customerEditDataSource = customerEditDataSource,
                    customersListViewModel = customersListViewModel,
                    settingsViewModel = settingsViewModel,
                    invoicePdfRenderer = invoicePdfRenderer,
                    referenceListDataSource = referenceListDataSource
                )
            }
        }
    }
}
