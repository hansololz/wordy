package com.deezus.wordy

import java.io.BufferedReader
import java.io.InputStreamReader


fun getWords(activity: MainActivity): List<String>? {
  return getWords(activity, "words/5.txt", 5)
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