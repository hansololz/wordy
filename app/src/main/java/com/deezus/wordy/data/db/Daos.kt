package com.deezus.wordy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GameRecordDao {

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(record: GameRecordEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAll(records: List<GameRecordEntity>)

  @Query(
    """
    SELECT r.*, EXISTS(SELECT 1 FROM bookmarks b WHERE b.word = r.word) AS is_bookmarked
    FROM game_records r
    ORDER BY r.finished_at DESC, r.id DESC
    LIMIT :limit
    """
  )
  fun observeRecent(limit: Int): Flow<List<GameRecordWithBookmark>>
}

@Dao
interface BookmarkDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(bookmark: BookmarkEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAll(bookmarks: List<BookmarkEntity>)

  @Query("DELETE FROM bookmarks WHERE word = :word")
  suspend fun delete(word: String)

  @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE word = :word)")
  suspend fun isBookmarked(word: String): Boolean

  @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE word = :word)")
  fun observeIsBookmarked(word: String): Flow<Boolean>

  @Query(
    """
    SELECT b.word, b.created_at,
      (SELECT MAX(r.score) FROM game_records r WHERE r.word = b.word) AS best_score
    FROM bookmarks b
    ORDER BY b.created_at DESC
    """
  )
  fun observeAll(): Flow<List<BookmarkWithScore>>

  @Transaction
  suspend fun toggle(word: String, now: Long) {
    if (isBookmarked(word)) delete(word) else insert(BookmarkEntity(word, now))
  }
}

@Dao
interface UsedWordDao {

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(word: UsedWordEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAll(words: List<UsedWordEntity>)

  @Query("SELECT * FROM used_words")
  suspend fun getAll(): List<UsedWordEntity>

  @Query("DELETE FROM used_words WHERE mode = :mode")
  suspend fun deleteByMode(mode: String)
}
