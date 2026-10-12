package com.benisalla.shufflemenu.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.benisalla.shufflemenu.model.AppCatalog
import com.benisalla.shufflemenu.model.AppEntry
import com.benisalla.shufflemenu.model.AppLauncher
import com.benisalla.shufflemenu.model.LaunchResult
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
    data class Error(val message: String) : MenuUiState
    data class Ready(val apps: List<AppEntry>) : MenuUiState
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
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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
                _message.value = "${entry.label} is no longer available"
                refresh()
            }
            // The technical reason is not useful to the user; the launcher may log it.
            is LaunchResult.Failed -> _message.value = "Couldn't open ${entry.label}"
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
            } catch (e: Exception) {
                val current = _state.value
                if (needsShuffle || current !is MenuUiState.Ready) {
                    _state.value = MenuUiState.Error(e.message ?: "Couldn't load apps")
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
