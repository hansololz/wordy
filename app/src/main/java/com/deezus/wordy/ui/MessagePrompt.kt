package com.deezus.wordy.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.deezus.wordy.R
import com.deezus.wordy.databinding.DialogMessageBinding


fun showMessagePrompt(activity: MainActivity,
    message: String,
    positiveAction: Pair<String, () -> Unit>,
    negativeAction: Pair<String, () -> Unit>? = null,
    dismissAction: (() -> Unit)? = null) {

  try {
    val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val view = inflater.inflate(R.layout.dialog_message, null)
    val binding = DialogMessageBinding.bind(view)

    var callback: (() -> Unit)? = dismissAction

    val dialog = AlertDialog.Builder(activity)
      .setView(view)
      .setOnDismissListener {
        callback?.invoke()
      }
      .create()

    binding.message.text = message
    binding.positiveButton.text = positiveAction.first
    binding.positiveButton.setOnClickListener {
      callback = positiveAction.second
      dialog.dismiss()
    }
    negativeAction?.let {
      binding.negativeButton.text = it.first
      binding.negativeButton.setOnClickListener {
        callback = negativeAction.second
        dialog.dismiss()
      }
    }

    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    dialog.show()
  } catch (exception: Exception) {

  }
}