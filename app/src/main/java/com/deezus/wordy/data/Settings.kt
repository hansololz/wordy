package com.deezus.wordy.data

import android.content.Context
import com.deezus.wordy.ui.MainActivity


class Settings(activity: MainActivity) {
  private val sharedPreferences = activity.getSharedPreferences("WORDY_SCORE", Context.MODE_PRIVATE)

  private val totalScore = "SETTINGS_TOTAL_SCORE"
  private val isLetterWordsSaved = "SETTINGS_IS_WORD_SET_SAVED_"
  private val currentGame = "SETTINGS_CURRENT_GAME"

  fun getCurrentGame(): GameName {
    return GameName.valueOf(sharedPreferences.getString(currentGame, null)
      ?: GameName.GUESS_5_ENGLISH.toString())
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

  fun setScore(score: Long) {
    sharedPreferences.edit().apply {
      putLong(totalScore, score)
      apply()
    }
  }

  fun isWordSetSaved(wordSet: WordSet): Boolean {
    return sharedPreferences.getBoolean(isLetterWordsSaved + wordSet.toString(), false)
  }

  fun setWordSetToTrue(wordSet: WordSet) {
    sharedPreferences.edit().apply {
      putBoolean(isLetterWordsSaved + wordSet.toString(), true)
      apply()
    }
  }
}
