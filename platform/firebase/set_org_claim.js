/**
 * One-off setup script — NOT shipped in the app, run once from a developer machine.
 *
 * Firebase custom claims (org_id) can only be set server-side via the Admin SDK — there's no
 * console UI for it, unlike Supabase's dashboard-editable app_metadata (see FirebaseAuthGate's
 * doc comment). This mints the org_id claim on the one fixed business account used by
 * FirebaseAuthGate, which firestore.rules / storage.rules then check on every request.
 *
 * Usage:
 *   1. In the Firebase console, create the business's Auth user (email + password) — the same
 *      credentials you'll put in local.properties as firebase.auth.email / firebase.auth.password.
 *   2. Project settings -> Service accounts -> Generate new private key. Save the JSON somewhere
 *      OUTSIDE this repo (never commit a service-account key).
 *   3. npm install firebase-admin (in a scratch directory, or wherever you run this from).
 *   4. node set_org_claim.js <path-to-service-account.json> <user-uid> <org-id>
 *      (find the user's uid in the console, under Authentication -> Users)
 *
 * Rollout note (see the cloud-sync plan's InvoiceNumberAllocator section): the first time this
 * ships to a device with existing local documents, also seed
 * orgs/<org-id>/counters/INVOICE and orgs/<org-id>/counters/CHALLAN with
 * { lastSequence: <current local max sequenceNumber for that type> } in the Firestore console —
 * otherwise the first online-atomic allocation would start from 0 and could collide with numbers
 * already used locally. A one-off manual step, not scripted here since it depends on whatever
 * that device's local data already is.
 */
// Modular imports (the current documented pattern), not the older `require("firebase-admin")`
// namespace-style API - that compatibility surface has shifted across firebase-admin versions
// and `admin.credential`/`admin.auth()` can come back undefined depending on which version npm
// resolves. This form is stable regardless of the installed version.
const { initializeApp, cert } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");

const [, , serviceAccountPath, uid, orgId] = process.argv;

if (!serviceAccountPath || !uid || !orgId) {
  console.error("Usage: node set_org_claim.js <service-account.json> <user-uid> <org-id>");
  process.exit(1);
}

initializeApp({
  credential: cert(require(serviceAccountPath))
});

getAuth()
  .setCustomUserClaims(uid, { org_id: orgId })
  .then(() => {
    console.log(`Set org_id="${orgId}" on user ${uid}.`);
    console.log("The user must sign in again (or refresh their ID token) for this to take effect.");
    process.exit(0);
  })
  .catch((error) => {
    console.error("Failed to set custom claim:", error);
    process.exit(1);
  });
