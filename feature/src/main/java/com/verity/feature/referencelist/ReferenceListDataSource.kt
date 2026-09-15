package com.verity.feature.referencelist

/**
 * The manually-curated reference lists Verity currently needs. All are a plain user-managed list
 * of short strings (Transporter Name, HSN Code, Unit) — one generic table keyed by this enum
 * instead of a near-identical entity/DAO pair per kind, since every case is structurally
 * identical (see CLAUDE.md's "earn your abstractions": this serves concrete, real cases, not a
 * hypothetical future one).
 */
enum class ReferenceListKind {
    TRANSPORTER_NAME,
    HSN_CODE,
    UNIT
}

/**
 * One entry in a manually-curated reference list.
 */
data class ReferenceListItem(
    val id: String,
    val value: String
)

/**
 * ReferenceListDataSource
 *
 * Read/write access to the manually-curated Transporter Name / HSN Code lists — added from their
 * own management screen (reached via Settings), then offered back as autocomplete suggestions
 * when filling in Transportation Details / a line item's HSN Code. Free text is always still
 * accepted at the point of entry; a list match is a convenience, not a constraint.
 */
interface ReferenceListDataSource {

    /**
     * All values for [kind], ordered for stable display (alphabetical).
     */
    suspend fun getAll(kind: ReferenceListKind): List<ReferenceListItem>

    /**
     * Adds [value] to [kind]'s list. A duplicate (case-insensitive) is a silent no-op rather than
     * an error — the management screen has no use for surfacing "you already added that".
     */
    suspend fun add(kind: ReferenceListKind, value: String)

    /**
     * Removes one entry by id.
     */
    suspend fun delete(kind: ReferenceListKind, id: String)
}
