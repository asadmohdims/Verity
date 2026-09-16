# Verity — Engineering Reference

Verity is a GST invoicing app (Invoices & Challans) built for real use, starting with a single
business, with multi-org support planned once it's actually needed. It's also the author's
vehicle for rebuilding hands-on fluency in modern Android/Kotlin/Compose — architecture and
correctness matter here, but so does keeping things comprehensible to someone relearning the
ecosystem after ~10 years away from hands-on coding.

This file is a **living reference**, not a constitution. Update it in place as decisions change
— no amendment ceremony required. If something here turns out to be wrong or stale, fix it here.

## Status (read this before trusting anything else below)

Current as of 2026-09-15. This is a snapshot of what's actually true, not a changelog — for the
history of how it got here (bugs found, decisions made mid-build, exact reasoning behind a fix),
read `git log` and individual commit messages rather than this file. Nothing here counts as "done"
until it's demonstrably reachable from the running app, not just unit-tested — see Principle 2
below.

**Invoicing core — built, wired, and verified on-device (both Invoice and Challan)**: draft
creation → line items → transport details → GST tax computation (skipped for Challan) → preview →
finalize → PDF, backed by a real local Room database (`DocumentEntity`/`LedgerEntryEntity`; the
original event-sourcing design was deleted, not deprecated). Finalize assigns a real sequential
number per document type (`INV-000001`/`CH-000001`, ...; see "Data & sync architecture"), persists
the document as a JSON snapshot of `InvoiceDocumentModel`, and — Invoice only, since a Challan
isn't a billing event — a matching ledger entry, all in one transaction. Every finalized document
gets a real PDF (see "Documents: search, PDF, and schema evolution" below), viewable in-app.
Invoice-workspace customer autocomplete still reads `CustomerSeedLoader`'s original 25-customer
fixture set at seed time, but the customers themselves are now real CRUD-managed rows (see below),
not a closed fixture — a customer added or edited through the new Customers screens is real data
the autocomplete picks up too, it's just that the *initial* seed population is still fixture-based.

**Challan → Invoice (job work) — built 2026-09-16, JVM/androidTest-green, on-device pass still
pending.** The Document Type dropdown in Invoice Workspace has a third option, "Challan + Invoice",
for the job-work scenario this section of the domain model previously only described (see "Domain
model" below for the finalized design). Selecting it reveals a "Job Work" section on the Challan
draft with an always-available "Received Vide Challan No. / Dated" reference (any Challan, not
gated by job work) and a static "Ref: Invoice No. — (assigned automatically at finalize)" row.
Finalizing that Challan atomically reserves-and-consumes the real next Invoice number
(`InvoiceFinalizer.JobWorkLinkage.ReserveLinkedInvoiceNumber`) — a deliberate choice over a
non-binding preview, so the number printed on the already-issued Challan PDF is always correct;
the accepted trade-off is a permanent numbering gap if the follow-up Invoice draft is abandoned.
The Challan's "Finalized" screen gets a "Continue to Invoice" button that opens a fresh Invoice
draft pre-filled with the same customer and a locked Document Type, but deliberately empty line
items — only the job-work service gets billed, never the Challan's goods value. That Invoice's
finalize reuses the reserved number (`JobWorkLinkage.UseReservedNumber`) instead of allocating a
new one, and writes `DocumentEntity.linkedDocumentId` — a column that has existed since schema v1
but was never written until now — pointing at the Challan's real row; the Challan's own row is
never mutated (finalized documents are insert-only), so its "Linked Invoice" is a reverse DAO
query, not a stored forward pointer. The Invoice's PDF gets a "JOB WORK INVOICE" banner (Challan's
does not, by design); both PDFs gain the new reference rows. `DefaultInvoiceFinalizerTest` has real
in-memory-Room coverage for the numbering/linking logic; **not yet run**: `connectedDebugAndroidTest`
itself, and an on-device pixel/behavior look at the full flow and the PDF banner/grid — this
project's own testing standards (see below) don't consider a Robolectric-green UI or a
compiled-but-unrun androidTest suite sufficient proof for either.

**Navigation & screens**: bottom nav (Home / Documents / Customers / Settings) plus a FAB for
Create is built and merged to `main` — see "UX direction" below for what's built vs. still planned.
Home shows a "This Month" invoiced-total card (Invoice only) and a Recent Documents list. Documents
has a working List, Detail, and broad-box Search with a minimal read-only customer rollup. Customers
(List/Detail/Add/Edit, including soft-deactivate) and Settings (theme picker, sync status, version,
an inert Business Profile row) are now built too — see "Next up" below for what's genuinely
verified vs. only JVM/Robolectric-tested so far.

**Cloud/sync — Phase 1 built, and the core push path is now verified for real (2026-09-15).**
Firebase (Firestore + Auth + Storage), not Supabase — see "Data & sync architecture" below for
the full reasoning (vendor lock-in analysis, why Firebase's built-in offline queue changes the
architecture, the atomic-numbering hardening for a shared login). `FirebaseSyncClient`/
`FirebaseRestoreClient`/`InvoiceNumberAllocator`/`FirebaseAuthGate` exist in `platform/.../sync/`,
wired into `DefaultInvoiceFinalizer`/`DefaultInvoicePdfRenderer`/`MainActivity`; `INTERNET`
permission added; `platform/firebase/{firestore,storage}.rules` are the checked-in source of
truth for access control. A real Firebase project now exists (console set up, Auth account
created, `org_id` custom claim set via `set_org_claim.js`, rules deployed), and a genuine
online-finalize round trip was run on a real emulator against it: finalizing `INV-000001` produced
a Firestore document and ledger entry with `syncedToCloud = 1` (confirmed by pulling the on-device
Room database directly, not just trusting the UI), the PDF generated, uploaded to Storage, and was
independently confirmed present in the Storage console and re-viewable from the app. Firestore's
own offline-persistence cache was observed active on-device, confirming the built-in-offline-queue
architecture claim, not just assuming it. This is real Principle-2 "wired and verified" for the
happy path — **still open, not yet run even once**: an offline-finalize-then-reconnect test, the
new-device restore flow (`FirebaseRestoreClient`), confirming `firestore.rules`/`storage.rules`
genuinely *reject* a request with a missing/wrong `org_id` claim (only the allow path has been
exercised), and the two-device concurrent-numbering scenario `InvoiceNumberAllocator` was built
for. All JVM/Robolectric-testable pieces are green (`InvoiceNumberAllocatorTest`,
`FirebaseRestoreClientTest`, `Migration2To3Test`, the extended `DefaultInvoiceFinalizerTest`).

**Known placeholders / gaps to close before this is production-ready**:
- `HardcodedSeller.kt`'s `pincode` field is still `PLACEHOLDER_PINCODE` — every other seller field
  is real (Unitech Machineries).
- Challan→Invoice job-work linkage's same-session flow is now built (2026-09-16 — see Status and
  "Domain model" below); its on-device pass is still outstanding (see Status), and the broader
  "mark a standalone Challan Invoice-Linked, link it from a later session/different device" version
  of this design remains deliberately deferred, not built.
- A real Business Profile screen (to retire `HardcodedSeller.kt`) isn't built — the Settings
  screen's Business Profile row ships visible-but-inert until it exists, deliberately, since no
  design exists for that screen yet (see "Next up" below). Customer CRUD and the Settings theme
  picker, by contrast, are now built — see "Next up".
- Share/Print/Export (and the `FileProvider` it needs) isn't built — now top priority, see
  "Next up" above.
- No real app icon — still the stock Android Studio default. See "Next up" above.
- `LineItemEntryScreen` (2026-09-15, replacing inline Line Item add/edit — see "UI conventions"
  above) is green on the full Robolectric suite but has **not** had the on-device pixel/behavior
  look this project's own testing standard requires before calling mockup-matching work done
  (Robolectric green ≠ visual proof — see "Testing standards" below). Do an `installDebug` pass
  on a real device/emulator, on a long invoice specifically, before treating this as fully closed.
- Several `platform` androidTest suites (real Room — `DefaultInvoiceFinalizerTest`,
  `Migration1To2Test`, `DefaultDocumentSearchDataSourceTest`) compile and pass in JVM/Robolectric
  form but need a connected device/emulator for `connectedDebugAndroidTest` itself, which hasn't
  been run as of this write-up.
- No on-device pixel-diff pass of the rendered PDF against its approved mockup — green tests don't
  prove a rendered page matches a design; see "Testing standards" below.
- Cloud sync's real Firebase project setup is **done** (2026-09-15) — project created, Android app
  added, `app/google-services.json` in place and now gitignored — decided 2026-09-15 not to commit
  it, despite Firebase's own guidance that the file is safe to commit (no secrets in it) — the one
  fixed business Auth account created, `org_id` claim set via `set_org_claim.js`, credentials in
  `local.properties`, rules deployed. The online-finalize path is verified for real — see the
  Status paragraph above. **Still to run**: finalize offline then reconnect and confirm the
  pending write syncs automatically; wipe local data (or a second device/emulator) and confirm
  `FirebaseRestoreClient` pulls everything back down, including PDFs downloading from Storage
  rather than regenerating; confirm `firestore.rules`/`storage.rules` genuinely *reject* a request
  with a missing/wrong `org_id` (only the allow path has been exercised so far); the two-device
  concurrent-finalize numbering scenario `InvoiceNumberAllocator` exists to handle.
- The `ensurePdf()` local → Storage → regenerate fallback order (`DefaultInvoicePdfRenderer`) has
  no automated test — the first two tiers return before touching `android.graphics.pdf`, so a
  plain JVM test could cover them, but doing so needs a fake `Context` this module doesn't
  currently have infrastructure for, and the third tier (regenerate) needs Robolectric or a real
  device either way. Skipped rather than forced into an awkward shape — worth returning to if/when
  `platform` grows a Robolectric setup for something else.

## Next up (prioritized 2026-09-15)

Decided in this session's review, reflecting actual priority now rather than the phase order
implied by "UX direction" below:

1. **PDF open/share/print — crucial.** Nothing lets a finalized invoice leave the app today (see
   the Share/Print/Export gap below). Needs, at minimum: open the generated PDF in the device's
   own PDF viewer, share it via any installed app (WhatsApp explicitly named as the real-world
   delivery channel), and print directly via Android's native print framework
   (`android.print.PrintManager`), not a share-to-a-printing-app workaround. Needs a
   `FileProvider` — the PDF lives in app-private external files storage today (see "Documents:
   search, PDF, and schema evolution"), not shareable as a raw `file://` URI as-is.
2. ~~Customers screen and Settings screen~~ — **built and merged to `main` (2026-09-15)**.
   Customers List/Detail/Add-Edit (including the soft-deactivate `CustomerDao` always documented
   but never implemented), and Settings (System/Light/Dark theme picker via a new
   `ThemePreferenceStore`, a sync-status row reusing Home's existing pending/synced logic, a
   version row, and a Business Profile row that ships visible-but-inert — no design exists yet for
   that screen, deliberately deferred rather than built undesigned). Real Business Profile screen
   (to retire `HardcodedSeller.kt`) remains not built. 34 new tests (ViewModels + Robolectric
   screens + a real-Room `CustomerDao.deactivate` test), full 137-test suite green,
   `compileDebugAndroidTestKotlin` clean. Installed on a real device (`installDebug`) and reviewed
   by the user — some issues found during that pass, not yet itemized here; expect a near-term
   follow-up commit fixing them once scoped.
3. **App icon** — real Verity branding before any store listing or install; currently the stock
   Android Studio default. Backlog, not urgent, but must land before going live.

**Explicitly deferred to Day 2** (confirmed 2026-09-15, not before this ships to the single
current business): real per-device/per-user authentication so a *new* business could download the
app and use it standalone under their own `org_id`, with Verity managing that provisioning. Today
there's one shared Firebase Auth account signed in silently on every install — fine for one
business on known devices, not fine for self-serve multi-tenant distribution. Same deferral
already recorded under "Commercialization scope check" below (per-employee accounts/roles),
reconfirmed here to also cover the *provisioning* side — how a brand-new org's device would get
an `org_id` and credentials in the first place — which that section didn't originally scope.

**Design references** (both living documents — re-read them rather than trusting a stale summary
here): the GST invoice PDF design ("Familiar Grid, Modernized" — navy+brass, bordered-grid layout)
at `https://claude.ai/code/artifact/7c9bd3c7-f1f9-4104-89de-645acf683abc`; the navigation/IA design
("Verity Design Blueprint") at
`https://claude.ai/code/artifact/f97670a4-8698-4cc2-9f8e-96015239b995`.

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
- **`platform`** — the only module allowed to touch Room, KSP, or Firebase/network code (`app`
  constructs Firebase SDK instances at the composition root, same manual-DI pattern as everything
  else, but all sync logic itself lives in `platform/.../sync/`). Infrastructure only, no UI
  dependencies.
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
- **Challan → Invoice (job work)** *(built 2026-09-16, same-session flow only — see Status)*: the
  Document Type dropdown's "Challan + Invoice" option declares job-work intent upfront, at Challan
  creation — there's no "mark a standalone Challan Invoice-Linked, to be linked later from a
  different session" flow; that broader version of this design (`DocumentLinkCreated` as a fully
  general additive fact, linkable from any device once synced) remains future/deferred, per
  Principle 1. Every finalized Challan still gets its own PDF immediately at finalization,
  identical to a plain Invoice's finalize → PDF → background-sync pattern, and the original
  Challan PDF is never regenerated once issued. What "job work" adds: (1) any Challan can carry a
  "Received vide Challan No. X dated Y" reference to the customer's own inbound delivery challan
  (an external document, never itself a Verity row); (2) a job-work Challan's finalize atomically
  reserves-and-consumes the real next Invoice number (`InvoiceFinalizer.JobWorkLinkage.
  ReserveLinkedInvoiceNumber`) and bakes it into the Challan's own printed "Ref: Invoice No."
  reference — chosen over a non-binding preview so that number is always correct once printed,
  accepting a permanent numbering gap if the user never finalizes the follow-up Invoice; (3) the
  Challan's "Finalized" screen offers "Continue to Invoice", opening a pre-filled Invoice draft
  (same customer, locked Document Type, empty line items — the Challan's goods value/dates/
  payments are deliberately **not** copied, only the job-work service gets billed) carrying that
  reserved number forward (`JobWorkLinkage.UseReservedNumber`); (4) that Invoice's finalize reuses
  the reserved number rather than allocating a new one, and writes `DocumentEntity.linkedDocumentId`
  (a column dormant since schema v1) pointing at the Challan's real row — written once, at the
  Invoice's own insert time, never as a mutation of the already-finalized Challan; the Challan's
  "Linked Invoice" is therefore a reverse `DocumentDao` query, not a stored forward pointer, and
  resolves to nothing if that Invoice was never finalized. Only the Invoice's PDF gets a visible
  "JOB WORK INVOICE" banner; both PDFs gain the new reference rows. A Challan still links to at
  most one Invoice, and none of the Challan's goods value/payments/ledger effects ever reach it.
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

## Data & sync architecture (Phase 1 built, not yet on-device-verified — see Status)

Cloud is a **backup and restore** layer, not a live multi-device sync system: the app stays
local-first and single-primary-device by design (confirmed explicitly, 2026-09-15 — see
"Commercialization scope check" below for why real multi-user support is deliberately deferred).
Every local write mirrors to the cloud in the background so a device loss doesn't lose the
business's records, and a replacement/new device can pull everything back down and be fully
operational again.

- **Room is the local source of truth**, unchanged. Every write lands in Room first and returns
  immediately — the UI never waits on network. `DocumentEntity`/`LedgerEntryEntity` now also
  carry `syncedToCloud: Boolean` (schema v3, `Migration2To3`) for the Home "N pending" indicator.
- **Firebase, not Supabase (decided 2026-09-15, superseding this section's earlier Supabase/
  outbox design)**. The two reference implementations already in this codebase's sibling
  projects were compared on the *providers'* merits, not just how well each reference app used
  them: Supabase's open-source self-hosting escape hatch (the whole stack — Postgres, GoTrue,
  PostgREST, Realtime, Storage — can be run identically outside Supabase Inc.) and its fit for
  this relational ledger domain were the stronger architectural case. Firebase was chosen anyway,
  for two concrete operational reasons: Supabase's free tier auto-pauses a project after 7 days
  of zero API requests (a manual dashboard "Restore" click, no data loss, but a chore the app
  owner didn't want to own), and the risk that Supabase could shorten that window unilaterally in
  the future. Firebase's Spark (free) tier has no inactivity pause, only hard daily caps (20K
  writes/day, 50K reads/day, 1GB storage) far above this app's realistic single-business volume.
  A real trade-off, made with eyes open, not a reversal of the earlier analysis.
- **No hand-rolled outbox table or WorkManager sync worker for the push side.** Firestore's
  Android SDK has offline persistence and a durable local write queue *built in* — writes are
  cached, optimistically applied, and automatically retried on reconnect, with no app code needed.
  That's the one thing that genuinely changes the architecture, not just the SDK import: the
  outbox pattern (used in Attendance, and in this file's pre-2026-09-15 Supabase draft) exists
  specifically to compensate for a backend with no client-side offline queue of its own —
  Firestore already has one, so building a second, parallel one would be redundant (Principle 1).
  `FirebaseSyncClient` (`platform/.../sync/`) is a thin, **fire-and-forget, never-awaited**
  wrapper: `pushDocument`/`pushLedgerEntry`/`pushPdf` call straight into the SDK and return; a
  success listener flips `syncedToCloud` and records a `SyncStatusStore` timestamp, a failure
  listener only logs (the SDK's own queue handles retry — re-implementing retry on top of it
  would be two queues fighting for the same job, a Principle 3 violation).
- **Restore-to-new-device** (`FirebaseRestoreClient`): a one-time bulk pull, triggered from
  `MainActivity` only when a device has no local documents yet and a
  `SyncStatusStore.isInitialRestoreCompleted()` flag is false. Fetches the whole
  `orgs/{orgId}/documents` and `orgs/{orgId}/ledgerEntries` collections in one call each (fine at
  a single business's realistic multi-year volume) and inserts with `OnConflictStrategy.REPLACE`,
  making a re-run after an interrupted attempt harmless — no separate resume/pagination logic.
  Deliberately **not** a live subscription (no `addSnapshotListener`): since restore only ever
  targets an empty local database, there's no merge/conflict logic needed, matching the
  single-primary-device design above.
- **PDF backup** (revisiting the 2026-09-09 "no secondary backup path" decision below, now that
  real cloud infrastructure exists for other reasons): the exact generated PDF bytes are uploaded
  to Firebase Storage after generation (`orgs/{orgId}/pdfs/{documentNumber}.pdf`), not just the
  JSON they're rendered from — chosen over relying on `ensurePdf()`'s existing regeneration
  ability, for GST audit fidelity: a future renderer change could otherwise make an old document
  look subtly different from what was actually issued. `ensurePdf()`'s fallback order is now
  local file → Storage download → regenerate (last resort, only when even the upload hasn't
  landed yet).
- **Auth**: one fixed Firebase Auth account (email/password), signed in silently on launch by
  `FirebaseAuthGate` — no login screen. Firestore/Storage Security Rules
  (`platform/firebase/{firestore,storage}.rules`, checked in, not just a console click — the
  discipline gap HelloCredit, an earlier reference project, was missing) enforce per-`org_id`
  scoping via a custom claim on that account's token, set once via
  `platform/firebase/set_org_claim.js` (Admin SDK — no console UI for custom claims, unlike
  Supabase's dashboard-editable `app_metadata`). See "Commercialization scope check" below for
  why a single shared login — not per-employee accounts — is the deliberate choice, including for
  the app's intended commercial future.
- **Append-only `ledger_entries`** table gives the audit trail without a generic replay engine,
  unchanged. Customer balance is `SUM(ledger_entries)` per customer, computed against local Room
  — not the cloud copy, and not a hand-rolled Kotlin replay loop. Firestore's own aggregation
  queries have real limits (no `GROUP BY`) that only matter if a future server-side reporting
  surface is ever built outside the app; not a concern for Phase 1, since the app's own UI never
  queries the cloud copy directly.
- **Invoice numbering — hardened 2026-09-15 for a shared login used from more than one device**
  (see "Commercialization scope check" below). The original 2026-09-09 decision (local
  `last known number + 1`, accepted collision risk) is superseded by `InvoiceNumberAllocator`:
  atomic online (a Firestore transaction against `orgs/{orgId}/counters/{documentType}`, short
  timeout), local-fallback offline. This shrinks the collision window from "every finalize" down
  to "two devices both offline and finalizing at the exact same moment" — full elimination would
  require every finalize to round-trip the network, which would break the never-blocked-on-network
  guarantee (Principle 6) this app is otherwise built around, so it's not attempted. **Still no
  database UNIQUE constraint on invoice number** — the residual risk (an online transaction
  succeeding server-side just as this call times out, followed by a local-fallback allocation) is
  narrower than before but not literally zero, and is an accepted, named trade-off, not
  reconciliation machinery. **PDF generation is still the finish line** from the user's
  perspective — numbering resolves (online or local-fallback) before it, cloud sync happens after,
  invisibly, in the background.
- **Device-loss data durability**: meaningfully improved by Phase 1, not just superseded. With
  the local outbox model this section used to describe, a lost device before sync meant the
  record could be gone from the cloud's perspective entirely. With Firestore's own durable local
  queue plus the PDF backup above, the actual risk window shrinks to "created and lost before the
  SDK ever got a network window to sync" — still possible, still not specially engineered around
  beyond what Firebase's SDK already does, but a smaller gap than the 2026-09-09 framing assumed.
- **Challan↔Invoice linkage atomicity**: unchanged in spirit — a finalized Invoice originating
  from a Challan must never exist without its `DocumentLinkCreated` fact, satisfied by writing
  both facts in one local database transaction before either reaches the cloud. No
  distributed-transaction machinery needed on top of the existing local-first design.

### Commercialization scope check (2026-09-15)

Verity's intended commercial audience is small industries, potentially with more than one person
managing invoices at a shared business. Real multi-user support — per-employee accounts, roles/
permissions, live real-time sync (`addSnapshotListener`), invite/remove-teammate flows — was
deliberately **not** built now: none of it has a confirmed real customer to design against yet
(Principle 1 — "if you're building infrastructure and can't name the third concrete case it
serves today, stop"). The resolution: **a single shared login per org is the deliberate choice**,
including for the commercial future, not a placeholder for real accounts — small operations
sharing one business account is a legitimate, common pattern, not a compromise waiting to be
fixed. The one piece that *does* need hardening under a shared login is invoice numbering (two
physical devices, same account, both finalizing) — see above. Real per-user accounts, live sync,
and roles remain explicitly deferred to a future phase, designed against an actual paying
multi-person customer rather than guessed at now.

## Documents: search, PDF, and schema evolution

- **PDF generation**: `DefaultInvoicePdfRenderer` (`platform/.../pdf/`) draws
  `InvoiceDocumentModel` straight to a PDF via `android.graphics.pdf.PdfDocument`/`Canvas` — no
  new dependency, fully on-device/offline. Stored at
  `getExternalFilesDir("documents")/{documentNumber}.pdf`, generated eagerly right after finalize
  and re-verified defensively (`ensurePdf()`, idempotent) whenever the PDF viewer opens, so a
  failed eager attempt self-heals with no dedicated retry UI. Viewing is a hand-rolled Compose
  screen (`PdfViewerScreen`) on the stable `android.graphics.pdf.PdfRenderer` API, not the
  pre-1.0 `androidx.pdf` Compose library. Uses its own bespoke navy+brass palette
  (`InvoicePalette`, private to the renderer), deliberately distinct from the app's own
  `VerityColors` — a printed GST document should look like a formal business document, not carry
  the app's mobile UI chrome. The line-item table paginates across pages rather than assuming
  everything fits on one.
- **Search**: a denormalized `DocumentEntity.searchIndexText` column (flattened lowercase text of
  every searchable field — customer/GSTIN, line items/HSN, transport fields, amount — computed
  once at finalize since documents are insert-only) backs a plain SQL `LIKE` query, reached via a
  single broad search box with no query-syntax operators or filter chips. Chosen over Room FTS4:
  the real latency risk is decoding every document's JSON payload per keystroke, not `LIKE` scan
  cost at this app's realistic volume, so a plain indexed column gets FTS4's "don't decode JSON
  up front" win without its tokenizer/virtual-table machinery. Ranking/snippet extraction
  (`DocumentSearchRanking`) is pure JVM-testable Kotlin; only the SQL narrowing and JSON decode
  live in `platform` (`DefaultDocumentSearchDataSource`).
- **Schema migrations are real from here on.** `PlatformDatabase` moved from `exportSchema =
  false` to `true` at version 2 (`Migration1To2`, adding `searchIndexText` and backfilling it for
  pre-existing rows) once real finalized documents existed on-device — a bare version bump with no
  migration would hard-crash the app rather than silently lose data. Verify any future migration
  with `MigrationTestHelper` against the exported `schemas/*.json`, not hand-written SQL guesses.

## UI conventions

- **Draft Spine**: `InvoiceDraftUiState` (data) ← `InvoiceDraftReducer` (pure functions) ←
  `InvoiceDraftStore` (mutation gate) ← ViewModel ← UI. Drafts are disposable UI-only state,
  never domain truth — process death loses an unsaved draft by design, unless/until autosave is
  explicitly designed in.
- Every Draft Reducer that derives financial or document-critical state must have JVM unit
  tests. A reducer without tests is incomplete.
- **EditBlock pattern** for inline draft editing: read-only projection → explicit user action →
  single expandable edit block → commit or cancel → re-rendered read-only projection. No
  always-visible inline editors. Exactly one edit block active at a time per section. **Exception,
  added 2026-09-15**: Line Items no longer uses an inline `VerityEditBlock` for add/edit — a long
  invoice's edit block could expand dozens of items down a single shared scroll region with no
  `imePadding()` anywhere in the app, so the keyboard would cover the field being typed into with
  no way to bring it back into view short of a manual scroll. Add/Edit now lives on
  `LineItemEntryScreen` (`feature/invoice/ui/`), a dedicated pushed (Support-mode) full-screen
  surface sharing the same `InvoiceWorkspaceViewModel`/`InvoiceDraftStore` instance as Workspace —
  see that file's header comment for the full reasoning, including why a full screen was chosen
  over a bottom sheet (five fields plus two autocomplete panels don't fit above a keyboard in a
  half-height sheet). Add mode offers "Save & Add Another" (commits and reopens a blank form
  without leaving the screen) as the primary action, specifically for entering many line items in
  one sitting. `MainActivity` now declares `android:windowSoftInputMode="adjustResize"` — required
  for `imePadding()` to behave correctly and consistently across API levels, not just legacy
  boilerplate.
- **Focus/IME ownership**: explicit, owned by the screen/caller — never inferred, never owned by
  layout containers. Edit blocks auto-focus the first field on expand and dismiss IME on
  collapse; field-to-field order is linear via `ImeAction` (Next/Done), never guessed. **Actually
  wired up for the first time 2026-09-15** (`LineItemEntryScreen`, via `VerityTextField`'s new
  `fieldModifier`/`keyboardOptions`/`keyboardActions` params; Transportation Mode's still-inline
  `VerityEditBlock` got the same chain the same day) — this bullet described the intended
  convention for a while before any screen implemented it.
  - **The scroll-into-view half of this needed two rounds to get right.** First attempt trusted
    Compose's own "auto-scroll a newly-focused TextField into view" behavior (implicit in
    `Modifier.verticalScroll`) to carry the user from field to field once the `ImeAction.Next`
    chain moved focus. On-device testing showed that doesn't reliably fire once focus moves
    between fields while the keyboard is *already* open — exactly the case that matters here. Both
    `LineItemEntryScreen` and Transportation's fields now wire an explicit
    `BringIntoViewRequester` per field instead (`rememberFocusScrollModifier` in
    `InvoiceWorkspaceScreen.kt`, shared by both files) — call it in `onFocusChanged` rather than
    trust the field to do it itself. Lesson: don't rely on this specific implicit Compose behavior
    going forward: make it explicit every time, consistent with this bullet's own rule.
  - **Inline edit blocks have a second, separate scroll problem full-screen surfaces don't**:
    a block that expands partway down a long shared scroll region (Transportation, still inline)
    can open with only its first field guaranteed visible, no matter how good the field-to-field
    chain is — `imePadding()` shrinks the viewport but doesn't decide *where* in the scroll content
    that viewport lands. Fix: track the block's own on-screen Y offset
    (`Modifier.onGloballyPositioned`) and, in a `LaunchedEffect` keyed on its expanded/collapsed
    state, `ScrollState.animateScrollTo()` that offset the moment it expands, so the section
    heading lands at the top of the screen before the user even taps a field. `LineItemEntryScreen`
    doesn't need this since it's already its own full screen with nothing above it to scroll past.
  - **Billed To / Shipped To still use the plain old inline `VerityEditBlock` with neither fix** —
    apply the same recipe there if the same complaint comes up, rather than re-deriving it. This is
    now the second and third call site hand-rolling both fixes; if a fourth shows up, it's worth
    reconsidering whether `VerityEditBlock` itself should grow this generically (it doesn't own the
    parent's `ScrollState` today, so the scroll-to-top-on-expand half would need that threaded in
    or reworked around `BringIntoViewRequester`) rather than copying the recipe a third time.
- **Chrome modes**: exactly one of Brand (entry surface only) / Workspace (primary task surfaces,
  back nav, title = document type) / Support (auxiliary surfaces) active at a time, derived from
  navigation context, never inferred heuristically.
- **Color semantics**: Primary = system backbone (focus, selection, structural state; calm,
  low-fatigue). Accent = action emphasis only (CTAs), used sparingly. Never conflate the two into
  one token.
- Autocomplete/search suggestions expand inline below the input field — never a popup/dropdown
  menu.

## UX direction (approved 2026-09-11; Phases 1 and part of 2 built — see Status)

Full rationale, WCAG contrast audit, and screen-by-screen mockups (light + dark, built from
Verity's real tokens) live in the "Verity Design Blueprint" artifact — a living document, kept
up to date in place, so re-read it rather than trusting a stale summary:
`https://claude.ai/code/artifact/f97670a4-8698-4cc2-9f8e-96015239b995`. What follows here is the
condensed, durable version of the same decisions, for when the artifact isn't at hand.

**The problem it solved**: pre-Phase-1, the app was one screen wearing three names — launch
dropped straight into the Workspace, with no way back to a document once you left it, no customer
list (customers existed only via `CustomerSeedLoader`'s fixture), no settings. Right scope for
Milestone 1; wrong scope once this needed to be a tool trusted with real customers. Phase 1 (nav
shell + Home) and the Documents-List/Detail/Search half of Phase 2 are now built — see Status.

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
- **New screens required**: Documents List and Document Detail are **built** (a real
  `document/{id}` route loading a persisted document by id via `DocumentDao.getById`, not just
  `InvoicePreviewScreen`'s live in-memory draft). Customers List, Customer Detail, and Add/Edit
  Customer (including `deactivate(customerId)` for the "soft-deactivated, not deleted" rule
  `CustomerDao`'s doc comment always stated) are now **built** too (2026-09-15, see "Next up"), as
  is a theme picker in Settings (`ThemePreferenceStore`, finally making `VerityDarkColors` reachable
  by a user). Still needed: a real Business Profile screen (retires `HardcodedSeller.kt`) — the
  Settings row for it ships visible-but-inert until that screen has its own design pass.
- **Build order** — four phases, each gated by the one before it, not by a calendar:
  1. **Foundation — built.** Nav shell + Home, plus the three Critical fixes from the Blueprint's
     audit (line-item validation, Undo on delete, the two light-mode WCAG contrast failures on
     `text.muted`/`borders.subtle`).
  2. **Complete the record — built.** Documents List/Detail/Search and Customer CRUD are both done.
     Remaining High-severity audit fixes (the `accent` color decision — still an unresolved
     placeholder, see `VerityLightColors`/`VerityDarkColors` — real screen transitions,
     auto-focus/IME chaining, Document Type control affordance) are not; light-mode's
     `borders.strong == borders.subtle` bug was already fixed before this file's 2026-09-15 review.
  3. **Make it yours — partially built.** Theme wiring is done (Settings' theme picker); Business
     Profile and remaining Medium fixes are not.
  4. **Close the loop — not started.** Share/PDF export, payment recording — the feature that
     finally makes Home's hero metric a real Outstanding/Overdue card.

This is a proposal the user reviewed and approved, not a spec to implement unmodified — confirm
before building if anything here seems to have drifted from the live artifact, and it's fine to
push back on a specific screen or flow choice during implementation rather than building it as
drawn.

## Tech stack

Kotlin 2.x · Jetpack Compose · Material 3 (tokens only) · Navigation-Compose · ViewModel +
StateFlow · Room + KSP · Coroutines · Kotlinx Serialization · Gradle version catalogs · Firebase
(Firestore + Auth + Storage, via the Firebase BOM) for cloud — see "Data & sync architecture" for
why Firebase over Supabase, and why WorkManager/an outbox table are *not* part of this stack
(Firestore's own offline queue makes them redundant).

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
