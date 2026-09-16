package com.verity.platform.sync

import android.content.Context

/**
 * The slice of SyncStatusStore that FirebaseRestoreClient needs to track its own incremental-sync
 * progress. Split out purely so FirebaseRestoreClientTest can fake it without pulling in a
 * Robolectric Context — SyncStatusStore is the only real implementation.
 */
interface SyncWatermarkStore {
    /**
     * True once this device has done at least one unfiltered catch-up fetch since the
     * serverSyncedAt field was introduced. Documents pushed before that field existed have no
     * value for it at all, so a filtered query would never match them — the one-time unfiltered
     * fetch this flag gates is what picks those up; every sync after that can safely filter.
     */
    fun hasCompletedBaselineSync(): Boolean
    fun markBaselineSyncCompleted()

    /** Highest serverSyncedAt (epoch millis) this device has pulled or pushed so far, or null. */
    fun lastSyncWatermarkMillis(): Long?
    fun recordSyncWatermark(epochMillis: Long)
}

/**
 * SyncStatusStore
 *
 * Small shared bit of state between FirebaseSyncClient (writer, on each successful push) and
 * DefaultHomeDataSource (reader, for the Home "last synced" line) plus FirebaseRestoreClient (its
 * own incremental-sync watermark, see SyncWatermarkStore above) — a plain SharedPreferences
 * wrapper, not a new persistence layer, since this is UI-affordance/sync bookkeeping, not domain
 * data.
 */
class SyncStatusStore(context: Context) : SyncWatermarkStore {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordSyncSuccess(epochMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNCED_AT, epochMillis).apply()
    }

    fun lastSyncedAtEpochMillis(): Long? =
        prefs.getLong(KEY_LAST_SYNCED_AT, -1L).takeIf { it >= 0 }

    override fun hasCompletedBaselineSync(): Boolean = prefs.getBoolean(KEY_BASELINE_SYNC_DONE, false)

    override fun markBaselineSyncCompleted() {
        prefs.edit().putBoolean(KEY_BASELINE_SYNC_DONE, true).apply()
    }

    override fun lastSyncWatermarkMillis(): Long? =
        prefs.getLong(KEY_SYNC_WATERMARK, -1L).takeIf { it >= 0 }

    override fun recordSyncWatermark(epochMillis: Long) {
        prefs.edit().putLong(KEY_SYNC_WATERMARK, epochMillis).apply()
    }

    private companion object {
        const val PREFS_NAME = "verity_sync"
        const val KEY_LAST_SYNCED_AT = "last_synced_at"
        const val KEY_BASELINE_SYNC_DONE = "baseline_sync_completed"
        const val KEY_SYNC_WATERMARK = "sync_watermark_millis"
    }
}
