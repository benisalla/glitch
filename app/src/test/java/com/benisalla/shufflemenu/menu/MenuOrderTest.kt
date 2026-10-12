package com.benisalla.shufflemenu.menu

import com.benisalla.shufflemenu.model.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MenuOrderTest {

    private fun entry(id: String, label: String = "App $id") = AppEntry("pkg.$id", "pkg.$id.Main", label)

    private fun entries(n: Int) = (0 until n).map { entry("$it") }

    @Test
    fun shuffled_isPermutationOfUnchangedEntries() {
        for (size in listOf(0, 1, 2, 3, 10, 50)) {
            val input = entries(size)
            for (seed in 0 until 200) {
                val output = MenuOrder.shuffled(input, Random(seed))
                assertEquals(size, output.size)
                assertEquals(size, output.map { it.key }.toSet().size)
                val inputByKey = input.associateBy { it.key }
                output.forEach { assertSame(inputByKey.getValue(it.key), it) }
            }
        }
    }

    @Test
    fun shuffled_doesNotMutateInput() {
        val input = entries(5)
        val copy = input.toList()
        MenuOrder.shuffled(input, Random(7))
        assertEquals(copy, input)
    }

    @Test
    fun shuffled_producesManyDistinctOrdersAcrossSessions() {
        // Deterministic for a fixed seed: 4 entries have 24 orders; 500 draws from a
        // uniform shuffle cover essentially all of them.
        val input = entries(4)
        val random = Random(1234)
        val seen = HashSet<List<String>>()
        repeat(500) { seen += MenuOrder.shuffled(input, random).map { it.key } }
        assertTrue("only ${seen.size} distinct orders", seen.size >= 20)
    }

    @Test
    fun shuffled_firstPositionIsRoughlyUniform() {
        val input = entries(4)
        val random = Random(99)
        val counts = IntArray(4)
        repeat(4000) { counts[input.indexOf(MenuOrder.shuffled(input, random).first())]++ }
        counts.forEach { assertTrue("counts=${counts.toList()}", it in 800..1200) }
    }

    @Test
    fun reconcile_keepsOrderDropsRemovedAppendsNew() {
        val a = entry("a"); val b = entry("b"); val c = entry("c"); val d = entry("d"); val e = entry("e")
        val current = listOf(c, a, d, b)
        val latest = listOf(a, b, c, e) // d removed, e added
        assertEquals(listOf(c, a, b, e), MenuOrder.reconcile(current, latest))
    }

    @Test
    fun reconcile_appendsMultipleNewInLatestOrder() {
        val a = entry("a"); val x = entry("x"); val y = entry("y")
        assertEquals(listOf(a, y, x), MenuOrder.reconcile(listOf(a), listOf(y, a, x)))
    }

    @Test
    fun reconcile_usesLatestDataForSurvivors() {
        val old = entry("a", label = "Old")
        val renamed = entry("a", label = "New")
        val b = entry("b")
        val result = MenuOrder.reconcile(listOf(b, old), listOf(renamed, b))
        assertEquals(listOf(b, renamed), result)
        assertEquals("New", result[1].label)
    }

    @Test
    fun reconcile_neverDuplicates() {
        val a = entry("a"); val b = entry("b")
        val result = MenuOrder.reconcile(listOf(a, b, a), listOf(b, a, b))
        assertEquals(listOf(a, b), result)
    }

    @Test
    fun reconcile_emptyCases() {
        val a = entry("a"); val b = entry("b")
        assertEquals(emptyList<AppEntry>(), MenuOrder.reconcile(emptyList(), emptyList()))
        assertEquals(emptyList<AppEntry>(), MenuOrder.reconcile(listOf(a, b), emptyList()))
        assertEquals(listOf(b, a), MenuOrder.reconcile(emptyList(), listOf(b, a)))
    }
}
