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
 * Owns the menu order and the dock. When a new order is drawn is decided by [ReshufflePolicy]:
 * once per newly opened menu as a normal app, once per unlock (screen off) as the default home
 * screen. Otherwise the order is only reconciled (never reshuffled) when the installed apps
 * change. Because the ViewModel survives configuration changes, rotation keeps the same order.
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

    private val _isDefaultHome = MutableStateFlow(false)
    val isDefaultHome: StateFlow<Boolean> = _isDefaultHome.asStateFlow()

    private val _homeCardDismissed = MutableStateFlow(store.isHomeCardDismissed())
    val homeCardDismissed: StateFlow<Boolean> = _homeCardDismissed.asStateFlow()

    /** Every launchable app in the current shuffled order, dock apps included; null before the first load. */
    private var order: List<AppEntry>? = null
    /** Dock keys; null until loaded (or chosen from the defaults) on the first catalog load. */
    private var dockKeys: List<String>? = null
    private var shuffleId = 0
    private var sessionActive = false
    private var needsShuffle = false
    private var loadJob: Job? = null
    private var changesJob: Job? = null

    /** Call from Activity.onStart (after [setDefaultHome]) and onResume with the current value. */
    fun setDefaultHome(isDefaultHome: Boolean) {
        _isDefaultHome.value = isDefaultHome
    }

    /** Call from Activity.onStart. Reloads the apps; shuffles when [ReshufflePolicy] says so. */
    fun onMenuStarted() {
        val newSession = !sessionActive
        if (newSession) {
            sessionActive = true
            if (ReshufflePolicy.shuffleOnStart(order != null, newSession, _isDefaultHome.value)) {
                needsShuffle = true
                _state.value = MenuUiState.Loading
            }
            // Picks up apps installed or removed while stopped; reconciles without reshuffling.
            refresh()
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

    /** The screen turned off. As the home screen, draw the next order now, ready for the unlock. */
    fun onScreenOff() {
        if (!ReshufflePolicy.shuffleOnScreenOff(_isDefaultHome.value)) return
        val current = order
        if (current == null) {
            needsShuffle = true
        } else {
            order = MenuOrder.shuffled(current, random)
            shuffleId++
            publish()
        }
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

    fun dismissHomeCard() {
        store.dismissHomeCard()
        _homeCardDismissed.value = true
    }

    fun consumeMessage() {
        _message.value = null
    }

    /** Only installed apps count, so an uninstalled dock app frees its place. */
    private fun visibleDockKeys(): List<String> {
        val current = order ?: return dockKeys.orEmpty()
        return Dock.partition(current, dockKeys.orEmpty()).dock.map { it.key }
    }

    private fun saveDock(keys: List<String>) {
        dockKeys = keys
        store.saveDock(keys)
        publish()
    }

    /**
     * Reloads the catalog. Shuffles if a new order is due; otherwise reconciles against the
     * current order. A newer refresh cancels an older one still in flight.
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
            if (dockKeys == null) dockKeys = store.loadDock() ?: firstRunDock(latest)
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

    private suspend fun firstRunDock(apps: List<AppEntry>): List<String> {
        val slots = try {
            defaultDockApps()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
        return Dock.defaults(slots, apps).also { store.saveDock(it) }
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
