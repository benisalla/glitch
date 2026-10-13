package com.benisalla.glitch.widget

import com.benisalla.glitch.model.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WidgetOrderTest {

    /** In-memory [WidgetOrderStore] with the same semantics as the SharedPreferences one. */
    private class FakeStore : WidgetOrderStore {
        var keys: List<String>? = null
        var requested = 0
        var handled = 0
        /** Runs between load and save, to simulate a request arriving during a reload. */
        var duringRefresh: (() -> Unit)? = null

        override fun load(): SavedWidgetOrder? {
            val saved = keys?.let { SavedWidgetOrder(it, requested, handled) }
            duringRefresh?.invoke()
            duringRefresh = null
            return saved
        }

        override fun save(keys: List<String>, handled: Int) {
            this.keys = keys
            this.handled = handled
        }

        override fun requestReshuffle() {
            requested++
        }

        override fun requested(): Int = requested
    }

    /** Seeded Random that counts bounded draws: a shuffle of n items makes exactly n - 1. */
    private class CountingRandom(seed: Int) : Random() {
        private val delegate = Random(seed)
        var calls = 0
        override fun nextBits(bitCount: Int): Int = delegate.nextBits(bitCount)
        override fun nextInt(from: Int, until: Int): Int {
            calls++
            return delegate.nextInt(from, until)
        }
    }

    private val apps = (1..8).map { AppEntry("pkg.$it", "pkg.$it.Main", "App $it") }
    private val store = FakeStore()
    private val random = CountingRandom(seed = 7)
    private val state = WidgetOrderState(store, random)
    private val drawsPerShuffle = apps.size - 1

    private fun assertPermutation(expected: List<AppEntry>, actual: List<AppEntry>) {
        assertEquals("no duplicates", actual.size, actual.map { it.key }.toSet().size)
        assertEquals("same keys, nothing lost", expected.map { it.key }.toSet(), actual.map { it.key }.toSet())
    }

    @Test
    fun firstRefresh_shufflesAndSaves() {
        val order = state.refresh(apps)
        assertPermutation(apps, order)
        assertEquals(drawsPerShuffle, random.calls)
        assertEquals(order.map { it.key }, store.keys)
    }

    @Test
    fun refreshWithoutRequest_keepsSavedOrder() {
        val first = state.refresh(apps)
        val callsAfterFirst = random.calls
        repeat(3) { assertEquals(first, state.refresh(apps)) }
        assertEquals("no reshuffle on an incidental refresh", callsAfterFirst, random.calls)
    }

    @Test
    fun requestReshuffle_shufflesOnceOnNextRefresh() {
        state.refresh(apps)
        store.requestReshuffle()
        val second = state.refresh(apps)
        assertPermutation(apps, second)
        assertEquals("one more shuffle", 2 * drawsPerShuffle, random.calls)
        assertEquals("request consumed", second, state.refresh(apps))
        assertEquals(2 * drawsPerShuffle, random.calls)
    }

    @Test
    fun severalRequestsBeforeRefresh_shuffleOnce() {
        state.refresh(apps)
        repeat(3) { store.requestReshuffle() }
        state.refresh(apps)
        state.refresh(apps)
        assertEquals(2 * drawsPerShuffle, random.calls)
    }

    @Test
    fun requestDuringRefresh_isNotLost() {
        state.refresh(apps)
        store.requestReshuffle()
        store.duringRefresh = { store.requestReshuffle() }
        state.refresh(apps) // consumes the first request only
        assertTrue(store.load()!!.reshufflePending)
        state.refresh(apps)
        assertEquals(3 * drawsPerShuffle, random.calls)
        assertFalse(store.load()!!.reshufflePending)
    }

    @Test
    fun requestBeforeFirstRefresh_isConsumedByFirstShuffle() {
        store.requestReshuffle()
        state.refresh(apps)
        state.refresh(apps)
        assertEquals(drawsPerShuffle, random.calls)
    }

    @Test
    fun packageChanges_reconcileWithoutReshuffle() {
        val first = state.refresh(apps)
        val callsAfterFirst = random.calls
        val removed = first[2]
        val added = AppEntry("pkg.new", "pkg.new.Main", "New")
        val latest = apps.filter { it.key != removed.key } + added

        val order = state.refresh(latest)

        assertEquals(callsAfterFirst, random.calls)
        assertEquals("survivors keep their relative order", first.filter { it.key != removed.key }, order.dropLast(1))
        assertEquals("new app appended", added, order.last())
        assertPermutation(latest, order)
    }

    @Test
    fun reconcile_usesLatestLabels() {
        val first = state.refresh(apps)
        val renamed = apps.map { if (it.key == first[0].key) it.copy(label = "Renamed") else it }
        assertEquals("Renamed", state.refresh(renamed)[0].label)
    }

    @Test
    fun persistence_newStateOnSameStoreKeepsOrder() {
        val first = state.refresh(apps)
        // A new process: new state object, new Random, same stored data.
        val otherRandom = CountingRandom(seed = 99)
        val restored = WidgetOrderState(store, otherRandom).refresh(apps)
        assertEquals(first, restored)
        assertEquals(0, otherRandom.calls)
    }

    @Test
    fun resolve_withoutSavedOrder_isPermutation() {
        val order = WidgetOrder.resolve(null, apps, Random(1))
        assertPermutation(apps, order)
    }

    @Test
    fun resolve_dropsDuplicatesAndUnknownSavedKeys() {
        val saved = SavedWidgetOrder(listOf("gone/gone.Main", apps[3].key, apps[3].key, apps[0].key), 0, 0)
        val order = WidgetOrder.resolve(saved, apps, Random(1))
        assertEquals(listOf(apps[3], apps[0]), order.take(2))
        assertPermutation(apps, order)
    }

    @Test
    fun emptyCatalog_givesEmptyOrder() {
        assertTrue(state.refresh(emptyList()).isEmpty())
        assertEquals(emptyList<String>(), store.keys)
        assertNull(WidgetOrder.resolve(null, emptyList(), Random(1)).firstOrNull())
    }
}
