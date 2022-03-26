package com.deezus.wordy.data

import com.deezus.wordy.R


enum class GameName { GUESS_4_ENGLISH, GUESS_5_ENGLISH, GUESS_6_ENGLISH, GUESS_7_ENGLISH }
enum class Language { ENGLISH }
enum class GameOutcome { NOT_COMPLETED, FAILED, SKIPPED, SUCCEEDED }
enum class WordSet { ENGLISH_4, ENGLISH_5, ENGLISH_6, ENGLISH_7 }

data class Game(
  val gameName: GameName,
  val language: Language,
  val wordSet: WordSet,
  val navigationId: Int
)

val guess4English = Game(
  GameName.GUESS_4_ENGLISH,
  Language.ENGLISH,
  WordSet.ENGLISH_4,
  R.id.navigation_wordy
)

val guess5English = Game(
  GameName.GUESS_5_ENGLISH,
  Language.ENGLISH,
  WordSet.ENGLISH_5,
  R.id.navigation_wordy
)

val guess6English = Game(
  GameName.GUESS_6_ENGLISH,
  Language.ENGLISH,
  WordSet.ENGLISH_6,
  R.id.navigation_wordy
)

val guess7English = Game(
  GameName.GUESS_7_ENGLISH,
  Language.ENGLISH,
  WordSet.ENGLISH_7,
  R.id.navigation_wordy
)

val gameSet = hashMapOf(
  guess4English.gameName to guess4English,
  guess5English.gameName to guess5English,
  guess6English.gameName to guess6English,
  guess7English.gameName to guess7English
)

fun getGame(gameName: GameName): Game {
  return gameSet[gameName] ?: guess5English
}