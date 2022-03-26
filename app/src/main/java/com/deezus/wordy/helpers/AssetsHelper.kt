package com.deezus.wordy.helpers

import com.deezus.wordy.ui.MainActivity
import java.io.BufferedReader
import java.io.InputStreamReader


fun getEnglish4(activity: MainActivity): List<String>? {
  return getWords(activity, "english4")
}

fun getEnglish5(activity: MainActivity): List<String>? {
  return getWords(activity, "english5")
}

fun getEnglish6(activity: MainActivity): List<String>? {
  return getWords(activity, "english6")
}

fun getEnglish7(activity: MainActivity): List<String>? {
  return getWords(activity, "english7")
}

fun getWords(activity: MainActivity, wordSetName: String): List<String>? {
  return getWords(activity, "words/$wordSetName.txt", 5)
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