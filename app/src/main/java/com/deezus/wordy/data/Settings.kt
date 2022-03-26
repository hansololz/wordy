package com.deezus.wordy.data

import android.content.Context
import com.deezus.wordy.ui.MainActivity

class Settings(activity: MainActivity) {
  private val sharedPreferences = activity.getSharedPreferences("WORDY_SCORE", Context.MODE_PRIVATE)

  private val totalScore = "SETTINGS_TOTAL_SCORE"
  private val is4LetterWordsSaved = "SETTINGS_IS_4_LETTER_WORD_SAVED"
  private val is5LetterWordsSaved = "SETTINGS_IS_5_LETTER_WORD_SAVED"
  private val is6LetterWordsSaved = "SETTINGS_IS_6_LETTER_WORD_SAVED"
  private val is7LetterWordsSaved = "SETTINGS_IS_7_LETTER_WORD_SAVED"

  fun getScore(): Long {
    return sharedPreferences.getLong(totalScore, 0)
  }

  fun setScore(score: Long) {
    sharedPreferences.edit().apply {
      putLong(totalScore, score)
      apply()
    }
  }

  fun getIs4LetterWordsSaved(): Boolean {
    return sharedPreferences.getBoolean(is4LetterWordsSaved, false)
  }

  fun setIs4LetterWordsSavedToTrue() {
    sharedPreferences.edit().apply {
      putBoolean(is4LetterWordsSaved, true)
      apply()
    }
  }

  fun getIs5LetterWordsSaved(): Boolean {
    return sharedPreferences.getBoolean(is5LetterWordsSaved, false)
  }

  fun setIs5LetterWordsSavedToTrue() {
    sharedPreferences.edit().apply {
      putBoolean(is5LetterWordsSaved, true)
      apply()
    }
  }

  fun getIs6LetterWordsSaved(): Boolean {
    return sharedPreferences.getBoolean(is6LetterWordsSaved, false)
  }

  fun setIs6LetterWordsSavedToTrue() {
    sharedPreferences.edit().apply {
      putBoolean(is6LetterWordsSaved, true)
      apply()
    }
  }

  fun getIs7LetterWordsSaved(): Boolean {
    return sharedPreferences.getBoolean(is7LetterWordsSaved, false)
  }

  fun setIs7LetterWordsSavedToTrue() {
    sharedPreferences.edit().apply {
      putBoolean(is7LetterWordsSaved, true)
      apply()
    }
  }

}
