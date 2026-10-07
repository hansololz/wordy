package com.deezus.wordy.ui.game

import com.deezus.wordy.core.GameEngine
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.core.GameStatus
import com.deezus.wordy.data.settings.ScoreDisplay
import com.deezus.wordy.data.settings.ScoreStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameUiStateTest {

  private fun game(answer: String, vararg guesses: String) =
    GameEngine.newGame(GameMode.FiveLetters, answer).copy(guesses = guesses.toList())

  @Test
  fun `board has six rows as wide as the word`() {
    val rows = boardRows(GameEngine.newGame(GameMode.FourLetters, "word"), autoCompleteEnabled = true)

    assertEquals(6, rows.size)
    assertTrue(rows.all { it.tiles.size == 4 })
  }

  @Test
  fun `submitted rows are marked and can be looked up`() {
    val rows = boardRows(game("crane", "trace"), autoCompleteEnabled = true)

    assertEquals("trace", rows[0].submittedWord)
    assertEquals(
      listOf(TileStyle.Absent, TileStyle.Correct, TileStyle.Correct, TileStyle.Present, TileStyle.Correct),
      rows[0].tiles.map { it.style },
    )
    assertNull(rows[1].submittedWord)
  }

  @Test
  fun `the cursor sits after the typed letters on the current row`() {
    val rows = boardRows(game("crane", "moist").copy(currentGuess = "cr"), autoCompleteEnabled = true)

    val current = rows[1].tiles
    assertEquals(listOf('c', 'r', null, null, null), current.map { it.letter })
    assertEquals(listOf(false, false, true, false, false), current.map { it.isCursor })
    assertEquals(TileStyle.Typed, current[0].style)
    assertEquals(TileStyle.Empty, current[2].style)
  }

  @Test
  fun `a full row has no cursor`() {
    val rows = boardRows(game("crane").copy(currentGuess = "slate"), autoCompleteEnabled = true)

    assertTrue(rows[0].tiles.none { it.isCursor })
  }

  @Test
  fun `auto-completed letters are shown only when the setting is on`() {
    val state = game("crane", "trace", "crank").copy(currentGuess = "cr")

    val completed = boardRows(state, autoCompleteEnabled = true)[2].tiles
    assertEquals(listOf('c', 'r', 'a', 'n', 'e'), completed.map { it.letter })
    assertEquals(TileStyle.AutoCompleted, completed[2].style)
    assertTrue(completed[2].isCursor)

    val plain = boardRows(state, autoCompleteEnabled = false)[2].tiles
    assertEquals(listOf('c', 'r', null, null, null), plain.map { it.letter })
  }

  @Test
  fun `a finished game shows no cursor or pending row`() {
    val won = game("crane", "crane").copy(status = GameStatus.Won)
    val rows = boardRows(won, autoCompleteEnabled = true)

    assertTrue(rows.drop(1).all { row -> row.tiles.all { it == Tile() } })
  }

  @Test
  fun `submit state follows the pending guess`() {
    val isValidWord: (String) -> Boolean = { it == "slate" }
    val state = game("crane")

    assertEquals(SubmitState.Incomplete, submitState(state.copy(currentGuess = "sla"), true, isValidWord))
    assertEquals(SubmitState.Ready, submitState(state.copy(currentGuess = "slate"), true, isValidWord))
    assertEquals(SubmitState.Ready, submitState(state.copy(currentGuess = "crane"), true, isValidWord))
    assertEquals(SubmitState.NotAWord, submitState(state.copy(currentGuess = "zzzzz"), true, isValidWord))
    assertEquals(
      SubmitState.GameOver,
      submitState(state.copy(status = GameStatus.Lost), true, isValidWord),
    )
  }

  @Test
  fun `scores are formatted for each display option`() {
    val stats = ScoreStats(
      totalScore = 11,
      gamesWon = 3,
      totalScoreWithoutHints = 6,
      gamesWonWithoutHints = 1,
    )

    assertEquals("11", formatScore(ScoreDisplay.Total, stats))
    assertEquals("3.7", formatScore(ScoreDisplay.Average, stats).replace(',', '.'))
    assertEquals("6", formatScore(ScoreDisplay.TotalWithoutHints, stats))
    assertEquals("6.0", formatScore(ScoreDisplay.AverageWithoutHints, stats).replace(',', '.'))
    assertEquals("0", formatScore(ScoreDisplay.Average, ScoreStats()))
  }
}
