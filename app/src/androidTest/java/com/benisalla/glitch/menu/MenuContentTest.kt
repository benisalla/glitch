package com.benisalla.glitch.menu

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
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.benisalla.glitch.R
import com.benisalla.glitch.model.AppEntry
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
        isDefaultHome: Boolean = false,
        showHomeCard: Boolean = false,
        actions: MenuActions = MenuActions(),
    ) {
        composeRule.setContent {
            GlitchTheme(darkTheme = darkTheme) {
                MenuContent(
                    state = state,
                    loadIcon = loadIcon,
                    onAppClick = onAppClick,
                    onRetry = onRetry,
                    isDefaultHome = isDefaultHome,
                    showHomeCard = showHomeCard,
                    actions = actions,
                )
            }
        }
    }

    private val dockApps = listOf(
        AppEntry("pkg.p", "pkg.p.Main", "Phone"),
        AppEntry("pkg.m", "pkg.m.Main", "Messages"),
    )

    private fun inDock(label: String) = hasContentDescription(label) and hasAnyAncestor(hasTestTag(MenuTags.DOCK))

    private fun longPress(label: String) {
        composeRule.onNodeWithContentDescription(label).performTouchInput { longClick() }
        composeRule.waitForIdle()
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

    // --- Dock ---

    @Test
    fun dock_rendersDockAppsBelowGridInGivenOrder() {
        setContent(MenuUiState.Ready(apps = apps, dock = dockApps))

        composeRule.onNodeWithTag(MenuTags.DOCK).assertIsDisplayed()
        composeRule.onNode(inDock("Phone")).assertIsDisplayed().assertHasClickAction()
        composeRule.onNode(inDock("Messages")).assertIsDisplayed()
        // Grid apps are not in the dock.
        composeRule.onNode(inDock("Alpha")).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Alpha").assertIsDisplayed()

        val phone = composeRule.onNode(inDock("Phone")).getUnclippedBoundsInRoot()
        val messages = composeRule.onNode(inDock("Messages")).getUnclippedBoundsInRoot()
        val alpha = composeRule.onNodeWithContentDescription("Alpha").getUnclippedBoundsInRoot()
        assertEquals(phone.top, messages.top)
        assertTrue("dock keeps its order", phone.left < messages.left)
        assertTrue("dock is below the grid", phone.top > alpha.bottom)
    }

    @Test
    fun dock_clickLaunchesExactEntry() {
        val clicked = mutableListOf<AppEntry>()
        setContent(MenuUiState.Ready(apps = apps, dock = dockApps), onAppClick = { clicked += it })
        composeRule.onNode(inDock("Messages")).performClick()
        assertEquals(listOf(dockApps[1]), clicked)
    }

    @Test
    fun dock_hiddenWhenEmpty() {
        setContent(MenuUiState.Ready(apps = apps))
        composeRule.onNodeWithTag(MenuTags.DOCK).assertDoesNotExist()
    }

    // --- Long-press menu ---

    @Test
    fun longPressGridTile_addToDockPassesEntry() {
        val added = mutableListOf<AppEntry>()
        setContent(MenuUiState.Ready(apps = apps), actions = MenuActions(onAddToDock = { added += it }))

        longPress("Bravo")
        composeRule.onNodeWithText(string(R.string.menu_remove_from_dock)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.menu_add_to_dock)).assertIsDisplayed().performClick()

        assertEquals(listOf(apps[1]), added)
        composeRule.onNodeWithText(string(R.string.menu_add_to_dock)).assertDoesNotExist()
    }

    @Test
    fun longPressDockTile_removeFromDockPassesEntry() {
        val removed = mutableListOf<AppEntry>()
        setContent(
            MenuUiState.Ready(apps = apps, dock = dockApps),
            actions = MenuActions(onRemoveFromDock = { removed += it }),
        )

        composeRule.onNode(inDock("Phone")).performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.menu_add_to_dock)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.menu_remove_from_dock)).performClick()

        assertEquals(listOf(dockApps[0]), removed)
    }

    @Test
    fun longPress_appInfoPassesEntry() {
        val info = mutableListOf<AppEntry>()
        setContent(MenuUiState.Ready(apps = apps), actions = MenuActions(onAppInfo = { info += it }))
        longPress("Alpha")
        composeRule.onNodeWithText(string(R.string.menu_app_info)).performClick()
        assertEquals(listOf(apps[0]), info)
    }

    @Test
    fun longPress_tileHasLongClickLabel() {
        setContent(MenuUiState.Ready(apps = apps))
        val hasLongClick = SemanticsMatcher("has long-click action") {
            it.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsActions.OnLongClick) { null }?.label ==
                string(R.string.menu_more_options)
        }
        composeRule.onNodeWithContentDescription("Alpha").assert(hasLongClick)
    }

    @Test
    fun longPress_asHome_offersChangeHomeApp() {
        var changes = 0
        setContent(MenuUiState.Ready(apps = apps), isDefaultHome = true, actions = MenuActions(onChangeHome = { changes++ }))
        longPress("Alpha")
        composeRule.onNodeWithText(string(R.string.home_card_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.menu_change_home)).performClick()
        assertEquals(1, changes)
    }

    @Test
    fun longPress_notHome_offersMakeHome() {
        var requests = 0
        setContent(MenuUiState.Ready(apps = apps), actions = MenuActions(onMakeHome = { requests++ }))
        longPress("Alpha")
        composeRule.onNodeWithText(string(R.string.menu_change_home)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.home_card_title)).performClick()
        assertEquals(1, requests)
    }

    // --- Home card ---

    @Test
    fun homeCard_shownWithActions() {
        var requests = 0
        var dismissals = 0
        setContent(
            MenuUiState.Ready(apps = apps),
            showHomeCard = true,
            actions = MenuActions(onMakeHome = { requests++ }, onDismissHomeCard = { dismissals++ }),
        )
        composeRule.onNodeWithTag(MenuTags.HOME_CARD).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.home_card_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.home_card_action)).performClick()
        composeRule.onNodeWithText(string(R.string.home_card_dismiss)).performClick()
        assertEquals(1, requests)
        assertEquals(1, dismissals)
    }

    @Test
    fun homeCard_hiddenByDefault() {
        setContent(MenuUiState.Ready(apps = apps))
        composeRule.onNodeWithTag(MenuTags.HOME_CARD).assertDoesNotExist()
    }
}
