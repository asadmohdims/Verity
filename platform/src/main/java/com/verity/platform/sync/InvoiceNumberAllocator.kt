package com.verity.platform.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.verity.platform.database.dao.DocumentDao
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * InvoiceNumberAllocator
 *
 * Hardened invoice numbering for a shared login used from more than one device (see the
 * cloud-sync plan's "Commercialization scope check" and "Atomic invoice numbering" sections).
 * Atomic online, local fallback offline — never blocks finalize on the network (Principle 6),
 * and shrinks the collision window from "every finalize" down to "two devices both offline and
 * finalizing at the exact same moment."
 *
 * Residual edge case, accepted rather than engineered around: if the online transaction actually
 * succeeds server-side but this call times out before confirming, and the local fallback then
 * runs too, the Firestore counter and local usage can drift by one. Narrow and low-probability
 * at this app's realistic volume (small businesses, a shared login, rare true concurrent
 * finalizes) — not worth reconciliation machinery for Phase 1.
 */
interface InvoiceNumberAllocator {
    suspend fun allocate(orgId: String, documentType: String): Long
}

/**
 * Talks to the online atomic counter only — kept behind its own interface so
 * DefaultInvoiceNumberAllocator's fallback logic (the actual thing worth unit-testing) can be
 * tested against a fake, without needing a real FirebaseFirestore instance.
 */
fun interface OnlineCounterSource {
    suspend fun nextValue(orgId: String, documentType: String): Long
}

class DefaultInvoiceNumberAllocator(
    private val onlineCounterSource: OnlineCounterSource,
    private val documentDao: DocumentDao,
    private val onlineTimeoutMillis: Long = 5_000
) : InvoiceNumberAllocator {

    override suspend fun allocate(orgId: String, documentType: String): Long {
        val atomic = withTimeoutOrNull(onlineTimeoutMillis) {
            runCatching { onlineCounterSource.nextValue(orgId, documentType) }.getOrNull()
        }
        return atomic ?: localFallback(orgId, documentType)
    }

    private suspend fun localFallback(orgId: String, documentType: String): Long =
        (documentDao.getMaxSequenceNumber(orgId, documentType) ?: 0L) + 1
}

/**
 * Real online counter: one Firestore document per org+documentType holding `lastSequence`,
 * incremented atomically inside a transaction. Must be seeded from the current local max
 * sequence the first time this ships in a given org — a one-off rollout step, not ongoing logic
 * (see the cloud-sync plan's rollout note).
 */
class FirestoreOnlineCounterSource(private val firestore: FirebaseFirestore) : OnlineCounterSource {

    override suspend fun nextValue(orgId: String, documentType: String): Long {
        val counterRef = firestore.collection("orgs/$orgId/counters").document(documentType)
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(counterRef)
            val next = (snapshot.getLong("lastSequence") ?: 0L) + 1
            transaction.set(counterRef, mapOf("lastSequence" to next))
            next
        }.await()
    }
}
