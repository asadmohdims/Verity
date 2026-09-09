# Verity — Engineering Reference

Verity is a GST invoicing app (Invoices & Challans) built for real use, starting with a single
business, with multi-org support planned once it's actually needed. It's also the author's
vehicle for rebuilding hands-on fluency in modern Android/Kotlin/Compose — architecture and
correctness matter here, but so does keeping things comprehensible to someone relearning the
ecosystem after ~10 years away from hands-on coding.

This file is a **living reference**, not a constitution. Update it in place as decisions change
— no amendment ceremony required. If something here turns out to be wrong or stale, fix it here.

## Status (read this before trusting anything else below)

As of 2026-09, Verity has:
- A working Invoice Workspace UI: draft creation, line items, transport details, GST tax
  computation, and preview — all in-memory only, nothing persists across restarts.
- A Room-based persistence layer (event store, customer table, ledger/document-index
  projections) that is **fully built and unit/instrumentation-tested but not wired into the
  running app** — zero call sites from `MainActivity`. Treat any class under `platform/` as
  unverified until you've confirmed something actually calls it at runtime.
- Zero cloud/network code — no Supabase, no Retrofit/Ktor, no `INTERNET` permission yet.

Nothing is "done" here until it's demonstrably reachable from the running app. A well-documented,
well-tested class sitting in isolation is not a finished feature — see Principle 2 below. Keep
this section honest and current; that's the entire reason it exists.

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

## Data & sync architecture (current direction — not yet built; see Status)

Replacing the original generic event-sourcing/replay design with something simpler that still
meets the real requirement (immutable finalized documents, auditable corrections):

- **Room is the local source of truth.** Every write lands in Room first and returns immediately
  — the UI never waits on network.
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
