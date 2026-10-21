package com.example.tasks

import android.app.Application
import com.example.tasks.core.data.DataModule
import com.example.tasks.core.domain.AddTaskUseCase
import com.example.tasks.core.domain.GetVisibleTasksUseCase
import com.example.tasks.feature.tasks.TasksViewModel

class TasksApplication : Application() {
  val container: AppContainer by lazy { AppContainer() }
}

/** Composition root: the only place that knows concrete data implementations. */
class AppContainer(private val data: DataModule = DataModule()) {
  fun tasksViewModel(): TasksViewModel =
    TasksViewModel(
      getVisibleTasks = GetVisibleTasksUseCase(data.taskRepository, data.settingsRepository),
      addTask = AddTaskUseCase(data.taskRepository),
      tasks = data.taskRepository,
      settings = data.settingsRepository,
    )
}
