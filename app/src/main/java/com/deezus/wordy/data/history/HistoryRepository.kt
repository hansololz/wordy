package com.deezus.wordy.data.history

import com.deezus.wordy.core.GameEngine
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.core.GameState
import com.deezus.wordy.core.GameStatus
import com.deezus.wordy.data.db.GameOutcome
import com.deezus.wordy.data.db.GameRecordDao
import com.deezus.wordy.data.db.GameRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class HistoryEntry(
  val id: Long,
  val word: String,
  val mode: GameMode?,
  val finishedAt: Long,
  val outcome: GameOutcome,
  val score: Int,
  val usedHint: Boolean,
  val guesses: List<String>,
  val isBookmarked: Boolean,
)

class HistoryRepository(private val gameRecordDao: GameRecordDao) {

  val history: Flow<List<HistoryEntry>> = gameRecordDao.observeRecent(HISTORY_LIMIT).map { rows ->
    rows.map { row ->
      HistoryEntry(
        id = row.record.id,
        word = row.record.word,
        mode = GameMode.fromId(row.record.mode),
        finishedAt = row.record.finishedAt,
        outcome = row.record.outcome,
        score = row.record.score,
        usedHint = row.record.usedHint,
        guesses = row.record.guesses,
        isBookmarked = row.isBookmarked,
      )
    }
  }

  /** Stores a finished game. Games that are still in progress are ignored. */
  suspend fun record(game: GameState, finishedAt: Long) {
    val outcome = when (game.status) {
      GameStatus.Won -> GameOutcome.Won
      GameStatus.Lost -> GameOutcome.Lost
      GameStatus.Skipped -> GameOutcome.Skipped
      GameStatus.InProgress -> return
    }

    gameRecordDao.insert(
      GameRecordEntity(
        word = game.answer,
        mode = game.mode.id,
        finishedAt = finishedAt,
        outcome = outcome,
        score = GameEngine.score(game),
        usedHint = game.usedHint,
        guesses = game.guesses,
      )
    )
  }

  private companion object {
    const val HISTORY_LIMIT = 1000
  }
}
