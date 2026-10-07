package com.deezus.wordy.ui.game

import com.deezus.wordy.core.GameEngine
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.core.GameState
import com.deezus.wordy.core.GameStatus
import com.deezus.wordy.core.LetterMark
import com.deezus.wordy.core.MAX_GUESSES
import com.deezus.wordy.data.settings.ScoreDisplay
import com.deezus.wordy.data.settings.ScoreStats

sealed interface GameUiState {
  data object Loading : GameUiState

  data class Ready(
    val mode: GameMode,
    val rows: List<BoardRow>,
    val keyMarks: Map<Char, LetterMark>,
    val isActive: Boolean,
    val submitState: SubmitState,
    val scoreDisplay: ScoreDisplay,
    val stats: ScoreStats,
    val hapticsEnabled: Boolean,
    val dialog: GameDialog? = null,
    val message: GameMessage? = null,
    val reviewRequested: Boolean = false,
  ) : GameUiState
}

data class BoardRow(
  val tiles: List<Tile>,
  /** The guess in this row once submitted, so its definition can be looked up. */
  val submittedWord: String? = null,
)

data class Tile(
  val letter: Char? = null,
  val style: TileStyle = TileStyle.Empty,
  /** Whether the next typed letter lands here. */
  val isCursor: Boolean = false,
)

enum class TileStyle { Empty, Typed, AutoCompleted, Correct, Present, Absent }

enum class SubmitState { Incomplete, Ready, NotAWord, GameOver }

sealed interface GameDialog {
  data object ConfirmSkip : GameDialog
  data class Finished(val status: GameStatus, val answer: String, val score: Int) : GameDialog
}

/** A one-off notice. [id] makes repeats of the same notice distinct. */
sealed interface GameMessage {
  val id: Long

  data class WordTooShort(override val id: Long, val wordLength: Int) : GameMessage
  data class NoHintAvailable(override val id: Long) : GameMessage
}

enum class HintType { RevealNextLetter, RevealAbsentLetter, RevealAllAbsentLetters }

/** Lays the game out as the grid of tiles shown on screen. */
fun boardRows(game: GameState, autoCompleteEnabled: Boolean): List<BoardRow> {
  val completion = if (autoCompleteEnabled) GameEngine.autoCompletion(game).orEmpty() else ""

  return List(MAX_GUESSES) { row ->
    when {
      row < game.guesses.size -> {
        val guess = game.guesses[row]
        val marks = GameEngine.evaluateGuess(guess, game.answer)
        BoardRow(
          tiles = guess.mapIndexed { index, letter -> Tile(letter, marks[index].toTileStyle()) },
          submittedWord = guess,
        )
      }
      row == game.guesses.size && game.isActive -> {
        val typed = game.currentGuess
        BoardRow(
          tiles = List(game.wordLength) { column ->
            when {
              column < typed.length -> Tile(typed[column], TileStyle.Typed)
              column - typed.length < completion.length -> Tile(
                letter = completion[column - typed.length],
                style = TileStyle.AutoCompleted,
                isCursor = column == typed.length,
              )
              else -> Tile(isCursor = column == typed.length)
            }
          }
        )
      }
      else -> BoardRow(List(game.wordLength) { Tile() })
    }
  }
}

fun submitState(
  game: GameState,
  autoCompleteEnabled: Boolean,
  isValidWord: (String) -> Boolean,
): SubmitState {
  if (!game.isActive) return SubmitState.GameOver
  val guess = GameEngine.pendingGuess(game, autoCompleteEnabled)
  return when {
    guess.length < game.wordLength -> SubmitState.Incomplete
    guess == game.answer || isValidWord(guess) -> SubmitState.Ready
    else -> SubmitState.NotAWord
  }
}

private fun LetterMark.toTileStyle(): TileStyle = when (this) {
  LetterMark.Correct -> TileStyle.Correct
  LetterMark.Present -> TileStyle.Present
  LetterMark.Absent -> TileStyle.Absent
}
