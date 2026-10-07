package com.deezus.wordy.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deezus.wordy.R
import com.deezus.wordy.data.bookmarks.Bookmark
import com.deezus.wordy.ui.components.Badge
import com.deezus.wordy.ui.components.MessageState
import com.deezus.wordy.ui.components.SubScreenScaffold
import com.deezus.wordy.ui.components.WordCard
import com.deezus.wordy.ui.theme.WordyColors
import kotlinx.coroutines.launch

@Composable
fun BookmarksScreen(
  onBack: () -> Unit,
  onLookUp: (String) -> Unit,
  viewModel: BookmarksViewModel = viewModel(factory = BookmarksViewModel.Factory),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val resources = LocalResources.current

  val onRemove: (Bookmark) -> Unit = { bookmark ->
    viewModel.onRemove(bookmark)
    coroutineScope.launch {
      snackbarHostState.currentSnackbarData?.dismiss()
      val result = snackbarHostState.showSnackbar(
        message = resources.getString(R.string.bookmark_removed, bookmark.word.uppercase()),
        actionLabel = resources.getString(R.string.action_undo),
        duration = SnackbarDuration.Short,
      )
      if (result == SnackbarResult.ActionPerformed) viewModel.onUndoRemove(bookmark)
    }
  }

  SubScreenScaffold(
    title = stringResource(R.string.bookmarks_title),
    onBack = onBack,
    snackbarHostState = snackbarHostState,
  ) { padding ->
    when (val state = uiState) {
      BookmarksUiState.Loading -> Unit
      is BookmarksUiState.Loaded -> {
        if (state.bookmarks.isEmpty()) {
          MessageState(
            title = stringResource(R.string.bookmarks_empty_title),
            detail = stringResource(R.string.bookmarks_empty_detail),
            modifier = Modifier.padding(padding),
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = padding + PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(state.bookmarks, key = { it.word }) { bookmark ->
              val bestScore = bookmark.bestScore
              WordCard(
                word = bookmark.word,
                isBookmarked = true,
                onClick = { onLookUp(bookmark.word) },
                onToggleBookmark = { onRemove(bookmark) },
                badge = if (bestScore != null) {
                  {
                    Badge(
                      text = pluralStringResource(R.plurals.points_earned, bestScore, bestScore),
                      containerColor = WordyColors.CorrectContainer,
                      contentColor = WordyColors.OnCorrectContainer,
                    )
                  }
                } else {
                  null
                },
              )
            }
          }
        }
      }
    }
  }
}
