package com.benisalla.glitch.model

import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.Flow

/** Discovers launchable apps. Owned by the Android integration code in `apps/`. */
interface AppCatalog {
    /** Returns every launchable entry exactly once, excluding this app. Safe to call off the main thread. */
    suspend fun loadApps(): List<AppEntry>

    /** Returns the entry's icon, or null if the app is gone or the icon cannot be loaded. */
    suspend fun loadIcon(entry: AppEntry): Drawable?

    /** Emits whenever installed/removed/changed apps may have altered the launchable set. */
    fun changes(): Flow<Unit>
}

/** Launches an entry. Owned by the Android integration code in `apps/`. */
fun interface AppLauncher {
    fun launch(entry: AppEntry): LaunchResult
}

sealed interface LaunchResult {
    data object Launched : LaunchResult

    /** The component no longer exists or is disabled; the menu should refresh. */
    data object Unavailable : LaunchResult

    data class Failed(val reason: String) : LaunchResult
}
