package com.deezus.wordy.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.deezus.wordy.*
import com.deezus.wordy.databinding.FragmentWordyBinding
import com.deezus.wordy.helpers.DialogMessage
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch


class WordyFragment : BaseFragment() {

  private var _binding: FragmentWordyBinding? = null
  private val binding get() = _binding!!

  private val letterViews = arrayListOf<ArrayList<TextView>>()
  private var currentX = 0
  private var currentY = 0
  private val keyViews = hashMapOf<Char, TextView>()
  private var currentWord = "nomad"
  private val guessedLetters = hashSetOf<Char>()
  private val maxGuessCount = 6

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentWordyBinding.inflate(inflater, container, false)
    val root: View = binding.root

    setupView()
    setupGame()

    return root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun setupView() {
    setupKey(binding.keyQ, 'Q')
    setupKey(binding.keyW, 'W')
    setupKey(binding.keyE, 'E')
    setupKey(binding.keyR, 'R')
    setupKey(binding.keyT, 'T')
    setupKey(binding.keyY, 'Y')
    setupKey(binding.keyU, 'U')
    setupKey(binding.keyI, 'I')
    setupKey(binding.keyO, 'O')
    setupKey(binding.keyP, 'P')

    setupKey(binding.keyA, 'A')
    setupKey(binding.keyS, 'S')
    setupKey(binding.keyD, 'D')
    setupKey(binding.keyF, 'F')
    setupKey(binding.keyG, 'G')
    setupKey(binding.keyH, 'H')
    setupKey(binding.keyJ, 'J')
    setupKey(binding.keyK, 'K')
    setupKey(binding.keyL, 'L')

    setupKey(binding.keyZ, 'Z')
    setupKey(binding.keyX, 'X')
    setupKey(binding.keyC, 'C')
    setupKey(binding.keyV, 'V')
    setupKey(binding.keyB, 'B')
    setupKey(binding.keyN, 'N')
    setupKey(binding.keyM, 'M')

    setupLetter(binding.letter00, 0, 0)
    setupLetter(binding.letter01, 0, 1)
    setupLetter(binding.letter02, 0, 2)
    setupLetter(binding.letter03, 0, 3)
    setupLetter(binding.letter04, 0, 4)
    setupLetter(binding.letter05, 0, 5)

    setupLetter(binding.letter10, 1, 0)
    setupLetter(binding.letter11, 1, 1)
    setupLetter(binding.letter12, 1, 2)
    setupLetter(binding.letter13, 1, 3)
    setupLetter(binding.letter14, 1, 4)
    setupLetter(binding.letter15, 1, 5)

    setupLetter(binding.letter20, 2, 0)
    setupLetter(binding.letter21, 2, 1)
    setupLetter(binding.letter22, 2, 2)
    setupLetter(binding.letter23, 2, 3)
    setupLetter(binding.letter24, 2, 4)
    setupLetter(binding.letter25, 2, 5)

    setupLetter(binding.letter30, 3, 0)
    setupLetter(binding.letter31, 3, 1)
    setupLetter(binding.letter32, 3, 2)
    setupLetter(binding.letter33, 3, 3)
    setupLetter(binding.letter34, 3, 4)
    setupLetter(binding.letter35, 3, 5)

    setupLetter(binding.letter40, 4, 0)
    setupLetter(binding.letter41, 4, 1)
    setupLetter(binding.letter42, 4, 2)
    setupLetter(binding.letter43, 4, 3)
    setupLetter(binding.letter44, 4, 4)
    setupLetter(binding.letter45, 4, 5)

    setupLetter(binding.letter50, 5, 0)
    setupLetter(binding.letter51, 5, 1)
    setupLetter(binding.letter52, 5, 2)
    setupLetter(binding.letter53, 5, 3)
    setupLetter(binding.letter54, 5, 4)
    setupLetter(binding.letter55, 5, 5)

    binding.deleteLetter.setOnClickListener {
      if (currentY > 0) {
        currentY--
        letterViews[currentX][currentY].text = ""
        letterViews[currentX][currentY].setBackgroundResource(R.drawable.letter_background_no_guess_and_focus)

        if (currentY + 1 < currentWord.length) {
          letterViews[currentX][currentY + 1].setBackgroundResource(R.drawable.letter_background_no_guess)
        }
      }

      updateButton()
    }

    binding.submitButton.setOnClickListener {
      if (currentY == currentWord.length) {
        val guessedWord = getGuessedWord()

        if (currentWord == guessedWord) {
          DialogMessage(getMainActivity(), "Success")
            .show()
        } else {
          guessedWord.forEachIndexed { index, letter ->
            guessedLetters.add(letter)

            val letterBackgroundId = when {
              currentWord[index] == letter -> R.drawable.letter_background_match
              currentWord.contains(letter) -> R.drawable.letter_background_present
              else -> R.drawable.letter_background_no_match
            }

            letterViews[currentX][index].setBackgroundResource(letterBackgroundId)
            letterViews[currentX][index].setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorLetterTextGuessed, Color.WHITE))

            val keyBackgroundId = when {
              letterViews.any { it[index].text?.firstOrNull()?.lowercaseChar() == currentWord[index] } -> R.drawable.key_background_match
              currentWord.contains(letter) -> R.drawable.key_background_present
              else -> R.drawable.key_background_no_match
            }

            keyViews[letter]?.setBackgroundResource(keyBackgroundId)
            keyViews[letter]?.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorKeyTextGuessed, Color.BLACK))
          }

          currentX++

          if (currentX == maxGuessCount) {
            DialogMessage(getMainActivity(), "Sorry, the word was \"$currentWord\"")
              .show()
          } else {
            currentY = 0
            letterViews[currentX][currentY].setBackgroundResource(R.drawable.letter_background_no_guess_and_focus)
          }
        }
      }
    }

  }

  private fun setupKey(keyView: TextView, key: Char) {
    keyViews[key.lowercaseChar()] = keyView

    keyView.setOnClickListener {
      if (currentY < currentWord.length && currentX < maxGuessCount && letterViews[currentX][currentY].text.isEmpty()) {
        letterViews[currentX][currentY].text = key.toString()
        currentY++
      }

      letterViews[currentX][currentY - 1].setBackgroundResource(R.drawable.letter_background_no_guess)

      if (currentY < currentWord.length) {
        letterViews[currentX][currentY].setBackgroundResource(R.drawable.letter_background_no_guess_and_focus)
      }

      updateButton()
    }
  }

  private fun setupLetter(letterView: TextView, x: Int, y: Int) {
    if (y >= currentWord.length || x >= maxGuessCount) {
      letterView.visibility = View.GONE
    } else {
      letterView.visibility = View.VISIBLE

      if (y == 0) {
        letterViews.add(arrayListOf())
      }

      letterViews[x].add(letterView)
    }
  }

  private fun setupGame() {
    letterViews[currentX][currentY].setBackgroundResource(R.drawable.letter_background_no_guess_and_focus)

    scope.launch {
      initWordDatabase(getMainActivity())

      getRandomWord()?.let { randomWord ->
        Log.d("WORDYYY", randomWord)
        currentWord = randomWord
        deleteWord(randomWord)
        addGuessedWord(currentWord, System.currentTimeMillis(), GuessOutcome.PENDING, 0)
        currentWord = randomWord
      }
    }
  }

  private fun updateButton() {
    if (currentY >= currentWord.length) {
      scope.launch {
        binding.submitButton.text = if (!hasWord(getGuessedWord())) {
          "Not A\nWord"
        } else {
          "Submit"
        }
      }
    } else {
      binding.submitButton.text = "Submit"
    }
  }

  private fun getGuessedWord(): String {
    val wordBuilder = StringBuilder()

    letterViews[currentX].forEach {
      wordBuilder.append(it.text)
    }

    return wordBuilder.toString().lowercase()
  }
}