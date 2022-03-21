package com.deezus.wordy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.deezus.wordy.databinding.FragmentSettingsBinding
import com.deezus.wordy.ui.BaseFragment


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

}