package com.example.tasks.core.data

import com.example.tasks.core.data.local.TaskLocalDataSource
import com.example.tasks.core.data.local.TaskRecord
import com.example.tasks.core.domain.Task
import com.example.tasks.core.domain.TaskId
import com.example.tasks.core.domain.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class DefaultTaskRepository(
  private val local: TaskLocalDataSource,
  private val ioDispatcher: CoroutineDispatcher,
  private val clock: () -> Long,
) : TaskRepository {
  override fun observeTasks(): Flow<List<Task>> =
    local.observe().map { records -> records.map(TaskRecord::toDomain) }.flowOn(ioDispatcher)

  // Writes are serialized in call order (Mutex is FIFO) so rapid toggles apply as the user made them.
  private val writes = Mutex()

  override suspend fun addTask(title: String): Task =
    writes.withLock { withContext(ioDispatcher) { local.insert(title).toDomain() } }

  override suspend fun setCompleted(id: TaskId, isCompleted: Boolean) {
    writes.withLock {
      withContext(ioDispatcher) { local.updateCompletedAt(id.value, if (isCompleted) clock() else null) }
    }
  }
}

internal fun TaskRecord.toDomain(): Task = Task(id = TaskId(id), title = title, isCompleted = completedAtMillis != null)
