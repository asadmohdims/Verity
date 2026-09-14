# Verity — Engineering Reference

Verity is a GST invoicing app (Invoices & Challans) built for real use, starting with a single
business, with multi-org support planned once it's actually needed. It's also the author's
vehicle for rebuilding hands-on fluency in modern Android/Kotlin/Compose — architecture and
correctness matter here, but so does keeping things comprehensible to someone relearning the
ecosystem after ~10 years away from hands-on coding.

This file is a **living reference**, not a constitution. Update it in place as decisions change
— no amendment ceremony required. If something here turns out to be wrong or stale, fix it here.

## Status (read this before trusting anything else below)

As of 2026-09-10, Verity has (Milestone 1 — local invoice persistence — complete and verified
end-to-end on-device, not just unit-tested):
- A working Invoice Workspace UI: draft creation, line items, transport details, GST tax
  computation, and preview, wired to a **real local Room database**. Customer autocomplete
  works against seeded fixture data (25 customers). Finalize assigns a real sequential number
  (`INV-000001`, `INV-000002`, ...), persists the document (as a JSON snapshot of
  `InvoiceDocumentModel`) and a matching ledger entry in one transaction, and shows a genuine
  "Invoice Finalized" screen. Verified by creating and finalizing three invoices in one session
  and inspecting the on-device sqlite database directly — not just by reading code or running
  `./gradlew test`. Confirmed idempotent seeding across two full process kills.
- The old event-sourcing design (`EventEntity`, ledger/document-index replay engines) has been
  **deleted**, not just deprecated — replaced by the simpler `DocumentEntity`/`LedgerEntryEntity`
  schema described below.
- The seller printed on every document is still a **hardcoded placeholder** (`core/.../
  HardcodedSeller.kt`) — needs real business details before this is used for anything but
  testing.
- Zero cloud/network code — no Supabase, no Retrofit/Ktor, no `INTERNET` permission yet.
- Zero PDF/print/share — explicitly out of scope for Milestone 1; finalize stops at the
  in-app "Invoice Finalized" screen.
- Two real bugs were found only by actually running the app (not by tests, which all passed):
  `previewDocument`'s `StateFlow` never started computing because of a `WhileSubscribed`/
  `.value`-read ordering deadlock, and `onFinalizeInvoice()` raced its own navigation by
  resetting the draft store synchronously in the same recomposition pass that was supposed to
  navigate away first. Both fixed — see git history on this file's directory for the exact
  sequencing reasoning if a similar pattern reappears.

Nothing is "done" here until it's demonstrably reachable from the running app. A well-documented,
well-tested class sitting in isolation is not a finished feature — see Principle 2 below. Keep
this section honest and current; that's the entire reason it exists.

A navigation/IA redesign (Home dashboard landing screen, bottom nav, Documents/Customers/Settings)
was proposed and **approved by the user on 2026-09-11** — see "UX Direction" below. Phase 1
(R-13: the nav shell + Home dashboard) is **built and merged to `main`** (2026-09-13, fast-forward
from `feature/r13-nav-and-home`, no merge commit) — see "Build Roadmap" in the UX Direction section
for what that covers and what's still Phase 2/3. `main` had been stale since before Milestone 1;
this merge is what brought it current.

A GST tax invoice **PDF design** — the artifact that makes an in-app "finalize" a physically real
document, per "PDF generation is the finish line" under Data & Sync below — was finalized on
2026-09-14: "Familiar Grid, Modernized", a bordered-grid skeleton (the layout Indian GST software/
auditors expect, for fast field lookup) restyled with real typographic hierarchy and a navy +
brass accent system. Full mockups, a stress test (Ship-To genuinely differing from Bill-To, 10
line items), and the rejected "Whitespace Document" alternative live at
`https://claude.ai/code/artifact/7c9bd3c7-f1f9-4104-89de-645acf683abc`.

**PDF generation is now implemented** (2026-09-14, same day): `DefaultInvoicePdfRenderer`
(`platform/.../pdf/`) draws `InvoiceDocumentModel` to a real PDF with `android.graphics.pdf.
PdfDocument`/`Canvas` — no new dependency, fully on-device/offline (no network, no downloadable
fonts). Stored deterministically at `getExternalFilesDir("documents")/{documentNumber}.pdf` — no
DB schema change, since `DocumentEntity` is insert-only and the path is always derivable from the
already-unique `documentNumber`. `onFinalizeInvoice()` generates it eagerly right after finalize;
the same `ensurePdf()` call also runs defensively whenever the PDF viewer opens, so a failed eager
attempt self-heals without dedicated retry UI. Viewing is a hand-rolled Compose screen
(`PdfViewerScreen`, `feature/.../pdf/`) on the stable `android.graphics.pdf.PdfRenderer` API — not
the newer `androidx.pdf` Compose library, which is still pre-1.0 alpha. The Finalized screen has a
new "View PDF" action alongside the existing "View Document" (the unrelated in-app Compose
preview). The line-item table paginates across pages rather than assuming everything fits on one
(silently truncating a financial document's line items would be a real bug, not a cosmetic one).

Two model gaps the design needed were closed: **Place of Supply** (`DocumentIdentity.
placeOfSupplyState`/`placeOfSupplyStateCode`, auto-computed from Shipped To, falling back to
Billed To) and **E-Way Bill Number** (`DocumentLogistics.ewayBillNumber`, now a real field in the
Transportation edit block). Two more surfaced while implementing the actual design spec, not
originally scoped: a **Reverse Charge** flag (`DocumentIdentity.reverseChargeApplicable`, wired
from `InvoiceDraftUiState.reverseCharge` — a field that already existed but was never connected to
anything) and an **Amount in Words** line (`core/.../formatting/money/AmountInWords.kt`, Indian
lakh/crore grouping, unit-tested directly against the design artifact's own worked examples).
`SellerDetails` gained the bank/MSME/contact/terms fields the design assumes — most are now filled
with Unitech Machineries' real details sourced from the manual invoice reviewed during the design
pass, **not fabricated placeholders**, except `pincode` (that source never stated it) which is
still `PLACEHOLDER_PINCODE` and needs a real value.

Full build (`./gradlew assembleDebug`) and all unit tests (`core`/`feature`/`platform`) pass as of
this write-up. **Not yet done**: an on-device visual pass against the design artifact — per this
file's own Compose-testing standards, green tests are not proof a rendered page matches its
mockup, and that's doubly true for a from-scratch Canvas-drawn PDF that was never pixel-diffed
against `Main.dc.html`. Also still open: Challan PDF generation (this pass was Invoice-only),
Share/Print/Export and the `FileProvider` it needs (Phase 4 of the UX roadmap), and a real Business
Profile screen to replace `HardcodedSeller.kt` (Phase 3).

## Who this is for

- Primary user: the app owner's own business, invoicing customers under Indian GST rules.
- Multi-organization support is a real future goal, not an immediate requirement. Keep it cheap
  (e.g. an `orgId` column present everywhere) — don't build tenant-management infrastructure
  before it's actually needed.

## Guiding engineering principles

1. **Earn your abstractions.** Don't build a generic system for two cases. Event sourcing,
   plugin architectures, generic replay engines — these are justified by variety and scale you
   actually have, not variety you might have someday. If you're building infrastructure and
   can't name the third concrete case it serves today, stop.
2. **Definition of done = wired and verified, not just unit-tested.** A class with full test
   coverage that nothing calls is not shipped. Every feature must be exercised end-to-end (run
   the app, actually see it work) before it's marked complete.
3. **Single ownership, always.** Every piece of mutable state has exactly one owner. Concrete
   instance: a Compose `TextField`'s `value` is owned by local UI/query state — never by draft or
   domain state, never derived conditionally from multiple sources. Ownership conflicts here are
   a well-known, very real Compose bug class (cursor snap-back, un-deletable input), not a style
   nitpick.
4. **Idempotency over cleverness for offline/sync correctness.** Client-generated UUID primary
   keys + `upsert` on sync beats hand-rolled retry/backoff logic. See Data & Sync below.
5. **Immutability where money and compliance are involved.** Finalized invoices/challans are
   never edited in place. Corrections are new, additive facts (credit notes, amendments), never
   mutations of the original.
6. **Local-first UX.** A user action (saving a draft, finalizing a document) never blocks on
   network availability. Write locally, sync in the background, surface sync status — don't make
   the user wait on a round trip to feel like their work was saved.
7. **No speculative flexibility.** Don't add a config option, interface, or extension point for a
   future requirement that isn't concrete yet.

## Architecture — module boundaries

- **`app`** — composition root only. `MainActivity`, NavHost, ViewModel instantiation, chrome
  rendering. No business logic.
- **`core`** — pure Kotlin/Compose. Design system (`Verity*` components), theme tokens,
  money/date formatting, domain models that touch neither Android nor persistence. No platform
  or feature knowledge.
- **`platform`** — the only module allowed to touch Room, KSP, or (once it exists) Supabase/
  network code. Infrastructure only, no UI dependencies.
- **`feature`** — one package per feature (currently `invoice`). Compose screens, ViewModels,
  feature-local state. No cross-feature dependencies; features reach persistence only through
  interfaces `platform` implements.

Route / Screen / ViewModel ownership:
- **Route** (composable): obtains ViewModels, collects `StateFlow`, passes state + callbacks
  down. May reference ViewModels.
- **Screen** (composable): pure UI renderer. Never obtains a ViewModel, never collects a Flow
  directly.
- **ViewModel**: instantiated at the app/NavHost root, injected into Routes only.

Namespace: `applicationId` is `com.verity`; every module's namespace roots at
`com.verity.<module>`. No customer, environment, or developer name in package paths — the
project was originally built for one company and later generalized; don't reintroduce that
coupling.

## Domain model

- **Invoice**, **Challan**, **Payment** are financially/legally significant — immutable once
  finalized. Corrections are additive events (credit notes / amendments), never in-place edits.
- **Customer** is supporting identity data — plain CRUD, not event-sourced. Snapshot customer
  identity into a document at finalization time so a later customer edit never retroactively
  changes a historical document.
- **Challan → Invoice (job work)**: at Challan finalization, the user explicitly declares intent
  — Standalone or Invoice-Linked (job-work) — this is always known upfront, never decided later.
  Every finalized Challan gets its own PDF **immediately at finalization**, identical to Invoice
  (same finalize → PDF → background-sync pattern, PDF is the finish line either way) —
  regardless of whether it's later linked to an invoice, because goods move now and something
  has to travel with them. The original Challan PDF is never regenerated once issued. If later
  linked, `DocumentLinkCreated` is an additive fact surfaced in-app (document view/search), not
  retroactively added to the already-issued PDF. A Challan links to at most one Invoice; not
  copied into the linked Invoice draft: the Challan's goods value, dates, payments, or ledger
  effects — only the job-work service actually being billed. Linking from a different device
  than the one that created the Challan requires that device to have already synced the Challan
  down first — no special handling for this beyond waiting for sync, consistent with the
  numbering and durability decisions above.
- **GST tax**: CGST+SGST for intra-state, IGST for inter-state, determined by comparing buyer
  state code to seller state code. Computed once, deterministically, from line items + freight —
  never recomputed or overridden in the UI layer.
- All monetary values are `Long` minor units (paise) end to end. Never `Double`, never floating
  point, anywhere in the money path.
- **E-Way Bill**: an optional reference field (e-way bill number) lives on Transportation
  Details, alongside transporter/vehicle/GR-LR/freight — applies uniformly to Invoice and
  Challan since Transportation Mode is already a shared, unified concept across both. Deliberately
  just a reference field for now: no threshold-based requirement rule, no validation, no
  generation or integration with the government e-way bill portal. Reserved because it's cheap to
  add, not because the requirement is confirmed — whether this needs to be more than a plain
  optional field is still open.

## Data & sync architecture (local half built and verified; cloud half not yet built — see Status)

Replaced the original generic event-sourcing/replay design with something simpler that still
meets the real requirement (immutable finalized documents, auditable corrections):

- **Room is the local source of truth.** Every write lands in Room first and returns immediately
  — the UI never waits on network. **Built and verified**: `DocumentEntity` (one table for
  Invoice and Challan, JSON payload + indexed columns) and `LedgerEntryEntity` (append-only) are
  live; the outbox/WorkManager/Supabase points below are still just the plan, not yet built.
- **A local outbox table**, keyed by a client-generated UUID, holds pending writes. A background
  job (WorkManager: periodic + an expedited one-off enqueued right after every local write) syncs
  the outbox to Supabase via `upsert()` on that same UUID. Retries are naturally safe because
  upsert-by-UUID can't create duplicates. This is deliberately copied from a pattern already
  proven reliable in a sibling project (Unitech Attendance), not a first attempt.
- **Supabase (Postgres + Auth + Realtime)** is the cloud backend. Real Row Level Security from
  day one — every table scoped by `orgId`, policy-enforced, never `using (true)`. Per-user
  accounts, not a shared login — GST audit trail needs to know who finalized what.
- **Append-only `ledger_entries`** table gives the audit trail without a generic replay engine:
  written transactionally alongside invoice finalization / payment recording, never updated.
  Customer balance is `SUM(ledger_entries)` per customer (computed on read, or as a materialized
  view) — not a hand-rolled Kotlin replay loop.
- **Invoice numbering (resolved 2026-09-09)**: assigned locally at finalize time (last known
  number + 1), immediately followed by PDF generation. From the user's perspective, **PDF
  generation is the finish line** — cloud sync happens after, invisibly, in the background; the
  user never waits on it or confirms it. Deliberately no server-side arbitration, no collision
  detection, no renumber/reprint flow: usage is one user, primarily one device, with rare
  deliberate device switches (not simultaneous multi-device use), and sync is expected to
  complete almost always before the next finalize. A genuine numbering collision is an accepted,
  unhandled rare risk, not something engineered against. Concrete consequence: **no database
  UNIQUE constraint on invoice number** — enforcing one would require the exact reconciliation
  machinery this decision rejects. A duplicate number in the rare collision case is a silent
  data-quality footnote, not a sync failure.
- **Device-loss data durability (accepted risk, resolved 2026-09-09)**: if a device is lost or
  destroyed before an outbox write syncs, that record can be permanently gone from the cloud's
  perspective even though a PDF/paper copy may already exist in the world. Deliberately not
  mitigated with a secondary backup path (e.g. auto-email, Drive export) — the risk is a
  compound, low-probability event (connectivity returning is near-certain; a device being
  destroyed inside the narrow pre-sync window is separately rare), and building for it would be
  exactly the speculative complexity Principle 7 rules out.
- **Challan↔Invoice linkage atomicity**: a finalized Invoice originating from a Challan must
  never exist without its `DocumentLinkCreated` fact. Satisfied trivially — write both the
  Invoice finalization and the linkage fact in one local database transaction before either is
  queued to the outbox. No distributed-transaction machinery needed on top of the existing
  local-first design.

## UI conventions

- **Draft Spine**: `InvoiceDraftUiState` (data) ← `InvoiceDraftReducer` (pure functions) ←
  `InvoiceDraftStore` (mutation gate) ← ViewModel ← UI. Drafts are disposable UI-only state,
  never domain truth — process death loses an unsaved draft by design, unless/until autosave is
  explicitly designed in.
- Every Draft Reducer that derives financial or document-critical state must have JVM unit
  tests. A reducer without tests is incomplete.
- **EditBlock pattern** for inline draft editing: read-only projection → explicit user action →
  single expandable edit block → commit or cancel → re-rendered read-only projection. No
  always-visible inline editors. Exactly one edit block active at a time per section.
- **Focus/IME ownership**: explicit, owned by the screen/caller — never inferred, never owned by
  layout containers. Edit blocks auto-focus the first field on expand and dismiss IME on
  collapse; field-to-field order is linear via `ImeAction` (Next/Done), never guessed.
- **Chrome modes**: exactly one of Brand (entry surface only) / Workspace (primary task surfaces,
  back nav, title = document type) / Support (auxiliary surfaces) active at a time, derived from
  navigation context, never inferred heuristically.
- **Color semantics**: Primary = system backbone (focus, selection, structural state; calm,
  low-fatigue). Accent = action emphasis only (CTAs), used sparingly. Never conflate the two into
  one token.
- Autocomplete/search suggestions expand inline below the input field — never a popup/dropdown
  menu.

## UX direction (approved 2026-09-11, not yet built)

Full rationale, WCAG contrast audit, and screen-by-screen mockups (light + dark, built from
Verity's real tokens) live in the "Verity Design Blueprint" artifact — a living document, kept
up to date in place, so re-read it rather than trusting a stale summary:
`https://claude.ai/code/artifact/f97670a4-8698-4cc2-9f8e-96015239b995`. What follows here is the
condensed, durable version of the same decisions, for when the artifact isn't at hand.

**The problem it solves**: today's app is one screen wearing three names — launch drops straight
into the Workspace, and there is no way back to a document once you leave it, no customer list
(customers exist only via `CustomerSeedLoader`'s fixture), no settings. Right scope for Milestone
1; wrong scope once this needs to be a tool trusted with real customers.

- **Landing screen**: replace "launch straight into Workspace" with a **Home dashboard** — a
  "This Month" card (invoiced total + document count) and a Recent Documents list, both honest
  with today's data (`DocumentDao.getAll()`, no new persistence). Deliberately *not* an
  "Outstanding/Overdue" hero metric yet: no `Payment` entity exists, so that number would be
  dishonest until payment recording ships (Phase 4 below) — same slot, promoted later.
- **Navigation**: bottom navigation, four destinations — **Home, Documents, Customers,
  Settings** — via `NavigationSuiteScaffold`, plus a FAB for Create reachable from any tab. No
  drawer, no multi-org switcher, no reports tab — four is the number of things this app actually
  does today. Maps onto the Chrome modes above without adding a fifth: the four nav-root screens
  are Brand-mode (no back arrow, switched by the bottom nav, never pushed); anything drilled into
  from them (Document Detail, Customer Detail, Add/Edit Customer, Business Profile) is
  Support-mode, pushed, with a back arrow.
- **New screens required** (none exist today): Documents List; Document Detail — note this needs
  a genuinely new `document/{id}` route, since `InvoicePreviewScreen` today only ever renders the
  *live* `InvoiceWorkspaceViewModel`'s in-memory document, never a persisted one loaded by id,
  even though `DocumentDao.getById` already supports it; Customers List; Customer Detail; Add/Edit
  Customer (`CustomerDao.insert` with `OnConflictStrategy.REPLACE` already covers create *and*
  edit-by-id; still needs a real single-row `deactivate(customerId)` for the "soft-deactivated,
  not deleted" rule `CustomerDao`'s own doc comment already states but never implements); Business
  Profile (retires `HardcodedSeller.kt`); and a theme picker in Settings — `MainActivity`
  currently hardcodes `val isDarkTheme = false`, so `VerityDarkColors` (a complete, WCAG-checked
  palette) has never been reachable by a user.
- **Build order** — four phases, each gated by the one before it, not by a calendar:
  1. **Foundation** (start here, zero dependencies): the nav shell + Home, plus the three
     Critical fixes from the Blueprint's audit (line-item validation, Undo on delete, the two
     light-mode WCAG contrast failures on `text.muted`/`borders.subtle`).
  2. **Complete the record**: Documents List/Detail, Customer CRUD, remaining High-severity audit
     fixes (the `accent` color decision, light-mode's `borders.strong == borders.subtle` bug, real
     screen transitions, auto-focus/IME chaining, Document Type control affordance).
  3. **Make it yours**: Business Profile, theme wiring, remaining Medium fixes.
  4. **Close the loop**: Share/PDF export, payment recording — the feature that finally makes
     Home's hero metric a real Outstanding/Overdue card.

This is a proposal the user reviewed and approved, not a spec to implement unmodified — confirm
before building if anything here seems to have drifted from the live artifact, and it's fine to
push back on a specific screen or flow choice during implementation rather than building it as
drawn.

## Tech stack

Kotlin 2.x · Jetpack Compose · Material 3 (tokens only) · Navigation-Compose · ViewModel +
StateFlow · Room + KSP · Coroutines · WorkManager · Kotlinx Serialization · Gradle version
catalogs · Supabase (`supabase-kt`, pinned to an exact version) for cloud.

**Not using Hilt.** Manual dependency construction at the composition root is sufficient at this
scale — don't introduce a DI framework until the object graph is actually painful to wire by
hand.

**Forbidden**: XML layouts, Fragments, LiveData, AsyncTask, RxJava, EventBus, DataBinding,
Material 2.

## Testing standards

- Delete boilerplate the moment you notice it. An unmodified Android Studio template test
  (`ExampleUnitTest`, `ExampleInstrumentedTest`) is worse than no test — one in this project
  asserted a stale package name and would fail outright if it ever ran.
- A test file with zero test methods is worse than no file — it looks like coverage and isn't.
  Don't create a test file until it has real assertions in it.
- Good shape to imitate: pure-function tests for deterministic logic (one behavior per test, no
  Android dependency), real-instance tests for persistence (in-memory Room, not mocks, so you're
  verifying actual query/constraint behavior).
- A test name is a claim about what it verifies. If the test body exercises a different code path
  than the name describes, fix the test, not just the assertion.

### Compose UI interaction tests (Robolectric)

Added 2026-09-13 after a real bug (Undo on line-item delete silently did nothing, on-device and
manually) took ~20 minutes of emulator/adb/screenshot round-trips to diagnose, when the actual
fix took one Robolectric test run to find. Root cause was `InvoiceWorkspaceScreen`'s root `Box`
declaring `SnackbarHost` *before* its scrollable `Column` — the Column, drawn on top, silently
intercepted every tap in that screen region including the snackbar's own action button, even
though nothing was visibly there. No crash, no visual symptom — this class of bug reproduces
identically for a careful human tap, an adb tap, and a Compose test `performClick()`, but is
invisible to reading the code casually since it's purely about declaration order.

- **For any "tap/click does nothing, no crash" report, write a Robolectric Compose test
  reproducing the exact steps *first* — before touching an emulator.** `feature/build.gradle.kts`
  already has the dependencies (`robolectric`, `ui-test-junit4`, `ui-test-manifest` on
  `testImplementation`) and `InvoiceWorkspaceScreenUndoTest.kt` is the template to copy: build the
  ViewModel from plain constructor args (fakes for any `feature`-defined interface it depends on —
  no Room/platform needed), wrap `setContent` in `VerityTheme`, and drive it with
  `composeTestRule.onNodeWithText(...).performClick()`. Runs in seconds on the JVM; no emulator
  boot, no coordinate guessing.
  - Use `@Config(sdk = [34], qualifiers = "w360dp-h800dp")` — Robolectric's unconfigured default
    window is a legacy ~320x470dp screen that pushes real content below the fold, and
    `performScrollTo()` before `performClick()` on anything that might be off-screen regardless.
  - Use `println(...)` for tracing and `composeTestRule.onAllNodes(isRoot())[0].printToString()`
    to dump the semantics tree — both land in the test's captured stdout. `android.util.Log` does
    not reliably show up under Robolectric; don't reach for it here.
- **When a click "succeeds" (node found, no exception) but nothing happens, bisect the dispatch
  path in the same test**: swap `performClick()` for
  `performSemanticsAction(SemanticsActions.OnClick)` (invokes the click handler directly, no real
  touch/hit-testing). If that makes it pass, the bug is touch *delivery* — go look at `Box`
  declaration order for an overlapping sibling drawn on top. If it still fails, it's a real logic
  bug in the handler or state. This turns "which of several theories is it" into one deterministic
  check instead of repeated manual reproduction.
- **Any `Box` with multiple full-size/overlapping children must declare overlay content
  (snackbars, FABs, dialogs, tooltips) last**, matching `Scaffold`'s own convention of keeping
  the snackbar host as the topmost layer. Worth a deliberate check whenever a new screen adds an
  overlay — the Documents/Customers/Settings screens in the UX roadmap will each need one.
- Screenshots are for genuinely visual questions ("does this look right"); for behavioral
  questions ("did the click fire"), a Robolectric test with `println`/`printToString()` tracing
  is faster, cheaper, and more conclusive. Reserve the emulator for a final human eyeball pass —
  and prefer letting the user do that pass themselves when it's cheap for them, rather than
  driving it manually via adb.
- **A green Robolectric suite is not proof a screen matches its approved mockup.** R-13's first
  pass (2026-09-13) had every test passing while the real render was visibly wrong against the
  Design Blueprint: a hero card silently rendering at wrap-content width instead of full width
  (`VeritySurface` doesn't stretch on its own — `VerityListItem`'s Recent Documents card only
  looked right by accident, because its inner `Row` happens to declare `fillMaxWidth()`), and a
  missing leading avatar that no `onNodeWithText` assertion would ever catch, because the text
  that *was* there was correct. `onNodeWithText`/`assertIsDisplayed` verify that specific text
  exists and some node bears it — they say nothing about size, position, or "is this element even
  present at all" for content with no distinguishing text. For any screen built against an
  approved mockup, install the actual APK and look at (or screenshot) the real render at least
  once before calling it done, even with a fully green suite — this is a different failure mode
  than the "tap does nothing" class above, and needs a different check.

## Working with this repo (for any AI assistant, including future sessions)

- Ask before making a significant architecture or framework decision — don't resolve an open
  question (see "open problem" above) unilaterally.
- No reflexive agreement. If a design looks fragile, say so, name the failure mode, and give a
  genuine recommendation — agreement without having weighed the counter-case is incomplete
  reasoning, not politeness.
- Don't write temporary/placeholder code silently. Say so out loud, get explicit sign-off, and
  don't let it linger uncommented-on across sessions.
- Prefer minimal, reversible changes over broad refactors unless a refactor is explicitly the
  ask.
- This file changes by discussion, in place, as decisions are made — there's no separate
  amendment process to follow.
