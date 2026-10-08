package com.deezus.wordy.ui.game

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.deezus.wordy.core.GameEngine
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.core.GameState
import com.deezus.wordy.core.GameStatus
import com.deezus.wordy.data.db.GameOutcome
import com.deezus.wordy.data.db.WordyDatabase
import com.deezus.wordy.data.game.SavedGameStore
import com.deezus.wordy.data.game.SavedGames
import com.deezus.wordy.data.history.HistoryRepository
import com.deezus.wordy.data.settings.ScoreStats
import com.deezus.wordy.data.settings.SettingsRepository
import com.deezus.wordy.data.words.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Drives the view model against real repositories backed by an in-memory database. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GameViewModelTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val backgroundScope = CoroutineScope(Dispatchers.IO + Job())

  private lateinit var database: WordyDatabase
  private lateinit var settingsRepository: SettingsRepository
  private lateinit var savedGameStore: SavedGameStore
  private lateinit var wordRepository: WordRepository
  private lateinit var historyRepository: HistoryRepository

  private val wordLists = mapOf(
    GameMode.FourLetters to setOf("word", "game"),
    GameMode.FiveLetters to setOf("crane", "slate", "moist", "pound", "fizzy", "jumpy", "brain"),
    GameMode.SixLetters to setOf("planet", "garden"),
  )

  @Before
  fun setUp() = runBlocking {
    Dispatchers.setMain(UnconfinedTestDispatcher())

    database = Room.inMemoryDatabaseBuilder(context, WordyDatabase::class.java).build()
    settingsRepository =
      SettingsRepository(SettingsRepository.createDataStore(context, backgroundScope), backgroundScope)
    savedGameStore =
      SavedGameStore(SavedGameStore.createDataStore(context, backgroundScope), backgroundScope)
    wordRepository = WordRepository(database.usedWordDao(), backgroundScope)
    wordRepository.initialize(wordLists)
    historyRepository = HistoryRepository(database.gameRecordDao())
  }

  @After
  fun tearDown() = runBlocking {
    // Lets background writes stop before the database they target is closed.
    backgroundScope.cancel()
    backgroundScope.coroutineContext.job.join()
    database.close()
    Dispatchers.resetMain()
  }

  private fun createViewModel() = GameViewModel(
    wordRepository = wordRepository,
    settingsRepository = settingsRepository,
    savedGameStore = savedGameStore,
    historyRepository = historyRepository,
    applicationScope = backgroundScope,
    clock = { 1_000L },
    awaitStartup = {},
  )

  private fun savedGame(answer: String, vararg guesses: String): GameState =
    GameEngine.newGame(GameMode.FiveLetters, answer).copy(guesses = guesses.toList())

  private suspend fun GameViewModel.awaitState(
    predicate: (GameUiState.Ready) -> Boolean = { true },
  ): GameUiState.Ready = withTimeout(5_000) {
    uiState.first { it is GameUiState.Ready && predicate(it) } as GameUiState.Ready
  }

  private fun GameViewModel.type(word: String) = word.forEach(::onLetter)

  @Test
  fun `a first launch starts a five letter game`() = runBlocking {
    val state = createViewModel().awaitState()

    assertEquals(GameMode.FiveLetters, state.mode)
    assertTrue(state.isActive)
    assertTrue(state.rows.all { it.submittedWord == null })
    assertNotNullSaved(GameMode.FiveLetters)
  }

  @Test
  fun `a saved game is restored`() = runBlocking {
    savedGameStore.save(SavedGames().with(savedGame("crane", "moist").copy(currentGuess = "sl")))

    val state = createViewModel().awaitState()

    assertEquals("moist", state.rows[0].submittedWord)
    assertEquals(listOf('s', 'l', null, null, null), state.rows[1].tiles.map { it.letter })
  }

  @Test
  fun `winning shows the result and records score, history and the used word`() = runBlocking {
    savedGameStore.save(SavedGames().with(savedGame("crane", "moist")))
    val viewModel = createViewModel()
    viewModel.awaitState()

    viewModel.type("crane")
    viewModel.onSubmit()

    val state = viewModel.awaitState { it.dialog != null }
    assertEquals(GameDialog.Finished(GameStatus.Won, "crane", 5), state.dialog)
    assertFalse(state.isActive)

    val stats = withTimeout(5_000) { settingsRepository.stats.first { it.gamesWon == 1L } }
    assertEquals(ScoreStats(5, 1, 5, 1), stats)

    val entry = withTimeout(5_000) { historyRepository.history.first { it.isNotEmpty() } }.single()
    assertEquals("crane", entry.word)
    assertEquals(GameOutcome.Won, entry.outcome)
    assertEquals(5, entry.score)
    assertEquals(listOf("moist", "crane"), entry.guesses)
    assertEquals(1_000L, entry.finishedAt)

    awaitUsedWords(setOf("crane"))
  }

  @Test
  fun `play again starts a new game that avoids the word just played`() = runBlocking {
    savedGameStore.save(SavedGames().with(savedGame("crane")))
    val viewModel = createViewModel()
    viewModel.awaitState()
    viewModel.type("crane")
    viewModel.onSubmit()
    viewModel.awaitState { it.dialog is GameDialog.Finished }

    viewModel.onPlayAgain()

    val state = viewModel.awaitState { it.dialog == null && it.isActive }
    assertTrue(state.rows.all { it.submittedWord == null })
    assertTrue(savedGameStore.load()[GameMode.FiveLetters]!!.answer != "crane")
  }

  @Test
  fun `dismissing the result keeps the finished board until next is pressed`() = runBlocking {
    savedGameStore.save(SavedGames().with(savedGame("crane")))
    val viewModel = createViewModel()
    viewModel.awaitState()
    viewModel.type("crane")
    viewModel.onSubmit()
    viewModel.awaitState { it.dialog is GameDialog.Finished }

    viewModel.onDialogDismissed()
    val finished = viewModel.awaitState { it.dialog == null }
    assertEquals("crane", finished.rows[0].submittedWord)
    assertEquals(SubmitState.GameOver, finished.submitState)

    viewModel.onSkipOrNext()
    assertTrue(viewModel.awaitState { it.isActive }.rows.all { it.submittedWord == null })
  }

  @Test
  fun `skipping asks first, then records the skip without scoring`() = runBlocking {
    savedGameStore.save(SavedGames().with(savedGame("crane", "moist")))
    val viewModel = createViewModel()
    viewModel.awaitState()

    viewModel.onSkipOrNext()
    assertEquals(GameDialog.ConfirmSkip, viewModel.awaitState { it.dialog != null }.dialog)

    viewModel.onSkipConfirmed()
    val state = viewModel.awaitState { it.dialog is GameDialog.Finished }
    assertEquals(GameDialog.Finished(GameStatus.Skipped, "crane", 0), state.dialog)

    val entry = withTimeout(5_000) { historyRepository.history.first { it.isNotEmpty() } }.single()
    assertEquals(GameOutcome.Skipped, entry.outcome)
    assertEquals(ScoreStats(), settingsRepository.stats.first())
  }

  @Test
  fun `cancelling a skip leaves the game untouched`() = runBlocking {
    savedGameStore.save(SavedGames().with(savedGame("crane", "moist")))
    val viewModel = createViewModel()
    viewModel.awaitState()

    viewModel.onSkipOrNext()
    viewModel.awaitState { it.dialog == GameDialog.ConfirmSkip }
    viewModel.onDialogDismissed()

    val state = viewModel.awaitState { it.dialog == null }
    assertTrue(state.isActive)
    assertEquals("moist", state.rows[0].submittedWord)
  }

  @Test
  fun `a wrong sixth guess loses and is recorded as lost`() = runBlocking {
    savedGameStore.save(
      SavedGames().with(savedGame("crane", "moist", "pound", "fizzy", "jumpy", "brain"))
    )
    val viewModel = createViewModel()
    viewModel.awaitState()

    viewModel.type("slate")
    viewModel.onSubmit()

    val state = viewModel.awaitState { it.dialog is GameDialog.Finished }
    assertEquals(GameDialog.Finished(GameStatus.Lost, "crane", 0), state.dialog)
    val entry = withTimeout(5_000) { historyRepository.history.first { it.isNotEmpty() } }.single()
    assertEquals(GameOutcome.Lost, entry.outcome)
  }

  @Test
  fun `switching word length keeps each game where it was left`() = runBlocking {
    savedGameStore.save(SavedGames().with(savedGame("crane", "moist")))
    val viewModel = createViewModel()
    viewModel.awaitState()

    settingsRepository.setMode(GameMode.FourLetters)
    val fourLetters = viewModel.awaitState { it.mode == GameMode.FourLetters }
    assertEquals(4, fourLetters.rows[0].tiles.size)
    assertTrue(fourLetters.rows.all { it.submittedWord == null })

    settingsRepository.setMode(GameMode.FiveLetters)
    val fiveLetters = viewModel.awaitState { it.mode == GameMode.FiveLetters }
    assertEquals("moist", fiveLetters.rows[0].submittedWord)
  }

  @Test
  fun `an incomplete guess explains what is missing`() = runBlocking {
    savedGameStore.save(SavedGames().with(savedGame("crane")))
    val viewModel = createViewModel()
    viewModel.awaitState()

    viewModel.type("cr")
    viewModel.onSubmit()

    val message = viewModel.awaitState { it.message != null }.message!!
    assertEquals(5, (message as GameMessage.WordTooShort).wordLength)

    viewModel.onMessageShown(message)
    assertNull(viewModel.awaitState { it.message == null }.message)
  }

  @Test
  fun `a hint that cannot be given reports it and does not count as a hint`() = runBlocking {
    savedGameStore.save(SavedGames().with(savedGame("crane").copy(currentGuess = "crane")))
    val viewModel = createViewModel()
    viewModel.awaitState()

    viewModel.onHint(HintType.RevealNextLetter)

    assertTrue(viewModel.awaitState { it.message != null }.message is GameMessage.NoHintAvailable)
    assertFalse(savedGameStore.load()[GameMode.FiveLetters]!!.usedHint)
  }

  @Test
  fun `a hinted win is left out of the no-hint totals`() = runBlocking {
    savedGameStore.save(SavedGames().with(savedGame("crane")))
    val viewModel = createViewModel()
    viewModel.awaitState()

    viewModel.onHint(HintType.RevealNextLetter)
    viewModel.type("rane")
    viewModel.onSubmit()
    viewModel.awaitState { it.dialog is GameDialog.Finished }

    val stats = withTimeout(5_000) { settingsRepository.stats.first { it.gamesWon == 1L } }
    assertEquals(ScoreStats(totalScore = 6, gamesWon = 1), stats)
  }

  private suspend fun assertNotNullSaved(mode: GameMode) {
    assertTrue(savedGameStore.load()[mode] != null)
  }

  private suspend fun awaitUsedWords(expected: Set<String>) = withTimeout(5_000) {
    while (database.usedWordDao().getAll().map { it.word }.toSet() != expected) delay(20)
  }
}
