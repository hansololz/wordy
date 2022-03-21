package com.deezus.wordy

import android.util.Log
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Entity(primaryKeys = ["word"])
data class WordEntry(
  @ColumnInfo(name = "word") var word: String
)

@Entity(primaryKeys = ["word"])
data class GuessedWordEntry(
  @ColumnInfo(name = "word") var word: String,
  @ColumnInfo(name = "time") var time: Int,
  @ColumnInfo(name = "outCome") var outCome: Int,
  @ColumnInfo(name = "scoreEarned") var scoreEarned: Int,
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

@Database(entities = [WordEntry::class], version = 1, exportSchema = false)
private abstract class WordDatabase : RoomDatabase() {
  abstract fun userDao(): WordEntryDao
}

private var word5Database: WordDatabase? = null

suspend fun initWordDatabase(activity: MainActivity) = withContext(Dispatchers.Default) {
  word5Database = Room
    .databaseBuilder(activity, WordDatabase::class.java, "database-word-5")
    .build()

  val availableWordsCount = word5Database?.userDao()?.getSize()

  Log.d("WORDYYYY", "WORD COUNT $availableWordsCount")

  if (availableWordsCount != null && availableWordsCount == 0) {
    getWords(activity)?.map {
      WordEntry(it)
    }?.let {
      word5Database?.userDao()?.insertAll(it)
    }

    Log.d("WORDYYYY", "WORD INIT")
  } else {
    Log.d("WORDYYYY", "SKIPPED INIT")
  }
}

suspend fun getRandomWord(): String? = withContext(Dispatchers.Default) {
  word5Database?.userDao()?.getRandom()?.firstOrNull()?.word
}

suspend fun deleteWord(word: String) = withContext(Dispatchers.Default) {
  word5Database?.userDao()?.delete(word)
}

suspend fun hasWord(word: String): Boolean = withContext(Dispatchers.Default) {
  word5Database?.userDao()?.getWord(word)?.firstOrNull() != null
}
