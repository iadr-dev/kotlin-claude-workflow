package com.example.tasks.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasks.core.domain.AddTaskResult
import com.example.tasks.core.domain.AddTaskUseCase
import com.example.tasks.core.domain.GetVisibleTasksUseCase
import com.example.tasks.core.domain.SettingsRepository
import com.example.tasks.core.domain.TaskId
import com.example.tasks.core.domain.TaskRepository
import com.example.tasks.core.domain.TitleError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TasksViewModel(
  getVisibleTasks: GetVisibleTasksUseCase,
  private val addTask: AddTaskUseCase,
  private val tasks: TaskRepository,
  private val settings: SettingsRepository,
) : ViewModel() {
  private val draft = MutableStateFlow(Draft())

  // Main-thread only: guards against double submission while an add is in flight.
  private var isSubmitting = false

  internal val uiState: StateFlow<TasksUiState> =
    combine(getVisibleTasks(), draft) { visible, draft ->
        TasksUiState.Content(
          tasks = visible.tasks.map { TaskItemUi(it.id.value, it.title, it.isCompleted) },
          hideCompleted = visible.hideCompleted,
          hiddenCount = visible.hiddenCount,
          completedCount = visible.completedCount,
          openCount = visible.openCount,
          draft = draft.text,
          draftError = draft.error,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState.Loading)

  fun onDraftChange(text: String) {
    draft.value = Draft(text = text)
  }

  fun onAddTask() {
    if (isSubmitting) return
    isSubmitting = true
    val submitted = draft.value.text
    viewModelScope.launch {
      try {
        when (val result = addTask(submitted)) {
          is AddTaskResult.Added -> draft.update { if (it.text == submitted) Draft() else it }
          is AddTaskResult.Invalid -> draft.update { if (it.text == submitted) it.copy(error = result.error.toUi()) else it }
        }
      } finally {
        isSubmitting = false
      }
    }
  }

  fun onCompletedChange(id: Long, isCompleted: Boolean) {
    viewModelScope.launch { tasks.setCompleted(TaskId(id), isCompleted) }
  }

  fun onClearCompleted() {
    viewModelScope.launch { tasks.clearCompleted() }
  }

  fun onHideCompletedChange(hide: Boolean) {
    viewModelScope.launch { settings.setHideCompleted(hide) }
  }

  private data class Draft(val text: String = "", val error: DraftError? = null)
}

private fun TitleError.toUi(): DraftError =
  when (this) {
    TitleError.Blank -> DraftError.Blank
    is TitleError.TooLong -> DraftError.TooLong(maxLength)
  }
