package com.example.tasks.core.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

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

  fun deleteCompleted() {
    records.update { list -> list.filter { it.completedAtMillis == null } }
  }

  fun observe(): Flow<List<TaskRecord>> = records
}
