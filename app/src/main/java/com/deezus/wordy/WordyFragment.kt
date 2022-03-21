package com.deezus.wordy

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.deezus.wordy.databinding.FragmentWordyBinding
import kotlinx.coroutines.launch
import java.lang.StringBuilder


class WordyFragment : BaseFragment() {

  private var _binding: FragmentWordyBinding? = null
  private val binding get() = _binding!!

  private val letterViews = arrayListOf<ArrayList<TextView>>()
  private var currentX = 0
  private var currentY = 0
  private val keyViews = hashMapOf<Char, TextView>()
  private var currentWord = "nomad"
  private val guessedLetters = hashSetOf<Char>()

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
      }

      updateButton()
    }

  }

  private fun setupKey(keyView: TextView, key: Char) {
    keyViews[key] = keyView

    keyView.setOnClickListener {
      if (currentY < currentWord.length && letterViews[currentX][currentY].text.isEmpty()) {
        letterViews[currentX][currentY].text = key.toString()
        currentY++
      }

      updateButton()
    }
  }

  private fun setupLetter(letterView: TextView, x: Int, y: Int) {
    if (y >= currentWord.length) {
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
    scope.launch {
      initWordDatabase(getMainActivity())

      getRandomWord()?.let { randomWord ->
        Log.d("WORDYYY", randomWord)
        deleteWord(randomWord)
        currentWord = randomWord
      }
    }
  }

  private fun updateButton() {
    if (currentY >= currentWord.length) {
      scope.launch {
        val wordBuilder = StringBuilder()

        letterViews[currentX].forEach {
          wordBuilder.append(it.text)
        }

        binding.submitButton.text = if (!hasWord(wordBuilder.toString().lowercase())) {
          "Not A\nWord"
        } else {
          "Submit"
        }
      }
    } else {
      binding.submitButton.text = "Submit"
    }
  }

}