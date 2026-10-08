package com.deezus.wordy.core

import kotlin.random.Random

sealed interface SubmitResult {
  /** The guess was accepted. [replacedAnswer] is set when a near miss became the new answer. */
  data class Accepted(val state: GameState, val replacedAnswer: String? = null) : SubmitResult
  data object TooShort : SubmitResult
  data object NotAWord : SubmitResult
  data object GameOver : SubmitResult
}

/** The rules of the game as pure functions over [GameState]. */
object GameEngine {

  private val alphabet = 'a'..'z'

  fun newGame(mode: GameMode, answer: String): GameState {
    require(answer.length == mode.wordLength) { "Answer must have ${mode.wordLength} letters" }
    return GameState(mode = mode, answer = answer.lowercase())
  }

  fun typeLetter(state: GameState, letter: Char): GameState {
    val lower = letter.lowercaseChar()
    if (!state.isActive || lower !in alphabet || state.currentGuess.length >= state.wordLength) {
      return state
    }
    return state.copy(currentGuess = state.currentGuess + lower)
  }

  fun deleteLetter(state: GameState): GameState {
    if (!state.isActive || state.currentGuess.isEmpty()) return state
    return state.copy(currentGuess = state.currentGuess.dropLast(1))
  }

  /**
   * Marks each letter of [guess] against [answer]. A letter is only [LetterMark.Present] while the
   * answer still has an unmatched copy of it, so repeated letters are never over-reported.
   */
  fun evaluateGuess(guess: String, answer: String): List<LetterMark> {
    require(guess.length == answer.length) { "Guess and answer must be the same length" }

    val marks = MutableList(guess.length) { LetterMark.Absent }
    val unmatched = HashMap<Char, Int>()

    for (i in guess.indices) {
      if (guess[i] == answer[i]) {
        marks[i] = LetterMark.Correct
      } else {
        unmatched[answer[i]] = (unmatched[answer[i]] ?: 0) + 1
      }
    }

    for (i in guess.indices) {
      if (marks[i] == LetterMark.Correct) continue
      val remaining = unmatched[guess[i]] ?: 0
      if (remaining > 0) {
        marks[i] = LetterMark.Present
        unmatched[guess[i]] = remaining - 1
      }
    }

    return marks
  }

  /** The best known mark for every letter that has been guessed or ruled out by a hint. */
  fun keyMarks(state: GameState): Map<Char, LetterMark> {
    val marks = HashMap<Char, LetterMark>()
    state.hintedAbsentLetters.forEach { marks[it] = LetterMark.Absent }

    for (guess in state.guesses) {
      evaluateGuess(guess, state.answer).forEachIndexed { index, mark ->
        val letter = guess[index]
        val known = marks[letter]
        if (known == null || mark > known) marks[letter] = mark
      }
    }

    return marks
  }

  /**
   * The letters that complete the current guess when everything after it is already known, i.e.
   * each remaining position was marked correct in an earlier guess. Null when nothing can be
   * completed.
   */
  fun autoCompletion(state: GameState): String? {
    val typed = state.currentGuess.length
    if (!state.isActive || state.guesses.isEmpty() || typed >= state.wordLength) return null

    var knownFrom = state.wordLength
    while (knownFrom > 0 && state.guesses.any { it[knownFrom - 1] == state.answer[knownFrom - 1] }) {
      knownFrom--
    }

    return if (typed >= knownFrom) state.answer.substring(typed) else null
  }

  /** The word that would be submitted right now, including any auto-completed letters. */
  fun pendingGuess(state: GameState, autoCompleteEnabled: Boolean): String {
    val completion = if (autoCompleteEnabled) autoCompletion(state).orEmpty() else ""
    return state.currentGuess + completion
  }

  fun submit(
    state: GameState,
    autoCompleteEnabled: Boolean,
    isValidWord: (String) -> Boolean,
  ): SubmitResult {
    if (!state.isActive) return SubmitResult.GameOver

    val guess = pendingGuess(state, autoCompleteEnabled)

    return when {
      guess.length < state.wordLength -> SubmitResult.TooShort
      guess == state.answer -> SubmitResult.Accepted(state.withGuess(guess, GameStatus.Won))
      !isValidWord(guess) -> SubmitResult.NotAWord
      isCloseEnough(state, guess) -> SubmitResult.Accepted(
        state = state.copy(answer = guess).withGuess(guess, GameStatus.Won),
        replacedAnswer = state.answer,
      )
      state.guesses.size + 1 >= MAX_GUESSES -> {
        SubmitResult.Accepted(state.withGuess(guess, GameStatus.Lost))
      }
      else -> SubmitResult.Accepted(state.withGuess(guess, GameStatus.InProgress))
    }
  }

  /**
   * A valid word that is one letter away from the answer is accepted as a win, as long as the
   * player had no information about the differing letter: it must not sit in a position revealed by
   * a hint or already marked correct in an earlier guess, be a letter a hint ruled out, or have
   * appeared in an earlier guess.
   */
  fun isCloseEnough(state: GameState, guess: String): Boolean {
    if (guess.length != state.answer.length) return false

    val differing = guess.indices.filter { guess[it] != state.answer[it] }
    val index = differing.singleOrNull() ?: return false
    val letter = guess[index]

    return index >= state.revealedPrefixLength &&
      letter !in state.hintedAbsentLetters &&
      state.guesses.none { letter in it || it[index] == state.answer[index] }
  }

  fun skip(state: GameState): GameState {
    if (!state.isActive) return state
    return state.copy(status = GameStatus.Skipped, currentGuess = "")
  }

  /** Points for a finished game: 6 for a first-guess win down to 1 for the last, 0 otherwise. */
  fun score(state: GameState): Int {
    if (state.status != GameStatus.Won) return 0
    return (MAX_GUESSES - (state.guesses.size - 1)).coerceIn(0, MAX_GUESSES)
  }

  /**
   * Keeps the correct leading letters of the current guess and reveals the next one. Returns null
   * when the whole word is already typed correctly.
   */
  fun revealNextLetter(state: GameState): GameState? {
    if (!state.isActive) return null

    val correctPrefix = state.currentGuess
      .zip(state.answer)
      .takeWhile { (typed, expected) -> typed == expected }
      .size
    if (correctPrefix >= state.wordLength) return null

    val revealed = correctPrefix + 1
    return state.copy(
      currentGuess = state.answer.take(revealed),
      usedHint = true,
      revealedPrefixLength = maxOf(state.revealedPrefixLength, revealed),
    )
  }

  /** Rules out one random letter that is not in the answer. Null when none are left. */
  fun revealAbsentLetter(state: GameState, random: Random = Random.Default): GameState? {
    if (!state.isActive) return null
    val letter = unusedLetters(state).randomOrNull(random) ?: return null
    return state.copy(usedHint = true, hintedAbsentLetters = state.hintedAbsentLetters + letter)
  }

  /** Rules out every remaining letter that is not in the answer. Null when none are left. */
  fun revealAllAbsentLetters(state: GameState): GameState? {
    if (!state.isActive) return null
    val letters = unusedLetters(state)
    if (letters.isEmpty()) return null
    return state.copy(usedHint = true, hintedAbsentLetters = state.hintedAbsentLetters + letters)
  }

  /** Letters the player knows nothing about yet and that are not in the answer. */
  private fun unusedLetters(state: GameState): List<Char> {
    val known = HashSet<Char>()
    state.guesses.forEach { known.addAll(it.asIterable()) }
    known.addAll(state.answer.asIterable())
    known.addAll(state.hintedAbsentLetters)
    return alphabet.filter { it !in known }
  }

  private fun GameState.withGuess(guess: String, newStatus: GameStatus): GameState =
    copy(guesses = guesses + guess, currentGuess = "", status = newStatus)
}
