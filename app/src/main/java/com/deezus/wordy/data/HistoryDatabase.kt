package com.deezus.wordy.data

import androidx.room.*
import com.deezus.wordy.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Entity(primaryKeys = ["word"])
data class HistoryEntry(
  @ColumnInfo(name = "word") var word: String,
  @ColumnInfo(name = "time") var time: Long,
  @ColumnInfo(name = "gameName") var game: GameName,
  @ColumnInfo(name = "outcome") var outcome: GameOutcome,
  @ColumnInfo(name = "scoreEarned") var scoreEarned: Long,
  @ColumnInfo(name = "hinted") var hinted: Boolean
)

@Dao
private interface HistoryEntryDao {

  @Query("SELECT * FROM HistoryEntry WHERE outcome = 'FAILED' OR outcome = 'SKIPPED' OR outcome = 'SUCCEEDED' ORDER BY time DESC")
  fun getAll(): List<HistoryEntry>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(entry: HistoryEntry)
}

@Database(entities = [HistoryEntry::class], version = 1, exportSchema = false)
private abstract class HistoryDatabase : RoomDatabase() {
  abstract fun userDao(): HistoryEntryDao
}

private var historyDatabase: HistoryDatabase? = null

suspend fun initHistoryDatabase(activity: MainActivity) {
  historyDatabase = Room
    .databaseBuilder(activity, HistoryDatabase::class.java, "database-history")
    .build()
}

suspend fun addHistory(word: String, time: Long, game: GameName, outcome: GameOutcome,
    scoreEarned: Long, hinted: Boolean) = withContext(Dispatchers.Default) {
  val entry = HistoryEntry(word, time, game, outcome, scoreEarned, hinted)
  historyDatabase?.userDao()?.insert(entry)
}

suspend fun getAllHistory(): List<HistoryEntry> = withContext(Dispatchers.Default) {
  historyDatabase?.userDao()?.getAll() ?: listOf()
}