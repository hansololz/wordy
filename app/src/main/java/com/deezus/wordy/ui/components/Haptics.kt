package com.deezus.wordy.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Whether the player has haptic feedback turned on. Provided once, at the root of the app. */
val LocalHapticsEnabled = compositionLocalOf { true }

/** A tap feedback to call from button handlers. Does nothing when haptics are off. */
@Composable
fun rememberHaptic(enabled: Boolean = LocalHapticsEnabled.current): () -> Unit {
  val view = LocalView.current
  return remember(view, enabled) {
    { if (enabled) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
  }
}
