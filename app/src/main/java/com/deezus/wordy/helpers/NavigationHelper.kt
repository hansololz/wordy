package com.deezus.wordy.helpers

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.deezus.wordy.R


private fun getDefaultNavigationOptions(): NavOptions {
  return NavOptions.Builder()
    .setEnterAnim(R.anim.nav_default_enter_anim)
    .setExitAnim(R.anim.nav_default_exit_anim)
    .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
    .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
    .build()
}

fun NavController.navigationWithOptions(navigationId: Int) {
  navigate(navigationId, null, getDefaultNavigationOptions())
}