package com.deezus.wordy.data.bookmarks

import com.deezus.wordy.data.db.BookmarkDao
import com.deezus.wordy.data.db.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Bookmark(
  val word: String,
  val createdAt: Long,
  /** The most points earned for this word, or null if it was never guessed correctly. */
  val bestScore: Int?,
)

class BookmarkRepository(
  private val bookmarkDao: BookmarkDao,
  private val clock: () -> Long,
) {
  val bookmarks: Flow<List<Bookmark>> = bookmarkDao.observeAll().map { rows ->
    rows.map { Bookmark(it.word, it.createdAt, it.bestScore?.takeIf { score -> score > 0 }) }
  }

  fun isBookmarked(word: String): Flow<Boolean> = bookmarkDao.observeIsBookmarked(word)

  suspend fun toggle(word: String) {
    bookmarkDao.toggle(word, clock())
  }

  suspend fun remove(word: String) {
    bookmarkDao.delete(word)
  }

  /** Puts back a bookmark that was just removed, keeping its original position in the list. */
  suspend fun restore(bookmark: Bookmark) {
    bookmarkDao.insert(BookmarkEntity(bookmark.word, bookmark.createdAt))
  }
}
