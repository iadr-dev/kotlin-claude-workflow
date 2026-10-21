package com.example.tasks.core.domain

@JvmInline
public value class TaskId(public val value: Long)

public data class Task(
  val id: TaskId,
  val title: String,
  val isCompleted: Boolean,
)
