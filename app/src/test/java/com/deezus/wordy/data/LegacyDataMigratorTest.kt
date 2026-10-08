package com.deezus.wordy.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.data.db.GameOutcome
import com.deezus.wordy.data.db.WordyDatabase
import com.deezus.wordy.data.legacy.LegacyDataMigrator
import com.deezus.wordy.data.settings.ScoreDisplay
import com.deezus.wordy.data.settings.ScoreStats
import com.deezus.wordy.data.settings.SettingsRepository
import com.deezus.wordy.data.settings.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Builds the files a pre-3.0 install leaves behind and checks they are carried over. */
@RunWith(RobolectricTestRunner::class)
class LegacyDataMigratorTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())

  private lateinit var database: WordyDatabase
  private lateinit var settingsRepository: SettingsRepository
  private lateinit var migrator: LegacyDataMigrator

  private val wordLists = mapOf(
    GameMode.FourLetters to setOf("word", "game", "play"),
    GameMode.FiveLetters to setOf("crane", "slate", "moist", "pound"),
    GameMode.SixLetters to setOf("planet", "garden"),
  )

  @Before
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder(context, WordyDatabase::class.java).build()
  }

  @After
  fun tearDown() {
    database.close()
    dataStoreScope.cancel()
  }

  /** Created after the legacy SharedPreferences are written, as it would be on a real upgrade. */
  private fun createMigrator() {
    settingsRepository =
      SettingsRepository(SettingsRepository.createDataStore(context, dataStoreScope), dataStoreScope)
    migrator = LegacyDataMigrator(context, database, settingsRepository, Dispatchers.IO)
  }

  @Test
  fun `history is copied and unfinished games are dropped`() = runBlocking {
    legacyDatabase(LegacyDataMigrator.HISTORY_DATABASE, HISTORY_SCHEMA) { db ->
      db.insertHistory("crane", 3000, "GUESS_5_ENGLISH", "SUCCEEDED", 4, hinted = true, "moist, slate, crane")
      db.insertHistory("word", 2000, "GUESS_4_ENGLISH", "SKIPPED", 0, hinted = false, "game")
      db.insertHistory("planet", 1000, "GUESS_6_ENGLISH", "NOT_COMPLETED", 0, hinted = false, "")
    }
    createMigrator()

    migrator.migrateIfNeeded(wordLists)

    val records = database.gameRecordDao().observeRecent(10).first().map { it.record }
    assertEquals(listOf("crane", "word"), records.map { it.word })

    val won = records[0]
    assertEquals(GameOutcome.Won, won.outcome)
    assertEquals(4, won.score)
    assertEquals(3000L, won.finishedAt)
    assertEquals("GUESS_5_ENGLISH", won.mode)
    assertTrue(won.usedHint)
    assertEquals(listOf("moist", "slate", "crane"), won.guesses)

    assertEquals(GameOutcome.Skipped, records[1].outcome)
    assertFalse(records[1].usedHint)
  }

  @Test
  fun `a legacy skip with a full board is recorded as a loss`() = runBlocking {
    legacyDatabase(LegacyDataMigrator.HISTORY_DATABASE, HISTORY_SCHEMA) { db ->
      db.insertHistory(
        "crane", 1000, "GUESS_5_ENGLISH", "SKIPPED", 0, hinted = false,
        "moist, slate, pound, fizzy, jumpy, brain",
      )
    }
    createMigrator()

    migrator.migrateIfNeeded(wordLists)

    val record = database.gameRecordDao().observeRecent(10).first().single().record
    assertEquals(GameOutcome.Lost, record.outcome)
    assertEquals(6, record.guesses.size)
  }

  @Test
  fun `bookmarks are copied with their original times`() = runBlocking {
    legacyDatabase(LegacyDataMigrator.BOOKMARK_DATABASE, BOOKMARK_SCHEMA) { db ->
      db.execSQL("INSERT INTO BookmarkEntry (word, time) VALUES ('crane', 500), ('slate', 900)")
    }
    createMigrator()

    migrator.migrateIfNeeded(wordLists)

    val bookmarks = database.bookmarkDao().observeAll().first()
    assertEquals(listOf("slate" to 900L, "crane" to 500L), bookmarks.map { it.word to it.createdAt })
  }

  @Test
  fun `words missing from a legacy pool are marked as used`() = runBlocking {
    legacyPreferences().edit().putBoolean("SETTINGS_IS_WORD_SET_SAVED_ENGLISH_5", true).commit()
    legacyDatabase(LegacyDataMigrator.wordPoolDatabase(GameMode.FiveLetters), WORD_SCHEMA) { db ->
      db.execSQL("INSERT INTO WordEntry (word) VALUES ('slate'), ('pound')")
    }
    createMigrator()

    migrator.migrateIfNeeded(wordLists)

    val used = database.usedWordDao().getAll()
    assertEquals(setOf("crane", "moist"), used.map { it.word }.toSet())
    assertTrue(used.all { it.mode == "GUESS_5_ENGLISH" })
  }

  @Test
  fun `a pool that never finished seeding marks nothing as used`() = runBlocking {
    legacyDatabase(LegacyDataMigrator.wordPoolDatabase(GameMode.FiveLetters), WORD_SCHEMA) { db ->
      db.execSQL("INSERT INTO WordEntry (word) VALUES ('slate')")
    }
    createMigrator()

    migrator.migrateIfNeeded(wordLists)

    assertTrue(database.usedWordDao().getAll().isEmpty())
  }

  @Test
  fun `legacy databases are deleted and a second run changes nothing`() = runBlocking {
    legacyDatabase(LegacyDataMigrator.HISTORY_DATABASE, HISTORY_SCHEMA) { db ->
      db.insertHistory("crane", 3000, "GUESS_5_ENGLISH", "SUCCEEDED", 4, hinted = false, "crane")
    }
    legacyDatabase(LegacyDataMigrator.BOOKMARK_DATABASE, BOOKMARK_SCHEMA) {}
    legacyDatabase(LegacyDataMigrator.DICTIONARY_DATABASE, WORD_SCHEMA) {}
    GameMode.entries.forEach { legacyDatabase(LegacyDataMigrator.wordPoolDatabase(it), WORD_SCHEMA) {} }
    createMigrator()

    migrator.migrateIfNeeded(wordLists)
    migrator.migrateIfNeeded(wordLists)

    val legacyNames = listOf(
      LegacyDataMigrator.HISTORY_DATABASE,
      LegacyDataMigrator.BOOKMARK_DATABASE,
      LegacyDataMigrator.DICTIONARY_DATABASE,
    ) + GameMode.entries.map(LegacyDataMigrator::wordPoolDatabase)
    legacyNames.forEach { assertFalse("$it should be deleted", context.getDatabasePath(it).exists()) }

    assertEquals(1, database.gameRecordDao().observeRecent(10).first().size)
  }

  @Test
  fun `history copied twice is not duplicated`() = runBlocking {
    createMigrator()
    repeat(2) {
      // Simulates the app dying after the copy but before the legacy file was deleted.
      legacyDatabase(LegacyDataMigrator.HISTORY_DATABASE, HISTORY_SCHEMA) { db ->
        db.insertHistory("crane", 3000, "GUESS_5_ENGLISH", "SUCCEEDED", 4, hinted = false, "crane")
      }
      migrator.migrateIfNeeded(wordLists)
    }

    assertEquals(1, database.gameRecordDao().observeRecent(10).first().size)
  }

  @Test
  fun `a fresh install has nothing to migrate`() = runBlocking {
    createMigrator()

    migrator.migrateIfNeeded(wordLists)

    assertTrue(database.gameRecordDao().observeRecent(10).first().isEmpty())
    assertTrue(database.usedWordDao().getAll().isEmpty())
    assertEquals(UserSettings(), settingsRepository.settings.first())
  }

  @Test
  fun `scores and settings are read from the legacy preferences`() = runBlocking {
    legacyPreferences().edit()
      .putLong("SETTINGS_TOTAL_SCORE", 42)
      .putLong("SETTINGS_TOTAL_GAMES_WON", 10)
      .putLong("SETTINGS_TOTAL_SCORE_WITHOUT_HINT", 30)
      .putLong("SETTINGS_TOTAL_GAMES_WON_WITHOUT_HINT", 6)
      .putString("SETTINGS_SCORE_VIEW_PREFERENCE", "AVERAGE_SCORE_WITHOUT_HINTS")
      .putString("SETTINGS_CURRENT_GAME", "GUESS_6_ENGLISH")
      .putBoolean("SETTINGS_IS_HAPTIC_FEEDBACK_ENABLED", true)
      .putBoolean("SETTINGS_IS_AUTO_COMPLETE_ENABLED", false)
      .putLong("SETTINGS_ASK_REVIEW_TIME", Long.MAX_VALUE)
      .commit()
    createMigrator()

    assertEquals(
      ScoreStats(totalScore = 42, gamesWon = 10, totalScoreWithoutHints = 30, gamesWonWithoutHints = 6),
      settingsRepository.stats.first(),
    )
    assertEquals(
      UserSettings(
        mode = GameMode.SixLetters,
        scoreDisplay = ScoreDisplay.AverageWithoutHints,
        hapticsEnabled = true,
        autoCompleteEnabled = false,
      ),
      settingsRepository.settings.first(),
    )
    // The player chose never to be asked for a review; that choice must survive.
    assertFalse(settingsRepository.isReviewDue(now = System.currentTimeMillis()))
  }

  @Test
  fun `legacy outcomes map to current ones`() {
    assertEquals(GameOutcome.Won, LegacyDataMigrator.legacyOutcome("SUCCEEDED", 3))
    assertEquals(GameOutcome.Lost, LegacyDataMigrator.legacyOutcome("FAILED", 6))
    assertEquals(GameOutcome.Skipped, LegacyDataMigrator.legacyOutcome("SKIPPED", 5))
    assertEquals(GameOutcome.Lost, LegacyDataMigrator.legacyOutcome("SKIPPED", 6))
    assertNull(LegacyDataMigrator.legacyOutcome("NOT_COMPLETED", 0))
    assertNull(LegacyDataMigrator.legacyOutcome(null, 0))
  }

  private fun legacyPreferences() = context.getSharedPreferences("WORDY_SCORE", Context.MODE_PRIVATE)

  private fun legacyDatabase(name: String, schema: String, fill: (SQLiteDatabase) -> Unit) {
    val file = context.getDatabasePath(name)
    file.parentFile?.mkdirs()
    SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
      db.execSQL(schema)
      fill(db)
    }
  }

  private fun SQLiteDatabase.insertHistory(
    word: String,
    time: Long,
    gameName: String,
    outcome: String,
    score: Int,
    hinted: Boolean,
    guesses: String,
  ) {
    execSQL(
      "INSERT INTO HistoryEntry (word, time, gameName, outcome, scoreEarned, hinted, guesses) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)",
      arrayOf<Any>(word, time, gameName, outcome, score, if (hinted) 1 else 0, guesses),
    )
  }

  // The tables Room 2.4 generated for the pre-3.0 entities.
  private companion object {
    const val HISTORY_SCHEMA =
      "CREATE TABLE IF NOT EXISTS `HistoryEntry` (`word` TEXT NOT NULL, `time` INTEGER NOT NULL, " +
        "`gameName` TEXT NOT NULL, `outcome` TEXT NOT NULL, `scoreEarned` INTEGER NOT NULL, " +
        "`hinted` INTEGER NOT NULL, `guesses` TEXT NOT NULL, PRIMARY KEY(`word`))"
    const val BOOKMARK_SCHEMA =
      "CREATE TABLE IF NOT EXISTS `BookmarkEntry` (`word` TEXT NOT NULL, `time` INTEGER NOT NULL, " +
        "PRIMARY KEY(`word`))"
    const val WORD_SCHEMA =
      "CREATE TABLE IF NOT EXISTS `WordEntry` (`word` TEXT NOT NULL, PRIMARY KEY(`word`))"
  }
}
