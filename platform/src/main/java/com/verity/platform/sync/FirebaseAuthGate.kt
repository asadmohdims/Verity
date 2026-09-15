package com.verity.platform.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * FirebaseAuthGate
 *
 * Signs in with one fixed business account, silently, on app launch — no login screen, no
 * multi-user UI. Real per-org access control (Firestore/Storage Security Rules checking the
 * `org_id` custom claim on this account's token) needs *some* authenticated identity; this is
 * the minimum that makes that claim enforceable. See CLAUDE.md's Data & sync architecture and
 * the cloud-sync plan's "Commercialization scope check" for why a single shared login is the
 * deliberate choice here, not a placeholder for real per-user accounts.
 *
 * Credentials are sourced from BuildConfig (see platform/build.gradle.kts), which reads them
 * from local.properties (gitignored) rather than being hardcoded in source — keeps the
 * credential out of git history. Still extractable from a decompiled APK, which is an accepted
 * limitation for a single-business, privately-distributed app; it would need real per-customer
 * provisioning (a server-minted token, or an actual login flow) before this app is ever
 * distributed to multiple different customer businesses — not a concern to solve now, but
 * deliberately not silently glossed over either.
 */
class FirebaseAuthGate(
    private val auth: FirebaseAuth,
    private val timeoutMillis: Long = 5_000
) {

    /**
     * Idempotent: a no-op if already signed in, relying on the SDK's own session persistence.
     *
     * Timeout-guarded (added 2026-09-15, alongside the same fix in FirebaseSyncClient.downloadPdf
     * - found by testing offline on a real device): this runs from MainActivity's startup
     * LaunchedEffect, not the finalize path, so a hang here never blocked finalize directly - but
     * an unguarded network .await() with no timeout is the same bug class regardless of which
     * caller hits it, so it's fixed here too rather than left as a latent trap.
     */
    suspend fun ensureSignedIn(email: String, password: String): Boolean {
        if (auth.currentUser != null) return true
        if (email.isBlank() || password.isBlank()) {
            Log.w(TAG, "No Firebase Auth credentials configured — cloud sync will stay offline.")
            return false
        }
        val signedIn = withTimeoutOrNull(timeoutMillis) {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                true
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sign-in failed — cloud sync will retry on next launch.", e)
                false
            }
        }
        return signedIn ?: false
    }

    private companion object {
        const val TAG = "FirebaseAuthGate"
    }
}
