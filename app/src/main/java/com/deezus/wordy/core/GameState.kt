package com.deezus.wordy.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MAX_GUESSES = 6

/**
 * A playable word length. [id] is written to settings, history and saved games, so it must never
 * change; it also matches the identifiers used by app versions before 3.0.
 */
@Serializable
enum class GameMode(val id: String, val wordLength: Int) {
  @SerialName("GUESS_4_ENGLISH")
  FourLetters("GUESS_4_ENGLISH", 4),

  @SerialName("GUESS_5_ENGLISH")
  FiveLetters("GUESS_5_ENGLISH", 5),

  @SerialName("GUESS_6_ENGLISH")
  SixLetters("GUESS_6_ENGLISH", 6);

  companion object {
    val Default = FiveLetters

    fun fromId(id: String?): GameMode? = entries.firstOrNull { it.id == id }
  }
}

@Serializable
enum class GameStatus { InProgress, Won, Lost, Skipped }

/** How a single guessed letter compares with the answer. Ordered from least to most informative. */
enum class LetterMark { Absent, Present, Correct }

/** The complete state of one game. Immutable; every move produces a new instance via [GameEngine]. */
@Serializable
data class GameState(
  val mode: GameMode,
  val answer: String,
  val guesses: List<String> = emptyList(),
  val currentGuess: String = "",
  val status: GameStatus = GameStatus.InProgress,
  val usedHint: Boolean = false,
  /** Letters a hint has revealed as not being in the answer. */
  val hintedAbsentLetters: Set<Char> = emptySet(),
  /** How many leading letters of the answer a hint has revealed. */
  val revealedPrefixLength: Int = 0,
) {
  val wordLength: Int get() = mode.wordLength
  val isActive: Boolean get() = status == GameStatus.InProgress
}
