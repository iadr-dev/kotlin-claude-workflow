package com.example.tasks.core.data.local

import kotlinx.coroutines.flow.MutableStateFlow

internal class SettingsLocalDataSource(hideCompleted: Boolean = false) {
  val hideCompleted = MutableStateFlow(hideCompleted)
}
