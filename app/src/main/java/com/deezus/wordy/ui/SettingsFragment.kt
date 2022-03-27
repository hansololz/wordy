package com.deezus.wordy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.deezus.wordy.data.GameName
import com.deezus.wordy.data.Settings
import com.deezus.wordy.databinding.FragmentSettingsBinding
import com.deezus.wordy.helpers.performFeedback
import com.deezus.wordy.helpers.popWithOptions


class SettingsFragment : BaseFragment() {

  private var _binding: FragmentSettingsBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSettingsBinding.inflate(inflater, container, false)
    val root: View = binding.root

    return root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val settings = Settings(getMainActivity())

    binding.backButton.setOnClickListener {
      findNavController().popWithOptions()
      performFeedback(getMainActivity(), it)
    }

    when (settings.getCurrentGame()) {
      GameName.GUESS_4_ENGLISH -> binding.optionGuess4English.isChecked = true
      GameName.GUESS_5_ENGLISH -> binding.optionGuess5English.isChecked = true
      GameName.GUESS_6_ENGLISH -> binding.optionGuess6English.isChecked = true
    }

    binding.optionGuess4English.setOnClickListener {
      settings.setCurrentGame(GameName.GUESS_4_ENGLISH)
    }

    binding.optionGuess5English.setOnClickListener {
      settings.setCurrentGame(GameName.GUESS_5_ENGLISH)
    }

    binding.optionGuess6English.setOnClickListener {
      settings.setCurrentGame(GameName.GUESS_6_ENGLISH)
    }

    binding.enableHapticFeedback.isChecked = settings.isHapticFeedbackEnabled()

    binding.enableHapticFeedback.setOnClickListener {
      settings.setHapticFeedback(binding.enableHapticFeedback.isChecked)
    }
  }
}