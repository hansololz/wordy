package com.deezus.wordy.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** How a finished game ended. Stored by name, so entries must not be renamed. */
enum class GameOutcome { Won, Lost, Skipped }

/** One finished game. A word can appear more than once if it is played again. */
@Entity(
  tableName = "game_records",
  indices = [
    Index(value = ["word", "finished_at"], unique = true),
    Index(value = ["finished_at"]),
  ],
)
data class GameRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val word: String,
  /** A [com.deezus.wordy.core.GameMode.id]. */
  val mode: String,
  @ColumnInfo(name = "finished_at") val finishedAt: Long,
  val outcome: GameOutcome,
  val score: Int,
  @ColumnInfo(name = "used_hint") val usedHint: Boolean,
  val guesses: List<String>,
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
  @PrimaryKey val word: String,
  @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** A word that has already been played and should not be picked again until the pool resets. */
@Entity(tableName = "used_words")
data class UsedWordEntity(
  @PrimaryKey val word: String,
  /** A [com.deezus.wordy.core.GameMode.id]. */
  val mode: String,
)

data class GameRecordWithBookmark(
  @Embedded val record: GameRecordEntity,
  @ColumnInfo(name = "is_bookmarked") val isBookmarked: Boolean,
)

data class BookmarkWithScore(
  val word: String,
  @ColumnInfo(name = "created_at") val createdAt: Long,
  @ColumnInfo(name = "best_score") val bestScore: Int?,
)
