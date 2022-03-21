package com.deezus.wordy.ui

import androidx.fragment.app.Fragment


abstract class BaseFragment : Fragment() {

  lateinit var mainViewModel: MainViewModel

  fun getMainActivity(): MainActivity {
    return activity as MainActivity
  }

}