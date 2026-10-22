package com.example.tasks.core.data.local

/** Storage record. Stays inside the data layer; repositories map it to the domain [com.example.tasks.core.domain.Task]. */
internal data class TaskRecord(
  val id: Long,
  val title: String,
  val completedAtMillis: Long?,
)
