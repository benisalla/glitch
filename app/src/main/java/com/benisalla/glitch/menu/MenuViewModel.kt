package com.benisalla.glitch.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.benisalla.glitch.model.AppCatalog
import com.benisalla.glitch.model.AppEntry
import com.benisalla.glitch.model.AppLauncher
import com.benisalla.glitch.model.LaunchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface MenuUiState {
    data object Loading : MenuUiState
    data object Empty : MenuUiState
    /** The catalog could not be read. The UI shows a plain message and a retry button. */
    data object Error : MenuUiState

    /**
     * [apps] is the shuffled grid and [dock] the fixed dock; no app is in both. [shuffleId]
     * changes with every new order, so the UI can scroll back to the top.
     */
    data class Ready(
        val apps: List<AppEntry>,
        val dock: List<AppEntry> = emptyList(),
        val shuffleId: Int = 0,
    ) : MenuUiState
}

/** One-shot message for the user. The UI turns it into localized text. */
sealed interface MenuMessage {
    /** The app was uninstalled or disabled since the menu was loaded. */
    data class Unavailable(val label: String) : MenuMessage

    /** The app exists but could not be started. */
    data class LaunchFailed(val label: String) : MenuMessage

    /** The dock already holds [Dock.MAX_APPS] apps. */
    data object DockFull : MenuMessage
}

/**
 * Owns the menu order and the dock for one "session": the time between the menu becoming visible
 * and the user actually leaving it. The order is shuffled exactly once per session and otherwise
 * only reconciled (never reshuffled) when the installed apps change. Because the ViewModel
 * survives configuration changes, rotation keeps the same order.
 */
class MenuViewModel(
    private val catalog: AppCatalog,
    private val launcher: AppLauncher,
    private val store: MenuStore,
    private val random: Random = Random.Default,
    /** The system default apps for the first-run dock, one candidate list per dock slot. */
    private val defaultDockApps: suspend () -> List<List<DefaultApp>> = { emptyList() },
) : ViewModel() {

    private val _state = MutableStateFlow<MenuUiState>(MenuUiState.Loading)
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    /** One-shot user message (e.g. a failed launch); call [consumeMessage] once shown. */
    private val _message = MutableStateFlow<MenuMessage?>(null)
    val message: StateFlow<MenuMessage?> = _message.asStateFlow()

    /** Every launchable app in the current shuffled order, dock apps included; null before the first load. */
    private var order: List<AppEntry>? = null
    /** Dock keys; null until loaded (or chosen from the defaults) on the first catalog load. */
    private var dockKeys: List<String>? = null
    /**
     * True once the dock is persisted: the user edited it, or the first-run defaults resolved to at
     * least one app. Until then the defaults are looked up again on every load, so a lookup that
     * found nothing (e.g. right after phone setup) is retried. A dock the user emptied is persisted
     * as empty and stays empty.
     */
    private var dockPersisted = false
    private var shuffleId = 0
    private var sessionActive = false
    private var needsShuffle = false
    private var loadJob: Job? = null
    private var changesJob: Job? = null

    /** Call from Activity.onStart. Starts a new session (one shuffle) unless returning from a config change. */
    fun onMenuStarted() {
        if (!sessionActive) {
            sessionActive = true
            startNewOrder()
        }
        if (changesJob == null) {
            changesJob = viewModelScope.launch {
                catalog.changes().collect { refresh() }
            }
        }
    }

    /** Call from Activity.onStop. A real stop (not a config change) ends the session. */
    fun onMenuStopped(changingConfigurations: Boolean) {
        changesJob?.cancel()
        changesJob = null
        if (!changingConfigurations) sessionActive = false
    }

    /**
     * Call when Glitch is launched again (from its icon or another app) while another app covered
     * it but Android had not yet delivered onStop, which can lag on a busy device. That is a newly
     * opened menu, so it starts a new session.
     */
    fun onMenuReopenedBeforeStop() {
        startNewOrder()
    }

    fun retry() {
        if (_state.value !is MenuUiState.Ready) _state.value = MenuUiState.Loading
        refresh()
    }

    fun launch(entry: AppEntry) {
        when (launcher.launch(entry)) {
            LaunchResult.Launched -> Unit
            LaunchResult.Unavailable -> {
                _message.value = MenuMessage.Unavailable(entry.label)
                refresh()
            }
            // The technical reason is not useful to the user; the launcher may log it.
            is LaunchResult.Failed -> _message.value = MenuMessage.LaunchFailed(entry.label)
        }
    }

    /** Moves [entry] from the grid to the end of the dock, or says the dock is full. */
    fun addToDock(entry: AppEntry) {
        val updated = Dock.withAdded(visibleDockKeys(), entry.key)
        if (updated == null) {
            _message.value = MenuMessage.DockFull
            return
        }
        saveDock(updated)
    }

    /** Moves [entry] back to the grid, at its place in the current order (no reshuffle). */
    fun removeFromDock(entry: AppEntry) {
        saveDock(Dock.withRemoved(visibleDockKeys(), entry.key))
    }

    fun consumeMessage() {
        _message.value = null
    }

    /** Only installed apps count, so an uninstalled dock app frees its place. */
    private fun visibleDockKeys(): List<String> {
        val current = order ?: return dockKeys.orEmpty()
        return Dock.partition(current, dockKeys.orEmpty()).dock.map { it.key }
    }

    private fun startNewOrder() {
        needsShuffle = true
        _state.value = MenuUiState.Loading
        refresh()
    }

    private fun saveDock(keys: List<String>) {
        dockKeys = keys
        dockPersisted = true
        store.saveDock(keys)
        publish()
    }

    /**
     * Reloads the catalog. Shuffles if this session has no order yet; otherwise reconciles against
     * the current order. A newer refresh cancels an older one still in flight.
     */
    private fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val latest = try {
                catalog.loadApps()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (needsShuffle || order == null) _state.value = MenuUiState.Error
                return@launch
            }
            if (!dockPersisted) dockKeys = loadOrResolveDock(latest)
            val current = order
            order = if (needsShuffle || current == null) {
                shuffleId++
                MenuOrder.shuffled(latest, random)
            } else {
                MenuOrder.reconcile(current, latest)
            }
            needsShuffle = false
            publish()
        }
    }

    /** The saved dock or, if none was ever saved, the system default apps (see [dockPersisted]). */
    private suspend fun loadOrResolveDock(apps: List<AppEntry>): List<String> {
        store.loadDock()?.let {
            dockPersisted = true
            return it
        }
        val slots = try {
            defaultDockApps()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
        val defaults = Dock.defaults(slots, apps)
        if (defaults.isNotEmpty()) {
            store.saveDock(defaults)
            dockPersisted = true
        }
        return defaults
    }

    private fun publish() {
        val all = order ?: return
        if (all.isEmpty()) {
            _state.value = MenuUiState.Empty
            return
        }
        val layout = Dock.partition(all, dockKeys.orEmpty())
        _state.value = MenuUiState.Ready(apps = layout.grid, dock = layout.dock, shuffleId = shuffleId)
    }

    companion object {
        fun factory(
            catalog: AppCatalog,
            launcher: AppLauncher,
            store: MenuStore,
            defaultDockApps: suspend () -> List<List<DefaultApp>>,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MenuViewModel(catalog, launcher, store, defaultDockApps = defaultDockApps)
            }
        }
    }
}
