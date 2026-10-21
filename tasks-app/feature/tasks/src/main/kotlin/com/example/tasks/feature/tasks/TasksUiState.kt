package com.example.tasks.feature.tasks

internal sealed interface TasksUiState {
  data object Loading : TasksUiState

  data class Content(
    val tasks: List<TaskItemUi>,
    val hideCompleted: Boolean,
    val hiddenCount: Int,
    val draft: String,
    val draftError: DraftError?,
  ) : TasksUiState
}

internal data class TaskItemUi(
  val id: Long,
  val title: String,
  val isCompleted: Boolean,
)

internal sealed interface DraftError {
  data object Blank : DraftError

  data class TooLong(val maxLength: Int) : DraftError
}
