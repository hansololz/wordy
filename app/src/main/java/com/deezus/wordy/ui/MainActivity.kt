package com.deezus.wordy.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deezus.wordy.WordyApplication
import com.deezus.wordy.ui.components.LocalHapticsEnabled
import com.deezus.wordy.ui.theme.WordyTheme
import kotlinx.coroutines.flow.map

// Lives in the `ui` package because launcher shortcuts created by earlier versions point here.
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    // The app is always dark, so system bar icons stay light whatever the system theme is.
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    super.onCreate(savedInstanceState)

    val container = (application as WordyApplication).container
    splashScreen.setKeepOnScreenCondition { !container.isStartupComplete }

    setContent {
      val hapticsSetting = remember { container.settingsRepository.settings.map { it.hapticsEnabled } }
      val hapticsEnabled by hapticsSetting.collectAsStateWithLifecycle(initialValue = true)

      WordyTheme {
        CompositionLocalProvider(LocalHapticsEnabled provides hapticsEnabled) {
          WordyApp()
        }
      }
    }
  }
}
