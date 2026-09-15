package com.verity.platform.sync

import android.content.Context

/**
 * SyncStatusStore
 *
 * Small shared bit of state between FirebaseSyncClient (writer, on each successful push) and
 * DefaultHomeDataSource (reader, for the Home "last synced" line) plus MainActivity (the
 * new-device restore trigger flag) — a plain SharedPreferences wrapper, not a new persistence
 * layer, since this is UI-affordance bookkeeping, not domain data.
 */
class SyncStatusStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordSyncSuccess(epochMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNCED_AT, epochMillis).apply()
    }

    fun lastSyncedAtEpochMillis(): Long? =
        prefs.getLong(KEY_LAST_SYNCED_AT, -1L).takeIf { it >= 0 }

    /** See FirebaseRestoreClient's doc comment — gates the one-time new-device bulk restore. */
    fun isInitialRestoreCompleted(): Boolean = prefs.getBoolean(KEY_INITIAL_RESTORE_DONE, false)

    fun markInitialRestoreCompleted() {
        prefs.edit().putBoolean(KEY_INITIAL_RESTORE_DONE, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "verity_sync"
        const val KEY_LAST_SYNCED_AT = "last_synced_at"
        const val KEY_INITIAL_RESTORE_DONE = "initial_restore_completed"
    }
}
