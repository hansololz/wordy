package com.deezus.wordy.ui

import android.app.AlertDialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import com.deezus.wordy.R
import com.deezus.wordy.databinding.DialogHintsBinding
import com.deezus.wordy.helpers.performFeedback


enum class HintAction { NONE, REVEAL_INVALID_CHARACTER, REVEAL_ALL_INVALID_CHARACTER, REVEAL_VALID_CHARACTER }

fun showHintPrompt(activity: MainActivity, callback: (HintAction) -> Unit) {
  try {
    val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val view = inflater.inflate(R.layout.dialog_hints, null)
    val binding = DialogHintsBinding.bind(view)
    var dismissAction = HintAction.NONE
    val dialog = AlertDialog.Builder(activity)
      .setView(view)
      .setOnDismissListener {
        callback(dismissAction)
      }
      .create()

    binding.promptHolder.setOnClickListener {
      dismissAction = HintAction.NONE
      dialog.dismiss()
    }

    binding.revealValidCharacter.setOnClickListener {
      dismissAction = HintAction.REVEAL_VALID_CHARACTER
      performFeedback(activity, it)
      dialog.dismiss()
    }

    binding.revealInvalidCharacter.setOnClickListener {
      dismissAction = HintAction.REVEAL_INVALID_CHARACTER
      performFeedback(activity, it)
      dialog.dismiss()
    }

    binding.revealAllInvalidCharacter.setOnClickListener {
      dismissAction = HintAction.REVEAL_ALL_INVALID_CHARACTER
      performFeedback(activity, it)
      dialog.dismiss()
    }

    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    dialog.show()
  } catch (exception: Exception) {

  }
}