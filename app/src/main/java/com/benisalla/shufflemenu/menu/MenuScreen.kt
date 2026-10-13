package com.benisalla.shufflemenu.menu

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benisalla.shufflemenu.R
import com.benisalla.shufflemenu.model.AppEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val IconSize = 52.dp
private val IconShape = RoundedCornerShape(14.dp)
private val TileShape = RoundedCornerShape(16.dp)
private val MinTileWidth = 92.dp
private val GridPadding = 12.dp

@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    loadIcon: suspend (AppEntry) -> Drawable?,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = message?.let { messageText(it) }
    LaunchedEffect(message) {
        val text = messageText ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
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
        )
    }
}

@Composable
private fun messageText(message: MenuMessage): String = when (message) {
    is MenuMessage.Unavailable -> stringResource(R.string.menu_app_unavailable, message.label)
    is MenuMessage.LaunchFailed -> stringResource(R.string.menu_app_launch_failed, message.label)
}

/** Stateless menu body. Renders apps in exactly the order given; it never reorders. */
@Composable
fun MenuContent(
    state: MenuUiState,
    loadIcon: suspend (AppEntry) -> Drawable?,
    onAppClick: (AppEntry) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        MenuUiState.Loading -> CenteredBox(contentPadding, modifier) {
            val loading = stringResource(R.string.menu_loading)
            CircularProgressIndicator(Modifier.semantics { contentDescription = loading })
        }
        MenuUiState.Empty -> CenteredBox(contentPadding, modifier) {
            StatusText(stringResource(R.string.menu_empty))
        }
        MenuUiState.Error -> CenteredBox(contentPadding, modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatusText(stringResource(R.string.menu_error))
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.menu_retry)) }
            }
        }
        is MenuUiState.Ready -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = MinTileWidth),
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding.plusAll(GridPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = HeaderKey, span = { GridItemSpan(maxLineSpan) }) { MenuTitle() }
            items(state.apps, key = { it.key }) { entry ->
                AppTile(entry = entry, loadIcon = loadIcon, onClick = { onAppClick(entry) })
            }
        }
    }
}

/** Grid key of the title row; cannot collide with app keys, which always contain a '/'. */
private const val HeaderKey = "header"

@Composable
private fun MenuTitle() {
    Text(
        text = stringResource(R.string.app_name),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AppTile(
    entry: AppEntry,
    loadIcon: suspend (AppEntry) -> Drawable?,
    onClick: () -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clip(TileShape)
            .clickable(role = Role.Button, onClick = onClick)
            // One node for TalkBack, announced with the full label even when the visible one is cut.
            .semantics(mergeDescendants = true) { contentDescription = entry.label }
            .padding(horizontal = 4.dp, vertical = 10.dp),
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
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

@Composable
private fun PaddingValues.plusAll(extra: Dp): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection) + extra,
        top = calculateTopPadding() + extra,
        end = calculateEndPadding(layoutDirection) + extra,
        bottom = calculateBottomPadding() + extra,
    )
}
