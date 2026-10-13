package com.benisalla.glitch.menu

import com.benisalla.glitch.model.AppEntry
import kotlin.random.Random

/** Pure ordering rules for the menu. No Android dependencies, so it is unit tested on the JVM. */
object MenuOrder {

    /** Returns a uniformly random permutation of [entries] (Fisher–Yates). */
    fun shuffled(entries: List<AppEntry>, random: Random): List<AppEntry> = entries.shuffled(random)

    /**
     * Applies a fresh catalog snapshot to the order the user is looking at, without reshuffling:
     * surviving entries keep their relative positions (with [latest]'s data, so label changes show),
     * removed entries are dropped, and new entries are appended in [latest] order.
     */
    fun reconcile(current: List<AppEntry>, latest: List<AppEntry>): List<AppEntry> {
        val latestByKey = LinkedHashMap<String, AppEntry>()
        latest.forEach { latestByKey.putIfAbsent(it.key, it) }

        val result = ArrayList<AppEntry>(latestByKey.size)
        val seen = HashSet<String>()
        for (entry in current) {
            val updated = latestByKey[entry.key] ?: continue
            if (seen.add(entry.key)) result += updated
        }
        for ((key, entry) in latestByKey) {
            if (seen.add(key)) result += entry
        }
        return result
    }
}
