package com.deezus.wordy.data

import androidx.room.*
import com.deezus.wordy.helpers.getWords
import com.deezus.wordy.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Entity(primaryKeys = ["word"])
data class WordEntry(
  @ColumnInfo(name = "word") var word: String
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

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertAll(entries: List<WordEntry>)

}

@Database(entities = [WordEntry::class], version = 1, exportSchema = false)
private abstract class WordDatabase : RoomDatabase() {
  abstract fun userDao(): WordEntryDao
}

private data class DatabaseHolder(
  var database: WordDatabase?,
  val databaseName: String,
  val language: Language
)

private var wordDatabases = hashMapOf<Language, WordDatabase>()
private var wordSetDatabases = hashMapOf(
  "english4" to DatabaseHolder(null, "database-english4", Language.ENGLISH),
  "english5" to DatabaseHolder(null, "database-english5", Language.ENGLISH),
  "english6" to DatabaseHolder(null, "database-english6", Language.ENGLISH),
  "english7" to DatabaseHolder(null, "database-english7", Language.ENGLISH)
)

suspend fun initWordDatabase(activity: MainActivity) = withContext(Dispatchers.Default) {
  val settings = Settings(activity)

  Room
    .databaseBuilder(activity, WordDatabase::class.java, "database-english")
    .build()
    .let { wordDatabases[Language.ENGLISH] = it  }

  wordSetDatabases.forEach {
    val wordSetName = it.key
    val value = it.value

    value.database = Room
      .databaseBuilder(activity, WordDatabase::class.java, value.databaseName)
      .build()

    if (!settings.isWordSetSaved(wordSetName)) {
      getWords(activity, wordSetName)
        ?.map { WordEntry(it) }
        ?.let {
          value.database?.userDao()?.insertAll(it)
          wordDatabases[value.language]?.userDao()?.insertAll(it)
          settings.setWordSetToTrue(wordSetName)
        }
    }
  }
}

suspend fun getRandomWord(activity: MainActivity): String? = withContext(Dispatchers.Default) {
  wordSetDatabases[Settings(activity).getCurrentWordSetName()]?.database?.userDao()?.getRandom()?.firstOrNull()?.word
}

suspend fun deleteWord(activity: MainActivity, word: String) = withContext(Dispatchers.Default) {
  wordSetDatabases[Settings(activity).getCurrentWordSetName()]?.database?.userDao()?.delete(word)
}

suspend fun hasWord(activity: MainActivity, word: String): Boolean = withContext(Dispatchers.Default) {
  wordDatabases[Settings(activity).getCurrentLanguage()]?.userDao()?.getWord(word) != null
}