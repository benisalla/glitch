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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benisalla.shufflemenu.model.AppEntry

private val IconSize = 48.dp

@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    loadIcon: suspend (AppEntry) -> Drawable?,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    Scaffold(
        modifier = modifier,
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
            CircularProgressIndicator()
        }
        MenuUiState.Empty -> CenteredBox(contentPadding, modifier) {
            Text("No apps found", style = MaterialTheme.typography.bodyLarge)
        }
        is MenuUiState.Error -> CenteredBox(contentPadding, modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.message, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
        is MenuUiState.Ready -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 88.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding.plusAll(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.apps, key = { it.key }) { entry ->
                AppCell(entry = entry, loadIcon = loadIcon, onClick = { onAppClick(entry) })
            }
        }
    }
}

@Composable
private fun AppCell(
    entry: AppEntry,
    loadIcon: suspend (AppEntry) -> Drawable?,
    onClick: () -> Unit,
) {
    val iconPx = with(LocalDensity.current) { IconSize.roundToPx() }
    val icon by produceState<ImageBitmap?>(initialValue = null, entry.key) {
        value = loadIcon(entry)?.toBitmap(iconPx, iconPx)?.asImageBitmap()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            // The full label is announced even when the visible one is ellipsized.
            .semantics(mergeDescendants = true) { contentDescription = entry.label }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(IconSize))
        } else {
            Box(
                Modifier
                    .size(IconSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry.label,
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
