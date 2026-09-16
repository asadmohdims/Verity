package com.verity.platform.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds `customers.orgId` and `customers.syncedToCloud`, bringing CustomerEntity in line with
 * DocumentEntity/LedgerEntryEntity now that customers are synced to Firestore too (see
 * FirebaseSyncClient.pushCustomer / FirebaseRestoreClient). Existing rows default to
 * orgId='default-org' (this app's one org so far) and syncedToCloud=0 — the latter is exactly the
 * signal CustomerDao.getUnsyncedCustomers() needs to push every pre-existing local customer to the
 * cloud once, the first time this migration runs on a device.
 */
object Migration5To6 : Migration(5, 6) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE customers ADD COLUMN orgId TEXT NOT NULL DEFAULT 'default-org'")
        db.execSQL("ALTER TABLE customers ADD COLUMN syncedToCloud INTEGER NOT NULL DEFAULT 0")
    }
}
