package com.deezus.wordy.core

import com.deezus.wordy.core.LetterMark.Absent
import com.deezus.wordy.core.LetterMark.Correct
import com.deezus.wordy.core.LetterMark.Present
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

  private val dictionary = setOf(
    "crane", "crank", "slate", "plate", "llama", "table", "abbey", "kebab", "geese", "eerie",
    "trace", "brain", "moist", "pound", "fizzy", "jumpy",
  )
  private val isValidWord: (String) -> Boolean = { it in dictionary }

  private fun game(answer: String, vararg guesses: String) =
    GameEngine.newGame(GameMode.FiveLetters, answer).copy(guesses = guesses.toList())

  private fun GameState.typing(text: String) = copy(currentGuess = text)

  private fun GameState.submit(autoComplete: Boolean = true) =
    GameEngine.submit(this, autoComplete, isValidWord)

  // Marking guesses

  @Test
  fun `marks correct, present and absent letters`() {
    assertEquals(
      listOf(Absent, Correct, Correct, Present, Correct),
      GameEngine.evaluateGuess("trace", "crane"),
    )
  }

  @Test
  fun `a repeated letter is only present as many times as the answer has it`() {
    // "table" has one L, so only the first L of "llama" is present.
    assertEquals(
      listOf(Present, Absent, Present, Absent, Absent),
      GameEngine.evaluateGuess("llama", "table"),
    )
  }

  @Test
  fun `a correct letter uses up the answer's copy before other positions can claim it`() {
    // The E at the end is correct, leaving no E for the two earlier ones.
    assertEquals(
      listOf(Absent, Absent, Absent, Absent, Correct),
      GameEngine.evaluateGuess("geese", "crane"),
    )
  }

  @Test
  fun `repeated letters in the answer can each be matched`() {
    assertEquals(
      listOf(Absent, Present, Correct, Present, Present),
      GameEngine.evaluateGuess("kebab", "abbey"),
    )
  }

  @Test
  fun `key marks keep the most informative result for each letter`() {
    val marks = GameEngine.keyMarks(game("table", "llama", "plate"))

    assertEquals(Correct, marks['e'])
    assertEquals(Present, marks['l'])
    assertEquals(Present, marks['a'])
    assertEquals(Absent, marks['m'])
    assertEquals(Absent, marks['p'])
    assertNull(marks['z'])
  }

  @Test
  fun `key marks include letters ruled out by hints`() {
    val state = game("crane").copy(hintedAbsentLetters = setOf('z', 'q'))

    assertEquals(mapOf('z' to Absent, 'q' to Absent), GameEngine.keyMarks(state))
  }

  // Typing

  @Test
  fun `typing appends lowercase letters up to the word length`() {
    var state = game("crane")
    "CRANES".forEach { state = GameEngine.typeLetter(state, it) }

    assertEquals("crane", state.currentGuess)
  }

  @Test
  fun `typing ignores anything that is not a letter`() {
    val state = game("crane")

    assertSame(state, GameEngine.typeLetter(state, '1'))
    assertSame(state, GameEngine.typeLetter(state, ' '))
  }

  @Test
  fun `deleting removes the last letter and is a no-op when empty`() {
    assertEquals("cr", GameEngine.deleteLetter(game("crane").typing("cra")).currentGuess)

    val empty = game("crane")
    assertSame(empty, GameEngine.deleteLetter(empty))
  }

  @Test
  fun `a finished game ignores typing`() {
    val won = game("crane", "crane").copy(status = GameStatus.Won)

    assertSame(won, GameEngine.typeLetter(won, 'a'))
    assertSame(won, GameEngine.deleteLetter(won))
  }

  // Submitting

  @Test
  fun `a short guess is rejected`() {
    assertEquals(SubmitResult.TooShort, game("crane").typing("cra").submit())
  }

  @Test
  fun `an unknown word is rejected`() {
    assertEquals(SubmitResult.NotAWord, game("crane").typing("zzzzz").submit())
  }

  @Test
  fun `a valid wrong guess is added to the board`() {
    val result = game("crane").typing("moist").submit() as SubmitResult.Accepted

    assertEquals(listOf("moist"), result.state.guesses)
    assertEquals("", result.state.currentGuess)
    assertEquals(GameStatus.InProgress, result.state.status)
    assertNull(result.replacedAnswer)
  }

  @Test
  fun `guessing the answer wins`() {
    val result = game("crane", "moist").typing("crane").submit() as SubmitResult.Accepted

    assertEquals(GameStatus.Won, result.state.status)
    assertEquals(listOf("moist", "crane"), result.state.guesses)
  }

  @Test
  fun `a wrong sixth guess loses`() {
    val state = game("crane", "moist", "pound", "fizzy", "jumpy", "brain").typing("slate")
    val result = state.submit() as SubmitResult.Accepted

    assertEquals(GameStatus.Lost, result.state.status)
    assertEquals(6, result.state.guesses.size)
  }

  @Test
  fun `a finished game rejects further guesses`() {
    val won = game("crane", "crane").copy(status = GameStatus.Won)

    assertEquals(SubmitResult.GameOver, won.typing("slate").submit())
  }

  // Close enough

  @Test
  fun `a valid word one letter off becomes the answer and wins`() {
    val result = game("crane", "moist").typing("crank").submit() as SubmitResult.Accepted

    assertEquals(GameStatus.Won, result.state.status)
    assertEquals("crank", result.state.answer)
    assertEquals("crane", result.replacedAnswer)
  }

  @Test
  fun `a near miss does not count when the differing letter was already guessed`() {
    // "kebab" already showed the player that K is not in the answer.
    val state = game("crane", "kebab").typing("crank")
    val result = state.submit() as SubmitResult.Accepted

    assertEquals(GameStatus.InProgress, result.state.status)
    assertEquals("crane", result.state.answer)
  }

  @Test
  fun `a near miss does not count when a hint ruled the letter out`() {
    val state = game("crane").copy(hintedAbsentLetters = setOf('k')).typing("crank")

    assertFalse(GameEngine.isCloseEnough(state, "crank"))
  }

  @Test
  fun `a near miss does not count inside the prefix revealed by a hint`() {
    val revealed = game("plate").copy(revealedPrefixLength = 1)
    assertFalse(GameEngine.isCloseEnough(revealed, "slate"))

    val notRevealed = game("plate")
    assertTrue(GameEngine.isCloseEnough(notRevealed, "slate"))
  }

  @Test
  fun `two letters off is not close enough`() {
    assertFalse(GameEngine.isCloseEnough(game("crane"), "trace"))
  }

  // Scoring

  @Test
  fun `score falls by one for each extra guess`() {
    val scores = (1..6).map { count ->
      val guesses = List(count - 1) { "moist" } + "crane"
      GameEngine.score(game("crane", *guesses.toTypedArray()).copy(status = GameStatus.Won))
    }

    assertEquals(listOf(6, 5, 4, 3, 2, 1), scores)
  }

  @Test
  fun `only wins score points`() {
    assertEquals(0, GameEngine.score(game("crane", "moist").copy(status = GameStatus.Skipped)))
    assertEquals(0, GameEngine.score(game("crane", "moist").copy(status = GameStatus.Lost)))
    assertEquals(0, GameEngine.score(game("crane", "moist")))
  }

  // Auto-complete

  @Test
  fun `auto-complete needs every remaining letter to be known`() {
    // "brain" gives nothing at the end; "plate" then confirms only the final E.
    val state = game("crane", "brain", "plate")

    assertNull(GameEngine.autoCompletion(state.typing("cra")))
    assertEquals("e", GameEngine.autoCompletion(state.typing("cran")))
  }

  @Test
  fun `auto-complete fills a known ending across several guesses`() {
    // "trace" confirms R, A and E; "crank" confirms C, R, A and N. Together the whole word.
    val state = game("crane", "trace", "crank")

    assertEquals("crane", GameEngine.autoCompletion(state))
    assertEquals("ane", GameEngine.autoCompletion(state.typing("cr")))
    assertEquals("crane", GameEngine.pendingGuess(state.typing("cr"), autoCompleteEnabled = true))
    assertEquals("cr", GameEngine.pendingGuess(state.typing("cr"), autoCompleteEnabled = false))
  }

  @Test
  fun `auto-complete is off before the first guess and once the row is full`() {
    assertNull(GameEngine.autoCompletion(game("crane").typing("cran")))
    assertNull(GameEngine.autoCompletion(game("crane", "trace", "crank").typing("crane")))
  }

  @Test
  fun `submitting uses the auto-completed word only when the setting is on`() {
    val state = game("crane", "trace", "crank").typing("cr")

    assertEquals(GameStatus.Won, (state.submit(autoComplete = true) as SubmitResult.Accepted).state.status)
    assertEquals(SubmitResult.TooShort, state.submit(autoComplete = false))
  }

  // Skipping

  @Test
  fun `skipping ends the game and clears the current guess`() {
    val skipped = GameEngine.skip(game("crane", "moist").typing("cr"))

    assertEquals(GameStatus.Skipped, skipped.status)
    assertEquals("", skipped.currentGuess)
  }

  // Hints

  @Test
  fun `revealing the next letter keeps the correct start of the current guess`() {
    val hinted = GameEngine.revealNextLetter(game("crane").typing("crxyz"))!!

    assertEquals("cra", hinted.currentGuess)
    assertEquals(3, hinted.revealedPrefixLength)
    assertTrue(hinted.usedHint)
  }

  @Test
  fun `revealing the next letter starts from the first letter when nothing typed is right`() {
    val hinted = GameEngine.revealNextLetter(game("crane").typing("xy"))!!

    assertEquals("c", hinted.currentGuess)
    assertEquals(1, hinted.revealedPrefixLength)
  }

  @Test
  fun `the revealed prefix never shrinks`() {
    val state = game("crane").copy(revealedPrefixLength = 3).typing("x")

    assertEquals(3, GameEngine.revealNextLetter(state)!!.revealedPrefixLength)
  }

  @Test
  fun `no letter can be revealed once the whole answer is typed`() {
    assertNull(GameEngine.revealNextLetter(game("crane").typing("crane")))
  }

  @Test
  fun `ruling out a letter never picks one that is in the answer or already known`() {
    var state = game("crane", "moist")
    val known = "cranemoist".toSet()

    repeat(26 - known.size) {
      state = GameEngine.revealAbsentLetter(state, Random(it))!!
    }

    assertEquals(26 - known.size, state.hintedAbsentLetters.size)
    assertTrue(state.hintedAbsentLetters.none { it in known })
    assertTrue(state.usedHint)
    assertNull(GameEngine.revealAbsentLetter(state))
  }

  @Test
  fun `ruling out every letter leaves only letters the player has information about`() {
    val hinted = GameEngine.revealAllAbsentLetters(game("crane", "moist"))!!

    assertEquals(('a'..'z').toSet() - "cranemoist".toSet(), hinted.hintedAbsentLetters)
    assertNull(GameEngine.revealAllAbsentLetters(hinted))
  }

  @Test
  fun `an unavailable hint does not mark the game as hinted`() {
    val state = game("crane").typing("crane")

    assertNull(GameEngine.revealNextLetter(state))
    assertFalse(state.usedHint)
  }

  @Test
  fun `hints are unavailable once the game is over`() {
    val won = game("crane", "crane").copy(status = GameStatus.Won)

    assertNull(GameEngine.revealNextLetter(won))
    assertNull(GameEngine.revealAbsentLetter(won))
    assertNull(GameEngine.revealAllAbsentLetters(won))
  }
}
