package com.deezus.wordy.data

import android.content.Context
import com.deezus.wordy.ui.MainActivity

class Settings(activity: MainActivity) {
  private val sharedPreferences = activity.getSharedPreferences("WORDY_5_SCORE", Context.MODE_PRIVATE)

  private val totalScore = "SETTINGS_TOTAL_SCORE"

  fun getScore(): Long {
    return sharedPreferences.getLong(totalScore, 0)
  }

  fun setScore(score: Long) {
    val edit = sharedPreferences.edit()

    edit.putLong(totalScore, score)
    edit.apply()
  }

}
