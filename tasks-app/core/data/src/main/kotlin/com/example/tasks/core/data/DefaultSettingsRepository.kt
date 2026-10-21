package com.example.tasks.core.data

import com.example.tasks.core.data.local.SettingsLocalDataSource
import com.example.tasks.core.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultSettingsRepository(private val local: SettingsLocalDataSource) : SettingsRepository {
  override fun observeHideCompleted(): Flow<Boolean> = local.hideCompleted

  override suspend fun setHideCompleted(hide: Boolean) {
    local.hideCompleted.value = hide
  }
}
