package com.deezus.wordy.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.deezus.wordy.R
import com.deezus.wordy.data.*
import com.deezus.wordy.databinding.ActivityMainBinding
import com.deezus.wordy.helpers.navigationWithOptions
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
    val settings = Settings(this)

    mainViewModel.currentGame.observe(this) { gameName ->
      Log.d("WORDYYY", "START $gameName")

      gameName?.let {
        val game = getGame(it)
        findNavController(R.id.host_fragment).apply {
          popBackStack(R.id.navigation_loading, true)
          navigationWithOptions(game.navigationId)
        }
      }
    }

    scope.launch {
      initWordDatabase(thisActivity)
      initBookmarkDatabase(thisActivity)
      initHistoryDatabase(thisActivity)

      settings.getCurrentGame() ?: run {
        settings.setCurrentGame(GameName.GUESS_5_ENGLISH)
      }

      mainViewModel.currentGame.value = settings.getCurrentGame()
    }
  }
}