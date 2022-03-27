package com.deezus.wordy.ui

import android.graphics.Color
import android.os.Bundle
import android.provider.Contacts.Settings.getSetting
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.deezus.wordy.*
import com.deezus.wordy.data.*
import com.deezus.wordy.databinding.FragmentWordyBinding
import com.deezus.wordy.helpers.DialogMessage
import com.deezus.wordy.helpers.navigationWithOptions
import com.deezus.wordy.helpers.scope
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


class WordyViewModel : ViewModel() {
  val currentWord = MutableLiveData<String?>(null)
  val currentGuess = MutableLiveData("")
  val pastGuesses = MutableLiveData(listOf<String>())
  val hasAskedForHint = MutableLiveData(false)
  val hintedInvalidLetters = MutableLiveData(setOf<Char>())
  val currentGameName = MutableLiveData<GameName?>(null)
}

class WordyFragment : BaseFragment() {

  private var _binding: FragmentWordyBinding? = null
  private val binding get() = _binding!!

  private val letterViews = arrayListOf<ArrayList<TextView>>()
  private val keyViews = hashMapOf<Char, TextView>()
  private val searchButtons = arrayListOf<ConstraintLayout>()

  private lateinit var viewModel: WordyViewModel

  private lateinit var game: Game
  private var maxGuessCount: Int = 6
  private var wordLength: Int = 5

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentWordyBinding.inflate(inflater, container, false)
    viewModel = ViewModelProvider(getMainActivity())[WordyViewModel::class.java]

    val settings = Settings(getMainActivity())

    if (settings.getCurrentGame() != viewModel.currentGameName.value) {
      viewModel.currentGameName.value = settings.getCurrentGame()
    }

    game = viewModel.currentGameName.value?.let { getGame(it) }!!
    updateBoardDimension(game.gameName)

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    scope.launch {
      if (getCurrentWord().isEmpty()) {
        setupGame()
      }

      setupView()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun updateBoardDimension(gameName: GameName) {
    when {
      gameName == GameName.GUESS_4_ENGLISH -> {
        maxGuessCount = 6
        wordLength = 4
      }
      gameName == GameName.GUESS_5_ENGLISH -> {
        maxGuessCount = 6
        wordLength = 5
      }
      gameName == GameName.GUESS_6_ENGLISH -> {
        maxGuessCount = 6
        wordLength = 6
      }
      gameName == GameName.GUESS_7_ENGLISH -> {
        maxGuessCount = 6
        wordLength = 7
      }
    }
  }

  private fun setupView() {
    updateScore()

    letterViews.clear()
    keyViews.clear()
    searchButtons.clear()

    setupKey(binding.keyQ, 'q')
    setupKey(binding.keyW, 'w')
    setupKey(binding.keyE, 'e')
    setupKey(binding.keyR, 'r')
    setupKey(binding.keyT, 't')
    setupKey(binding.keyY, 'y')
    setupKey(binding.keyU, 'u')
    setupKey(binding.keyI, 'i')
    setupKey(binding.keyO, 'o')
    setupKey(binding.keyP, 'p')

    setupKey(binding.keyA, 'a')
    setupKey(binding.keyS, 's')
    setupKey(binding.keyD, 'd')
    setupKey(binding.keyF, 'f')
    setupKey(binding.keyG, 'g')
    setupKey(binding.keyH, 'h')
    setupKey(binding.keyJ, 'j')
    setupKey(binding.keyK, 'k')
    setupKey(binding.keyL, 'l')

    setupKey(binding.keyZ, 'z')
    setupKey(binding.keyX, 'x')
    setupKey(binding.keyC, 'c')
    setupKey(binding.keyV, 'v')
    setupKey(binding.keyB, 'b')
    setupKey(binding.keyN, 'n')
    setupKey(binding.keyM, 'm')

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

    setupSearchButton(binding.searchButton0)
    setupSearchButton(binding.searchButton1)
    setupSearchButton(binding.searchButton2)
    setupSearchButton(binding.searchButton3)
    setupSearchButton(binding.searchButton4)
    setupSearchButton(binding.searchButton5)

    val newConstraintSet = ConstraintSet()
    newConstraintSet.clone(binding.wordyGame)
    newConstraintSet.setDimensionRatio(R.id.letters_holder, "$wordLength:$maxGuessCount")
    newConstraintSet.applyTo(binding.wordyGame)

    binding.deleteLetter.setOnClickListener {
      if (getCurrentGuess().isNotEmpty()) {
        viewModel.currentGuess.value = getCurrentGuess().substring(0, getCurrentGuess().length - 1)
      }
    }

    binding.submitButton.setOnClickListener {
      val settings = Settings(getMainActivity())

      scope.launch {
        when {
          getCurrentGuess().length > getWordLength() -> {

          }
          getCurrentGuess() == getCurrentWord() -> {
            viewModel.pastGuesses.value = getPastGuesses() + getCurrentGuess()
            viewModel.currentGuess.value = ""

            val newScore = getEarnedScore()

            settings.setScore(settings.getScore() + newScore)
            updateScore()

            scope.launch {
              addHistory(
                getCurrentWord(),
                System.currentTimeMillis(),
                game.gameName,
                GameOutcome.SUCCEEDED,
                newScore,
                hasAskedForHint())
            }
          }
          hasWord(game.language, getCurrentGuess()) && getPastGuesses().size + 1 == getMaxGuessCount() -> {
            viewModel.pastGuesses.value = getPastGuesses() + getCurrentGuess()
            viewModel.currentGuess.value = ""

            scope.launch {
              addHistory(
                getCurrentWord(),
                System.currentTimeMillis(),
                game.gameName,
                GameOutcome.SKIPPED,
                0,
                hasAskedForHint())
            }
          }
          hasWord(game.language, getCurrentGuess()) -> {
            viewModel.pastGuesses.value = getPastGuesses() + getCurrentGuess()
            viewModel.currentGuess.value = ""
          }
          else -> {

          }
        }
      }
    }

    viewModel.currentGuess.observe(viewLifecycleOwner) {
      updateLetters()
      updateSubmitButton()
      updateKeys()
      updateModule()
    }

    viewModel.hintedInvalidLetters.observe(viewLifecycleOwner) {
      updateKeys()
      updateGetHintButton()
    }

    binding.skipNext.setOnClickListener {
      DialogMessage(getMainActivity(), "Are you sure you want to skip to the next word?")
        .setPositiveCallback("Yes") {
          val oldWord = getCurrentWord()

          scope.launch {
            addHistory(
              getCurrentWord(),
              System.currentTimeMillis(),
              game.gameName,
              GameOutcome.SKIPPED,
              0,
              hasAskedForHint())

            setupGame()
          }

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
      viewModel.hasAskedForHint.value = true

      getUnusedLetters().shuffled().firstOrNull()?.let {
        viewModel.hintedInvalidLetters.value = getHintedInvalidLetters() + it
      }
    }

    binding.viewHistory.setOnClickListener {
      findNavController().navigationWithOptions(R.id.navigation_history)
    }

    binding.viewBookmark.setOnClickListener {
      findNavController().navigationWithOptions(R.id.navigation_bookmark)
    }

    binding.viewSettings.visibility = View.GONE
    binding.viewSettings.setOnClickListener {
      findNavController().navigationWithOptions(R.id.navigation_settings)
    }
  }

  private fun setupKey(keyView: TextView, key: Char) {
    keyViews[key.lowercaseChar()] = keyView

    keyView.text = key.uppercaseChar().toString()

    keyView.setOnClickListener {
      if (getCurrentGuess().length < getWordLength()) {
        viewModel.currentGuess.value = getCurrentGuess() + key
      }
    }
  }

  private fun setupLetter(letterView: TextView, x: Int, y: Int) {
    if (y >= getWordLength() || x >= getMaxGuessCount()) {
      letterView.visibility = View.GONE
    } else {
      letterView.visibility = View.VISIBLE

      if (y == 0) {
        letterViews.add(arrayListOf())
      }

      letterViews[x].add(letterView)
    }
  }

  private fun getEarnedScore(): Long {
    return min(getMaxGuessCount(), max(getMaxGuessCount() - (getPastGuesses().size - 1), 0)).toLong()
  }

  private fun updateModule() {
    if (getPastGuesses().lastOrNull() == getCurrentWord()) {
      val newScore = getEarnedScore()
      val scoreMessage = if (newScore > 1) {
        "$newScore points"
      } else {
        "$newScore point"
      }

      DialogMessage(getMainActivity(), "Congrats, you guessed the mystery word \"${getCurrentWord()}\" and earned $scoreMessage.")
        .setOnDismissCallback {
          scope.launch {
            setupGame()
          }
        }
        .setPositiveCallback("Play Again") {
          scope.launch {
            setupGame()
          }
        }
        .show()
    } else if (getPastGuesses().size >= getMaxGuessCount()) {
      DialogMessage(getMainActivity(), "Sorry, the mystery word was \"${getCurrentWord()}\"")
        .setOnDismissCallback {
          scope.launch {
            setupGame()
          }
        }
        .setPositiveCallback("Play Again") {
          scope.launch {
            setupGame()
          }
        }
        .show()
    }
  }

  private fun setupSearchButton(view: ConstraintLayout) {
    val index = searchButtons.size

    searchButtons.add(view)

    view.setOnClickListener {
      getPastGuesses().getOrNull(index)?.let {
        DefinitionFragment.currentWord = it
        findNavController().navigationWithOptions(R.id.navigation_definition)
      }
    }
  }

  private fun updateLetters() {
    for (i in 0 until getMaxGuessCount()) {
      if (i < getCurrentX()) {
        for (j in 0 until getWordLength()) {
          val letter = letterViews[i][j]
          val currentChar = getPastGuesses()[i][j]

          letter.text = getPastGuesses()[i][j].uppercase()
          letter.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorLetterTextGuessed, Color.WHITE))

          val backgroundDrawableId = when {
            currentChar == getCurrentWord()[j] -> R.drawable.letter_background_match
            getCurrentWord().contains(currentChar) -> R.drawable.letter_background_present
            else -> R.drawable.key_background_no_match
          }

          letter.setBackgroundResource(backgroundDrawableId)
        }

        searchButtons[i].visibility = View.VISIBLE
      } else {
        for (j in 0 until getWordLength()) {
          val letter = letterViews[i][j]

          letter.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorLetterText, Color.BLACK))

          val backgroundDrawableId = if (i == getCurrentX() && j == getCurrentY()) {
            R.drawable.letter_background_no_guess_and_focus
          } else {
            R.drawable.letter_background_no_guess
          }

          letter.setBackgroundResource(backgroundDrawableId)

          if (i == getCurrentX() && j < getCurrentGuess().length) {
            letter.text = getCurrentGuess()[j].uppercase()
          } else {
            letter.text = ""
          }
        }

        searchButtons[i].visibility = View.INVISIBLE
      }
    }
  }

  private fun updateKeys() {
    val matchedLetters = hashSetOf<Char>()
    val presentLetters = hashSetOf<Char>()
    val noMatchLetters = hashSetOf<Char>()

    getPastGuesses().forEach { guess ->
      guess.forEachIndexed { index, letter ->
        when {
          letter == getCurrentWord()[index] -> matchedLetters.add(letter)
          getCurrentWord().contains(letter) -> presentLetters.add(letter)
          else -> noMatchLetters.add(letter)
        }
      }
    }

    noMatchLetters.addAll(getHintedInvalidLetters())

    keyViews.forEach {
      val key = it.key
      val keyView = it.value

      when {
        matchedLetters.contains(key) -> {
          keyView.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorKeyTextGuessed, Color.WHITE))
          keyView.setBackgroundResource(R.drawable.key_background_match)
        }
        presentLetters.contains(key) -> {
          keyView.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorKeyTextGuessed, Color.WHITE))
          keyView.setBackgroundResource(R.drawable.key_background_present)
        }
        noMatchLetters.contains(key) -> {
          keyView.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorKeyTextGuessed, Color.WHITE))
          keyView.setBackgroundResource(R.drawable.key_background_no_match)
        }
        else -> {
          keyView.setTextColor(MaterialColors.getColor(getMainActivity(), R.attr.colorText, Color.WHITE))
          keyView.setBackgroundResource(R.drawable.key_background_no_guess)
        }
      }
    }
  }

  private fun updateSubmitButton() {
    scope.launch {
      when {
        getCurrentGuess().length < getWordLength() -> {
          binding.submitButton.setBackgroundResource(R.drawable.button_background_disabled)
          binding.submitButton.text = "Guess"
        }
        hasWord(game.language, getCurrentGuess()) -> {
          binding.submitButton.setBackgroundResource(R.drawable.button_background_positive)
          binding.submitButton.text = "Guess"
        }
        else -> {
          binding.submitButton.setBackgroundResource(R.drawable.button_background_disabled)
          binding.submitButton.text = "Not a Word"
        }
      }
    }
  }

  private fun updateGetHintButton() {
    if (getUnusedLetters().isEmpty() && hasAskedForHint()) {
      binding.showHint.setBackgroundResource(R.drawable.button_background_disabled)
    } else {
      binding.showHint.setBackgroundResource(R.drawable.button_background_info)
    }
  }

  private fun updateScore() {
    binding.scoreMessage.text = "Score\n${Settings(getMainActivity()).getScore()}"
  }

  private suspend fun setupGame() {

    getRandomWord(game.wordSet)?.let { randomWord ->
      viewModel.currentWord.value = randomWord
      deleteWord(game.wordSet, randomWord)
      addHistory(
        getCurrentWord(),
        System.currentTimeMillis(),
        game.gameName,
        GameOutcome.NOT_COMPLETED,
        0,
        hasAskedForHint())
    }

    viewModel.pastGuesses.value = listOf()
    viewModel.hasAskedForHint.value = false
    viewModel.hintedInvalidLetters.value = setOf()
    viewModel.currentGuess.value = ""
  }

  private fun getCurrentX(): Int {
    return viewModel.pastGuesses.value?.size ?: 0
  }

  private fun getCurrentY(): Int {
    return viewModel.currentGuess.value?.length ?: 0
  }

  private fun getCurrentGuess(): String {
    return viewModel.currentGuess.value ?: ""
  }

  private fun getCurrentWord(): String {
    return viewModel.currentWord.value ?: ""
  }

  private fun getPastGuesses(): List<String> {
    return viewModel.pastGuesses.value ?: listOf()
  }

  private fun hasAskedForHint(): Boolean {
    return viewModel.hasAskedForHint.value ?: false
  }

  private fun getHintedInvalidLetters(): Set<Char> {
    return viewModel.hintedInvalidLetters.value ?: setOf()
  }

  private fun getMaxGuessCount(): Int {
    return maxGuessCount
  }

  private fun getWordLength(): Int {
    return wordLength
  }

  private fun getUnusedLetters(): List<Char> {
    val usedLetter = hashSetOf<Char>()

    getPastGuesses().forEach {
      it.forEach {
        usedLetter.add(it)
      }
    }

    getCurrentWord().forEach {
      usedLetter.add(it)
    }

    getHintedInvalidLetters().forEach {
      usedLetter.add(it)
    }

    return keyViews.keys.filter { !usedLetter.contains(it) }
  }
}