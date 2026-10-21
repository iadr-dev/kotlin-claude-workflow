package com.example.tasks.core.testing

import com.example.tasks.core.domain.SettingsRepository
import com.example.tasks.core.domain.Task
import com.example.tasks.core.domain.TaskId
import com.example.tasks.core.domain.TaskRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeTaskRepository(initial: List<Task> = emptyList()) : TaskRepository {
  private val tasks = MutableStateFlow(initial)
  private var nextId = (initial.maxOfOrNull { it.id.value } ?: 0L) + 1

  /** When set, [addTask] suspends until it completes, simulating a slow write. */
  var addGate: CompletableDeferred<Unit>? = null

  override fun observeTasks(): Flow<List<Task>> = tasks

  override suspend fun addTask(title: String): Task {
    addGate?.await()
    val task = Task(TaskId(nextId++), title, isCompleted = false)
    tasks.update { it + task }
    return task
  }

  override suspend fun setCompleted(id: TaskId, isCompleted: Boolean) {
    tasks.update { list -> list.map { if (it.id == id) it.copy(isCompleted = isCompleted) else it } }
  }
}

class FakeSettingsRepository(hideCompleted: Boolean = false) : SettingsRepository {
  private val hide = MutableStateFlow(hideCompleted)

  override fun observeHideCompleted(): Flow<Boolean> = hide

  override suspend fun setHideCompleted(hide: Boolean) {
    this.hide.value = hide
  }
}
