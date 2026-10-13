package com.benisalla.glitch.menu

import com.benisalla.glitch.model.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DockTest {

    private fun entry(id: String, activity: String = "Main") = AppEntry("pkg.$id", "pkg.$id.$activity", "App $id")

    private val apps = (0 until 8).map { entry("$it") }

    private fun assertPartitionInvariants(ordered: List<AppEntry>, layout: DockLayout) {
        val all = layout.dock + layout.grid
        assertEquals("nothing lost, nothing duplicated", ordered.toSet(), all.toSet())
        assertEquals(ordered.size, all.size)
        assertTrue(layout.dock.size <= Dock.MAX_APPS)
        assertTrue(layout.dock.none { it in layout.grid })
        assertEquals("grid keeps the given order", ordered.filter { it !in layout.dock }, layout.grid)
    }

    @Test
    fun partition_dockAppsAreExcludedFromGrid() {
        val layout = Dock.partition(apps, listOf(apps[3].key, apps[0].key))
        assertEquals(listOf(apps[3], apps[0]), layout.dock)
        assertPartitionInvariants(apps, layout)
    }

    @Test
    fun partition_dockOrderIsStableAcrossShuffles() {
        val dockKeys = listOf(apps[5].key, apps[2].key, apps[7].key)
        val random = Random(7)
        repeat(100) {
            val shuffled = MenuOrder.shuffled(apps, random)
            val layout = Dock.partition(shuffled, dockKeys)
            assertEquals(listOf(apps[5], apps[2], apps[7]), layout.dock)
            assertPartitionInvariants(shuffled, layout)
        }
    }

    @Test
    fun partition_atMostFourAppsAndExtrasStayInGrid() {
        val layout = Dock.partition(apps, apps.take(6).map { it.key })
        assertEquals(apps.take(4), layout.dock)
        assertTrue(apps[4] in layout.grid && apps[5] in layout.grid)
        assertPartitionInvariants(apps, layout)
    }

    @Test
    fun partition_removedAppsAreDropped() {
        val installed = apps - apps[1]
        val layout = Dock.partition(installed, listOf(apps[0].key, apps[1].key, apps[2].key))
        assertEquals(listOf(apps[0], apps[2]), layout.dock)
        assertPartitionInvariants(installed, layout)
    }

    @Test
    fun partition_duplicateAndUnknownKeysAreIgnored() {
        val layout = Dock.partition(apps, listOf(apps[2].key, "nope/nope", apps[2].key, apps[4].key))
        assertEquals(listOf(apps[2], apps[4]), layout.dock)
        assertPartitionInvariants(apps, layout)
    }

    @Test
    fun partition_emptyCases() {
        assertEquals(DockLayout(emptyList(), apps), Dock.partition(apps, emptyList()))
        assertEquals(DockLayout(emptyList(), emptyList()), Dock.partition(emptyList(), listOf(apps[0].key)))
    }

    @Test
    fun withAdded_appendsUntilFull() {
        var dock = emptyList<String>()
        for (i in 0 until Dock.MAX_APPS) dock = Dock.withAdded(dock, apps[i].key)!!
        assertEquals(apps.take(4).map { it.key }, dock)
        assertNull(Dock.withAdded(dock, apps[5].key))
        assertEquals("already docked is a no-op, even when full", dock, Dock.withAdded(dock, apps[0].key))
    }

    @Test
    fun withRemoved_keepsOrderOfOthers() {
        val dock = apps.take(4).map { it.key }
        assertEquals(listOf(apps[0].key, apps[2].key, apps[3].key), Dock.withRemoved(dock, apps[1].key))
        assertEquals(dock, Dock.withRemoved(dock, "not/there"))
    }

    // --- First-run defaults ---

    @Test
    fun defaults_pickOnePerSlotInSlotOrder() {
        val slots = listOf(
            listOf(DefaultApp("pkg.4", "pkg.4.Main")), // dialer
            listOf(DefaultApp("pkg.1")), // messaging (default SMS package only)
            listOf(DefaultApp("pkg.6", "pkg.6.Main")), // camera
            listOf(DefaultApp("pkg.2", "pkg.2.Main")), // browser
        )
        assertEquals(listOf(apps[4], apps[1], apps[6], apps[2]).map { it.key }, Dock.defaults(slots, apps))
    }

    @Test
    fun defaults_skipUnresolvedSlots() {
        val slots = listOf(
            emptyList(),
            listOf(DefaultApp("android", "com.android.internal.app.ResolverActivity")), // no default chosen
            listOf(DefaultApp("pkg.not.installed")),
            listOf(DefaultApp("pkg.3", "pkg.3.Main")),
        )
        assertEquals(listOf(apps[3].key), Dock.defaults(slots, apps))
    }

    @Test
    fun defaults_fallBackToNextCandidateInSlot() {
        val slots = listOf(listOf(DefaultApp("pkg.missing"), DefaultApp("pkg.5", "pkg.5.Main")))
        assertEquals(listOf(apps[5].key), Dock.defaults(slots, apps))
    }

    @Test
    fun defaults_preferExactActivityThenAnyLauncherActivityOfPackage() {
        val contacts = entry("c", "Contacts")
        val dialer = entry("c", "Dialer")
        val catalog = listOf(contacts, dialer)
        assertEquals(listOf(dialer.key), Dock.defaults(listOf(listOf(DefaultApp("pkg.c", dialer.activityName))), catalog))
        // The resolved activity is not a launcher activity (e.g. a DIAL handler): use the package's entry.
        assertEquals(listOf(contacts.key), Dock.defaults(listOf(listOf(DefaultApp("pkg.c", "pkg.c.DialHandler"))), catalog))
    }

    @Test
    fun defaults_neverDuplicateAnApp() {
        val slots = listOf(
            listOf(DefaultApp("pkg.1")),
            listOf(DefaultApp("pkg.1"), DefaultApp("pkg.2")), // same app again: falls to the next candidate
            listOf(DefaultApp("pkg.1")), // only a duplicate: slot skipped
        )
        assertEquals(listOf(apps[1].key, apps[2].key), Dock.defaults(slots, apps))
    }

    @Test
    fun defaults_atMostFour() {
        val slots = (0 until 6).map { listOf(DefaultApp("pkg.$it")) }
        assertEquals(apps.take(4).map { it.key }, Dock.defaults(slots, apps))
    }
}
