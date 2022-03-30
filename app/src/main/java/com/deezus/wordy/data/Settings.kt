package com.deezus.wordy.data

import android.content.Context
import com.deezus.wordy.ui.MainActivity

enum class ScoreViewPreference { TOTAL_SCORE, AVERAGE_SCORE, TOTAL_SCORE_WITHOUT_HINTS, AVERAGE_SCORE_WITHOUT_HINTS }

class Settings(activity: MainActivity) {
  private val sharedPreferences = activity.getSharedPreferences("WORDY_SCORE", Context.MODE_PRIVATE)

  private val totalScore = "SETTINGS_TOTAL_SCORE"
  private val totalGamesWon = "SETTINGS_TOTAL_GAMES_WON"
  private val totalScoreWithoutHint = "SETTINGS_TOTAL_SCORE_WITHOUT_HINT"
  private val totalGamesWonWithoutHint = "SETTINGS_TOTAL_GAMES_WON_WITHOUT_HINT"

  private val scoreViewPreference = "SETTINGS_SCORE_VIEW_PREFERENCE"

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

  fun setScoreViewPreference(preference: ScoreViewPreference) {
    sharedPreferences.edit().apply {
      putString(scoreViewPreference, preference.toString())
      apply()
    }
  }

  fun getScoreViewPreference(): ScoreViewPreference {
    return sharedPreferences.getString(scoreViewPreference, null)?.let {
      ScoreViewPreference.valueOf(it)
    } ?: ScoreViewPreference.TOTAL_SCORE
  }

  fun getScore(): String {
    return when (getScoreViewPreference()) {
      ScoreViewPreference.TOTAL_SCORE -> {
        sharedPreferences.getLong(totalScore, 0).toString()
      }
      ScoreViewPreference.AVERAGE_SCORE -> {
        val score = sharedPreferences.getLong(totalScore, 0)
        val gameCount = sharedPreferences.getLong(totalGamesWon, 0)
        getScore(score, gameCount)
      }
      ScoreViewPreference.TOTAL_SCORE_WITHOUT_HINTS -> {
        sharedPreferences.getLong(totalScoreWithoutHint, 0).toString()
      }
      ScoreViewPreference.AVERAGE_SCORE_WITHOUT_HINTS -> {
        val score = sharedPreferences.getLong(totalScoreWithoutHint, 0)
        val gameCount = sharedPreferences.getLong(totalGamesWonWithoutHint, 0)
        getScore(score, gameCount)
      }
    }
  }

  private fun getScore(score: Long, gameCount: Long): String {
    return if (gameCount == 0L) {
      "0"
    } else {
      (((score.toFloat()/(gameCount * 6f)) * 60).toInt().toFloat() / 10f).toString()
    }
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
