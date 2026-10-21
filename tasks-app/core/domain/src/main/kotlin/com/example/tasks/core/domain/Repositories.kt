package com.example.tasks.core.domain

import kotlinx.coroutines.flow.Flow

public interface TaskRepository {
  public fun observeTasks(): Flow<List<Task>>

  public suspend fun addTask(title: String): Task

  public suspend fun setCompleted(id: TaskId, isCompleted: Boolean)
}

public interface SettingsRepository {
  public fun observeHideCompleted(): Flow<Boolean>

  public suspend fun setHideCompleted(hide: Boolean)
}
