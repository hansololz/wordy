package com.deezus.wordy.helpers

import android.view.HapticFeedbackConstants
import android.view.View
import com.deezus.wordy.data.Settings
import com.deezus.wordy.ui.MainActivity


fun performFeedback(activity: MainActivity, view: View) {
  if (Settings(activity).isHapticFeedbackEnabled()) {
    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
      HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
  }
}