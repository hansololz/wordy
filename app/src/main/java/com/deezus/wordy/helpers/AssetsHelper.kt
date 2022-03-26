package com.deezus.wordy.helpers

import com.deezus.wordy.data.WordSet
import com.deezus.wordy.ui.MainActivity
import java.io.BufferedReader
import java.io.InputStreamReader


private val wordSetToFilenameMap = mapOf(
  WordSet.ENGLISH_4 to "english4",
  WordSet.ENGLISH_5 to "english5",
  WordSet.ENGLISH_6 to "english6",
  WordSet.ENGLISH_7 to "english7"
)

fun getWords(activity: MainActivity, wordSet: WordSet): List<String>? {
  return getWords(activity, "words/${wordSetToFilenameMap[wordSet]}.txt", 5)
}

private fun getWords(activity: MainActivity, filepath: String, wordLength: Int) : List<String>? {
  try {
    val reader = BufferedReader(InputStreamReader(activity.assets.open(filepath)))
    val words = arrayListOf<String>()
    var line = reader.readLine()

    while (line != null) {
      if (line.length == wordLength) {
        words.add(line)
      }
      line = reader.readLine()
    }

    return words
  } catch (exception: Exception) {
    return null
  }
}