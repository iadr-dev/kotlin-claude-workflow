package com.example.tasks.core.data

import com.example.tasks.core.data.local.SettingsLocalDataSource
import com.example.tasks.core.data.local.TaskLocalDataSource
import com.example.tasks.core.data.local.TaskRecord
import com.example.tasks.core.domain.SettingsRepository
import com.example.tasks.core.domain.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/** Composition entry point for the data layer; callers only see domain interfaces. */
public class DataModule(
  ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  clock: () -> Long = System::currentTimeMillis,
) {
  private val taskSource =
    TaskLocalDataSource(
      seed = listOf(
        TaskRecord(id = 1, title = "Try the Tasks sample", completedAtMillis = null),
        TaskRecord(id = 2, title = "Read the architecture skill", completedAtMillis = 0L),
      ),
    )

  public val taskRepository: TaskRepository = DefaultTaskRepository(taskSource, ioDispatcher, clock)

  public val settingsRepository: SettingsRepository = DefaultSettingsRepository(SettingsLocalDataSource())
}

internal class DefaultSettingsRepository(private val local: SettingsLocalDataSource) : SettingsRepository {
  override fun observeHideCompleted(): Flow<Boolean> = local.hideCompleted

  override suspend fun setHideCompleted(hide: Boolean) {
    local.hideCompleted.value = hide
  }
}
