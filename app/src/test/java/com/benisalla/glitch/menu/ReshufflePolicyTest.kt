package com.benisalla.glitch.menu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReshufflePolicyTest {

    @Test
    fun noOrderYet_alwaysShuffles() {
        for (home in listOf(true, false)) {
            for (newSession in listOf(true, false)) {
                assertTrue(ReshufflePolicy.shuffleOnStart(hasOrder = false, newSession = newSession, isDefaultHome = home))
            }
        }
    }

    @Test
    fun normalApp_newlyOpenedMenuShuffles() {
        assertTrue(ReshufflePolicy.shuffleOnStart(hasOrder = true, newSession = true, isDefaultHome = false))
    }

    @Test
    fun configChange_neverShuffles() {
        for (home in listOf(true, false)) {
            assertFalse(ReshufflePolicy.shuffleOnStart(hasOrder = true, newSession = false, isDefaultHome = home))
        }
    }

    @Test
    fun home_returnFromAppDoesNotShuffle() {
        assertFalse(ReshufflePolicy.shuffleOnStart(hasOrder = true, newSession = true, isDefaultHome = true))
    }

    @Test
    fun screenOff_shufflesOnlyAsHome() {
        assertTrue(ReshufflePolicy.shuffleOnScreenOff(isDefaultHome = true))
        assertFalse(ReshufflePolicy.shuffleOnScreenOff(isDefaultHome = false))
    }
}
