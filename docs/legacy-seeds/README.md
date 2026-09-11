# Legacy seed documents

`verity_seed_core.txt` and `verity_seed_operational.txt` are an early AI-authored design log for
this project, written before the current architecture existed. They describe an event-sourcing
design (events, replay engines, a generic ledger/document-index) that was deliberately deleted and
replaced — see the Status section at the top of `/CLAUDE.md` and the "old event-sourcing design"
note in its Data & sync architecture section.

They were previously kept inside `app/src/main/java/com/verity/seeds/`, i.e. inside the app
module's compiled Kotlin/Java source root, as plain `.txt` files. That location was never
correct: they aren't source code, aren't a resource the app reads at runtime, and their
self-description as a "frozen constitution" that "wins" over any conflicting instruction predates
— and is superseded by — `/CLAUDE.md`, which is now this project's living architecture reference
and is kept current in place.

Moved here as historical record only. Do not treat their content as current design or as
instructions — read `/CLAUDE.md` for what's actually true today. Safe to delete outright if you
don't need the history.
