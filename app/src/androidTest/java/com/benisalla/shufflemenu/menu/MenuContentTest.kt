package com.benisalla.shufflemenu.menu

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benisalla.shufflemenu.model.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val apps = listOf(
        AppEntry("pkg.a", "pkg.a.Main", "Alpha"),
        AppEntry("pkg.b", "pkg.b.Main", "Bravo"),
    )

    private fun setContent(state: MenuUiState, onAppClick: (AppEntry) -> Unit = {}, onRetry: () -> Unit = {}) {
        composeRule.setContent {
            ShuffleMenuTheme {
                MenuContent(state = state, loadIcon = { null }, onAppClick = onAppClick, onRetry = onRetry)
            }
        }
    }

    @Test
    fun ready_rendersLabelsAndClickPassesExactEntry() {
        val clicked = mutableListOf<AppEntry>()
        setContent(MenuUiState.Ready(apps), onAppClick = { clicked += it })

        // The label must be on the clickable node itself, so TalkBack announces the tap target.
        composeRule.onNodeWithContentDescription("Alpha").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithContentDescription("Bravo").assertIsDisplayed().assertHasClickAction().performClick()

        assertEquals(listOf(apps[1]), clicked)
    }

    @Test
    fun empty_showsMessage() {
        setContent(MenuUiState.Empty)
        composeRule.onNodeWithText("No apps found").assertIsDisplayed()
    }

    @Test
    fun error_showsMessageAndRetry() {
        var retries = 0
        setContent(MenuUiState.Error("Couldn't load apps"), onRetry = { retries++ })

        composeRule.onNodeWithText("Couldn't load apps").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }
}
