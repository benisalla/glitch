package com.benisalla.glitch.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import androidx.core.net.toUri
import com.benisalla.glitch.menu.DefaultApp
import com.benisalla.glitch.model.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DefaultApps"

/** The system "App info" screen for [entry]'s package. */
fun appInfoIntent(entry: AppEntry): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", entry.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/**
 * The system default dialer, messaging, camera and browser apps, one candidate list per dock
 * slot, best candidate first. These packages are visible to us only because they also have a
 * launcher activity (the manifest's narrow MAIN/LAUNCHER query), which is exactly what the dock
 * can show. Unresolvable slots come back empty; [com.benisalla.glitch.menu.Dock.defaults] skips them.
 */
suspend fun resolveDefaultDockApps(context: Context): List<List<DefaultApp>> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    fun resolve(intent: Intent): DefaultApp? = resolveDefault(pm, intent)?.let { DefaultApp(it.packageName, it.name) }
    fun defaultSms(): DefaultApp? = Telephony.Sms.getDefaultSmsPackage(context)?.let { DefaultApp(it) }
    fun slot(vararg candidates: () -> DefaultApp?): List<DefaultApp> = candidates.mapNotNull { candidate ->
        try {
            candidate()
        } catch (e: Exception) {
            // E.g. no telephony on a tablet. One missing default must not cost the others.
            Log.w(TAG, "Default app lookup failed", e)
            null
        }
    }
    listOf(
        slot({ resolve(Intent(Intent.ACTION_DIAL)) }),
        slot(
            // The default SMS app is the user's explicit choice, so it goes first.
            ::defaultSms,
            { resolve(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)) },
        ),
        slot({ resolve(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) }),
        slot(
            { resolve(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)) },
            { resolve(Intent(Intent.ACTION_VIEW, "https://".toUri())) },
        ),
    )
}

/**
 * The activity that handles [intent] by default, or null. When there is no default the system
 * returns its chooser (package "android"), which never matches a launcher entry.
 */
private fun resolveDefault(pm: PackageManager, intent: Intent): ActivityInfo? {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    } else {
        @Suppress("DEPRECATION")
        pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    return info?.activityInfo
}
