package com.verity.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SeedSampleData"

/**
 * SeedSampleDataReceiver
 *
 * DEBUG-ONLY (declared only in app/src/debug/AndroidManifest.xml, so it doesn't exist in a
 * release build at all). Lets sample data be seeded into the already-installed app's real
 * database from the terminal, without touching the app's install state the way
 * `connectedAndroidTest` does:
 *
 *   adb shell am broadcast -n com.verity/.debug.SeedSampleDataReceiver \
 *       -a com.verity.debug.SEED_SAMPLE_DOCUMENTS
 *
 * Watch progress with: adb logcat -s SeedSampleData
 */
class SeedSampleDataReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val created = SampleDataSeeder.seed(appContext)
                Log.i(TAG, "Seeded $created sample documents")
            } catch (e: Exception) {
                Log.e(TAG, "Seeding failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
