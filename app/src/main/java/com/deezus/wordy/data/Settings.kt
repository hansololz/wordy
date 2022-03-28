package com.deezus.wordy.data

import android.content.Context
import com.deezus.wordy.ui.MainActivity


class Settings(activity: MainActivity) {
  private val sharedPreferences = activity.getSharedPreferences("WORDY_SCORE", Context.MODE_PRIVATE)

  private val totalScore = "SETTINGS_TOTAL_SCORE"
  private val totalGamesWon = "SETTINGS_TOTAL_GAMES_WON"
  private val totalScoreWithoutHint = "SETTINGS_TOTAL_SCORE_WITHOUT_HINT"
  private val totalGamesWonWithoutHint = "SETTINGS_TOTAL_GAMES_WON_WITHOUT_HINT"

  private val isWordsSetSaved = "SETTINGS_IS_WORD_SET_SAVED_"
  private val currentGame = "SETTINGS_CURRENT_GAME"
  private val isHapticFeedbackEnabled = "SETTINGS_IS_HAPTIC_FEEDBACK_ENABLED"

  fun getCurrentGame(): GameName? {
    return sharedPreferences.getString(currentGame, null)?.let {
      GameName.valueOf(it)
    }
  }

  fun setCurrentGame(gameName: GameName) {
    sharedPreferences.edit().apply {
      putString(currentGame, gameName.toString())
      apply()
    }
  }

  fun getScore(): Long {
    return sharedPreferences.getLong(totalScore, 0)
  }

  fun setScore(gameScore: Long, hasAskedForHint: Boolean) {
    val score = sharedPreferences.getLong(totalScore, 0) + gameScore
    val gamesWon = sharedPreferences.getLong(totalGamesWon, 0) + 1

    sharedPreferences.edit().apply {
      putLong(totalScore, score)
      apply()
    }

    sharedPreferences.edit().apply {
      putLong(totalGamesWon, gamesWon)
      apply()
    }

    if (!hasAskedForHint) {
      val scoreWithoutHint = sharedPreferences.getLong(totalScoreWithoutHint, 0) + gameScore
      val gamesWonWithoutHint = sharedPreferences.getLong(totalGamesWonWithoutHint, 0) + 1

      sharedPreferences.edit().apply {
        putLong(totalScoreWithoutHint, scoreWithoutHint)
        apply()
      }

      sharedPreferences.edit().apply {
        putLong(totalGamesWonWithoutHint, gamesWonWithoutHint)
        apply()
      }
    }
  }

  fun isWordSetSaved(wordSet: WordSet): Boolean {
    return sharedPreferences.getBoolean(isWordsSetSaved + wordSet.toString(), false)
  }

  fun setWordSetToTrue(wordSet: WordSet) {
    sharedPreferences.edit().apply {
      putBoolean(isWordsSetSaved + wordSet.toString(), true)
      apply()
    }
  }

  fun isHapticFeedbackEnabled(): Boolean {
    return sharedPreferences.getBoolean(isHapticFeedbackEnabled, false)
  }

  fun setHapticFeedback(isEnabled: Boolean) {
    sharedPreferences.edit().apply {
      putBoolean(isHapticFeedbackEnabled, isEnabled)
      apply()
    }
  }
}
