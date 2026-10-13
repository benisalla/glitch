package com.benisalla.glitch.apps

import android.app.role.RoleManager
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

private const val TAG = "HomeScreen"

private fun homeIntent(): Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

/**
 * True when Glitch is the default home app.
 *
 * Android 10+: asks RoleManager whether we hold the HOME role. Resolving the HOME intent is not
 * reliable there: package visibility (Android 11+) hides a default launcher that has no LAUNCHER
 * activity, and the resolution then falls back to our own HOME filter, wrongly answering "Glitch".
 * Android 8-9 have no package visibility, so resolving the HOME intent is exact there.
 */
fun isDefaultHome(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        }
    }
    return resolveDefault(context.packageManager, homeIntent())?.packageName == context.packageName
}

/** True if [intent] is a Home button press (as opposed to the LAUNCHER icon). */
fun isHomeIntent(intent: Intent?): Boolean =
    intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)

/**
 * The system dialog that asks to make Glitch the home app (Android 10+), or null when it is not
 * available; callers then open [homeSettingsIntent].
 */
fun requestHomeRoleIntent(context: Context): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
    if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return null
    return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
}

/** System settings screen for choosing the default home app. */
fun homeSettingsIntent(): Intent =
    Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

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
