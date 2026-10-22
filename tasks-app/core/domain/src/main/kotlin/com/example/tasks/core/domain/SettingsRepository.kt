package com.example.tasks.core.domain

import kotlinx.coroutines.flow.Flow

public interface SettingsRepository {
  public fun observeHideCompleted(): Flow<Boolean>

  public suspend fun setHideCompleted(hide: Boolean)
}
