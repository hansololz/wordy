package com.deezus.wordy.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.deezus.wordy.R
import com.deezus.wordy.data.*
import com.deezus.wordy.databinding.ActivityMainBinding
import com.deezus.wordy.helpers.scope
import kotlinx.coroutines.launch


class MainViewModel : ViewModel() {
  val currentGame = MutableLiveData<GameName>(null)
}

class MainActivity : AppCompatActivity() {

  lateinit var mainViewModel: MainViewModel
  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    supportActionBar?.hide()

    mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    binding = ActivityMainBinding.inflate(layoutInflater)

    val thisActivity = this

    mainViewModel.currentGame.observe(this) { gameName ->
      gameName?.let {
        val game = getGame(it)
        findNavController(R.id.host_fragment).apply {
          popBackStack(R.id.navigation_loading, true)
          navigate(R.id.navigation_wordy)
        }
      }
    }

    scope.launch {
      initWordDatabase(thisActivity)
      initBookmarkDatabase(thisActivity)
      initHistoryDatabase(thisActivity)

      mainViewModel.currentGame.value = GameName.GUESS_5_ENGLISH
    }
  }
}