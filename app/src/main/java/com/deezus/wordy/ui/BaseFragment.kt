package com.deezus.wordy.ui

import androidx.fragment.app.Fragment


abstract class BaseFragment : Fragment() {

  fun getMainActivity(): MainActivity {
    return activity as MainActivity
  }

}