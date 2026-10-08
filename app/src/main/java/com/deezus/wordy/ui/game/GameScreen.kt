package com.deezus.wordy.ui.game

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deezus.wordy.R
import com.deezus.wordy.core.GameStatus
import com.deezus.wordy.data.settings.ScoreDisplay
import com.deezus.wordy.data.settings.ScoreStats
import com.deezus.wordy.review.ReviewPrompter
import com.deezus.wordy.ui.components.rememberHaptic
import com.deezus.wordy.ui.theme.WordyColors
import kotlinx.coroutines.launch
import java.util.Locale

private val MaxContentWidth = 560.dp

@Composable
fun GameScreen(
  onOpenBookmarks: () -> Unit,
  onOpenHistory: () -> Unit,
  onOpenSettings: () -> Unit,
  onLookUp: (String) -> Unit,
  viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  when (val state = uiState) {
    GameUiState.Loading -> {
      Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    }
    is GameUiState.Ready -> {
      GameContent(
        state = state,
        viewModel = viewModel,
        onOpenBookmarks = onOpenBookmarks,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        onLookUp = onLookUp,
      )
    }
  }
}

@Composable
private fun GameContent(
  state: GameUiState.Ready,
  viewModel: GameViewModel,
  onOpenBookmarks: () -> Unit,
  onOpenHistory: () -> Unit,
  onOpenSettings: () -> Unit,
  onLookUp: (String) -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val performHaptic = rememberHaptic(state.hapticsEnabled)
  var showHints by rememberSaveable { mutableStateOf(false) }

  state.message?.let { message ->
    val text = message.text()
    LaunchedEffect(message.id) {
      // Shown from a scope that outlives this effect, because acknowledging the message removes it.
      snackbarHostState.currentSnackbarData?.dismiss()
      coroutineScope.launch { snackbarHostState.showSnackbar(text) }
      viewModel.onMessageShown(message)
    }
  }

  if (state.reviewRequested) {
    val activity = LocalActivity.current
    LaunchedEffect(Unit) {
      if (activity != null) ReviewPrompter.request(activity)
      viewModel.onReviewRequestHandled()
    }
  }

  val onLetter: (Char) -> Unit = { performHaptic(); viewModel.onLetter(it) }
  val onDelete = { performHaptic(); viewModel.onDelete() }
  val onSubmit = { performHaptic(); viewModel.onSubmit() }

  // Lets a hardware keyboard drive the game on tablets, foldables and Chromebooks.
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) { focusRequester.requestFocus() }

  Scaffold(
    modifier = Modifier
      .focusRequester(focusRequester)
      .focusable()
      .onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        val letter = event.utf16CodePoint.toChar().lowercaseChar()
        when {
          event.key == Key.Enter || event.key == Key.NumPadEnter -> viewModel.onSubmit()
          event.key == Key.Backspace -> viewModel.onDelete()
          letter in 'a'..'z' -> viewModel.onLetter(letter)
          else -> return@onKeyEvent false
        }
        true
      },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
      // Short, wide windows (a phone on its side) put the board beside the keyboard.
      val sideBySide = maxWidth > maxHeight && maxHeight < 560.dp
      val keyHeight = if (sideBySide) 44.dp else 54.dp

      val header = @Composable {
        GameHeader(
          scoreDisplay = state.scoreDisplay,
          stats = state.stats,
          onOpenBookmarks = { performHaptic(); onOpenBookmarks() },
          onOpenHistory = { performHaptic(); onOpenHistory() },
          onOpenSettings = { performHaptic(); onOpenSettings() },
        )
      }
      val keyboard = @Composable {
        Keyboard(
          keyMarks = state.keyMarks,
          enabled = state.isActive,
          keyHeight = keyHeight,
          onLetter = onLetter,
          onDelete = onDelete,
        )
      }
      val controls = @Composable {
        GameControls(
          isActive = state.isActive,
          submitState = state.submitState,
          height = keyHeight,
          onHint = { performHaptic(); showHints = true },
          onSubmit = onSubmit,
          onSkipOrNext = { performHaptic(); viewModel.onSkipOrNext() },
        )
      }

      if (sideBySide) {
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Board(
            rows = state.rows,
            onLookUp = onLookUp,
            modifier = Modifier.weight(1f).fillMaxHeight(),
          )
          Column(
            modifier = Modifier.weight(1f).fillMaxHeight().widthIn(max = MaxContentWidth),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            header()
            Spacer(Modifier.weight(1f))
            keyboard()
            controls()
          }
        }
      } else {
        Column(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .widthIn(max = MaxContentWidth)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          header()
          Board(
            rows = state.rows,
            onLookUp = onLookUp,
            modifier = Modifier.weight(1f).fillMaxWidth(),
          )
          keyboard()
          controls()
        }
      }
    }
  }

  if (showHints) {
    HintSheet(
      onDismiss = { showHints = false },
      onHint = { type ->
        performHaptic()
        showHints = false
        viewModel.onHint(type)
      },
    )
  }

  when (val dialog = state.dialog) {
    GameDialog.ConfirmSkip -> SkipDialog(
      onConfirm = viewModel::onSkipConfirmed,
      onDismiss = viewModel::onDialogDismissed,
    )
    is GameDialog.Finished -> FinishedDialog(
      dialog = dialog,
      onPlayAgain = viewModel::onPlayAgain,
      onDismiss = viewModel::onDialogDismissed,
    )
    null -> Unit
  }
}

@Composable
private fun GameHeader(
  scoreDisplay: ScoreDisplay,
  stats: ScoreStats,
  onOpenBookmarks: () -> Unit,
  onOpenHistory: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
    Row(
      modifier = Modifier.align(Alignment.CenterStart),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      HeaderButton(
        iconRes = R.drawable.ic_round_history_24,
        descriptionRes = R.string.history_title,
        onClick = onOpenHistory,
      )
      HeaderButton(
        iconRes = R.drawable.ic_round_bookmark_border_24,
        descriptionRes = R.string.bookmarks_title,
        onClick = onOpenBookmarks,
      )
    }

    Column(
      modifier = Modifier.align(Alignment.Center).semantics(mergeDescendants = true) {},
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(scoreDisplay.labelRes()).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.2.sp,
        color = WordyColors.TextMuted,
        textAlign = TextAlign.Center,
      )
      Text(
        text = formatScore(scoreDisplay, stats),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
    }

    HeaderButton(
      iconRes = R.drawable.ic_round_settings_24,
      descriptionRes = R.string.settings_title,
      onClick = onOpenSettings,
      modifier = Modifier.align(Alignment.CenterEnd),
    )
  }
}

@Composable
private fun HeaderButton(
  iconRes: Int,
  descriptionRes: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilledIconButton(
    onClick = onClick,
    modifier = modifier.size(46.dp),
    colors = IconButtonDefaults.filledIconButtonColors(
      containerColor = WordyColors.SurfaceBright,
      contentColor = WordyColors.Text,
    ),
  ) {
    Icon(painterResource(iconRes), contentDescription = stringResource(descriptionRes))
  }
}

@Composable
private fun GameControls(
  isActive: Boolean,
  submitState: SubmitState,
  height: Dp,
  onHint: () -> Unit,
  onSubmit: () -> Unit,
  onSkipOrNext: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ControlButton(
      text = stringResource(R.string.action_hint),
      containerColor = WordyColors.HintContainer,
      contentColor = WordyColors.OnHintContainer,
      enabled = isActive,
      height = height,
      onClick = onHint,
      modifier = Modifier.weight(1f),
    )

    // Stays tappable while a guess is incomplete so the player is told what is missing.
    val (guessContainer, guessContent) = when (submitState) {
      SubmitState.Ready -> WordyColors.Correct to WordyColors.OnMark
      SubmitState.NotAWord -> WordyColors.ErrorContainer to WordyColors.OnErrorContainer
      else -> WordyColors.SurfaceBright to WordyColors.TextMuted
    }
    ControlButton(
      text = stringResource(
        if (submitState == SubmitState.NotAWord) R.string.action_not_a_word else R.string.action_guess
      ),
      containerColor = guessContainer,
      contentColor = guessContent,
      enabled = isActive,
      height = height,
      onClick = onSubmit,
      modifier = Modifier.weight(2f),
    )

    // Once the game is over, moving on is the main thing left to do.
    ControlButton(
      text = stringResource(if (isActive) R.string.action_skip else R.string.action_next),
      containerColor = if (isActive) WordyColors.WarningContainer else WordyColors.Accent,
      contentColor = if (isActive) WordyColors.OnWarningContainer else WordyColors.OnAccent,
      enabled = true,
      height = height,
      onClick = onSkipOrNext,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun ControlButton(
  text: String,
  containerColor: Color,
  contentColor: Color,
  enabled: Boolean,
  height: Dp,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.height(height),
    shape = MaterialTheme.shapes.medium,
    contentPadding = PaddingValues(horizontal = 4.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      contentColor = contentColor,
      disabledContainerColor = WordyColors.Surface,
      disabledContentColor = WordyColors.TextDisabled,
    ),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SkipDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.dialog_skip_title)) },
    text = { Text(stringResource(R.string.dialog_skip_message)) },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_skip)) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_keep_playing)) }
    },
  )
}

@Composable
private fun FinishedDialog(
  dialog: GameDialog.Finished,
  onPlayAgain: () -> Unit,
  onDismiss: () -> Unit,
) {
  val won = dialog.status == GameStatus.Won
  val (title, message) = when (dialog.status) {
    GameStatus.Won -> stringResource(R.string.dialog_won_title) to
      pluralStringResource(R.plurals.dialog_won_message, dialog.score, dialog.score)
    GameStatus.Lost -> stringResource(R.string.dialog_lost_title) to
      stringResource(R.string.dialog_lost_message)
    else -> stringResource(R.string.dialog_skipped_title) to
      stringResource(R.string.dialog_skipped_message)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(message)
        AnswerTiles(word = dialog.answer, highlighted = won)
      }
    },
    confirmButton = {
      Button(
        onClick = onPlayAgain,
        colors = ButtonDefaults.buttonColors(
          containerColor = WordyColors.Accent,
          contentColor = WordyColors.OnAccent,
        ),
      ) {
        Text(stringResource(R.string.action_play_again))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_view_guesses)) }
    },
  )
}

/** The answer spelled out as a row of small tiles. */
@Composable
private fun AnswerTiles(word: String, highlighted: Boolean) {
  val tileSize = 34.dp
  val fontSize = with(LocalDensity.current) { (tileSize * 0.5f).toSp() }

  Row(
    modifier = Modifier.clearAndSetSemantics { contentDescription = word },
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    for (letter in word.uppercase()) {
      Box(
        modifier = Modifier
          .size(tileSize)
          .clip(RoundedCornerShape(7.dp))
          .background(if (highlighted) WordyColors.Correct else WordyColors.SurfaceBrighter),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = letter.toString(),
          color = WordyColors.OnMark,
          fontSize = fontSize,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun GameMessage.text(): String = when (this) {
  is GameMessage.WordTooShort -> stringResource(R.string.message_word_too_short, wordLength)
  is GameMessage.NoHintAvailable -> stringResource(R.string.message_no_hint)
}

private fun ScoreDisplay.labelRes(): Int = when (this) {
  ScoreDisplay.Total -> R.string.score_total
  ScoreDisplay.Average -> R.string.score_average
  ScoreDisplay.TotalWithoutHints -> R.string.score_total_without_hints
  ScoreDisplay.AverageWithoutHints -> R.string.score_average_without_hints
}

internal fun formatScore(scoreDisplay: ScoreDisplay, stats: ScoreStats): String = when (scoreDisplay) {
  ScoreDisplay.Total -> stats.totalScore.toString()
  ScoreDisplay.Average -> formatAverage(stats.averageScore)
  ScoreDisplay.TotalWithoutHints -> stats.totalScoreWithoutHints.toString()
  ScoreDisplay.AverageWithoutHints -> formatAverage(stats.averageScoreWithoutHints)
}

internal fun formatAverage(average: Double?): String =
  if (average == null) "0" else String.format(Locale.getDefault(), "%.1f", average)
