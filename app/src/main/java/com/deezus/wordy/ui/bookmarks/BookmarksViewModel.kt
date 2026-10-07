package com.deezus.wordy.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.deezus.wordy.appContainer
import com.deezus.wordy.data.bookmarks.Bookmark
import com.deezus.wordy.data.bookmarks.BookmarkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BookmarksUiState {
  data object Loading : BookmarksUiState
  data class Loaded(val bookmarks: List<Bookmark>) : BookmarksUiState
}

class BookmarksViewModel(private val bookmarkRepository: BookmarkRepository) : ViewModel() {

  val uiState: StateFlow<BookmarksUiState> = bookmarkRepository.bookmarks
    .map<List<Bookmark>, BookmarksUiState> { BookmarksUiState.Loaded(it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookmarksUiState.Loading)

  fun onRemove(bookmark: Bookmark) {
    viewModelScope.launch { bookmarkRepository.remove(bookmark.word) }
  }

  fun onUndoRemove(bookmark: Bookmark) {
    viewModelScope.launch { bookmarkRepository.restore(bookmark) }
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer { BookmarksViewModel(appContainer.bookmarkRepository) }
    }
  }
}
