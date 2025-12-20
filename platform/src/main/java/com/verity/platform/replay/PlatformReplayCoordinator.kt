package com.verity.platform.replay

import com.verity.platform.database.projection.LedgerProjectionWriter
import com.verity.platform.replay.document.DocumentIndexProjectionWriter

/**
 * PlatformReplayCoordinator
 *
 * PURPOSE
 * -------
 * Single canonical entry point for running replay-driven
 * projections for an organization.
 *
 * This coordinator:
 * • Defines projection execution order
 * • Delegates to projection writers
 * • Provides a stable platform-level replay contract
 *
 * It contains no business logic, persistence logic,
 * or scheduling concerns.
 */
interface PlatformReplayCoordinator {

    /**
     * Runs incremental replay for all projections
     * for the given organization.
     */
    suspend fun runIncremental(orgId: String)

    /**
     * Performs a full rebuild of all projections
     * for the given organization.
     *
     * Rebuild support is projection-specific.
     */
    suspend fun rebuildAll(orgId: String)
}

/**
 * DefaultPlatformReplayCoordinator
 *
 * Phase-1 implementation.
 *
 * This class is deliberately minimal, deterministic,
 * and free of policy decisions.
 */
class DefaultPlatformReplayCoordinator(
    private val ledgerProjectionWriter: LedgerProjectionWriter,
    private val documentIndexProjectionWriter: DocumentIndexProjectionWriter
) : PlatformReplayCoordinator {

    override suspend fun runIncremental(orgId: String) {
        // 1. Ledger must always be updated first
        ledgerProjectionWriter.runIncremental(orgId)

        // 2. Document index follows
        documentIndexProjectionWriter.run(orgId)
    }

    override suspend fun rebuildAll(orgId: String) {
        // Ledger rebuild is intentionally not supported yet (Phase 1 scope)
        // Document index rebuild is safe and deterministic
        documentIndexProjectionWriter.rebuild(orgId)
    }
}