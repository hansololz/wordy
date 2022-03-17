package com.deezus.wordy

import androidx.fragment.app.Fragment


abstract class BaseFragment : Fragment() {

  lateinit var mainViewModel: MainViewModel

  fun getMainActivity(): MainActivity {
    return activity as MainActivity
  }

}