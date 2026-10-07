package com.deezus.wordy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.deezus.wordy.appContainer
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.data.settings.ScoreDisplay
import com.deezus.wordy.data.settings.ScoreStats
import com.deezus.wordy.data.settings.SettingsRepository
import com.deezus.wordy.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(val settings: UserSettings, val stats: ScoreStats)

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

  /** Null until settings have been read from disk. */
  val uiState: StateFlow<SettingsUiState?> =
    combine(settingsRepository.settings, settingsRepository.stats, ::SettingsUiState)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  fun onModeSelected(mode: GameMode) {
    viewModelScope.launch { settingsRepository.setMode(mode) }
  }

  fun onScoreDisplaySelected(scoreDisplay: ScoreDisplay) {
    viewModelScope.launch { settingsRepository.setScoreDisplay(scoreDisplay) }
  }

  fun onHapticsChanged(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
  }

  fun onAutoCompleteChanged(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setAutoCompleteEnabled(enabled) }
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer { SettingsViewModel(appContainer.settingsRepository) }
    }
  }
}
