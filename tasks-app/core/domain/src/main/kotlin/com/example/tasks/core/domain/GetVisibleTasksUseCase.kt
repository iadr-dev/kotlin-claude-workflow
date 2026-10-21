package com.example.tasks.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Tasks the user should currently see: open tasks first, then completed ones, with completed
 * tasks dropped while the "hide completed" setting is on.
 */
public class GetVisibleTasksUseCase(
  private val tasks: TaskRepository,
  private val settings: SettingsRepository,
) {
  public operator fun invoke(): Flow<VisibleTasks> =
    combine(tasks.observeTasks(), settings.observeHideCompleted()) { all, hideCompleted ->
      val ordered = all.sortedWith(compareBy<Task> { it.isCompleted }.thenBy { it.id.value })
      VisibleTasks(
        tasks = if (hideCompleted) ordered.filterNot { it.isCompleted } else ordered,
        hideCompleted = hideCompleted,
        hiddenCount = if (hideCompleted) all.count { it.isCompleted } else 0,
      )
    }
}

public data class VisibleTasks(
  val tasks: List<Task>,
  val hideCompleted: Boolean,
  val hiddenCount: Int,
)
