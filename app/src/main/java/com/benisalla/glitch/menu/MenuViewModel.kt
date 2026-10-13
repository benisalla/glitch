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
    data class Ready(val apps: List<AppEntry>) : MenuUiState
}

/** One-shot message for the user. The UI turns it into localized text. */
sealed interface MenuMessage {
    val label: String

    /** The app was uninstalled or disabled since the menu was loaded. */
    data class Unavailable(override val label: String) : MenuMessage

    /** The app exists but could not be started. */
    data class LaunchFailed(override val label: String) : MenuMessage
}

/**
 * Owns the menu order for one "session": the time between the menu becoming visible and the
 * user actually leaving it. The order is shuffled exactly once per session and otherwise only
 * reconciled (never reshuffled) when the installed apps change. Because the ViewModel survives
 * configuration changes, rotation keeps the same order.
 */
class MenuViewModel(
    private val catalog: AppCatalog,
    private val launcher: AppLauncher,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _state = MutableStateFlow<MenuUiState>(MenuUiState.Loading)
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    /** One-shot user message (e.g. a failed launch); call [consumeMessage] once shown. */
    private val _message = MutableStateFlow<MenuMessage?>(null)
    val message: StateFlow<MenuMessage?> = _message.asStateFlow()

    private var sessionActive = false
    private var needsShuffle = false
    private var loadJob: Job? = null
    private var changesJob: Job? = null

    /** Call from Activity.onStart. Starts a new session (one shuffle) unless returning from a config change. */
    fun onMenuStarted() {
        if (!sessionActive) {
            sessionActive = true
            needsShuffle = true
            _state.value = MenuUiState.Loading
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

    fun consumeMessage() {
        _message.value = null
    }

    /**
     * Reloads the catalog. Shuffles if this session has no order yet; otherwise reconciles
     * against the visible order. A newer refresh cancels an older one still in flight.
     */
    private fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val latest = try {
                catalog.loadApps()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                val current = _state.value
                if (needsShuffle || current !is MenuUiState.Ready) {
                    _state.value = MenuUiState.Error
                }
                return@launch
            }
            val current = _state.value
            val ordered = if (needsShuffle || current !is MenuUiState.Ready) {
                MenuOrder.shuffled(latest, random)
            } else {
                MenuOrder.reconcile(current.apps, latest)
            }
            needsShuffle = false
            _state.value = if (ordered.isEmpty()) MenuUiState.Empty else MenuUiState.Ready(ordered)
        }
    }

    companion object {
        fun factory(catalog: AppCatalog, launcher: AppLauncher): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { MenuViewModel(catalog, launcher) }
            }
    }
}
