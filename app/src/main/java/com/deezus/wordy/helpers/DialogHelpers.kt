package com.deezus.wordy.helpers

import android.app.AlertDialog
import android.graphics.Color
import com.deezus.wordy.R
import com.deezus.wordy.ui.MainActivity
import com.google.android.material.color.MaterialColors


class DialogMessage {
  private val activity: MainActivity
  private val messageText: String?
  private val messageId: Int?

  private var title: String? = null
  private var positiveMessage: String? = null
  private var positiveCallback: (() -> Unit)? = null
  private var negativeMessage: String? = null
  private var negativeCallback: (() -> Unit)? = null
  private var neutralMessage: String? = null
  private var neutralCallback: (() -> Unit)? = null
  private var onDismissCallback: (() -> Unit)? = null
  private var isDismissable = true

  constructor(activity: MainActivity, messageText: String) {
    this.activity = activity
    this.messageText = messageText
    this.messageId = null
  }

  constructor(activity: MainActivity, messageId: Int) {
    this.activity = activity
    this.messageText = null
    this.messageId = messageId
  }

  fun setIsDismissable(isDismissable: Boolean): DialogMessage {
    this.isDismissable = isDismissable
    return this
  }

  fun setTitle(title: String): DialogMessage {
    this.title = title
    return this
  }

  fun setPositiveCallback(message: String, callback: (() -> Unit)): DialogMessage {
    positiveMessage = message
    positiveCallback = callback
    return this
  }

  fun setNegativeCallback(message: String, callback: (() -> Unit)): DialogMessage {
    negativeMessage = message
    negativeCallback = callback
    return this
  }

  fun setNeutalCallback(message: String, callback: (() -> Unit)): DialogMessage {
    neutralMessage = message
    neutralCallback = callback
    return this
  }

  fun setOnDismissCallback(callback: () -> Unit): DialogMessage {
    onDismissCallback = callback
    return this
  }

  fun show() {
    val dialogBuilder = AlertDialog.Builder(activity)

    this.title?.let { dialogBuilder.setTitle(it) }

    given(positiveMessage, positiveCallback)?.thenLet { message, callback ->
      dialogBuilder.setPositiveButton(message) { dialog, it ->
        callback.invoke()
      }
    }

    given(negativeMessage, negativeCallback)?.thenLet { message, callback ->
      dialogBuilder.setNegativeButton(message) { dialog, it ->
        callback.invoke()
      }
    }

    given(neutralMessage, neutralCallback)?.thenLet { message, callback ->
      dialogBuilder.setNeutralButton(message) { dialog, it ->
        callback.invoke()
      }
    }

    onDismissCallback?.let {
      dialogBuilder.setOnDismissListener { it() }
    }

    if (!isDismissable) {
      dialogBuilder.setCancelable(false)
    }

    if (messageText != null) {
      dialogBuilder.setMessage(messageText)
    }

    val dialog = dialogBuilder.create()

    try {
      dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

      dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
          ?.setTextColor(MaterialColors.getColor(activity, R.attr.colorDialogButton, Color.BLACK))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
          ?.setTextColor(MaterialColors.getColor(activity, R.attr.colorDialogButton, Color.BLACK))
      }

      dialog.show()
    } catch (exception: Exception) {

    }
  }
}