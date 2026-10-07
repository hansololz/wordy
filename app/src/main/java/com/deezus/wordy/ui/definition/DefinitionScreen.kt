package com.deezus.wordy.ui.definition

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deezus.wordy.R
import com.deezus.wordy.data.dictionary.DefinitionResult
import com.deezus.wordy.data.dictionary.Meaning
import com.deezus.wordy.data.dictionary.WordDefinition
import com.deezus.wordy.ui.components.Badge
import com.deezus.wordy.ui.components.BookmarkButton
import com.deezus.wordy.ui.components.MessageState
import com.deezus.wordy.ui.components.SubScreenScaffold
import com.deezus.wordy.ui.theme.WordyColors
import kotlinx.coroutines.launch

@Composable
fun DefinitionScreen(
  word: String,
  onBack: () -> Unit,
  viewModel: DefinitionViewModel = viewModel(factory = DefinitionViewModel.factory(word)),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val noBrowserMessage = stringResource(R.string.definition_no_browser)

  val onSearchWeb: () -> Unit = {
    if (!openWebSearch(context, uiState.word)) {
      coroutineScope.launch { snackbarHostState.showSnackbar(noBrowserMessage) }
    }
  }

  SubScreenScaffold(
    title = uiState.word.replaceFirstChar { it.uppercase() },
    onBack = onBack,
    snackbarHostState = snackbarHostState,
    actions = {
      BookmarkButton(uiState.word, uiState.isBookmarked, viewModel::onToggleBookmark)
      IconButton(onClick = onSearchWeb) {
        Icon(
          painter = painterResource(R.drawable.ic_round_open_in_new_24),
          contentDescription = stringResource(R.string.action_search_web),
        )
      }
    },
  ) { padding ->
    when (val result = uiState.result) {
      null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
      is DefinitionResult.Found -> DefinitionContent(result.definition, padding)
      DefinitionResult.NotFound -> MessageState(
        title = stringResource(R.string.definition_not_found),
        modifier = Modifier.padding(padding),
      ) {
        OutlinedButton(onClick = onSearchWeb) { Text(stringResource(R.string.action_search_web)) }
      }
      DefinitionResult.NetworkError, DefinitionResult.ServerError -> MessageState(
        title = stringResource(
          if (result == DefinitionResult.NetworkError) {
            R.string.definition_network_error
          } else {
            R.string.definition_server_error
          }
        ),
        modifier = Modifier.padding(padding),
      ) {
        Button(onClick = viewModel::onRetry) { Text(stringResource(R.string.action_retry)) }
        OutlinedButton(onClick = onSearchWeb) { Text(stringResource(R.string.action_search_web)) }
      }
    }
  }
}

@Composable
private fun DefinitionContent(definition: WordDefinition, padding: PaddingValues) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    contentPadding = padding + PaddingValues(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(definition.meanings) { meaning ->
      MeaningCard(meaning)
    }
  }
}

@Composable
private fun MeaningCard(meaning: Meaning) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = WordyColors.Surface,
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      if (meaning.partOfSpeech.isNotBlank()) {
        Badge(
          text = meaning.partOfSpeech,
          containerColor = WordyColors.AccentContainer,
          contentColor = WordyColors.OnAccentContainer,
        )
      }

      meaning.senses.forEachIndexed { index, sense ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = stringResource(R.string.definition_number, index + 1),
            style = MaterialTheme.typography.bodyLarge,
            color = WordyColors.TextMuted,
          )
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = sense.definition, style = MaterialTheme.typography.bodyLarge)
            if (sense.example != null) {
              Text(
                text = stringResource(R.string.definition_example, sense.example),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = WordyColors.TextMuted,
              )
            }
          }
        }
      }

      if (meaning.synonyms.isNotEmpty()) {
        Text(
          text = stringResource(R.string.definition_synonyms, meaning.synonyms.joinToString(", ")),
          style = MaterialTheme.typography.bodyMedium,
          color = WordyColors.TextMuted,
        )
      }
    }
  }
}

/** Opens a web search for the word's definition. Returns false if nothing can handle it. */
private fun openWebSearch(context: Context, word: String): Boolean {
  val uri = "https://www.google.com/search".toUri().buildUpon()
    .appendQueryParameter("q", "define $word")
    .build()
  return try {
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    true
  } catch (error: ActivityNotFoundException) {
    false
  }
}
