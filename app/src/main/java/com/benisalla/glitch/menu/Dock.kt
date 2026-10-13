package com.benisalla.glitch.menu

import com.benisalla.glitch.model.AppEntry

/**
 * A system default app (e.g. the default dialer) as resolved by the platform. [activityName] is
 * null when only the package is known (e.g. the default SMS app).
 */
data class DefaultApp(val packageName: String, val activityName: String? = null)

/** The fixed dock (stable, user-chosen order) and the shuffled grid (everything else). */
data class DockLayout(val dock: List<AppEntry>, val grid: List<AppEntry>)

/** Pure dock rules. No Android dependencies, so it is unit tested on the JVM. */
object Dock {

    const val MAX_APPS = 4

    /**
     * Splits [ordered] into the dock, in [dockKeys] order, and the grid, in [ordered] order.
     * Keys of apps that are no longer installed are skipped, duplicates are ignored, and at most
     * [MAX_APPS] apps go to the dock. Every entry ends up in exactly one of the two lists.
     */
    fun partition(ordered: List<AppEntry>, dockKeys: List<String>): DockLayout {
        val byKey = ordered.associateBy { it.key }
        val dock = dockKeys.distinct().mapNotNull { byKey[it] }.take(MAX_APPS)
        val inDock = dock.mapTo(HashSet()) { it.key }
        return DockLayout(dock = dock, grid = ordered.filter { it.key !in inDock })
    }

    /**
     * First-run dock: for each slot (dialer, messaging, camera, browser), the first candidate that
     * maps to a launcher entry. An exact activity match wins over another launcher activity of the
     * same package. Slots that resolve to nothing, or only to an app already chosen, are skipped.
     */
    fun defaults(slots: List<List<DefaultApp>>, apps: List<AppEntry>): List<String> {
        val chosen = LinkedHashSet<String>()
        for (slot in slots) {
            if (chosen.size == MAX_APPS) break
            val entry = slot.asSequence().mapNotNull { match(it, apps) }.firstOrNull { it.key !in chosen }
            if (entry != null) chosen += entry.key
        }
        return chosen.toList()
    }

    /** [dockKeys] with [key] appended, unchanged if already docked, or null if the dock is full. */
    fun withAdded(dockKeys: List<String>, key: String): List<String>? = when {
        key in dockKeys -> dockKeys
        dockKeys.size >= MAX_APPS -> null
        else -> dockKeys + key
    }

    fun withRemoved(dockKeys: List<String>, key: String): List<String> = dockKeys.filter { it != key }

    private fun match(target: DefaultApp, apps: List<AppEntry>): AppEntry? =
        apps.firstOrNull { it.packageName == target.packageName && it.activityName == target.activityName }
            ?: apps.firstOrNull { it.packageName == target.packageName }
}
