package com.example.tasks.core.testing

import com.example.tasks.core.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(hideCompleted: Boolean = false) : SettingsRepository {
  private val hide = MutableStateFlow(hideCompleted)

  override fun observeHideCompleted(): Flow<Boolean> = hide

  override suspend fun setHideCompleted(hide: Boolean) {
    this.hide.value = hide
  }
}
