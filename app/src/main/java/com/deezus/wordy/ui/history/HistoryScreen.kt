package com.deezus.wordy.ui.history

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deezus.wordy.R
import com.deezus.wordy.data.db.GameOutcome
import com.deezus.wordy.data.history.HistoryEntry
import com.deezus.wordy.ui.components.Badge
import com.deezus.wordy.ui.components.MessageState
import com.deezus.wordy.ui.components.SubScreenScaffold
import com.deezus.wordy.ui.components.WordCard
import com.deezus.wordy.ui.theme.WordyColors

@Composable
fun HistoryScreen(
  onBack: () -> Unit,
  onLookUp: (String) -> Unit,
  viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SubScreenScaffold(title = stringResource(R.string.history_title), onBack = onBack) { padding ->
    when (val state = uiState) {
      HistoryUiState.Loading -> Unit
      is HistoryUiState.Loaded -> {
        if (state.entries.isEmpty()) {
          MessageState(
            title = stringResource(R.string.history_empty_title),
            detail = stringResource(R.string.history_empty_detail),
            modifier = Modifier.padding(padding),
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = padding + PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(state.entries, key = { it.id }) { entry ->
              HistoryRow(
                entry = entry,
                onLookUp = { onLookUp(entry.word) },
                onToggleBookmark = { viewModel.onToggleBookmark(entry.word) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onLookUp: () -> Unit, onToggleBookmark: () -> Unit) {
  val context = LocalContext.current

  val date = DateUtils.formatDateTime(
    context,
    entry.finishedAt,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH,
  )
  val summary = listOfNotNull(
    date,
    stringResource(R.string.history_hint_used).takeIf { entry.usedHint },
  ).joinToString(" · ")

  WordCard(
    word = entry.word,
    isBookmarked = entry.isBookmarked,
    onClick = onLookUp,
    onToggleBookmark = onToggleBookmark,
    summary = summary,
    detail = entry.guesses.takeIf { it.isNotEmpty() }?.joinToString("  ") { it.uppercase() },
    badge = {
      when (entry.outcome) {
        GameOutcome.Won -> Badge(
          text = pluralStringResource(R.plurals.points_earned, entry.score, entry.score),
          containerColor = WordyColors.CorrectContainer,
          contentColor = WordyColors.OnCorrectContainer,
        )
        GameOutcome.Lost -> Badge(
          text = stringResource(R.string.history_lost),
          containerColor = WordyColors.ErrorContainer,
          contentColor = WordyColors.OnErrorContainer,
        )
        GameOutcome.Skipped -> Badge(
          text = stringResource(R.string.history_skipped),
          containerColor = WordyColors.SurfaceBright,
          contentColor = WordyColors.TextMuted,
        )
      }
    },
  )
}
