package com.deezus.wordy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deezus.wordy.R

/** The frame shared by every screen reached from the game: a title bar with a back button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenScaffold(
  title: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
  actions: @Composable RowScope.() -> Unit = {},
  content: @Composable (PaddingValues) -> Unit,
) {
  val performHaptic = rememberHaptic()

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
          IconButton(onClick = { performHaptic(); onBack() }) {
            Icon(
              painter = painterResource(R.drawable.ic_round_arrow_back_24),
              contentDescription = stringResource(R.string.action_back),
            )
          }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = WindowInsets.safeDrawing,
    content = content,
  )
}

/** A centred message for screens that have nothing to list or could not load. */
@Composable
fun MessageState(
  title: String,
  modifier: Modifier = Modifier,
  detail: String? = null,
  actions: @Composable () -> Unit = {},
) {
  Column(
    modifier = modifier.fillMaxSize().padding(32.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    if (detail != null) {
      Text(
        text = detail,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
    actions()
  }
}
