package com.deezus.wordy.helpers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


val viewModelJob = SupervisorJob()
val scope = CoroutineScope(Dispatchers.Main + viewModelJob)
