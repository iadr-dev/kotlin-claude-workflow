package com.example.tasks.core.domain

/** Validates and adds a task. Titles are trimmed; blank or overlong titles are rejected. */
public class AddTaskUseCase(private val tasks: TaskRepository) {
  public suspend operator fun invoke(rawTitle: String): AddTaskResult {
    val title = rawTitle.trim()
    return when {
      title.isEmpty() -> AddTaskResult.Invalid(TitleError.Blank)
      title.length > MAX_TITLE_LENGTH -> AddTaskResult.Invalid(TitleError.TooLong(MAX_TITLE_LENGTH))
      else -> AddTaskResult.Added(tasks.addTask(title))
    }
  }

  public companion object {
    public const val MAX_TITLE_LENGTH: Int = 100
  }
}

public sealed interface AddTaskResult {
  public data class Added(val task: Task) : AddTaskResult

  public data class Invalid(val error: TitleError) : AddTaskResult
}

public sealed interface TitleError {
  public data object Blank : TitleError

  public data class TooLong(val maxLength: Int) : TitleError
}
