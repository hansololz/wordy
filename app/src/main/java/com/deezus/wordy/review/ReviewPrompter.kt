package com.deezus.wordy.review

import android.app.Activity
import android.util.Log
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CancellationException

/** Asks for a store rating through Google Play's in-app review sheet. */
object ReviewPrompter {
  private const val TAG = "ReviewPrompter"

  /**
   * Play decides whether the sheet is actually shown, and there is no result to act on. Failures
   * (for example on a device without the Play Store) are ignored.
   */
  suspend fun request(activity: Activity) {
    try {
      val manager = ReviewManagerFactory.create(activity)
      manager.launchReview(activity, manager.requestReview())
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Log.w(TAG, "In-app review unavailable", error)
    }
  }
}
