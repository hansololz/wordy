package com.deezus.wordy.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.deezus.wordy.R
import com.deezus.wordy.data.*
import com.deezus.wordy.databinding.ActivityMainBinding
import com.deezus.wordy.helpers.navigationWithOptions
import com.deezus.wordy.helpers.scope
import com.deezus.wordy.helpers.showSnackBar
import kotlinx.coroutines.launch


class MainViewModel : ViewModel() {

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

    scope.launch {
      initWordDatabase(thisActivity)
      initBookmarkDatabase(thisActivity)
      initHistoryDatabase(thisActivity)

      settings.getCurrentGame() ?: run {
        settings.setCurrentGame(GameName.GUESS_5_ENGLISH)
      }

      settings.getCurrentGame()?.let {
        getGame(it)?.let {
          findNavController(R.id.host_fragment).apply {
            popBackStack(R.id.navigation_loading, true)
            navigationWithOptions(it.navigationId)
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()

    if (Settings(this).shouldAskForReview()) {
      showMessagePrompt(
        this,
        "Would you like to take a moment to rate this app on the Google play store?",
          Pair("Rate App", this::rateApp),
          Pair("Never", this::neverRateApp),
          this::rateAppLater
        )
    }
  }

  private fun rateApp() {
    Settings(this).disableShouldAskForReviewTime()

    val flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
        Intent.FLAG_ACTIVITY_NEW_DOCUMENT
    val uri = Uri.parse("market://details?id=$packageName")
    val goToMarket = Intent(Intent.ACTION_VIEW, uri)

    goToMarket.addFlags(flags)

    try {
      startActivity(goToMarket)
      Settings(this).disableShouldAskForReviewTime()
    } catch (e: ActivityNotFoundException) {
      showSnackBar(this, "Something went wrong, can't open Google Play Store.")
    }
  }

  private fun neverRateApp() {
    Settings(this).disableShouldAskForReviewTime()
  }

  private fun rateAppLater() {
    Settings(this).setShouldAskForReviewTime()
  }
}