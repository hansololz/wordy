package com.deezus.wordy.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.deezus.wordy.*
import com.deezus.wordy.data.Settings
import com.deezus.wordy.databinding.FragmentWordyBinding
import com.deezus.wordy.helpers.DialogMessage
import com.deezus.wordy.helpers.showSnackBar
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


class WordyFragment : BaseFragment() {

  private var _binding: FragmentWordyBinding? = null
  private val binding get() = _binding!!

  private val letterViews = arrayListOf<ArrayList<TextView>>()
  private var currentX = 0
  private var currentY = 0
  private val keyViews = hashMapOf<Char, TextView>()
  private var currentWord = "nomad"
  private val maxGuessCount = 6

  private var hasAskedForHint = false
  private var hintedLetters = hashSetOf<Char>()

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

    setupLetter(binding.letter10, 1, 0)
    setupLetter(binding.letter11, 1, 1)
    setupLetter(binding.letter12, 1, 2)
    setupLetter(binding.letter13, 1, 3)
    setupLetter(binding.letter14, 1, 4)

    setupLetter(binding.letter20, 2, 0)
    setupLetter(binding.letter21, 2, 1)
    setupLetter(binding.letter22, 2, 2)
    setupLetter(binding.letter23, 2, 3)
    setupLetter(binding.letter24, 2, 4)

    setupLetter(binding.letter30, 3, 0)
    setupLetter(binding.letter31, 3, 1)
    setupLetter(binding.letter32, 3, 2)
    setupLetter(binding.letter33, 3, 3)
    setupLetter(binding.letter34, 3, 4)

    setupLetter(binding.letter40, 4, 0)
    setupLetter(binding.letter41, 4, 1)
    setupLetter(binding.letter42, 4, 2)
    setupLetter(binding.letter43, 4, 3)
    setupLetter(binding.letter44, 4, 4)

    setupLetter(binding.letter50, 5, 0)
    setupLetter(binding.letter51, 5, 1)
    setupLetter(binding.letter52, 5, 2)
    setupLetter(binding.letter53, 5, 3)
    setupLetter(binding.letter54, 5, 4)

    binding.deleteLetter.setOnClickListener {
      if (currentY > 0) {
        currentY--
        letterViews[currentX][currentY].text = ""
        updateLetters()
        updateSubmitButton()
      }
    }

    binding.submitButton.setOnClickListener {
      val guessedWord = getGuessedWord()

      scope.launch {
        if (currentY == currentWord.length && hasWord(guessedWord)) {
          if (currentWord == guessedWord) {
            val newScore = min(maxGuessCount, max(maxGuessCount - currentX, 0)).toLong()
            val settings = Settings(getMainActivity())

            settings.setScore(settings.getScore() + newScore)
            updateScore()

            scope.launch {
              addGuessedWord(currentWord, System.currentTimeMillis(), GuessOutcome.SUCCEEDED, newScore)
            }

            val scoreMessage = if (newScore > 1) {
              "$newScore points"
            } else {
              "$newScore point"
            }

            DialogMessage(getMainActivity(), "Congrats, you guessed the mystery word and earned $scoreMessage.")
              .setOnDismissCallback {
                setupGame()
              }
              .setPositiveCallback("Play Again") {
                setupGame()
              }
              .show()
          } else if (currentX + 1 == maxGuessCount) {
            scope.launch {
              addGuessedWord(currentWord, System.currentTimeMillis(), GuessOutcome.FAILED, 0)
            }

            DialogMessage(getMainActivity(), "Sorry, the word was \"$currentWord\"")
              .setOnDismissCallback {
                setupGame()
              }
              .setPositiveCallback("Play Again") {
                setupGame()
              }
              .show()
          } else {
            currentX++
            currentY = 0

            updateLetters()
            updateKeys()
            updateSubmitButton()
          }
        }
      }
    }

    binding.skipNext.setOnClickListener {
      DialogMessage(getMainActivity(), "Are you sure you want to skip to the next word?")
        .setPositiveCallback("Yes") {
          val oldWord = currentWord

          setupGame()

          DialogMessage(getMainActivity(), "The mystery word was \"$oldWord\".")
            .setPositiveCallback("Ok") {

            }
            .show()
        }
        .setNegativeCallback("No") {

        }
        .show()
    }

    binding.showHint.setOnClickListener {
      val unusedLetters = getUnusedLetter()

      Log.d("WORDYY", hintedLetters.toString())

      hasAskedForHint = true

      if (unusedLetters.isEmpty()) {
        showSnackBar(getMainActivity(), "No more hints available.")
      } else {
        val hintedLetter = unusedLetters.toList().toList().shuffled().first()
        hintedLetters.add(hintedLetter)
      }

      updateHintButton()
      updateKeys()
    }
  }

  private fun setupKey(keyView: TextView, key: Char) {
    keyViews[key.lowercaseChar()] = keyView

    keyView.text = key.uppercaseChar().toString()

    keyView.setOnClickListener {
      if (currentY < currentWord.length && currentX < maxGuessCount && letterViews[currentX][currentY].text.isEmpty()) {
        letterViews[currentX][currentY].text = key.toString()
        currentY++
      }

      letterViews[currentX][currentY - 1].setBackgroundResource(R.drawable.letter_background_no_guess)

      if (currentY < currentWord.length) {
        letterViews[currentX][currentY].setBackgroundResource(R.drawable.letter_background_no_guess_and_focus)
      }

      updateSubmitButton()
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
    currentX = 0
    currentY = 0

    letterViews.forEach { row ->
      row.forEach {
        it.text = ""
      }
    }

    letterViews[currentX][currentY].setBackgroundResource(R.drawable.letter_background_no_guess_and_focus)

    updateScore()
    updateLetters()
    updateKeys()
    updateSubmitButton()

    scope.launch {
      initWordDatabase(getMainActivity())

      getRandomWord()?.let { randomWord ->
        Log.d("WORDYYY", randomWord)
        currentWord = randomWord
        deleteWord(randomWord)
        addGuessedWord(currentWord, System.currentTimeMillis(), GuessOutcome.NOT_COMPLETED, 0)
        currentWord = randomWord
      }
    }
  }

  private fun updateSubmitButton() {
    if (currentY >= currentWord.length) {
      scope.launch {
        binding.submitButton.text = if (!hasWord(getGuessedWord())) {
          binding.submitButton.setBackgroundResource(R.drawable.button_background_submit_disabled)
          "Not a Word"
        } else {
          binding.submitButton.setBackgroundResource(R.drawable.button_background_submit_enabled)
          "Submit"
        }
      }
    } else {
      binding.submitButton.setBackgroundResource(R.drawable.button_background_submit_disabled)
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

  private fun updateKeys() {
    val matchedLetters = hashSetOf<Char>()
    val presentLetters = hashSetOf<Char>()
    val noMatchLetters = hashSetOf<Char>()

    letterViews.forEachIndexed { x, row ->
      if (x < currentX) {
        row.forEachIndexed { y, letter ->
          letter.text.firstOrNull()?.lowercaseChar()?.let { char ->
            when {
              currentWord[y] == char -> matchedLetters.add(char)
              currentWord.contains(char) -> presentLetters.add(char)
              else -> noMatchLetters.add(char)
            }
          }
        }
      }
    }

    keyViews.forEach {
      val char = it.key
      val keyView = it.value

      when {
        matchedLetters.contains(char) -> {
          keyView.setBackgroundResource(R.drawable.key_background_match)
          keyView.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorKeyTextGuessed, Color.WHITE))
        }
        presentLetters.contains(char) -> {
          keyView.setBackgroundResource(R.drawable.key_background_present)
          keyView.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorKeyTextGuessed, Color.WHITE))
        }
        noMatchLetters.contains(char) || hintedLetters.contains(char) -> {
          keyView.setBackgroundResource(R.drawable.key_background_no_match)
          keyView.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorKeyTextGuessed, Color.WHITE))
        }
        else -> {
          keyView.setBackgroundResource(R.drawable.key_background_no_guess)
          keyView.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorKeyText, Color.BLACK))
        }
      }
    }
  }

  private fun updateLetters() {
    letterViews.forEachIndexed { x, word -> 
      word.forEachIndexed { y, letter ->
        when {
          x < currentX -> {
            val char = letter.text.firstOrNull()?.lowercaseChar()

            val letterBackgroundRes = when {
              char == null -> R.drawable.letter_background_no_guess
              currentWord[y] == char -> R.drawable.letter_background_match
              currentWord.contains(char) -> R.drawable.letter_background_present
              else -> R.drawable.letter_background_no_match
            }

            letter.setBackgroundResource(letterBackgroundRes)
            letter.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorLetterTextGuessed, Color.WHITE))
          }
          x == currentX -> {
            val letterBackgroundRes = if (x == currentX && y == currentY) {
              R.drawable.letter_background_no_guess_and_focus
            } else {
              R.drawable.letter_background_no_guess
            }

            letter.setBackgroundResource(letterBackgroundRes)
            letter.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorLetterText, Color.BLACK))
          }
          else -> {
            letter.setBackgroundResource(R.drawable.letter_background_no_guess)
            letter.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorLetterText, Color.BLACK))
          }
        }
      }
    }
  }
  
  private fun updateScore() {
    binding.scoreMessage.text = "Score\n${Settings(getMainActivity()).getScore()}"
  }

  private fun updateHintButton() {
    when {
      !hasAskedForHint -> {
        binding.showHint.setBackgroundResource(R.drawable.button_background_get_hint)
      }
      getUnusedLetter().isNotEmpty() -> {
        binding.showHint.setBackgroundResource(R.drawable.button_background_get_hint)
      }
      else -> {
        binding.showHint.setBackgroundResource(R.drawable.button_background_get_hint_unavailable)
      }
    }
  }

  private fun getUnusedLetter(): Set<Char> {
    val usedLetter = hashSetOf<Char>()

    letterViews.forEachIndexed { x, row ->
      row.forEachIndexed { y, letter ->
        letter.text.firstOrNull()?.lowercaseChar()?.let { char ->
          usedLetter.add(char)
        }
      }
    }

    return keyViews.filter { !usedLetter.contains(it.key) }
      .filter { !currentWord.contains(it.key) }
      .filter { !hintedLetters.contains(it.key) }
      .keys
  }
}