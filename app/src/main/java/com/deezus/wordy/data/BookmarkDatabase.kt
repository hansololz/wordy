package com.deezus.wordy.data

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.room.*
import com.deezus.wordy.R
import com.deezus.wordy.helpers.performFeedback
import com.deezus.wordy.helpers.scope
import com.deezus.wordy.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Entity(primaryKeys = ["word"])
data class BookmarkEntry(
  @ColumnInfo(name = "word") var word: String,
  @ColumnInfo(name = "time") var time: Long
)

@Dao
private interface BookmarkEntryDao {

  @Query("DELETE FROM BookmarkEntry WHERE word = :word")
  fun delete(word: String)

  @Query("SELECT * FROM BookmarkEntry ORDER BY time DESC LIMIT 1000")
  fun getAll(): List<BookmarkEntry>

  @Query("SELECT * FROM BookmarkEntry WHERE word = :word LIMIT 1")
  fun getBookmark(word: String): List<BookmarkEntry>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(entry: BookmarkEntry)

}

@Database(entities = [BookmarkEntry::class], version = 1, exportSchema = false)
private abstract class BookmarkDatabase : RoomDatabase() {
  abstract fun userDao(): BookmarkEntryDao
}

private var bookmarkDatabase: BookmarkDatabase? = null

suspend fun initBookmarkDatabase(activity: MainActivity) = withContext(Dispatchers.Default) {
  bookmarkDatabase = Room
    .databaseBuilder(activity, BookmarkDatabase::class.java, "database-bookmark")
    .build()
}

suspend fun getBookmark(word: String): BookmarkEntry? = withContext(Dispatchers.Default) {
  bookmarkDatabase?.userDao()?.getBookmark(word)?.firstOrNull()
}

suspend fun addBookmark(word: String) = withContext(Dispatchers.Default) {
  bookmarkDatabase?.userDao()?.insert(BookmarkEntry(word, System.currentTimeMillis()))
}

suspend fun getAllBookmark(): List<BookmarkEntry> = withContext(Dispatchers.Default) {
  bookmarkDatabase?.userDao()?.getAll() ?: listOf()
}

suspend fun deleteBookmark(word: String) = withContext(Dispatchers.Default) {
  bookmarkDatabase?.userDao()?.delete(word)
}

fun setupBookmarkButton(activity: MainActivity, bookmarkButtonHolder: ConstraintLayout, bookmarkButton: ImageView, word: String) {
  bookmarkButtonHolder.setOnClickListener {
    scope.launch {
      if (getBookmark(word) != null) {
        deleteBookmark(word)
        bookmarkButton.setImageResource(R.drawable.ic_round_bookmark_border_24)
      } else {
        addBookmark(word)
        bookmarkButton.setImageResource(R.drawable.ic_round_bookmark_24)
      }
    }

    performFeedback(activity, it)
  }

  scope.launch {
    if (getBookmark(word) != null) {
      bookmarkButton.setImageResource(R.drawable.ic_round_bookmark_24)
    } else {
      bookmarkButton.setImageResource(R.drawable.ic_round_bookmark_border_24)
    }
  }
}



