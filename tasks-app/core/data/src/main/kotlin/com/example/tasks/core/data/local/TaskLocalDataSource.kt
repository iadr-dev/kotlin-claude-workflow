package com.example.tasks.core.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/** Storage record. Stays inside the data layer; repositories map it to the domain [com.example.tasks.core.domain.Task]. */
internal data class TaskRecord(
  val id: Long,
  val title: String,
  val completedAtMillis: Long?,
)

/** In-memory source standing in for a database (Room/SQLDelight) in this sample. */
internal class TaskLocalDataSource(seed: List<TaskRecord> = emptyList()) {
  private val records = MutableStateFlow(seed)
  private val nextId = AtomicLong((seed.maxOfOrNull { it.id } ?: 0L) + 1)

  fun insert(title: String): TaskRecord {
    val record = TaskRecord(id = nextId.getAndIncrement(), title = title, completedAtMillis = null)
    records.update { it + record }
    return record
  }

  fun updateCompletedAt(id: Long, completedAtMillis: Long?) {
    records.update { list -> list.map { if (it.id == id) it.copy(completedAtMillis = completedAtMillis) else it } }
  }

  fun observe(): Flow<List<TaskRecord>> = records
}

internal class SettingsLocalDataSource(hideCompleted: Boolean = false) {
  val hideCompleted = MutableStateFlow(hideCompleted)
}
