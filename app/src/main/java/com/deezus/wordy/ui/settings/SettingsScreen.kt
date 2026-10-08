package com.deezus.wordy.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deezus.wordy.R
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.data.settings.ScoreDisplay
import com.deezus.wordy.data.settings.ScoreStats
import com.deezus.wordy.ui.components.SubScreenScaffold
import com.deezus.wordy.ui.game.formatAverage
import com.deezus.wordy.ui.theme.WordyColors
import kotlinx.coroutines.launch

private const val FEEDBACK_EMAIL = "david@zhang.email"
private const val SOURCE_URL = "https://github.com/hansololz/wordy"

@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val noEmailAppMessage = stringResource(R.string.settings_no_email_app, FEEDBACK_EMAIL)
  val noBrowserMessage = stringResource(R.string.settings_no_browser, SOURCE_URL)

  SubScreenScaffold(
    title = stringResource(R.string.settings_title),
    onBack = onBack,
    snackbarHostState = snackbarHostState,
  ) { padding ->
    val state = uiState ?: return@SubScreenScaffold

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(padding)
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Section(stringResource(R.string.settings_word_length)) {
          SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(12.dp)) {
            GameMode.entries.forEachIndexed { index, mode ->
              SegmentedButton(
                selected = state.settings.mode == mode,
                onClick = { viewModel.onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, GameMode.entries.size),
              ) {
                Text(stringResource(R.string.settings_word_length_option, mode.wordLength))
              }
            }
          }
        }

        Section(stringResource(R.string.settings_score_display)) {
          Column(Modifier.selectableGroup()) {
            ScoreDisplay.entries.forEach { scoreDisplay ->
              RadioRow(
                label = stringResource(scoreDisplay.optionRes()),
                selected = state.settings.scoreDisplay == scoreDisplay,
                onSelect = { viewModel.onScoreDisplaySelected(scoreDisplay) },
              )
            }
          }
        }

        Section(stringResource(R.string.settings_preferences)) {
          SwitchRow(
            label = stringResource(R.string.settings_haptics),
            checked = state.settings.hapticsEnabled,
            onCheckedChange = viewModel::onHapticsChanged,
          )
          HorizontalDivider(Modifier.padding(horizontal = 16.dp))
          SwitchRow(
            label = stringResource(R.string.settings_auto_complete),
            description = stringResource(R.string.settings_auto_complete_description),
            checked = state.settings.autoCompleteEnabled,
            onCheckedChange = viewModel::onAutoCompleteChanged,
          )
        }

        Section(stringResource(R.string.settings_statistics)) {
          Statistics(state.stats)
        }

        Section(stringResource(R.string.settings_feedback)) {
          LinkRow(
            title = stringResource(R.string.settings_feedback_action),
            description = stringResource(R.string.settings_feedback_description, FEEDBACK_EMAIL),
            onClick = {
              if (!openFeedbackEmail(context)) {
                coroutineScope.launch { snackbarHostState.showSnackbar(noEmailAppMessage) }
              }
            },
          )
        }

        Section(stringResource(R.string.settings_about)) {
          LinkRow(
            title = stringResource(R.string.settings_source_code),
            description = stringResource(R.string.settings_source_code_description, SOURCE_URL),
            onClick = {
              if (!openUrl(context, SOURCE_URL)) {
                coroutineScope.launch { snackbarHostState.showSnackbar(noBrowserMessage) }
              }
            },
          )
        }

        Text(
          text = stringResource(R.string.settings_version, appVersion(context)),
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          style = MaterialTheme.typography.bodySmall,
          color = WordyColors.TextDisabled,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

/** A tappable row that leads somewhere outside the app: a title with the destination underneath. */
@Composable
private fun LinkRow(title: String, description: String, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClick = onClick)
      .padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title)
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = WordyColors.TextMuted,
      )
    }
    Icon(
      painter = painterResource(R.drawable.ic_round_open_in_new_24),
      contentDescription = null,
      tint = WordyColors.TextMuted,
    )
  }
}

/** A titled group of related settings, drawn as a card. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title.uppercase(),
      modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
      style = MaterialTheme.typography.labelMedium,
      letterSpacing = 1.2.sp,
      color = WordyColors.TextMuted,
    )
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      color = WordyColors.Surface,
    ) {
      Column(content = content)
    }
  }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 52.dp)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    RadioButton(selected = selected, onClick = null)
    Text(label)
  }
}

@Composable
private fun SwitchRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  description: String? = null,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 60.dp)
      .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(label)
      if (description != null) {
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = WordyColors.TextMuted,
        )
      }
    }
    Switch(checked = checked, onCheckedChange = null)
  }
}

/** Lifetime totals, with and without hinted games, as a small table. */
@Composable
private fun Statistics(stats: ScoreStats) {
  Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
    StatRow(
      label = "",
      score = stringResource(R.string.stat_score),
      wins = stringResource(R.string.stat_wins),
      average = stringResource(R.string.stat_average),
      isHeader = true,
    )
    StatRow(
      label = stringResource(R.string.stat_all_games),
      score = stats.totalScore.toString(),
      wins = stats.gamesWon.toString(),
      average = formatAverage(stats.averageScore),
    )
    HorizontalDivider()
    StatRow(
      label = stringResource(R.string.stat_without_hints),
      score = stats.totalScoreWithoutHints.toString(),
      wins = stats.gamesWonWithoutHints.toString(),
      average = formatAverage(stats.averageScoreWithoutHints),
    )
  }
}

@Composable
private fun StatRow(
  label: String,
  score: String,
  wins: String,
  average: String,
  isHeader: Boolean = false,
) {
  val valueStyle = if (isHeader) {
    MaterialTheme.typography.labelMedium
  } else {
    MaterialTheme.typography.titleMedium
  }
  val valueColor = if (isHeader) WordyColors.TextMuted else WordyColors.Text

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = if (isHeader) 2.dp else 10.dp)
      .semantics(mergeDescendants = true) {},
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, modifier = Modifier.weight(1.6f))
    for (value in listOf(score, wins, average)) {
      Text(
        text = value,
        modifier = Modifier.weight(1f),
        style = valueStyle,
        fontWeight = if (isHeader) FontWeight.Normal else FontWeight.SemiBold,
        color = valueColor,
        textAlign = TextAlign.End,
      )
    }
  }
}

private fun ScoreDisplay.optionRes(): Int = when (this) {
  ScoreDisplay.Total -> R.string.stat_total_score
  ScoreDisplay.Average -> R.string.stat_average_score
  ScoreDisplay.TotalWithoutHints -> R.string.stat_total_score_without_hints
  ScoreDisplay.AverageWithoutHints -> R.string.stat_average_score_without_hints
}

private fun openFeedbackEmail(context: Context): Boolean {
  val intent = Intent(Intent.ACTION_SENDTO, "mailto:$FEEDBACK_EMAIL".toUri())
    .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_feedback_subject))
  return try {
    context.startActivity(intent)
    true
  } catch (error: ActivityNotFoundException) {
    false
  }
}

private fun openUrl(context: Context, url: String): Boolean {
  return try {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
  } catch (error: ActivityNotFoundException) {
    false
  }
}

private fun appVersion(context: Context): String =
  context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
