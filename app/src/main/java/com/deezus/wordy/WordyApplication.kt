package com.deezus.wordy

import android.app.Application
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.deezus.wordy.di.AppContainer

class WordyApplication : Application() {

  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}

/** The dependency container, for use inside ViewModel factories. */
val CreationExtras.appContainer: AppContainer
  get() = (checkNotNull(this[APPLICATION_KEY]) as WordyApplication).container
