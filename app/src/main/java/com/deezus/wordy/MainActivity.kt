package com.deezus.wordy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModelProvider
import com.deezus.wordy.databinding.ActivityMainBinding
import kotlinx.coroutines.*


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

    scope.launch {
      initWordDatabase(thisActivity)
    }

  }
}