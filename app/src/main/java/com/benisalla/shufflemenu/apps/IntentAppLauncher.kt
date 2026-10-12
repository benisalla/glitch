package com.benisalla.shufflemenu.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.benisalla.shufflemenu.model.AppEntry
import com.benisalla.shufflemenu.model.AppLauncher
import com.benisalla.shufflemenu.model.LaunchResult

/**
 * The same intent a home-screen launcher sends, so the target app opens normally
 * and resumes its existing task if it has one.
 */
internal fun launchIntentFor(entry: AppEntry): Intent =
    Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName(entry.packageName, entry.activityName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

/** [AppLauncher] that starts the entry's launcher activity. */
class IntentAppLauncher(context: Context) : AppLauncher {

    private val context = context.applicationContext

    override fun launch(entry: AppEntry): LaunchResult {
        val intent = launchIntentFor(entry)
        // Disabled or removed components do not resolve with default flags.
        val activity = resolve(intent)?.activityInfo
        if (activity == null || !activity.exported) return LaunchResult.Unavailable
        return try {
            context.startActivity(intent)
            LaunchResult.Launched
        } catch (e: ActivityNotFoundException) {
            LaunchResult.Unavailable
        } catch (e: SecurityException) {
            LaunchResult.Failed(e.message ?: "Not allowed to open ${entry.label}")
        }
    }

    private fun resolve(intent: Intent): ResolveInfo? {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, 0)
        }
    }
}
