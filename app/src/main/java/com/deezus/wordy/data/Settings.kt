package com.deezus.wordy.data

import android.content.Context
import com.deezus.wordy.ui.MainActivity


enum class Game { GUESS_4, GUESS_5, GUESS_6, GUESS_7 }
enum class Language { ENGLISH }

class Settings(activity: MainActivity) {
  private val sharedPreferences = activity.getSharedPreferences("WORDY_SCORE", Context.MODE_PRIVATE)

  private val totalScore = "SETTINGS_TOTAL_SCORE"
  private val isLetterWordsSaved = "SETTINGS_IS_WORD_SET_SAVED_"
  private val currentGame = "SETTINGS_CURRENT_GAME"
  private val currentLanguage = "SETTINGS_CURRENT_LANGUAGE"

  fun getCurrentGame(): Game {
    return Game.valueOf(sharedPreferences.getString(currentGame, null) ?: Game.GUESS_5.toString())
  }

  fun setCurrentGame(game: Game) {
    sharedPreferences.edit().apply {
      putString(currentGame, game.toString())
      apply()
    }
  }

  fun getCurrentLanguage(): Language {
    return Language.valueOf(sharedPreferences.getString(currentLanguage, null) ?: Language.ENGLISH.toString())
  }

  fun setCurrentLanguage(language: Language) {
    sharedPreferences.edit().apply {
      putString(currentLanguage, language.toString())
      apply()
    }
  }

  fun getCurrentWordSetName(): String {
    val language = when {
      Language.ENGLISH == getCurrentLanguage() -> "english"
      else -> "english"
    }

    return when {
      Game.GUESS_4 == getCurrentGame() -> "${language}4"
      Game.GUESS_5 == getCurrentGame() -> "${language}5"
      Game.GUESS_6 == getCurrentGame() -> "${language}6"
      Game.GUESS_7 == getCurrentGame() -> "${language}7"
      else -> "${language}5"
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

  fun isWordSetSaved(wordSetName: String): Boolean {
    return sharedPreferences.getBoolean(isLetterWordsSaved + wordSetName, false)
  }

  fun setWordSetToTrue(wordSetName: String) {
    sharedPreferences.edit().apply {
      putBoolean(isLetterWordsSaved + wordSetName, true)
      apply()
    }
  }

}
