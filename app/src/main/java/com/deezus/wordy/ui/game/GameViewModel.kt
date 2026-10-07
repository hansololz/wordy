package com.deezus.wordy.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.deezus.wordy.appContainer
import com.deezus.wordy.core.GameEngine
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.core.GameState
import com.deezus.wordy.core.GameStatus
import com.deezus.wordy.core.SubmitResult
import com.deezus.wordy.data.game.SavedGameStore
import com.deezus.wordy.data.game.SavedGames
import com.deezus.wordy.data.history.HistoryRepository
import com.deezus.wordy.data.settings.SettingsRepository
import com.deezus.wordy.data.settings.UserSettings
import com.deezus.wordy.data.words.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
  private val wordRepository: WordRepository,
  private val settingsRepository: SettingsRepository,
  private val savedGameStore: SavedGameStore,
  private val historyRepository: HistoryRepository,
  private val applicationScope: CoroutineScope,
  private val clock: () -> Long,
  private val awaitStartup: suspend () -> Unit,
) : ViewModel() {

  /** Null until saved games and settings have been read from disk. */
  private val session = MutableStateFlow<Session?>(null)
  private val overlay = MutableStateFlow(Overlay())

  private var nextMessageId = 0L
  private var reviewDueAfterDialog = false

  val uiState: StateFlow<GameUiState> =
    combine(session, overlay, settingsRepository.stats) { session, overlay, stats ->
      val game = session?.game ?: return@combine GameUiState.Loading
      val autoComplete = session.settings.autoCompleteEnabled

      GameUiState.Ready(
        mode = game.mode,
        rows = boardRows(game, autoComplete),
        keyMarks = GameEngine.keyMarks(game),
        isActive = game.isActive,
        submitState = submitState(game, autoComplete, wordRepository::isValidWord),
        scoreDisplay = session.settings.scoreDisplay,
        stats = stats,
        hapticsEnabled = session.settings.hapticsEnabled,
        dialog = overlay.dialog,
        message = overlay.message,
        reviewRequested = overlay.reviewRequested,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState.Loading)

  init {
    viewModelScope.launch {
      awaitStartup()
      val savedGames = savedGameStore.load()

      settingsRepository.settings.collect { settings ->
        val games = session.value?.games ?: savedGames
        val withCurrentGame =
          if (games[settings.mode] == null) games.with(newGame(settings.mode)) else games

        session.value = Session(settings, withCurrentGame)
        if (withCurrentGame != games) savedGameStore.save(withCurrentGame)
      }
    }
  }

  fun onLetter(letter: Char) = updateGame { GameEngine.typeLetter(it, letter) }

  fun onDelete() = updateGame { GameEngine.deleteLetter(it) }

  fun onSubmit() {
    val current = session.value ?: return
    val game = current.game ?: return

    val result = GameEngine.submit(
      state = game,
      autoCompleteEnabled = current.settings.autoCompleteEnabled,
      isValidWord = wordRepository::isValidWord,
    )

    when (result) {
      is SubmitResult.Accepted -> {
        setGame(result.state)
        if (!result.state.isActive) onGameFinished(result.state)
      }
      SubmitResult.TooShort -> showMessage(GameMessage.WordTooShort(nextMessageId++, game.wordLength))
      SubmitResult.NotAWord, SubmitResult.GameOver -> Unit
    }
  }

  /** The skip button asks for confirmation mid-game and starts the next word once a game is over. */
  fun onSkipOrNext() {
    val game = session.value?.game ?: return
    if (game.isActive) {
      overlay.update { it.copy(dialog = GameDialog.ConfirmSkip) }
    } else {
      startNewGame()
    }
  }

  fun onSkipConfirmed() {
    val game = session.value?.game ?: return
    if (!game.isActive) {
      onDialogDismissed()
      return
    }

    val skipped = GameEngine.skip(game)
    setGame(skipped)
    onGameFinished(skipped)
  }

  fun onPlayAgain() {
    onDialogDismissed()
    startNewGame()
  }

  fun onDialogDismissed() {
    val wasFinishedDialog = overlay.value.dialog is GameDialog.Finished
    overlay.update {
      it.copy(dialog = null, reviewRequested = wasFinishedDialog && reviewDueAfterDialog)
    }
    if (wasFinishedDialog) reviewDueAfterDialog = false
  }

  fun onHint(type: HintType) {
    val game = session.value?.game ?: return
    val hinted = when (type) {
      HintType.RevealNextLetter -> GameEngine.revealNextLetter(game)
      HintType.RevealAbsentLetter -> GameEngine.revealAbsentLetter(game)
      HintType.RevealAllAbsentLetters -> GameEngine.revealAllAbsentLetters(game)
    }

    if (hinted != null) {
      setGame(hinted)
    } else if (game.isActive) {
      showMessage(GameMessage.NoHintAvailable(nextMessageId++))
    }
  }

  fun onMessageShown(message: GameMessage) {
    overlay.update { if (it.message?.id == message.id) it.copy(message = null) else it }
  }

  fun onReviewRequestHandled() {
    overlay.update { it.copy(reviewRequested = false) }
    applicationScope.launch { settingsRepository.onReviewRequested(clock()) }
  }

  private fun onGameFinished(game: GameState) {
    val finishedAt = clock()
    wordRepository.markUsed(game.mode, game.answer)

    // These writes must complete even if the screen is closed straight after the last guess.
    applicationScope.launch {
      if (game.status == GameStatus.Won) {
        settingsRepository.recordWin(GameEngine.score(game), game.usedHint)
      }
      historyRepository.record(game, finishedAt)
    }

    if (game.status == GameStatus.Won) {
      viewModelScope.launch { reviewDueAfterDialog = settingsRepository.isReviewDue(finishedAt) }
    }

    overlay.update {
      it.copy(dialog = GameDialog.Finished(game.status, game.answer, GameEngine.score(game)))
    }
  }

  private fun startNewGame() {
    val mode = session.value?.settings?.mode ?: return
    setGame(newGame(mode))
  }

  private fun newGame(mode: GameMode): GameState =
    GameEngine.newGame(mode, wordRepository.nextAnswer(mode))

  private inline fun updateGame(transform: (GameState) -> GameState) {
    val game = session.value?.game ?: return
    val updated = transform(game)
    if (updated != game) setGame(updated)
  }

  private fun setGame(game: GameState) {
    val current = session.value ?: return
    val games = current.games.with(game)
    session.value = current.copy(games = games)
    savedGameStore.save(games)
  }

  private fun showMessage(message: GameMessage) {
    overlay.update { it.copy(message = message) }
  }

  private data class Session(val settings: UserSettings, val games: SavedGames) {
    val game: GameState? get() = games[settings.mode]
  }

  private data class Overlay(
    val dialog: GameDialog? = null,
    val message: GameMessage? = null,
    val reviewRequested: Boolean = false,
  )

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val container = appContainer
        GameViewModel(
          wordRepository = container.wordRepository,
          settingsRepository = container.settingsRepository,
          savedGameStore = container.savedGameStore,
          historyRepository = container.historyRepository,
          applicationScope = container.applicationScope,
          clock = container.clock,
          awaitStartup = container::awaitStartup,
        )
      }
    }
  }
}
