package com.benisalla.shufflemenu.menu

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.benisalla.shufflemenu.R
import com.benisalla.shufflemenu.model.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun string(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(
        state: MenuUiState,
        darkTheme: Boolean = false,
        onAppClick: (AppEntry) -> Unit = {},
        onRetry: () -> Unit = {},
        loadIcon: suspend (AppEntry) -> Drawable? = { null },
    ) {
        composeRule.setContent {
            ShuffleMenuTheme(darkTheme = darkTheme) {
                MenuContent(state = state, loadIcon = loadIcon, onAppClick = onAppClick, onRetry = onRetry)
            }
        }
    }

    @Test
    fun loading_showsAccessibleProgress() {
        setContent(MenuUiState.Loading)
        composeRule.onNodeWithContentDescription(string(R.string.menu_loading)).assertIsDisplayed()
    }

    @Test
    fun ready_rendersLabelsAndClickPassesExactEntry() {
        val clicked = mutableListOf<AppEntry>()
        setContent(MenuUiState.Ready(apps), onAppClick = { clicked += it })

        // The label must be on the clickable node itself, so TalkBack announces the tap target.
        val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        composeRule.onNodeWithContentDescription("Alpha").assertIsDisplayed().assertHasClickAction().assert(isButton)
        composeRule.onNodeWithContentDescription("Bravo").assertIsDisplayed().assertHasClickAction().assert(isButton)
            .performClick()

        assertEquals(listOf(apps[1]), clicked)
    }

    @Test
    fun ready_brokenIcons_keepMenuAndLabels() {
        val entries = apps + AppEntry("pkg.c", "pkg.c.Main", "Charlie")
        // Alpha's loader throws, Bravo's drawable throws while drawing, Charlie's icon is fine.
        val brokenDrawable = object : ColorDrawable(Color.RED) {
            override fun draw(canvas: Canvas): Unit = throw IllegalStateException("broken icon")
        }
        setContent(MenuUiState.Ready(entries), loadIcon = { entry ->
            when (entry.packageName) {
                "pkg.a" -> throw IllegalStateException("broken loader")
                "pkg.b" -> brokenDrawable
                else -> ColorDrawable(Color.BLUE)
            }
        })

        composeRule.waitForIdle()
        // The tile node carries the label (the visible Text is merged into it).
        for (label in listOf("Alpha", "Bravo", "Charlie")) {
            composeRule.onNodeWithContentDescription(label).assertIsDisplayed().assertHasClickAction()
        }
    }

    @Test
    fun ready_showsTitleAsHeading() {
        setContent(MenuUiState.Ready(apps))
        composeRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed().assert(isHeading())
    }

    @Test
    fun ready_rendersAppsInGivenOrder() {
        val reversed = apps.reversed()
        setContent(MenuUiState.Ready(reversed))

        val first = composeRule.onNodeWithContentDescription(reversed[0].label).getUnclippedBoundsInRoot()
        val second = composeRule.onNodeWithContentDescription(reversed[1].label).getUnclippedBoundsInRoot()
        // Same row, first entry on the left (test runs LTR).
        assertEquals(first.top, second.top)
        assertTrue(first.left < second.left)
    }

    @Test
    fun ready_tilesMeetMinimumTouchTarget() {
        setContent(MenuUiState.Ready(apps))
        composeRule.onNodeWithContentDescription("Alpha")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun ready_darkTheme_rendersLabels() {
        setContent(MenuUiState.Ready(apps), darkTheme = true)
        composeRule.onNodeWithContentDescription("Alpha").assertIsDisplayed()
    }

    @Test
    fun empty_showsMessage() {
        setContent(MenuUiState.Empty)
        composeRule.onNodeWithText(string(R.string.menu_empty)).assertIsDisplayed()
    }

    @Test
    fun error_showsMessageAndRetry() {
        var retries = 0
        setContent(MenuUiState.Error, onRetry = { retries++ })

        composeRule.onNodeWithText(string(R.string.menu_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.menu_retry)).assertHasClickAction().performClick()

        assertEquals(1, retries)
    }
}
