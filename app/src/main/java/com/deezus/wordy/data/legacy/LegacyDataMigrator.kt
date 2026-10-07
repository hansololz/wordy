package com.deezus.wordy.data.legacy

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.withTransaction
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.core.MAX_GUESSES
import com.deezus.wordy.data.db.BookmarkEntity
import com.deezus.wordy.data.db.GameOutcome
import com.deezus.wordy.data.db.GameRecordEntity
import com.deezus.wordy.data.db.UsedWordEntity
import com.deezus.wordy.data.db.WordyDatabase
import com.deezus.wordy.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Moves data written by app versions before 3.0 into [WordyDatabase].
 *
 * Those versions kept history, bookmarks and each word pool in their own Room database. Every
 * legacy database is copied and then deleted, so each step runs at most once and is safe to retry
 * if the app is killed part way through. Scores and settings are migrated separately by the
 * settings DataStore.
 */
class LegacyDataMigrator(
  private val context: Context,
  private val database: WordyDatabase,
  private val settingsRepository: SettingsRepository,
  private val ioDispatcher: CoroutineDispatcher,
) {

  suspend fun migrateIfNeeded(wordLists: Map<GameMode, Set<String>>) = withContext(ioDispatcher) {
    migrate(HISTORY_DATABASE) { legacy ->
      val records = readHistory(legacy)
      database.withTransaction { database.gameRecordDao().insertAll(records) }
    }

    migrate(BOOKMARK_DATABASE) { legacy ->
      val bookmarks = readBookmarks(legacy)
      database.withTransaction { database.bookmarkDao().insertAll(bookmarks) }
    }

    for (mode in GameMode.entries) {
      migrate(wordPoolDatabase(mode)) { legacy ->
        // A pool that never finished seeding says nothing about which words were played.
        if (!settingsRepository.wasLegacyWordPoolSeeded(mode)) return@migrate

        val remaining = readWords(legacy)
        val used = wordLists.getValue(mode)
          .filterNot { it in remaining }
          .map { UsedWordEntity(it, mode.id) }
        database.withTransaction { database.usedWordDao().insertAll(used) }
      }
    }

    // The old dictionary of valid guesses; the bundled word lists replace it.
    context.deleteDatabase(DICTIONARY_DATABASE)
  }

  /** Runs [copy] against a legacy database if it exists and deletes it once [copy] succeeds. */
  private suspend fun migrate(name: String, copy: suspend (SQLiteDatabase) -> Unit) {
    val file = context.getDatabasePath(name)
    if (!file.exists()) return

    try {
      SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { legacy ->
        copy(legacy)
      }
      context.deleteDatabase(name)
    } catch (error: SQLiteException) {
      // Leave the file in place so the next launch can try again.
      Log.w(TAG, "Could not migrate $name", error)
    }
  }

  private fun readHistory(legacy: SQLiteDatabase): List<GameRecordEntity> {
    val records = ArrayList<GameRecordEntity>()
    legacy.rawQuery(
      "SELECT word, time, gameName, outcome, scoreEarned, hinted, guesses FROM HistoryEntry",
      null,
    ).use { cursor ->
      while (cursor.moveToNext()) {
        val guesses = parseLegacyGuesses(cursor.getString(6))
        val outcome = legacyOutcome(cursor.getString(3), guesses.size) ?: continue
        records += GameRecordEntity(
          word = cursor.getString(0),
          finishedAt = cursor.getLong(1),
          mode = cursor.getString(2),
          outcome = outcome,
          score = cursor.getInt(4),
          usedHint = cursor.getInt(5) != 0,
          guesses = guesses,
        )
      }
    }
    return records
  }

  private fun readBookmarks(legacy: SQLiteDatabase): List<BookmarkEntity> {
    val bookmarks = ArrayList<BookmarkEntity>()
    legacy.rawQuery("SELECT word, time FROM BookmarkEntry", null).use { cursor ->
      while (cursor.moveToNext()) {
        bookmarks += BookmarkEntity(cursor.getString(0), cursor.getLong(1))
      }
    }
    return bookmarks
  }

  private fun readWords(legacy: SQLiteDatabase): Set<String> {
    val words = HashSet<String>()
    legacy.rawQuery("SELECT word FROM WordEntry", null).use { cursor ->
      while (cursor.moveToNext()) {
        words += cursor.getString(0)
      }
    }
    return words
  }

  companion object {
    private const val TAG = "LegacyDataMigrator"

    const val HISTORY_DATABASE = "database-history"
    const val BOOKMARK_DATABASE = "database-bookmark"
    const val DICTIONARY_DATABASE = "database-english"

    fun wordPoolDatabase(mode: GameMode) = "database-english${mode.wordLength}"

    /**
     * Maps a legacy outcome to the current one, or null for games that never finished.
     *
     * Old versions recorded a game lost on the final guess as skipped. A skip always happens
     * before the last guess, so a full board identifies those losses.
     */
    fun legacyOutcome(outcome: String?, guessCount: Int): GameOutcome? = when (outcome) {
      "SUCCEEDED" -> GameOutcome.Won
      "FAILED" -> GameOutcome.Lost
      "SKIPPED" -> if (guessCount >= MAX_GUESSES) GameOutcome.Lost else GameOutcome.Skipped
      else -> null
    }

    fun parseLegacyGuesses(guesses: String?): List<String> =
      guesses.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
  }
}
