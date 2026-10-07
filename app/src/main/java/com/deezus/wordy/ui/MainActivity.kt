package com.deezus.wordy.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.deezus.wordy.WordyApplication
import com.deezus.wordy.ui.theme.WordyTheme

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
      WordyTheme {
        WordyApp()
      }
    }
  }
}
