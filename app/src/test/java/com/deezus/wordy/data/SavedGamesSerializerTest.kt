package com.deezus.wordy.data

import androidx.datastore.core.CorruptionException
import com.deezus.wordy.core.GameEngine
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.core.GameStatus
import com.deezus.wordy.data.game.SavedGames
import com.deezus.wordy.data.game.SavedGamesSerializer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SavedGamesSerializerTest {

  @Test
  fun `saved games survive a round trip`() = runTest {
    val games = SavedGames()
      .with(
        GameEngine.newGame(GameMode.FiveLetters, "crane").copy(
          guesses = listOf("moist", "trace"),
          currentGuess = "cr",
          usedHint = true,
          hintedAbsentLetters = setOf('z', 'q'),
          revealedPrefixLength = 2,
        )
      )
      .with(GameEngine.newGame(GameMode.SixLetters, "planet").copy(status = GameStatus.Skipped))

    val output = ByteArrayOutputStream()
    SavedGamesSerializer.writeTo(games, output)
    val restored = SavedGamesSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

    assertEquals(games, restored)
  }

  @Test
  fun `word lengths are stored under their stable ids`() = runTest {
    val output = ByteArrayOutputStream()
    SavedGamesSerializer.writeTo(
      SavedGames().with(GameEngine.newGame(GameMode.FourLetters, "word")),
      output,
    )

    assertTrue(output.toString().contains("GUESS_4_ENGLISH"))
  }

  @Test
  fun `unreadable data is reported as corruption`() {
    assertThrows(CorruptionException::class.java) {
      kotlinx.coroutines.runBlocking {
        SavedGamesSerializer.readFrom(ByteArrayInputStream("not json".toByteArray()))
      }
    }
  }
}
