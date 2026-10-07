package com.deezus.wordy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.deezus.wordy.appContainer
import com.deezus.wordy.data.bookmarks.BookmarkRepository
import com.deezus.wordy.data.history.HistoryEntry
import com.deezus.wordy.data.history.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HistoryUiState {
  data object Loading : HistoryUiState
  data class Loaded(val entries: List<HistoryEntry>) : HistoryUiState
}

class HistoryViewModel(
  historyRepository: HistoryRepository,
  private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

  val uiState: StateFlow<HistoryUiState> = historyRepository.history
    .map<List<HistoryEntry>, HistoryUiState> { HistoryUiState.Loaded(it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

  fun onToggleBookmark(word: String) {
    viewModelScope.launch { bookmarkRepository.toggle(word) }
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        HistoryViewModel(appContainer.historyRepository, appContainer.bookmarkRepository)
      }
    }
  }
}
