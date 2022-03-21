package com.deezus.wordy.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

  val username = MutableLiveData<String?>().apply { value = null }
  val sessionToken = MutableLiveData<String?>().apply { value = null }

}