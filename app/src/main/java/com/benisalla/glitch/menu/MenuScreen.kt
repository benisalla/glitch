package com.benisalla.glitch.menu

import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benisalla.glitch.R
import com.benisalla.glitch.model.AppEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val IconSize = 52.dp
private val IconShape = RoundedCornerShape(14.dp)
private val TileShape = RoundedCornerShape(16.dp)
private val DockShape = RoundedCornerShape(28.dp)
private val MinTileWidth = 92.dp
private val GridPadding = 12.dp

/** Test tags, also exposed as resource ids for UI Automator scripts. */
object MenuTags {
    const val GRID_TILE = "app_tile"
    const val DOCK_TILE = "dock_tile"
    const val DOCK = "dock"
    const val HOME_CARD = "home_card"
    const val HEADER_MENU = "header_menu"
}

/** One entry of the header's More options (⋮) menu. */
class HeaderMenuItem(@StringRes val label: Int, val onClick: () -> Unit)

/**
 * Entries of the header's More options (⋮) menu, top to bottom. It is always reachable (Ready,
 * Empty and Error states), so app-wide actions belong here; later features add their entry to
 * this list.
 */
fun headerMenuItems(isDefaultHome: Boolean, actions: MenuActions): List<HeaderMenuItem> = listOf(
    // The system decides the default home app; this is the visible way in and back out.
    if (isDefaultHome) {
        HeaderMenuItem(R.string.menu_change_home, actions.onChangeHome)
    } else {
        HeaderMenuItem(R.string.home_card_title, actions.onMakeHome)
    },
)

/** What the long-press menu and the home card can do, besides opening an app. */
class MenuActions(
    val onAddToDock: (AppEntry) -> Unit = {},
    val onRemoveFromDock: (AppEntry) -> Unit = {},
    val onAppInfo: (AppEntry) -> Unit = {},
    val onMakeHome: () -> Unit = {},
    val onChangeHome: () -> Unit = {},
    val onDismissHomeCard: () -> Unit = {},
)

@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    loadIcon: suspend (AppEntry) -> Drawable?,
    scrollToTopSignal: Int,
    onAppInfo: (AppEntry) -> Unit,
    onMakeHome: () -> Unit,
    onChangeHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isDefaultHome by viewModel.isDefaultHome.collectAsStateWithLifecycle()
    val homeCardDismissed by viewModel.homeCardDismissed.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = message?.let { messageText(it) }
    LaunchedEffect(message) {
        val text = messageText ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    val actions = remember(viewModel, onAppInfo, onMakeHome, onChangeHome) {
        MenuActions(
            onAddToDock = viewModel::addToDock,
            onRemoveFromDock = viewModel::removeFromDock,
            onAppInfo = onAppInfo,
            onMakeHome = onMakeHome,
            onChangeHome = onChangeHome,
            onDismissHomeCard = viewModel::dismissHomeCard,
        )
    }

    Scaffold(
        modifier = modifier,
        // Edge-to-edge: keep content clear of status/navigation bars and display cutouts.
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        MenuContent(
            state = state,
            loadIcon = loadIcon,
            onAppClick = viewModel::launch,
            onRetry = viewModel::retry,
            contentPadding = innerPadding,
            isDefaultHome = isDefaultHome,
            showHomeCard = !isDefaultHome && !homeCardDismissed,
            actions = actions,
            scrollToTopSignal = scrollToTopSignal,
        )
    }
}

@Composable
private fun messageText(message: MenuMessage): String = when (message) {
    is MenuMessage.Unavailable -> stringResource(R.string.menu_app_unavailable, message.label)
    is MenuMessage.LaunchFailed -> stringResource(R.string.menu_app_launch_failed, message.label)
    MenuMessage.DockFull -> stringResource(R.string.menu_dock_full)
}

/**
 * Stateless menu body. Renders the grid and the dock in exactly the order given; it never reorders.
 * The grid scrolls back to the top on a new order ([MenuUiState.Ready.shuffleId]) and whenever
 * [scrollToTopSignal] changes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MenuContent(
    state: MenuUiState,
    loadIcon: suspend (AppEntry) -> Drawable?,
    onAppClick: (AppEntry) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    isDefaultHome: Boolean = false,
    showHomeCard: Boolean = false,
    actions: MenuActions = MenuActions(),
    scrollToTopSignal: Int = 0,
) {
    val gridState = rememberLazyGridState()
    ScrollToTopEffects(gridState, (state as? MenuUiState.Ready)?.shuffleId, scrollToTopSignal)

    when (state) {
        MenuUiState.Loading -> CenteredBox(contentPadding, modifier) {
            val loading = stringResource(R.string.menu_loading)
            CircularProgressIndicator(Modifier.semantics { contentDescription = loading })
        }
        // Empty and Error keep the header, so its menu (e.g. "Change home app") is never out of reach.
        MenuUiState.Empty -> StatusLayout(contentPadding, modifier, isDefaultHome, actions) {
            StatusText(stringResource(R.string.menu_empty))
        }
        MenuUiState.Error -> StatusLayout(contentPadding, modifier, isDefaultHome, actions) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatusText(stringResource(R.string.menu_error))
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.menu_retry)) }
            }
        }
        is MenuUiState.Ready -> Column(modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
            val hasDock = state.dock.isNotEmpty()
            val layoutDirection = LocalLayoutDirection.current
            val start = contentPadding.calculateStartPadding(layoutDirection)
            val end = contentPadding.calculateEndPadding(layoutDirection)
            val bottom = contentPadding.calculateBottomPadding()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = MinTileWidth),
                state = gridState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = start + GridPadding,
                    top = contentPadding.calculateTopPadding() + GridPadding,
                    end = end + GridPadding,
                    // With a dock, the dock sits above the navigation bar instead of the grid.
                    bottom = GridPadding + if (hasDock) 0.dp else bottom,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = HeaderKey, span = { GridItemSpan(maxLineSpan) }) {
                    MenuHeader(isDefaultHome = isDefaultHome, actions = actions, modifier = Modifier.padding(bottom = 4.dp))
                }
                if (showHomeCard) {
                    item(key = HomeCardKey, span = { GridItemSpan(maxLineSpan) }) {
                        HomeCard(onMakeHome = actions.onMakeHome, onDismiss = actions.onDismissHomeCard)
                    }
                }
                items(state.apps, key = { it.key }) { entry ->
                    AppTile(
                        entry = entry,
                        inDock = false,
                        isDefaultHome = isDefaultHome,
                        loadIcon = loadIcon,
                        onClick = { onAppClick(entry) },
                        actions = actions,
                    )
                }
            }
            if (hasDock) {
                DockRow(
                    dock = state.dock,
                    isDefaultHome = isDefaultHome,
                    loadIcon = loadIcon,
                    onAppClick = onAppClick,
                    actions = actions,
                    modifier = Modifier.padding(start = start + GridPadding, end = end + GridPadding, bottom = bottom + 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ScrollToTopEffects(gridState: LazyGridState, shuffleId: Int?, scrollToTopSignal: Int) {
    // Saved across configuration changes, so rotation does not count as a new order.
    var seenShuffleId by rememberSaveable { mutableStateOf(shuffleId) }
    LaunchedEffect(shuffleId) {
        if (shuffleId != null && shuffleId != seenShuffleId) {
            seenShuffleId = shuffleId
            gridState.scrollToItem(0)
        }
    }
    var seenSignal by rememberSaveable { mutableIntStateOf(scrollToTopSignal) }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal != seenSignal) {
            seenSignal = scrollToTopSignal
            gridState.animateScrollToItem(0)
        }
    }
}

/** Grid keys of the title row and home card; cannot collide with app keys, which always contain a '/'. */
private const val HeaderKey = "header"
private const val HomeCardKey = "home_card"

/** The "Glitch" title with the More options (⋮) menu at the end of the row. */
@Composable
private fun MenuHeader(isDefaultHome: Boolean, actions: MenuActions, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 8.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HeaderMenu(headerMenuItems(isDefaultHome, actions))
    }
}

@Composable
private fun HeaderMenu(items: List<HeaderMenuItem>) {
    var open by remember { mutableStateOf(false) }
    val moreOptions = stringResource(R.string.menu_more_options)
    Box {
        IconButton(
            onClick = { open = true },
            // Explicit 48 dp: the minimum touch target, whatever the IconButton default is.
            modifier = Modifier
                .size(48.dp)
                .testTag(MenuTags.HEADER_MENU)
                .semantics { contentDescription = moreOptions },
        ) {
            // A vertical ellipsis drawn in place, so no icon library is needed.
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { item -> MenuItem(stringResource(item.label), { open = false }, item.onClick) }
        }
    }
}

@Composable
private fun HomeCard(onMakeHome: () -> Unit, onDismiss: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag(MenuTags.HOME_CARD),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.home_card_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_card_body),
                modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_card_dismiss)) }
                TextButton(onClick = onMakeHome) { Text(stringResource(R.string.home_card_action)) }
            }
        }
    }
}

@Composable
private fun DockRow(
    dock: List<AppEntry>,
    isDefaultHome: Boolean,
    loadIcon: suspend (AppEntry) -> Drawable?,
    onAppClick: (AppEntry) -> Unit,
    actions: MenuActions,
    modifier: Modifier = Modifier,
) {
    val dockLabel = stringResource(R.string.menu_dock)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MenuTags.DOCK)
            .semantics { paneTitle = dockLabel },
        shape = DockShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            // Fixed-width slots in the user's order; nothing here is ever shuffled.
            dock.forEach { entry ->
                Box(Modifier.weight(1f)) {
                    AppTile(
                        entry = entry,
                        inDock = true,
                        isDefaultHome = isDefaultHome,
                        loadIcon = loadIcon,
                        onClick = { onAppClick(entry) },
                        actions = actions,
                    )
                }
            }
            repeat(Dock.MAX_APPS - dock.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun AppTile(
    entry: AppEntry,
    inDock: Boolean,
    isDefaultHome: Boolean,
    loadIcon: suspend (AppEntry) -> Drawable?,
    onClick: () -> Unit,
    actions: MenuActions,
) {
    val iconPx = with(LocalDensity.current) { IconSize.roundToPx() }
    val icon by produceState<ImageBitmap?>(initialValue = null, entry.key, iconPx) {
        // A third-party app's broken icon must not crash the whole menu: on failure the
        // tile keeps its placeholder. Cancellation (tile scrolled away) still propagates.
        value = try {
            val drawable = loadIcon(entry) ?: return@produceState
            // Rasterize off the main thread so scrolling a long grid stays smooth.
            withContext(Dispatchers.Default) { drawable.toBitmap(iconPx, iconPx).asImageBitmap() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val moreOptions = stringResource(R.string.menu_more_options)

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp)
                .clip(TileShape)
                .testTag(if (inDock) MenuTags.DOCK_TILE else MenuTags.GRID_TILE)
                // One node for TalkBack (and UI Automator) carrying the tag, the full label (even
                // when the visible one is cut) and the click actions.
                .semantics { contentDescription = entry.label }
                .combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = moreOptions,
                    onLongClick = { menuOpen = true },
                    onClick = onClick,
                )
                .padding(horizontal = 4.dp, vertical = if (inDock) 8.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val bitmap = icon
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(IconSize))
            } else {
                // Same-size placeholder so tiles do not jump when icons arrive.
                Box(
                    Modifier
                        .size(IconSize)
                        .clip(IconShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = entry.label,
                modifier = Modifier.clearAndSetSemantics {},
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = if (inDock) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            val close = { menuOpen = false }
            if (inDock) {
                MenuItem(stringResource(R.string.menu_remove_from_dock), close) { actions.onRemoveFromDock(entry) }
            } else {
                MenuItem(stringResource(R.string.menu_add_to_dock), close) { actions.onAddToDock(entry) }
            }
            MenuItem(stringResource(R.string.menu_app_info), close) { actions.onAppInfo(entry) }
            // A quiet way in, and back out: the system decides the default home app.
            if (isDefaultHome) {
                MenuItem(stringResource(R.string.menu_change_home), close, actions.onChangeHome)
            } else {
                MenuItem(stringResource(R.string.home_card_title), close, actions.onMakeHome)
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, close: () -> Unit, action: () -> Unit) {
    DropdownMenuItem(text = { Text(text) }, onClick = { close(); action() })
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CenteredBox(
    contentPadding: PaddingValues,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Header at the top, a status message centered below it. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun StatusLayout(
    contentPadding: PaddingValues,
    modifier: Modifier,
    isDefaultHome: Boolean,
    actions: MenuActions,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(contentPadding)
            .semantics { testTagsAsResourceId = true },
    ) {
        MenuHeader(
            isDefaultHome = isDefaultHome,
            actions = actions,
            modifier = Modifier.padding(start = GridPadding, end = GridPadding, top = GridPadding),
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
