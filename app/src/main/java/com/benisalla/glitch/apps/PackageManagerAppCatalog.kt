package com.benisalla.glitch.apps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.util.Log
import androidx.core.content.ContextCompat
import com.benisalla.glitch.model.AppCatalog
import com.benisalla.glitch.model.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

/**
 * [AppCatalog] backed by [PackageManager]. Only activities answering the standard
 * MAIN/LAUNCHER intent are visible, matching the manifest's narrow `<queries>` block.
 */
class PackageManagerAppCatalog(context: Context) : AppCatalog {

    private val context = context.applicationContext
    private val pm = this.context.packageManager

    override suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val ownPackage = context.packageName
        queryLauncherActivities()
            .mapNotNull { info ->
                try {
                    val activity = info.activityInfo ?: return@mapNotNull null
                    if (activity.packageName == ownPackage) return@mapNotNull null
                    // The query already skips components disabled at runtime. Do not check
                    // ActivityInfo.enabled: it is the manifest value, and apps that declare a launcher
                    // activity disabled and enable it later (e.g. icon aliases) would be hidden.
                    if (!activity.exported) return@mapNotNull null
                    val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                    AppEntry(
                        packageName = activity.packageName,
                        activityName = activity.name,
                        label = label.ifBlank { activity.packageName },
                    )
                } catch (e: Exception) {
                    // One broken or concurrently removed app must not break the whole list.
                    Log.w(TAG, "Skipping launcher entry", e)
                    null
                }
            }
            .distinctBy { it.key }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, AppEntry::label).thenBy { it.key })
    }

    override suspend fun loadIcon(entry: AppEntry): Drawable? = withContext(Dispatchers.IO) {
        try {
            pm.getActivityIcon(ComponentName(entry.packageName, entry.activityName))
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            Log.w(TAG, "Icon load failed for ${entry.key}", e)
            null
        }
    }

    /**
     * Cold flow that emits when the set of launchable apps may have changed. The
     * listener is registered only while collected and unregistered on cancellation;
     * bursts of events are conflated.
     */
    override fun changes(): Flow<Unit> = callbackFlow {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        if (launcherApps != null) {
            val callback = object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: UserHandle) { trySend(Unit) }
                override fun onPackageRemoved(packageName: String, user: UserHandle) { trySend(Unit) }
                override fun onPackageChanged(packageName: String, user: UserHandle) { trySend(Unit) }
                override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                    trySend(Unit)
                }
                override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                    trySend(Unit)
                }
                override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) { trySend(Unit) }
                override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) { trySend(Unit) }
            }
            launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
            awaitClose { launcherApps.unregisterCallback(callback) }
        } else {
            // Fallback: package broadcasts are protected system broadcasts, which still reach
            // a not-exported receiver.
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) { trySend(Unit) }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            awaitClose { context.unregisterReceiver(receiver) }
        }
    }.conflate()

    private fun queryLauncherActivities(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    }

    private companion object {
        const val TAG = "AppCatalog"
    }
}
