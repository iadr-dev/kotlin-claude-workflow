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
      val completed = all.count { it.isCompleted }
      VisibleTasks(
        tasks = if (hideCompleted) ordered.filterNot { it.isCompleted } else ordered,
        hideCompleted = hideCompleted,
        hiddenCount = if (hideCompleted) completed else 0,
        completedCount = completed,
        openCount = all.size - completed,
      )
    }
}

public data class VisibleTasks(
  val tasks: List<Task>,
  val hideCompleted: Boolean,
  val hiddenCount: Int,
  /** Completed tasks, whether or not the "hide completed" setting currently hides them. */
  val completedCount: Int,
  /** Tasks not yet completed, whether or not any completed ones are hidden. */
  val openCount: Int,
)
