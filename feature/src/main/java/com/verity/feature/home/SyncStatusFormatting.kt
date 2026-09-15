package com.verity.feature.home

/**
 * Shared text for a SyncStatus — originally private to HomeScreen's SyncStatusLine, promoted here
 * once Settings' sync-status row needed the exact same text (see CLAUDE.md's "earn your
 * abstractions" principle: two real call sites, not a speculative one).
 */
fun syncStatusText(status: SyncStatus): String? = when {
    status.pendingCount > 0 -> if (status.pendingCount == 1) "1 pending" else "${status.pendingCount} pending"
    status.lastSyncedAtEpochMillis != null -> "Synced · ${relativeTimeAgo(status.lastSyncedAtEpochMillis)}"
    else -> null
}

fun relativeTimeAgo(epochMillis: Long): String {
    val elapsedMinutes = (System.currentTimeMillis() - epochMillis) / 60_000
    return when {
        elapsedMinutes < 1 -> "just now"
        elapsedMinutes < 60 -> "${elapsedMinutes}m ago"
        elapsedMinutes < 24 * 60 -> "${elapsedMinutes / 60}h ago"
        else -> "${elapsedMinutes / (24 * 60)}d ago"
    }
}
