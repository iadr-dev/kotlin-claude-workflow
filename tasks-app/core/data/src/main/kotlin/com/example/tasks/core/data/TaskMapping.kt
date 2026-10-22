package com.example.tasks.core.data

import com.example.tasks.core.data.local.TaskRecord
import com.example.tasks.core.domain.Task
import com.example.tasks.core.domain.TaskId

internal fun TaskRecord.toDomain(): Task = Task(id = TaskId(id), title = title, isCompleted = completedAtMillis != null)
