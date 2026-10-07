package com.deezus.wordy.ui.definition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.deezus.wordy.appContainer
import com.deezus.wordy.data.bookmarks.BookmarkRepository
import com.deezus.wordy.data.dictionary.DefinitionResult
import com.deezus.wordy.data.dictionary.DictionaryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DefinitionUiState(
  val word: String,
  val isBookmarked: Boolean = false,
  /** Null while the definition is loading. */
  val result: DefinitionResult? = null,
)

class DefinitionViewModel(
  private val word: String,
  private val dictionaryRepository: DictionaryRepository,
  private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

  private val result = MutableStateFlow<DefinitionResult?>(null)
  private var loadJob: Job? = null

  val uiState: StateFlow<DefinitionUiState> =
    combine(result, bookmarkRepository.isBookmarked(word)) { result, isBookmarked ->
      DefinitionUiState(word, isBookmarked, result)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DefinitionUiState(word))

  init {
    load()
  }

  fun onRetry() = load()

  fun onToggleBookmark() {
    viewModelScope.launch { bookmarkRepository.toggle(word) }
  }

  private fun load() {
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      result.value = null
      result.value = dictionaryRepository.lookUp(word)
    }
  }

  companion object {
    fun factory(word: String): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        DefinitionViewModel(word, appContainer.dictionaryRepository, appContainer.bookmarkRepository)
      }
    }
  }
}
