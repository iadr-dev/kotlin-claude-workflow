package com.example.tasks.core.domain

import kotlinx.coroutines.flow.Flow

public interface TaskRepository {
  public fun observeTasks(): Flow<List<Task>>

  public suspend fun addTask(title: String): Task

  public suspend fun setCompleted(id: TaskId, isCompleted: Boolean)

  /** Permanently deletes every completed task. Open tasks are untouched; with none completed it is a no-op. */
  public suspend fun clearCompleted()
}
