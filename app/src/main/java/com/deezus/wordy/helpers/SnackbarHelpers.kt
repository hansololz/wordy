package com.deezus.wordy.helpers

import com.deezus.wordy.ui.MainActivity
import com.google.android.material.snackbar.Snackbar


fun showSnackBar(activity: MainActivity, text: String, maybePrompt: String? = null, maybeCallback: (() -> Unit)? = null) {
  val snackBar = Snackbar.make(activity.findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG)

  given(maybePrompt, maybeCallback)?.thenLet { prompt, callback ->
    snackBar.setAction(prompt) { callback() }
  }

//  snackBar.setBackgroundTint(getColors(activity).snackBarBackground)
//  snackBar.setActionTextColor(getColors(activity).accent)
//  snackBar.setTextColor(getColors(activity).snackBarText)

  snackBar.show()
}