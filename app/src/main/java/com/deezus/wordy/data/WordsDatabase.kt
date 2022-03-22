package com.deezus.wordy

import android.util.Log
import androidx.room.*
import com.deezus.wordy.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Entity(primaryKeys = ["word"])
data class WordEntry(
  @ColumnInfo(name = "word") var word: String
)

enum class GuessOutcome { NOT_COMPLETED, FAILED, SUCCEEDED }

@Entity(primaryKeys = ["guessedWord"])
data class GuessedWordEntry(
  @ColumnInfo(name = "guessedWord") var word: String,
  @ColumnInfo(name = "time") var time: Long,
  @ColumnInfo(name = "outcome") var outcome: GuessOutcome,
  @ColumnInfo(name = "scoreEarned") var scoreEarned: Long,
)

@Dao
private interface WordEntryDao {

  @Query("DELETE FROM WordEntry WHERE word = :word")
  fun delete(word: String)

  @Query("SELECT * FROM WordEntry")
  fun getAll(): List<WordEntry>

  @Query("SELECT * FROM WordEntry WHERE word = :word LIMIT 1")
  fun getWord(word: String): List<WordEntry>

  @Query("SELECT * FROM WordEntry ORDER BY RANDOM() LIMIT 1")
  fun getRandom(): List<WordEntry>

  @Query("SELECT COUNT(word) FROM WordEntry")
  fun getSize(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertAll(entries: List<WordEntry>)

}

@Dao
private interface GuessedWordEntryDao {

  @Query("DELETE FROM GuessedWordEntry WHERE guessedWord = :word")
  fun delete(word: String)

  @Query("SELECT * FROM GuessedWordEntry ORDER BY time")
  fun getAll(): List<GuessedWordEntry>

  @Query("SELECT * FROM GuessedWordEntry WHERE guessedWord = :word LIMIT 1")
  fun getWord(word: String): List<GuessedWordEntry>

  @Query("SELECT COUNT(guessedWord) FROM GuessedWordEntry")
  fun getSize(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertAll(entry: List<GuessedWordEntry>)
}

@Database(entities = [WordEntry::class], version = 1, exportSchema = false)
private abstract class WordDatabase : RoomDatabase() {
  abstract fun userDao(): WordEntryDao
}

@Database(entities = [GuessedWordEntry::class], version = 1, exportSchema = false)
private abstract class GuessedWordDatabase : RoomDatabase() {
  abstract fun userDao(): GuessedWordEntryDao
}

private var word5Database: WordDatabase? = null
private var guessedWordDatabase: GuessedWordDatabase? = null

suspend fun initWordDatabase(activity: MainActivity) = withContext(Dispatchers.Default) {
  word5Database = Room
    .databaseBuilder(activity, WordDatabase::class.java, "database-word-5")
    .build()

  guessedWordDatabase = Room
    .databaseBuilder(activity, GuessedWordDatabase::class.java, "database-guessed-word")
    .build()

  val availableWordsCount = word5Database?.userDao()?.getSize()

  Log.d("WORDYYYY", "WORD COUNT $availableWordsCount")

  if (availableWordsCount != null && availableWordsCount == 0) {
    getWords(activity)?.map {
      WordEntry(it)
    }?.let {
      word5Database?.userDao()?.insertAll(it)
    }
  }
}

suspend fun getRandomWord(): String? = withContext(Dispatchers.Default) {
  word5Database?.userDao()?.getRandom()?.firstOrNull()?.word
}

suspend fun deleteWord(word: String) = withContext(Dispatchers.Default) {
  word5Database?.userDao()?.delete(word)
}

suspend fun hasWord(word: String): Boolean = withContext(Dispatchers.Default) {
  word5Database?.userDao()?.getWord(word)?.firstOrNull() != null ||
      guessedWordDatabase?.userDao()?.getWord(word)?.firstOrNull() != null
}

suspend fun addGuessedWord(word: String, time: Long, outcome: GuessOutcome, scoreEarned: Long) = withContext(Dispatchers.Default) {
  val entry = GuessedWordEntry(word, time, outcome, scoreEarned)
  guessedWordDatabase?.userDao()?.insertAll(listOf(entry))
}

suspend fun getGuessedWord(word: String): GuessedWordEntry? = withContext(Dispatchers.Default) {
  guessedWordDatabase?.userDao()?.getWord(word)?.firstOrNull()
}

suspend fun getAllGuessedWords(word: String): List<GuessedWordEntry> = withContext(Dispatchers.Default) {
  guessedWordDatabase?.userDao()?.getAll() ?: listOf()
}
